package com.fiberhome.dbserver.common.services;

public interface Stoppable
{

    /**
     * Stop this service.
     * 
     * @param why Why we're stopping.
     */
    void stop(String why);

    /**
     * 是停止吗
     * @return True if {@link #stop(String)} has been closed.
     */
    boolean isStopped();

}
