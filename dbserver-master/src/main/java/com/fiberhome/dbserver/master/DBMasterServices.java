package com.fiberhome.dbserver.master;

import com.fiberhome.dbserver.common.executor.DBExecutorManager;
import com.fiberhome.dbserver.common.services.Service;
// import com.fiberhome.dbserver.master.archive.ArchiveManager;
// import com.fiberhome.dbserver.master.region.RegionManager;
// import com.fiberhome.dbserver.master.slave.SlaveManager;
// import com.fiberhome.dbserver.master.table.TableManager;
// import com.fiberhome.dbserver.master.zookeeper.MasterTrackerManager;

/**
 * <p>
 * DBMaster端的所有服务，主要包括如下子服务:
 * </p>
 * 
 // * <dd>MasterZK追踪管理服务：{@link MasterTrackerManager }
 // * <dd>Master主备切换管理服务：{@link ActiveMasterManager }
 // * <dd>归档管理：{@linkplain ArchiveManager}
 // * <dd>表管理：{@link TableManager }
 // * <dd>Slave管理：{@link SlaveManager }
 // * <dd>Region管理：{@link RegionManager }
 * <dd>Master端执行框架：{@link DBExecutorManager }
 * 
 * @author xiajunsheng
 * @update dinne
 * @date 2020/10/19
 * @since 1.0.0
 */
public interface DBMasterServices extends Service
{
    // /**
    //  * Master端访问ZK相关服务
    //  *
    //  * @return MasterTrackerManager {@linkplain MasterTrackerManager}
    //  */
    // MasterTrackerManager getMasterTrackerManager();
    //
    // /**
    //  * Master主备切换服务
    //  *
    //  * @return ActiveMasterManager {@linkplain ActiveMasterManager}
    //  */
    // ActiveMasterManager getActiveMasterManager();
    //
    // /**
    //  * Master表管理服务
    //  *
    //  * @return MetaManager {@linkplain TableManager}
    //  */
    // TableManager getTableManager();
    //
    // /**
    //  * Master region管理服务
    //  *
    //  * @return RegionManager {@linkplain RegionManager}
    //  */
    // RegionManager getRegionManager();
    //
    // /**
    //  * Master slave管理服务
    //  *
    //  * @return SlaveManager {@linkplain SlaveManager}
    //  */
    // SlaveManager getSlaveManager();
    //
    // /**
    //  * Master 归档管理服务
    //  *
    //  * @return ArchiveManager {@linkplain ArchiveManager}
    //  */
    // ArchiveManager getArchiveManager();
    //
    /**
     * Master执行服务
     *
     * @return DBExecutorManager {@linkplain DBExecutorManager}
     */
    DBExecutorManager getExecutorService();

    /**
     * 是否初始化化
     *
     * @return true:初始化 , false:未初始化
     */
    boolean isInitialized();

    /**
     * 是否为Active
     *
     * @return true:是 , false:否
     */
    boolean isActiveMaster();

    /**
     * 关闭集群
     */
    void shutdown();

    /**
     * Server接收器是否启动
     *
     * @return true:是 , false:否
     */
    boolean isStartupSlaveReceptor();
}
