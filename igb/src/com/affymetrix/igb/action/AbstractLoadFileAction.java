package com.affymetrix.igb.action;



import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.igb.IGB;

import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.igb.shared.OpenURIAction;
import com.affymetrix.igb.util.MergeOptionChooser;


/**
 *
 * @author hiralv
 */
public abstract class AbstractLoadFileAction extends OpenURIAction {

	private static final long serialVersionUID = 1L;
	
	private final FileTracker load_dir_tracker;
	protected MergeOptionChooser chooser = null;
	protected final JFrame gviewerFrame;

	/**
	 *  Constructor.
	 *  @param ft  a FileTracker used to keep track of directory to load from
	 */
	protected AbstractLoadFileAction() {
		super(IGBServiceImpl.getInstance());
		
		this.gviewerFrame = ((IGB) IGB.getSingleton()).getFrame();
		load_dir_tracker = FileTracker.DATA_DIR_TRACKER;
	}

	public void actionPerformed(ActionEvent e) {
		loadFile(load_dir_tracker, gviewerFrame, getId(), loadSequenceAsTrack());
	}
	
	protected MergeOptionChooser getFileChooser(String id) {
		chooser = new MergeOptionChooser(id);
		chooser.setMultiSelectionEnabled(true);
		
		addSupportedFiles();
		
		Set<String> all_known_endings = new HashSet<String>();
		for (javax.swing.filechooser.FileFilter filter : chooser.getChoosableFileFilters()) {
			if (filter instanceof UniFileFilter) {
				UniFileFilter uff = (UniFileFilter) filter;
				uff.addCompressionEndings(GeneralUtils.compression_endings);
				all_known_endings.addAll(uff.getExtensions());
			}
		}
		UniFileFilter all_known_types = new UniFileFilter(
				all_known_endings.toArray(new String[all_known_endings.size()]),
				"Known Types");
		all_known_types.setExtensionListInDescription(false);
		all_known_types.addCompressionEndings(GeneralUtils.compression_endings);
		chooser.addChoosableFileFilter(all_known_types);
		chooser.setFileFilter(all_known_types);
		return chooser;
	}
	
	/** Load a file into the global singleton genometry model. */
	private void loadFile(final FileTracker load_dir_tracker, final JFrame gviewerFrame, String id, boolean loadAsTrack) {
		MergeOptionChooser fileChooser = getFileChooser(id);
		File currDir = load_dir_tracker.getFile();
		if (currDir == null) {
			currDir = new File(System.getProperty("user.home"));
		}
		fileChooser.setCurrentDirectory(currDir);
		fileChooser.rescanCurrentDirectory();

		int option = fileChooser.showOpenDialog(gviewerFrame);

		if (option != JFileChooser.APPROVE_OPTION) {
			return;
		}

		load_dir_tracker.setFile(fileChooser.getCurrentDirectory());

		final File[] fils = fileChooser.getSelectedFiles();

		final AnnotatedSeqGroup loadGroup = gmodel.addSeqGroup((String) fileChooser.versionCB.getSelectedItem());

		final boolean mergeSelected = loadGroup == gmodel.getSelectedSeqGroup();

		for (File file : fils) {
			URI uri = file.toURI();
			openURI(uri, file.getName(), mergeSelected, loadGroup, (String) fileChooser.speciesCB.getSelectedItem(), loadAsTrack);
		}

	}

	public void openURI(URI uri, String fileName) {
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		openURI(uri, fileName, true, group, group.getOrganism(), false);
	}

	@Override
	protected boolean checkFriendlyName(String friendlyName) {
		if (!getFileChooser(getFriendlyNameID()).accept(new File(friendlyName))) {
			return false;
		}
		return true;
	}

	@Override
	public String getIconPath() {
		return null;
	}

	protected abstract void addSupportedFiles();
	
	protected abstract String getID();
	
	protected abstract String getFriendlyNameID();

	protected abstract boolean loadSequenceAsTrack();

}
