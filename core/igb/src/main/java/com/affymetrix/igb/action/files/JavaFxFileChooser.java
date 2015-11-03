/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action.files;

import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaFxFileChooser {

    private static final Logger logger = LoggerFactory.getLogger(JavaFxFileChooser.class);
    private static final JFXPanel fxRuntimeInitializer = new JFXPanel(); // see https://docs.oracle.com/javase/8/javafx/api/javafx/application/Platform.html#runLater-java.lang.Runnable- for why this is needed
    private static final Object LOCK = new Object();

    public static final Optional<File> retrieveFileFromFxChooser(Optional<File> context) {
        synchronized (LOCK) {
            final File[] selectedFile = new File[1];
            final boolean[] keepWaiting = new boolean[1];
            keepWaiting[0] = true;

            Platform.runLater(() -> {
                synchronized (LOCK) {
                    final FileChooser fileChooser = new FileChooser();
                    if (context.isPresent()) {
                        fileChooser.setInitialDirectory(context.get());
                    }
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

    public static final Optional<List<File>> retrieveFilesFromFxChooser(Optional<File> context) {
        synchronized (LOCK) {
            final List<File> selectedFiles = Lists.newArrayList();
            final boolean[] keepWaiting = new boolean[1];
            keepWaiting[0] = true;

            Platform.runLater(() -> {
                synchronized (LOCK) {
                    final FileChooser fileChooser = new FileChooser();
                    if (context.isPresent()) {
                        final File file = context.get();
                        fileChooser.setInitialDirectory(file);
                    }
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
