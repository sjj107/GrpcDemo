package com.fiberhome.dbserver.master;

import java.io.File;

import org.apache.commons.lang.StringUtils;
// import org.apache.zookeeper.KeeperException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fiberhome.dbserver.common.common.ServerType;
import com.fiberhome.dbserver.common.config.MasterConfiguration;
// import com.fiberhome.dbserver.common.config.ZookeeperConfiguration;
import com.fiberhome.dbserver.common.elements.server.ServerName;
import com.fiberhome.dbserver.common.exception.DBException;
import com.fiberhome.dbserver.common.executor.DBExecutorManager;
import com.fiberhome.dbserver.common.services.HasThread;
import com.fiberhome.dbserver.common.services.MonitoredTask;
import com.fiberhome.dbserver.common.services.Sleeper;
import com.fiberhome.dbserver.common.services.TaskMonitor;
// import com.fiberhome.dbserver.common.zookeeper.ZooKeeperWatcher;
// import com.fiberhome.dbserver.master.archive.ArchiveManager;
import com.fiberhome.dbserver.master.constants.MasterConstants;
// import com.fiberhome.dbserver.master.region.RegionManager;
// import com.fiberhome.dbserver.master.slave.SlaveManager;
import com.fiberhome.dbserver.master.slave.heartbeatserver.HeartBeatManager;
// import com.fiberhome.dbserver.master.table.TableManager;
// import com.fiberhome.dbserver.master.zookeeper.MasterTrackerManager;
// import com.fiberhome.dbserver.protocol.client.GrpcClientFactory;
import com.fiberhome.dbserver.protocol.server.GrpcServer;
import com.fiberhome.dbserver.protocol.server.GrpcServiceRegistry;
import com.fiberhome.dbserver.tools.conf.BaseConfiguration;
import com.fiberhome.dbserver.tools.util.Addressing;

/**
 * Master服务启动主类
 * 包括服务配置加载、初始化、各子服务启动、停止等操作
 *
 * @author xiajunsheng
 * @update dinne
 * @date 2020/10/29
 * @since 1.0.0
 */
public class DBMaster extends HasThread implements DBMasterServices
{
    /**
     * 日志
     */
    static
    {
        System.setProperty("SERVICE_NAME", "master");
    }

    private static final Logger LOG = LoggerFactory.getLogger(DBMaster.class);

    /**
     * 唯一实例
     *
     * @return Master实例
     */
    public static DBMaster getMaster()
    {
        return Holder.master;
    }

    static class Holder
    {
        private static DBMaster master = new DBMaster();
    }

    /**
     * Master配置项
     */
    private final BaseConfiguration config;

    /**
     * Master的ServerName
     */
    private final ServerName serverName;

    /**
     * Master标识
     */
    private final MasterFlag masterFlag = new MasterFlag();

    /**
     * 进程睡眠管理
     */
    private final Sleeper stopSleeper = new Sleeper(60000L, this);

    /**
     * 集群版本
     */
    private final String version;

    /**
     * Master启动时间
     */
    private long masterStartTime;

    /**
     * Master切换为Active时间
     */
    private long masterActiveTime;

    // /**
    //  * Master端ZK管理服务
    //  */
    // private MasterTrackerManager masterTrackerManager;
    //
    // /**
    //  * 主备切换
    //  */
    // private ActiveMasterManager activeMasterManager;
    //
    // /**
    //  * Master表管理服务
    //  */
    // private TableManager tableManager;
    //
    // /**
    //  * Master归档管理
    //  */
    // private ArchiveManager archiveManager;
    //
    // /**
    //  * Slave管理
    //  */
    // private SlaveManager slaveManager;
    //
    // /**
    //  * Region管理
    //  */
    // private RegionManager regionManager;
    //
    // /**
    //  * ZK访问
    //  */
    // private ZooKeeperWatcher zooKeeper;
    //
    /**
     * Master端执行服务
     */
    private DBExecutorManager executorService;

    /**
     * 心跳管理服务
     */
    private HeartBeatManager heartbeatManager;

    /**
     * GRPC服务注册器
     */
    private GrpcServiceRegistry registry;

    /**
     * GRPC服务管理
     */
    private GrpcServer grpcServer;

    private DBExecutorManager dbExecutorManager; // 任务调度模块

