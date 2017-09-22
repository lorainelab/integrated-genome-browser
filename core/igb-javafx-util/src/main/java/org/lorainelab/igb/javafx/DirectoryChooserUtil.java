/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.javafx;

import java.io.File;
import java.util.Optional;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.DirectoryChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoryChooserUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryChooserUtil.class);
    private static final JFXPanel FX_RUNTIME_INITIALIZER = new JFXPanel(); // see https://docs.oracle.com/javase/8/javafx/api/javafx/application/Platform.html#runLater-java.lang.Runnable- for why this is needed
    private static final Object LOCK = new Object();
    private Optional<String> title;
    private Optional<File> context;

    private DirectoryChooserUtil() {
        title = Optional.empty();
        context = Optional.empty();
    }

    public static DirectoryChooserUtil build() {
        return new DirectoryChooserUtil();
    }

    public DirectoryChooserUtil setTitle(String title) {
        this.title = Optional.of(title);
        return this;
    }

    public DirectoryChooserUtil setContext(File file) {
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                this.context = Optional.ofNullable(file);
            } else {
                this.context = Optional.ofNullable(file.getParentFile());
            }
        }
        return this;
    }

    public final Optional<File> retrieveDirFromFxChooser() {
        synchronized (LOCK) {
            final File[] selectedFile = new File[1];
            final boolean[] keepWaiting = new boolean[1];
            keepWaiting[0] = true;

            Platform.runLater(() -> {
                synchronized (LOCK) {
                    final DirectoryChooser dirChooser = getDirChooser();
                    selectedFile[0] = dirChooser.showDialog(null);
                    keepWaiting[0] = false;
                    LOCK.notifyAll();
                }
            });

            // Wait for runLater to complete
            do {
                try {
                    LOCK.wait();
                } catch (final InterruptedException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            } while (keepWaiting[0]);

            return Optional.ofNullable(selectedFile[0]);
        }

    }

    private DirectoryChooser getDirChooser() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        if (title.isPresent()) {
            dirChooser.setTitle(title.get());
        }
        if (context.isPresent()) {
            dirChooser.setInitialDirectory(context.get());
        }
        return dirChooser;
    }
}
    