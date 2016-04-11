/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.elemenopy.backupcopy.config;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Scott
 */
public class LoadConfigTest {
    
    public LoadConfigTest() {
    }

    @Test
    public void loadDefault() {
        BackupConfig config = BackupConfig.loadDefault();
        assertNotNull(config);
        System.out.println(ToStringBuilder.reflectionToString(config, ToStringStyle.MULTI_LINE_STYLE));
    }
}
