package com.fiberhome.dbserver.protocol.client;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.fiberhome.dbserver.tools.util.MathUtils;
import com.google.common.collect.Maps;

/**
 * 用于获取GrpcClient
 *
 * @author fuyuanyuan, 2020/10/20
 * @since 1.0.0
 */
public class GrpcClientFactory
{
    /*
     * 客户端缓存数
     */
    private static final int CLIENT_SIZE = 10;

    /*
     * Server客户端缓存, 主机名及主机对应的客户端
     */
    private static Map<String, GrpcClient[]> serverGrpcClientCache = Maps.newHashMap();

    /*
     * 计数器 key:主机名称 value:计数器
     */
    private static Map<String, AtomicInteger> serverCounters = Maps.newHashMap();

    /*
     * 获取Server客户端锁
     */
    private static final Object SERVERLOCK = new Object();

    /**
     * 获取访问masterGrpc的客户端
     *
     * @param hostName master地址
     * @param port     master端口
     * @return 客户端
     */
    public static GrpcClient getGrpcClient(String hostName, int port)
    {
        if (!serverGrpcClientCache.containsKey(hostName))
        {
            synchronized (SERVERLOCK)
            {
                if (!serverGrpcClientCache.containsKey(hostName))
                {
                    GrpcClient[] clients = new GrpcClient[CLIENT_SIZE];
                    for (int i = 0; i < CLIENT_SIZE; i++)
                    {
                        // TODO 线程池信息和缓存信息需要从客户端读取
                        clients[i] = new GrpcClient(hostName, port, 4, Executors.newCachedThreadPool());
                    }
                    serverGrpcClientCache.put(hostName, clients);
                    serverCounters.put(hostName, new AtomicInteger());
                }
            }
        }
        int random = MathUtils.mod(serverCounters.get(hostName).incrementAndGet(), CLIENT_SIZE);
        return serverGrpcClientCache.get(hostName)[random];
    }

    /**
     * 关闭客户端
     */
    public static void close()
    {
        // 关闭Grpc链接
        if (!serverGrpcClientCache.isEmpty())
        {
            for (GrpcClient[] grpcClients : serverGrpcClientCache.values())
            {
                for (GrpcClient client : grpcClients)
                {
                    client.shutdown();
                }
            }
            serverGrpcClientCache.clear();
        }
    }
}
