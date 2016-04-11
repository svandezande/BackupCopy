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
public class RootFolderSynchronizerTest {
    
    private BackupConfig config;
    
    public RootFolderSynchronizerTest() {
    }
    
    @Before
    public void setUp() {
        config = BackupConfig.loadFromClasspath("backup-config.json");
    }

    @Test
    public void testSync() throws Exception {
        RootFolderSynchronizer sync = new RootFolderSynchronizer(config.getRemotePath(), config.getRootFolders().get(0));
        sync.synchronize();
    }
}
