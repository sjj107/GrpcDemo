package com.fiberhome.dbserver.protocol.registry;

import com.fiberhome.dbserver.protocol.server.GrpcServiceRegistry;

/**
 * 管理模块通用接口定义
 * @author lizhen
 * @update dinne
 * @date 2020/10/21
 * @since 1.0.0
 */
public interface IRegistryService
{
    /**
     * 接口初始化方法，Master启动时会调用该方法初始化各个模块
     */
    void init();

    /**
     * 当前服务提供RPC服务时，请注册对应的服务
     * @param serviceRegistry rpc注册器
     */
    void registryRpcServer(GrpcServiceRegistry serviceRegistry);
    
    /**
     * stop方法
     */
    void stop();
}
