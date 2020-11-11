package com.fiberhome.dbserver.common.executor.payload;

import java.util.concurrent.Callable;

import com.fiberhome.dbserver.common.exception.DBException;
import com.fiberhome.dbserver.common.executor.ExecutorType;

/**
 * 该类为一个抽象类，主要用来定义任务的抽象方法
 *
 * @author xiajunsheng
 * @date 2020/10/20
 * @since 1.0.0
 */
public abstract class Payload<R> implements Callable<R>
{


    private ExecutorType executorType;

    /**
     * <p>
     * 任务执行前的准备工作，主要用来做一些检查工作
     * </p>
     *
     * @return 返回任然是这个task，只是做一些简单的校验工作
     */
    public Payload<R> prepare()
    {
        return this;
    }

    public Payload(ExecutorType executorType)
    {
        this.executorType = executorType;
    }

    /**
     * <p>
     * callable的构造函数
     * </p>
     *
     * @return 返回该任务的泛型R
     * @throws Exception 异常
     */
    public final R call() throws Exception
    {
        return process();
    }

    /**
     * <p>
     * 该任务的主业务逻辑，任务task必须将该函数重新实现。定义自己的业务逻辑
     * </p>
     *
     * @return 该任务执行后的返回结果
     * @throws DBException 异常
     */
    public abstract R process() throws DBException;


    public ExecutorType getServiceType()
    {
        return executorType;
    }
}
