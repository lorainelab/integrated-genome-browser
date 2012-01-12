package com.affymetrix.genometryImpl.quickload;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.SpeciesLookup;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.util.VersionDiscoverer;

public class QuickloadServerType implements ServerTypeI {
	private static final String name = "Quickload";
	public static final int ordinal = 30;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	/**
	 * Private copy of the default Synonym lookup
	 * @see SynonymLookup#getDefaultLookup()
	 */
	private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();
	/** For files too be looked up on server. **/
	private static final Set<String> quickloadFiles = new HashSet<String>();

	/** Add files to be looked up. **/
	static{
		quickloadFiles.add(Constants.annotsTxt);
		quickloadFiles.add(Constants.annotsXml);
		quickloadFiles.add(Constants.modChromInfoTxt);
		quickloadFiles.add(Constants.liftAllLft);
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
	 * Returns true if file may not exist else false.
	 * @param fileName
	 * @return
	 */
	private boolean getFileAvailability(String fileName){
		if(fileName.equals(Constants.annotsTxt) || fileName.equals(Constants.annotsXml) || fileName.equals(Constants.liftAllLft))
			return true;

		return false;
	}

	/**
	 * Gets files for a genome and copies it to it's directory.
	 * @param servertype	Server type to determine which set of files to be used.
	 * @param server_path	Server path from where mapping is to be copied.
	 * @param local_path	Local path from where mapping is to saved.
	 */
	private boolean getAllFiles(GenericServer gServer, String genome_name, String local_path){
		File file;
		Set<String> files = quickloadFiles;

		String server_path = gServer.URL + "/" + genome_name;
		local_path += "/" + genome_name;
		GeneralUtils.makeDir(local_path);
		boolean fileMayNotExist;
		for(String fileName : files){
			fileMayNotExist = getFileAvailability(fileName);

			file = GeneralUtils.getFile(server_path+"/"+fileName, fileMayNotExist);

			if((file == null && !fileMayNotExist))
				return false;

			if(!GeneralUtils.moveFileTo(file,fileName,local_path))
				return false;
		}

		return true;
	}
	
	@Override
	public boolean processServer(GenericServer gServer, String path) {
		File file = GeneralUtils.getFile(gServer.URL+Constants.contentsTxt, false);

		String quickloadStr = null;
		quickloadStr = (String) gServer.serverObj;
		
		QuickLoadServerModel quickloadServer = new QuickLoadServerModel(quickloadStr);

		List<String> genome_names = quickloadServer.getGenomeNames();
		if(!GeneralUtils.moveFileTo(file,Constants.contentsTxt,path))
			return false;
		
		for(String genome_name : genome_names){
			if(!getAllFiles(gServer,genome_name,path)){
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

	@Override
	public void discoverFeatures(GenericVersion gVersion, boolean autoload) {
		// not implemented here
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
	 * @param gServer
	 * @param loadGenome boolean to check load genomes from server.
	 * @return false if there's an obvious failure.
	 */
	@Override
	public boolean getSpeciesAndVersions(GenericServer gServer, GenericServer primaryServer, URL primaryURL, VersionDiscoverer versionDiscoverer) {
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
		List<String> genomeList = quickloadServer.getGenomeNames();
		if (genomeList == null || genomeList.isEmpty()) {
			System.out.println("WARNING: No species found in server: " + gServer);
			return false;
		}

		//update species.txt with information from the server.
		if( quickloadServer.hasSpeciesTxt()){
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
}
