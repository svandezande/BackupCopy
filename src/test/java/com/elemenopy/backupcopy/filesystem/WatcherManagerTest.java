/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.elemenopy.backupcopy.filesystem;

import com.elemenopy.backupcopy.config.BackupConfig;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Scott
 */
public class WatcherManagerTest {
    
    private BackupConfig config;
    
    @Before
    public void setUp() {
        config = BackupConfig.loadFromClasspath("backup-config.json");
    }

    @Test
    public void testWatchInit() throws Exception {
        WatcherManager.init(config);
        System.out.println("WatcherManager.init() returned");
        Thread.sleep(60000);
        WatcherManager.stop();
        Thread.sleep(10000);
    }
}
