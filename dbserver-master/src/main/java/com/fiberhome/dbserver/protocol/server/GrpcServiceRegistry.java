package com.fiberhome.dbserver.protocol.server;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.BindableService;

/**
 * grpc  注册服务
 *
 * @author fuyuanyuan, 2020/10/20
 * @since 1.0.0
 */
public class GrpcServiceRegistry
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServiceRegistry.class);

    /**
     * GRPC service集合:服务名---服务实现
     */
    private final Map<String, BindableService> servicesMap;

    private static class Holder
    {
        private static final GrpcServiceRegistry REGISTRY = new GrpcServiceRegistry();
    }

    public static GrpcServiceRegistry getInstance()
    {
        return Holder.REGISTRY;
    }

    private GrpcServiceRegistry()
    {
        this.servicesMap = new HashMap<>();
    }


    /**
     * 获取注册的gRPC server
     *
     * @return RPC服务
     */
    public Map<String, BindableService> getRpcService()
    {
        return new HashMap<>(servicesMap);
    }

    /**
     * 注册rpc服务
     *
     * @param serviceName 服务名
     * @param service     服务方法
     */
    public synchronized void registryRpcService(String serviceName, BindableService service)
    {
        LOGGER.info("{} try registry.", serviceName);
        if (servicesMap.containsKey(serviceName))
        {
            throw new IllegalArgumentException(serviceName + " has registried");
        }
        else
        {
            servicesMap.put(serviceName, service);
        }
    }
}
