package com.fiberhome.dbserver.slave;

/**
 * Server启动标记
 * 
 * @author dinne
 * @date 2018/08/16
 */
public class ServerFlag
{
    /**
     * [0, stopped, abort, initialized, isPremiered]
     */
    private volatile byte flag = 0;

    /**
     * 是否可正常退出
     */
    private boolean isExit = false;
    
    /**
     * <p>
     * 构造器
     * </p>
     */
    public ServerFlag()
    {
        //
    }

    /**
     * <p>
     * 图服务是否关闭
     * </p>
     * 
     * @return 是否停止 true:停止 false:未停止
     */
    public boolean isStopped()
    {
        return (flag & 0x1) != 0;
    }

    /**
     * <p>
     * 设置关闭
     * </p>
     * 
     * @param stopped 停止
     * @return 标识
     */
    public ServerFlag setStopped(boolean stopped)
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
     * 
     * @return 是否终止
     */
    public boolean isAborted()
    {
        return (flag & 0x2) != 0;
    }

    /**
     * <p>
     * 设置中断
     * </p>
     * 
     * @param abort 停止
     * @return 标识
     */
    public ServerFlag setAborted(boolean abort)
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
     * 
     * @return 标识
     */
    public boolean isActiveMaster()
    {
        return (flag & 0x4) != 0;
    }

    /**
     * <p>
     * 是否初始化完成
     * </p>
     * 
     * @return 标识
     */
    public boolean isInitialized()
    {
        return (flag & 0x8) != 0;
    }

    /**
     * <p>
     * 设置是否初始化完成
     * </p>
     * 
     * @param isInitialized 初始化
     * @return 标识
     */
    public ServerFlag setInitialized(boolean isInitialized)
    {
        byte initializeFlag = (flag |= 0x8);
        byte nonInitializeFlag = (flag &= 0xF7);
        flag = isInitialized ? initializeFlag : nonInitializeFlag;
        return this;
    }
    
    /**
     * 主线程是否允许退出
     * 防止底层数据未flush完成，主线程退出导致数据丢失
     * @return 是否可退出
     */
    public boolean isExit()
    {
        return isExit;
    }
    
    /**
     * <p>
     * 设置是否可以退出
     * </p>
     * 
     * @param exit 退出
     */
    public void setExit(boolean exit)
    {
        this.isExit = exit;
    }
}
