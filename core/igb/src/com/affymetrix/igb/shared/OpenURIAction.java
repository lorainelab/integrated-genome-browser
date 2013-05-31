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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.net.URI;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.genoviz.swing.recordplayback.ScriptManager;
import com.affymetrix.genoviz.swing.recordplayback.ScriptProcessorHolder;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.action.RunScriptAction;
import com.affymetrix.igb.util.MergeOptionChooser;
import com.affymetrix.igb.IGBServiceImpl;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import javax.swing.filechooser.FileFilter;

public abstract class OpenURIAction extends GenericAction {

	private static final long serialVersionUID = 1L;

	public static int unknown_group_count = 1;
	public static final String UNKNOWN_SPECIES_PREFIX = BUNDLE.getString("customSpecies");
	public static final String UNKNOWN_GENOME_PREFIX = BUNDLE.getString("customGenome");
	protected static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	protected final IGBService igbService;
	protected MergeOptionChooser chooser = null;
	
	public OpenURIAction(String text, String tooltip, String iconPath, String largeIconPath, int mnemonic, Object extraInfo, boolean popup){
		super(text, tooltip, iconPath, largeIconPath, mnemonic, extraInfo, popup);
		igbService = IGBServiceImpl.getInstance();
	}
			
	protected void openURI(URI uri, final String fileName, final boolean mergeSelected, 
		final AnnotatedSeqGroup loadGroup, final String speciesName, boolean loadAsTrack) {
		
		if(ScriptManager.getInstance().isScript(uri.toString())){
			int result = JOptionPane.showConfirmDialog(igbService.getFrame(), "Do you want to run the script?", "Found Script", JOptionPane.YES_NO_OPTION);
			if(result == JOptionPane.YES_OPTION){
				RunScriptAction.getAction().runScript(uri.toString());
			}
			return;
		}
		
		igbService.openURI(uri, fileName, loadGroup, speciesName, loadAsTrack);
		
		if (!mergeSelected) {
			unknown_group_count++;
		}

	}

	protected MergeOptionChooser getFileChooser(String id) {
		chooser = new MergeOptionChooser(id);
		chooser.setMultiSelectionEnabled(true);
	
		/**
		 * The following code implements function check each single file (from file selector or URI input) for known sequence file, 
		 * enable the 'Open as reference sequence' checkbox if yes.
		 * 
		 */
		List<UniFileFilter> filters = getSupportedFiles(FileTypeCategory.Sequence);
		Set<String> all_known_endings = new HashSet<String>();
		for (UniFileFilter filter : filters) {
			all_known_endings.addAll(filter.getExtensions());
		}
		
		final UniFileFilter seq_ref_filter = new UniFileFilter(all_known_endings.toArray(new String[all_known_endings.size()]), "Known Types");
		
		chooser.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (JFileChooser.SELECTED_FILES_CHANGED_PROPERTY.equals(evt.getPropertyName())) { // Single selection included
					File[] files = chooser.getSelectedFiles();
					if(files.length == 1) {
						if(files[0] != null) {
							boolean enableLoadAsSeqCB = seq_ref_filter.accept(files[0]);
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
		
		filters = getSupportedFiles(null);
		filters.add(new UniFileFilter(ScriptProcessorHolder.getInstance().getScriptExtensions().toArray(new String[]{}), "Script File"));
		
		all_known_endings = new HashSet<String>();
		for (UniFileFilter filter : filters) {
			chooser.addChoosableFileFilter(filter);
			all_known_endings.addAll(filter.getExtensions());
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
	
	protected boolean checkFriendlyName(String friendlyName, FileFilter all_known_types) {
		if (!all_known_types.accept(new File(friendlyName))) {
			return false;
		}
		return true;
	}
	
	protected UniFileFilter getAllKnowFilter(){
		Map<String, List<String>> nameToExtensionMap = FileTypeHolder.getInstance().getNameToExtensionMap(null);
		Set<String> all_known_endings = new HashSet<String>();
		//filters.add(new UniFileFilter(ScriptProcessorHolder.getInstance().getScriptExtensions(), "Script File"));
		
		for (String name : nameToExtensionMap.keySet()) {
			all_known_endings.addAll(nameToExtensionMap.get(name));
		}
		all_known_endings.addAll(ScriptProcessorHolder.getInstance().getScriptExtensions());
		
		UniFileFilter all_known_types = new UniFileFilter(
				all_known_endings.toArray(new String[all_known_endings.size()]),
				"Known Types");
		all_known_types.setExtensionListInDescription(false);
		all_known_types.addCompressionEndings(GeneralUtils.compression_endings);
		
		return all_known_types;
	}
	
	protected List<UniFileFilter> getSupportedFiles(FileTypeCategory category){
		Map<String, List<String>> nameToExtensionMap = FileTypeHolder.getInstance().getNameToExtensionMap(category);
		List<UniFileFilter> filters = new ArrayList<UniFileFilter>(nameToExtensionMap.keySet().size() + 1);
		
		for (String name : nameToExtensionMap.keySet()) {
			UniFileFilter uff = new UniFileFilter(nameToExtensionMap.get(name).toArray(new String[]{}), name + " Files");
			uff.addCompressionEndings(GeneralUtils.compression_endings);
			filters.add(uff);
		}
		return filters;
	}
	
	protected abstract String getID();
}
