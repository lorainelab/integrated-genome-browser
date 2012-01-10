package com.affymetrix.genometryImpl.quickload;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;

public class QuickloadServerType implements ServerTypeI {
	private static final String name = "Quickload";
	public static final int ordinal = 30;
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
}
