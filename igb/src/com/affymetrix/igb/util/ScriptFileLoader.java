package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.action.RefreshDataAction;
import com.affymetrix.igb.bookmarks.UnibrowControlServlet;
import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.igb.view.MapRangeBox;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jnicol
 * Parse actions from IGB response file.
 */
public class ScriptFileLoader {

	public static String getScriptFileStr(String[] args) {
		for (int i=0;i<args.length;i++) {
			if (args[i].equalsIgnoreCase("-" + IGBConstants.SCRIPTFILETAG)) {
				if (i+1 < args.length) {
					return args[i+1];
				} else {
					Logger.getLogger(ScriptFileLoader.class.getName()).severe("File was not specified.");
					return null;
				}
			}
		}
		return null;
	}

	public static void doActions(String batchFileStr) {
		// A response file was requested.  Run response file parser, and ignore any other parameters.
			File f = new File(batchFileStr);
			if (!f.exists()) {
				f = LocalUrlCacher.convertURIToFile(URI.create(batchFileStr));
			}
			if (f == null || !f.exists()) {
				Logger.getLogger(ScriptFileLoader.class.getName()).log(
						Level.SEVERE, "Couldn't find response file: " + batchFileStr);
				return;
			}

			ScriptFileLoader.doActions(f);
	}

	/**
	 * Read and execute the actions from a file.
	 * @param bis
	 */
	private static void doActions(File f) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(f));
			ScriptFileLoader.doActions(br);
		} catch (FileNotFoundException ex) {
			Logger.getLogger(ScriptFileLoader.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(br);
		}
	}


	/**
	 * Read and execute the actions from the stream.
	 * @param bis
	 */
	private static void doActions(BufferedReader br) {
		try {
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] fields = line.split("\\t");
				Logger.getLogger(ScriptFileLoader.class.getName()).info("line " + line);
				//System.out.println("parsing line " + line + " with field len" + fields.length);
				doSingleAction(fields);
				Thread.sleep(500);	// user actions don't happen instantaneously, so give a short sleep time between batch actions.
			}
		} catch (Exception ex) {
			Logger.getLogger(ScriptFileLoader.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private static void doSingleAction(String[] fields) {
		String action = fields[0].toLowerCase();
		if (action.equals("genome") && fields.length == 2) {
			// go to genome
			goToGenome(fields[1]);
			return;
		}
		if (action.equals("goto") && fields.length == 2) {
			// go to region
			goToRegion(fields[1]);
			return;
		}
		if (action.equals("load")) {
			if (fields.length == 2) {
				loadFile(fields[1]);
				return;
			}
			if (fields.length == 3) {
				loadData(fields[1], fields[2]);
				return;
			}
			if (fields.length == 4) {
				loadData(fields[1], fields[2], fields[3]);
				return;
			}
		}
		if (action.equals("print")) {
			if (fields.length == 1) {
				try {
					Application.getSingleton().getMapView().getSeqMap().print(0, true);
				} catch (Exception ex) {
					ErrorHandler.errorPanel("Problem trying to print.", ex);
				}
				return;
			}
		}
		if (action.equals("refresh")) {
			RefreshDataAction.getAction().actionPerformed(null);
		}
		if (action.equals("select") && fields.length==2) {
			UnibrowControlServlet.performSelection(fields[1]);
		}
	}

	private static void goToGenome(String genomeVersion) {
		UnibrowControlServlet.determineAndSetGroup(genomeVersion);
	}

	private static void goToRegion(String region) {
		MapRangeBox.setRange(Application.getSingleton().getMapView(), region);
	}

	private static void loadData(String serverType, String URIorFeature) {
		if (serverType.equalsIgnoreCase("file")) {
			loadFile(URIorFeature);
		}
	}

	private static void loadData(String serverType, String serverURIorName, String feature) {
		if (serverType.equalsIgnoreCase("quickload")) {
			//loadQuickLoad(URIorFeature);
		}
		if (serverType.equalsIgnoreCase("das")) {
			//loadDAS(URIorFeature):
		}
		if (serverType.equalsIgnoreCase("das2")) {
			//loadDAS(URIorFeature):
		}
	}

	private static void loadFile(String fileName) {
		File f = new File(fileName);
		LoadFileAction.openURI(f.toURI(), f.getName(), true, GenometryModel.getGenometryModel().getSelectedSeqGroup());
	}

}
