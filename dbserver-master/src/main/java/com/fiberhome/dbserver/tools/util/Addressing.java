package com.fiberhome.dbserver.tools.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Utility for network addresses, resolving and naming.
 */
public class Addressing
{
    public static final String VALID_PORT_REGEX = "[\\d]+";
    private static final String HOSTNAME_PORT_SEPARATOR = ":";

    /** 根据主机名和端口号创建socket链接地址
     * @param hostAndPort Formatted as <code>&lt;hostname> ':' &lt;port></code>
     * @return An InetSocketInstance
     */
    public static InetSocketAddress createInetSocketAddressFromHostAndPortStr(final String hostAndPort)
    {
        return new InetSocketAddress(parseHostname(hostAndPort), parsePort(hostAndPort));
    }

    /** 创建主机名与端口号
     * @param hostname Server hostname
     * @param port Server port
     * @return Returns a concatenation of <code>hostname</code> and
     *         <code>port</code> in following form: <code>&lt;hostname> ':'
     *         &lt;port></code>. For example, if hostname is
     *         <code>example.org</code> and port is 1234, this method will return
     *         <code>example.org:1234</code>
     */
    public static String createHostAndPortStr(final String hostname, final int port)
    {
        return hostname + HOSTNAME_PORT_SEPARATOR + port;
    }

    /** 解析hostname
     * @param hostAndPort Formatted as <code>&lt;hostname> ':' &lt;port></code>
     * @return The hostname portion of <code>hostAndPort</code>
     */
    public static String parseHostname(final String hostAndPort)
    {
        int colonIndex = hostAndPort.lastIndexOf(HOSTNAME_PORT_SEPARATOR);
        if (colonIndex < 0)
        {
            throw new IllegalArgumentException("Not a host:port pair: " + hostAndPort);
        }
        return hostAndPort.substring(0, colonIndex);
    }

    /** 解析port
     * @param hostAndPort Formatted as <code>&lt;hostname> ':' &lt;port></code>
     * @return The port portion of <code>hostAndPort</code>
     */
    public static int parsePort(final String hostAndPort)
    {
        int colonIndex = hostAndPort.lastIndexOf(HOSTNAME_PORT_SEPARATOR);
        if (colonIndex < 0)
        {
            throw new IllegalArgumentException("Not a host:port pair: " + hostAndPort);
        }
        return Integer.parseInt(hostAndPort.substring(colonIndex + 1));
    }

    /**
     * 获取Host IP
     * 
     * @param hostname 主机名
     * @return host ip
     */
    public static String getHostIpFromInetAddress(final String hostname)
    {
        try
        {
            return InetAddress.getByName(hostname).getHostAddress();
        }
        catch (UnknownHostException e)
        {
            return "UnknownHost";
        }
    }

    /** 获取主机名
     * @return host name
     */
    public static String getLocalHostName()
    {
        try
        {
            String hostName = System.getenv("COMPUTERNAME");
            if (hostName != null)
            {
                return hostName;
            }
            else
            {
                return InetAddress.getLocalHost().getHostName();
            }
        }
        catch (UnknownHostException e)
        {
            return "UnknownHost";
        }
    }
}
