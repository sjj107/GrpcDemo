package com.fiberhome.dbserver.slave;

import java.io.File;

import org.apache.commons.lang.StringUtils;
// import org.apache.zookeeper.KeeperException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fiberhome.dbserver.common.common.ServerType;
import com.fiberhome.dbserver.common.config.ServerConfiguration;
// import com.fiberhome.dbserver.common.config.ZookeeperConfiguration;
import com.fiberhome.dbserver.common.elements.server.ServerName;
import com.fiberhome.dbserver.common.executor.DBExecutorManager;
import com.fiberhome.dbserver.common.services.HasThread;
import com.fiberhome.dbserver.common.services.MonitoredTask;
import com.fiberhome.dbserver.common.services.Sleeper;
import com.fiberhome.dbserver.common.services.TaskMonitor;
// import com.fiberhome.dbserver.common.zookeeper.ZooKeeperWatcher;
import com.fiberhome.dbserver.protocol.client.GrpcClientFactory;
import com.fiberhome.dbserver.protocol.server.GrpcServer;
import com.fiberhome.dbserver.protocol.server.GrpcServiceRegistry;
// import com.fiberhome.dbserver.slave.blockletdata.BlockletDataMangerService;
// import com.fiberhome.dbserver.slave.blockletindex.BlockletIndexManagerService;
// import com.fiberhome.dbserver.slave.compact.CompactService;
// import com.fiberhome.dbserver.slave.constants.SlaveConstants;
// import com.fiberhome.dbserver.slave.coordinate.CoordinateService;
import com.fiberhome.dbserver.slave.constants.SlaveConstants;
import com.fiberhome.dbserver.slave.heartbeat.HeartBeatService;
// import com.fiberhome.dbserver.slave.region.RegionManagerService;
// import com.fiberhome.dbserver.slave.zookeeper.ServerTrackerManager;
import com.fiberhome.dbserver.tools.conf.BaseConfiguration;
import com.fiberhome.dbserver.tools.util.Addressing;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * DBSlave服务
 *
 * @author xiajunsheng, 2020/10/19
 * @since 1.0.0
 */
public class DBSlave extends HasThread implements DBSlaveServices, SignalHandler
{
    static
    {
        System.setProperty("SERVICE_NAME", "slave");
    }

    /**
     * 日志信息
     */
    private static final Logger LOG = LoggerFactory.getLogger(DBSlave.class);

    /**
     * 唯一实例
     *
     * @return Server服务
     */
    public static DBSlave getDBServer()
    {
        return Holder.dbSlave;
    }

    static class Holder
    {

        private Holder()
        {

        }

        private static DBSlave dbSlave = new DBSlave();
    }

    /**
     * 服务状态标识
     */
    private final ServerFlag flag = new ServerFlag();

    /**
     * 进程睡眠管理
     */
    private final Sleeper stopSleeper = new Sleeper(60000L, this);

    // /**
    //  * 块数据管理
    //  */
    // private BlockletDataMangerService blockletDataMangerService;
    //
    // /**
    //  * 块索引管理服务
    //  */
    // private BlockletIndexManagerService blockletIndexManagerService;
    //
    // /**
    //  * 协调服务
    //  */
    // private CoordinateService coordinateService;
    /**
     * 心跳服务
     */
    private HeartBeatService heartBeatService;
    // /**
    //  * Region管理服务
    //  */
    // private RegionManagerService regionManagerService;
    //
    // /**
    //  * Server端ZK管理服务
    //  */
    // private ServerTrackerManager serverTrackerManager;
    //
    // /**
    //  * Server端合併服務
    //  */
    // private CompactService compactService;

    /**
     * 配置信息
     */
    private ServerConfiguration config = null;
    // /**
    //  * ZK监听器
    //  */
    // private ZooKeeperWatcher zooKeeper;
    /**
     * 服务启动时间
     */
    private long startTime;
    /**
     * 服务启动Server
     */
    private ServerName serverName;

