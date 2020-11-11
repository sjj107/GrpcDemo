package com.fiberhome.dbserver.slave.heartbeat;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fiberhome.dbserver.common.config.ClientCongfiguration;
import com.fiberhome.dbserver.common.config.MasterConfiguration;
import com.fiberhome.dbserver.common.config.ServerConfiguration;
// import com.fiberhome.dbserver.common.elements.region.RegionLoad;
import com.fiberhome.dbserver.common.elements.server.ServerName;
import com.fiberhome.dbserver.common.executor.DBExecutorManager;
import com.fiberhome.dbserver.common.executor.payload.CyclePayload;
// import com.fiberhome.dbserver.common.metrics.JvmResource;
// import com.fiberhome.dbserver.common.metrics.MemoryResource;
import com.fiberhome.dbserver.common.transport.protobuf.HeartBeatMangerServiceGrpc;
import com.fiberhome.dbserver.common.transport.protobuf.MasterServerProtos;
import com.fiberhome.dbserver.common.util.ProtoConvertUtil;
// import com.fiberhome.dbserver.common.zookeeper.MasterAddressTracker;
import com.fiberhome.dbserver.protocol.client.GrpcClientFactory;
import com.fiberhome.dbserver.protocol.common.GrpcConstants;
import com.fiberhome.dbserver.slave.DBSlave;
// import com.fiberhome.dbserver.slave.resource.ServerResourceCache;
import com.fiberhome.dbserver.tools.util.TimeValue;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;


/**
 * 负责周期性的和DBMaster通信，上报该Slave存活状态，并且在心跳中包含其他的负载信息，包括Region状态、资源情况和监控信息
 *
 * @author xiajunsheng, 2020/10/19
 * @since 1.0.0
 */
public class HeartBeatService extends CyclePayload
{
    private static final Logger LOG = LoggerFactory.getLogger(HeartBeatService.class);

    // HeartBeatService 服务的单体类
    private static HeartBeatService instance = null;

    /**
     * 心跳端口号
     */
    private int port = 10050;

    /**
     * Server配置信息
     */
    private ServerConfiguration conf;

    /**
     * 集群状态
     */
    private boolean clusterState = true;

    /**
     * master是否重启标识
     */
    private boolean masterRestart = false;

    /**
     * Master重启超时时间
     */
    private TimeValue masterRestartTimeOut;

    /**
     * 心跳时间间隔
     */
    private static TimeValue heartBeatInterval;

    /**
     * 等待Master重连或者汇报Region状态的最大重试次数
     */
    private int maxRetryNumber;

    /**
     * 等待Master重启时当前已经重试的次数
     */
    private int retryNumber = 0;

    // /**
    //  * master地址跟踪器
    //  */
    // private MasterAddressTracker masterAddressTracker;

    /**
     * master节点ServerName
     */
    private ServerName master;

    /**
     * master阻塞存根
     */
    private HeartBeatMangerServiceGrpc.HeartBeatMangerServiceFutureStub heartBeatStub;

    /**
     * Server节点ServerName
     */
    private ServerName serverName;

    /**
     * Server缓存管理
     *
     */
    // private ServerResourceCache resourceCacheMgr;

    /**
     * 私有构造函数
     *
     * @param fixedRate   是否按固定频率执行，否则按固定延时
     * @param period      触发周期
     * @param delayedTime 初次执行延时时间
     */
    private HeartBeatService(boolean fixedRate, long period, long delayedTime)
    {
        super(fixedRate, period, delayedTime);
        conf = ServerConfiguration.getServerConf();

        //心跳端口号 默认10051
        this.port = conf.getInt("dbserver.master.grpc.server.port", 10051);

        // 心跳时间间隔 默认3s
        this.heartBeatInterval = new TimeValue(conf.getLong("server.heartbeat.interval", 300000L),
                TimeUnit.MILLISECONDS);

        // master重启超时时间 默认600s
        this.masterRestartTimeOut = new TimeValue(conf.getLong("server.wait.master.restart.timeout", 600000L),
                TimeUnit.MILLISECONDS);

        // 最大重试次数 600s/3s = 200次
        this.maxRetryNumber = (int) (this.masterRestartTimeOut.getMillis() / this.heartBeatInterval.getMillis());

        // Server的ServerName对象
        serverName = DBSlave.getDBServer().getServerName();

        // masterAddressTracker = DBSlave.getDBServer().getServerTrackerManager().getMasterAddressTracker();

        // 从zk中获取Master地址
        // waitingClusterUp();
        //todo sjj
        master = new ServerName("SW", "192.168.85.1", 10051, System.currentTimeMillis());

        // 初始化缓存
        // resourceCacheMgr = ServerResourceCache.getInstance();

        int heartPort = ClientCongfiguration.getClientConfiguration().getMasterServiceRpcPort();
        this.heartBeatStub = (HeartBeatMangerServiceGrpc.HeartBeatMangerServiceFutureStub) GrpcClientFactory
                .getGrpcClient(master.getHostName(), heartPort)
                .getServiceFutureStub(GrpcConstants.HEARTBEAT_MANAGER_SERVICE);

        // 开始执行心跳线程
        LOG.info("Start HeartBeat service, interval is " + heartBeatInterval.getSeconds() + " s.");

        startHeartBeat();
    }


