package com.fiberhome.dbserver.common.executor.payload;

/**
 * 周期性任务
 *
 * @author lizhen, 2020/11/6
 * @since 1.0.0
 */
public abstract class CyclePayload implements Runnable
{
    private boolean fixedRate = false; // 是否按照固定频率执行
    private long period; // 周期
    private long delayedTime; // 时延

    /**
     * 周期性任务
     * @param fixedRate 是否按固定频率执行，否则按固定延时
     * @param period 触发周期
     * @param delayedTime 初次执行延时时间
     */
    public CyclePayload(boolean fixedRate, long period, long delayedTime)
    {
        this.fixedRate = fixedRate;
        this.period = period;
        this.delayedTime = delayedTime;
    }

    @Override
    public final void run()
    {
        try
        {
            process();
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    protected abstract void process() throws Exception;

    public boolean isFixedRate()
    {
        return fixedRate;
    }

    public long getPeriod()
    {
        return period;
    }

    public long getDelayedTime()
    {
        return delayedTime;
    }
}
