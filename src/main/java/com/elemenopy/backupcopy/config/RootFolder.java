/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.elemenopy.backupcopy.config;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Scott
 */
public class RootFolder {
    /**
     * The local (source) path
     */
    private String path;
    
    /**
     * List of paths or patterns to exclude from synchronization.
     */
    private List<String> exclude = new ArrayList<>();
    
    /**
     * Optional. Overrides BackupConfig's remotePath
     */
    private String remotePath;
    
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getExclude() {
        return exclude;
    }

    public void setExclude(List<String> exclude) {
        this.exclude = exclude;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }
}