    /**
     * GRPC服务注册器
     */
    private GrpcServiceRegistry registry;

    /**
     * GRPC服务管理
     */
    private GrpcServer grpcServer;

    /**
     * Slave端执行服务
     */
    private DBExecutorManager executorService;

    /**
     * 私有构造函数
     */
    private DBSlave()
    {
        super("DBServer");
        this.config = ServerConfiguration.getServerConf();
        // 服务器参数
        final String hostNameConfig = config.get(SlaveConstants.SLAVE_HOST_NAME);
        // 判断配置和启动机器名不为空
        if (!StringUtils.isNotBlank(hostNameConfig))
        {
            String errorMessage = String
                    .format("serverName is %s; localHostName is %s; serverName or localHostName is null; please check!",
                            hostNameConfig, hostNameConfig);
            throw new IllegalArgumentException(errorMessage);
        }
        this.serverName = new ServerName(hostNameConfig, Addressing.getHostIpFromInetAddress(hostNameConfig),
                config.getInt(SlaveConstants.SLAVE_GRPC_SERVER_PORT_CONF, 10060), System.currentTimeMillis());
        // this.zooKeeper = new ZooKeeperWatcher(ZookeeperConfiguration.getInstance(null), serverName.getHostName(), this,
        //         false);
    }

    /**
     * 完成初始化 <br>
     * 1. start commonService 2. start subService
     */
    private void finishInitialization(MonitoredTask status)
    {
        // 开启线程服务
        LOG.info("Initializing DBslave sub service...");

        // 启动执行服务
        this.startExecutorService(status);

        // 初始化Slave各个子服务
        this.initSubServices(status);

        // 启动Slave各个子服务
        this.startSubServices(status);

        // 启动GRPC
        this.startGrpcService(status);

        // // 写入ServerName到zk中
        // try
        // {
        //     this.serverTrackerManager.setServerName(serverName);
        // }
        // catch (KeeperException e)
        // {
        //     abort("Set serverName into zookeeper /rs failed, abort DBSlave...", e);
        // }

        LOG.info("Initialization successful");
        this.flag.setInitialized(true);
        status.markComplete("Initialization successful");
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
        int serverPort = config.getInt(SlaveConstants.SLAVE_GRPC_SERVER_PORT_CONF, 10060);
        this.grpcServer = new GrpcServer(ServerType.SERVER, serverName.getHostName(), serverPort);
        this.grpcServer.start();
        status.setStatus("Start GRPC services finish.");
    }

    /**
     * 启动公共服务
     */
    void startExecutorService(MonitoredTask task)
    {

        // 初始化执行服务
        task.setStatus("Start the DBexecutor service.");
        this.executorService = DBExecutorManager.getInstance();
        LOG.info("start execute service finish.");
        // 监控信息
        task.setDescription("start execute service finish.");
    }

    /**
     * 初始化Server各个子服务
     */
    private void initSubServices(MonitoredTask task)
    {
        // // 实例化各个子服务
        // this.serverTrackerManager = new ServerTrackerManager(this);

        this.registry = GrpcServiceRegistry.getInstance();

        // this.blockletDataMangerService = BlockletDataMangerService.getInstance();
        // LOG.info("Init  blocketDataManger service finish.");
        //
        // this.blockletIndexManagerService = BlockletIndexManagerService.getInstance();
        // LOG.info("Init  blockletIndexManagerService service finish.");
        //
        // this.coordinateService = CoordinateService.getInstance();
        // LOG.info("Init  coordinatemanager service finish.");

        this.heartBeatService = HeartBeatService.getInstance();
        LOG.info("Init heartBeatmanager service finish.");
        //
        // this.compactService = CompactService.getInstance();
        // LOG.info("Init compact service finish.");
        //
        // this.regionManagerService = RegionManagerService.getInstance();
        // LOG.info("Init regionmanager service finish.");

        task.setStatus("Init sub service has been  finish for server.");
    }

