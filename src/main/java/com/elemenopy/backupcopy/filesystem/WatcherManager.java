/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.elemenopy.backupcopy.filesystem;

import com.elemenopy.backupcopy.TaskManager;
import com.elemenopy.backupcopy.config.BackupConfig;
import com.elemenopy.backupcopy.config.RootFolder;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.StandardCopyOption.*;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Scott
 */
public class WatcherManager {

    private static final Logger logger = LoggerFactory.getLogger(WatcherManager.class);

    private static boolean stopped = false;
    private static final FileSystem fileSystem = FileSystems.getDefault();
    private static BackupConfig backupConfig;

    public static void stop() {
        stopped = true;
    }
    
    public static boolean isRunning() {
        return !stopped;
    }

    public static void init(BackupConfig config) throws IOException {
        backupConfig = config;
        
        for (RootFolder rootFolder : config.getRootFolders()) {

            WatchService watcher = fileSystem.newWatchService();

            //start monitoring the source folder
            TaskManager.runTask(new WatcherTask(rootFolder, watcher));

            TaskManager.runTask(() -> {
                //traverse the source folder tree, registering subfolders on the watcher
                final WatcherRegisteringFileVisitor visitor = new WatcherRegisteringFileVisitor(rootFolder, watcher);

                try {
                    EnumSet<FileVisitOption> opts = EnumSet.of(FOLLOW_LINKS);
                    Files.walkFileTree(fileSystem.getPath(rootFolder.getPath()), opts, Integer.MAX_VALUE, visitor);
                } catch (IOException ex) {
                    logger.error("Error while setting up directory watchers", ex);
                }
            });
        }

    }

    private static class WatcherTask implements Runnable {

        private final RootFolder rootFolder;
        private final Path rootParent;
        private final WatchService watcher;
        private final Path remoteRootPath;

        public WatcherTask(RootFolder rootFolder, WatchService watcher) {
            this.rootFolder = rootFolder;
            this.watcher = watcher;
            this.rootParent = fileSystem.getPath(rootFolder.getPath()).getParent();
            String remoteRoot = StringUtils.isBlank(rootFolder.getRemotePath()) ? backupConfig.getRemotePath() : rootFolder.getRemotePath();
            remoteRootPath = fileSystem.getPath(remoteRoot);
        }

        @Override
        public void run() {
            try {
                while (!stopped) {
                    // wait for key to be signaled
                    WatchKey key;
                    try {
                        key = watcher.poll(5000, TimeUnit.MILLISECONDS);
                        if (key == null) {
                            logger.debug("Poll: No file changes detected.");
                            continue;
                        }
                    } catch (InterruptedException x) {
                        logger.debug("Poll: interrupted.");
                        return;
                    }

                    eventLoop : for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path dir = (Path) key.watchable();

                        // An OVERFLOW event can
                        // occur regardless if events
                        // are lost or discarded.
                        if (kind == OVERFLOW) {
                            logger.info("Overflow event detected");
                            continue;
                        }

                        // The filename is the
                        // context of the event.
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();
                        Path fullPath = dir.resolve(filename);
                        Path relPath = rootParent.relativize(fullPath);
                        
                        for(String regex : rootFolder.getExclude()) {
                            if(Pattern.matches(regex, filename.toString()) || Pattern.matches(regex, relPath.toString())) {
                                logger.debug("Skipping path {} due to exclusion pattern {}", relPath, regex);
                                continue eventLoop;
                            }
                        }
                        
                        logger.debug("Processing {} event for {}", kind, relPath);
                        Path fullRemotePath = remoteRootPath.resolve(relPath);

                        if (Files.isDirectory(fullPath)) {
                            if (ENTRY_CREATE.equals(kind)) {
                                logger.debug("Creating remote dir {}", fullRemotePath);
                                copyRecursive(fullPath, fullRemotePath);
                            } 
                            else if (ENTRY_DELETE.equals(kind)) {
                                logger.debug("Deleting remote dir {}", fullRemotePath);
                                //dir must be empty
                                removeRecursive(fullRemotePath);
                            }
                            else if(ENTRY_MODIFY.equals(kind)) {
                                logger.debug("Dir {} modified", relPath);
                            }
                        } else if (ENTRY_MODIFY.equals(kind) || ENTRY_CREATE.equals(kind)) {
                            logger.debug("Copying to remote file {}", fullRemotePath);
                            Files.createDirectories(fullRemotePath.getParent());
                            try {
                                Files.copy(fullPath, fullRemotePath, COPY_ATTRIBUTES, REPLACE_EXISTING);
                            } catch(NoSuchFileException nsf) {
                                logger.info("File didn't exist when attempted to copy: {}", relPath);
                            }
                        } else if (ENTRY_DELETE.equals(kind)) {
                            logger.debug("Deleting remote file {}", fullRemotePath);
                            Files.deleteIfExists(fullRemotePath);
                        }
                    }

                    // Reset the key -- this step is critical if you want to
                    // receive further watch events.  If the key is no longer valid,
                    // the directory is inaccessible so exit the loop.
                    boolean valid = key.reset();
                    if (!valid) {
                        logger.debug("Key no longer valid after reset");
                        break;
                    }
                }

                logger.debug("Watcher for {} shutting down", rootFolder.getPath());

            } catch (Throwable e) {
                logger.warn("Error while waiting for file changes", e);
            }
        }

