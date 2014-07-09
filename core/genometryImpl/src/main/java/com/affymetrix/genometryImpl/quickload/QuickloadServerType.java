package com.affymetrix.genometryImpl.quickload;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryConstants;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser.AnnotMapElt;
import com.affymetrix.genometryImpl.symloader.BNIB;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symloader.TwoBitNew;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.*;
import java.net.HttpURLConnection;

public class QuickloadServerType implements ServerTypeI {

	enum QFORMAT {

		BNIB,
		VTWOBIT,
		TWOBIT,
		FA
	};
	private static final boolean DEBUG = false;
	private static final String name = "Quickload";
	public static final int ordinal = 20;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private static final List<QuickLoadSymLoaderHook> quickLoadSymLoaderHooks = new ArrayList<QuickLoadSymLoaderHook>();
	/**
	 * Private copy of the default Synonym lookup
	 *
	 * @see SynonymLookup#getDefaultLookup()
	 */
	private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();
	/**
	 * For files too be looked up on server. *
	 */
	private static final Set<String> quickloadFiles = new HashSet<String>();

	/**
	 * Add files to be looked up. *
	 */
	static {
		quickloadFiles.add(Constants.annotsTxt);
		quickloadFiles.add(Constants.annotsXml);
		quickloadFiles.add(Constants.modChromInfoTxt);
		quickloadFiles.add(Constants.liftAllLft);
		quickloadFiles.add(Constants.genomeTxt);
	}
	private static final QuickloadServerType instance = new QuickloadServerType();

	public static QuickloadServerType getInstance() {
		return instance;
	}

