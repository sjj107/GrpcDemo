package com.fiberhome.dbserver.common.config;

import com.fiberhome.dbserver.tools.conf.BaseConfiguration;


/**
 * <p>
 * <b> Server配置 </b> <br>
 * Server配置,只进行server-site.xml配置的读取.
 * </p>
 *
 * @author lizhen
 * @date 2020/10/23
 * @since 1.0.0
 */
public class ServerConfiguration extends BaseConfiguration
{
    /**
     * 懒执行获取配置
     *
     * @return Server配置
     */
    public static ServerConfiguration getServerConf()
    {
        return Holder.conf;
    }

    static class Holder
    {
        private Holder()
        {

        }

        private static ServerConfiguration conf = new ServerConfiguration();
    }

    /**
     * <p>
     * 私有构造器
     * </p>
     */
    private ServerConfiguration()
    {
        // Can't be instantiated directly.
        super(false);
        addResource("conf/dbslave-site.xml");
    }

    /**
     * 获取块索引grpc查询超时时间
     * @return 块索引grpc查询超时时间
     */
    public int getBlockletIndexGrpcQueryTimeOut()
    {
        return getInt("slave.query.index.timeout.max", 1000);
    }

    /**
     * 获取块索引返回CK列表失效的大小
     * @return 失效的大小
     */
    public int getBlockletIndexCKMaxSize()
    {
        return getInt("slave.query.index.cksize.max", 10000);
    }
}
