package com.affymetrix.genometryImpl.das;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.SpeciesLookup;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.util.VersionDiscoverer;

public class DasServerType implements ServerTypeI {
	/** boolean to indicate should script continue to run if error occurs **/
	private static final boolean exitOnError = false;
	private static final String dsn = "dsn.xml";
	private static final String name = "DAS";
	public static final int ordinal = 20;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	/**
	 * Private copy of the default Synonym lookup
	 * @see SynonymLookup#getDefaultLookup()
	 */
	private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();
	private static final DasServerType instance = new DasServerType();
	public static DasServerType getInstance() {
		return instance;
	}

	private DasServerType() {
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
	 * Returns server path for a mapping on Das server.
	 * @param id	Genome id
	 * @param server	Server url.
	 * @param mapping	File name.
	 * @return
	 */
	private String getPath(String id, URL server, String file){
		try {
			URL server_path = new URL(server, id + "/" + file);
			return server_path.toExternalForm();
		} catch (MalformedURLException ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	/**
	 * Gets files for a genome and copies it to it's directory.
	 * @param server_path	Server path from where mapping is to be copied.
	 * @param local_path	Local path from where mapping is to saved.
	 */
	@SuppressWarnings("unused")
	private boolean getAllDasFiles(String id, URL server, URL master, String local_path){
		local_path += "/" + id;
		GeneralUtils.makeDir(local_path);

		File file;
		final Map<String, String> DasFilePath = new HashMap<String, String>();

		String entry_point = getPath(master.getPath(),master, DasSource.ENTRY_POINTS);
		
		String types = getPath(id,server,DasSource.TYPES);

		DasFilePath.put(entry_point, DasSource.ENTRY_POINTS + Constants.xml_ext);
		DasFilePath.put(types, DasSource.TYPES + Constants.xml_ext);

		for(Entry<String, String> fileDet : DasFilePath.entrySet()){
			file = GeneralUtils.getFile(fileDet.getKey(), false);

			if((file == null || !GeneralUtils.moveFileTo(file,fileDet.getValue(),local_path)) && exitOnError)
				return false;

		}

		return true;
	}

	@Override
	public boolean processServer(GenericServer gServer, String path) {
		File file = GeneralUtils.getFile(gServer.URL, false);
		if(!GeneralUtils.moveFileTo(file,dsn,path))
			return false;
		
		DasServerInfo server = (DasServerInfo) gServer.serverObj;
		Map<String, DasSource> sources = server.getDataSources();

		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Couldn't find species for server: ",gServer);
			return false;
		}

		for (DasSource source : sources.values()) {
			
			if(!getAllDasFiles(source.getID(),source.getServerURL(), source.getMasterURL(), path)){
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Could not find all files for {0} !!!", gServer.serverName);
				return false;
			}

			for(String src : source.getSources()){
				if(!getAllDasFiles(src,source.getServerURL(), source.getMasterURL(), path)){
					Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Could not find all files for {0} !!!", gServer.serverName);
					return false;
				}
			}

		}

		return true;
	}

	@Override
	public String formatURL(String url) {
		while (url.endsWith("/")) {
			url = url.substring(0, url.length()-1);
		}
		return url;
	}

	@Override
	public Object getServerInfo(String url, String name) {
		return new DasServerInfo(url);
	}

	@Override
	public String adjustURL(String url) {
		String tempURL = url;
		if (tempURL.endsWith("/dsn")) {
			tempURL = tempURL.substring(0, tempURL.length() - 4);
		}
		return tempURL;
	}

	@Override
	public boolean loadStrategyVisibleOnly() {
		return true;
	}

	@Override
	public void discoverFeatures(GenericVersion gVersion, boolean autoload) {
		DasSource version = (DasSource) gVersion.versionSourceObj;
		for (Entry<String,String> type : version.getTypes().entrySet()) {
			String type_name = type.getKey();
			if (type_name == null || type_name.length() == 0) {
				System.out.println("WARNING: Found empty feature name in " + gVersion.versionName + ", " + gVersion.gServer.serverName);
				continue;
			}
			gVersion.addFeature(new GenericFeature(type_name, null, gVersion, null, type.getValue(), autoload));
		}
	}

	@Override
	public void discoverChromosomes(Object versionSourceObj) {
		// Discover chromosomes from DAS
		DasSource version = (DasSource) versionSourceObj;

		version.getGenome();
		version.getEntryPoints();
	}

	@Override
	public boolean hasFriendlyURL() {
		return false;
	}

	@Override
	public boolean canHandleFeature() {
		return true;
	}

	/**
	 * Discover species from DAS
	 * @param gServer
	 * @return false if there's an obvious problem
	 */
	@Override
	public boolean getSpeciesAndVersions(GenericServer gServer, GenericServer primaryServer, URL primaryURL, VersionDiscoverer versionDiscoverer) {
		DasServerInfo server = (DasServerInfo) gServer.serverObj;
		if (primaryURL == null) {
			try {
				primaryURL = new URL(gServer.URL);
				primaryServer = null;
			}
			catch (MalformedURLException x) {
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "cannot load URL " + gServer.URL + " for DAS server " + gServer.serverName, x);
			}
		}
		Map<String, DasSource> sources = server.getDataSources(primaryURL, primaryServer);
		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			System.out.println("WARNING: Couldn't find species for server: " + gServer);
			return false;
		}
		for (DasSource source : sources.values()) {
			String speciesName = SpeciesLookup.getSpeciesName(source.getID());
			String versionName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), source.getID());
			String versionID = source.getID();
			versionDiscoverer.discoverVersion(versionID, versionName, gServer, source, speciesName);
		}
		return true;
	}
}