    /**
     * Master私有构造器
     */
    private DBMaster()
    {
        // 设置线程名称
        super("DBMaster");

        // 加载配置并且获取Master主机名参数
        this.config = MasterConfiguration.getMasterConf();
        final String hostNameConfig = config.get(MasterConstants.MASTER_HOST_NAME);
        final String localHostName = Addressing.getLocalHostName();
        //判断配置和启动机器名不为空
        if (StringUtils.isNotBlank(hostNameConfig) && StringUtils.isNotBlank(localHostName))
        {
            // 判断配置和启动机器是否为同一机器，如果为false，则打出警告信息，抛出参数异常，否则继续执行
            if (!hostNameConfig.equals(localHostName))
            {
                String errorMessage = String
                        .format("Fetal error, wrong configuration hostname=%s "
                                        + "with key 'master.host.name', current hostname=%s; checking it can work",
                                hostNameConfig, localHostName);
                LOG.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
        }
        else
        {
            String errorMessage = String
                    .format("serverName is %s; localHostName is %s; serverName or localHostName is null; please check!",
                            hostNameConfig, localHostName);
            throw new IllegalArgumentException(errorMessage);
        }

        // 初始serverName
        this.serverName = new ServerName(hostNameConfig, Addressing.getHostIpFromInetAddress(hostNameConfig),
                config.getInt(MasterConstants.MASTER_GRPC_SERVER_PORT_CONF, MasterConstants.MASTER_GRPC_SERVER_PORT),
                System.currentTimeMillis());

        // 获取版本信息
        this.version = config.get(MasterConstants.CLUSTER_VERSION);

        // 初始化ZookeeperWatcher
        // this.zooKeeper = new ZooKeeperWatcher(ZookeeperConfiguration.getInstance(null), serverName.getHostIp(), this,
        //         true);
        // LOG.info("Init zookeeper watcher finish.");
    }

    /**
     * <p>
     * Master初始化
     * </p>
     * 完成初始化，Master必须是Active状态 <br>
     * 1. 启动Executor <br>
     * 2. 启动GRPC Server <br>
     * 3. 启动Sub service <br>
     * 4. 启动ZK Tracker <br>
     * 5. Wait For RegionServers To Check-in ① Meta上线 ② RegionServer上线 <br>
     * 6. Set cluster as UP
     *
     * @param status 监控服务
     * @throws InterruptedException 终端异常
     // * @throws KeeperException      ZK异常
     * @throws DBException          DB异常
     */
    private void finishInitialization(MonitoredTask status) throws InterruptedException/*, KeeperException*/, DBException
    {
        // 设置active标记
        this.masterFlag.setActiveMaster(true);
        // 设置active时间
        this.masterActiveTime = System.currentTimeMillis();

        // 启动执行服务
        this.startExecutorService(status);

        //初始化GRPC注册器
        this.registry = GrpcServiceRegistry.getInstance();

        // 启动子服务
        this.startSubService(status);
        // 启动GRPC
        this.startGrpcService(status);
        // 初始化子服务
        this.initSubService(status);

        // // 启动基于ZK的trackers
        // this.initZKTrackers(status);
        // // 启动基于ZK的trackers
        // status.setStatus("Initializing ZK system trackers");

        // Master与RegionServer启动控制
        this.startSlaveAddressTracker(status);

        // //加载元数据
        // this.tableManager.init();

        LOG.info("Initialization successful");
        this.masterFlag.setInitialized(true);
    }

    /**
     * 初始化子服务
     *
     * @param status 服务状态
     */
    private void initSubService(MonitoredTask status)
    {
        LOG.info("start initSubService");
        status.setStatus("start initSubService");
        this.heartbeatManager.init();
        // this.tableManager.init();
        // this.slaveManager.init();
        // this.archiveManager.init();
        // this.regionManager.init();
        LOG.info("start initSubService done");
        status.setStatus("start initSubService done");
    }

    /**
     * <p>
     * 启动GRPC服务
     * </p>
     *
     * @param status 状态监控器
     */
    private void startGrpcService(MonitoredTask status)
    {
        status.setStatus("Start GRPC service...");
        int rpcPort = config
                .getInt(MasterConstants.MASTER_GRPC_SERVER_PORT_CONF, MasterConstants.MASTER_GRPC_SERVER_PORT);
        grpcServer = new GrpcServer(ServerType.MASTER, serverName.getHostName(), rpcPort);
        grpcServer.start();
        status.setStatus("Start GRPC services finish.");
    }

    /**
     * <p>
     * loop睡眠
     * </p>
     */
    private void loop()
    {
        printMasterStatus();
        while (!masterFlag.isStopped())
        {
            stopSleeper.sleep();
        }
    }

    /**
     * <p>
     * 打印Master Status
     * </p>
     */
    private void printMasterStatus()
    {
        StringBuilder masterStatus = new StringBuilder();
        masterStatus.append(MasterConstants.MASTER_LOG_PREFIX).append("Master:").append("\n\t").append("Host Server[")
                .append(serverName.getHostName()).append("]").append("\n\t").append("Version:").append(this.version)
                .append("\n\t").append("startup time: {}").append("\n\t").append("be active time: {}");

        String str = masterStatus.toString();
        String format1 = MasterConstants.STD_DATE_FORMAT.get().format(this.masterStartTime);
        String format2 = MasterConstants.STD_DATE_FORMAT.get().format(this.masterActiveTime);
        LOG.info(str, format1, format2);
    }

    /**
     * 构造Master关系信息
     *
     * @return 关闭信息message
     */
    private String buildMasterCloseInfo()
    {
        StringBuilder closeMessage = new StringBuilder();
        closeMessage.append("Master:").append("\n\t").append("Host Server[").append(serverName.getHostName())
                .append("]").append("\n\t").append("Shutdown Time[")
                .append(MasterConstants.STD_DATE_FORMAT.get().format(System.currentTimeMillis())).append("]")
                .append("\n\t").append("Abort? ").append(this.isAborted()).append("\n\t").append("Stopped? ")
                .append(this.isStopped()).append("\n\t").append("Alive? ").append(this.isAlive()).append("\n\t")
                .append("Interrupted? ").append(this.isInterrupted());
        return closeMessage.toString();
    }

    /**
     * <p>
     * 尝试成为主节点
     * </p>
     *
     * @param startupStatus 状态监控
     * @return 如果成为Active Master，返回{@link true}
     */
    private boolean becomeActiveMaster(MonitoredTask startupStatus)
    {
        // activeMasterManager = new ActiveMasterManager(this.zooKeeper, this);
        // this.zooKeeper.registerListener(activeMasterManager);
        this.masterFlag.setStopped(false);

        // // 竞争Master(堵塞直到成为Master)
        // return activeMasterManager.blockUntilBecomingActiveMaster(startupStatus);
        return true;
    }

    /**
     * <p>
     * Master与RegionServer协调启动
     * </p>
     *
     * @throws InterruptedException 中断异常，除非虚拟机宕机，一般不会出现
     // * @throws DBException          元数据创建或上线异常
     */
    private void startSlaveAddressTracker(MonitoredTask status) throws InterruptedException/*, DBException*/
    {
        status.setStatus("Start region server accord with maste");
        // // 等待RegionServer上报
        // this.slaveManager.waitForSlaves();
        // // 开启RS监控
        // this.masterTrackerManager.startSlaveAddressTracker();
    }

    /**
     * <p>
     * 启动执行服务框架
     * 表元数据执行池
     * 表管理执行池
     * 归档管理执行池
     * Region管理执行池
     * Slave管理执行池
     * </p>
     *
     * @param status 状态监控器
     */
    private void startExecutorService(MonitoredTask status)
    {
        //初始化执行服务
        status.setStatus("Start the DBexecutor service.");
        this.executorService = DBExecutorManager.getInstance();

        LOG.info("Initiated executor services finish");
        //监控信息
        status.setDescription("Initiated executor services finish");
    }

    /**
     * <p>
     * 启动Master端各个子服务
     * </p>
     */
    private void startSubService(MonitoredTask status)
    {
        LOG.info("Start sub services...");

        // 初始化心跳服务
        this.heartbeatManager = HeartBeatManager.getInstance();
        this.heartbeatManager.registryRpcServer(registry);

        // // 初始化表管理
        // this.tableManager = TableManager.getInstance();
        // this.tableManager.registryRpcServer(registry);
        // LOG.info("Init meta manager service finish.");
        //
        // // 初始化Slave管理
        // this.slaveManager = SlaveManager.getInstance();
        // this.slaveManager.registryRpcServer(registry);
        // LOG.info("Init slave manager service finish.");
        //
        // // 初始化归档管理
        // this.archiveManager = ArchiveManager.getInstance();
        // // this.archiveManager.registryRpcServer(registry);
        // LOG.info("Init archive manager service finish.");
        //
        // // 初始化Region状态管理
        // this.regionManager = RegionManager.getInstance();
        // this.regionManager.registryRpcServer(registry);
        // LOG.info("Init region manager service finish.");
        //
        // LOG.info("Start sub services finish.");
        //
        // // 初始化主备切换
        // this.activeMasterManager = new ActiveMasterManager(this.zooKeeper, this);
        // status.setStatus("Start sub service has been  finish for master.");
    }

    // /**
    //  * <p>
    //  * ZK初始化 <br>
    //  * 启动元数据Region追踪，Active Master追踪，集群状态，集群ID等
    //  * </p>
    //  *
    //  * @param status 状态监控
    //  * @throws KeeperException ZK异常信息
    //  */
    // private void initZKTrackers(MonitoredTask status) throws KeeperException
    // {
    //     // zookeeper tracker启动
    //     masterTrackerManager = new MasterTrackerManager(this);
    //     // 设置集群id
    //     status.setStatus("Publishing cluster id in zooKeeper");
    //     masterTrackerManager.getClusterIdTracker()
    //             .setClusterId("dbserver-" + this.serverName.getHostName() + "-" + this.serverName.getStartTime());
    //     // 设置集群状态
    //     boolean wasUp = masterTrackerManager.getClusterStatusTracker().isClusterUp();
    //     if (!wasUp)
    //     {
    //         masterTrackerManager.getClusterStatusTracker().setClusterUp();
    //     }
    //
    //     status.setDescription("Server active/primary master=" + this.serverName + ", sessionid=0x" + Long.toHexString(
    //             this.zooKeeper.getRecoverableZooKeeper()
    //                     .getSessionId()) + ", setting cluster-up flag (Was=" + masterTrackerManager
    //             .getClusterStatusTracker().isClusterUp() + ")");
    //     // 开启Master地址数据追踪器
    //     masterTrackerManager.startMasterAddressTracker();
    //     status.setDescription(String.format("ClusterId= %s startup first? %s",
    //             masterTrackerManager.getClusterIdTracker().getClusterId(), masterFlag.isPremiered()));
    // }

    /**
     * <p>
     * 停止Master GRPC及执行池服务
     * </p>
     */
    private void stopServiceThreads()
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Stopping service threads");
        }
        // // 停止表管理服务
        // if (this.tableManager != null)
        // {
        //     this.tableManager.stop();
        // }
        // // 停止归档管理服务
        // if (this.archiveManager != null)
        // {
        //     this.archiveManager.stop();
        // }
        // // 停止Region管理服务
        // if (this.regionManager != null)
        // {
        //     this.regionManager.stop();
        // }

