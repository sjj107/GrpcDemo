package com.fiberhome.dbserver.slave;

import com.fiberhome.dbserver.common.services.Service;
// import com.fiberhome.dbserver.slave.blockletdata.BlockletDataMangerService;
// import com.fiberhome.dbserver.slave.blockletindex.BlockletIndexManagerService;
// import com.fiberhome.dbserver.slave.compact.CompactService;
// import com.fiberhome.dbserver.slave.coordinate.CoordinateService;
import com.fiberhome.dbserver.slave.heartbeat.HeartBeatService;
// import com.fiberhome.dbserver.slave.region.RegionManagerService;
// import com.fiberhome.dbserver.slave.zookeeper.ServerTrackerManager;

/**
 * DBSlave服务接口
 *
 * @author xiajunsheng
 * @update dinne
 * @date 2020/10/29
 * @since 1.0.0
 */
public interface DBSlaveServices extends Service
{

    // /**
    //  * 块数据服务管理
    //  *
    //  * @return {@link BlockletDataMangerService}
    //  */
    // BlockletDataMangerService getBlockletDataMangerServices();
    //
    // /**
    //  * 块索引管理服务
    //  *
    //  * @return {@link BlockletIndexManagerService}
    //  */
    // BlockletIndexManagerService getBlockletIndexLoadService();
    //
    // /**
    //  * Region管理, 涉及块数据、块索引、Projection 的region管理
    //  *
    //  * @return {@link RegionManagerService}
    //  */
    // RegionManagerService getRegionManagerService();

    /**
     * Server心跳服务
     *
     * @return {@link HeartBeatService}
     */
    HeartBeatService getHeartBeatService();

    // /**
    //  * 协调服务管理
    //  *
    //  * @return {@link CoordinateService}
    //  */
    // CoordinateService getCoordinateService();
    //
    // /**
    //  * ZK管理服务
    //  *
    //  * @return {@link ServerTrackerManager}
    //  */
    // ServerTrackerManager getServerTrackerManager();
    //
    // /**
    //  * 合并服务
    //  * @return CompactService
    //  */
    // CompactService getCompactService();
}
