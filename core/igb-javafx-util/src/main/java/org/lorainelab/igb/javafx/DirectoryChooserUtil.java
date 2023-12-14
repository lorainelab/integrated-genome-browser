package org.lorainelab.igb.javafx;

import java.io.File;
import java.util.Optional;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.FileDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoryChooserUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryChooserUtil.class);
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

    public final Optional<File> retrieveDirFromDialog() {
        final File[] selectedFile = new File[1];
        Runnable fileDialogTask = () -> {
            FileDialog fileDialog = new FileDialog((JDialog) null);
            title.ifPresent(fileDialog::setTitle);
            context.ifPresent(file -> fileDialog.setDirectory(file.getAbsolutePath()));
            fileDialog.setMode(FileDialog.LOAD);
            fileDialog.setFilenameFilter((dir, name) -> new File(dir, name).isDirectory());
            fileDialog.setVisible(true);
            if (fileDialog.getDirectory() != null) {
                selectedFile[0] = new File(fileDialog.getDirectory());
            }
        };

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                fileDialogTask.run();
            } else {
                SwingUtilities.invokeAndWait(fileDialogTask);
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        return Optional.ofNullable(selectedFile[0]);
    }

}
