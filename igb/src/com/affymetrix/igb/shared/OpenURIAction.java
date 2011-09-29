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

import com.affymetrix.genometryImpl.util.ErrorHandler;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.ZipInputStream;

import javax.swing.*;
import java.net.URI;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genometryImpl.symloader.SymLoaderInstNC;
import com.affymetrix.genometryImpl.util.ParserController;
import com.affymetrix.genometryImpl.parsers.useq.ArchiveInfo;
import com.affymetrix.genometryImpl.parsers.graph.BarParser;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.parsers.Bprobe1Parser;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.parsers.useq.USeqGraphParser;
import com.affymetrix.igb.Application;

import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.igb.featureloader.QuickLoad;
import com.affymetrix.igb.view.load.GeneralLoadUtils;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public abstract class OpenURIAction extends GenericAction {

	private static final long serialVersionUID = 1L;

	public static int unknown_group_count = 1;
	public static final String UNKNOWN_SPECIES_PREFIX = BUNDLE.getString("unknownSpecies");
	public static final String UNKNOWN_GENOME_PREFIX = BUNDLE.getString("unknownGenome");
	private static final String SELECT_SPECIES = BUNDLE.getString("speciesCap");
	protected static final GenometryModel gmodel = GenometryModel.getGenometryModel();

	protected void openURI(URI uri, final String fileName, final boolean mergeSelected, final AnnotatedSeqGroup loadGroup, final String speciesName) {
		// If server requires authentication then.
		// If it cannot be authenticated then don't add the feature.
		if (!LocalUrlCacher.isValidURI(uri)) {
			ErrorHandler.errorPanel("UNABLE TO FIND URL", uri + "\n URL provided not found or times out: ");
			return;
		}

		GenericFeature gFeature = getFeature(uri, fileName, speciesName, loadGroup);

		if (gFeature == null) {
			return;
		}

		GeneralLoadView.getLoadView().initVersion(gFeature.gVersion.group.getID());

		if (gFeature.symL != null) {
			addChromosomesForUnknownGroup(fileName, gFeature);
		}

		// force a refresh of this server		
		ServerList.getServerInstance().fireServerInitEvent(ServerList.getServerInstance().getLocalFilesServer(), ServerStatus.Initialized, true, true);

		// Annotated Seq Group must be selected before feature table change call.
		if(gmodel.getSelectedSeqGroup() != gFeature.gVersion.group){
			gmodel.setSelectedSeqGroup(gFeature.gVersion.group);
		}

		GeneralLoadView.getLoadView().createFeaturesTable();

		if (!mergeSelected) {
			unknown_group_count++;
		}

	}

	public GenericFeature getFeature(URI uri, String fileName, String speciesName, AnnotatedSeqGroup loadGroup) {
		boolean isloaded = GeneralLoadView.getLoadView().getFeatureTree().isLoaded(uri);
		// Test to determine if a feature with this uri is contained in the load mode table
		if (isloaded) {
			ErrorHandler.errorPanel("Cannot add same feature",
					"The feature " + uri + " has already been added.");
			return null;
		}
		boolean isContained = GeneralLoadView.getLoadView().getFeatureTree().isContained(loadGroup, uri);
		// Test to determine if a feature already exist in the feature tree
		if (isContained) {
			GeneralLoadView.getLoadView().getFeatureTree().updateTree(uri);
			return null;
		}

		GenericVersion version = GeneralLoadUtils.getLocalFilesVersion(loadGroup, speciesName);
		version = setVersion(uri, loadGroup, version);

		// In case of BAM
		if (version == null) {
			return null;
		}

		// handle URL case.
		String uriString = uri.toString();
		int httpIndex = uriString.toLowerCase().indexOf("http:");
		if (httpIndex > -1) {
			// Strip off initial characters up to and including http:
			// Sometimes this is necessary, as URLs can start with invalid "http:/"
			uriString = GeneralUtils.convertStreamNameToValidURLName(uriString);
			uri = URI.create(uriString);
		}
		boolean autoload = PreferenceUtils.getBooleanParam(PreferenceUtils.AUTO_LOAD, PreferenceUtils.default_auto_load);
		GenericFeature gFeature = new GenericFeature(fileName, null, version, new QuickLoad(version, uri), File.class, autoload);

		version.addFeature(gFeature);

		gFeature.setVisible(); // this should be automatically checked in the feature tree

		return gFeature;
	}

	/**
	 * Handle file formats that has SeqGroup info.
	 * @param uri
	 * @param loadGroup
	 * @param version
	 * @return
	 */
	private static GenericVersion setVersion(URI uri, AnnotatedSeqGroup loadGroup, GenericVersion version) {
		String unzippedStreamName = GeneralUtils.stripEndings(uri.toString());
		String extension = ParserController.getExtension(unzippedStreamName);

		if(extension.equals(".sam")){
			if(!LocalUrlCacher.isFile(uri)){
				ErrorHandler.errorPanel("Cannot open file","Only local sam file is permitted.");
				version = null;
			}
		}if (extension.equals(".bam")) {
			if (!handleBam(uri)) {
				ErrorHandler.errorPanel("Cannot open file", "Could not find index file");
				version = null;
			}
		} else if (extension.equals(".useq")) {
			loadGroup = handleUseq(uri, loadGroup);
			version = GeneralLoadUtils.getLocalFilesVersion(loadGroup, loadGroup.getOrganism());
		} else if (extension.equals(".bar")) {
			loadGroup = handleBar(uri, loadGroup);
			version = GeneralLoadUtils.getLocalFilesVersion(loadGroup, loadGroup.getOrganism());
		} else if (extension.equals(".bp1") || extension.equals(".bp2")) {
			loadGroup = handleBp(uri, loadGroup);
			version = GeneralLoadUtils.getLocalFilesVersion(loadGroup, loadGroup.getOrganism());
		}

		return version;
	}

	private static boolean handleBam(URI uri) {
		try {
			return BAM.hasIndex(uri);
		} catch (IOException ex) {
			Logger.getLogger(OpenURIAction.class.getName()).log(Level.SEVERE, null, ex);
		}
		return false;
	}

	/**
	 * Get AnnotatedSeqGroup for BAR file format.
	 * @param uri
	 * @param group
	 * @return
	 */
	private static AnnotatedSeqGroup handleBar(URI uri, AnnotatedSeqGroup group) {
		InputStream istr = null;
		try {
			istr = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
			List<AnnotatedSeqGroup> groups = BarParser.getSeqGroups(uri.toString(), istr, group, gmodel);
			if (groups.isEmpty()) {
				return group;
			}

			//TODO: What if there are more than one seq group ?
			if (groups.size() > 1) {
				Logger.getLogger(BarParser.class.getName()).log(
						Level.WARNING, "File {0} has more than one group", new Object[]{uri.toString()});
			}

			return groups.get(0);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(istr);
		}

		return group;
	}

	/**
	 * Get AnnotatedSeqGroup for USEQ file format.
	 * @param uri
	 * @param group
	 * @return
	 */
	private static AnnotatedSeqGroup handleUseq(URI uri, AnnotatedSeqGroup group) {
		InputStream istr = null;
		ZipInputStream zis = null;
		try {
			istr = LocalUrlCacher.getInputStream(uri.toURL());
			zis = new ZipInputStream(istr);
			zis.getNextEntry();
			ArchiveInfo archiveInfo = new ArchiveInfo(zis, false);
			AnnotatedSeqGroup gr = USeqGraphParser.getSeqGroup(archiveInfo.getVersionedGenome(), gmodel);
			if (gr != null) {
				return gr;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(istr);
			GeneralUtils.safeClose(zis);
		}

		return group;
	}

	private static AnnotatedSeqGroup handleBp(URI uri, AnnotatedSeqGroup group) {
		InputStream istr = null;
		try {
			istr = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
			AnnotatedSeqGroup gr = Bprobe1Parser.getSeqGroup(istr, group, gmodel);
			if (gr != null) {
				return gr;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(istr);
		}

		return group;
	}

	private void addChromosomesForUnknownGroup(final String fileName, final GenericFeature gFeature) {
		if (((QuickLoad) gFeature.symL).getSymLoader() instanceof SymLoaderInstNC) {
			((QuickLoad) gFeature.symL).loadAllSymmetriesThread(gFeature);
			// force a refresh of this server. This forces creation of 'genome' sequence.
			ServerList.getServerInstance().fireServerInitEvent(ServerList.getServerInstance().getLocalFilesServer(), ServerStatus.Initialized, true, true);
			return;
		}

		final AnnotatedSeqGroup loadGroup = gFeature.gVersion.group;
		final String message = "Retrieving chromosomes for " + fileName;
		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

			@Override
			protected Boolean doInBackground() throws Exception {

				try {
					Application.getSingleton().addNotLockedUpMsg(message);
					for (BioSeq seq : gFeature.symL.getChromosomeList()) {
						loadGroup.addSeq(seq.getID(), seq.getLength(), gFeature.symL.uri.toString());
					}
					return true;
				} catch (Exception ex) {
					((QuickLoad) gFeature.symL).logException(ex);
					if (Application.confirmPanel("Unable to retrieve chromosome. \n Would you like to remove feature " + gFeature.featureName)) {
						if (gFeature.gVersion.removeFeature(gFeature)) {
							SeqGroupView.getInstance().refreshTable();
						}
					}
					return false;
				}

			}

			@Override
			protected void done() {
				boolean result = true;
				try {
					result = get();
				} catch (Exception ex) {
					Logger.getLogger(OpenURIAction.class.getName()).log(Level.SEVERE, null, ex);
				}
				ServerList.getServerInstance().fireServerInitEvent(ServerList.getServerInstance().getLocalFilesServer(), ServerStatus.Initialized, true, true);
				if (result) {
					SeqGroupView.getInstance().refreshTable();
					if (loadGroup.getSeqCount() > 0 && gmodel.getSelectedSeq() == null) {
						// select a chromosomes
						gmodel.setSelectedSeq(loadGroup.getSeq(0));
					}
				} else {
					//Feature was remove
					GeneralLoadView.getLoadView().refreshTreeView();
					GeneralLoadView.getLoadView().createFeaturesTable();
				}
				Application.getSingleton().removeNotLockedUpMsg(message);
			}
		};
		ThreadUtils.getPrimaryExecutor(gFeature).execute(worker);
	}

	protected boolean openURI(URI uri) {
		String unzippedName = GeneralUtils.getUnzippedName(uri.toString());
		String friendlyName = unzippedName.substring(unzippedName.lastIndexOf("/") + 1);

		if (!checkFriendlyName(friendlyName)) {
			return false;
		}

		AnnotatedSeqGroup loadGroup = gmodel.getSelectedSeqGroup();
		boolean mergeSelected = loadGroup == null ? false : true;
		if (loadGroup == null) {
			loadGroup = gmodel.addSeqGroup(UNKNOWN_GENOME_PREFIX + " " + unknown_group_count);
		}

		String speciesName = GeneralLoadView.getLoadView().getSelectedSpecies();
		if (SELECT_SPECIES.equals(speciesName)) {
			speciesName = UNKNOWN_SPECIES_PREFIX + " " + unknown_group_count;
		}
		openURI(uri, friendlyName, mergeSelected, loadGroup, speciesName);

		return true;
	}

	protected boolean checkFriendlyName(String friendlyName) {
		return true;
	}
}
