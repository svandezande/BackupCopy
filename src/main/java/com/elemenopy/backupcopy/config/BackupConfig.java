/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.elemenopy.backupcopy.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    	InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(fname);
    	if(is == null) return null;
        return loadConfigFile(is);
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
            File home = FileSystems.getDefault().getPath(System.getProperty("user.home"), ".backupCopy").toFile();
            logger.info("Config not found in classpath. Looking in {}...", home.getAbsolutePath());
            config = loadFromFileSystem(new File(home, DEFAULT_CONFIG_FILE).getAbsolutePath());
        }
        if(config == null) {
            File workingDir = new File(System.getProperty("user.dir"));
            logger.info("Config not found in user home. Looking in {}...", workingDir.getAbsolutePath());
            config = loadFromFileSystem(new File(workingDir, DEFAULT_CONFIG_FILE).getAbsolutePath());
        }
        if(config == null) logger.warn("Config file {} not found at classpath root, working dir, or in user home directory.", DEFAULT_CONFIG_FILE);
        return config;
    }

    private static BackupConfig loadConfigFile(InputStream in) {
    	if(in == null) return null;
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
