package com.fiberhome.dbserver.protocol.client;

import static com.fiberhome.dbserver.protocol.common.GrpcConstants.MAX_MESSAGE_SIZE;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fiberhome.dbserver.tools.util.Addressing;
import com.fiberhome.dbserver.tools.util.MathUtils;
import com.fiberhome.dbserver.tools.util.PreconditionVerifier;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.AbstractStub;

/**
 * GRPC客户端
 *
 * @author fuyuanyuan, 2020/10/20
 * @since 1.0.0
 */
public class GrpcClient
{
    private static final Logger LOG = LoggerFactory.getLogger(GrpcClient.class);

    /**
     * 客户端通讯链路
     */
    private ManagedChannel[] channels;

    /**
     * 通讯链路的数量
     */
    private int channelCount;

    /**
     * 使用通讯链路的计数器
     */
    private AtomicInteger channelCounter = new AtomicInteger();

    /**
     * 访问服务地址
     */
    private String transportHost;

    /**
     * 访问服务端口
     */
    private int transportPort;

    /**
     * GRPC Client端业务线程池
     */
    private ExecutorService pool;

    /**
     * 配置
     */
    private static final Object CHANNELMUTEX = new Object();

    /**
     * 构造客户端函数
     * 指定要连接的机器，端口号，客户端需要建立的连接数，rpc所用的线程池
     *
     * @param hostName 主机名称
     * @param port 端口名称
     * @param cachedChannelCount 客户端需要建立的连接数
     * @param executor rpc所用的线程池
     *
     */
    GrpcClient(String hostName, int port, int cachedChannelCount, ExecutorService executor)
    {
        this.transportHost = Addressing.getHostIpFromInetAddress(hostName);
        PreconditionVerifier.isValidIpAddress(this.transportHost);
        this.transportPort = port;
        this.channelCount = cachedChannelCount;
        this.pool = executor;
        //创建通信链路
        channels = new ManagedChannel[channelCount];
        for (int i = 0; i < channelCount; i++)
        {
            channels[i] = createClientChannel();
        }
    }

    /**
     * 创建客户端访问server的管道
     *
     * @return
     */
    private ManagedChannel createClientChannel()
    {
        LOG.info("init client host: {} and port:{}", transportHost, transportPort);
        NettyChannelBuilder builder = NettyChannelBuilder
                .forAddress(new InetSocketAddress(transportHost, transportPort)).usePlaintext()
                .maxInboundMessageSize(MAX_MESSAGE_SIZE).executor(this.pool);
        return builder.build();
    }

    /**
     * 关闭通信的通道
     */
    public void shutdown()
    {
        if (channels != null)
        {
            for (ManagedChannel channel : channels)
            {

                if (!channel.isShutdown())
                {
                    channel.shutdown();
                }
            }
        }
        if (this.pool != null)
        {
            this.pool.shutdownNow();
        }
    }

    public ManagedChannel[] getChannels()
    {
        return channels;
    }

    public AbstractStub getServiceAsyncStub(String serviceName)
    {
        return (AbstractStub) getRpcStub(transportHost, this.transportPort, serviceName, ClientStubType.ASYNCSTUB);
    }

    public AbstractStub getServiceBlockStub(String serviceName)
    {
        return (AbstractStub) getRpcStub(transportHost, this.transportPort, serviceName, ClientStubType.BLOCKSTUB);
    }

    public AbstractStub getServiceFutureStub(String serviceName)
    {
        return (AbstractStub) getRpcStub(transportHost, this.transportPort, serviceName, ClientStubType.FUTURESTUB);
    }

    /**
     * 获取Rpc根
     * @param host 客户端主机名
     * @param port 客户端端口
     * @param serviceName 服务名
     * @param stubType 根类别
     * @return Object
     */
    public static Object getRpcStub(String host, int port, String serviceName, ClientStubType stubType)
    {
        try
        {
            Class<?> rpcClass = Class.forName(serviceName);
            Method stubMethod = null;

            switch (stubType)
            {
                case ASYNCSTUB:
                case BLOCKSTUB:
                case FUTURESTUB:
                    stubMethod = rpcClass.getDeclaredMethod(stubType.getMethod(), Channel.class);
                    stubMethod.setAccessible(true);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown stubType: " + stubType);
            }
            ManagedChannel channel = GrpcClientFactory.getGrpcClient(host, port).getChannel();
            return stubMethod.invoke(rpcClass, channel);
        }
        catch (Exception e)
        {
            LOG.error("Failed to get rpcStub.", e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * 客户端调用服务的存根类型
     */
    public enum ClientStubType
    {
        // 异步存根
        ASYNCSTUB("newStub"), // 同步阻塞
        BLOCKSTUB("newBlockingStub"), // 同步不阻塞
        FUTURESTUB("newFutureStub");

        private String method;

        private ClientStubType(String method)
        {
            this.method = method;
        }

        public String getMethod()
        {
            return method;
        }

    }

    /**
     * 随机获取链路
     *
     * @return 轮询数组中的通信链路
     */
    public ManagedChannel getChannel()
    {
        int random = MathUtils.mod(channelCounter.incrementAndGet(), channelCount);
        if (channels[random] != null && !channels[random].isShutdown() && !channels[random].isTerminated())
        {
            return channels[random];
        }
        else
        {
            synchronized (CHANNELMUTEX)
            {
                if (channels[random] != null && !channels[random].isShutdown() && !channels[random].isTerminated())
                {
                    return channels[random];
                }
                else
                {
                    channels[random] = createClientChannel();
                    return channels[random];
                }
            }
        }
    }
}
