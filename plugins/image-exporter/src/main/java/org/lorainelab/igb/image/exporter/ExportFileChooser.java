package org.lorainelab.igb.image.exporter;

import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

/**
 * An Export File Chooser class for IGB, it is extended from java default class
 * and implemented some override methods. (Code referred from IGV)
 *
 * @author nick
 */
public class ExportFileChooser extends JFileChooser {

    private static final long serialVersionUID = 1L;
    boolean accepted = false;
    File previousFile;
    FileFilter selectedFilter;

    public ExportFileChooser(File directory, File selectedFile, FileFilter selectedFilter, ExportDialog exportDialog) {
        super(directory);
        this.selectedFilter = selectedFilter;
        setPreviousFile(selectedFile);
        setAcceptAllFileFilterUsed(false);
        init(exportDialog);
    }

    @Override
    public void approveSelection() {
        accepted = true;
        super.approveSelection();
    }

    public void setPreviousFile(File file) {
        this.previousFile = file;
        setSelectedFile(previousFile);
    }

    public File getPreviousFile() {
        return previousFile;
    }

    @Override
    public void cancelSelection() {
        setSelectedFile(null);
        super.cancelSelection();
    }

    @Override
    protected JDialog createDialog(Component parent) throws HeadlessException {
        JDialog dialog = super.createDialog(parent);
        dialog.setLocation(300, 200);
        dialog.setResizable(false);
        dialog.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                if (!accepted) {
                    setSelectedFile(null);
                }
            }
        });
        return dialog;
    }

    private void init(final ExportDialog exportDialog) {

        for (FileFilter fileFilter : exportDialog.getAllExportFileFilters()) {
            addChoosableFileFilter(fileFilter);
        }

        setFileFilter(selectedFilter);

        addPropertyChangeListener(e -> {
            String property = e.getPropertyName();
            if (JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(property)) {

                if (e.getOldValue() instanceof ExportFileFilter
                        && e.getNewValue() instanceof ExportFileFilter) {

                    ExportFileFilter newFilter = (ExportFileFilter) e.getNewValue();

                    File currentDirectory = getCurrentDirectory();
                    File previousFile1 = getPreviousFile();
                    if (previousFile1 != null) {

                        File file = null;
                        if (currentDirectory != null) {
                            file = new File(currentDirectory, previousFile1.getName());
                        } else {
                            file = previousFile1;
                        }

                        final File selectedFile = exportDialog.changeFileExtension(
                                file, newFilter.getExtension());
                        SwingUtilities.invokeLater(() -> {
                            setPreviousFile(selectedFile);
                            validate();
                        });
                    }
                }
            }
        });
    }
}
