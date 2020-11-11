package com.fiberhome.dbserver.common.executor.monitorexecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fiberhome.dbserver.common.config.MasterConfiguration;
import com.fiberhome.dbserver.common.executor.ExecutorType;
import com.google.common.collect.Maps;

/**
 * 线程池管理类
 *
 * @author fuyuanyuan, 2020/10/26
 * @since 1.0.0
 */
public class DBThreadPoolManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DBThreadPoolManager.class);

    private static final int DEFAULT_CORE_NUM = Runtime.getRuntime().availableProcessors();
    private static final int DEFAULT_MAX_NUM = 2 * DEFAULT_CORE_NUM + 1;
    private static final String THREAD_PREFIX = "thread-";

    /*
     * 单个线程池允许申请的最大核心线程数，从配置文件中读取
     */
    private int maxCoreThreadCount;

    /*
     * 单个线程池允许申请的最大线程数，从配置文件中读取
     */
    private int maxThreadCount;

    /*
     * 总的普通线程池允许申请的最大核心线程数，从配置文件中读取
     */
    private int maxNormalCoreThreadCount;

    /*
     * 总的周期线程池允许申请的最大核心线程数，从配置文件中读取
     */
    private int maxScheduleCoreThreadCount;
    /*
     * 总的普通线程池允许申请的最大线程数，从配置文件中读取
     *
     */
    private int maxNormalThreadCount;
    /*
     * 统计所有普通线程池核心线程数的总和
     */
    private int allNormalPoolCoreThreadSum = 0;

    /*
     * 统计所有周期性线程池核心线程数的总和
     */
    private int allSchedulePoolCoreThreadSum = 0;
    /*
     * 统计所有普通线程池最大线程数的总和
     */
    private int allNormalPoolMaxThreadSum = 0;

    /*
     * 存放普通线程池
     * 线程池及线程池描述
     */
    private WeakHashMap<ExecutorType, DBThreadPool> dbThreadPool = new WeakHashMap<>();

    /*
     * 存放普通线程池
     * 线程池及线程池描述
     */
    private WeakHashMap<ExecutorType, DBScheduledThreadPool> dbScheduledThreadPool = new WeakHashMap<>();

    /*
     * 默认的拒绝策略
     */
    private static final RejectedExecutionHandler DEFAULT_HANDLER = new ThreadPoolExecutor.AbortPolicy();

    /**
     * 线程池管理实例
     */
    private static DBThreadPoolManager instance;

    /**
     * 获得线程池管理
     *
     * @return 线程池管理
     */

    public static DBThreadPoolManager getInstance()
    {
        if (null != instance)
        {
            return instance;
        }

        synchronized (DBThreadPoolManager.class)
        {
            if (null != instance)
            {
                return instance;
            }

            instance = new DBThreadPoolManager();
        }
        return instance;
    }

    /**
     * 构造函数
     */
    private DBThreadPoolManager()
    {
        MasterConfiguration masterConf = new MasterConfiguration();
        this.maxCoreThreadCount = masterConf.getInt("dbserver.master.thread.coreNum", 1000);
        this.maxThreadCount = masterConf.getInt("dbserver.master.thread.maxNum", 10000);
        this.maxNormalCoreThreadCount = masterConf.getInt("dbserver.master.thread.maxNormalCoreThreadCount", 6000);
        this.maxNormalThreadCount = masterConf.getInt("dbserver.master.thread.maxNormalThreadCount", 60000);
        this.maxScheduleCoreThreadCount = masterConf.getInt("dbserver.master.thread.maxScheduleCoreThreadCount", 3000);
    }

    /**
     * 注册一个周期性线程池
     *
     * @param coreNum                  核心线程数
     * @param factory                  线程池工厂
     * @param rejectedExecutionHandler 拒绝策略
     * @param poolDescription          线程池描述
     * @return 返回周期线程池
     */
    public synchronized ScheduledExecutorService registerScheduleThreadPool(int coreNum, ThreadFactory factory,
                                                                            RejectedExecutionHandler rejectedExecutionHandler,
                                                                            ExecutorType poolDescription)
    {
        if (poolDescription == null)
        {
            LOGGER.info("Must specified a poolDescription for this pool.");
            throw new IllegalArgumentException("Must specified a poolDescription for this pool.");
        }
        allSchedulePoolCoreThreadSum += coreNum;
        if (allSchedulePoolCoreThreadSum > maxScheduleCoreThreadCount)
        {
            LOGGER.info(" The number of allSchedulePoolCoreThreadSum exceeds the number maxScheduleCoreThreadCount.");
            throw new IllegalArgumentException(
                    " The number of allSchedulePoolCoreThreadSum exceeds the number maxScheduleCoreThreadCount.");
        }
        DBScheduledThreadPool scheduledThreadPool = new DBScheduledThreadPool(coreNum, factory,
                rejectedExecutionHandler);
        dbScheduledThreadPool.put(poolDescription, scheduledThreadPool);
        return scheduledThreadPool;
    }

    /**
     * 注册一个周期线程池
     *
     * @param coreNum         核心线程数
     * @param poolDescription 线程池描述
     * @return 周期性线程池
     */
    public synchronized ScheduledExecutorService registerScheduleThreadPool(int coreNum, ExecutorType poolDescription)
    {
        ThreadFactory factory = new ThreadFactory()
        {
            private AtomicInteger atoNum = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r)
            {
                return new Thread(r, THREAD_PREFIX + poolDescription + "-" + atoNum.getAndIncrement());
            }
        };
        return this.registerScheduleThreadPool(coreNum, factory, DEFAULT_HANDLER, poolDescription);
    }

    /**
     * 注册一个普通线程池
     *
     * @param coreNum                  核心线程数
     * @param maxNum                   最大线程数
     * @param maxAlive                 存活时间
     * @param timeUnit                 时间单位
     * @param taskQueue                任务队列
     * @param factory                  线程池工厂
     * @param rejectedExecutionHandler 拒绝策略
     * @param poolDescription          线程池描述
     * @return 普通线程池
     */
    public synchronized ExecutorService registerThreadPool(int coreNum, int maxNum, long maxAlive, TimeUnit timeUnit,
                                                           BlockingQueue<Runnable> taskQueue, ThreadFactory factory,
                                                           RejectedExecutionHandler rejectedExecutionHandler,
                                                           ExecutorType poolDescription)
    {
        if (coreNum > maxCoreThreadCount)
        {
            LOGGER.info("The number of coreNum exceeds the number maxCoreThreadCount.");
            throw new RejectedExecutionException("The number of coreNum exceeds the number maxCoreThreadCount.");
        }
        allNormalPoolCoreThreadSum += coreNum;
        if (allNormalPoolCoreThreadSum > maxNormalCoreThreadCount)
        {
            LOGGER.info("The number of allNormalPoolCoreThreadCount exceeds the number maxNormalCoreThreadCount.");
            throw new RejectedExecutionException(
                    "The number of allNormalPoolCoreThreadCount exceeds the number maxNormalCoreThreadCount.");
        }
        if (maxNum > maxThreadCount)
        {
            LOGGER.info("The number of maxNum exceeds the number maxThreadCount.");
            throw new RejectedExecutionException("The number of maxNum exceeds the number maxThreadCount.");
        }
        allNormalPoolMaxThreadSum += maxNum;
        if (allNormalPoolMaxThreadSum > maxNormalThreadCount)
        {
            LOGGER.info("The number of allNormalPoolThreadSum exceeds the number maxNormalThreadCount.");
            throw new RejectedExecutionException(
                    "The number of allNormalPoolThreadSum exceeds the number maxNormalThreadCount.");
        }
        rejectedExecutionHandler = rejectedExecutionHandler == null ? DEFAULT_HANDLER : rejectedExecutionHandler;

        if (poolDescription == null)
        {
            LOGGER.info("You must specified a poolDescription for this pool.");
            throw new IllegalArgumentException("You must specified a poolDescription for this pool.");
        }
        if (factory == null)
        {
            factory = new ThreadFactory()
            {
                private AtomicInteger atoNum = new AtomicInteger();

                @Override
                public Thread newThread(Runnable r)
                {
                    return new Thread(r, THREAD_PREFIX + poolDescription + "-" + atoNum.getAndIncrement());
                }
            };
        }
        DBThreadPool threadPool = new DBThreadPool(coreNum, maxNum, maxAlive, timeUnit, taskQueue, factory,
                rejectedExecutionHandler);
        dbThreadPool.put(poolDescription, threadPool);
        return threadPool;
    }

    /**
     * 注册一个线程池
     *
     * @param coreNum         核心线程数
     * @param maxNum          最大线程数
     * @param maxAlive        最大存活时间
     * @param taskQueue       任务队列
     * @param timeUnit        存活时间单位
     * @param poolDescription 线程池名称
     * @return 普通线程池
     */
    public synchronized ExecutorService registerThreadPool(int coreNum, int maxNum, long maxAlive, TimeUnit timeUnit,
                                                           BlockingQueue<Runnable> taskQueue,
                                                           ExecutorType poolDescription)
    {
        if (poolDescription == null)
        {
            LOGGER.info("You must specified a poolDescription for threadPool.");
            throw new IllegalArgumentException("You must specified a poolDescription for threadPool.");
        }
        ThreadFactory threadFactory = new ThreadFactory()
        {
            private AtomicInteger atoNum = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r)
            {
                return new Thread(r, THREAD_PREFIX + poolDescription + "-" + atoNum.getAndIncrement());
            }
        };
        return this.registerThreadPool(coreNum, maxNum, maxAlive, timeUnit, taskQueue, threadFactory, DEFAULT_HANDLER,
                poolDescription);
    }

    /**
     * 注册一个普通线程池
     *
     * @param coreNum         核心线程数
     * @param maxNum          最大线程数
     * @param maxAlive        最长存活时间
     * @param taskQueue       任务队列
     * @param timeUnit        存活时间单位
     * @param factory         线程池工厂
     * @param poolDescription 线程池描述
     * @return 普通线程池
     */

    public synchronized ExecutorService registerThreadPool(int coreNum, int maxNum, long maxAlive, TimeUnit timeUnit,
                                                           BlockingQueue<Runnable> taskQueue, ThreadFactory factory,
                                                           ExecutorType poolDescription)
    {
        return this.registerThreadPool(coreNum, maxNum, maxAlive, timeUnit, taskQueue, factory, DEFAULT_HANDLER,
                poolDescription);
    }

    /**
     * 注册一个普通线程池
     *
     * @param coreNum                  核心线程数
     * @param maxNum                   最大线程数
     * @param maxAlive                 最长存活时间
     * @param timeUnit                 存活时间单位
     * @param taskQueue                任务队列
     * @param rejectedExecutionHandler 拒绝策略
     * @param poolDescription          线程池描述
     * @return 普通线程池
     */
    public synchronized ExecutorService registerThreadPool(int coreNum, int maxNum, long maxAlive, TimeUnit timeUnit,
                                                           BlockingQueue<Runnable> taskQueue,
                                                           RejectedExecutionHandler rejectedExecutionHandler,
                                                           ExecutorType poolDescription)
    {
        return this.registerThreadPool(coreNum, maxNum, maxAlive, timeUnit, taskQueue, Executors.defaultThreadFactory(),
                rejectedExecutionHandler, poolDescription);
    }

    /**
     * 注册一个普通线程池
     *
     * @param maxAlive        最长存活时间
     * @param taskQueue       任务队列
     * @param timeUnit        存活时间单位
     * @param poolDescription 线程池描述
     * @return 普通线程池
     */

    public synchronized ExecutorService registerThreadPool(long maxAlive, TimeUnit timeUnit,
                                                           BlockingQueue<Runnable> taskQueue,
                                                           ExecutorType poolDescription)
    {
        return this.registerThreadPool(DEFAULT_CORE_NUM, DEFAULT_MAX_NUM, maxAlive, timeUnit, taskQueue,
                Executors.defaultThreadFactory(), DEFAULT_HANDLER, poolDescription);
    }

    /**
     * 统计普通线程池中任务最大的耗时
     *
     * @return 线程池中最大耗时
     */
    public Map<Map.Entry<ExecutorType, DBThreadPool>, Long> getNormalPoolMaxCost()
    {
        Map<Map.Entry<ExecutorType, DBThreadPool>, Long> normalPoolMaxCost = Maps.newHashMap();
        for (Map.Entry<ExecutorType, DBThreadPool> entries : dbThreadPool.entrySet())
        {
            DBThreadPool threadPool = entries.getValue();
            long maxTaskCost = threadPool.getMaxTaskCost();
            normalPoolMaxCost.put(entries, maxTaskCost);
        }
        return normalPoolMaxCost;
    }

    /**
     * 统计普通线程池中任务最小的耗时
     *
     * @return 普通线程池中任务最小的耗时
     */
    public Map<Map.Entry<ExecutorType, DBThreadPool>, Long> getNormalPoolMinCost()
    {
        Map<Map.Entry<ExecutorType, DBThreadPool>, Long> normalPoolMinCost = Maps.newHashMap();
        for (Map.Entry<ExecutorType, DBThreadPool> entries : dbThreadPool.entrySet())
        {
            DBThreadPool threadPool = entries.getValue();
            long minTaskCost = threadPool.getMinTaskCost();
            normalPoolMinCost.put(entries, minTaskCost);
        }
        return normalPoolMinCost;
    }

    /**
     * 统计普通线程池中任务平均的耗时
     *
     * @return 普通线程池中任务平均的耗时
     */
    public Map<Map.Entry<ExecutorType, DBThreadPool>, Long> getNormalPoolAvgCost()
    {
        Map<Map.Entry<ExecutorType, DBThreadPool>, Long> normalPoolAvgCost = Maps.newHashMap();
        for (Map.Entry<ExecutorType, DBThreadPool> entries : dbThreadPool.entrySet())
        {
            DBThreadPool threadPool = entries.getValue();
            long avgTaskCost = threadPool.getAverageTaskCost();
            normalPoolAvgCost.put(entries, avgTaskCost);
        }
        return normalPoolAvgCost;
    }

    /**
     * 获取当前所有普通线程池及描述
     *
     * @return 当前所有普通线程池及描述
     */
    public Map<ExecutorType, ExecutorService> getAllNormalThreadPool()
    {
        return new HashMap<>(dbThreadPool);
    }

    /**
     * 获取当前所有注册周期性线程池及描述
     *
     * @return 所有注册周期性线程池及描述
     */
    public Map<ExecutorType, ScheduledExecutorService> getAllScheduleThreadPool()
    {
        return new HashMap<>(dbScheduledThreadPool);
    }

    /**
     * 统计周期性线程池中任务最大的耗时
     *
     * @return 周期性线程池中任务最大的耗时
     */
    public Map<Map.Entry<ExecutorType, DBScheduledThreadPool>, Long> getScheduleTaskMaxCost()
    {
        Map<Map.Entry<ExecutorType, DBScheduledThreadPool>, Long> scheduleTaskMaxCost = Maps.newHashMap();
        for (Map.Entry<ExecutorType, DBScheduledThreadPool> entries : dbScheduledThreadPool.entrySet())
        {
            DBScheduledThreadPool scheThreadPool = entries.getValue();
            long maxTaskCost = scheThreadPool.getMaxTaskCost();
            scheduleTaskMaxCost.put(entries, maxTaskCost);
        }
        return scheduleTaskMaxCost;
    }

    /**
     * 统计周期性线程池中任务最小的耗时
     *
     * @return 周期性线程池中任务最小的耗时
     */
    public Map<Map.Entry<ExecutorType, DBScheduledThreadPool>, Long> getScheduleTaskMinCost()
    {
        Map<Map.Entry<ExecutorType, DBScheduledThreadPool>, Long> scheduleTaskMinCost = Maps.newHashMap();
        for (Map.Entry<ExecutorType, DBScheduledThreadPool> entries : dbScheduledThreadPool.entrySet())
        {
            DBScheduledThreadPool threadPool = entries.getValue();
            long minTaskCost = threadPool.getMinTaskCost();
            scheduleTaskMinCost.put(entries, minTaskCost);
        }
        return scheduleTaskMinCost;
    }

    /**
     * 统计周期性线程池中任务平均的耗时
     *
     * @return 周期性线程池中任务平均的耗时
     */
    public Map<Map.Entry<ExecutorType, DBScheduledThreadPool>, Long> getScheduleTaskAvgCost()
    {
        Map<Map.Entry<ExecutorType, DBScheduledThreadPool>, Long> scheduleTaskAvgCost = Maps.newHashMap();
        for (Map.Entry<ExecutorType, DBScheduledThreadPool> entries : dbScheduledThreadPool.entrySet())
        {
            DBScheduledThreadPool threadPool = entries.getValue();
            long avgTaskCost = threadPool.getAverageTaskCost();
            scheduleTaskAvgCost.put(entries, avgTaskCost);
        }
        return scheduleTaskAvgCost;
    }

    /**
     * 停止普通线程池
     */
    public void stopNormalExecutor()
    {
        for (ThreadPoolExecutor poolExecutor : dbThreadPool.values())
        {
            poolExecutor.shutdownNow();
        }
        dbThreadPool.clear();
    }

    /**
     * 停止周期性线程池
     */
    public void stopScheduleExecutor()
    {
        for (ThreadPoolExecutor poolExecutor : dbScheduledThreadPool.values())
        {
            poolExecutor.shutdownNow();
        }
        dbScheduledThreadPool.clear();
    }

}