	private QuickloadServerType() {
		super();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int compareTo(ServerTypeI o) {
		return ordinal - o.getOrdinal();
	}

	@Override
	public int getOrdinal() {
		return ordinal;
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * @return true if file may not exist else false.
	 */
	private boolean getFileAvailability(String fileName) {
		if (fileName.equals(Constants.annotsTxt) || fileName.equals(Constants.annotsXml) || fileName.equals(Constants.liftAllLft)) {
			return true;
		}

		return false;
	}

	/**
	 * Gets files for a genome and copies it to it's directory.
	 * @param local_path	Local path from where mapping is to saved.
	 */
	private boolean getAllFiles(GenericServer gServer, String genome_name, String local_path) {
		File file;
		Set<String> files = quickloadFiles;

		String server_path = gServer.URL + "/" + genome_name;
		local_path += "/" + genome_name;
		GeneralUtils.makeDir(local_path);
		boolean fileMayNotExist;
		for (String fileName : files) {
			fileMayNotExist = getFileAvailability(fileName);

			file = GeneralUtils.getFile(server_path + "/" + fileName, fileMayNotExist);

			if ((file == null && !fileMayNotExist)) {
				return false;
			}

			if (!GeneralUtils.moveFileTo(file, fileName, local_path)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean processServer(GenericServer gServer, String path) {
		File file = GeneralUtils.getFile(gServer.URL + Constants.contentsTxt, false);

		String quickloadStr = null;
		quickloadStr = (String) gServer.serverObj;

		QuickLoadServerModel quickloadServer = new QuickLoadServerModel(quickloadStr);

		List<String> genome_names = quickloadServer.getGenomeNames();
		if (!GeneralUtils.moveFileTo(file, Constants.contentsTxt, path)) {
			return false;
		}

		for (String genome_name : genome_names) {
			if (!getAllFiles(gServer, genome_name, path)) {
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Could not find all files for {0} !!!", gServer.serverName);
				return false;
			}
		}

		return true;
	}

	@Override
	public String formatURL(String url) {
		return url.endsWith("/") ? url : url + "/";
	}

	@Override
	public Object getServerInfo(String url, String name) {
		return formatURL(url);
	}

	@Override
	public String adjustURL(String url) {
		return url;
	}

	@Override
	public boolean loadStrategyVisibleOnly() {
		return false;
	}

	public static void addQuickLoadSymLoaderHook(QuickLoadSymLoaderHook quickLoadSymLoaderHook) {
		quickLoadSymLoaderHooks.add(quickLoadSymLoaderHook);
	}

	private QuickLoadSymLoader getQuickLoad(GenericVersion version, String featureName) {
		URI uri = determineURI(version, featureName);
		QuickLoadSymLoader quickLoadSymLoader = new QuickLoadSymLoader(uri, featureName, version.group);
		for (QuickLoadSymLoaderHook quickLoadSymLoaderHook : quickLoadSymLoaderHooks) {
			quickLoadSymLoader = quickLoadSymLoaderHook.processQuickLoadSymLoader(quickLoadSymLoader);
		}
		return quickLoadSymLoader;
	}

	private static URI determineURI(GenericVersion version, String featureName) {
		URI uri = null;

		if (version.gServer.URL == null || version.gServer.URL.length() == 0) {
			int httpIndex = featureName.toLowerCase().indexOf("http:");
			if (httpIndex > -1) {
				// Strip off initial characters up to and including http:
				// Sometimes this is necessary, as URLs can start with invalid "http:/"
				featureName = GeneralUtils.convertStreamNameToValidURLName(featureName);
				uri = URI.create(featureName);
			} else {
				uri = (new File(featureName)).toURI();
			}
		} else {
			String fileName = determineFileName(version, featureName);
			int httpIndex = fileName.toLowerCase().indexOf("http:");
			int httpsIndex = fileName.toLowerCase().indexOf("https:");
			int ftpIndex = fileName.toLowerCase().indexOf("ftp:");
			if (httpIndex > -1 || httpsIndex > -1 || ftpIndex > -1) {
				uri = URI.create(fileName);
			} else {
				uri = URI.create(
						version.gServer.serverObj // Changed from 'version.gServer.URL' since quickload uses serverObj
						+ version.versionID + "/"
						+ determineFileName(version, featureName));
			}
		}
		
		return uri;
	}

	private static String determineFileName(GenericVersion version, String featureName) {
		URL quickloadURL;
		try {
			quickloadURL = new URL((String) version.gServer.serverObj);
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
			return "";
		}

		QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL);
		List<AnnotMapElt> annotsList = quickloadServer.getAnnotsMap(version.versionID);

		// Linear search, but over a very small list.
		for (AnnotMapElt annotMapElt : annotsList) {
			if (annotMapElt.title.equals(featureName)) {
				return annotMapElt.fileName;
			}
		}
		return "";
	}

	@Override
	public void discoverFeatures(GenericVersion gVersion, boolean autoload) {
		// Discover feature names from QuickLoad
		try {
			URL quickloadURL = new URL((String) gVersion.gServer.serverObj);
			if (DEBUG) {
				System.out.println("Discovering Quickload features for " + gVersion.versionName + ". URL:" + (String) gVersion.gServer.serverObj);
			}

			QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL);
			List<String> typeNames = quickloadServer.getTypes(gVersion.versionName);
			if (typeNames == null) {
				String errorText = MessageFormat.format(GenometryConstants.BUNDLE.getString("quickloadGenomeError"), gVersion.gServer.serverName, gVersion.group.getOrganism(), gVersion.versionName);
				ErrorHandler.errorPanelWithReportBug(gVersion.gServer.serverName, errorText, Level.SEVERE);
				return;
			}
			
			for (String type_name : typeNames) {	
				if (type_name == null || type_name.length() == 0) {
					System.out.println("WARNING: Found empty feature name in " + gVersion.versionName + ", " + gVersion.gServer.serverName);
					continue;
				}
				if (DEBUG) {
					System.out.println("Adding feature " + type_name);
				}
				Map<String, String> type_props = quickloadServer.getProps(gVersion.versionName, type_name);
				gVersion.addFeature(
						new GenericFeature(
						type_name, type_props, gVersion, getQuickLoad(gVersion, type_name), null, autoload));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void discoverChromosomes(Object versionSourceObj) {
	}

	@Override
	public boolean hasFriendlyURL() {
		return true;
	}

	@Override
	public boolean canHandleFeature() {
		return false;
	}

	/**
	 * Discover genomes from Quickload
	 * @return false if there's an obvious failure.
	 */
	@Override
	public boolean getSpeciesAndVersions(GenericServer gServer,
	        GenericServer primaryServer, URL primaryURL,
		    VersionDiscoverer versionDiscoverer) {
		URL quickloadURL = null;
		try {
			quickloadURL = new URL((String) gServer.serverObj);
		} catch (MalformedURLException ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
			return false;
		}
		QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL, primaryURL, primaryServer);

		if (quickloadServer == null) {
			System.out.println("ERROR: No quickload server model found for server: " + gServer);
			return false;
		}
		if (!ping(quickloadURL.toString(), 3000)) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not reach url:", quickloadURL);
			return false;
		}
		
		quickloadServer.loadGenomeNames();
		List<String> genomeList = quickloadServer.getGenomeNames();
		if (genomeList == null || genomeList.isEmpty()) {
			System.out.println("WARNING: No species found in server: " + gServer);
			return false;
		}

		//update species.txt with information from the server.
		if (quickloadServer.hasSpeciesTxt()) {
			try {
				SpeciesLookup.load(quickloadServer.getSpeciesTxt());
			} catch (IOException ex) {
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "No species.txt found at this quickload server.", ex);
			}
		}
		for (String genomeID : genomeList) {
			String genomeName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), genomeID);
			String versionName, speciesName;
			// Retrieve group identity, since this has already been added in QuickLoadServerModel.
			Set<GenericVersion> gVersions = gmodel.addSeqGroup(genomeName).getEnabledVersions();
			if (!gVersions.isEmpty()) {
				// We've found a corresponding version object that was initialized earlier.
				versionName = GeneralUtils.getPreferredVersionName(gVersions);
//				speciesName = versionName2species.get(versionName);
				speciesName = versionDiscoverer.versionName2Species(versionName);
			} else {
				versionName = genomeName;
				speciesName = SpeciesLookup.getSpeciesName(genomeName);
			}
			versionDiscoverer.discoverVersion(genomeID, versionName, gServer, quickloadServer, speciesName);
		}
		return true;
	}
	
	//Note  exception may be thrown on invalid SSL certificates.
	public static boolean ping(String url, int timeout) {
		try {
			if (url.startsWith("file:")) {
				File file = new File(url.substring(5));
				return file.exists();
			} else {
				HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
				connection.setConnectTimeout(timeout);
				connection.setReadTimeout(timeout);
				connection.setRequestMethod("HEAD");
				int responseCode = connection.getResponseCode();
				return (200 <= responseCode && responseCode <= 399);
			}
		} catch (Exception exception) {
			return false;
		}
	}

	@Override
	public Map<String, List<? extends SeqSymmetry>> loadFeatures(SeqSpan span, GenericFeature feature) throws Exception {
		return (((QuickLoadSymLoader) feature.symL).loadFeatures(span, feature));
	}

	@Override
	public boolean isAuthOptional() {
		return false;
	}

	// Generate URI (e.g., "http://www.bioviz.org/das2/genome/A_thaliana_TAIR8/chr1.bnib")
	private String generateQuickLoadURI(String common_url, String vPath, QFORMAT Format) {
		Logger.getLogger(this.getClass().getName()).log(Level.FINE, "trying to load residues via Quickload");
		switch (Format) {
			case BNIB:
				common_url += "bnib";
				break;

			case FA:
				common_url += "fa";
				break;

			case VTWOBIT:
				common_url = vPath;
				break;

			case TWOBIT:
				common_url += "2bit";
				break;

		}

		return common_url;
	}

	private QFORMAT determineFormat(String common_url, String vPath) {

		for (QFORMAT format : QFORMAT.values()) {
			String url_path = generateQuickLoadURI(common_url, vPath, format);
			if (LocalUrlCacher.isValidURL(url_path)) {
				Logger.getLogger(this.getClass().getName()).log(Level.FINE,
						"  Quickload location of " + format + " file: {0}", url_path);

				return format;
			}
		}

		return null;
	}

	private SymLoader determineLoader(String common_url, String vPath, AnnotatedSeqGroup seq_group, String seq_name) {
		QFORMAT format = determineFormat(common_url, vPath);

		if (format == null) {
			return null;
		}

		URI uri = null;
		try {
			uri = new URI(generateQuickLoadURI(common_url, vPath, format));
		} catch (URISyntaxException ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
		}

		switch (format) {
			case BNIB:
				return new BNIB(uri, "", seq_group);

			case VTWOBIT:
				return new TwoBitNew(uri, "", seq_group);

			case TWOBIT:
				return new TwoBitNew(uri, "", seq_group);

//			case FA:
//				return new Fasta(uri, seq_group);
		}

		return null;
	}

	/**
	 * Get the partial residues from the specified QuickLoad server.
	 * @return residue String.
	 */
	private String GetQuickLoadResidues(
			GenericServer server, GenericVersion version, AnnotatedSeqGroup seq_group,
			String seq_name, String root_url, SeqSpan span, BioSeq aseq) {
		String common_url = "";
		String path = "";
		SymLoader symloader;
		try {
			URL quickloadURL = new URL((String) server.serverObj);
			QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL);
			path = quickloadServer.getPath(version.versionName, seq_name);
			common_url = root_url + path + ".";
			String vPath = root_url + quickloadServer.getPath(version.versionName, version.versionName) + ".2bit";

			symloader = determineLoader(common_url, vPath, seq_group, seq_name);

			if (symloader != null) {
				return symloader.getRegionResidues(span);
			}
		} catch (Exception ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	@Override
	public boolean getResidues(GenericVersion version, String genomeVersionName,
			BioSeq aseq, int min, int max, SeqSpan span) {
		String seq_name = aseq.getID();
		AnnotatedSeqGroup seq_group = aseq.getSeqGroup();
		String residues = GetQuickLoadResidues(version.gServer, version, seq_group, seq_name, (String)version.gServer.serverObj, span, aseq);
		if (residues != null) {
			BioSeq.addResiduesToComposition(aseq, residues, span);
			return true;
		}
		return false;
	}

	@Override
	public void removeServer(GenericServer server) {
		QuickLoadServerModel.removeQLModelForURL(server.URL);
	}
	
	@Override
	public boolean isSaveServersInPrefs() {
		return true;
	}
	
	@Override
	public String getFriendlyURL (GenericServer gServer) {
		if (gServer.serverObj == null) {
			return null;
		}
		String tempURL = (String)gServer.serverObj;
		if (tempURL.endsWith("/")) {
			tempURL = tempURL.substring(0, tempURL.length() - 1);
		}
		if (gServer.serverType != null) {
			tempURL = gServer.serverType.adjustURL(tempURL);
		}
		return tempURL;
	}
	
	@Override
	public boolean useMirrorSite(GenericServer gServer) {
		if (gServer.mirrorURL != null && gServer.mirrorURL != gServer.serverObj && LocalUrlCacher.isValidURL(gServer.mirrorURL)) {
			return true;
		}
		return false;
	}
}
