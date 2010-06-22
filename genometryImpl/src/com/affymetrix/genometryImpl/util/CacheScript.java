package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.das.DasServerInfo;
import com.affymetrix.genometryImpl.das.DasSource;
import com.affymetrix.genometryImpl.das2.Das2ServerInfo;
import com.affymetrix.genometryImpl.das2.Das2Source;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.quickload.QuickLoadServerModel;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A class to get meta data from all servers.
 * @author hiralv
 */
public class CacheScript {

	private static final String dsn = "dsn.xml";
	
	/** Local path where data is cached. **/
	private static final String path = ".";

	/** For files too be looked up on server. **/
	private static final Set<String> quickloadFiles = new HashSet<String>();
	private static final Set<String> das2Files = new HashSet<String>();

	/** Add filed to be looked up. **/
	static{
		quickloadFiles.add(Constants.annotsTxt);
		quickloadFiles.add(Constants.annotsXml);
		quickloadFiles.add(Constants.modChromInfoTxt);
		quickloadFiles.add(Constants.liftAllLft);
	}

	static{
		das2Files.add(Das2VersionedSource.TYPES_CAP_QUERY);
		das2Files.add(Das2VersionedSource.SEGMENTS_CAP_QUERY);
		das2Files.add(Das2VersionedSource.FEATURES_CAP_QUERY);
	}

	/** Default server list. **/
	private static final String defaultList =	" <servers> " +

//												" <server type='das2' name='NetAffx Das2' url='http://netaffxdas.affymetrix.com/das2/genome' />" +
//												" <server type='quickload' name='NetAffx Quickload' url='http://netaffxdas.affymetrix.com/quickload_data' />" +

												" <server type='das2' name='Bioviz Das2' url='http://bioviz.org/das2/genome' />" +
												" <server type='quickload' name='Bioviz Quickload' url='http://bioviz.org/quickload/' />" +

//												" <server type='das' name='UCSC Das' url='http://genome.cse.ucsc.edu/cgi-bin/das/dsn' />" +

//												" <server type='quickload' name='HughesLab' url='http://hugheslab.ccbr.utoronto.ca/igb/' />" +


//												" <server type='das' name='Ensembl' url='http://www.ensembl.org/das/dsn' enabled='false' />" +
//												" <server type='das2' name='UofUtahBioinfoCore' url='http://bioserver.hci.utah.edu:8080/DAS2DB/genome' enabled='false' />" +

												" </servers> ";
	
	/**
	 * Creates directory of server name.
	 * Determines the server type and process it accordingly.
	 * @param gServer	GenericServer to be processed.
	 */
	private static void processServer(GenericServer gServer){
		String serverCachePath = path+gServer.serverName;
		makeDir(serverCachePath);

		switch(gServer.serverType){
			case QuickLoad:
				processQuickLoad(gServer, serverCachePath);
				break;

			case DAS2:
				processDas2Server(gServer, serverCachePath);
				break;

			case DAS:
				processDasServer(gServer, serverCachePath);
				break;
		}
	}

	/**
	 * Gets files for all genomes from Quickload server and copies it to appropriate directory.
	 * @param gServer	GenericServer from where mapping are fetched.
	 * @param serverCachePath	Local path where fetched files are stored.
	 * @return
	 */
	private static boolean processQuickLoad(GenericServer gServer, String serverCachePath){
		File file = getFile(gServer.URL+Constants.contentsTxt);

		String quickloadStr = null;
		quickloadStr = (String) gServer.serverObj;
		
		QuickLoadServerModel quickloadServer = new QuickLoadServerModel(quickloadStr);

		List<String> genome_names = quickloadServer.getGenomeNames();
		if(!moveFileTo(file,Constants.contentsTxt,serverCachePath))
			return false;

		for(String genome_name : genome_names){
			if(!getAllFiles(gServer,genome_name,serverCachePath))
				return false;
		}

		return true;
	}

