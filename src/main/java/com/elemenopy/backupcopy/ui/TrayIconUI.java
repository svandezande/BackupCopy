/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.elemenopy.backupcopy.ui;

import com.elemenopy.backupcopy.config.BackupConfig;
import com.elemenopy.backupcopy.filesystem.WatcherManager;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Scott
 */
public class TrayIconUI {

    private static final Logger logger = LoggerFactory.getLogger(TrayIconUI.class);
    private static BackupConfig config;
    
    public static void main(String[] args) throws MalformedURLException {
        try {
            Path configFile = FileSystems.getDefault().getPath(System.getProperty("user.home"), ".backupCopy", "backup-config.json");
            if(!Files.exists(configFile)) {
                Files.createDirectories(configFile.getParent());
            }
            config = BackupConfig.loadFromFileSystem(configFile.toString());
        } catch(IOException e) {
            logger.error("IOException while loading config file", e);
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
