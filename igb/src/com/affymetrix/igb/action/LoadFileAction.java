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

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.TransferHandler;

import com.affymetrix.genometryImpl.util.FileDropHandler;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;

import com.affymetrix.igb.view.load.GeneralLoadView;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.util.List;

/**
 *
 * @version $Id$
 */
public final class LoadFileAction extends AbstractLoadFileAction {

	private static final long serialVersionUID = 1L;
	private static final LoadFileAction ACTION = new LoadFileAction();
	private static final String SELECT_SPECIES = BUNDLE.getString("speciesCap");
	
	public static LoadFileAction getAction() {
		return ACTION;
	}

	private final TransferHandler fdh = new FileDropHandler() {

		private static final long serialVersionUID = 1L;

		@Override
		public void openFileAction(List<File> files) {
			AnnotatedSeqGroup loadGroup = getloadGroup();
			String speciesName = getSpeciesName();
			for (File f : files) {
				URI uri = f.toURI();
				if (!openURI(uri, loadGroup, speciesName)) {
					ErrorHandler.errorPanel("FORMAT NOT RECOGNIZED", "Format not recognized for file: " + f.getName());
				}
			}
		}

		@Override
		public void openURLAction(String url) {
			if (url.contains("fromTree:")) {
				url = url.substring(url.indexOf(":") + 1, url.length());
				try {
					GeneralLoadView.getLoadView().getFeatureTree().updateTree(url);
					GeneralLoadView.getLoadView().refreshTreeView();
				} catch (URISyntaxException ex) {
					Logger.getLogger(LoadFileAction.class.getName()).log(Level.SEVERE, null, ex);
				}
			} else {
				try {
					URI uri = new URI(url.trim());
					AnnotatedSeqGroup loadGroup = getloadGroup();
					String speciesName = getSpeciesName();
					if (!openURI(uri, loadGroup, speciesName)) {
						ErrorHandler.errorPanel("FORMAT NOT RECOGNIZED", "Format not recognized for file: " + url);
					}
				} catch (URISyntaxException ex) {
					ex.printStackTrace();
					ErrorHandler.errorPanel("INVALID URL", url + "\n Url provided is not valid: ");
				}
			}
		}
	};
	
	private boolean openURI(URI uri, AnnotatedSeqGroup loadGroup, String speciesName) {
		getFileChooser(getId());
		String unzippedName = GeneralUtils.getUnzippedName(uri.getPath());
		String friendlyName = unzippedName.substring(unzippedName.lastIndexOf("/") + 1);
		boolean mergeSelected = loadGroup == null ? false : true;
		if (!checkFriendlyName(friendlyName)) {
			return false;
		}
		openURI(uri, friendlyName, mergeSelected, loadGroup, speciesName);

		return true;
	}
	
	private AnnotatedSeqGroup getloadGroup(){
		AnnotatedSeqGroup loadGroup = gmodel.getSelectedSeqGroup();
		if (loadGroup == null) {
			loadGroup = gmodel.addSeqGroup(UNKNOWN_GENOME_PREFIX + " " + unknown_group_count);
		}
		return loadGroup;
	}
	private String getSpeciesName(){
		String speciesName = igbService.getSelectedSpecies();
		if (SELECT_SPECIES.equals(speciesName)) {
			speciesName = UNKNOWN_SPECIES_PREFIX + " " + unknown_group_count;
		}
		return speciesName;
	}
		
	/**
	 *  Constructor.
	 *  @param ft  a FileTracker used to keep track of directory to load from
	 */
	private LoadFileAction() {
		super();
		this.gviewerFrame.setTransferHandler(fdh);
	}
	
	@Override
	public String getText() {
		return BUNDLE.getString("openFile");
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_O;
	} 	 

	@Override
	protected String getID() {
		return "loadFile";
	}
		
}
