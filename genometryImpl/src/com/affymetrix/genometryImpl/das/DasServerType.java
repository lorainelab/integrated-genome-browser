package com.affymetrix.genometryImpl.das;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;

public class DasServerType implements ServerTypeI {
	/** boolean to indicate should script continue to run if error occurs **/
	private static final boolean exitOnError = false;
	private static final String dsn = "dsn.xml";
	private static final String name = "DAS";
	public static final int ordinal = 20;
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
}
