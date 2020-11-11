package com.fiberhome.dbserver.master.slave.heartbeatserver;

import java.util.Objects;
import java.util.Set;

// import com.fiberhome.dbserver.common.elements.region.RegionLoad;
import com.fiberhome.dbserver.common.elements.server.ServerName;
// import com.fiberhome.dbserver.common.metrics.JvmResource;
// import com.fiberhome.dbserver.common.metrics.MemoryResource;

/**
 * 心跳信息
 * 心跳信息需转化为proto定义的HeartBeatRequest
 * 上报给master的心跳管理模块
 *
 * @author lizhen, 2020/10/16
 * @since 1.0.0
 */
public class HeartBeatInfo
{
    private ServerName serverName; // Slave主机信息
    // private MemoryResource memoryResource; // Slave主机内存资源信息
    // private JvmResource jvmResource; //虚拟机资源信息
    // private Set<RegionLoad> regionLoad; // Region负载信息
    private long reportTime; // 上报时间

    /**
     * 心跳信息
     * @param serverName Slave主机信息
     // * @param memoryResource Slave主机内存资源信息
     // * @param jvmResource 虚拟机资源信息
     // * @param regionLoad Region负载信息
     * @param reportTime 上报时间
     */
    public HeartBeatInfo(ServerName serverName, /*MemoryResource memoryResource, JvmResource jvmResource,
                         Set<RegionLoad> regionLoad,*/ long reportTime)
    {
        this.serverName = serverName;
        // this.memoryResource = memoryResource;
        // this.jvmResource = jvmResource;
        // this.regionLoad = regionLoad;
        this.reportTime = reportTime;
    }

    public ServerName getServerName()
    {
        return serverName;
    }

    public void setServerName(ServerName serverName)
    {
        this.serverName = serverName;
    }

    // public MemoryResource getMemoryResource()
    // {
    //     return memoryResource;
    // }
    //
    // public void setMemoryResource(MemoryResource memoryResource)
    // {
    //     this.memoryResource = memoryResource;
    // }
    //
    // public JvmResource getJvmResource()
    // {
    //     return jvmResource;
    // }
    //
    // public void setJvmResource(JvmResource jvmResource)
    // {
    //     this.jvmResource = jvmResource;
    // }
    //
    // public Set<RegionLoad> getRegionLoad()
    // {
    //     return regionLoad;
    // }
    //
    // public void setRegionLoad(Set<RegionLoad> regionLoad)
    // {
    //     this.regionLoad = regionLoad;
    // }

    public long getReportTime()
    {
        return reportTime;
    }

    public void setReportTime(long reportTime)
    {
        this.reportTime = reportTime;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || this.getClass() != o.getClass())
        {
            return false;
        }
        HeartBeatInfo that = (HeartBeatInfo) o;
        return getReportTime() == that.getReportTime() && Objects
                .equals(getServerName(), that.getServerName()) /*&& Objects
                .equals(getMemoryResource(), that.getMemoryResource()) && Objects
                .equals(getJvmResource(), that.getJvmResource()) && Objects
                .equals(getRegionLoad(), that.getRegionLoad())*/;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getServerName()/*, getMemoryResource(), getJvmResource(), getRegionLoad()*/, getReportTime());
    }

    @Override
    public String toString()
    {
        return "HeartBeatInfo{" + "serverName=" + serverName + ", memoryResource="/* + memoryResource + ", jvmResource="
                + jvmResource + ", regionLoad=" + regionLoad */+ ", reportTime=" + reportTime + '}';
    }
}