	/**
	 * Gets files for all genomes from Das2 server and copies it to appropriate directory.
	 * @param gServer	GenericServer from where mapping are fetched.
	 * @param serverCachePath	Local path where fetched files are stored.
	 * @return
	 */
	private static boolean processDas2Server(GenericServer gServer, String serverCachePath){
		File file = getFile(gServer.URL);
		moveFileTo(file, Constants.GENOME_SEQ_ID+ Constants.xml_ext, serverCachePath);

		Das2ServerInfo serverInfo = (Das2ServerInfo) gServer.serverObj;
		Map<String,Das2Source> sources = serverInfo.getSources();
		
		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.WARNING,"Couldn't find species for server: ",gServer);
			return false;
		}

		for (Das2Source source : sources.values()) {
			// Das/2 has versioned sources.  Get each version.
			for (Das2VersionedSource versionSource : source.getVersions().values()) {
				if(!getAllFiles(gServer,versionSource.getName(),serverCachePath))
					return false;
			}
		}

		return true;
	}

	/**
	 * Gets files for a genome and copies it to it's directory.
	 * @param servertype	Server type to determine which set of files to be used.
	 * @param server_path	Server path from where mapping is to be copied.
	 * @param local_path	Local path from where mapping is to saved.
	 */
	private static boolean getAllFiles(GenericServer gServer, String genome_name, String local_path){
		File file;
		Set<String> files = null;

		switch(gServer.serverType){
			case QuickLoad:
				files = quickloadFiles;
				break;

			case DAS2:
				files = das2Files;
				break;

			default:
				return false;
		}

		String server_path = gServer.URL + "/" + genome_name;
		local_path += "/" + genome_name;
		makeDir(local_path);

		for(String fileName : files){
			file = getFile(server_path+"/"+fileName);

			if(file == null)
				continue;
			
			if(gServer.serverType.equals(ServerType.DAS2))
				fileName += Constants.xml_ext;
			
			if(!moveFileTo(file,fileName,local_path))
				return false;
		}

		return true;
	}
	
	/**
	 * Gets files for all genomes from Das server and copies it to appropriate directory.
	 * @param gServer	GenericServer from where mapping are fetched.
	 * @param serverCachePath	Local path where fetched files are stored.
	 * @return
	 */
	private static boolean processDasServer(GenericServer gServer, String serverCachePath){
		File file = getFile(gServer.URL);
		moveFileTo(file,dsn,serverCachePath);
		
		DasServerInfo server = (DasServerInfo) gServer.serverObj;
		Map<String, DasSource> sources = server.getDataSources();

		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.WARNING,"Couldn't find species for server: ",gServer);
			return false;
		}

		for (DasSource source : sources.values()) {
			
			String localPath = serverCachePath + "/" + source.getID();
			makeDir(localPath);
			getAllDasFiles(source.getID(),source.getServerURL(), source.getMasterURL(), localPath);
		}

		return true;
	}

	/**
	 * Gets files for a genome and copies it to it's directory.
	 * @param server_path	Server path from where mapping is to be copied.
	 * @param local_path	Local path from where mapping is to saved.
	 */
	private static void getAllDasFiles(String id, URL server, URL master, String local_path){
		File file;
		final Map<String, String> DasFilePath = new HashMap<String, String>();

		String entry_point = getPath(master.getPath(),master, DasSource.ENTRY_POINTS);
		String types = getPath(id,server,DasSource.TYPES);
	
		DasFilePath.put(entry_point, DasSource.ENTRY_POINTS + Constants.xml_ext);
		DasFilePath.put(types, DasSource.TYPES + Constants.xml_ext);

		for(Entry<String, String> fileDet : DasFilePath.entrySet()){
			file = getFile(fileDet.getKey());
			moveFileTo(file,fileDet.getValue(),local_path);
		}
		
	}

	/**
	 * Returns server path for a mapping on Das server.
	 * @param id	Genome id
	 * @param server	Server url.
	 * @param mapping	File name.
	 * @return
	 */
	private static String getPath(String id, URL server, String file){
		try {
			URL server_path = new URL(server, id + "/" + file);
			return server_path.toExternalForm();
		} catch (MalformedURLException ex) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	/**
	 * Moves mapping to the given path and renames it to filename.
	 * @param mapping	File to be moved.
	 * @param fileName	File name to be given to moved mapping.
	 * @param path	Path to where mapping is moved.
	 * @return
	 */
	private static boolean moveFileTo(File file, String fileName, String path){
		File newLocation = new File(path+ "/" +fileName);
		return file.renameTo(newLocation);
	}

	/**
	 * Creates directory for the given path.
	 * @param path	Path where directory is to be created.
	 * @return
	 */
	private static boolean makeDir(String path){
		File dir = new File(path);
		if(!dir.exists()){
			dir.mkdir();
		}
		return true;
	}

	/**
	 * Returns mapping for give path.
	 * @param path	File path.
	 * @return
	 */
	private static File getFile(String path){
		File file = null;
		try{
			file = LocalUrlCacher.convertURIToFile(URI.create(path));
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return file;
	}

	/**
	 * Parses xml mapping of server list.
	 * @param istr
	 * @return	Returns a list of generic server.
	 */
	private static Set<GenericServer> parseServerList(InputStream istr) throws Exception {
		Set<GenericServer> serverList = new HashSet<GenericServer>();

		Document list = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(istr);
		Element top_element = list.getDocumentElement();
		String topname = top_element.getTagName();
		if (!(topname.equalsIgnoreCase("servers"))) {
			System.err.println("not a server list file -- can't parse!");
		}
		NodeList children = top_element.getChildNodes();
		Node child;
		String name;
		Element el = null;

		for (int i = 0; i < children.getLength(); i++) {
			child = children.item(i);
			name = child.getNodeName();
			if (child instanceof Element) {
				el = (Element) child;
				if (name.equalsIgnoreCase("server")) {
					ServerType server_type = getServerType(el.getAttribute("type"));
					String server_name = el.getAttribute("name");
					String server_url = el.getAttribute("url");
					String en = el.getAttribute("enabled");
					Boolean enabled = en == null || en.isEmpty() ? true : Boolean.valueOf(en);

					String serverURL = ServerUtils.formatURL(server_url, server_type);
					Object serverInfo = ServerUtils.getServerInfo(server_type, serverURL, server_name);
					GenericServer server = new GenericServer(server_name, serverURL, server_type, enabled, serverInfo);
					serverList.add(server);
				}
			}
		}

		return serverList;
	}

	/**
	 * Returns server type.
	 * @param type	Type name.
	 * @return
	 */
	private static ServerType getServerType(String type) {
		for (ServerType t : ServerType.values()) {
			if (type.equalsIgnoreCase(t.toString())) {
				return t;
			}
		}
		return null;
	}

	/**
	 * Runs caching script for given set of server list.
	 * @param server_list	List of server.
	 */
	static public void runScript(Set<GenericServer> server_list){

		FileOutputStream fos = null;
		File mapping = new File(path + "/" + Constants.serverMapping);
		try {
			mapping.createNewFile();
			fos = new FileOutputStream(mapping);
			final PrintStream out = new PrintStream(fos);

			for (final GenericServer gServer : server_list) {

				final Timer ser_tim = new Timer();
				ExecutorService vexec = Executors.newSingleThreadExecutor();
				SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

					protected Void doInBackground(){
						ser_tim.start();
						processServer(gServer);
						return null;
					}

					@Override
					public void done() {
						out.println(gServer.friendlyURL.toExternalForm() + "\t" + gServer.serverName);
						Logger.getLogger(CacheScript.class.getName()).log(Level.INFO, "Time required to cache " + gServer.serverName + " :" + (ser_tim.read()/ 1000f), ser_tim);
					}

				};
				vexec.execute(worker);
				vexec.shutdown();
			}
		} catch (Exception ex) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(fos);
		}
	}

	static public void main(String[] args){
		InputStream istr = null;
		try {
			istr = new ByteArrayInputStream(defaultList.getBytes());
			Set<GenericServer> server_list = parseServerList(istr);
			runScript(server_list);
		} catch (Exception ex) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(istr);
		}
				
	}

}
