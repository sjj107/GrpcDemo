package com.fiberhome.dbserver.common.executor;

import static com.fiberhome.dbserver.common.executor.ExecutorType.DEFAULT_POOL;
import static com.fiberhome.dbserver.common.executor.ExecutorType.SCHEDULE_THREAD_POOL;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fiberhome.dbserver.common.config.MasterConfiguration;
import com.fiberhome.dbserver.common.executor.monitorexecutor.DBThreadPoolManager;
import com.fiberhome.dbserver.common.executor.payload.CyclePayload;
import com.fiberhome.dbserver.common.executor.payload.DelayedPayload;
import com.fiberhome.dbserver.common.executor.payload.Payload;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * <p>
 * 执行服务的包装类，主要定义系统需要的执行服务并且按照不同的类型存储
 * </p>
 *
 * @author lizhen
 * @date 2020/11/06
 */
public class DBExecutorManager
{
    private static final Logger LOG = LoggerFactory.getLogger(DBExecutorManager.class);

    private static final DBThreadPoolManager POOL_MANAGER = DBThreadPoolManager.getInstance();
    private static final String EXECUTOR_NOT_EXIST = "EXECUTOR_NOT_EXIST ";
    private static final String RETURN_DEFAULT_POOL = "will return default pool";

    // 普通线程池集合
    private Map<ExecutorType, ListeningExecutorService> normalThreadPoolMap = new ConcurrentHashMap<>();
    // 周期性线程池集合
    private ListeningScheduledExecutorService scheduledThreadPool;

    private static class Holder
    {
        private static final DBExecutorManager DB_EXECUTOR_SERVICE = new DBExecutorManager();
    }

    private DBExecutorManager()
    {
        MasterConfiguration conf = MasterConfiguration.getMasterConf();
        initScheduledThreadPool(conf);
        initDefaultPool(conf);
        initArchivePool(conf);
        initCompactPool(conf);
    }

    /**
     * 初始化归档服务线程池
     * @param conf 配置文件
     */
    private void initArchivePool(MasterConfiguration conf)
    {
        final int coreNum = conf.getInt("executorservice.archive.threadnum.core", 100);
        final int maxNum = conf.getInt("executorservice.archive.threadnum.max", 100);
        final int queueSize = conf.getInt("executorservice.archive.queue.size", 100);
        final long aliveTime = conf.getLong("executorservice.archive.alivetime", 1000);
        final boolean isDaemon = conf.getBoolean("executorservice.archive.threadnum.daemon", true);
        BlockingQueue<Runnable> queue = getBlockingQueue(queueSize);
        ExecutorService executorService = POOL_MANAGER
                .registerThreadPool(coreNum, maxNum, aliveTime, TimeUnit.MILLISECONDS, queue, r ->
                {
                    Thread thread = new Thread(r);
                    thread.setName("archive-" + UUID.randomUUID().toString());
                    thread.setDaemon(isDaemon);
                    return thread;
                }, ExecutorType.ARCHIVE_THREAD_POOL);
        ListeningExecutorService service = MoreExecutors.listeningDecorator(executorService);
        if (normalThreadPoolMap.containsKey(ExecutorType.ARCHIVE_THREAD_POOL))
        {
            LOG.error("Duplicate register thread pool. {}", ExecutorType.ARCHIVE_THREAD_POOL);
            throw new IllegalStateException("Duplicate register thread pool. " + ExecutorType.ARCHIVE_THREAD_POOL);
        }
        normalThreadPoolMap.put(ExecutorType.ARCHIVE_THREAD_POOL, service);
    }

    /**
     * 获取缓存队列
     * @param queueSize 队列长度
     * @return 缓存队列，队列长度-1时返回换手队列
     */
    private BlockingQueue<Runnable> getBlockingQueue(int queueSize)
    {
        BlockingQueue<Runnable> queue;
        if (queueSize <= 0)
        {
            queue = new SynchronousQueue<>();
        }
        else
        {
            queue = new LinkedBlockingQueue<>(queueSize);
        }
        return queue;
    }

