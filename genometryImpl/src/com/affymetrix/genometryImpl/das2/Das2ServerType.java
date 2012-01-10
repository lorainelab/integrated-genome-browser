package com.affymetrix.genometryImpl.das2;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.ServerUtils;

public class Das2ServerType implements ServerTypeI {
	private static final String name = "DAS2";
	public static final int ordinal = 10;
	/** For files too be looked up on server. **/
	private static final Set<String> das2Files = new HashSet<String>();

	/** Add files to be looked up. **/
	static{
		das2Files.add(Das2VersionedSource.TYPES_CAP_QUERY);
		das2Files.add(Das2VersionedSource.SEGMENTS_CAP_QUERY);
	}
	private static final Das2ServerType instance = new Das2ServerType();
	public static Das2ServerType getInstance() {
		return instance;
	}

	private Das2ServerType() {
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
		Set<String> files = das2Files;

		String server_path = gServer.URL + "/" + genome_name;
		local_path += "/" + genome_name;
		GeneralUtils.makeDir(local_path);
		boolean fileMayNotExist;
		for(String fileName : files){
			fileMayNotExist = getFileAvailability(fileName);

			file = GeneralUtils.getFile(server_path+"/"+fileName, fileMayNotExist);

			fileName += Constants.xml_ext;

			if((file == null && !fileMayNotExist))
				return false;

			if(!GeneralUtils.moveFileTo(file,fileName,local_path))
				return false;
		}

		return true;
	}

	@Override
	public boolean processServer(GenericServer gServer, String path) {
		File file = GeneralUtils.getFile(gServer.URL, false);
		if(!GeneralUtils.moveFileTo(file, Constants.GENOME_SEQ_ID+ Constants.xml_ext, path))
			return false;
		
		Das2ServerInfo serverInfo = (Das2ServerInfo) gServer.serverObj;
		Map<String,Das2Source> sources = serverInfo.getSources();
		
		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Couldn't find species for server: ",gServer);
			return false;
		}

		for (Das2Source source : sources.values()) {
			// Das/2 has versioned sources.  Get each version.
			for (Das2VersionedSource versionSource : source.getVersions().values()) {
				if(!getAllFiles(gServer,versionSource.getName(),path)){
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
		Object info = null;

		try {
			info = new Das2ServerInfo(url, name, false);
		} catch (URISyntaxException e) {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING,
					"Could not initialize {0} server with address: {1}", new Object[]{name, url});
			e.printStackTrace(System.out);
		}
		return info;
	}

	@Override
	public String adjustURL(String url) {
		String tempURL = url;
		if (tempURL.endsWith("/genome")) {
			tempURL = tempURL.substring(0, tempURL.length() - 7);
		} 
		return tempURL;
	}

	@Override
	public boolean loadStrategyVisibleOnly() {
		return true;
	}

	@Override
	public void discoverFeatures(GenericVersion gVersion, boolean autoload) {
		Das2VersionedSource version = (Das2VersionedSource) gVersion.versionSourceObj;
		for (Das2Type type : version.getTypes().values()) {
			String type_name = type.getName();
			if (type_name == null || type_name.length() == 0) {
				System.out.println("WARNING: Found empty feature name in " + gVersion.versionName + ", " + gVersion.gServer.serverName);
				continue;
			}
			Map<String, String> type_props = type.getProps();
			gVersion.addFeature(new GenericFeature(type_name, type_props, gVersion, null, type, autoload));
		}
	}

	@Override
	public void discoverChromosomes(Object versionSourceObj) {
		Das2VersionedSource version = (Das2VersionedSource) versionSourceObj;
		
		version.getGenome();  // adds genome to singleton genometry model if not already present
		// Calling version.getSegments() to ensure that Das2VersionedSource is populated with Das2Region segments,
		//    which in turn ensures that AnnotatedSeqGroup is populated with SmartAnnotBioSeqs
		version.getSegments();
	}

	@Override
	public boolean hasFriendlyURL() {
		return true;
	}

	@Override
	public boolean canHandleFeature() {
		return true;
	}
}
