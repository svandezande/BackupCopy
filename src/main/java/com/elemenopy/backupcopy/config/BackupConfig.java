/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.elemenopy.backupcopy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Scott
 */
public class BackupConfig {

    private static final String DEFAULT_CONFIG_FILE = "backup-config.json";

    private static final Logger logger = LoggerFactory.getLogger(BackupConfig.class);
    
    private List<RootFolder> rootFolders = new ArrayList<>();
    
    private String remotePath;

    public static BackupConfig loadFromClasspath(String fname) {
        return loadConfigFile(ClassLoader.getSystemClassLoader().getResourceAsStream(fname));
    }

    public static BackupConfig loadFromFileSystem(String path) {
        try {
            return loadConfigFile(new FileInputStream(path));
        } catch (FileNotFoundException ex) {
            logger.error("Config file {} not found!", path);
            return null;
        }
    }

    public static BackupConfig loadDefault() {
        logger.info("Looking for {} at root of classpath...", DEFAULT_CONFIG_FILE);
        BackupConfig config = loadFromClasspath(DEFAULT_CONFIG_FILE);
        if (config == null) {
            File home = new File(System.getProperty("user.home"));
            logger.info("Config not found in classpath. Looking in {}...", home.getAbsolutePath());
            config = loadFromFileSystem(new File(home, DEFAULT_CONFIG_FILE).getAbsolutePath());
        }
        if(config == null) logger.warn("Config file {} not found at classpath root or in user home directory.", DEFAULT_CONFIG_FILE);
        return config;
    }

    private static BackupConfig loadConfigFile(InputStream in) {
        try {
            ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
            BackupConfig config = mapper.readValue(in, BackupConfig.class);
            return config;
        } catch (IOException ex) {
            logger.error("IO Exception while loading config file", ex);
            return null;
        }
    }

    public List<RootFolder> getRootFolders() {
        return rootFolders;
    }

    public void setRootFolders(List<RootFolder> rootFolders) {
        this.rootFolders = rootFolders;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }
}
