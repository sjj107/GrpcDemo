package com.fiberhome.dbserver.common.services;

import com.fiberhome.dbserver.common.elements.server.ServerName;
// import com.fiberhome.dbserver.common.zookeeper.ZooKeeperWatcher;
import com.fiberhome.dbserver.tools.conf.BaseConfiguration;

/**
 * 服务包含的基础信息：
 * <dd>
 * <b>服务配置信息 {@linkplain BaseConfiguration}</b>
 *  <dd>
 // * <b>ZK监听器 {@linkplain ZooKeeperWatcher}</b>
 * <dd>
 * <b>ServerName {@linkplain ServerName}</b>
 * 
 * @author dinne
 * @date 2018/08/18
 */
public interface Service extends Abortable, Stoppable
{
    /**
     * 服务基本配置信息
     * @return 配置信息
     */
    BaseConfiguration getConfiguration();

    // /**
    //  *ZooKeeperWatcher
    //  * @return ZKWatcher
    //  */
    // ZooKeeperWatcher getZooKeeper();

    /**
     * Server名称
     * @return ServerName
     */
    ServerName getServerName();

}
