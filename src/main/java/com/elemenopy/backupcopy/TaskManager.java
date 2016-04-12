/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.elemenopy.backupcopy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Scott
 */
public class TaskManager {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(30);
    
    public static void runTask(Runnable runnable) {
        executorService.submit(runnable);
    }
}
