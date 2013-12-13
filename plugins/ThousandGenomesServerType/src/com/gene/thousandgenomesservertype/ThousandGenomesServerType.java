package com.gene.thousandgenomesservertype;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.quickload.QuickLoadSymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.SpeciesLookup;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.util.VersionDiscoverer;

public class ThousandGenomesServerType implements ServerTypeI {
	private static final String name = "1000 genomes";
	private static final String VERSION = "H_sapiens_Feb_2009";
	private static final int MAX_DIRS = 12;
	private static final boolean DEBUG = false;
	public static final int ordinal = Integer.MAX_VALUE;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();
	private static final ThousandGenomesServerType instance = new ThousandGenomesServerType();
	public static ThousandGenomesServerType getInstance() {
		return instance;
	}

	private ThousandGenomesServerType() {
		super();
	}

	@Override
	public int compareTo(ServerTypeI o) {
		return ordinal - o.getOrdinal();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getOrdinal() {
		return ordinal;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public String formatURL(String url) {
		return url;
	}

	@Override
	public Object getServerInfo(String url, String name) {
		return VERSION;
	}

	@Override
	public String adjustURL(String url) {
		return url;
	}

	@Override
	public boolean loadStrategyVisibleOnly() {
		return false;
	}

	private void addFile(GenericVersion gVersion, boolean autoload, String url) {
		if (DEBUG) {
			System.out.println(">>>>> addFile " + url);
		}
		String featureName = url.substring(Activator._1000_GENOMES_US.length());
		FileTypeHandler fth = FileTypeHolder.getInstance().getFileTypeHandlerForURI(url);
		if (fth == null) {
			return;
		}
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return;
		}
		//TODO: Use symloader created below
		SymLoader symL = fth.createSymLoader(uri, featureName, gVersion.group);
		QuickLoadSymLoader quickLoadSymLoader = new QuickLoadSymLoader(uri, featureName, gVersion.group);
		Map<String, String> type_props = new HashMap<String, String>();
		gVersion.addFeature(
				new GenericFeature(
						featureName, type_props, gVersion, quickLoadSymLoader, null, autoload));
	}

	private void addIndexFile(GenericVersion gVersion, boolean autoload, String url) {
		if (DEBUG) {
			System.out.println(">>>>> addIndexFile " + url);
		}
		InputStream is = null;
		try {
			is = new URL(url).openConnection().getInputStream();
			String dataString = IOUtils.toString(is, "UTF-8");
			String urlDir = url.substring(0, url.lastIndexOf('/'));
			String[] lines = dataString.split("\n");
			for (String line : lines) {
				if (line.startsWith("#")) {
					continue;
				}
				int firstPos = line.indexOf('/');
				int lastPos = line.indexOf('\t');
				String fileURL = urlDir + "/" + line.substring(firstPos + 1, lastPos);
				addFile(gVersion, autoload, fileURL);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void addDirectory(GenericVersion gVersion, boolean autoload, String url) {
		if (DEBUG) {
			System.out.println(">>>>> addDirectory " + url);
		}
	InputStream is = null;
		try {
			is = new URL(url).openConnection().getInputStream();
			String dataString = IOUtils.toString(is, "UTF-8");
			String[] lines = dataString.split("\n");
			if (lines.length > MAX_DIRS) {
				if (DEBUG) {
					System.out.println(">>>>> skipping directory " + url + " size " + lines.length);
				}
				return;
			}
			for (String line : lines) {
				boolean isDir = line.startsWith("d");
				int pos = line.lastIndexOf(' ');
				String urlSuffix = line.substring(pos + 1);
				if (isDir) {
					addDirectory(gVersion, autoload, url + urlSuffix + "/");
				}
				else if (urlSuffix.endsWith(".index")) {
					addIndexFile(gVersion, autoload, url + urlSuffix);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void discoverFeatures(GenericVersion gVersion, boolean autoload) {
		addDirectory(gVersion, autoload, Activator._1000_GENOMES_US);
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

	@Override
	public boolean getSpeciesAndVersions(GenericServer gServer,
			GenericServer primaryServer, URL primaryURL,
			VersionDiscoverer versionDiscoverer) {
		String genomeID = (String)gServer.serverObj;
		String genomeName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), genomeID);
		String versionName, speciesName;
		// Retrieve group identity, since this has already been added in QuickLoadServerModel.
		Set<GenericVersion> gVersions = gmodel.addSeqGroup(genomeName).getEnabledVersions();
		if (!gVersions.isEmpty()) {
			// We've found a corresponding version object that was initialized earlier.
			versionName = GeneralUtils.getPreferredVersionName(gVersions);
			speciesName = versionDiscoverer.versionName2Species(versionName);
		} else {
			versionName = genomeName;
			speciesName = SpeciesLookup.getSpeciesName(genomeName);
		}
		versionDiscoverer.discoverVersion(genomeID, versionName, gServer, gServer, speciesName);
		return true;
	}

	@Override
	public Map<String, List<? extends SeqSymmetry>> loadFeatures(SeqSpan span,
			GenericFeature feature) throws Exception {
		return (((QuickLoadSymLoader) feature.symL).loadFeatures(span, feature));
	}

	@Override
	public boolean isAuthOptional() {
		return false;
	}

	@Override
	public boolean getResidues(GenericVersion versions, String genomeVersionName,
			BioSeq aseq, int min, int max, SeqSpan span) {
		return false;
	}

	@Override
	public boolean processServer(GenericServer gServer, String path) {
		return false;
	}

	@Override
	public void removeServer(GenericServer server) {
		// Do Nothing for now
	}
	
	@Override
	public boolean isSaveServersInPrefs() {
		return false;
	}
	
	@Override
	public String getFriendlyURL (GenericServer gServer) {
		if (gServer.URL == null) {
			return null;
		}
		String tempURL = gServer.URL;
		if (tempURL.endsWith("/")) {
			tempURL = tempURL.substring(0, tempURL.length() - 1);
		}
		if (gServer.serverType != null) {
			tempURL = gServer.serverType.adjustURL(tempURL);
		}
		return tempURL;
	}
	
	@Override 
	public boolean useMirrorSite(GenericServer server) {
		return false;
	}
}
