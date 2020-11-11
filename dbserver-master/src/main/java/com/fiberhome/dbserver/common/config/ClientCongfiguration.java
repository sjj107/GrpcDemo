package com.fiberhome.dbserver.common.config;

import com.fiberhome.dbserver.tools.conf.BaseConfiguration;

/**
 * <p>
 * <b> Master配置 </b> <br>
 * Master配置,只进行master-site.xml配置的读取.
 * </p>
 *
 * @author lizhen
 * @date 2020/10/23
 * @since 1.0.0
 */
public class ClientCongfiguration extends BaseConfiguration
{
    private static final String DEFAULT_CONF = "conf/dbclient-site.xml";
    private static volatile ClientCongfiguration instance;


    /**
     * 获取客户端配置
     *
     * @return 客户端配置
     */
    public static ClientCongfiguration getClientConfiguration()
    {
        return getClientConfiguration(DEFAULT_CONF);
    }


    /**
     * 懒执行获取配置
     *
     * @param confPath 配置文件路径
     * @return ClientCongfiguration Master配置
     */
    public static ClientCongfiguration getClientConfiguration(String confPath)
    {
        if (instance == null)
        {
            synchronized (ClientCongfiguration.class)
            {
                if (instance == null)
                {
                    if (confPath == null)
                    {
                        instance = new ClientCongfiguration(DEFAULT_CONF);
                    }
                    else
                    {
                        instance = new ClientCongfiguration(confPath);
                    }
                }
            }
        }
        return instance;
    }

    /**
     * <p>
     * 私有构造器
     * </p>
     */
    private ClientCongfiguration(String confFile)
    {
        // Can't be instantiated directly.
        super(false);
        addResource(confFile);
    }

    /**
     * 获取Master服务的默认rpc端口
     *
     * @return aster服务的默认rpc端口
     */
    public int getMasterServiceRpcPort()
    {
        return getInt("dbserver.master.grpc.server.port", 10050);
    }

    /**
     * 获取Slave服务的默认rpc端口
     *
     * @return lave服务的默认rpc端口
     */
    public int getSlaveServiceRpcPort()
    {
        return getInt("dbserver.server.grpc.server.port", 10060);
    }

}
