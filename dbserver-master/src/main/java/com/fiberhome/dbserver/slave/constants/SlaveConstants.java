package com.fiberhome.dbserver.slave.constants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Slave常量类
 *
 * @author hzq, 2020年10月21日
 * @since 1.0.0
 */
public class SlaveConstants
{

    /**
     * Prefix for Master log.
     */
    public static final String SLAVE_LOG_PREFIX = "##";

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
     * Slave主机名
     */
    public static final String SLAVE_HOST_NAME = "dbserver.slave.host.name";

    /**
     * Slave GRPC服务端口配置项
     */
    public static final String SLAVE_GRPC_SERVER_PORT_CONF = "dbserver.slave.grpc.server.port";

    /**
     * Region管理线程池大小配置
     */
    public static final String REGION_POOL_SIZE_CONF = "dbserver.slave.region.handle.pool.size";

    /**
     * Region管理线程池默认大小
     */
    public static final int REGION_DEFAULT_POOL_SIZE = 10;

    /**
     * Server 块数据管理线程池大小配置
     */
    public static final String BLOCKLET_DATA_POOL_SIZE_CONF = "dbserver.slave.blockletdata.handle.pool.size";

    /**
     * 块数据管理线程池默认大小
     */
    public static final int BLOCKLET_DATA_DEFAULT_POOL_SIZE = 10;

    /**
     * Server 块索引管理线程池大小配置
     */
    public static final String BLOCKLET_INDEX_POOL_SIZE_CONF = "dbserver.slave.blockletindex.handle.pool.size";

    /**
     * 块索引管理线程池默认大小
     */
    public static final int BLOCKLET_INDEX_DEFAULT_POOL_SIZE = 10;

    /**
     * Server Compact管理线程池大小配置
     */
    public static final String COMPACT_POOL_SIZE_CONF = "dbserver.slave.compact.handle.pool.size";

    /**
     * 合并线程池默认大小
     */
    public static final int COMPACT_DEFAULT_POOL_SIZE = 10;

    /**
     * Server HeartBeat管理线程池大小配置
     */
    public static final String HEARTBEAT_POOL_SIZE_CONF = "dbserver.slave.heartbeat.handle.pool.size";

    /**
     * 心跳线程池默认大小
     */
    public static final int HEARTBEAT_DEFAULT_POOL_SIZE = 10;

    /**
     * Server 协调服务线程池大小配置
     */
    public static final String COORDINATESERVICE_POOL_SIZE_CONF = "dbserver.slave.coordinateservice.handle.pool.size";

    /**
     * 协调服务线程池默认大小
     */
    public static final int COORDINATESERVICE_DEFAULT_POOL_SIZE = 10;

}
