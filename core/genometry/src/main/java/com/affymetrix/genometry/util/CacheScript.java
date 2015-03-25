package com.affymetrix.genometry.util;

import com.affymetrix.genometry.general.GenericServer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashSet;
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
 *
 * @author hiralv
 */
public class CacheScript extends Thread {

	private static final String temp = "temp";
	/**
	 * boolean to indicate should script continue to run if error occurs *
	 */
	/**
	 * Local path where data is cached. *
	 */
	private final String path;
	/**
	 * List of server to be cached. *
	 */
	private final Set<GenericServer> server_list;
	/**
	 * Default server list. *
	 */
	private static final String defaultList = " <servers> "
			+ //												" <server type='das2' name='NetAffx Das2' url='http://netaffxdas.affymetrix.com/das2/genome' />" +
			//												" <server type='quickload' name='NetAffx Quickload' url='http://netaffxdas.affymetrix.com/quickload_data' />" +
			//												" <server type='das2' name='Bioviz Das2' url='http://bioviz.org/das2/genome' />" +
			//												" <server type='quickload' name='Bioviz Quickload' url='http://bioviz.org/quickload/' />" +
			" <server type='das' name='UCSC Das' url='http://genome.cse.ucsc.edu/cgi-bin/das/dsn' />"
			+ //												" <server type='quickload' name='HughesLab' url='http://hugheslab.ccbr.utoronto.ca/igb/' />" +
			" <server type='das' name='Ensembl' url='http://www.ensembl.org/das/dsn' enabled='false' />"
			+ //												" <server type='das2' name='UofUtahBioinfoCore' url='http://bioserver.hci.utah.edu:8080/DAS2DB/genome' enabled='false' />" +
			" </servers> ";

	public CacheScript(String path, Set<GenericServer> server_list) {
		this.path = path;
		this.server_list = server_list;
	}

	/**
	 * Runs caching script for given set of server list.
	 */
	@Override
	public void run() {
		for (final GenericServer gServer : server_list) {

			final Timer ser_tim = new Timer();
			ExecutorService vexec = Executors.newSingleThreadExecutor();
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

				protected Void doInBackground() {
					ser_tim.start();
					if (processServer(gServer, path)) {
						copyDirectoryFor(path, gServer.serverName);
					}
					return null;
				}

				@Override
				public void done() {
					Logger.getLogger(CacheScript.class.getName()).log(Level.INFO, "Time required to cache " + gServer.serverName + " :" + (ser_tim.read() / 1000f), ser_tim);
				}
			};
			vexec.execute(worker);
			vexec.shutdown();
		}
	}

	/**
	 * Create serverMapping.txt and add server name and corresponding directory
	 * to it.
	 */
	public void writeServerMapping() {
		FileOutputStream fos = null;
		PrintStream out = null;
		try {
			File mapping = new File(path + "/" + Constants.SERVER_MAPPING);
			mapping.createNewFile();
			fos = new FileOutputStream(mapping);
			out = new PrintStream(fos);
			for (final GenericServer gServer : server_list) {
				out.println(gServer.URL + "\t" + gServer.serverName);
			}
		} catch (IOException ex) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(fos);
			GeneralUtils.safeClose(out);
		}
	}

	/**
	 * Creates directory of server name. Determines the server type and process
	 * it accordingly.
	 *
	 * @param gServer	GenericServer to be processed.
	 */
	private static boolean processServer(GenericServer gServer, String path) {
		Logger.getLogger(CacheScript.class.getName()).log(Level.FINE, "Caching {0} at path {1}", new Object[]{gServer.serverName, path});

		String serverCachePath = path + gServer.serverName + temp;
		GeneralUtils.makeDir(serverCachePath);

		return gServer.serverType.processServer(gServer, serverCachePath);
	}

	/**
	 * @return true if file may not exist else false.
	 */
	public static boolean getFileAvailability(String fileName) {
		if (fileName.equals(Constants.ANNOTS_TXT) || fileName.equals(Constants.ANNOTS_XML) || fileName.equals(Constants.LIFT_ALL_LFT)) {
			return true;
		}

		return false;
	}

	/**
	 * Parses xml mapping of server list.
	 *
	 * @param istr
	 * @return	Returns a list of generic server.
	 */
	private static Set<GenericServer> parseServerList(InputStream istr) throws Exception {
		Set<GenericServer> serverList = new HashSet<>();

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
					ServerTypeI server_type = getServerType(el.getAttribute("type"));
					String server_name = el.getAttribute("name").replaceAll("\\W", "");
					String server_url = el.getAttribute("url");

					// qlmirror - Quickload Mirror Server
					String mirror_url = el.getAttribute("mirror");
					
					String en = el.getAttribute("enabled");
					Boolean enabled = en == null || en.isEmpty() ? true : Boolean.valueOf(en);
					String d = el.getAttribute("default");
					Boolean isDefault = d == null || d.isEmpty() ? true : Boolean.valueOf(d);
					String serverURL = ServerUtils.formatURL(server_url, server_type);
					Object serverInfo = server_type.getServerInfo(serverURL, server_name);
					GenericServer server = new GenericServer(server_name, serverURL,
							server_type, enabled, serverInfo, isDefault, mirror_url);
					serverList.add(server);
				}
			}
		}

		return serverList;
	}

	/**
	 * Get a named server type.
	 * @param type	name.
	 * @return server type.
	 */
	private static ServerTypeI getServerType(String type) {
		for (ServerTypeI t : ServerUtils.getServerTypes()) {
			if (type.equalsIgnoreCase(t.toString())) {
				return t;
			}
		}
		return null;
	}

	/**
	 * Recursively copies data from source to destination.
	 *
	 * @param source	Source directory.
	 * @param dest	Destination directory.
	 */
	private static void copyRecursively(File source, File dest) {
		for (File file : source.listFiles()) {
			if (file.isDirectory()) {
				copyRecursively(file, GeneralUtils.makeDir(dest.getPath() + "/" + file.getName()));
			} else {
				GeneralUtils.moveFileTo(file, file.getName(), dest.getPath());
			}
		}
	}

	/**
	 * Recursively copies directory data for given server name.
	 *
	 * @param servername	Name of the server.
	 */
	private static void copyDirectoryFor(String path, String servername) {
		File temp_dir = new File(path + servername + temp);

		String perm_path = path + servername;
		GeneralUtils.makeDir(perm_path);

		File perm_dir = new File(perm_path);

		copyRecursively(temp_dir, perm_dir);
	}

	static public void main(String[] args) {
		InputStream istr = null;
		try {
			istr = new ByteArrayInputStream(defaultList.getBytes());
			String path = "/";

			if (args.length >= 1) {
				path = args[0];
			}

			Set<GenericServer> server_list = parseServerList(istr);
			CacheScript script = new CacheScript(path, server_list);
			script.start();
			script.writeServerMapping();
		} catch (Exception ex) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(istr);
		}

	}
}
