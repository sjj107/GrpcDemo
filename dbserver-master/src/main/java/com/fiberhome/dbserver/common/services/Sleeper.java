package com.fiberhome.dbserver.common.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sleeper
{
    private static final Logger LOG = LoggerFactory.getLogger(Sleeper.class);
    private final long period;
    private final Stoppable stopper;
    private static final long MINIMAL_DELTA_FOR_LOGGING = 10000;

    private final Object sleepLock = new Object();
    private boolean triggerWake = false;

    /**
     * 构造函数
     * @param sleep sleep time in milliseconds
     * @param stopper When {@link Stoppable#isStopped()} is true, this thread
     *            will cleanup and exit cleanly.
     */
    public Sleeper(final long sleep, final Stoppable stopper)
    {
        this.period = sleep;
        this.stopper = stopper;
    }



    /**
     * If currently asleep, stops sleeping; if not asleep, will skip the next
     * sleep cycle.
     */
    public void skipSleepCycle()
    {
        synchronized (sleepLock)
        {
            triggerWake = true;
            sleepLock.notifyAll();
        }
    }

    /**
     * Sleep for period.
     */
    public void sleep()
    {
        sleep(System.currentTimeMillis());
    }
    
    /**
     * Sleep for period adjusted by passed <code>startTime</code>
     * 
     * @param startTime Time some task started previous to now. Time to sleep
     *            will be docked current time minus passed <code>startTime
     *            </code>.
     */
    public void sleep(final long startTime)
    {
        if (this.stopper.isStopped())
        {
            return;
        }
        long now = System.currentTimeMillis();
        long waitTime = this.period - (now - startTime);
        if (waitTime > this.period)
        {
            LOG.warn("Calculated wait time > " + this.period + "; setting to this.period: " + System.currentTimeMillis()
                + ", " + startTime);
            waitTime = this.period;
        }
        while (waitTime > 0)
        {
            long woke = -1;
            try
            {
                synchronized (sleepLock)
                {
                    if (triggerWake)
                    {
                        break;
                    }
                    sleepLock.wait(waitTime);
                }
                woke = System.currentTimeMillis();
                long slept = woke - now;
                if (slept - this.period > MINIMAL_DELTA_FOR_LOGGING)
                {
                    LOG.warn("We slept " + slept + "ms instead of " + this.period + "ms, this is likely due to a long "
                        + "garbage collecting pause and it's usually bad");
                }
            }
            catch (InterruptedException ex)
            {
                // We we interrupted because we're meant to stop? If not, just
                // continue ignoring the interruption
                if (this.stopper.isStopped())
                {
                    return;
                }
                Thread.currentThread().interrupt();
            }
            // Recalculate waitTime.
            woke = (woke == -1) ? System.currentTimeMillis() : woke;
            waitTime = this.period - (woke - startTime);
        }
        triggerWake = false;
    }

}
