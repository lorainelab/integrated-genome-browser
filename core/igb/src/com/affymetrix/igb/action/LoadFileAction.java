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

import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.TransferHandler;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.FileDropHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;

import com.affymetrix.genoviz.swing.recordplayback.ScriptManager;

import com.affymetrix.igb.view.load.GeneralLoadView;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import javax.swing.JOptionPane;

/**
 *
 * @version $Id: LoadFileAction.java 11360 2012-05-02 14:41:01Z anuj4159 $
 */
public final class LoadFileAction extends AbstractLoadFileAction {

	private static final long serialVersionUID = 1L;
	private static final LoadFileAction ACTION = new LoadFileAction();
	private static final String SELECT_SPECIES = BUNDLE.getString("speciesCap");
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
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
				openURIOrRunScript(f.toURI(), loadGroup, speciesName, f.getAbsolutePath(), f.getName());
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
					openURIOrRunScript(new URI(url.trim()), getloadGroup(), getSpeciesName(), url, url);
				} catch (URISyntaxException ex) {
					ex.printStackTrace();
					ErrorHandler.errorPanel("INVALID URL", url + "\n Url provided is not valid: ", Level.SEVERE);
				}
			}
		}
	};
	
	private void openURIOrRunScript(URI uri, AnnotatedSeqGroup loadGroup, String speciesName, String path, String name){
		
		if (openURI(uri, loadGroup, speciesName)) {
			return;
		}
		
		if(ScriptManager.getInstance().isScript(path)){
			int result = JOptionPane.showConfirmDialog(gviewerFrame, "Do you want to run the script?", "Found Script", JOptionPane.YES_NO_OPTION);
			if(result == JOptionPane.YES_OPTION){
				RunScriptAction.getAction().runScript(path);
			}
			return;
		}
		
		ErrorHandler.errorPanel("FORMAT NOT RECOGNIZED", "Format not recognized for file: " + name, Level.WARNING);
	}
	
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

	private LoadFileAction() {
		super(BUNDLE.getString("openFile"), null,
				"16x16/actions/document-open.png",
				"22x22/actions/document-open.png",
				KeyEvent.VK_O, null, true);
		this.gviewerFrame.setTransferHandler(fdh);
	}

	@Override
	protected String getID() {
		return "loadFile";
	}

	@Override
	public int getOrdinal() {
		return -9009000;
	}
}