    /**
     * 初始化合并任务线程池
     * @param conf 配置文件
     */
    private void initCompactPool(MasterConfiguration conf)
    {
        final int coreNum = conf.getInt("executorservice.compact.threadnum.core", 100);
        final int maxNum = conf.getInt("executorservice.compact.threadnum.max", 100);
        final int queueSize = conf.getInt("executorservice.compact.queue.size", 100);
        final long aliveTime = conf.getLong("executorservice.compact.alivetime", 1000);
        final boolean isDaemon = conf.getBoolean("executorservice.compact.threadnum.daemon", true);
        BlockingQueue<Runnable> queue = getBlockingQueue(queueSize);
        ExecutorService executorService = POOL_MANAGER
                .registerThreadPool(coreNum, maxNum, aliveTime, TimeUnit.MILLISECONDS, queue, r ->
                {
                    Thread thread = new Thread(r);
                    thread.setName("compact-" + UUID.randomUUID().toString());
                    thread.setDaemon(isDaemon);
                    return thread;
                }, ExecutorType.COMPACT_TASK_POOL);
        ListeningExecutorService service = MoreExecutors.listeningDecorator(executorService);
        if (normalThreadPoolMap.containsKey(ExecutorType.COMPACT_TASK_POOL))
        {
            LOG.error("Duplicate register thread pool. {}", ExecutorType.COMPACT_TASK_POOL);
            throw new IllegalStateException("Duplicate register thread pool. " + ExecutorType.COMPACT_TASK_POOL);
        }
        normalThreadPoolMap.put(ExecutorType.COMPACT_TASK_POOL, service);
    }

    /**
     * 初始化默认线程池
     *
     * @param conf 配置文件
     */
    private void initDefaultPool(MasterConfiguration conf)
    {
        final int coreNum = conf.getInt("executorservice.defaultpool.threadnum.core", 100);
        final int maxNum = conf.getInt("executorservice.defaultpool.threadnum.max", 100);
        final int queueSize = conf.getInt("executorservice.defaultpool.queue.size", 100);
        final long aliveTime = conf.getLong("executorservice.defaultpool.alivetime", 1000);
        final boolean isDaemon = conf.getBoolean("executorservice.defaultpool.threadnum.daemon", true);
        BlockingQueue<Runnable> queue = getBlockingQueue(queueSize);
        ExecutorService executorService = POOL_MANAGER
                .registerThreadPool(coreNum, maxNum, aliveTime, TimeUnit.MILLISECONDS, queue, r ->
                {
                    Thread thread = new Thread(r);
                    thread.setName("default-" + UUID.randomUUID().toString());
                    thread.setDaemon(isDaemon);
                    return thread;
                }, ExecutorType.DEFAULT_POOL);
        ListeningExecutorService service = MoreExecutors.listeningDecorator(executorService);
        if (normalThreadPoolMap.containsKey(ExecutorType.DEFAULT_POOL))
        {
            LOG.error("Duplicate register thread pool. {}", ExecutorType.DEFAULT_POOL);
            throw new IllegalStateException("Duplicate register thread pool. " + ExecutorType.DEFAULT_POOL);
        }
        normalThreadPoolMap.put(ExecutorType.DEFAULT_POOL, service);
    }

