/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.javafx;

import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaFxFileChooser {

    private static final Logger logger = LoggerFactory.getLogger(JavaFxFileChooser.class);
    private static final Object LOCK = new Object();
    private Optional<String> title = Optional.empty();
    private Optional<String> defaultFileName = Optional.empty();
    private Optional<File> context = Optional.empty();

    private JavaFxFileChooser() {
    }
    
    public static JavaFxFileChooser build() {
        return new JavaFxFileChooser();
    }

    public JavaFxFileChooser setTitle(String title) {
        this.title = Optional.of(title);
        return this;
    }

    public JavaFxFileChooser setDefaultFileName(String defaultFileName) {
        this.defaultFileName = Optional.of(defaultFileName);
        return this;
    }

    public JavaFxFileChooser setContext(File file) {
        this.context = Optional.ofNullable(file);
        return this;
    }

    public final Optional<File> retrieveFileFromFxChooser() {
        synchronized (LOCK) {
            final File[] selectedFile = new File[1];
            final boolean[] keepWaiting = new boolean[1];
            keepWaiting[0] = true;

            Platform.runLater(() -> {
                synchronized (LOCK) {
                    final FileChooser fileChooser = getFileChooser();
                    selectedFile[0] = fileChooser.showOpenDialog(null);
                    keepWaiting[0] = false;
                    LOCK.notifyAll();
                }
            });

            // Wait for runLater to complete
            do {
                try {
                    LOCK.wait();
                } catch (final InterruptedException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } while (keepWaiting[0]);

            return Optional.ofNullable(selectedFile[0]);
        }

    }

    private FileChooser getFileChooser() {
        FileChooser fileChooser = new FileChooser();
        if (title.isPresent()) {
            fileChooser.setTitle(title.get());
        }
        if (defaultFileName.isPresent()) {
            fileChooser.setInitialFileName(defaultFileName.get());
        }
        if (context.isPresent()) {
            fileChooser.setInitialDirectory(context.get());
        }
        return fileChooser;
    }

    public final Optional<File> saveFilesFromFxChooser() {
        synchronized (LOCK) {
            final File[] selectedFile = new File[1];
            final boolean[] keepWaiting = new boolean[1];
            keepWaiting[0] = true;

            Platform.runLater(() -> {
                synchronized (LOCK) {
                    final FileChooser fileChooser = getFileChooser();
                    selectedFile[0] = fileChooser.showSaveDialog(null);
                    keepWaiting[0] = false;
                    LOCK.notifyAll();
                }
            });

            do {
                try {
                    LOCK.wait();
                } catch (final InterruptedException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } while (keepWaiting[0]);
            return Optional.ofNullable(selectedFile[0]);
        }
    }

    public final Optional<List<File>> retrieveFilesFromFxChooser() {
        synchronized (LOCK) {
            final List<File> selectedFiles = Lists.newArrayList();
            final boolean[] keepWaiting = new boolean[1];
            keepWaiting[0] = true;

            Platform.runLater(() -> {
                synchronized (LOCK) {
                    final FileChooser fileChooser = getFileChooser();
                    final List<File> returnedFiles = fileChooser.showOpenMultipleDialog(null);
                    if (returnedFiles != null) {
                        selectedFiles.addAll(returnedFiles);
                    }
                    keepWaiting[0] = false;
                    LOCK.notifyAll();
                }
            });

            // Wait for runLater to complete
            do {
                try {
                    LOCK.wait();
                } catch (final InterruptedException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } while (keepWaiting[0]);

            return Optional.ofNullable(selectedFiles);
        }

    }

}