        private void copyRecursive(Path source, Path dest) throws IOException {
            Files.copy(source, dest);

            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Path relPath = source.relativize(file);
                    Files.copy(file, dest.resolve(relPath), COPY_ATTRIBUTES, REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path relPath = source.relativize(dir);
                    Files.createDirectories(dest.resolve(relPath));
                    return FileVisitResult.CONTINUE;
                }
                
            });
        }
        
        private void removeRecursive(Path path) throws IOException {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    // try to delete the file anyway, even if its attributes
                    // could not be read, since delete-only access is
                    // theoretically possible
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed; propagate exception
                        throw exc;
                    }
                }
            });
        }

    }

    private static class WatcherRegisteringFileVisitor extends SimpleFileVisitor<Path> {

        private final Path sourceRoot;
        private final RootFolder sourceRootFolder;
        private WatchService watcher;
        private final Path remoteRootPath;

        public WatcherRegisteringFileVisitor(RootFolder sourceRootFolder, WatchService watcher) {
            this.sourceRootFolder = sourceRootFolder;
            sourceRoot = fileSystem.getPath(sourceRootFolder.getPath());
            if (!sourceRoot.toFile().isDirectory()) {
                throw new IllegalArgumentException("Source path " + sourceRootFolder.getPath() + " is not a directory");
            }
            this.watcher = watcher;
            String remoteRoot = StringUtils.isBlank(sourceRootFolder.getRemotePath()) ? backupConfig.getRemotePath() : sourceRootFolder.getRemotePath();
            remoteRootPath = fileSystem.getPath(remoteRoot);
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            final Path relPath = sourceRoot.getParent().relativize(dir);
            logger.debug("Visiting relative source path {}", relPath);

            final String dirName = dir.getFileName().toString();
            for (String regex : sourceRootFolder.getExclude()) {
                if (Pattern.matches(regex, dirName) || Pattern.matches(regex, relPath.toString())) {
                    logger.debug("Skipping path {} due to exclusion pattern {}", dir, regex);
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }

            //not skipping - register this dir for watching
            logger.debug("Registering {} for watching", relPath);
            dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            
            //see if we should skip this file
            final Path relPath = sourceRoot.getParent().relativize(file);
            for (String regex : sourceRootFolder.getExclude()) {
                if (Pattern.matches(regex, file.getFileName().toString()) || Pattern.matches(regex, relPath.toString())) {
                    logger.debug("Skipping path {} due to exclusion pattern {}", relPath, regex);
                    return FileVisitResult.CONTINUE;
                }
            }

            //not skipping - check for existence in destination
            Path fullDest = remoteRootPath.resolve(relPath);
            Files.createDirectories(fullDest.getParent());
            boolean copy = false;
            if(Files.exists(fullDest)) {
                //check file size, then timestamp
                if(Files.size(fullDest) != Files.size(file)
                        || Files.getLastModifiedTime(file).compareTo(Files.getLastModifiedTime(fullDest)) > 0) {
                    logger.debug("Remote file {} exists but appears different than the original. Replacing...", relPath);
                    copy = true;
                }
                else {
                    logger.debug("Remote file {} already exists and appears identical to the original.", relPath);
                }
            }
            else {
                logger.debug("Remote file {} does not exist. Creating it now.", relPath);
                copy = true;
            }
            
            if(copy) Files.copy(file, fullDest, COPY_ATTRIBUTES, REPLACE_EXISTING);
            
            return FileVisitResult.CONTINUE;
        }

        
    }
}
