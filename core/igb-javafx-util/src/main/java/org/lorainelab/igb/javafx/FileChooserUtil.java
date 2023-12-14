/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.javafx;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileChooserUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileChooserUtil.class);
    private Optional<String> title;
    private Optional<String> defaultFileName;
    private Optional<File> context;
    private Optional<List<FileNameExtensionFilter>> extensionFilters;
    private FileNameExtensionFilter selectedExtensionFilter;

    private FileChooserUtil() {
        title = Optional.empty();
        defaultFileName = Optional.empty();
        context = Optional.empty();
        extensionFilters = Optional.empty();
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

    public FileChooserUtil setFileExtensionFilters(List<FileNameExtensionFilter> extensionFilters) {
        this.extensionFilters = Optional.ofNullable(extensionFilters);
        return this;
    }

    private JFileChooser getFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        title.ifPresent(fileChooser::setDialogTitle);
        context.ifPresent(fileChooser::setCurrentDirectory);
        defaultFileName.ifPresent(fileName -> fileChooser.setSelectedFile(new File(fileName)));
        extensionFilters.ifPresent(filters -> filters.forEach(fileChooser::addChoosableFileFilter));
        return fileChooser;
    }

    public final Optional<File> retrieveFileFromDialog() {
        final File[] selectedFile = new File[1];
        Runnable fileChooserTask = () -> {
            JFileChooser fileChooser = getFileChooser();
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile[0] = fileChooser.getSelectedFile();
                setSelectedExtensionFilter(fileChooser);
            }
        };

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                fileChooserTask.run();
            } else {
                SwingUtilities.invokeAndWait(fileChooserTask);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return Optional.ofNullable(selectedFile[0]);
    }

    private void setSelectedExtensionFilter(JFileChooser fileChooser) {
        File selected = fileChooser.getSelectedFile();
        if (selected != null && extensionFilters.isPresent()) {
            String fileName = selected.getName();
            String fileExt = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "";
            extensionFilters.get().stream()
                    .filter(filter -> Arrays.asList(filter.getExtensions()).contains(fileExt))
                    .findFirst()
                    .ifPresent(filter -> selectedExtensionFilter = filter);
        }
    }

    public FileNameExtensionFilter getSelectedFileExtension() {
        return selectedExtensionFilter;
    }

    public final Optional<List<File>> retrieveFilesFromDialog() {
        final List<File>[] selectedFiles = new List[]{null};
        Runnable fileChooserTask = () -> {
            JFileChooser fileChooser = getFileChooser();
            fileChooser.setMultiSelectionEnabled(true); // Enable multi-selection
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFiles[0] = Arrays.asList(fileChooser.getSelectedFiles());
            }
        };

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                fileChooserTask.run();
            } else {
                SwingUtilities.invokeAndWait(fileChooserTask);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return Optional.ofNullable(selectedFiles[0]);
    }

    public final Optional<File> saveFileFromDialog() {
        final File[] selectedFile = new File[1];
        Runnable fileChooserTask = () -> {
            JFileChooser fileChooser = getFileChooser();
            int result = fileChooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile[0] = fileChooser.getSelectedFile();
            }
        };

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                fileChooserTask.run();
            } else {
                SwingUtilities.invokeAndWait(fileChooserTask);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return Optional.ofNullable(selectedFile[0]);
    }

}
