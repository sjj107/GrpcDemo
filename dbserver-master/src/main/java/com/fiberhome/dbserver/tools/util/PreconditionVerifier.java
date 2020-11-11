package com.fiberhome.dbserver.tools.util;

import java.net.InetAddress;
import java.util.regex.Pattern;

/**
 * <p>
 * 字符名检查
 * </p>
 * 
 * @author P.F.XING
 */
public class PreconditionVerifier
{
    /**
     * 图名必须以字母或数字开头, 支持全数字或字母为图名，除了数字和字母符号外仅支持'_','-'和'.'符号；
     */
    private static final String VALID_NAME_REGEX = "([a-zA-Z0-9][a-zA-Z_0-9-.]*)";

    /**
     * 只检查图名字符合法性 (图名不能为null或者Empty)
     * 
     * @param graphName 图名
     * @return true or false
     */
    public static boolean isValidGraphName(String graphName)
    {
        return Pattern.matches(VALID_NAME_REGEX, graphName) && graphName.length() <= 255;
    }

    /**
     * <p>
     * 只检查命名空间字符合法性
     * </p>
     * 
     * @param nameSpace 命名空间
     * @return {@code nameSpace}不为空且命名空间合法则返回{@code true}，否则返回{@code false}
     */
    public static boolean isValidNameSpace(String nameSpace)
    {
        return nameSpace == null ? false : Pattern.matches(VALID_NAME_REGEX, nameSpace);
    }

    /**
     * <p>
     * 只检查表名字符串的合法性
     * </p>
     * 
     * @param tableName 表名
     * @return {@code tableName}不为空且表名合法则返回{@code true}，否则返回{@code false}
     */
    public static boolean isValidTableName(String tableName)
    {
        return tableName == null ? false : Pattern.matches(VALID_NAME_REGEX, tableName);
    }

    /**
     * <p>
     * 通用名字符串的合法性
     * </p>
     * 
     * @param name 名称
     * @return {@code name}不为空且通用名合法则返回{@code true}，否则返回{@code false}
     */
    public static boolean isValidName(String name)
    {
        return name == null ? false : Pattern.matches(VALID_NAME_REGEX, name);
    }

    /**
     * <p>
     * 只检查列族名字符串的合法性
     * </p>
     * 
     * @param cfName 列族名
     * @return {@code cfName}不为空且列族名合法则返回{@code true}，否则返回{@code false}
     */
    public static boolean isValidColumnFamilyName(String cfName)
    {
        return cfName == null ? false : Pattern.matches(VALID_NAME_REGEX, cfName);
    }

    /** 回环地址 */
    private static final String LOCALHOST = InetAddress.getLoopbackAddress().toString();

    /**
     * 校验IP地址合法性
     * 
     * @param hostIp 主机名称
     * @throws IllegalArgumentException 异常
     */
    public static void isValidIpAddress(String hostIp) throws IllegalArgumentException
    {
        if (hostIp == null || hostIp.isEmpty() || hostIp.equals(LOCALHOST))
        {
            throw new IllegalArgumentException("invalid param host: " + hostIp);
        }

        final String ip_regex = "((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)";
        // 也可用 IPAddressUtil.isIPv4LiteralAddress(hostIp);
        if (Pattern.matches(ip_regex, hostIp))
        {
            return;
        }
        throw new IllegalArgumentException("invalid param host: " + hostIp);
    }

}
