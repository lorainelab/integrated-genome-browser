package com.affymetrix.igb.action;



import java.io.File;
import java.net.URI;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.igb.IGB;

import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.igb.util.MergeOptionChooser;


/**
 *
 * @author hiralv
 */
public abstract class AbstractLoadFileAction extends AbstractLoadFileOrURLAction {

	private static final long serialVersionUID = 1L;
	private final FileTracker load_dir_tracker;
	
	protected AbstractLoadFileAction(String text, String tooltip, String iconPath, String largeIconPath, int mnemonic, Object extraInfo, boolean popup) {
		super(text, tooltip, iconPath, largeIconPath, mnemonic, extraInfo, popup);
		load_dir_tracker = FileTracker.DATA_DIR_TRACKER;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		MergeOptionChooser fileChooser = getFileChooser(getId());
		File currDir = load_dir_tracker.getFile();
		if (currDir == null) {
			currDir = new File(System.getProperty("user.home"));
		}
		fileChooser.setCurrentDirectory(currDir);
		fileChooser.rescanCurrentDirectory();

		int option = fileChooser.showOpenDialog(igbService.getFrame());

		if (option != JFileChooser.APPROVE_OPTION) {
			return;
		}

		load_dir_tracker.setFile(fileChooser.getCurrentDirectory());

		final File[] fils = fileChooser.getSelectedFiles();

		final AnnotatedSeqGroup loadGroup = gmodel.addSeqGroup((String) fileChooser.getSelectedVersion());

		final boolean mergeSelected = loadGroup == gmodel.getSelectedSeqGroup();

		for (File file : fils) {
			URI uri = file.toURI();
			openURI(uri, file.getName(), mergeSelected, loadGroup, (String) fileChooser.getSelectedSpecies());
		}
	}
		
	public void openURI(URI uri, String fileName) {
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		openURI(uri, fileName, true, group, group.getOrganism());
	}
}
