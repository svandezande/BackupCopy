/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.elemenopy.backupcopy.ui;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elemenopy.backupcopy.config.BackupConfig;
import com.elemenopy.backupcopy.filesystem.WatcherManager;

/**
 *
 * @author Scott
 */
public class TrayIconUI {

    private static final Logger logger = LoggerFactory.getLogger(TrayIconUI.class);
    private static BackupConfig config;
    
    public static void main(String[] args) throws MalformedURLException {
        config = BackupConfig.loadDefault();
        if(config == null) {
        	logger.warn("Config not found. Exiting.");
        	System.exit(1);
        }
                
        TrayIcon trayIcon = null;
        if (SystemTray.isSupported()) {
            // get the SystemTray instance
            SystemTray tray = SystemTray.getSystemTray();
            // load an image
            Image image = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemClassLoader().getResource("img/copy_icon.png"));
            // create a action listener to listen for default action executed on the tray icon
            // create a popup menu
            PopupMenu popup = new PopupMenu();
            
            MenuItem pauseItem = new MenuItem("Pause Sync");
            pauseItem.setEnabled(true);
            MenuItem resumeItem = new MenuItem("Resume Sync");
            resumeItem.setEnabled(false);
            
            pauseItem.addActionListener((ActionEvent e) -> {
                WatcherManager.stop();
                resumeItem.setEnabled(true);
                pauseItem.setEnabled(false);
            });
            
            resumeItem.addActionListener((ActionEvent e) -> {
                try {
                    WatcherManager.init(config);
                } catch (IOException ex) {
                    logger.error("IOException while initializing file watcher", ex);
                }
                resumeItem.setEnabled(false);
                pauseItem.setEnabled(true);
            });
            
            popup.add(pauseItem);
            popup.add(resumeItem);
            
            // create menu item for the default action
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    WatcherManager.stop();
                    System.exit(0);
                }
            });
            popup.add(exitItem);

            /// ... add other items

            // construct a TrayIcon
            trayIcon = new TrayIcon(image, "Backup Copy", popup);

            // set the TrayIcon properties

            // add the tray image
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println(e);
            }
            // disable tray option in your application or
            // perform other actions
            //...
            try {
                WatcherManager.init(config);
            } catch (IOException ex) {
                logger.error("IOException while initializing file watcher", ex);
            }
        }

    }
}
