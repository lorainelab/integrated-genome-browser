package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A class to get meta data from all servers.
 * @author hiralv
 */
public class CacheScript {

	private static final Pattern tab_regex = Pattern.compile("\t");
	private static final String path = "";
	
	
	private static boolean processQuickLoad(GenericServer gServer){

		//Get file file and move it to preferred location.
		File file = getFile(gServer.URL+Constants.contentsTxt);
		Set<String> genome_names = processContentTxt(file);
		if(!MoveFileTo(file,Constants.contentsTxt,path))
			return false;

		for(String genome_name : genome_names){
			String server_path = gServer.URL + "/" + genome_name;
			String local_path = path+genome_name;
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
						System.out.println("Found blank QuickLoad genome -- skipping");
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
			
		}
		return file;
	}

	private static boolean processDas2Server(GenericServer gServer){
		return false;
	}

	private static boolean processDasServer(GenericServer gServer){
		return false;
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
	}

}