    /**
     * 单体类
     *
     * @return HeartBeatService
     */

    public static HeartBeatService getInstance()
    {
        if (instance != null)
        {
            return instance;
        }
        synchronized (HeartBeatService.class)
        {
            if (instance != null)
            {
                return instance;
            }
            instance = new HeartBeatService(false, 3000, 0);
            return instance;
        }
    }

    // /**
    //  * 等待集群第一次启动，从ZK中获取Master地址
    //  */
    // private void waitingClusterUp()
    // {
    //     // 第一次启动，如果30s依然没有获取到Master地址，则抛出异常
    //     TimeValue maxClusterUpTime = new TimeValue(conf.getLong("server.wait.cluster.timeout", 30000L),
    //             TimeUnit.MILLISECONDS);
    //
    //     try
    //     {
    //         for (int i = 0; i < maxClusterUpTime.getSeconds(); i++)
    //         {
    //             master = masterAddressTracker.getMasterAddress(true);
    //             if (null == master)
    //             {
    //                 Thread.sleep(1000L);
    //             }
    //             else
    //             {
    //                 break;
    //             }
    //         }
    //     }
    //     catch (InterruptedException e)
    //     {
    //         LOG.warn("Get master address from zk found error.", e);
    //         Thread.currentThread().interrupt();
    //     }
    //
    //     // 30s master地址还未写到zookeeper中，则认为集群没有启动
    //     if (null == master)
    //     {
    //         LOG.error("Heartbeat can not get master address from zookeeper.");
    //         throw new RuntimeException("Heartbeat can not get master address from zookeeper.");
    //     }
    //     else
    //     {
    //         LOG.info("Get Master address : {} , set cluster state = true", master);
    //         clusterState = true;
    //     }
    // }

    /**
     * 启动心跳服务
     */
    public void startHeartBeat()
    {
        DBExecutorManager.getInstance().submitCycleTask(this);
    }


    /**
     * 发送心跳
     */
    private void sendHeartBeat()
    {
        // 向Master发送心跳
        ListenableFuture<MasterServerProtos.HeartBeatResponse> future = heartBeatStub
                .reportHeartBeat(buildHeartBeatRequest());
        LOG.info("Sent heartbeat to master, wait for response..."); // todo 改回debug
        // 获取心跳响应，如果3s内没有获取到结果，则打印WARN日志
        try
        {
            MasterServerProtos.HeartBeatResponse heartBeatResponse = future.get(3000L, TimeUnit.MILLISECONDS);
            LOG.info("heartBeatResponse is [{}]", heartBeatResponse);
        }
        catch (InterruptedException | ExecutionException | TimeoutException e)
        {
            LOG.error("error", e);
            // // zk中存在master地址,发送失败,网络问题或者zk失联
            // ServerName newMaster = masterAddressTracker.getMasterAddress(true);
            //
            // if (null != newMaster)
            // {
            //     LOG.warn("send heartbeat failed, retry report heartbeat, causing:", e);
            //     // 判断从ZK中获取的Master地址与之前的地址是否一致，如果不一致，可能存在master切换
            //     if (!ServerName.isSameHostnameAndPort(master, newMaster) || (master.getStartTime() != newMaster
            //             .getStartTime()))
            //     {
            //         LOG.warn("master has changed, old master is {}, new master is {}.", master, newMaster);
            //         master = newMaster;
            //         // 重新构造masterStub
            //         int heartPort = ClientCongfiguration.getClientConfiguration()
            //                 .getMasterServiceRpcPort();
            //         this.heartBeatStub = (HeartBeatMangerServiceGrpc.HeartBeatMangerServiceFutureStub) GrpcClientFactory
            //                 .getGrpcClient(master.getHostName(), heartPort)
            //                 .getServiceFutureStub(GrpcConstants.HEARTBEAT_MANAGER_SERVICE);
            //     }
            // }
            // else
            // {
            //     LOG.warn("can not get master from zookeeper, master has exit.");
            //     clusterState = false;
            // }
        }
    }

    // /**
    //  * <p>
    //  * master重启处理
    //  * </p>
    //  */
    // private void handleMasterRestart() throws InterruptedException
    // {
    //     LOG.info("Master has restart.");
    //
    //     masterRestart = false;
    //
    //     sendHeartBeat();
    //
    //     // master重启后，由Server重新汇报当前Server缓存的Region状态
    //     // reportRegionStates();
    // }


