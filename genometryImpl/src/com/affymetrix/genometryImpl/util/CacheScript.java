package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.das.DasServerInfo;
import com.affymetrix.genometryImpl.das.DasSource;
import com.affymetrix.genometryImpl.das2.Das2ServerInfo;
import com.affymetrix.genometryImpl.das2.Das2Source;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A class to get meta data from all servers.
 * @author hiralv
 */
public class CacheScript {

	private static final String XML = ".xml";
	private static final Pattern tab_regex = Pattern.compile("\t");
	private static final String path = "/Users/aloraine/Desktop/Caching/";
	
	private static boolean processQuickLoad(GenericServer gServer){
		String serverCachePath = path+gServer.serverName;
		MakeDir(serverCachePath);

		//Get file file and move it to preferred location.
		File file = getFile(gServer.URL+Constants.contentsTxt);
		Set<String> genome_names = processContentTxt(file);
		if(!MoveFileTo(file,Constants.contentsTxt,serverCachePath))
			return false;

		for(String genome_name : genome_names){
			String server_path = gServer.URL + "/" + genome_name;
			String local_path = serverCachePath+ "/" + genome_name;
			MakeDir(local_path);

			getAllQuickLoadFiles(server_path,local_path);

		}
		
		return true;
	}

	private static Set<String> processContentTxt(File file){
		Set<String> genome_names = new HashSet<String>();
		InputStream istr = null;
		InputStreamReader ireader = null;
		BufferedReader br = null;
		try {
			istr = new FileInputStream(file);
			ireader = new InputStreamReader(istr);
			br = new BufferedReader(ireader);
			String line;
			while ((line = br.readLine()) != null) {
				if ((line.length() == 0) || line.startsWith("#")) {
					continue;
				}
				String[] fields = tab_regex.split(line);
				if (fields.length >= 1) {
					String genome_name = fields[0];
					genome_name = genome_name.trim();
					if (genome_name.length() == 0) {
						Logger.getLogger(CacheScript.class.getName()).log(Level.WARNING,"Found blank QuickLoad genome -- skipping",file);
						continue;
					}
					genome_names.add(genome_name);
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(istr);
			GeneralUtils.safeClose(ireader);
			GeneralUtils.safeClose(br);
		}
		return genome_names;
	}

	private static void getAllQuickLoadFiles(String server_path,String local_path){
		File file;
		final Set<String> quickloadFile = new HashSet<String>();

		quickloadFile.add(Constants.annotsTxt);
		quickloadFile.add(Constants.annotsXml);
		quickloadFile.add(Constants.modChromInfoTxt);
		quickloadFile.add(Constants.liftAllLft);

		for(String fileName : quickloadFile){
			file = getFile(server_path+"/"+fileName);
			MoveFileTo(file,fileName,local_path);
		}
	}

	private static boolean MoveFileTo(File file, String fileName, String path){
		if(file == null)
			return false;
		
		File newLocation = new File(path+ "/" +fileName);
		file.renameTo(newLocation);
		return true;
	}

	private static boolean MakeDir(String path){
		File dir = new File(path);
		if(!dir.exists()){
			dir.mkdir();
		}
		return true;
	}

	private static File getFile(String path){
		File file = null;
		try{
			file = LocalUrlCacher.convertURIToFile(URI.create(path));
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return file;
	}

	private static boolean processDas2Server(GenericServer gServer){
		String serverCachePath = path+gServer.serverName;
		MakeDir(serverCachePath);

		File file = getFile(gServer.URL);
		MoveFileTo(file, Constants.GENOME_SEQ_ID+XML, serverCachePath);
		
		Das2ServerInfo serverInfo = (Das2ServerInfo) gServer.serverObj;
		Map<String,Das2Source> sources = serverInfo.getSources();
		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.WARNING,"Couldn't find species for server: ",gServer);
			return false;
		}
		for (Das2Source source : sources.values()) {
			// Das/2 has versioned sources.  Get each version.
			for (Das2VersionedSource versionSource : source.getVersions().values()) {
				String serverPath = gServer.URL + "/" + versionSource.getName();
				String localPath = serverCachePath + "/" + versionSource.getName();
				MakeDir(localPath);

				getAllDas2Files(serverPath,localPath);

			}
		}

		return true;
	}

	private static void getAllDas2Files(String server_path,String local_path){
		File file;
		final Set<String> Das2Files = new HashSet<String>();

		Das2Files.add(Das2VersionedSource.TYPES_CAP_QUERY);
		Das2Files.add(Das2VersionedSource.SEGMENTS_CAP_QUERY);
		Das2Files.add(Das2VersionedSource.FEATURES_CAP_QUERY);
	
		for(String fileName : Das2Files){
			file = getFile(server_path+"/"+fileName);
			MoveFileTo(file,fileName+XML,local_path);
		}
	}

	private static boolean processDasServer(GenericServer gServer){
		String serverCachePath = path+gServer.serverName;
		MakeDir(serverCachePath);
		
		DasServerInfo server = (DasServerInfo) gServer.serverObj;
		Map<String, DasSource> sources = server.getDataSources();

		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.WARNING,"Couldn't find species for server: ",gServer);
			return false;
		}

