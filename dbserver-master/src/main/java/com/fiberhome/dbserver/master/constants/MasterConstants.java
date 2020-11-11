package com.fiberhome.dbserver.master.constants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Master端默认配置，使用较多的参数请添加于此.
 * 
 * @author xiajunsheng, 2020/10/19
 * @since 1.0.0
 */
public final class MasterConstants
{
    /**
     * 日志前缀符
     */
    public static final String MASTER_LOG_PREFIX = "##";

    /**
     * Display to date in standard format.
     */
    public static final ThreadLocal<DateFormat> STD_DATE_FORMAT = new ThreadLocal<DateFormat>()
    {
        @Override
        protected DateFormat initialValue()
        {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    /**
     * Master主机名
     */
    public static final String MASTER_HOST_NAME = "dbserver.master.host.name";
    

    /**
     * 集群版本号
     */
    public static final String CLUSTER_VERSION = "dbserver.cluster.version";

    
    /**
     * Master GRPC服务端口配置项
     */
    public static final String MASTER_GRPC_SERVER_PORT_CONF = "dbserver.master.grpc.server.port";

    /**
     * 算法GRPC服务默认端口
     */
    public static final int MASTER_GRPC_SERVER_PORT = 10050;

    /**
     * GRPC服务通道数配置项
     */
    public static final String GRPC_CHANNEL_NUM_CONF = "dbserver.master.grpc.channel.num";

    /**
     * GRPC服务默认通道数
     */
    public static final int GRPC_CHANNEL_NUM = 4;

    /**
     * GRPC服务最大线程数配置项
     */
    public static final String GRPC_MAX_THREADS_CONF = "dbserver.master.grpc.max.threads";

    /**
     * GRPC服务最大默认线程数
     */
    public static final int GRPC_MAX_THREADS = 5;

    /**
     * GRPC消息传输大小最大限制配置项
     */
    public static final String GRPC_MAX_MESSAGE_SIZE_CONF = "dbserver.master.grpc.max.message.size";

    /**
     * GRPC消息传输最大值，默认80M
     */
    public static final int GRPC_MAX_MESSAGE_SIZE = 83886080;

    /**
     * GRPC客户端线程名前缀
     */
    public static final String GRPC_CLIENT_THREAD_PREFIX = "grpc-client-";

    /**
     * GRPC服务类包名
     */
    public static final String GRPC_CODE_PACKAGE = "com.fiberhome.dbserver.common.transport.protobuf";

    /**
     * GRPC服务类名后缀
     */
    public static final String GRPC_SERVICE_SUFFIX = "Grpc";

    /**
     * 元数据线程池大小配置
     */
    public static final String META_POOL_SIZE_CONF = "dbserver.master.meta.handle.pool.size";

    /**
     * 元数据线程池默认大小
     */
    public static final int META_DEFAULT_POOL_SIZE = 10;

    /**
     * Region管理线程池大小配置
     */
    public static final String REGION_POOL_SIZE_CONF = "dbserver.master.region.handle.pool.size";

    /**
     * Region管理线程池默认大小
     */
    public static final int REGION_DEFAULT_POOL_SIZE = 10;

    /**
     * 归档管理线程池大小配置
     */
    public static final String ARCHIVE_POOL_SIZE_CONF = "dbserver.master.archive.handle.pool.size";

    /**
     * 归档管理线程池默认大小
     */
    public static final int ARCHIVE_DEFAULT_POOL_SIZE = 20;
    
    /**
     * 表管理线程池大小配置
     */
    public static final String TABLE_POOL_SIZE_CONF = "dbserver.master.table.handle.pool.size";

    /**
     * 表管理线程池默认大小
     */
    public static final int TABLE_DEFAULT_POOL_SIZE = 20;
    
    /**
     * 归档管理线程池大小配置
     */
    public static final String SLAVE_POOL_SIZE_CONF = "dbserver.master.slave.handle.pool.size";
    
    /**
     * Slave管理线程池默认大小
     */
    public static final int SLAVE_DEFAULT_POOL_SIZE = 20;

    /**
     * <p>
     * 私有构造器
     * </p>
     */
    private MasterConstants()
    {
        // Can't be instantiated.
    }
}
