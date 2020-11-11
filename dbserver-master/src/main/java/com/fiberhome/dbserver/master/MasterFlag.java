package com.fiberhome.dbserver.master;

/**
 * <b> Master启动标识 </b>
 */
final class MasterFlag
{
    /**
     * [0, stopped, abort, isActiveMaster, initialized, startupRawRSReceptor,
     * isPremiered, failToCreateMetaTable]
     */
    private volatile byte flag = 0;

    /**
     * <p>
     * 构造器
     * </p>
     */
    public MasterFlag()
    {
        // NOTHING
    }

    /**
     * <p>
     * Master是否关闭
     * </p>
     */
    public boolean isStopped()
    {
        return (flag & 0x1) != 0;
    }

    /**
     * <p>
     * 设置关闭
     * </p>
     */
    public MasterFlag setStopped(boolean stopped)
    {
        byte stopFlag = (flag |= 0x1);
        byte nonStopFlag = (flag &= 0xFE);
        flag = stopped ? stopFlag : nonStopFlag;
        return this;
    }

    /**
     * <p>
     * 是否中断
     * </p>
     */
    public boolean isAborted()
    {
        return (flag & 0x2) != 0;
    }

    /**
     * <p>
     * 设置中断
     * </p>
     */
    public MasterFlag setAborted(boolean abort)
    {
        byte abortFlag = (flag |= 0x2);
        byte nonAbortFlag = (flag &= 0xFD);
        flag = abort ? abortFlag : nonAbortFlag;
        return this;
    }

    /**
     * <p>
     * 主节点是否为Active状态
     * </p>
     */
    public boolean isActiveMaster()
    {
        return (flag & 0x4) != 0;
    }

    /**
     * <p>
     * 设置主节点的Active状态
     * </p>
     */
    public MasterFlag setActiveMaster(boolean isActiveMaster)
    {
        byte activeMasterFlag = (flag |= 0x4);
        byte nonActiveMasterFlag = (flag &= 0xFB);
        flag = isActiveMaster ? activeMasterFlag : nonActiveMasterFlag;
        return this;
    }

    /**
     * <p>
     * 是否初始化完成
     * </p>
     */
    public boolean isInitialized()
    {
        return (flag & 0x8) != 0;
    }

    /**
     * <p>
     * 设置是否初始化完成
     * </p>
     */
    public MasterFlag setInitialized(boolean isInitialized)
    {
        byte initializeFlag = (flag |= 0x8);
        byte nonInitializedFlag = (flag &= 0xF7);
        flag = isInitialized ? initializeFlag : nonInitializedFlag;
        return this;
    }

    /**
     * flag set after we complete send meta first.
     */
    public boolean isStartupSlaveReceptor()
    {
        return (flag & 0x10) != 0;
    }

    /**
     * 启动server接收器
     * 
     * @param startupSlaveReceptor 是否启动
     * @return 启动标示
     */
    public MasterFlag startupSlaveReceptor(boolean startupSlaveReceptor)
    {
        byte trueFlag = (flag |= 0x10);
        byte falseFlag = (flag &= 0xEF);
        flag = startupSlaveReceptor ? trueFlag : falseFlag;
        return this;
    }

    /**
     * <p>
     * 是否为第一次启动
     * </p>
     */
    public boolean isPremiered()
    {
        return (flag & 0x20) != 0;
    }

    /**
     * <p>
     * 设置是否为第一次启动
     * </p>
     */
    public MasterFlag setPremiered(boolean isPremiered)
    {
        byte trueFlag = (flag |= 0x20);
        byte falseFlag = (flag &= 0xDF);
        flag = isPremiered ? trueFlag : falseFlag;
        return this;
    }

}
