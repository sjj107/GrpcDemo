package com.fiberhome.dbserver.common.elements.server;

import java.util.Objects;

/**
 * Server位置信息
 *
 * @author lizhen
 * @date 2020/10/16
 * @since 1.0.0
 */
public class ServerName implements Comparable<ServerName>
{
    /**
     * 分隔符
     */
    private static final String SERVERNAME_SEPARATOR = ":";
    /**
     * 主机名
     */
    private String hostName;

    /**
     * 主机ip
     */
    private String ip;

    /**
     * 端口号
     */
    private int port;

    /**
     * 启动时间
     */
    private long startTime;

    /**
     * Server名
     *
     * @param hostName  服务主机名
     * @param ip        服务ip地址
     * @param port      服务端口号
     * @param startTime 起始时间
     */

    public ServerName(String hostName, String ip, int port, long startTime)
    {
        this.hostName = hostName;
        this.ip = ip;
        this.port = port;
        this.startTime = startTime;
    }

    /**
     * 构建ServerName实例
     *
     * @param serverName <b>格式必须为：IP:Port:StartCode</b>
     * @return ServerName
     */
    public static ServerName valueOf(final String serverName)
    {
        return new ServerName(parseHostname(serverName), null, parsePort(serverName), parseStartcode(serverName));
    }

    /**
     * 解析主机名
     *
     * @param serverName ${@link ServerName}
     * @return 根据ServerName解析后的主机名
     */
    public static String parseHostname(final String serverName)
    {
        if (serverName == null || serverName.length() <= 0)
        {
            throw new IllegalArgumentException("Passed hostname is null or empty");
        }
        if (!Character.isLetterOrDigit(serverName.charAt(0)))
        {
            throw new IllegalArgumentException("Bad passed hostname, serverName=" + serverName);
        }
        int index = serverName.indexOf(SERVERNAME_SEPARATOR);
        return serverName.substring(0, index);
    }

    /**
     * 解析端口
     *
     * @param serverName ${@link ServerName} ServerName
     * @return 根据ServerName解析出的端口号
     */
    public static int parsePort(final String serverName)
    {
        String[] split = serverName.split(SERVERNAME_SEPARATOR);
        return Integer.parseInt(split[1]);
    }

    /**
     * 解析startcode
     *
     * @param serverName ${@link ServerName}
     * @return 根据ServerName解析出的startCode
     */
    public static long parseStartcode(final String serverName)
    {
        int index = serverName.lastIndexOf(SERVERNAME_SEPARATOR);
        return Long.parseLong(serverName.substring(index + 1));
    }

    /**
     * 构造ZK中存储数据
     *
     * @return ServerName字符串
     */
    public String getServerName()
    {
        final StringBuilder name = new StringBuilder(hostName.length() + 1 + 5 + 1 + 13);
        name.append(hostName);
        name.append(SERVERNAME_SEPARATOR);
        name.append(port);
        name.append(SERVERNAME_SEPARATOR);
        name.append(startTime);
        return name.toString();
    }

    /**
     * compareTo 方法
     *
     * @param serverName Server位置信息
     * @return
     */
    @Override
    public int compareTo(ServerName serverName)
    {
        // todo
        throw new UnsupportedOperationException();
    }


    /**
     * HostName get方法
     *
     * @return String
     */
    public String getHostName()
    {
        return hostName;
    }

    /**
     * hostName Set方法
     *
     * @param hostName 主机名
     */
    public void setHostName(String hostName)
    {
        this.hostName = hostName;
    }

    /**
     * HostIp get方法
     *
     * @return String
     */
    public String getHostIp()
    {
        return ip;
    }

    /**
     * HostIp set方法
     *
     * @param hostIp ip
     */
    public void setHostIp(String hostIp)
    {
        this.ip = hostIp;
    }

    /**
     * Port get方法
     *
     * @return int
     */
    public int getPort()
    {
        return port;
    }

    /**
     * Port set方法
     *
     * @param port 端口号
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * StartTime get方法
     *
     * @return long
     */
    public long getStartTime()
    {
        return startTime;
    }


    /**
     * StartTime set方法
     *
     * @param startTime 开始时间
     */
    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    /**
     * 比较两个ServerName的hostname和port
     *
     * @param left  第一个ServerName
     * @param right 第一个ServerName
     * @return 两个ServerName是否相同
     */
    public static boolean isSameHostnameAndPort(final ServerName left, final ServerName right)
    {
        if (left == null)
        {
            return false;
        }
        if (right == null)
        {
            return false;
        }
        return left.getHostName().equals(right.getHostName()) && left.getPort() == right.getPort();
    }

    /**
     * equals方法
     *
     * @param o Object
     * @return boolean
     */
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
        ServerName that = (ServerName) o;
        return getPort() == that.getPort() && Objects
                .equals(getHostName(), that.getHostName()) && Objects.equals(getHostIp(), that.getHostIp());
    }

    /**
     * hashCode方法
     *
     * @return int
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(getHostName(), getHostIp(), getPort(), getStartTime());
    }

    /**
     * toString方法
     *
     * @return String
     */
    @Override
    public String toString()
    {
        return "ServerName{" + "hostName='" + hostName + '\'' + ", hostIp='" + ip + '\'' + ", port="
                + port + ", startTime=" + startTime + '}';
    }
}
