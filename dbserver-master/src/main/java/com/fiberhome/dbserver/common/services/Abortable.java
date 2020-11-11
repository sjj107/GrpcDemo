package com.fiberhome.dbserver.common.services;

public interface Abortable
{

    /**
     * Abort the server or client.
     * 
     * @param why Why we're aborting.
     * @param e Throwable that caused abort. Can be null.
     */
    void abort(String why, Throwable e);

    /**
     * Check if the server or client was aborted.
     * 
     * @return true if the server or client was aborted, false otherwise
     */
    boolean isAborted();

}
