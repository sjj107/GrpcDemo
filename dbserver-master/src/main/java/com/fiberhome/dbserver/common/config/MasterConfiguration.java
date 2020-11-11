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
public class MasterConfiguration extends BaseConfiguration
{
    /**
     * 懒执行获取配置
     *
     * @return Master配置
     */
    public static MasterConfiguration getMasterConf()
    {
        return Holder.conf;
    }

    static class Holder
    {
        private Holder()
        {

        }

        private static MasterConfiguration conf = new MasterConfiguration();
    }

    /**
     * <p>
     * 私有构造器
     * </p>
     */
    public MasterConfiguration()
    {
        // Can't be instantiated directly.
        super(false);
        addResource("conf/dbmaster-site.xml");
    }
}
