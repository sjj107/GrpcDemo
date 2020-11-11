package com.fiberhome.dbserver.protocol.server;

import static com.fiberhome.dbserver.protocol.common.GrpcConstants.MAX_FLOWWINDOW_SIZE;
import static com.fiberhome.dbserver.protocol.common.GrpcConstants.MAX_MESSAGE_SIZE;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fiberhome.dbserver.common.common.ServerType;
import com.fiberhome.dbserver.common.config.MasterConfiguration;
import com.fiberhome.dbserver.common.config.ServerConfiguration;
import com.fiberhome.dbserver.common.services.Threads;
import com.fiberhome.dbserver.tools.util.Addressing;
import com.fiberhome.dbserver.tools.util.PreconditionVerifier;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

/**
 * grpcServer
 *
 * @author fuyuanyuan, 2020/10/20
 * @since 1.0.0
 */
public class GrpcServer
{
    private static final Logger LOG = LoggerFactory.getLogger(GrpcServer.class);

    /*
     * server主机ip参数
     */
    private String transportHost;

    /*
     * grpc服务注册
     */
    private GrpcServiceRegistry serviceRegistry;

    /*
     * netty server
     */
    private Server rpcServer;

    /*
     * 当前执行grpcServer的name
     */
    private ServerType name;

    /*
     * grpc server 业务线程池中最大线程数
     */
    private int maxThreads;

    /*
     * 注册的服务名称列表
     */
    private Set<String> services = new HashSet<>();

    /*
     * grpc server端业务线程池
     */
    private ThreadPoolExecutor pool;
    /*
    服务端口
     */
    int port;
    /*
    Master配置文件：dbmaster-site.xml
     */
    private MasterConfiguration masterConf;
    /*
     本机处理器数量，用于配置默认线程数
     */
    private int numberOfProcessors;

    /**
     * 构造函数
     *
     * @param type     类型
     * @param port 启动端口号
     * @param hostName 主机名
     */
    public GrpcServer(ServerType type, String hostName, int port)
    {
        this.serviceRegistry = GrpcServiceRegistry.getInstance();
        this.numberOfProcessors = Runtime.getRuntime().availableProcessors();
        this.transportHost = Addressing.getHostIpFromInetAddress(hostName);
        this.port = port;
        PreconditionVerifier.isValidIpAddress(this.transportHost);
        this.name = type;
        switch (type)
        {
            case MASTER:
                masterConf = MasterConfiguration.getMasterConf();
                this.maxThreads = masterConf.getInt("dbserver.master.grpc.max.threads", numberOfProcessors * 2);
                break;
            case SERVER:
                ServerConfiguration serverConf = ServerConfiguration.getServerConf();
                this.maxThreads = serverConf.getInt("dbserver.slave.grpc.max.threads", numberOfProcessors * 2);
                break;
            default:
                LOG.error("build grpcServer type error. type {}", type);
                throw new IllegalArgumentException("this ServerType is not support. type:" + type);


        }
        // 线程池
        this.pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads,
                Threads.getNamedThreadFactory("grpcserver-" + name.toString().toLowerCase() + "-"));
        initServers();
    }

    private void initServers()
    {
        LOG.info("init server host: {} and port:{}", transportHost, port);
        NettyServerBuilder builder = NettyServerBuilder.forAddress(new InetSocketAddress(transportHost, port))
                .flowControlWindow(MAX_FLOWWINDOW_SIZE).maxInboundMessageSize(MAX_MESSAGE_SIZE);
        for (Map.Entry<String, BindableService> bindableServiceEntry : serviceRegistry.getRpcService().entrySet())
        {
            builder.addService(bindableServiceEntry.getValue());
            services.add(bindableServiceEntry.getKey());
        }
        builder.executor(this.pool);
        rpcServer = builder.build();
    }

    /**
     * 启动server
     */
    public void start()
    {
        try
        {
            rpcServer.start();
        }
        catch (IOException e)
        {
            LOG.error("grpc server started failed", e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * 停止server
     */
    public void shutdown()
    {
        shutdownServers();
    }

    private void shutdownServers()
    {
        if (rpcServer != null && !rpcServer.isShutdown())
        {
            rpcServer.shutdown();
        }
        if (this.pool != null && !this.pool.isShutdown())
        {
            this.pool.shutdownNow();
        }
    }
}
