package com.fiberhome.dbserver.slave.heartbeat;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class HeartBeatServiceTest
{

    @Test
    public void getInstance()
    {
        HeartBeatService instance = HeartBeatService.getInstance();
        Assert.assertTrue(instance != null);
    }
}