		for (DasSource source : sources.values()) {
			
			String localPath = serverCachePath + "/" + source.getID();
			MakeDir(localPath);
			getAllDasFiles(source.getID(),source.getServerURL(), source.getMasterURL(), localPath);
		}

		return true;
	}

	private static void getAllDasFiles(String id, URL server, URL master, String local_path){
		File file;
		final Map<String, String> DasFilePath = new HashMap<String, String>();

		String entry_point = getPath(master.getPath(),master, DasSource.ENTRY_POINTS);
		String types = getPath(id,server,DasSource.TYPES);
	
		DasFilePath.put(entry_point, DasSource.ENTRY_POINTS + XML);
		DasFilePath.put(types, DasSource.TYPES + XML);

		for(Entry<String, String> fileDet : DasFilePath.entrySet()){
			file = getFile(fileDet.getKey());
			MoveFileTo(file,fileDet.getValue(),local_path);
		}
		
	}

	private static String getPath(String id, URL server, String file){
		try {
			URL server_path = new URL(server, id + "/" + file);
			return server_path.toExternalForm();
		} catch (MalformedURLException ex) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	private static String formatURL(String url, ServerType type) {
		try {
			/* remove .. and // from URL */
			url = new URI(url).normalize().toASCIIString();
		} catch (URISyntaxException ex) {
			String message = "Unable to parse URL: '" + url + "'";
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, message, ex);
			throw new IllegalArgumentException(message, ex);
		}
		switch (type) {
			case DAS:
			case DAS2:
				while (url.endsWith("/")) {
					url = url.substring(0, url.length()-1);
				}
				return url;
			case QuickLoad:
				return url.endsWith("/") ? url : url + "/";
			default:
				return url;
		}
	}

	static public void main(String[] args){
		String bioviz_quickload = formatURL("http://bioviz.org/quickload/",ServerType.QuickLoad);

		GenericServer gServer = new GenericServer("Bioviz QuickLoad",			 //Server Name
												 bioviz_quickload,				 //Server URL
												 ServerType.QuickLoad,			 //Server type
												 true,							 //Enable
												 null);				 //Server Object

		processQuickLoad(gServer);

		String bioviz_das2 = formatURL("http://bioviz.org/das2/genome", ServerType.DAS2);
		Das2ServerInfo serverInfo = null;
		try {
			serverInfo = new Das2ServerInfo(bioviz_das2, "Bioviz Das", true);
		} catch (URISyntaxException ex) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		gServer = new GenericServer("Bioviz Das",
									bioviz_das2,
									ServerType.DAS2,
									true,
									serverInfo);
		
		processDas2Server(gServer);

		String ucsc_das = formatURL("http://genome.cse.ucsc.edu/cgi-bin/das/dsn", ServerType.DAS);
		DasServerInfo serverInf = new DasServerInfo(ucsc_das);
	

		gServer = new GenericServer("UCSC",
									ucsc_das,
									ServerType.DAS,
									true,
									serverInf);

		processDasServer(gServer);

		
	}

}
