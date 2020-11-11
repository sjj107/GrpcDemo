package com.fiberhome.dbserver.common.executor;

/**
 * 定义DBServer支持的执行池的业务类型
 * 主要分为3类：Client、Master、Server
 * 
 * @author xiajunsheng
 * @date 2020/10/20
 * @since 1.0.0
 */
public enum ExecutorType
{

    DEFAULT_POOL, // 默认线程池，主要用于任务收集和回调
    SCHEDULE_THREAD_POOL, // 周期性线程池，只允许注册一个
    ARCHIVE_THREAD_POOL, // 归档服务线程池
    COMPACT_TASK_POOL // 合并任务线程池
}