    // /**
    //  * 等待master重启 当获取不到Master地址时，阻塞等待直到获取新的Master地址，如果超过超时时间，则抛出Timeout异常
    //  *
    //  * @throws TimeoutException 等待集群重启超时
    //  */
    // private void waitingMasterRestart() throws InterruptedException, TimeoutException
    // {
    //     // 距离Master重启超时的剩余时间 - 600s
    //     long remainTime = masterRestartTimeOut.getSeconds() - retryNumber * heartBeatInterval.getSeconds();
    //
    //     ServerName newMaster = null;
    //     while (remainTime > 0)
    //     {
    //         // 日志打印时间间隔
    //         int printInterval = 10;
    //
    //         // 尝试从ZK中获取Master地址，如果获取不到则进行等待
    //         newMaster = masterAddressTracker.getMasterAddress(true);
    //         if (null != newMaster)
    //         {
    //             // 判断从ZK中获取的Master地址与之前的地址是否一致，如果不一致，则使用新地址
    //             if (!ServerName.isSameHostnameAndPort(master, newMaster) || (master.getStartTime() != newMaster
    //                     .getStartTime()))
    //             {
    //                 LOG.warn("master has changed, old master is {}, new master is {}.", master, newMaster);
    //                 master = newMaster;
    //             }
    //             masterRestart = true;
    //             clusterState = true;
    //             // 构建到新地址的 Grpc client
    //             int heartPort = ClientCongfiguration.getClientConfiguration()
    //                     .getMasterServiceRpcPort();
    //             this.heartBeatStub = (HeartBeatMangerServiceGrpc.HeartBeatMangerServiceFutureStub) GrpcClientFactory
    //                     .getGrpcClient(master.getHostName(), heartPort)
    //                     .getServiceFutureStub(GrpcConstants.HEARTBEAT_MANAGER_SERVICE);;
    //             break;
    //         }
    //         else
    //         {
    //             retryNumber++;
    //             remainTime = remainTime - heartBeatInterval.getSeconds();
    //             Thread.sleep(heartBeatInterval.getMillis());
    //             if (retryNumber % printInterval == 0)
    //             {
    //                 LOG.warn("master has exit, waiting master fail over or restart for {}s, the rest time is {}s.",
    //                         masterRestartTimeOut.getSeconds(), remainTime);
    //             }
    //         }
    //     }
    //
    //     if (null == newMaster)
    //     {
    //         throw new TimeoutException("Waiting master restart timeout, cannot get master address from zookeeper.");
    //     }
    //     else
    //     {
    //         LOG.info("Get master address: {} , start send heartbeat", newMaster);
    //         // 获取到Master地址后，向Master发送一次心跳
    //         sendHeartBeat();
    //     }
    //
    // }

    /**
     * <p>
     * 构建心跳请求
     * </p>
     */
    private MasterServerProtos.HeartBeatRequest buildHeartBeatRequest()
    {
        // JvmResource jvmResource = null;
        // MemoryResource memoryResource = null;
        // List<RegionLoad> regionLoadList = Lists.newArrayList();
        //
        // // 从cache中获取对应的资源信息
        // try
        // {
        //     jvmResource = resourceCacheMgr.getJvmCache().get(serverName);
        //     memoryResource = resourceCacheMgr.getMemroyCache().get(serverName);
        //     regionLoadList = resourceCacheMgr.getRegionCache().get(serverName);
        // }
        // catch (ExecutionException e)
        // {
        //     LOG.warn("Get server resource form cache error. " + e);
        // }

        // 构建ReportHeartBeatRequest
        MasterServerProtos.HeartBeatRequest.Builder request = MasterServerProtos.HeartBeatRequest.newBuilder();


        // request.setJvmResource(ProtoConvertUtil.toJvmResourceProto(jvmResource));
        // request.setMemResource(ProtoConvertUtil.toMemoryResourceProto(memoryResource));
        // request.addAllRegionLoads(ProtoConvertUtil.toRegionLoadProtoList(regionLoadList));
        request.setSlaveName(ProtoConvertUtil.toServerNameProto(serverName));
        LOG.debug("Finish build HeartBeatRequest");
        String str = request.toString();
        LOG.debug("BaseRequest is : {}", str);
        return request.build();
    }

    @Override
    protected void process() throws Exception
    {
        try
        {
            // 集群状态正常
            if (clusterState)
            {
                // master没重启
                if (!masterRestart)
                {
                    // 常规处理
                    sendHeartBeat();
                }
                // else
                // {
                //     // master重启处理
                //     handleMasterRestart();
                // }
            }
            else
            {
                // 等待Master重新启动
                LOG.error("todo sjj");
                // waitingMasterRestart();
            }
        }
        catch (Exception e)
        {

        }
        // catch (InterruptedException e)
        // {
        //     LOG.warn("Heart beat thread found InterruptedException. " + e);
        //     Thread.currentThread().interrupt();
        // }
        // catch (TimeoutException e)
        // {
        //     LOG.warn("Because of TimeoutException, stop HeartBeat thread ");
        //
        //     DBSlave.getDBServer().stop("Heart beat thread found TimeoutException.");
        // }
    }

    /**
     * stop方法
     */
    public void stop()
    {
        //todo
    }

}
