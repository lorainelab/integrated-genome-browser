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
import javafx.embed.swing.JFXPanel;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileChooserUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileChooserUtil.class);
    private static final JFXPanel FX_RUNTIME_INITIALIZER = new JFXPanel(); // see https://docs.oracle.com/javase/8/javafx/api/javafx/application/Platform.html#runLater-java.lang.Runnable- for why this is needed
    private static final Object LOCK = new Object();
    private Optional<String> title;
    private Optional<String> defaultFileName;
    private Optional<File> context;
    private Optional<ExtensionFilter> extensionFilter;

    private FileChooserUtil() {
        title = Optional.empty();
        defaultFileName = Optional.empty();
        context = Optional.empty();
        extensionFilter = Optional.empty();
    }

    public static FileChooserUtil build() {
        return new FileChooserUtil();
    }

    public FileChooserUtil setTitle(String title) {
        this.title = Optional.of(title);
        return this;
    }

    public FileChooserUtil setDefaultFileName(String defaultFileName) {
        this.defaultFileName = Optional.of(defaultFileName);
        return this;
    }

    public FileChooserUtil setContext(File file) {
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                this.context = Optional.ofNullable(file);
            } else {
                this.context = Optional.ofNullable(file.getParentFile());
            }
        }
        return this;
    }

    public FileChooserUtil setFileExtensionFilter(ExtensionFilter extensionFilter) {
        this.extensionFilter = Optional.ofNullable(extensionFilter);
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
        if (extensionFilter.isPresent()) {
            fileChooser.getExtensionFilters().add(extensionFilter.get());
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