        // // 停止心跳管理服务
        // if (this.heartbeatManager != null)
        // {
        //     this.heartbeatManager.stop();
        // }
        //
        // // 停止Slave管理服务
        // if (this.slaveManager != null)
        // {
        //     this.slaveManager.stop();
        // }
        //
        // // 关闭GRPC
        // if (this.grpcServer != null)
        // {
        //     this.grpcServer.shutdown();
        //     GrpcClientFactory.close();
        // }
        //
        // // 关闭执行框架
        // if (this.executorService != null)
        // {
        //     this.executorService.shutdown();
        // }
    }

    @Override
    public void run()
    {
        //启动状态监控线程
        MonitoredTask startupStatus = TaskMonitor.get().createStatus("Master startup");
        //设置master启动时间
        this.masterStartTime = System.currentTimeMillis();

        try
        {
            // 阻塞直到成为ActiveMaster
            becomeActiveMaster(startupStatus);

            // 当前集群并不是shutdown的处理(启动、初始化)
            if (!masterFlag.isStopped())
            {
                finishInitialization(startupStatus);
                loop();
            }
        }
        catch (Throwable t)
        {
            abort("Unhandled exception. Starting shutdown.", t);
        }
        finally
        {
            close();
        }
    }

    @Override
    public void abort(String msg, Throwable t)
    {
        // ZK任务超时
        this.masterFlag.setActiveMaster(false);
        if (t != null)
        {
            LOG.error(msg, t);
        }
        else
        {
            LOG.error(msg);
        }
        this.masterFlag.setAborted(true);
        stop("Aborting");
    }

    @Override
    public boolean isAborted()
    {
        return this.masterFlag.isAborted();
    }

    @Override
    public void stop(String why)
    {
        LOG.info(why);
        this.masterFlag.setStopped(true);

        // 唤醒 <code>stopSleeper</code>进行立即停止状态，不再进行睡眠
        stopSleeper.skipSleepCycle();

        // // 备Master需要等待
        // synchronized (activeMasterManager.clusterHasActiveMaster)
        // {
        //     this.activeMasterManager.clusterHasActiveMaster.notifyAll();
        // }
    }

    /**
     * <p>
     * Master关闭 <br>
     * Master关闭分两种情况：命令触发集群关闭和异常中断。<br>
     * Master被迫中断有以下几种场景：<br>
     * 1. Master初始化中元数据Region异常；<br>
     * 2. Master初始化中Zookeeper服务异常；<br>
     * 3. Master子服务启动异常<br>
     * 4. Master服务中元数据Region异常，不能进行访问<br>
     * 关闭时，停止服务，停止执行池和GRPC服务，停止Zookeeper连接，退出虚拟机。
     * </p>
     */
    public void close()
    {
        LOG.info("Master close");

        // 停止永久服务
        LOG.info("Stop master service threads");
        // 停止服务线程
        stopServiceThreads();
        LOG.info("Stop master activeMasterManager");
        // // ZK关闭处理
        // if (this.activeMasterManager != null)
        // {
        //     activeMasterManager.stop();
        // }
        LOG.info(buildMasterCloseInfo());
    }

    @Override
    public void shutdown()
    {
        // DO NOTHING
    }

    @Override
    public boolean isStopped()
    {
        return this.masterFlag.isStopped();
    }

    // @Override
    // public ZooKeeperWatcher getZooKeeper()
    // {
    //     return this.zooKeeper;
    // }

    @Override
    public boolean isActiveMaster()
    {
        return this.masterFlag.isActiveMaster();
    }

    @Override
    public boolean isInitialized()
    {
        return this.masterFlag.isInitialized();
    }

    @Override
    public boolean isStartupSlaveReceptor()
    {
        return masterFlag.isStartupSlaveReceptor();
    }

    @Override
    public BaseConfiguration getConfiguration()
    {
        return this.config;
    }

    // @Override
    // public MasterTrackerManager getMasterTrackerManager()
    // {
    //     return this.masterTrackerManager;
    // }
    //
    // @Override
    // public TableManager getTableManager()
    // {
    //     return this.tableManager;
    // }
    //
    // @Override
    // public ArchiveManager getArchiveManager()
    // {
    //     return this.archiveManager;
    // }
    //
    // @Override
    // public SlaveManager getSlaveManager()
    // {
    //     return this.slaveManager;
    // }
    //
    // @Override
    // public RegionManager getRegionManager()
    // {
    //     return this.regionManager;
    // }

    @Override
    public ServerName getServerName()
    {
        return this.serverName;
    }

    // @Override
    // public ActiveMasterManager getActiveMasterManager()
    // {
    //     return this.activeMasterManager;
    // }
    //
    @Override
    public DBExecutorManager getExecutorService()
    {
        return executorService;
    }

    /**
     * Master启动主函数
     *
     * @param args 参数
     */
    public static void main(String[] args)
    {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        File conFile = new File("conf/log4j2.xml");
        loggerContext.setConfigLocation(conFile.toURI());
        loggerContext.reconfigure();
        // 启动Master服务
        final DBMaster master = DBMaster.getMaster();
        master.start();

        // JVM退出的钩子，执行Master的stop方法，停止各个子服务
        Runtime.getRuntime().addShutdownHook(new Thread("Master ShutdownHook")
        {
            @Override
            public void run()
            {
                try
                {
                    if (!master.isAborted())
                    {
                        master.stop("Close Command Trigger");
                    }
                    Thread.sleep(3000L);
                    LOG.info("Master Exit");
                }
                catch (InterruptedException e)
                {
                    LOG.error("Master Exit error:", e);
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

}
