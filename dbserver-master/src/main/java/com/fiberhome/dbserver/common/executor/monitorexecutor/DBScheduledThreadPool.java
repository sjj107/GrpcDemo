package com.fiberhome.dbserver.common.executor.monitorexecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 周期线程池实现
 *
 * @author fuyuanyuan, 2020/10/27
 * @since 1.0.0
 */
public class DBScheduledThreadPool extends ScheduledThreadPoolExecutor
{
    /*
     * 统计任务最大耗时
     */
    private long maxTaskCost = Integer.MIN_VALUE;

    /*
     * 统计任务最小耗时
     */
    private long minTaskCost = Integer.MAX_VALUE;

    /*
     * 统计任务平均耗时
     */
    private long averageTaskCost;

    /*
     * 统计任务的总时间
     */
    private long taskCostSum;

    /*
     * 统计执行次数
     */
    private AtomicInteger count = new AtomicInteger(0);

    /*
     * 任务开始时间
     */
    private ThreadLocal<Long> taskStartTime = new ThreadLocal<>();

    /**
     * 周期线程池
     *
     * @param corePoolSize  线程核心数
     * @param threadFactory 线程池工厂
     * @param handler       拒绝策略
     */
    DBScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler)
    {
        super(corePoolSize, threadFactory, handler);
    }

    /**
     * 实现该方法可以进行一些任务执行前的准备工作，例如进行任务计时
     *
     * @param thread   执行任务的线程
     * @param runnable 要执行的任务
     */

    @Override
    protected void beforeExecute(Thread thread, Runnable runnable)
    {
        taskStartTime.set(System.currentTimeMillis());
        super.beforeExecute(thread, runnable);
    }

    /**
     * 实现该方法可以进行一些任务执行完后的收尾工作
     *
     * @param runnable  已完成的任务
     * @param throwable 任务是否出错，出错原因
     */
    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable)
    {
        super.afterExecute(runnable, throwable);
        long cost = System.currentTimeMillis() - taskStartTime.get();
        int countSum = count.addAndGet(1);
        taskCostSum += cost;
        averageTaskCost = taskCostSum / countSum;
        maxTaskCost = Math.max(maxTaskCost, cost);
        minTaskCost = Math.min(minTaskCost, cost);
        taskStartTime.remove();
    }

    /**
     * 统计线程池中任务的平均耗时
     *
     * @return 平均耗时
     */
    public long getAverageTaskCost()
    {
        return averageTaskCost;
    }

    /**
     * 统计线程池中任务的最大耗时
     *
     * @return 最大耗时
     */

    public long getMaxTaskCost()
    {
        return maxTaskCost;
    }

    /**
     * 统计线程池中任务的最小耗时
     *
     * @return 最小耗时
     */
    public long getMinTaskCost()
    {
        return minTaskCost;
    }

}
