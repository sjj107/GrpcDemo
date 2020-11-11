package com.fiberhome.dbserver.common.services;

import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class TaskMonitor
{

    public static final int MAX_TASKS = 1000;
    private static final Logger LOG = LoggerFactory.getLogger(TaskMonitor.class);
    // Don't keep around any tasks that have completed more than
    // 60 seconds ago
    private static final long EXPIRATION_TIME = 60 * 1000L;
    private static TaskMonitor instance;
    private CircularFifoBuffer tasks = new CircularFifoBuffer(MAX_TASKS);

    /**
     * Get singleton instance.
     *
     * @return TaskMonitor
     */
    public static synchronized TaskMonitor get()
    {
        if (instance == null)
        {
            instance = new TaskMonitor();
        }
        return instance;
    }

    /**
     * 创建状态监控
     *
     * @param description 描述信息
     * @return TaskMonitor
     */
    public synchronized MonitoredTask createStatus(String description)
    {
        MonitoredTask stat = new MonitoredTaskImpl();
        stat.setStatus(description);
        MonitoredTask proxy = (MonitoredTask) Proxy
                .newProxyInstance(stat.getClass().getClassLoader(), new Class<?>[]{MonitoredTask.class},
                        new PassthroughInvocationHandler<MonitoredTask>(stat));
        TaskAndWeakRefPair pair = new TaskAndWeakRefPair(stat, proxy);
        tasks.add(pair);
        return proxy;
    }

    private synchronized void purgeExpiredTasks()
    {
        for (@SuppressWarnings("unchecked") Iterator<TaskAndWeakRefPair> it = tasks.iterator(); it.hasNext(); )
        {
            TaskAndWeakRefPair pair = it.next();
            MonitoredTask stat = pair.get();

            if (pair.isDead())
            {
                // The class who constructed this leaked it. So we can
                // assume it's done.
                if (stat.getState() == MonitoredTaskImpl.State.RUNNING)
                {
                    LOG.warn("Status " + stat + " appears to have been leaked");
                    stat.cleanup();
                }
            }

            if (canPurge(stat))
            {
                it.remove();
            }
        }
    }

    /**
     * Produces a list containing copies of the current state of all non-expired
     * MonitoredTasks handled by this TaskMonitor.
     *
     * @return A complete list of MonitoredTasks.
     */
    public synchronized List<MonitoredTask> getTasks()
    {
        purgeExpiredTasks();
        ArrayList<MonitoredTask> ret = Lists.newArrayListWithCapacity(tasks.size());
        for (@SuppressWarnings("unchecked") Iterator<TaskAndWeakRefPair> it = tasks.iterator(); it.hasNext(); )
        {
            TaskAndWeakRefPair pair = it.next();
            MonitoredTask t = pair.get();
            ret.add(t.clone());
        }
        return ret;
    }

    private boolean canPurge(MonitoredTask stat)
    {
        long cts = stat.getCompletionTimestamp();
        return (cts > 0 && System.currentTimeMillis() - cts > EXPIRATION_TIME);
    }

    /**
     * 写出为文本
     *
     * @param out 输出流
     */
    public void dumpAsText(PrintWriter out)
    {
        long now = System.currentTimeMillis();

        List<MonitoredTask> tasks = getTasks();
        for (MonitoredTask task : tasks)
        {
            out.println("Task: " + task.getDescription());
            out.println("Status: " + task.getState() + ":" + task.getStatus());
            long running = (now - task.getStartTime()) / 1000;
            if (task.getCompletionTimestamp() != -1)
            {
                long completed = (now - task.getCompletionTimestamp()) / 1000;
                out.println("Completed " + completed + "s ago");
                out.println("Ran for " + (task.getCompletionTimestamp() - task.getStartTime()) / 1000 + "s");
            }
            else
            {
                out.println("Running for " + running + "s");
            }
            out.println();
        }
    }

    /**
     * This class encapsulates an object as well as a weak reference to a proxy
     * that passes through calls to that object. In art form:  Proxy
     * <------------------ | \ v \ PassthroughInvocationHandler | weak reference
     * | / MonitoredTaskImpl / | / StatAndWeakRefProxy ------/
     * Since we only return the Proxy to the creator of the MonitorableStatus,
     * this means that they can leak that object, and we'll detect it since our
     * weak reference will go null. But, we still have the actual object, so we
     * can log it and display it as a leaked (incomplete) action.
     */
    private static class TaskAndWeakRefPair
    {
        private MonitoredTask impl;
        private WeakReference<MonitoredTask> weakProxy;

        public TaskAndWeakRefPair(MonitoredTask stat, MonitoredTask proxy)
        {
            this.impl = stat;
            this.weakProxy = new WeakReference<MonitoredTask>(proxy);
        }

        public MonitoredTask get()
        {
            return impl;
        }

        public boolean isDead()
        {
            return weakProxy.get() == null;
        }
    }

    /**
     * An InvocationHandler that simply passes through calls to the original
     * object.
     */
    private static class PassthroughInvocationHandler<T> implements InvocationHandler
    {
        private T delegatee;

        public PassthroughInvocationHandler(T delegatee)
        {
            this.delegatee = delegatee;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            return method.invoke(delegatee, args);
        }
    }
}
