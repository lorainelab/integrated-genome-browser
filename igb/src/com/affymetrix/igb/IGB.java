/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/
package com.affymetrix.igb;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.*;
import java.util.logging.Logger;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

import com.affymetrix.genoviz.util.Memer;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genoviz.util.ComponentPagePrinter;

import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.*;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.StateProvider;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.bookmarks.BookmarkController;
import com.affymetrix.igb.das.DasDiscovery;
import com.affymetrix.igb.das2.Das2Discovery;
import com.affymetrix.igb.menuitem.*;
import com.affymetrix.igb.view.*;
import com.affymetrix.igb.parsers.XmlPrefsParser;
import com.affymetrix.igb.prefs.*;
import com.affymetrix.igb.bookmarks.SimpleBookmarkServer;
import com.affymetrix.igb.glyph.EdgeMatchAdjuster;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap.ActionToggler;
import com.affymetrix.igb.tiers.MultiWindowTierMap;
import com.affymetrix.igb.util.EPSWriter;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.util.UnibrowAuthenticator;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.util.WebBrowserControl;
import com.affymetrix.igb.util.ViewPersistenceUtils;
import com.affymetrix.swing.DisplayUtils;




/**
 *  Main class for the Integrated Genome Browser (IGB, pronounced ig-bee).
 */