    /**
     * 初始化周期性线程池
     *
     * @param conf 配置文件
     */
    private void initScheduledThreadPool(MasterConfiguration conf)
    {
        final int coreNum = conf.getInt("executorservice.schedule.threadnum.core", 100);
        final boolean isDaemon = conf.getBoolean("executorservice.schedule.threadnum.daemon", true);
        ScheduledExecutorService scheduledPool = POOL_MANAGER.registerScheduleThreadPool(coreNum, r ->
        {
            Thread thread = new Thread(r);
            thread.setName("schedule-" + UUID.randomUUID().toString());
            thread.setDaemon(isDaemon);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy(), SCHEDULE_THREAD_POOL);
        scheduledThreadPool = MoreExecutors.listeningDecorator(scheduledPool);
    }

    /**
     * 获取单体实例
     *
     * @return 执行服务
     */
    public static DBExecutorManager getInstance()
    {
        return Holder.DB_EXECUTOR_SERVICE;
    }

    /**
     * <p>
     * 根据任务获取对应的执行server
     * </p>
     *
     * @param payload 任务
     * @return 执行服务
     */
    public ExecutorService getExecutor(Payload payload)
    {
        ExecutorType executorType = payload.getServiceType();

        // 选择对应的提交池
        ExecutorService executor = normalThreadPoolMap.get(executorType);

        if (executor == null)
        {
            LOG.error(EXECUTOR_NOT_EXIST + " for {}, " + RETURN_DEFAULT_POOL, payload);
            executor = normalThreadPoolMap.get(DEFAULT_POOL);
        }
        return executor;
    }

    /**
     * <p>
     * 根据任务类型获取对应的执行服务
     * </p>
     *
     * @param executorType 任务类型
     * @return 执行服务
     */
    public ExecutorService getExecutor(ExecutorType executorType)
    {
        // 选择对应的提交池
        ExecutorService executor = normalThreadPoolMap.get(executorType);

        if (executor == null)
        {
            LOG.error(EXECUTOR_NOT_EXIST + " for {}, " + RETURN_DEFAULT_POOL, executorType);
            executor = normalThreadPoolMap.get(DEFAULT_POOL);
        }
        return executor;
    }

    /**
     * 获取所有执行类型和执行服务映射
     *
     * @return 任务类型和执行服务映射
     */
    public Map<ExecutorType, ListeningExecutorService> getNormalThreadPoolMap()
    {
        return normalThreadPoolMap;
    }

    public ListeningScheduledExecutorService getScheduledThreadPool()
    {
        return scheduledThreadPool;
    }

    /**
     * <p>
     * 关闭对应所有的server执行器
     * </p>
     */
    public void shutdown()
    {
        normalThreadPoolMap.values().forEach(ExecutorService::shutdownNow);
        scheduledThreadPool.shutdownNow();
    }

    /**
     * <p>
     * 向执行服务提交任务，该接口提交的是一阶段任务，提交即可返回句柄供后续获取结果
     * </p>
     * <p>
     * 执行服务会根据提交的任务类型决定提交的执行池
     * </p>
     *
     * @param <V>     任务泛型
     * @param payload 封装好的任务
     * @return 任务提交执行的句柄，后续用于获取数据
     */
    public <V> ListenableFuture<V> submit(final Payload<V> payload)
    {
        ExecutorType executorType = payload.getServiceType();

        // 选择对应的提交池
        ListeningExecutorService executor = normalThreadPoolMap.get(executorType);

        if (executor == null)
        {
            LOG.error(EXECUTOR_NOT_EXIST + " for : {}, " + RETURN_DEFAULT_POOL, payload);
            executor = normalThreadPoolMap.get(DEFAULT_POOL);
        }

        return executor.submit(payload);

    }

    /**
     * <p>
     * 提交任务，该接口提交的多阶段任务，可以通过guava异步函数将多级任务串联执行
     * </p>
     *
     * @param <R>      任务泛型
     * @param <V>      结果泛型
     * @param payload  提交的任务
     * @param function 异步函数，主要用于一级任务提交后得到的结果需要在转换情况
     * @return 转换后的句柄
     */
    public <R, V> ListenableFuture<R> submit(final Payload<V> payload, AsyncFunction<? super V, R> function)
    {
        ExecutorType executorType = payload.getServiceType();

        // 选择对应的提交池
        ListeningExecutorService executor = normalThreadPoolMap.get(executorType);

        if (executor == null)
        {
            LOG.error(EXECUTOR_NOT_EXIST + " for : {}, " + RETURN_DEFAULT_POOL, payload);
            executor = normalThreadPoolMap.get(DEFAULT_POOL);
        }

        // 提交一级任务
        ListenableFuture<V> future = executor.submit(payload);

        // 转换函数
        return Futures.transformAsync(future, function, normalThreadPoolMap.get(ExecutorType.DEFAULT_POOL));
    }

    /**
     * 提交一次性延时任务
     * @param payload 延时任务
     * @param <V> 任务返回结果
     * @return 任务future
     */
    public <V> ListenableFuture<V> submitDelayTask(final DelayedPayload<V> payload)
    {
        return scheduledThreadPool.schedule(payload, payload.getDelayedTime(), TimeUnit.MILLISECONDS);
    }

    /**
     * 提交周期任务
     * @param payload 周期任务
     */
    public void submitCycleTask(final CyclePayload payload)
    {
        if (payload.isFixedRate())
        {
            scheduledThreadPool
                    .scheduleAtFixedRate(payload, payload.getDelayedTime(), payload.getPeriod(), TimeUnit.MILLISECONDS);
        }
        else
        {
            scheduledThreadPool.scheduleWithFixedDelay(payload, payload.getDelayedTime(), payload.getPeriod(),
                    TimeUnit.MILLISECONDS);
        }
    }
}
