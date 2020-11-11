package com.fiberhome.dbserver.master.slave.heartbeatserver;

import com.fiberhome.dbserver.protocol.common.GrpcConstants;
import com.fiberhome.dbserver.protocol.registry.IRegistryService;
import com.fiberhome.dbserver.protocol.server.GrpcServiceRegistry;

/**
 * 心跳管理模块
 * 心跳管理模块负责处理心跳信息，并通知心跳监听
 *
 * @author lizhen, 2020/10/16
 * @since 1.0.0
 */
public class HeartBeatManager implements IRegistryService
{
    private static class Holder
    {
        private static final HeartBeatManager SINGLETON = new HeartBeatManager();
    }

    public static HeartBeatManager getInstance()
    {
        return Holder.SINGLETON;
    }

    private HeartBeatRpcProcessor heartBeatRpcProcessor;

    private HeartBeatManager()
    {
        heartBeatRpcProcessor = new HeartBeatRpcProcessor(this);
    }

    /**
     * 接口初始化方法，Master启动时会调用该方法初始化各个模块
     */
    @Override
    public void init()
    {
        //TODO
    }

    /**
     * 当前服务提供Rpc服务时，请注册对应的服务
     *
     * @param serviceRegistry rpc注册器
     */
    @Override
    public void registryRpcServer(GrpcServiceRegistry serviceRegistry)
    {
        serviceRegistry.registryRpcService(GrpcConstants.HEARTBEAT_MANAGER_SERVICE, heartBeatRpcProcessor);
    }

    /**
     * 处理心跳信息
     *
     * @param heartBeatInfo 心跳信息
     */
    public void receiveHeartbeat(HeartBeatInfo heartBeatInfo)
    {
        // todo
    }

    /**
     * 添加心跳监听
     *
     * @param listener 监听器
     */
    public void addHeartbeatListener(HeartbeatListener listener)
    {
        // todo
    }

    @Override
    public void stop()
    {
        // TODO Auto-generated method stub

    }

}
