package com.fiberhome.dbserver.common.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitoredTaskImpl implements MonitoredTask
{

    private static final Logger LOG = LoggerFactory.getLogger(MonitoredTaskImpl.class);

    public static final String MASTER_LOG_PREFIX = "##";

    private long startTime;
    private long statusTime;
    private long stateTime;

    private volatile String status;
    private volatile String description;

    protected volatile State state = State.RUNNING;

    /**
     * 构造函数
     */
    public MonitoredTaskImpl()
    {
        startTime = System.currentTimeMillis();
        statusTime = startTime;
        stateTime = startTime;
    }

    @Override
    public synchronized MonitoredTaskImpl clone()
    {
        try
        {
            return (MonitoredTaskImpl) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new AssertionError(); // Won't happen
        }
    }

    @Override
    public long getStartTime()
    {
        return startTime;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public String getStatus()
    {
        return status;
    }

    @Override
    public long getStatusTime()
    {
        return statusTime;
    }

    @Override
    public State getState()
    {
        return state;
    }

    @Override
    public long getStateTime()
    {
        return stateTime;
    }

    @Override
    public long getCompletionTimestamp()
    {
        if (state == State.COMPLETE || state == State.ABORTED)
        {
            return stateTime;
        }
        return -1;
    }

    @Override
    public void markComplete(String status)
    {
        setState(State.COMPLETE);
        setStatus(status);
    }

    @Override
    public void pause(String msg)
    {
        setState(State.WAITING);
        setStatus(msg);
    }

    @Override
    public void resume(String msg)
    {
        setState(State.RUNNING);
        setStatus(msg);
    }

    @Override
    public void abort(String msg)
    {
        setStatus(msg);
        setState(State.ABORTED);
    }

    @Override
    public void setStatus(String status)
    {
        this.status = status;
        statusTime = System.currentTimeMillis();
        LOG.info(MASTER_LOG_PREFIX + status);
    }

    protected void setState(State state)
    {
        this.state = state;
        stateTime = System.currentTimeMillis();
        LOG.info(MASTER_LOG_PREFIX + state);
    }

    @Override
    public void setDescription(String description)
    {
        this.description = description;
        LOG.info(description);
    }

    @Override
    public void cleanup()
    {
        if (state == State.RUNNING)
        {
            setState(State.ABORTED);
        }
    }

    /**
     * Force the completion timestamp backwards so that it expires now.
     */
    public void expireNow()
    {
        stateTime -= 180 * 1000;
    }

    @Override
    public Map<String, Object> toMap()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("description", getDescription());
        map.put("status", getStatus());
        map.put("state", getState());
        map.put("starttimems", getStartTime());
        map.put("statustimems", getCompletionTimestamp());
        map.put("statetimems", getCompletionTimestamp());
        return map;
    }

    @Override
    public String toJson() throws IOException
    {
        // return MAPPER.writeValueAsString(toMap());
        return null;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(512);
        sb.append(getDescription());
        sb.append(": status=");
        sb.append(getStatus());
        sb.append(", state=");
        sb.append(getState());
        sb.append(", startTime=");
        sb.append(getStartTime());
        sb.append(", completionTime=");
        sb.append(getCompletionTimestamp());
        return sb.toString();
    }

}
