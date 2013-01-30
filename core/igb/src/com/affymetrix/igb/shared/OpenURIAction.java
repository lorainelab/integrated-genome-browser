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
package com.affymetrix.igb.shared;


import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.net.URI;
import javax.swing.JOptionPane;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.genoviz.swing.recordplayback.ScriptManager;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.action.RunScriptAction;
import com.affymetrix.igb.util.MergeOptionChooser;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;

public abstract class OpenURIAction extends GenericAction {

	private static final long serialVersionUID = 1L;

	public static int unknown_group_count = 1;
	public static final String UNKNOWN_SPECIES_PREFIX = BUNDLE.getString("customSpecies");
	public static final String UNKNOWN_GENOME_PREFIX = BUNDLE.getString("customGenome");
	protected static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	protected final IGBService igbService;
	protected MergeOptionChooser chooser = null;
	protected Set<String> seq_ref_endings = new HashSet<String>();
	
	public OpenURIAction(IGBService _igbService, String text, String tooltip, String iconPath, String largeIconPath, int mnemonic, Object extraInfo, boolean popup){
		super(text, tooltip, iconPath, largeIconPath, mnemonic, extraInfo, popup);
		igbService = _igbService;
	}
	
	protected void openURI(URI uri, final String fileName, final boolean mergeSelected, 
		final AnnotatedSeqGroup loadGroup, final String speciesName) {
		
		if(ScriptManager.getInstance().isScript(uri.toString())){
			int result = JOptionPane.showConfirmDialog(igbService.getFrame(), "Do you want to run the script?", "Found Script", JOptionPane.YES_NO_OPTION);
			if(result == JOptionPane.YES_OPTION){
				RunScriptAction.getAction().runScript(uri.toString());
			}
			return;
		}
		
		igbService.openURI(uri, fileName, loadGroup, speciesName, loadSequenceAsTrack());
		
		if (!mergeSelected) {
			unknown_group_count++;
		}

	}

	protected MergeOptionChooser getFileChooser(String id) {
		chooser = new MergeOptionChooser(id);
		chooser.setMultiSelectionEnabled(true);
	
		// Build the set for sequence file
		seq_ref_endings = new HashSet<String>();
		Map<String, List<String>> nameToExtensionMap = FileTypeHolder.getInstance().getNameToExtensionMap(FileTypeCategory.Sequence);
		for (String name : nameToExtensionMap.keySet()) {
			seq_ref_endings.addAll(nameToExtensionMap.get(name));
		}
		
		chooser.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (JFileChooser.SELECTED_FILES_CHANGED_PROPERTY.equals(evt.getPropertyName())) { // Single selection included
					File[] files = chooser.getSelectedFiles();
					if(files.length == 1) {
						File file = files[0];
						if(file!=null) {
							String fileExtension = SymLoader.getExtension(file.getName());
							boolean enableLoadAsSeqCB = seq_ref_endings.contains(fileExtension);
							chooser.optionChooser.getLoadAsSeqCB().setEnabled(enableLoadAsSeqCB);
							
							if(!enableLoadAsSeqCB) {
								chooser.optionChooser.getLoadAsSeqCB().setSelected(false); // Uncheck for disabled
							}
						}
					} else if(files.length > 1) {
						chooser.optionChooser.getLoadAsSeqCB().setSelected(false); // Uncheck & disable for multiple selection
						chooser.optionChooser.getLoadAsSeqCB().setEnabled(false);
					}
					
				}
			}
		});
		
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
	
	protected boolean checkFriendlyName(String friendlyName) {
		if (!chooser.accept(new File(friendlyName))) {
			return false;
		}
		return true;
	}
	
	protected abstract void addSupportedFiles();
	
	protected abstract String getFriendlyNameID();
	
	protected abstract boolean loadSequenceAsTrack();
	
	
}
