package com.fiberhome.dbserver.common.services;

import java.io.IOException;
import java.util.Map;

/**
 *  Master Monitor
 */
public interface MonitoredTask extends Cloneable
{

    enum State
    {
        RUNNING, WAITING, COMPLETE, ABORTED;
    }

    long getStartTime();

    String getDescription();

    String getStatus();

    long getStatusTime();

    State getState();

    long getStateTime();

    long getCompletionTimestamp();

    void markComplete(String msg);

    void pause(String msg);

    void resume(String msg);

    void abort(String msg);

    void expireNow();

    void setStatus(String status);

    void setDescription(String description);

    /**
     * Explicitly mark this status as able to be cleaned up, even though it
     * might not be complete.
     */
    void cleanup();

    /**
     * Public exposure of Object.clone() in order to allow clients to easily
     * capture current state.
     * 
     * @return a copy of the object whose references will not change
     */
    MonitoredTask clone();

    /**
     * Creates a string map of internal details for extensible exposure of
     * monitored tasks.
     * @return A Map containing information for this task.
     * @throws IOException 异常信息
     */
    Map<String, Object> toMap() throws IOException;

    /**
     * Creates a JSON object for parseable exposure of monitored tasks.
     * 
     * @return An encoded JSON object containing information for this task.
     * @throws IOException 异常信息
     */
    String toJson() throws IOException;

}
