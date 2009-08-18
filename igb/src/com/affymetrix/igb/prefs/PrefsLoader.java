package com.affymetrix.igb.prefs;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.parsers.XmlPrefsParser;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 *
 * @author jnicol1
 */
public abstract class PrefsLoader {
	static Map<String,Map> prefs_hash;

	private static final String user_dir = System.getProperty("user.dir");
  private static final String user_home = System.getProperty("user.home");
	 /**
   *  We no longer distribute a file called "igb_prefs.xml".
   *  Instead there is a default prefs file hidden inside the igb.jar file, and
   *  this is augmented by a web-based prefs file at {@link #WEB_PREFS_URL}.
   *  But, we still will load a file called "igb_prefs.xml" if it exists in
   *  the user's home directory, since they may have put some personal modifications
   *  there.
   */
  private static final String DEFAULT_PREFS_FILENAME = "igb_prefs.xml";
  static String default_user_prefs_files =
    (new File(user_home, DEFAULT_PREFS_FILENAME)).getAbsolutePath() +
    ";" +
    (new File(user_dir, DEFAULT_PREFS_FILENAME)).getAbsolutePath();

	/**
   *  Returns IGB prefs hash
   *  If prefs haven't been loaded yet, will force loading of prefs
   */
  public static Map<String,Map> getIGBPrefs(String [] main_args) {
      if (prefs_hash != null) {
          return prefs_hash;
      }

      prefs_hash = new HashMap<String,Map>();
			
			String def_prefs_url = get_default_prefs_url(main_args);
			String[] prefs_list = get_prefs_list(main_args);
      XmlPrefsParser prefs_parser = new XmlPrefsParser();

      LoadDefaultPrefsFromJar(prefs_parser);

			LoadDefaultAPIPrefsFromJar();

      LoadWebPrefs(def_prefs_url, prefs_parser);

      LoadFileOrURLPrefs(prefs_list, prefs_parser);

			ServerList.LoadServerPrefs();

      return prefs_hash;
  }

 private static void LoadDefaultPrefsFromJar(XmlPrefsParser prefs_parser) {
		/**  first load default prefs from jar (with XmlPrefsParser)*/
		InputStream default_prefs_stream = null;
		try {
			default_prefs_stream = IGB.class.getResourceAsStream(IGBConstants.default_prefs_resource);
			System.out.println("loading default prefs from: " + IGBConstants.default_prefs_resource);
			prefs_parser.parse(default_prefs_stream, "", prefs_hash);
		} catch (Exception ex) {
			System.out.println("Problem parsing prefs from: " + IGBConstants.default_prefs_resource);
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(default_prefs_stream);
		}
	}


	private static void LoadDefaultAPIPrefsFromJar() {
		// Return if there are not already Preferences defined.  (Since we define keystroke shortcuts, this is a reasonable test.)
		try {
			if ((UnibrowPrefsUtil.getTopNode()).nodeExists("keystrokes")) {
				return;
			}
		} catch (BackingStoreException ex) {
		}

		InputStream default_prefs_stream = null;
		/**  load default prefs from jar (with Preferences API).  This will be the standard method soon.*/
		try {
			default_prefs_stream = IGB.class.getResourceAsStream(IGBConstants.DEFAULT_PREFS_API_RESOURCE);
			System.out.println("Default User preferences were not found.  loading default User preferencess from: " + IGBConstants.DEFAULT_PREFS_API_RESOURCE);
			Preferences.importPreferences(default_prefs_stream);
			//prefs_parser.parse(default_prefs_stream, "", prefs_hash);
		} catch (Exception ex) {
			System.out.println("Problem parsing prefs from: " + IGBConstants.DEFAULT_PREFS_API_RESOURCE);
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(default_prefs_stream);
		}
	}


	private static void LoadWebPrefs(String def_prefs_url, XmlPrefsParser prefs_parser) {
		// If a particular web prefs file was specified, then load it.
		// Otherwise try to load the web-based-default prefs file. (But
		// only load it if it is cached, then later update the cache on
		// a background thread.)
		if (def_prefs_url != null) {
		//	loadDefaultWebBasedPrefs(prefs_parser, prefs_hash);
		//} else {
			LoadPreferencesFromURL(def_prefs_url, prefs_parser);
		}
	}

	private static void LoadPreferencesFromURL(String prefs_url, XmlPrefsParser prefs_parser) {
		InputStream prefs_url_stream = null;
		try {
			prefs_url_stream = LocalUrlCacher.getInputStream(prefs_url);
			System.out.println("loading prefs from url: " + prefs_url);
			prefs_parser.parse(prefs_url_stream, prefs_url, prefs_hash);
		} catch (IOException ex) {
			System.out.println("Problem parsing prefs from url: " + prefs_url);
			System.out.println("Caused by: " + ex.toString());
		} finally {
			GeneralUtils.safeClose(prefs_url_stream);
		}
	}

	private static void LoadFileOrURLPrefs(String[] prefs_list, XmlPrefsParser prefs_parser) {
		if (prefs_list == null || prefs_list.length == 0) {
			return;
		}

		prefs_parser = new XmlPrefsParser();
		for (String fileOrURL : prefs_list) {
			InputStream strm = null;
			try {
				System.out.flush();

				File fil = new File(fileOrURL);
				if (fil.exists()) {
					System.out.println("loading user prefs from: " + fileOrURL);
					strm = new FileInputStream(fil);
					prefs_parser.parse(strm, fil.getCanonicalPath(), prefs_hash);
				} else {
					// May be a URL
					if (fileOrURL.startsWith("http")) {
						System.out.println("loading user prefs from: " + fileOrURL);
						LoadPreferencesFromURL(fileOrURL, prefs_parser);
					}
				}
			} catch (Exception ex) {
				System.out.flush();
				System.out.println("Problem parsing prefs from: " + fileOrURL);
				System.out.println(ex.toString());
			} finally {
				GeneralUtils.safeClose(strm);
			}
		}
	}


	 /**
   * Parse the command line arguments.  Find out what prefs file to use.
   * Return the name of the file as a String, or null if not invoked with
   * -prefs option.
   */
  private static String[] get_prefs_list(String[] args) {
    String files = IGB.get_arg("-prefs", args);
    if (files==null) {files = default_user_prefs_files;}
    StringTokenizer st = new StringTokenizer(files, ";");
    Set<String> result = new HashSet<String>();
    result.add(st.nextToken());
    while (st.hasMoreTokens()) {
      result.add(st.nextToken());
    }
    return result.toArray(new String[result.size()]);
  }

  private static String get_default_prefs_url(String[] args) {
    String def_prefs_url = IGB.get_arg("-default_prefs_url", args);
    return def_prefs_url;
  }


}
