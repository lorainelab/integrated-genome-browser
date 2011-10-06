/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.event.*;

import javax.swing.*;
import java.net.URI;
import java.text.MessageFormat;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.genometryImpl.util.FileDropHandler;
import com.affymetrix.igb.IGB;

import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.igb.shared.OpenURIAction;
import com.affymetrix.igb.util.MergeOptionChooser;
import com.affymetrix.igb.util.ScriptFileLoader;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @version $Id$
 */
public final class LoadFileAction extends OpenURIAction {

	private static final long serialVersionUID = 1L;
	private static final LoadFileAction ACTION = new LoadFileAction();

	public static LoadFileAction getAction() {
		return ACTION;
	}
	private final JFrame gviewerFrame;
	private final FileTracker load_dir_tracker;
	private final TransferHandler fdh = new FileDropHandler() {

		private static final long serialVersionUID = 1L;

		@Override
		public void openFileAction(File f) {
			URI uri = f.toURI();
			if (!openURI(uri)) {
				ErrorHandler.errorPanel("FORMAT NOT RECOGNIZED", "Format not recognized for file: " + f.getName());
			}
		}

		@Override
		public void openURLAction(String url) {
			if (url.contains("fromTree:")) {
				url = url.substring(url.indexOf(":") + 1, url.length());
				try {
					GeneralLoadView.getLoadView().getFeatureTree().updateTree(url);
				} catch (URISyntaxException ex) {
					Logger.getLogger(LoadFileAction.class.getName()).log(Level.SEVERE, null, ex);
				}
			} else {
				try {
					URI uri = new URI(url.trim());
					if (!openURI(uri)) {
						ErrorHandler.errorPanel("FORMAT NOT RECOGNIZED", "Format not recognized for file: " + url);
					}
				} catch (URISyntaxException ex) {
					ex.printStackTrace();
					ErrorHandler.errorPanel("INVALID URL", url + "\n Url provided is not valid: ");
				}
			}
		}
	};
	private MergeOptionChooser chooser = null;

	/**
	 *  Constructor.
	 *  @param ft  a FileTracker used to keep track of directory to load from
	 */
	private LoadFileAction() {
		super(IGBServiceImpl.getInstance());

		this.gviewerFrame = ((IGB) IGB.getSingleton()).getFrame();
		load_dir_tracker = FileTracker.DATA_DIR_TRACKER;
		this.gviewerFrame.setTransferHandler(fdh);
	}

	public void actionPerformed(ActionEvent e) {
		loadFile(load_dir_tracker, gviewerFrame);
	}

	private MergeOptionChooser getFileChooser(String id) {
		chooser = new MergeOptionChooser(id);
		chooser.setMultiSelectionEnabled(true);
		Map<String, List<String>> nameToExtensionMap = FileTypeHolder.getInstance().getNameToExtensionMap();
		for (String name : nameToExtensionMap.keySet()) {
			chooser.addChoosableFileFilter(new UniFileFilter(
					nameToExtensionMap.get(name).toArray(new String[]{}),
					name + " Files"));
		}
		chooser.addChoosableFileFilter(new UniFileFilter(
				new String[]{"igb", "py"},
				"Script File"));

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
	private void loadFile(final FileTracker load_dir_tracker, final JFrame gviewerFrame) {
		MergeOptionChooser fileChooser = getFileChooser("loadFile");
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
			openURI(uri, file.getName(), mergeSelected, loadGroup, (String) fileChooser.speciesCB.getSelectedItem());
		}

	}

	public void openURI(URI uri, String fileName) {
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		if (ScriptFileLoader.isScript(uri.toString())) {
			ScriptFileLoader.runScript(uri.toString());
			return;
		}
		openURI(uri, fileName, true, group, group.getOrganism());
	}

	protected boolean checkFriendlyName(String friendlyName) {
		if (!getFileChooser("openURI").accept(new File(friendlyName))) {
			return false;
		}
		return true;
	}

	@Override
	public String getText() {
		return MessageFormat.format(
				BUNDLE.getString("menuItemHasDialog"),
				BUNDLE.getString("openFile"));
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Open16.gif";
	}

	@Override
	public int getShortcut() {
		return KeyEvent.VK_O;
	}
}
