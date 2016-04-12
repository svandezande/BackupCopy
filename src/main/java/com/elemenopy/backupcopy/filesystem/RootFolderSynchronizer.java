/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.elemenopy.backupcopy.filesystem;

import com.elemenopy.backupcopy.config.RootFolder;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import static java.nio.file.FileVisitOption.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import static java.nio.file.StandardCopyOption.*;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.commons.lang3.StringUtils;
import java.util.EnumSet;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Scott
 */
@Deprecated
public class RootFolderSynchronizer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private final Path destination;
    private final RootFolder sourceRootFolder;
    private final FileSystem fileSystem = FileSystems.getDefault();

    public RootFolderSynchronizer(String defaultDestination, RootFolder sourceRootFolder) {
        this.destination = fileSystem.getPath(!StringUtils.isBlank(sourceRootFolder.getRemotePath()) ? sourceRootFolder.getRemotePath() : defaultDestination);
        if(!Files.isDirectory(destination)) {
            throw new IllegalArgumentException("Destination path " + destination + " is not a directory");
        }
        this.sourceRootFolder = sourceRootFolder;
    }

    public void synchronize() throws IOException {
        Path sourceRoot = fileSystem.getPath(sourceRootFolder.getPath());
        if (!sourceRoot.toFile().isDirectory()) {
            throw new IllegalArgumentException("Source path " + sourceRootFolder.getPath() + " is not a directory");
        }
        EnumSet<FileVisitOption> opts = EnumSet.of(FOLLOW_LINKS);
        Files.walkFileTree(sourceRoot, opts, Integer.MAX_VALUE, new SyncingFileVisitor(sourceRoot, destination));
    }
    
    private class SyncingFileVisitor extends SimpleFileVisitor<Path> {
        private final Path sourceRoot;
        private final Path destRoot;
        
        public SyncingFileVisitor(Path sourceRoot, Path destRoot) {
            this.sourceRoot = sourceRoot;
            this.destRoot = destRoot;
        }
        
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            final Path relPath = sourceRoot.getParent().relativize(dir);
            logger.debug("Visiting relative source path {}", relPath);
            
            final String dirName = dir.getFileName().toString();
            for(String regex : sourceRootFolder.getExclude()) {
                if(Pattern.matches(regex, dirName) || Pattern.matches(regex, relPath.toString())) {
                    logger.debug("Skipping path {} due to exclusion pattern {}", dir, regex);
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
            
            //not skipping - check for existence in destination
            Path fullDest = destRoot.resolve(relPath);
            if(fullDest.toFile().exists()) {
                logger.debug("Remote path {} already exists.", fullDest);
            }
            else {
                logger.debug("Remote path {} does not exist. Creating it now.", fullDest);
                fullDest.toFile().mkdirs();
            }
            
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            final Path relPath = sourceRoot.getParent().relativize(file);
            logger.debug("Visiting relative source file {}", relPath);
            
            final String dirName = file.getFileName().toString();
            for(String regex : sourceRootFolder.getExclude()) {
                if(Pattern.matches(regex, dirName) || Pattern.matches(regex, relPath.toString())) {
                    logger.debug("Skipping file {} due to exclusion pattern {}", file, regex);
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
            
            //not skipping - check for existence in destination
            Path fullDest = destRoot.resolve(relPath);
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