    /**
     * 启动子服务
     */
    private void startSubServices(MonitoredTask task)
    {

        // this.blockletDataMangerService.init();
        // this.blockletDataMangerService.registryRpcServer(this.registry);
        // LOG.info("Init blocketData manager service finish.");
        //
        // this.coordinateService.init();
        // this.coordinateService.registryRpcServer(this.registry);
        // LOG.info("Init coordinate service finish.");
        //
        // this.blockletIndexManagerService.init();
        // this.blockletIndexManagerService.registryRpcServer(this.registry);
        // LOG.info("Init blockletIndex manager service finish.");
        //
        // this.regionManagerService.init();
        // this.regionManagerService.registryRpcServer(this.registry);
        // LOG.info("Init region manager service finish.");

        task.setStatus("Start sub service has been  finish for server.");
    }

    /**
     * 关闭服务线程
     */
    private void stopServiceThreads(MonitoredTask task)
    {

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Stopping service threads");
        }
        // // 停止表管理服务
        // if (this.blockletDataMangerService != null)
        // {
        //     this.blockletDataMangerService.stop();
        // }
        // // 停止归档管理服务
        // if (this.blockletIndexManagerService != null)
        // {
        //     this.blockletIndexManagerService.stop();
        // }
        // // 停止Region管理服务
        // if (this.compactService != null)
        // {
        //     this.compactService.stop();
        // }

        // 停止心跳管理服务
        if (this.heartBeatService != null)
        {
            this.heartBeatService.stop();
        }

        // // 停止Slave管理服务
        // if (this.coordinateService != null)
        // {
        //     this.coordinateService.stop();
        // }
        //
        // // 停止Slave管理服务
        // if (this.regionManagerService != null)
        // {
        //     this.regionManagerService.stop();
        // }

        // 关闭GRPC
        if (this.grpcServer != null)
        {

            this.grpcServer.shutdown();
            GrpcClientFactory.close();
        }

