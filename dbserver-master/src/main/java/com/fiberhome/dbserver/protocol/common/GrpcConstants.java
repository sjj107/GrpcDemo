package com.fiberhome.dbserver.protocol.common;

// import com.fiberhome.dbserver.common.transport.protobuf.ArchiveManagerServiceGrpc;
import com.fiberhome.dbserver.common.transport.protobuf.HeartBeatMangerServiceGrpc;
// import com.fiberhome.dbserver.common.transport.protobuf.RegionManagerServiceGrpc;
// import com.fiberhome.dbserver.common.transport.protobuf.SlaveBlockletDataServiceGrpc;
// import com.fiberhome.dbserver.common.transport.protobuf.SlaveBlockletIndexServiceGrpc;
// import com.fiberhome.dbserver.common.transport.protobuf.SlaveCoordinateServiceGrpc;
// import com.fiberhome.dbserver.common.transport.protobuf.SlaveManagerServiceGrpc;
// import com.fiberhome.dbserver.common.transport.protobuf.SlaveRegionMangerServiceGrpc;
// import com.fiberhome.dbserver.common.transport.protobuf.TableManagerServiceGrpc;

public class GrpcConstants
{
    /*
     * 默认的集群名配置
     */
    public static final String DEFAULT_CLUSTER_NAME = "dbserver.client.clusters.default";
    /*
     * master端 grpc的server地址
     */
    public static final String DBSERVER_MASTER_HOSTIP = "dbserver.master.hostip";
    /*
     * master端grpc的server端口
     */
    public static final String DBSERVER_MASTER_PORT = "dbserver.master.port";
    /*
     * server端grpc的server地址
     */
    public static final String DBSERVER_SERVER_HOSTIP = "dbserver.server.hostip";
    /*
     * server端grpc的server端口
     */
    public static final String DBSERVER_SERVER_PORT = "dbserver.server.port";
    /*
     * 每种server启动grpc最大线程数
     */
    public static final String DBSERVER_GRPC_SERVER_MAXTHREAD = "dbserver.grpc.server.maxthread";
    /*
     * 一次通讯传递消息最大值 默认80M
     */
    public static final int MAX_MESSAGE_SIZE = 80 * 1024 * 1024;

    /*
     * FlowWindow值 默认20M
     */
    public static final int MAX_FLOWWINDOW_SIZE = 80 * 1024 * 1024;

    /*
     * 线程存活时间
     */
    public static final int KEEP_LIVE_TIME = 60;

    /*
     * 分隔符：点号
     */
    public static final char SPLIT_DOT = '.';
    /*
     * Grpc服务后缀
     */
    public static final String GRPC_SERVICE_SUFFIX = "Grpc";
    /*
     * 分隔符：逗号
     */
    public static final String SPLIT_COMMA = ",";

    /*
      心跳管理服务
     */
    public static final String HEARTBEAT_MANAGER_SERVICE = HeartBeatMangerServiceGrpc.class.getName();
    // /*
    // Region管理服务
    //  */
    // public static final String REGION_MANAGER_SERVICE = RegionManagerServiceGrpc.class.getName();
    // /*
    // Server管理服务
    //  */
    // public static final String SERVER_MANAGER_SERVICE = SlaveManagerServiceGrpc.class.getName();
    // /*
    // 表管理服务
    //  */
    // public static final String TABLE_MANAGER_SERVICE = TableManagerServiceGrpc.class.getName();
    // /*
    // 归档服务
    //  */
    // public static final String ARCHIVE_MANAGER_SERVICE = ArchiveManagerServiceGrpc.class.getName();
    //
    // /*
    // Slave Region管理
    //  */
    // public static final String SLAVE_REGION_MANAGER_SERVICE = SlaveRegionMangerServiceGrpc.class.getName();
    // /*
    // Slave BlockIndex管理
    //  */
    // public static final String SLAVE_BLOCKINDEX_MANAGER_SERVICE = SlaveBlockletIndexServiceGrpc.class.getName();
    // /*
    // Slave BLOCKDATA管理
    //  */
    // public static final String SLAVA_BLOCKDATA_MANAGER_SERVICE = SlaveBlockletDataServiceGrpc.class.getName();
    // /*
    // Slave COORDINATESERVICE管理
    //  */
    // public static final String SLAVA_COORDINATESERVICE_MANAGER_SERVICE = SlaveCoordinateServiceGrpc.class.getName();

    /**
     * 私有构造
     */
    private GrpcConstants()
    {

    }
}
