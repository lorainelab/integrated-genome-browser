package com.affymetrix.igb.util;

import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

/**
 * Code referred from IGV
 * @author nick
 */
public class ExportFileChooser extends JFileChooser {

	boolean accepted = false;
	File previousFile;
	FileFilter selectedFilter;

	public ExportFileChooser(File directory, File selectedFile, FileFilter selectedFilter) {
		super(directory);
		this.selectedFilter = selectedFilter;
		setPreviousFile(selectedFile);
		init();
	}

	@Override
	public void approveSelection() {
		accepted = true;
		File f = getSelectedFile();
		if (!f.exists()) {
			super.approveSelection();
			return;
		} else { // give the user the choice to overwrite the existing file or not
			// The option pane used differs from the confirmDialog only in
			// that "No" is the default choice.
			getToolkit().beep();
			String[] options = {"Yes", "No"};
			if (JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(
					this, "Overwrite Existing File?", "File Exists",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					options, options[1])) {
				super.approveSelection();
				return;
			}
			return;
		}
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

	private void init() {
		FileFilter[] fileFilters = ExportDialog.getAllExportFileFilters();

		if (fileFilters != null) {
			for (FileFilter fileFilter : fileFilters) {
				this.addChoosableFileFilter(fileFilter);
			}
		}

		this.setFileFilter(this.selectedFilter);

		addPropertyChangeListener(new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent e) {


				File oldFile = null;
				String property = e.getPropertyName();
				if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(property)) {
					oldFile = (File) e.getOldValue();
				} else if (JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(property)) {

					if (e.getOldValue() instanceof ExportFileFilter
							&& e.getNewValue() instanceof ExportFileFilter) {

						ExportFileFilter newFilter = (ExportFileFilter) e.getNewValue();

						File currentDirectory = getCurrentDirectory();
						File previousFile = getPreviousFile();
						if (previousFile != null) {

							File file = null;
							if (currentDirectory != null) {
								file = new File(currentDirectory, previousFile.getName());
							} else {
								file = previousFile;
							}

							final File selectedFile = ExportDialog.changeFileExtension(
									file, newFilter.getExtension());
							SwingUtilities.invokeLater(new Runnable() {

								public void run() {
									setPreviousFile(selectedFile);
									validate();
								}
							});
						}
					}
				}
			}
		});
	}
}
