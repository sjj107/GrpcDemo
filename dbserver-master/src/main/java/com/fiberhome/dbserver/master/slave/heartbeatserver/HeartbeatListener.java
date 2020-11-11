package com.fiberhome.dbserver.master.slave.heartbeatserver;

import com.fiberhome.dbserver.common.elements.server.ServerName;
// import com.fiberhome.dbserver.common.elements.server.SlaveLoad;

/**
 * 心跳监听器
 * @author lizhen, 2020/10/16
 * @since 1.0.0
 */
public interface HeartbeatListener
{
    /**
     * 心跳处理
     *
     * @param serverName 上报心跳的服务器
     // * @param serverLoad 心跳信息
     */
    void receiveHeartbeat(ServerName serverName/*, SlaveLoad serverLoad*/);
}