public class IGB extends Application
  implements ActionListener, ContextualPopupListener, GroupSelectionListener, SeqSelectionListener  {
  static IGB singleton_igb;
  public static String APP_NAME = IGBConstants.APP_NAME;
	public static String APP_SHORT_NAME = IGBConstants.APP_SHORT_NAME;
  public static String APP_VERSION = IGBConstants.IGB_VERSION;
	/**
	 * HTTP User Agent presented to servers.  Java version is appended by
	 * the JRE at runtime.  The user agent is of the format
	 * "APP_SHORT_NAME/APP_VERSION, os.name/os.version (os.arch)".
	 */
	public static String HttpUserAgent = APP_SHORT_NAME + "/" + APP_VERSION + ", " + System.getProperty("os.name") + "/" + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")";

  //public static boolean USE_OVERVIEW = false;
  public static boolean USE_MULTI_WINDOW_MAP = false;
  public static boolean USE_REFRESH_BUTTON = true;
  public static boolean REPLACE_REPAINT_MANAGER = false;
  public static boolean REPORT_GRAPHICS_CONFIG = false;

  public static String USE_QUICKLOAD_INSTEAD_OF_DAS2 = "USE_QUICKLOAD_INSTEAD_OF_DAS2";
  public static boolean DEFAULT_USE_QUICKLOAD_INSTEAD_OF_DAS2 = false;
  //  public static boolean USE_QUICKLOAD = false;  // if false, QuickLoadView2 may still be used by DataLoadView
  //  public static boolean USE_DATALOAD = true;  //  DataLoadView may also use QuickLoadView2
  public static final boolean DEBUG_EVENTS = false;
  public static final boolean ADD_DIAGNOSTICS = false;
  public static boolean ALLOW_PARTIAL_SEQ_LOADING = true;

  // Whether to allow users to delete data from the loaded AnnotatedSeqGroup.
  // This should not be turned on until all the caching and optimization features
  // are aware of how to deal with it.  Specifically, QuickLoad needs to know
  // when data has been deleted so that it can reload data of the same time
  // if requested, and the DAS/1 loader needs to get rid of its cached queries
  // related to the given feature type.
  public static final boolean ALLOW_DELETING_DATA = false;

  public static final String PREF_SEQUENCE_ACCESSIBLE = "Sequence accessible";
  public static boolean default_sequence_accessible = true;

  final static String TABBED_PANES_TITLE = "Tabbed Panes";

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static Map prefs_hash;
  static String[] main_args;
  static Map comp2window = new HashMap(); // Maps Component -> Frame
  Map comp2plugin = new HashMap(); // Maps Component -> PluginInfo
  Map comp2menu_item = new HashMap(); // Component -> JCheckBoxMenuItem

  JMenu popup_windowsM = new JMenu("Open in Window...");
  Memer mem = new Memer();
  SimpleBookmarkServer web_control = null;

  JFrame frm;
  JMenuBar mbar;
  JMenu file_menu;
  JMenu export_to_file_menu;
  JMenu view_menu;
  //JMenu navigation_menu;
  JMenu bookmark_menu;
  JMenu tools_menu;
  JMenu help_menu;
  JTabbedPane tab_pane;
  JSplitPane splitpane;

  public BookMarkAction bmark_action; // needs to be public for the BookmarkManagerView plugin
  LoadFileAction open_file_action;
  DasFeaturesAction2 load_das_action;

  JMenuItem gc_item;
  JMenuItem memory_item;
  JMenuItem about_item;
  JMenuItem documentation_item;
  JMenuItem console_item;

  JMenuItem clear_item;
  JMenuItem clear_graphs_item;

  JMenuItem open_file_item;
  //JMenuItem load_das_item;
  JMenuItem print_item;
  JMenuItem print_frame_item;
  JMenuItem export_map_item;
  JMenuItem export_labelled_map_item;
  JMenuItem export_slice_item;
  JMenuItem preferences_item;
  JMenuItem exit_item;

  JMenuItem view_ucsc_item;

  JMenuItem res2clip_item;
  JMenuItem clamp_view_item;
  JMenuItem unclamp_item;
  JMenuItem rev_comp_item;
  JCheckBoxMenuItem shrink_wrap_item;
  JMenuItem adjust_edgematch_item;
  JCheckBoxMenuItem toggle_hairline_label_item;
  JCheckBoxMenuItem toggle_edge_matching_item;
  JMenuItem autoscroll_item;
  JMenuItem web_links_item;

  JMenuItem move_tab_to_window_item;
  JMenuItem move_tabbed_panel_to_window_item;

  SeqMapView map_view;
  //OverView overview;

  //QuickLoaderView quickload_view;

  CurationControl curation_control;
  AlignControl align_control;
  DataLoadView data_load_view = null;
  AltSpliceView slice_view = null;

  List plugins_info = new ArrayList(16);
  List plugins = new ArrayList(16);

  static String user_dir = System.getProperty("user.dir");
  static String user_home = System.getProperty("user.home");

  FileTracker load_directory = FileTracker.DATA_DIR_TRACKER;

  static final String WEB_PREFS_URL = "http://genoviz.sourceforge.net/igb_web_prefs.xml";

  static String default_prefs_resource = "/igb_default_prefs.xml";

  /**
   *  We no longer distribute a file called "igb_prefs.xml".
   *  Instead there is a default prefs file hidden inside the igb.jar file, and
   *  this is augmented by a web-based prefs file at {@link #WEB_PREFS_URL}.
   *  But, we still will load a file called "igb_prefs.xml" if it exists in
   *  the user's home directory, since they may have put some personal modifications
   *  there.
   */
  public static final String DEFAULT_PREFS_FILENAME = "igb_prefs.xml";
  static String default_user_prefs_files =
    (new File(user_home, DEFAULT_PREFS_FILENAME)).getAbsolutePath() +
    ";" +
    (new File(user_dir, DEFAULT_PREFS_FILENAME)).getAbsolutePath();

  static String rest_file = "rest_enzymes"; // located in same directory as this class

  boolean initialized = false;

  /**
   * Start the program.
   */
  public static void main(String[] args) {
    try {
        
        // Configure HTTP User agent
        System.setProperty("http.agent", HttpUserAgent);    

    // Turn on anti-aliased fonts. (Ignored prior to JDK1.5)
    System.setProperty("swing.aatext", "true");

    // Letting the look-and-feel determine the window decorations would
    // allow exporting the whole frame, including decorations, to an eps file.
    // But it also may take away some things, like resizing buttons, that the
    // user is used to in their operating system, so leave as false.
    JFrame.setDefaultLookAndFeelDecorated(false);

    String laf = System.getProperty("swing.defaultlaf");
    // if laf != null, then the user-requested l-and-f has already been applied
    // if laf == null, then apply the windows look and feel
    if (laf == null) try {
      // It this is Windows, then use the Windows look and feel.

      Class cl = Class.forName("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
      LookAndFeel look_and_feel = (LookAndFeel) cl.newInstance();

      if (look_and_feel.isSupportedLookAndFeel()) {
        UIManager.setLookAndFeel(look_and_feel);
      }
    } catch (Exception ulfe) {
      // Windows look and feel is only supported on Windows, and only in
      // some version of the jre.  That is perfectly ok.
    }

    // Initialize the ConsoleView right off, so that ALL output will
    // be captured there.
    ConsoleView.init();

    System.out.println("Starting \"" + APP_NAME + " " + APP_VERSION + "\"");
    System.out.println();

    main_args = args;

    String offline = get_arg("-offline", args);
    if (offline != null) {
      LocalUrlCacher.setOffLine("true".equals(offline));
    }

    getIGBPrefs(); // force loading of prefs

    String quick_load_url = QuickLoadView2.getQuickLoadUrl();
    //    String quick_load_url = "file:/C:/data/quickload/";
    SynonymLookup dlookup = SynonymLookup.getDefaultLookup();
    LocalUrlCacher.loadSynonyms(dlookup, quick_load_url + "synonyms.txt");
    processDasServersList(quick_load_url);
    //processDas2ServersList(quick_load_url);   -- the processing code was commented out.

    singleton_igb = new IGB();
    singleton_igb.init();


    // If the command line contains a parameter "-href http://..." where
    // the URL is a valid IGB control bookmark, then go to that bookmark.
    final String url = get_arg("-href", args);
    if (url != null && url.length() > 0) {
      try {
        final Bookmark bm = new Bookmark(null, url);
        if (bm.isUnibrowControl()) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              System.out.println("Loading bookmark: "+url);
              BookmarkController.viewBookmark(singleton_igb, bm);
            }
          });
        } else {
          System.out.println("ERROR: URL given with -href argument is not a valid bookmark: \n" + url);
        }
      } catch (MalformedURLException mue) {
        mue.printStackTrace(System.err);
      }
    }

   } catch (Exception e) {
     e.printStackTrace();
     System.exit(1);
   }
  }


  public IGB() { }

  /**
   * Adds the DAS servers from the URL gl_url to the
   *  persistent list managed by DasDiscovery.  If the file doesn't exist,
   *  or can't be loaded, a warning is printed to stdout, but that is all,
   *  since it isn't a fatal error.
   *  Meant to replace QuickLoadView2.processDasServerList()
   *  @param ql_url The root URL for the QuickLoad server, ending with "/".
   */
  public static void processDasServersList(String ql_url) {
    String server_loc_list = ql_url + "das_servers.txt";
    try {
      System.out.println("Trying to load DAS Server list: " + server_loc_list);
      DasDiscovery.addServersFromTabFile(server_loc_list);
    }
    catch (Exception ex) {
      System.out.println("WARNING: Failed to load DAS Server list: " + ex);
    }
  }

  public static void processDas2ServersList(String ql_url) {
    String server_loc_list = ql_url + "das2_servers.txt";
    try {
      System.out.println("Trying to load DAS2 Server list: " + server_loc_list);
      Das2Discovery.addServersFromTabFile(server_loc_list);
    }
    catch (Exception ex) {
      System.out.println("WARNING: Failed to load DAS2 Server list: " + ex);
    }
  }


  public static boolean isSequenceAccessible() {
    //return UnibrowPrefsUtil.getBooleanParam(PREF_SEQUENCE_ACCESSIBLE, default_sequence_accessible);
    return default_sequence_accessible;
  }

  public SeqMapView getMapView() {
    return map_view;
  }

  public CurationControl getCurationControl() {
    return curation_control;
  }

  // currently not needed
  //public QuickLoaderView getQuickLoaderView() {
  //  return quickload_view;
  //}

  public static Application getSingleton() {
    return singleton_igb;
  }

  public JMenuBar getMenuBar() { return mbar; }
  public JFrame getFrame() { return frm; }
  public JTabbedPane getTabPane() { return tab_pane; }

  void startControlServer() {
    // Use the Swing Thread to start a non-Swing thread
    // that will start the control server.
    // Thus the control server will be started only after current GUI stuff is finished,
    // but starting it won't cause the GUI to hang.

    Runnable r = new Runnable() {
      public void run() {
        web_control = new SimpleBookmarkServer(IGB.this);
      }
    };

    final Thread t = new Thread(r);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        t.start();
      }
    });
  }

  public SimpleBookmarkServer getControlServer() {
    return web_control;
  }

  /**
   * Parse the command line arguments.  Find out what prefs file to use.
   * Return the name of the file as a String, or null if not invoked with
   * -prefs option.
   */
  public static String[] get_prefs_list(String[] args) {
    String files = get_arg("-prefs", args);
    if (files==null) {files = default_user_prefs_files;}
    StringTokenizer st = new StringTokenizer(files, ";");
    Set result = new HashSet();
    result.add(st.nextToken());
    while (st.hasMoreTokens()) {
      result.add(st.nextToken());
    }
    return (String[]) result.toArray(new String[result.size()]);
  }

  public static String get_default_prefs_url(String[] args) {
    String def_prefs_url = get_arg("-default_prefs_url", args);
    return def_prefs_url;
  }


  /**
   * Returns the value of the argument indicated by label.
   * If arguments are
   *   "-flag_2 -foo bar", then get_arg("foo", args)
   * returns "bar", get_arg("flag_2") returns a non-null string,
   * and get_arg("flag_5") returns null.
   */
  public static String get_arg(String label,String[] args) {
    String to_return = null;
    boolean got_it = false;
    if (label != null && args != null) {
      int num_args = args.length;
      for (int i = 0 ; i < num_args ; i++) {
	String item = args[i];
	if (got_it) {
	  to_return = item;
	  break;
	}
	if (item.equals(label)) {
	  got_it = true;
	}
      }
    }
    if (got_it && to_return == null) {
      to_return = "true";
    }
    return to_return;
  }

  public static SingletonGenometryModel getGenometryModel() {
    return gmodel;
  }

  /**
   *  Returns IGB prefs hash
   *  If prefs haven't been loaded yet, will force loading of prefs
   */
  public static Map getIGBPrefs() {
      if (prefs_hash != null) {
          return prefs_hash;
      }

      prefs_hash = new HashMap();
      XmlPrefsParser prefs_parser = new XmlPrefsParser();
      
      LoadDefaultPrefsFromJar(prefs_parser);

      LoadWebPrefs(prefs_parser);
      
      LoadFileOrURLPrefs(prefs_parser);

      return prefs_hash;
  }

  private static void LoadDefaultPrefsFromJar(XmlPrefsParser prefs_parser) {
      /**  first load default prefs from jar */
      InputStream default_prefs_stream = null;
      try {
          default_prefs_stream = IGB.class.getResourceAsStream(default_prefs_resource);
          System.out.println("loading default prefs from: " + default_prefs_resource);
          prefs_parser.parse(default_prefs_stream, "", prefs_hash);
      } catch (Exception ex) {
          System.out.println("Problem parsing prefs from: " + default_prefs_resource);
          ex.printStackTrace();
      } finally {
          try {
              default_prefs_stream.close();
          } catch (Exception e) {
          }
      }
    }
  
     
  private static void LoadWebPrefs(XmlPrefsParser prefs_parser) {
      // If a particular web prefs file was specified, then load it.
      // Otherwise try to load the web-based-default prefs file. (But
      // only load it if it is cached, then later update the cache on
      // a background thread.)
      String def_prefs_url = get_default_prefs_url(main_args);
      if (def_prefs_url == null) {
          loadDefaultWebBasedPrefs(prefs_parser, prefs_hash);
      } else {
          LoadPreferencesFromURL(def_prefs_url, prefs_parser);
      }
    }
     
    /**
   *  Attempts to load the web-based XML default preferences file from the
   *  local cache.  If this file is not in the cache, will skip
   *  it and will NOT try to read it from the web.  (This is to prevent slowing
   *  down the start-up process.)  Regardless of whether the file was actualy read,
   *  will then spawn a background thread that will try to create or update
   *  the local cached copy of this preferences file so it will be available
   *  the next time the program runs.
   *
   */
    private static void loadDefaultWebBasedPrefs(XmlPrefsParser prefs_parser, Map prefs_hash) {
        String web_prefs_url = WEB_PREFS_URL;
        InputStream is = null;
        try {
            is = LocalUrlCacher.getInputStream(web_prefs_url, LocalUrlCacher.ONLY_CACHE, true);
        } catch (IOException ioe) {
            System.out.println("There is no cached copy of the web preferences file " + web_prefs_url);
            is = null;
        }

        if (is != null) {
            try {
                prefs_parser.parse(is, web_prefs_url, prefs_hash);
                System.out.println("Loading default prefs from url: " + web_prefs_url);
            } catch (Exception ex) {
                System.out.println("Problem parsing prefs from url: " + web_prefs_url);
                System.out.println("Caused by: " + ex.toString());
            } finally {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }

        LocalUrlCacher.updateCacheUrlInBackground(web_prefs_url);
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
            try {
                prefs_url_stream.close();
            } catch (Exception e) {
            }
        }
    }
    
    private static void LoadFileOrURLPrefs(XmlPrefsParser prefs_parser) {
        String[] prefs_list = get_prefs_list(main_args);
        if (prefs_list == null || prefs_list.length == 0)
            return;
        
        prefs_parser = new XmlPrefsParser();
        for (int i = 0; i < prefs_list.length; i++) {
            String fileOrURL = prefs_list[i];
            InputStream strm = null;

            try {
                System.out.flush();
                System.out.println("loading user prefs from: " + fileOrURL);
                File fil = new File(fileOrURL);
                if (fil.exists()) {
                    strm = new FileInputStream(fil);
                    prefs_parser.parse(strm, fil.getCanonicalPath(), prefs_hash);
                } else {
                    // May be a URL
                    if (fileOrURL.startsWith("http")) {
                        LoadPreferencesFromURL(fileOrURL, prefs_parser);
                    } else {
                        System.out.println("could not find prefs file: " + fileOrURL);
                    }
                }
            } catch (Exception ex) {
                System.out.flush();
                System.out.println("Problem parsing prefs from: " + fileOrURL);
                System.out.println(ex.toString());
            } finally {
                try {
                    strm.close();
                } catch (Exception e) {
                }
            }
        }
    }
    


  protected void init() {
    frm = new JFrame(APP_NAME + " " + APP_VERSION);
    RepaintManager rm = RepaintManager.currentManager(frm);
    /*
    if (REPLACE_REPAINT_MANAGER) {
      RepaintManager new_manager = new IgbRepaintManager();
      new_manager.setDoubleBufferMaximumSize(new Dimension(4096, 768));
      RepaintManager.setCurrentManager(new_manager);
      //	RepaintManager.setCurrentManager(new DiagnosticRepaintManager());
    }
    else {
    }
    */
    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    // hardwiring to switch to using multiple windows for main map if there are 4 or more screens
    GraphicsDevice[] devices = genv.getScreenDevices();

    if (devices.length >= 4) {
      int multi_window_choice =
	JOptionPane.showConfirmDialog(frm,
				      "Use multi-screen rendering speedups?\n" +
				      "(but some features will be disabled)",
				      "Multi-screen Rendering Options",
				      JOptionPane.YES_NO_OPTION);

      if (multi_window_choice == JOptionPane.YES_OPTION) {
	System.out.println("&&&&&&&&&&&&&&  MULTI_WINDOW_CHOICE = YES &&&&&&&&&&&&&&");
	USE_MULTI_WINDOW_MAP = true;
	REPORT_GRAPHICS_CONFIG = true;
      }
    }
    if (REPORT_GRAPHICS_CONFIG)  {
      System.out.println("*** double buffer max size: " + rm.getDoubleBufferMaximumSize());
      GraphicsConfigChecker gchecker = new GraphicsConfigChecker();  // auto-reports config
    }
    // force loading of prefs if hasn't happened yet
    // usually since IGB.main() is called first, prefs will have already been loaded
    //   via getUnibrowPrefs() call in main().  But if for some reason an IGB instance
    //   is created without call to main(), will force loading of prefs here...
    getIGBPrefs();

    StateProvider stateProvider = new IGBStateProvider();
    DefaultStateProvider.setGlobalStateProvider(stateProvider);

    // when HTTP authentication is needed, getPasswordAuthentication will
    //    be called on the authenticator set as the default
    Authenticator.setDefault(new UnibrowAuthenticator(frm));

    frm.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    Image icon = getIcon();
    if (icon != null) { frm.setIconImage(icon); }

    mbar = MenuUtil.getMainMenuBar();
    frm.setJMenuBar(mbar);
    file_menu = MenuUtil.getMenu("File");
    file_menu.setMnemonic('F');
    //mbar.add( file_menu );

    view_menu = MenuUtil.getMenu("View");
    view_menu.setMnemonic('V');
    //mbar.add(view_menu);

    bookmark_menu = MenuUtil.getMenu("Bookmarks");
    bookmark_menu.setMnemonic('B');
    //mbar.add(bookmark_menu);

    tools_menu = MenuUtil.getMenu("Tools");
    tools_menu.setMnemonic('T');
    //mbar.add(tools_menu);

    help_menu = MenuUtil.getMenu("Help");
    help_menu.setMnemonic('H');
    //mbar.add(help_menu);
    //    select_broker = new SymSelectionBroker();

    String tile_xpixels_arg = get_arg("-tile_width", main_args);
    String tile_ypixels_arg = get_arg("-tile_height", main_args);
    String tile_col_arg = get_arg("-tile_columns", main_args);
    String tile_row_arg = get_arg("-tile_rows", main_args);
    if (tile_xpixels_arg != null &&
	tile_ypixels_arg != null &&
	tile_col_arg != null &&
	tile_row_arg != null) {
      USE_MULTI_WINDOW_MAP = true;
      try {
	MultiWindowTierMap.tile_width = Integer.parseInt(tile_xpixels_arg);
	MultiWindowTierMap.tile_height = Integer.parseInt(tile_ypixels_arg);
	MultiWindowTierMap.tile_columns = Integer.parseInt(tile_col_arg);
	MultiWindowTierMap.tile_rows = Integer.parseInt(tile_row_arg);
      }
      catch (Exception ex) {
	ex.printStackTrace();
      }
    }


    gmodel.addGroupSelectionListener(this);
    gmodel.addSeqSelectionListener(this);
    // WARNING!!  IGB _MUST_ be added as group and seq selection listener to model _BEFORE_ map_view is,
    //    otherwise assumptions for persisting group / seq / span prefs are not valid!

    map_view = SeqMapView.makeSeqMapView(true, USE_MULTI_WINDOW_MAP, USE_REFRESH_BUTTON);
    map_view.setFrame(frm);
    gmodel.addSeqSelectionListener(map_view);
    gmodel.addGroupSelectionListener(map_view);
    gmodel.addSymSelectionListener(map_view);
    //    gmodel.addSeqModifiedListener(map_view);

    //    navigation_menu = map_view.getNavigationMenu("Go");
    //    navigation_menu.setMnemonic('G');
    //    navigation_menu.add(new JMenu("Genome..."));
    //    mbar.add( navigation_menu, 2);

    bmark_action = new BookMarkAction(this, map_view, bookmark_menu);

    align_control = new AlignControl(this, map_view);
    if (UnibrowPrefsUtil.getTopNode().getBoolean(CurationControl.PREF_ENABLE_CURATIONS, CurationControl.default_enable_curations)) {
      curation_control = new CurationControl(map_view);
    }

    open_file_action = new LoadFileAction(map_view.getFrame(), load_directory);
    load_das_action = new DasFeaturesAction2(map_view);
    clear_item = new JMenuItem("Clear All", KeyEvent.VK_C);
    clear_graphs_item = new JMenuItem("Clear Graphs", KeyEvent.VK_L);
    open_file_item = new JMenuItem("Open file", KeyEvent.VK_O);
    open_file_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Open16.gif"));
    //load_das_item = new JMenuItem("Access DAS/1 Servers", KeyEvent.VK_D);
    //load_das_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Import16.gif"));
    print_item = new JMenuItem("Print", KeyEvent.VK_P);
    print_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Print16.gif"));
    print_frame_item = new JMenuItem("Print Whole Frame", KeyEvent.VK_F);
    export_to_file_menu = new JMenu("Print to EPS File");
    export_to_file_menu.setMnemonic('T');
    export_map_item = new JMenuItem("Main View", KeyEvent.VK_M);
    export_labelled_map_item = new JMenuItem("Main View (With Labels)", KeyEvent.VK_L);
    export_slice_item = new JMenuItem("Sliced View (With Labels)", KeyEvent.VK_S);

    exit_item = new JMenuItem("Exit", KeyEvent.VK_E);

    adjust_edgematch_item = new JMenuItem("Adjust edge match fuzziness", KeyEvent.VK_F);
    view_ucsc_item = new JMenuItem("View Region in UCSC Browser", KeyEvent.VK_R);
    view_ucsc_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/development/WebComponent16.gif"));

    clamp_view_item = new JMenuItem("Clamp To View", KeyEvent.VK_V);
    res2clip_item = new JMenuItem("Copy Selected Residues to Clipboard", KeyEvent.VK_C);
    res2clip_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Copy16.gif"));
    unclamp_item = new JMenuItem("Unclamp", KeyEvent.VK_U);
    rev_comp_item = new JMenuItem("Reverse Complement", KeyEvent.VK_R);
    shrink_wrap_item = new JCheckBoxMenuItem("Toggle Shrink Wrapping");
    shrink_wrap_item.setMnemonic(KeyEvent.VK_S);
    shrink_wrap_item.setState(map_view.getShrinkWrap());

    toggle_hairline_label_item = new JCheckBoxMenuItem("Toggle Hairline Label");
    toggle_hairline_label_item.setMnemonic(KeyEvent.VK_H);
    boolean use_hairline_label = UnibrowPrefsUtil.getTopNode().getBoolean(SeqMapView.PREF_HAIRLINE_LABELED, false);
    if (map_view.isHairlineLabeled() != use_hairline_label) {
      map_view.toggleHairlineLabel();
    }
    toggle_hairline_label_item.setState(map_view.isHairlineLabeled());

    toggle_edge_matching_item = new JCheckBoxMenuItem("Toggle Edge Matching");
    toggle_edge_matching_item.setMnemonic(KeyEvent.VK_M);
    toggle_edge_matching_item.setState(map_view.getEdgeMatching());
    autoscroll_item = new JMenuItem("AutoScroll", KeyEvent.VK_A);
    autoscroll_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/media/Movie16.gif"));
    move_tab_to_window_item = new JMenuItem("Open Current Tab in New Window", KeyEvent.VK_O);
    move_tabbed_panel_to_window_item = new JMenuItem("Open Tabbed Panes in New Window", KeyEvent.VK_P);

    web_links_item = new JMenuItem(WebLinksManagerView.getShowFrameAction());

    preferences_item = new JMenuItem("Preferences ...", KeyEvent.VK_E);
    preferences_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Preferences16.gif"));
    preferences_item.addActionListener(this);

    MenuUtil.addToMenu(file_menu, open_file_item);
    //MenuUtil.addToMenu(file_menu, load_das_item);
    MenuUtil.addToMenu(file_menu, clear_item);
    MenuUtil.addToMenu(file_menu, clear_graphs_item);
    file_menu.addSeparator();
    MenuUtil.addToMenu(file_menu, print_item);
    file_menu.add(export_to_file_menu);
    MenuUtil.addToMenu(export_to_file_menu, export_map_item);
    MenuUtil.addToMenu(export_to_file_menu, export_labelled_map_item);
    file_menu.addSeparator();
    MenuUtil.addToMenu(file_menu, preferences_item);

    file_menu.addSeparator();
    MenuUtil.addToMenu(file_menu, exit_item);

    // rev_comp option currently not working, so disabled
    JMenu strands_menu = new JMenu("Strands");
    strands_menu.add(new ActionToggler(getMapView().getSeqMap().show_plus_action));
    strands_menu.add(new ActionToggler(getMapView().getSeqMap().show_minus_action));
    strands_menu.add(new ActionToggler(getMapView().getSeqMap().show_mixed_action));

    view_menu.add(strands_menu);
    //    MenuUtil.addToMenu(view_menu, rev_comp_item);
    MenuUtil.addToMenu(view_menu, autoscroll_item);
    MenuUtil.addToMenu(view_menu, res2clip_item);
    MenuUtil.addToMenu(view_menu, view_ucsc_item);

    MenuUtil.addToMenu(view_menu, toggle_edge_matching_item);
    MenuUtil.addToMenu(view_menu, adjust_edgematch_item);
    MenuUtil.addToMenu(view_menu, clamp_view_item);
    MenuUtil.addToMenu(view_menu, unclamp_item);
    MenuUtil.addToMenu(view_menu, shrink_wrap_item);
    MenuUtil.addToMenu(view_menu, toggle_hairline_label_item);
    MenuUtil.addToMenu(view_menu, move_tab_to_window_item);
    MenuUtil.addToMenu(view_menu, move_tabbed_panel_to_window_item);

    MenuUtil.addToMenu(tools_menu, web_links_item);

    gc_item = new JMenuItem("Invoke Garbage Collection", KeyEvent.VK_I);
    memory_item = new JMenuItem("Print Memory Usage", KeyEvent.VK_M);
    about_item = new JMenuItem("About " + APP_NAME + "...", KeyEvent.VK_A);
    about_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/About16.gif"));
    console_item = new JMenuItem("Show Console...", KeyEvent.VK_C);
    console_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/development/Host16.gif"));
    documentation_item = new JMenuItem("Documentation...", KeyEvent.VK_D);
    documentation_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Help16.gif"));

    MenuUtil.addToMenu(help_menu, about_item);
    MenuUtil.addToMenu(help_menu, documentation_item);
    MenuUtil.addToMenu(help_menu, console_item);
    if (ADD_DIAGNOSTICS) {
      MenuUtil.addToMenu(help_menu, gc_item);
      MenuUtil.addToMenu(help_menu, memory_item);
    }

    gc_item.addActionListener(this);
    memory_item.addActionListener(this);
    about_item.addActionListener(this);
    documentation_item.addActionListener(this);
    console_item.addActionListener(this);
    clear_item.addActionListener(this);
    clear_graphs_item.addActionListener(this);
    open_file_item.addActionListener(this);
    //load_das_item.addActionListener(this);
    print_item.addActionListener(this);
    print_frame_item.addActionListener(this);
    export_map_item.addActionListener(this);
    export_labelled_map_item.addActionListener(this);
    export_slice_item.addActionListener(this);
    exit_item.addActionListener(this);

    toggle_edge_matching_item.addActionListener(this);
    autoscroll_item.addActionListener(this);
    adjust_edgematch_item.addActionListener(this);
    view_ucsc_item.addActionListener(this);

    res2clip_item.addActionListener(this);
    rev_comp_item.addActionListener(this);
    shrink_wrap_item.addActionListener(this);
    clamp_view_item.addActionListener(this);
    unclamp_item.addActionListener(this);
    toggle_hairline_label_item.addActionListener(this);
    move_tab_to_window_item.addActionListener(this);
    move_tabbed_panel_to_window_item.addActionListener(this);

    Container cpane = frm.getContentPane();
    int table_height = 250;
    int fudge = 55;
    //    RepaintManager rm = RepaintManager.currentManager(frm);
    if (REPORT_GRAPHICS_CONFIG) { System.out.println("repaint manager: " + rm.getClass()); }
    Rectangle frame_bounds = UnibrowPrefsUtil.retrieveWindowLocation("main window",
        new Rectangle(0, 0, 800, 600));
    UnibrowPrefsUtil.setWindowSize(frm, frame_bounds);

    tab_pane = new JTabbedPane();

    cpane.setLayout(new BorderLayout());
    splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitpane.setOneTouchExpandable(true);
    splitpane.setDividerSize(8);
    splitpane.setDividerLocation(frm.getHeight() - (table_height + fudge));
    splitpane.setTopComponent(map_view);

    boolean tab_panel_in_a_window = (UnibrowPrefsUtil.getComponentState(TABBED_PANES_TITLE).equals(UnibrowPrefsUtil.COMPONENT_STATE_WINDOW));
    if (tab_panel_in_a_window) {
      openTabbedPanelInNewWindow(tab_pane);
    } else {
      splitpane.setBottomComponent(tab_pane);
    }

    /*if (USE_OVERVIEW) {
      //      overview = new SeqMapView(true);
      overview = new OverView(false);
      gmodel.addSeqSelectionListener(overview);
      gmodel.addGroupSelectionListener(overview);
      gmodel.addSymSelectionListener(overview);
      overview.setFrame(frm);
      JSplitPane oversplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
      oversplit.setOneTouchExpandable(true);
      oversplit.setDividerSize(8);
      oversplit.setDividerLocation(100);
      oversplit.setTopComponent(overview);
      oversplit.setBottomComponent(splitpane);
      cpane.add("Center", oversplit);
    }
    else {*/
      cpane.add("Center", splitpane);
    //}

    // Using JTabbedPane.SCROLL_TAB_LAYOUT makes it impossible to add a
    // pop-up menu (or any other mouse listener) on the tab handles.
    // (A pop-up with "Open tab in a new window" would be nice.)
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4465870
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4499556
    tab_pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    tab_pane.setMinimumSize(new Dimension(0,0));

    if (USE_STATUS_BAR) {
      status_bar.setStatus(getApplicationName() + " " + getVersion());
      cpane.add(status_bar, BorderLayout.SOUTH);
    }

    // Show the frame before loading the plugins.  Thus any error panel
    // that is created by an exception during plugin set-up will appear
    // on top of the main frame, not hidden by it.

    frm.addWindowListener( new WindowAdapter() {
	public void windowClosing(WindowEvent evt) {exit();}
      });
    //    frm.resize(1000, 750);
    frm.setVisible(true);

    if (useQuickLoad()) {
      //      PluginInfo quickload = new PluginInfo(QuickLoaderView.class.getName(), "QuickLoad", true);
      PluginInfo quickload = new PluginInfo(QuickLoadView2.class.getName(), "QuickLoad", true);
      plugins_info.add(quickload);
    }
    else {
      //    if (USE_DATALOAD)  {
      PluginInfo dataload = new PluginInfo(DataLoadView.class.getName(), "Data Access", true);
      plugins_info.add(dataload);

    }

    PluginInfo selection_info = new PluginInfo(SymTableView.class.getName(), "Selection Info", true);
    plugins_info.add(selection_info);

    plugins_info.addAll(getPluginsFromXmlPrefs(getIGBPrefs()));
    //plugin_list = null;
    //try {
    //  plugin_list = Plugin>Info.getAllPlugins();
    //} catch (java.util.prefs.BackingStoreException bse) {
    //  UnibrowPrefsUtil.handleBSE(this.frm, bse);
    //}

    if (plugins_info == null || plugins_info.isEmpty()) {
      System.out.println("There are no plugins specified in preferences.");
    } else {
      Iterator iter = plugins_info.iterator();
      while (iter.hasNext()) {
        PluginInfo pi = (PluginInfo) iter.next();
        Object plugin = setUpPlugIn(pi);
        plugins.add(plugin);
      }
    }

    for (int i=0; i<plugins.size(); i++)  {
        Object plugin = plugins.get(i);
        if (plugin instanceof DataLoadView) {
            data_load_view = (DataLoadView)plugin;
            //data_load_view.initialize();
        }
        if (plugin instanceof QuickLoadView2)  {
          ((QuickLoadView2)plugin).initialize();
        }
    }

    if (slice_view != null) {
      MenuUtil.addToMenu(export_to_file_menu, export_slice_item);
      export_slice_item.setEnabled(true);
    }

    WebLink.autoLoad();

    // bootstrap bookmark from Preferences for last genome / sequence / region
    ViewPersistenceUtils.restoreLastView(map_view);

    // Need to let the QuickLoad system get started-up before starting
    //   the control server that listens to ping requests?
    // Therefore start listening for http requests only after all set-up is done.
    startControlServer();

    initialized = true;
  }

  public static boolean useQuickLoad() {
    return UnibrowPrefsUtil.getBooleanParam(USE_QUICKLOAD_INSTEAD_OF_DAS2, DEFAULT_USE_QUICKLOAD_INSTEAD_OF_DAS2);
  }


  /** Returns true if initialization has completed. */
  public boolean isInitialized() {
    return initialized;
  }

  /**
   *  Puts the given component either in the tab pane or in its own window,
   *  depending on saved user preferences.
   */
  Object setUpPlugIn(PluginInfo pi) {

    if (! pi.shouldLoad()) return null;

    String class_name = pi.getClassName();
    if (class_name == null || class_name.trim().length()==0) {
      ErrorHandler.errorPanel("Bad Plugin",
        "Cannot create plugin '"+pi.getPluginName()+"' because it has no class name.",
        this.frm);
      PluginInfo.getNodeForName(pi.getPluginName()).putBoolean("load", false);
      return null;
    }

    Object plugin = null;
    Throwable t = null;
    try {
      plugin = pi.instantiatePlugin(class_name);
    } catch (InstantiationException e) {
      plugin = null;
      t = e;
    }

    if (plugin == null) {
      ErrorHandler.errorPanel("Bad Plugin",
        "Could not create plugin '"+pi.getPluginName()+"'.",
        this.frm, t);
      PluginInfo.getNodeForName(pi.getPluginName()).putBoolean("load", false);
      return null;
    }

    ImageIcon icon = null;

    if (plugin instanceof IPlugin) {
      IPlugin plugin_view = (IPlugin) plugin;
      this.setPluginInstance(plugin_view.getClass(), plugin_view);
      icon = (ImageIcon) plugin_view.getPluginProperty(IPlugin.TEXT_KEY_ICON);
    }

    if (plugin instanceof JComponent) {
      comp2plugin.put(plugin, pi);
      String title = pi.getDisplayName();
      String tool_tip = ((JComponent) plugin).getToolTipText();
      if (tool_tip == null) {tool_tip = title;}
      JComponent comp = (JComponent) plugin;
      boolean in_a_window = (UnibrowPrefsUtil.getComponentState(title).equals(UnibrowPrefsUtil.COMPONENT_STATE_WINDOW));
      //boolean in_a_window = PluginInfo.PLACEMENT_WINDOW.equals(pi.getPlacement());
      addToPopupWindows(comp, title);
      JCheckBoxMenuItem menu_item = (JCheckBoxMenuItem) comp2menu_item.get(comp);
      menu_item.setSelected(in_a_window);
      if (in_a_window) {
        //openCompInWindow(comp, title, tool_tip, null, tab_pane);
        openCompInWindow(comp, tab_pane);
      }
      else {
        tab_pane.addTab(title, icon, comp, tool_tip);
      }
    }
    return plugin;
  }

  public void setPluginInstance(Class c, IPlugin plugin) {
    super.setPluginInstance(c, plugin);
    if (c.equals(BookmarkManagerView.class)) {
      bmark_action.setBookmarkManager((BookmarkManagerView) plugin);
    }

    if (plugin instanceof DataLoadView) {
      data_load_view = (DataLoadView) plugin;
    }
    if (plugin instanceof AltSpliceView) {
      slice_view = (AltSpliceView) plugin;
    }
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == open_file_item) {
      open_file_action.actionPerformed(evt);
    }
    /*else if (src == load_das_item) {
      load_das_action.actionPerformed(evt);
    }*/
    else if (src == print_item) {
      try {
        map_view.getSeqMap().print();
      }
      catch (Exception ex) {
        errorPanel("Problem trying to print.", ex);
      }
    }
    else if (src == print_frame_item) {
      ComponentPagePrinter cprinter = new ComponentPagePrinter(getFrame());
      try {
        cprinter.print();
      }
      catch (Exception ex) {
        errorPanel("Problem trying to print.", ex);
      }
    }
    else if (src == export_map_item) {
      try {
        EPSWriter.outputToFile(map_view.getSeqMap().getNeoCanvas());
      } catch (Exception ex) {
        errorPanel("Problem during output.", ex);
      }
    }
    else if (src == export_labelled_map_item) {
      try {
        AffyLabelledTierMap tm = (AffyLabelledTierMap) map_view.getSeqMap();
        EPSWriter.outputToFile(tm.getSplitPane());
      } catch (Exception ex) {
        errorPanel("Problem during output.", ex);
      }
    }
    else if (src == export_slice_item) {
      try {
        if (slice_view != null) {
          AffyLabelledTierMap tm = (AffyLabelledTierMap) slice_view.getSplicedView().getSeqMap();
          EPSWriter.outputToFile(tm.getSplitPane());
        }
      } catch (Exception ex) {
        errorPanel("Problem during output.", ex);
      }
    }
    else if (src == clear_item) {
      if (confirmPanel("Really clear entire view?")) {
        map_view.clear();
      }
    }
    else if (src == clear_graphs_item) {
      if (confirmPanel("Really clear graphs?")) {
        map_view.clearGraphs();
      }
    }
    else if (src == exit_item) {
      exit();
    }
    else if (src == res2clip_item) {
      map_view.copySelectedResidues();
    }
    else if (src == view_ucsc_item) {
      System.out.println("trying to invoke UCSC genome browser");
      map_view.invokeUcscView();
    }
    else if (src == autoscroll_item) {
      map_view.toggleAutoScroll();
    }
    else if (src == toggle_edge_matching_item) {
      map_view.setEdgeMatching(! map_view.getEdgeMatching());
      toggle_edge_matching_item.setState(map_view.getEdgeMatching());
      //adjust_edgematch_item.setEnabled(map_view.getEdgeMatching());
    }
    else if (src == adjust_edgematch_item) {
      EdgeMatchAdjuster.showFramedThresholder(map_view.getEdgeMatcher(), map_view);
    }
    // rev comp not working
    //    else if (src == rev_comp_item) {
    //      map_view.reverseComplement();
    //    }
    else if (src == shrink_wrap_item) {
      System.out.println("trying to toggle map bounds shrink wrapping to extent of annotations");
      map_view.setShrinkWrap(! map_view.getShrinkWrap());
      shrink_wrap_item.setState(map_view.getShrinkWrap());
    }
    else if (src == clamp_view_item) {
      System.out.println("trying to clamp to view");
      map_view.clampToView();
    }
    else if (src == unclamp_item) {
      System.out.println("trying to unclamp");
      map_view.unclamp();
    }
    else if (src == toggle_hairline_label_item) {
      map_view.toggleHairlineLabel();
      boolean b = map_view.isHairlineLabeled();
      toggle_hairline_label_item.setState(b);
      UnibrowPrefsUtil.getTopNode().putBoolean(SeqMapView.PREF_HAIRLINE_LABELED, b);
    } else if (src == move_tab_to_window_item) {
      openTabInNewWindow(tab_pane);
    } else if (src == move_tabbed_panel_to_window_item) {
      openTabbedPanelInNewWindow(tab_pane);
    }
    else if (src == gc_item) {
      System.gc();
    }
    else if (src == memory_item) {
      mem.printMemory();
    }
    else if (src == about_item) {
      showAboutDialog();
    } else if (src == documentation_item) {
      showDocumentationDialog();
    }
    else if (src == console_item) {
      ConsoleView.showConsole();
    } else if (src == preferences_item) {
      PreferencesPanel pv = PreferencesPanel.getSingleton();
      JFrame f = pv.getFrame();
      f.setVisible(true);
    }
  }

  public void showAboutDialog() {
    JPanel message_pane = new JPanel();
    message_pane.setLayout(new BoxLayout(message_pane, BoxLayout.Y_AXIS));
    JTextArea about_text = new JTextArea();
    about_text.append(APP_NAME + ", version: " + APP_VERSION + "\n");
    about_text.append("Copyright 2001-2007 Affymetrix Inc." + "\n");
    about_text.append("\n");
    about_text.append(APP_NAME + " uses the Xerces\n");
    about_text.append("package from the Apache Software Foundation, \n");
    about_text.append("the Fusion SDK from Affymetrix,  \n");
    about_text.append("and the Vector Graphics package from java.FreeHEP.org \n");
    about_text.append("(released under the LGPL license).\n");
    about_text.append(" \n");
    Iterator names = XmlPrefsParser.getFilenames(prefs_hash).iterator();
    if (names.hasNext()) {
      about_text.append("\nLoaded the following preference file(s): \n");
      while (names.hasNext()) {
        about_text.append("  " + (String) names.next() + "\n");
      }
    }
    String cache_root = com.affymetrix.igb.util.LocalUrlCacher.getCacheRoot();
    File cache_file = new File(cache_root);
    if (cache_file.exists()) {
      about_text.append("\nCached data stored in: \n");
      about_text.append("  " + cache_file.getAbsolutePath() + "\n");
    }
    String data_dir = UnibrowPrefsUtil.getAppDataDirectory();
    if (data_dir != null) {
      File data_dir_f = new File(data_dir);
      about_text.append("\nApplication data stored in: \n  "+
        data_dir_f.getAbsolutePath() +"\n");
    }

    message_pane.add(new JScrollPane(about_text));
    JButton licenseB = new JButton("View IGB License");
    JButton apacheB = new JButton("View Apache License");
    JButton freehepB = new JButton("View FreeHEP Vector Graphics License");
    JButton fusionB = new JButton("View Fusion SDK License");
    licenseB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          WebBrowserControl.displayURL("http://www.affymetrix.com/support/developer/tools/igbsource_terms.affx?to");
        }
      } );
    apacheB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          WebBrowserControl.displayURL("http://www.apache.org/licenses/LICENSE-2.0");
        }
      } );
    freehepB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          WebBrowserControl.displayURL("http://java.freehep.org/vectorgraphics/license.html");
        }
      } );
    fusionB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          WebBrowserControl.displayURL("http://www.affymetrix.com/support/developer/fusion/index.affx");
        }
      } );
    JPanel buttonP = new JPanel(new GridLayout(2,2));
    buttonP.add(licenseB);
    buttonP.add(apacheB);
    buttonP.add(freehepB);
    buttonP.add(fusionB);
    message_pane.add(buttonP);

    final JOptionPane pane = new JOptionPane(message_pane, JOptionPane.INFORMATION_MESSAGE,
     JOptionPane.DEFAULT_OPTION);
    final JDialog dialog = pane.createDialog(frm, "About " + APP_NAME);
    //dialog.setResizable(true);
    dialog.setVisible(true);
  }

  public void showDocumentationDialog() {
    JPanel message_pane = new JPanel();
    message_pane.setLayout(new BoxLayout(message_pane, BoxLayout.Y_AXIS));
    JTextArea about_text = new JTextArea();
    about_text.append(DocumentationView.getDocumentationText());
    message_pane.add(new JScrollPane(about_text));

    JButton sfB = new JButton("Go To IGB at SourceForge");
    sfB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          WebBrowserControl.displayURL("http://sourceforge.net/projects/genoviz/");
        }
      } );
    Box buttonP = Box.createHorizontalBox();
    buttonP.add(sfB);

    message_pane.add(buttonP);

    final JOptionPane pane = new JOptionPane(message_pane, JOptionPane.INFORMATION_MESSAGE,
     JOptionPane.DEFAULT_OPTION);
    final JDialog dialog = pane.createDialog(frm, "Documentation");
    dialog.setResizable(true);
    dialog.setVisible(true);
  }


  /** Returns the icon stored in the jar file.
   *  It is expected to be at com.affymetrix.igb.affychip.gif.
   *  @return null if the image file is not found or can't be opened.
   */
  public Image getIcon() {
    Image icon = null;
    try {
      URL url = IGB.class.getResource("affychip.gif");
      if (url != null) {
        icon = Toolkit.getDefaultToolkit().getImage(url);
      }
    } catch (Exception e) {
      // It isn't a big deal if we can't find the icon, just return null
    }
    return icon;
  }

  private void exit() {
    boolean ask_before_exit = UnibrowPrefsUtil.getBooleanParam(UnibrowPrefsUtil.ASK_BEFORE_EXITING,
        UnibrowPrefsUtil.default_ask_before_exiting);
    String message = "Save state and exit?";
    if ( (! ask_before_exit) || confirmPanel(message)) {
      if (bmark_action != null) {
        bmark_action.autoSaveBookmarks();
      }
      WebLink.autoSave();
      saveWindowLocations();
      ViewPersistenceUtils.saveCurrentView(map_view);
      System.exit(0);
    }
  }


  /**
   * Saves information about which plugins are in separate windows and
   * what their preferred sizes are.
   */
  private void saveWindowLocations() {
    // Save the main window location
    UnibrowPrefsUtil.saveWindowLocation(frm, "main window");

    Iterator iter = comp2plugin.keySet().iterator();
    while (iter.hasNext()) {
      Component comp = (Component) iter.next();
      PluginInfo pi = (PluginInfo) comp2plugin.get(comp);
      Frame f = (Frame) comp2window.get(comp);
      if (f != null) {
        UnibrowPrefsUtil.saveWindowLocation(f, pi.getPluginName());
      }
    }
    Frame f = (Frame) comp2window.get(tab_pane);
    if (f != null) {
      UnibrowPrefsUtil.saveWindowLocation(f, TABBED_PANES_TITLE);
    }
  }

  public void openTabInNewWindow(final JTabbedPane tab_pane) {
    Runnable r = new Runnable() {
      public void run() {
        int index = tab_pane.getSelectedIndex();
        if (index<0) {
          errorPanel("No more panes!");
          return;
        }
        final JComponent comp = (JComponent) tab_pane.getComponentAt(index);
        final String title = tab_pane.getTitleAt(index);
        final String tool_tip = tab_pane.getToolTipTextAt(index);
        //openCompInWindow(comp, title, tool_tip, null, tab_pane);
        openCompInWindow(comp, tab_pane);
      }
    };
    SwingUtilities.invokeLater(r);
  }

  void openCompInWindow(final JComponent comp, final JTabbedPane tab_pane) {
    final String title;
    final String display_name;
    final String tool_tip = comp.getToolTipText();

    if (comp2plugin.get(comp) instanceof PluginInfo) {
      PluginInfo pi = (PluginInfo) comp2plugin.get(comp);
      title = pi.getPluginName();
      display_name = pi.getDisplayName();
    } else {
      title = comp.getName();
      display_name = comp.getName();
    }

    Image temp_icon = null;
    if (comp instanceof IPlugin) {
      IPlugin pv = (IPlugin) comp;
      ImageIcon image_icon = (ImageIcon) pv.getPluginProperty(IPlugin.TEXT_KEY_ICON);
      if (image_icon != null) temp_icon = image_icon.getImage();
    }
    if (temp_icon==null) { temp_icon = getIcon(); }

    // If not already open in a new window, make a new window
    if (comp2window.get(comp) == null) {
      tab_pane.remove(comp);
      tab_pane.validate();

      final JFrame frame = new JFrame(display_name);
      final Image icon = temp_icon;
      if (icon != null) { frame.setIconImage(icon); }
      final Container cont = frame.getContentPane();
      frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

      cont.add(comp);
      comp.setVisible(true);
      comp2window.put(comp, frame);
      frame.pack(); // pack() to set frame to its preferred size

      Rectangle pos = UnibrowPrefsUtil.retrieveWindowLocation(title, frame.getBounds());
      if (pos != null) {
        UnibrowPrefsUtil.setWindowSize(frame, pos);
      }
      frame.setVisible(true);
      frame.addWindowListener( new WindowAdapter() {
	  public void windowClosing(WindowEvent evt) {
            // save the current size into the preferences, so the window
            // will re-open with this size next time
            UnibrowPrefsUtil.saveWindowLocation(frame, title);
	    comp2window.remove(comp);
	    cont.remove(comp);
	    cont.validate();
	    frame.dispose();
	    tab_pane.addTab(display_name, null, comp, (tool_tip == null ? display_name : tool_tip));
            UnibrowPrefsUtil.saveComponentState(title, UnibrowPrefsUtil.COMPONENT_STATE_TAB);
            //PluginInfo.getNodeForName(title).put(PluginInfo.KEY_PLACEMENT, PluginInfo.PLACEMENT_TAB);
            JCheckBoxMenuItem menu_item = (JCheckBoxMenuItem) comp2menu_item.get(comp);
            if (menu_item != null) {
              menu_item.setSelected(false);
            }
	  }
	});
    }
    // extra window already exists, but may not be visible
    else {
      DisplayUtils.bringFrameToFront((Frame) comp2window.get(comp));
    }
    UnibrowPrefsUtil.saveComponentState(title, UnibrowPrefsUtil.COMPONENT_STATE_WINDOW);
    //PluginInfo.getNodeForName(title).put(PluginInfo.KEY_PLACEMENT, PluginInfo.PLACEMENT_WINDOW);
  }

  void openTabbedPanelInNewWindow(final JComponent comp) {

    final String title = TABBED_PANES_TITLE;
    final String display_name = title;
    final String tool_tip = null;
    Image temp_icon = null;

    // If not already open in a new window, make a new window
    if (comp2window.get(comp) == null) {
      splitpane.remove(comp);
      splitpane.validate();

      final JFrame frame = new JFrame(display_name);
      final Image icon = temp_icon;
      if (icon != null) { frame.setIconImage(icon); }
      final Container cont = frame.getContentPane();
      frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

      cont.add(comp);
      comp.setVisible(true);
      comp2window.put(comp, frame);
      frame.pack(); // pack() to set frame to its preferred size

      Rectangle pos = UnibrowPrefsUtil.retrieveWindowLocation(title, frame.getBounds());
      if (pos != null) {
    	  //check that it's not too small, problems with using two screens
    	  int posW = (int)pos.getWidth();
    	  if (posW < 650) posW = 650;
    	  int posH = (int)pos.getHeight();
    	  if (posH < 300) posH = 300;
    	  pos.setSize(posW, posH);
        UnibrowPrefsUtil.setWindowSize(frame, pos);       
      }
      frame.setVisible(true);

      final Runnable return_panes_to_main_window = new Runnable() {
        public void run() {  	
          // save the current size into the preferences, so the window
          // will re-open with this size next time
          UnibrowPrefsUtil.saveWindowLocation(frame, title);
          comp2window.remove(comp);
          cont.remove(comp);
          cont.validate();
          frame.dispose();
          splitpane.setBottomComponent(comp);
          splitpane.setDividerLocation(0.70);
          UnibrowPrefsUtil.saveComponentState(title, UnibrowPrefsUtil.COMPONENT_STATE_TAB);
          JCheckBoxMenuItem menu_item = (JCheckBoxMenuItem) comp2menu_item.get(comp);
          if (menu_item != null) {
            menu_item.setSelected(false);
          }
        }
      };

      frame.addWindowListener( new WindowAdapter() {
        public void windowClosing(WindowEvent evt) {
          SwingUtilities.invokeLater(return_panes_to_main_window);
        }});

       JMenuBar mbar = new JMenuBar();
       frame.setJMenuBar(mbar);
       JMenu menu1 = new JMenu("Windows");
       menu1.setMnemonic('W');
       mbar.add( menu1 );

       menu1.add(new AbstractAction("Return Tabbed Panes to Main Window") {
        public void actionPerformed(ActionEvent evt) {
          SwingUtilities.invokeLater(return_panes_to_main_window);
        }
       });
       menu1.add(new AbstractAction("Open Current Tab in New Window") {
        public void actionPerformed(ActionEvent evt) {
          openTabInNewWindow(tab_pane);
        }
       });
    }
    // extra window already exists, but may not be visible
    else {
      DisplayUtils.bringFrameToFront((Frame) comp2window.get(comp));
    }
    UnibrowPrefsUtil.saveComponentState(title, UnibrowPrefsUtil.COMPONENT_STATE_WINDOW);
  }

  public void popupNotify(JPopupMenu popup,  List selected_items, SeqSymmetry primary_sym) {
    popup.add(popup_windowsM);
  }

  void addToPopupWindows(final JComponent comp, final String title) {
    JCheckBoxMenuItem popupMI = new JCheckBoxMenuItem(title);
    popup_windowsM.add(popupMI);
    popupMI.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  JCheckBoxMenuItem src = (JCheckBoxMenuItem) evt.getSource();
	  //openCompInWindow(comp, title, tool_tip, null, tab_pane);
          Frame frame = (Frame) comp2window.get(comp);
          if (frame == null) {
            openCompInWindow(comp, tab_pane);
            src.setSelected(true);
          } else {
            // would like to move window back into tab, but needs some work
            //src.setSelected(false);
          }
	}
      } );
    comp2menu_item.put(comp, popupMI);
    popup_windowsM.add(new JCheckBoxMenuItem("foo"));
  }

  /**
   *  Determines which plugins to use based on the preferences file.
   *  Several basic plugins are turned on or off using boolean flags
   *  such as "USE_SLICE_VIEW".  The user can turn these on or off, but
   *  cannot affect their ordering.  Several of these default to "true"; others
   *  default to "false".
   *  Any other plugin can be turned on with the "&lt;plugin ... &gt;" tags.
   *  The ordering of those tags is maintained in the order of the tab panes
   *  they create.
   */
  List getPluginsFromXmlPrefs(Map prefs_hash) {
    ArrayList plugin_list = new ArrayList(16);

    boolean USE_ANNOT_BROWSER = false;
    boolean USE_SLICE_VIEW = true;
    boolean USE_GRAPH_ADJUSTER = true;
    boolean USE_PATTERN_SEARCHER = true;
    boolean USE_RESTRICTION_MAPPER = false;
    boolean USE_PIVOT_VIEW = false;
    if (prefs_hash.get("USE_GRAPH_ADJUSTER") != null) {
      USE_GRAPH_ADJUSTER = ((Boolean)prefs_hash.get("USE_GRAPH_ADJUSTER")).booleanValue(); }
    if (prefs_hash.get("USE_PIVOT_VIEW") != null) {
      USE_PIVOT_VIEW = ((Boolean)prefs_hash.get("USE_PIVOT_VIEW")).booleanValue(); }
    if (prefs_hash.get("USE_SLICE_VIEW") != null) {
      USE_SLICE_VIEW = ((Boolean)prefs_hash.get("USE_SLICE_VIEW")).booleanValue();  }
    if (prefs_hash.get("USE_RESTRICTION_MAPPER") != null) {
      USE_RESTRICTION_MAPPER = ((Boolean)prefs_hash.get("USE_RESTRICTION_MAPPER")).booleanValue(); }
    if (prefs_hash.get("USE_PATTERN_SEARCHER") != null) {
      USE_PATTERN_SEARCHER = ((Boolean)prefs_hash.get("USE_PATTERN_SEARCHER")).booleanValue(); }
    if (prefs_hash.get("USE_ANNOT_BROWSER") != null) {
      USE_ANNOT_BROWSER = ((Boolean)prefs_hash.get("USE_ANNOT_BROWSER")).booleanValue(); }

    if (USE_SLICE_VIEW) {
      PluginInfo pi = new PluginInfo(AltSpliceView.class.getName(), "Sliced View", true);
      plugin_list.add(pi);
    }
    if (USE_GRAPH_ADJUSTER) {
      PluginInfo pi = new PluginInfo(SimpleGraphTab.class.getName(), "Graph Adjuster", true);
      plugin_list.add(pi);
    }
    if (USE_PATTERN_SEARCHER) {
      PluginInfo pi = new PluginInfo(SeqSearchView.class.getName(), "Pattern Search", true);
      plugin_list.add(pi);
    }

    if (USE_PIVOT_VIEW) {
      PluginInfo pi = new PluginInfo(ExperimentPivotView.class.getName(), "Pivot View", true);
      plugin_list.add(pi);
    }

    if (USE_ANNOT_BROWSER) {
      PluginInfo pi = new PluginInfo(AnnotBrowserView.class.getName(), "Annotation Browser", true);
      plugin_list.add(pi);
    }

    if (USE_RESTRICTION_MAPPER) {
      PluginInfo pi = new PluginInfo(RestrictionControlView.class.getName(), "Restriction Sites", true);
      plugin_list.add(pi);
    }

    Map other_plugins = XmlPrefsParser.getNamedMap(prefs_hash, XmlPrefsParser.PLUGINS);
    Iterator iter = other_plugins.values().iterator();
    while (iter.hasNext()) {
      PluginInfo pi = (PluginInfo) iter.next();
      if ("com.affymetrix.igb.plugin.menu.EpsOutputAction".equals(pi.getClassName())) {
        System.out.println("This plugin is obsolete, not using: " + pi.getClassName());
      } else {
        plugin_list.add(pi);
      }
    }

    return plugin_list;
  }

  public String getApplicationName() {
    return APP_NAME;
  }

  public String getVersion() {
    return APP_VERSION;
  }

  /** Not yet implemented. */
  public String getResourceString(String key) {
    return null;
  }


  AnnotatedSeqGroup prev_selected_group = null;
  public void groupSelectionChanged(GroupSelectionEvent evt) {
    AnnotatedSeqGroup selected_group = evt.getSelectedGroup();
    if ((prev_selected_group != selected_group) && (prev_selected_seq != null)) {
      ViewPersistenceUtils.saveSeqSelection(prev_selected_seq);
      ViewPersistenceUtils.saveSeqVisibleSpan(map_view);
    }
    prev_selected_group = selected_group;
  }

  AnnotatedBioSeq prev_selected_seq = null;
  public void seqSelectionChanged(SeqSelectionEvent evt) {
    AnnotatedBioSeq selected_seq = evt.getSelectedSeq();
    if ((prev_selected_seq != null) && (prev_selected_seq != selected_seq)) {
      //      System.out.println("----------- saving visible span selection for seq: " + prev_selected_seq.getID());
      ViewPersistenceUtils.saveSeqVisibleSpan(map_view);
    }
    prev_selected_seq = selected_seq;
  }

  //public static final Logger APP = Logger.getLogger("app", RESOURCE_BUNDLE_NAME);
  public static final Logger APP = Logger.getLogger("app");
  public Logger getLogger() {
    return APP;
  }
}
