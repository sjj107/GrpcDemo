package com.fiberhome.dbserver.common.executor.payload;

import com.fiberhome.dbserver.common.executor.ExecutorType;

/**
 * 延迟任务
 *
 * @author lizhen, 2020/11/6
 * @since 1.0.0
 */
public abstract class DelayedPayload<R> extends Payload<R>
{
    private long delayedTime;

    public DelayedPayload(long delayedTime)
    {
        super(ExecutorType.SCHEDULE_THREAD_POOL);
        this.delayedTime = delayedTime;
    }

    public long getDelayedTime()
    {
        return delayedTime;
    }
}