        // 关闭执行框架
        if (this.executorService != null)
        {
            this.executorService.shutdown();
        }
        this.flag.setExit(true);
        task.setStatus("stop executor service & grpc service has been finish.");
    }

    /**
     * loop睡眠
     */
    private void loop()
    {
        printServerStatus();
        while (!flag.isStopped())
        {
            stopSleeper.sleep();
        }
    }

    /**
     * 打印Server Status
     */
    private void printServerStatus()
    {
        StringBuilder status = new StringBuilder();
        status.append(SlaveConstants.SLAVE_LOG_PREFIX).append("Slave:").append("\n\t").append("Host Server[")
                .append(serverName.getHostName()).append("]").append("\n\t").append("startup time: {}");
        String str = status.toString();
        String format = SlaveConstants.STD_DATE_FORMAT.get().format(this.startTime);
        LOG.info(str, format);
    }

    /**
     * 是否正常退出
     *
     * @throws InterruptedException 异常
     */
    private void isNormalExit() throws InterruptedException
    {
        long exitTimeout = config.getLong("slave.exit.timeout.max", 60000);
        // 退出允许等待的超时时间60 000ms
        while (exitTimeout > 0)
        {
            if (!this.flag.isExit())
            {
                Thread.sleep(3000L);
                exitTimeout = exitTimeout - 3000;
                LOG.warn("This Slave background flush data, please wait...");
            }
            else
            {
                break;
            }
        }
        // 超时后还未退出
        if (!this.flag.isExit())
        {
            LOG.error("The Slave is not normal exit of time out [{}] ms, maybe lost flush data.", exitTimeout);
        }
    }

    /**
     * 构造打印关系信息
     *
     * @return message
     */
    private String printCloseMessage()
    {
        // close message
        StringBuilder closeMessage = new StringBuilder();
        closeMessage.append("DBSlave:").append("\n\t").append("Host Server[").append(serverName).append("]")
                .append("\n\t").append("Shutdown Time[")
                .append(SlaveConstants.STD_DATE_FORMAT.get().format(System.currentTimeMillis())).append("]")
                .append("\n\t").append("Abort? ").append(this.isAborted()).append("\n\t").append("Stopped? ")
                .append(this.isStopped()).append("\n\t").append("Alive? ").append(this.isAlive()).append("\n\t")
                .append("Interrupted? ").append(this.isInterrupted());
        return closeMessage.toString();
    }

    @Override
    public void stop(String why)
    {
        LOG.info(why);
        this.flag.setStopped(true);
        stopSleeper.skipSleepCycle();
    }

    @Override
    public boolean isStopped()
    {
        return this.flag.isStopped();
    }

    @Override
    public void abort(String msg, Throwable e)
    {
        if (e != null)
        {
            LOG.error(msg, e);
        }
        else
        {
            LOG.error(msg);
        }
        this.flag.setAborted(true);
        stop("Aborting");
    }

    @Override
    public boolean isAborted()
    {
        return false;
    }

    // @Override
    // public RegionManagerService getRegionManagerService()
    // {
    //     return this.regionManagerService;
    // }

    @Override
    public HeartBeatService getHeartBeatService()
    {
        return this.heartBeatService;
    }

    // @Override
    // public CoordinateService getCoordinateService()
    // {
    //     return this.coordinateService;
    // }
    //
    // @Override
    // public ServerTrackerManager getServerTrackerManager()
    // {
    //     return this.serverTrackerManager;
    // }
    //
    // @Override
    // public CompactService getCompactService()
    // {
    //     return null;
    // }

    @Override
    public BaseConfiguration getConfiguration()
    {
        return this.config;
    }

    // @Override
    // public ZooKeeperWatcher getZooKeeper()
    // {
    //     return zooKeeper;
    // }

    @Override
    public ServerName getServerName()
    {
        return this.serverName;
    }

    // @Override
    // public BlockletIndexManagerService getBlockletIndexLoadService()
    // {
    //     return this.blockletIndexManagerService;
    // }
    //
    // @Override
    // public BlockletDataMangerService getBlockletDataMangerServices()
    // {
    //     return this.blockletDataMangerService;
    // }

    @Override
    public void run()
    {
        MonitoredTask startupStatus = TaskMonitor.get().createStatus("DBSlave startup");
        this.startTime = System.currentTimeMillis();
        try
        {
            // 服务启动、初始化
            if (!flag.isStopped())
            {
                finishInitialization(startupStatus);
                loop();
            }
        }
        catch (Exception t)
        {
            abort("Unhandled exception. Starting shutdown.", t);
        }
        finally
        {
            close();
        }

    }

    /**
     * 关闭
     */
    public void close()
    {
        MonitoredTask closeStatus = TaskMonitor.get().createStatus("DBSlave close");
        // try
        // {
        //     this.zooKeeper.close();
        //     stopServiceThreads(closeStatus);
        // }
        // catch (Exception e)
        // {
        //     LOG.warn("stop service is faild.", e);
        // }

        // 打印关闭日志
        closeStatus.setDescription(printCloseMessage());
    }

    /**
     * 中断、退出信号处理，替代addShutdownHook写法
     */
    @Override
    public void handle(Signal arg0)
    {
        try
        {
            // 修改标识符stopped 后台线程开始停止
            stop("Close Command Trigger");
            isNormalExit();
            LOG.info("DBSlave Exit");
        }
        catch (InterruptedException e)
        {
            LOG.error("DBSlave service was interrupted.");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Server启动主函数
     *
     * @param args 参数
     */
    public static void main(String[] args)
    {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        File conFile = new File("conf/log4j2.xml");
        loggerContext.setConfigLocation(conFile.toURI());
        loggerContext.reconfigure();
        final DBSlave dbSlave = DBSlave.getDBServer();
        dbSlave.start();
        Signal.handle(new Signal("TERM"), dbSlave);
        Signal.handle(new Signal("INT"), dbSlave);
        Signal.handle(new Signal("ABRT"), dbSlave);
    }

}
