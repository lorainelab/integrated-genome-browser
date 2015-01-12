/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.google.common.base.Optional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

/**
 * @version $Id: ServiceUtils.java 7505 2011-02-10 20:27:35Z hiralv $
 */
public final class ServiceUtils {

	private static final ServiceUtils instance = new ServiceUtils();
	private static final String UNKNOWN_GENOME_VERSION = "unknown";

	private ServiceUtils() {
		super();
	}

	public static final ServiceUtils getInstance() {
		return instance;
	}

	public GenericFeature getFeature(AnnotatedSeqGroup seqGroup, GenericServer gServer, String feature_url, boolean showErrorForUnsupported) {
		GenericFeature feature = null;

		URI uri = URI.create(feature_url);
		GenericVersion gVersion = seqGroup.getVersionOfServer(gServer);
		if (gVersion == null && gServer.serverType != ServerTypeI.LocalFiles) {
			Logger.getLogger(ServiceUtils.class.getName()).log(
					Level.WARNING, "Couldn''t find version {0} in server {1}",
					new Object[]{seqGroup.getID(), gServer.serverName});
			return null;
		}

		if (gVersion != null) {
			feature = GeneralUtils.findFeatureWithURI(gVersion.getFeatures(), uri);
		}

		if (feature == null && gServer.serverType == ServerTypeI.LocalFiles) {
			String uriString = uri.toASCIIString().toLowerCase();
			String unzippedStreamName = GeneralUtils.stripEndings(uriString);
			String extension = GeneralUtils.getExtension(unzippedStreamName);
			extension = extension.substring(extension.indexOf('.') + 1);

			if (FileTypeHolder.getInstance().getFileTypeHandler(extension) == null) {
				if(showErrorForUnsupported) {
					ErrorHandler.errorPanel("File type " + extension + " is not supported");
				}
				Logger.getLogger(ServiceUtils.class.getName()).log(
						Level.SEVERE, "File type {0} is not supported", extension);
				return null;
			}

			// If feature doesn't not exist then add it.
			String fileName = feature_url.substring(feature_url.lastIndexOf('/') + 1, feature_url.length());
			feature = GeneralLoadUtils.getFeature(uri, fileName, seqGroup.getOrganism(), seqGroup, false);

		}

		return feature;
	}

	/**
	 * Finds server from server url and enables it, if found disabled.
	 *
	 * @param server_url	Server url string.
	 * @return	Returns GenericServer if found else null.
	 */
	public GenericServer loadServer(String server_url) {
		GenericServer gServer = ServerList.getServerInstance().getServer(server_url);
		if (gServer == null) {
			

			gServer = ServerList.getServerInstance().getLocalFilesServer();

		} else if (!gServer.isEnabled()) {
			// enable the server for this session only
			gServer.enableForSession();
			GeneralLoadUtils.discoverServer(gServer);
		}
		return gServer;
	}

	public Optional<AnnotatedSeqGroup> determineAndSetGroup(final String version) {
		final AnnotatedSeqGroup group;
		GenometryModel gmodel = GenometryModel.getInstance();
		if (StringUtils.isBlank(version) || UNKNOWN_GENOME_VERSION.equals(version)) {
			group = gmodel.getSelectedSeqGroup();
		} else {
			group = gmodel.getSeqGroup(version);
		}
		if (group != null && !group.equals(gmodel.getSelectedSeqGroup())) {
			GeneralLoadView.getLoadView().initVersion(version);
			gmodel.setSelectedSeqGroup(group);
		}
		return Optional.fromNullable(group);
	}

	/**
	 * This handles the "select" API parameter. The "select" parameter can be
	 * followed by one or more comma separated IDs in the form:
	 * &select=<id_1>,<id_2>,...,<id_n> Example: "&select=EPN1,U2AF2,ZNF524"
	 * Each ID that exists in IGB's ID to symmetry hash will be selected, even
	 * if the symmetries lie on different sequences.
	 *
	 * @param selectParam The select parameter passed in through the API
	 */
	public void performSelection(String selectParam) {
		
		if (StringUtils.isBlank(selectParam)) {
			return;
		}

		// split the parameter by commas
		String[] ids = selectParam.split(",");

		if (ids.length == 0) {
			return;
		}

		AnnotatedSeqGroup group = GenometryModel.getInstance().getSelectedSeqGroup();
		List<SeqSymmetry> sym_list = new ArrayList<>(ids.length);
		for (String id : ids) {
			sym_list.addAll(group.findSyms(id));
		}

		GenometryModel.getInstance().setSelectedSymmetriesAndSeq(sym_list, ServiceUtils.class);
	}

	public void selectFeatureAndCenterZoomStripe(String selectParam) {

		if (StringUtils.isBlank(selectParam)) {
			return;
		}

		// split the parameter by commas
		String[] ids = selectParam.split(",");

		if (ids.length == 0) {
			return;
		}

		AnnotatedSeqGroup group = GenometryModel.getInstance().getSelectedSeqGroup();
		List<SeqSymmetry> sym_list = new ArrayList<>(ids.length);
		SeqSpan span;
		double midpoint = -1;
		
		for (String id : ids) {
			for(SeqSymmetry sym : group.findSyms(id)){
				span = sym.getSpan(0);
				if(midpoint == -1){
					midpoint = span.getMin() + (span.getLengthDouble()/2);
				}
				sym_list.add(sym);
			}
			
		}

		GenometryModel.getInstance().setSelectedSymmetriesAndSeq(sym_list, IGB.getSingleton().getMapView().getSeqMap());
		IGB.getSingleton().getMapView().setZoomSpotX(midpoint);
		IGB.getSingleton().getMapView().setZoomSpotY(0);
	}
}
