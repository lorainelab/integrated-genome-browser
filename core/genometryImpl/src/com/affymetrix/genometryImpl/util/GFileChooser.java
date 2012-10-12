package com.affymetrix.genometryImpl.util;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/**
 *  A user-friendly JFileChooser.  This is most useful when saving files,
 *  because it will warn the user if they try to over-write an existing file,
 *  and it both forces and helps the user to supply the correct file extension.
 */
public class GFileChooser extends JFileChooser {

	/**
	 *  If the selected file looks like a reasonable choice, then open it.
	 *  Else suggest a new filename.
	 */
	@Override
	public void approveSelection() {
		File f = getSelectedFile();
		if (f.isDirectory()) {
			setSelectedFile(null);
			setCurrentDirectory(f);
			return;
		}
		FileFilter filter = getFileFilter();
		UniFileFilter uni_filter = null;
		Set<String> extensions = Collections.<String>emptySet();
		if (filter instanceof UniFileFilter) {
			uni_filter = (UniFileFilter) filter;
			extensions = uni_filter.getExtensions();
		} else {
			// if not a UniFileFilter, defer to the approval mechanism of the filter
			if (filter.accept(f)) {
				super.approveSelection();
			} else {
				getToolkit().beep();
				return;
			}
		}
		if (getDialogType() == OPEN_DIALOG) {
			if (f.exists()) {
				super.approveSelection();
			} else if (!extensions.isEmpty()) {
				// if a similar filename with "."+extension exists, suggest that
				getToolkit().beep();
				for (String ext : extensions) {
					File file2 = applyExtension(f, ext);
					if (file2.exists()) {
						setSelectedFile(file2);
						return;
					}
				}
			}
		} else if (getDialogType() == SAVE_DIALOG) {
			for (String ext : extensions) {
				if (f.getName().endsWith("." + ext)) {
					if (!f.exists()) {
						super.approveSelection();
						return;
					} else {
						// give the user the choice to overwrite the existing file or not
						// The option pane used differs from the confirmDialog only in
						// that "No" is the default choice.
						getToolkit().beep();
						String[] options = {"Yes", "No"};
						if (JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(this, "Overwrite Existing File?", "File Exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1])) {
							super.approveSelection();
							return;
						}
						return;
					}
				}
			}
			if (!extensions.isEmpty()) {
				// Suggest to the user a new filename ending with "." + the first extension
				getToolkit().beep();
				String first_extension = extensions.iterator().next();
				setSelectedFile(applyExtension(f, first_extension));
			}
		}
	}
    
	/** Return a new file with the given extension at the end of the name. */
	private static File applyExtension(File f, String extension) {
		String name = f.getName();
		String dotExtension = "." + extension;
		if (name.endsWith(".")) {
			return new File(name + extension);
		} else if (!name.endsWith(dotExtension)) {
			return new File(name + dotExtension);
		} else {
			return f;
		}
	}
}
