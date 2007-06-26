/**
*   Copyright (c) 2007 Affymetrix, Inc.
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

import com.affymetrix.igb.tiers.SimpleAnnotStyle;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;

import com.affymetrix.genoviz.util.ComponentPagePrinter;

import com.affymetrix.igb.event.SymSelectionEvent;
import com.affymetrix.igb.event.SymSelectionListener;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.menuitem.*;
import com.affymetrix.igb.view.*;
import com.affymetrix.igb.prefs.IPlugin;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap.ActionToggler;
import com.affymetrix.igb.tiers.IAnnotStyleExtended;
import com.affymetrix.igb.util.EPSWriter;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.util.UnibrowAuthenticator;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.util.WebBrowserControl;
import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.swing.DisplayUtils;

/**
 *  Main class for Genotyping Console browser.
 */
public class GTCBrowser extends Application implements ActionListener {

  public static boolean REPLACE_REPAINT_MANAGER = false;

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static String[] main_args;
  static Map comp2window = new HashMap(); // Maps Component -> Frame
  Map comp2plugin = new HashMap(); // Maps Component -> PluginInfo
  Map comp2menu_item = new HashMap(); // Component -> JCheckBoxMenuItem

  JFrame frm;
  JMenuBar mbar;
  JMenu file_menu;
  JMenu export_to_file_menu;
  JMenu view_menu;
  JMenu tools_menu;
  JMenu help_menu;
  JTabbedPane tab_pane;
  JSplitPane splitpane;

  LoadFileAction open_file_action;

  JMenuItem about_item;
  JMenuItem documentation_item;
  JMenuItem console_item;

  JMenuItem clear_item;
  JMenuItem clear_graphs_item;

  JMenuItem open_file_item;
  JMenuItem print_item;
  JMenuItem print_frame_item;
  JMenuItem export_map_item;
  JMenuItem export_labelled_map_item;
  JMenuItem exit_item;

  JMenuItem view_ucsc_item;

  SeqMapView map_view;
  KaryotypeView karyotype_view;
  SimpleGraphTab simple_graph_tab;

  java.util.List plugin_list;


  // USE_STATUS_BAR can be set to false in public releases until we have
  // started putting enough useful information in the status bar.
  final static boolean USE_STATUS_BAR = true;
  StatusBar status_bar;

  static String user_dir = System.getProperty("user.dir");
  static String user_home = System.getProperty("user.home");

  FileTracker load_directory = FileTracker.DATA_DIR_TRACKER;

  boolean initialized = false;

  /**
   * Start the program.
   */
  public static void main(String[] args) {
    try {

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

    System.out.println("Starting \"" + ResourceBundle.getBundle("com/affymetrix/igb/GTCBrowser").getString("app_name") + "\"");
    System.out.println("Version: " + ResourceBundle.getBundle("com/affymetrix/igb/GTCBrowser").getString("app_version"));
    System.out.println();

    main_args = args;

    String quick_load_url = QuickLoadView2.getQuickLoadUrl();
    SynonymLookup dlookup = SynonymLookup.getDefaultLookup();
    LocalUrlCacher.loadSynonyms(dlookup, quick_load_url + "synonyms.txt");

    GTCBrowser singleton_gtcbrowser = new com.affymetrix.igb.GTCBrowser();
    singleton_gtcbrowser.init();

   } catch (Exception e) {
     e.printStackTrace();
     System.exit(1);
   }
  }


  public GTCBrowser() { }

  public SeqMapView getMapView() {
    return map_view;
  }
  
  public KaryotypeView geKaryotypeView() {
    return karyotype_view;
  }

  public JFrame getFrame() { return frm; }

  JPanel tool_bar_container;
//  Component viewer_component = null;
//
//  /** Set the main viewer component, such as a SeqMapView, for example. */
//  public void setViewerComponent(Component c) {
//    viewer_component = c;
//    tool_bar_container.add(BorderLayout.CENTER, c);
//  }
//  
//  public Component getViewerComponent() {
//    return viewer_component;
//  }

  
  protected void init() {
    frm = new JFrame(getApplicationName());
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

    // when HTTP authentication is needed, getPasswordAuthentication will
    //    be called on the authenticator set as the default
    Authenticator.setDefault(new UnibrowAuthenticator(frm));

    frm.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    Image icon = getIcon();
    if (icon != null) { frm.setIconImage(icon); }

    mbar = MenuUtil.getMainMenuBar();
    frm.setJMenuBar(mbar);
    file_menu = MenuUtil.getMenu(getResourceBundle().getString("file_menu"));
    file_menu.setMnemonic('F');

    tools_menu = MenuUtil.getMenu(getResourceBundle().getString("tools_menu"));
    tools_menu.setMnemonic('T');
    
    help_menu = MenuUtil.getMenu(getResourceBundle().getString("help_menu"));
    help_menu.setMnemonic('H');

    tool_bar_container = new JPanel(); // contains tool-bar and map_view
    tool_bar_container.setLayout(new BorderLayout());
    
    map_view = new SeqMapView(true, false, false);    
    karyotype_view = new KaryotypeView();
    JTabbedPane viewers_tab_pane = new JTabbedPane();
    viewers_tab_pane.add(getResourceBundle().getString("seqmap_view_menu"), map_view);
    viewers_tab_pane.add(getResourceBundle().getString("karyotype_view_menu"), karyotype_view);
    

    simple_graph_tab = new SimpleGraphTab(this);


    gmodel.addSeqSelectionListener(map_view);
    gmodel.addGroupSelectionListener(map_view);
    gmodel.addSymSelectionListener(map_view);
    //    gmodel.addSeqModifiedListener(map_view);

    map_view.setFrame(frm);

    open_file_action = new LoadFileAction(map_view, load_directory);
    clear_item = new JMenuItem(getResourceBundle().getString("clear_all_menu"), KeyEvent.VK_C);
    clear_graphs_item = new JMenuItem(getResourceBundle().getString("clear_graphs_menu"), KeyEvent.VK_L);
    open_file_item = new JMenuItem(getResourceBundle().getString("open_file_menu"), KeyEvent.VK_O);
    open_file_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Open16.gif"));
    print_item = new JMenuItem("Print", KeyEvent.VK_P);
    print_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Print16.gif"));
    print_frame_item = new JMenuItem("Print Whole Frame", KeyEvent.VK_F);
    export_to_file_menu = new JMenu("Print to EPS File");
    export_to_file_menu.setMnemonic('T');
    export_map_item = new JMenuItem("Main View", KeyEvent.VK_M);
    export_labelled_map_item = new JMenuItem("Main View (With Labels)", KeyEvent.VK_L);

    exit_item = new JMenuItem(getResourceBundle().getString("exit_menu"), KeyEvent.VK_E);

    view_ucsc_item = new JMenuItem("View Region in UCSC Browser", KeyEvent.VK_R);
    view_ucsc_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/development/WebComponent16.gif"));

    boolean use_hairline_label = false;
    if (map_view.isHairlineLabeled() != use_hairline_label) {
      map_view.toggleHairlineLabel();
    }
    
    MenuUtil.addToMenu(file_menu, open_file_item);
    MenuUtil.addToMenu(file_menu, clear_item);
    MenuUtil.addToMenu(file_menu, clear_graphs_item);
    file_menu.addSeparator();
    MenuUtil.addToMenu(file_menu, print_item);
    file_menu.add(export_to_file_menu);
    MenuUtil.addToMenu(export_to_file_menu, export_map_item);
    MenuUtil.addToMenu(export_to_file_menu, export_labelled_map_item);

    file_menu.addSeparator();
    MenuUtil.addToMenu(file_menu, exit_item);

    // rev_comp option currently not working, so disabled
    JMenu strands_menu = new JMenu("Strands");
    strands_menu.add(new ActionToggler(getMapView().getSeqMap().show_plus_action));
    strands_menu.add(new ActionToggler(getMapView().getSeqMap().show_minus_action));
    strands_menu.add(new ActionToggler(getMapView().getSeqMap().show_mixed_action));

    MenuUtil.addToMenu(tools_menu, view_ucsc_item);
    MenuUtil.addToMenu(tools_menu, new JMenuItem(GTCBrowserActions.makeKaryotypeAction()));


    about_item = new JMenuItem("About " + getApplicationName() + "...", KeyEvent.VK_A);
    about_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/About16.gif"));
    console_item = new JMenuItem("Show Console...", KeyEvent.VK_C);
    console_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/development/Host16.gif"));
    documentation_item = new JMenuItem("Documentation...", KeyEvent.VK_D);
    documentation_item.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Help16.gif"));

    MenuUtil.addToMenu(help_menu, about_item);
    MenuUtil.addToMenu(help_menu, console_item);
    
    about_item.addActionListener(this);
    documentation_item.addActionListener(this);
    console_item.addActionListener(this);
    clear_item.addActionListener(this);
    clear_graphs_item.addActionListener(this);
    open_file_item.addActionListener(this);
    print_item.addActionListener(this);
    print_frame_item.addActionListener(this);
    export_map_item.addActionListener(this);
    export_labelled_map_item.addActionListener(this);
    exit_item.addActionListener(this);

    view_ucsc_item.addActionListener(this);

    MenuUtil.addToMenu(getResourceBundle().getString("graphs_menu"), new JMenuItem(GTCBrowserActions.select_all_graphs_action));
    MenuUtil.addToMenu(getResourceBundle().getString("graphs_menu"), new JMenuItem(GTCBrowserActions.delete_selected_graphs_action));
    MenuUtil.addToMenu(getResourceBundle().getString("graphs_menu"), new JMenuItem(GTCBrowserActions.save_selected_graphs_action));
    MenuUtil.addToMenu(getResourceBundle().getString("graphs_menu"), new JMenuItem(GTCBrowserActions.graph_threshold_action));
    
    JToolBar tool_bar = new JToolBar(JToolBar.HORIZONTAL);
    tool_bar.setFloatable(false);
    tool_bar_container.add(BorderLayout.NORTH, tool_bar);
    tool_bar.add(simple_graph_tab.select_all_graphs_action);
    tool_bar.add(simple_graph_tab.delete_selected_graphs_action);
    tool_bar.add(simple_graph_tab.save_selected_graphs_action);
    tool_bar.add(simple_graph_tab.graph_threshold_action);
    tool_bar.addSeparator();

    tool_bar_container.add(BorderLayout.CENTER, viewers_tab_pane);
    
    Container cpane = frm.getContentPane();
    int table_height = 250;
    int fudge = 55;

    Rectangle frame_bounds = UnibrowPrefsUtil.retrieveWindowLocation(MAIN_WINDOW_PREF_NAME,
        new Rectangle(0, 0, 800, 600));
    UnibrowPrefsUtil.setWindowSize(frm, frame_bounds);

    tab_pane = new JTabbedPane();

    cpane.setLayout(new BorderLayout());
    splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitpane.setOneTouchExpandable(true);
    splitpane.setDividerSize(8);
    splitpane.setDividerLocation(frm.getHeight() - (table_height + fudge));
    splitpane.setTopComponent(tool_bar_container);
    
    splitpane.setBottomComponent(tab_pane);
    
    cpane.add(BorderLayout.CENTER, splitpane);

    // Using JTabbedPane.SCROLL_TAB_LAYOUT makes it impossible to add a
    // pop-up menu (or any other mouse listener) on the tab handles.
    // (A pop-up with "Open tab in a new window" would be nice.)
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4465870
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4499556
    tab_pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    tab_pane.setMinimumSize(new Dimension(0,0));

    status_bar = new StatusBar();
    status_bar.setStatus(getApplicationName());
    if (USE_STATUS_BAR) {
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

    //tab_pane.add("Graphs", simple_graph_tab);
    
    ArrayList plugin_list = new ArrayList(16);

    plugin_list.addAll(getPluginsForGCViewer());

    AnnotBrowserView abv = new AnnotBrowserView();
    //MenuUtil.addToMenu("Tools", new JMenuItem(abv.search_action));

    JFrame frm = new JFrame();
    // We never actually display this frame here, but don't worry,
    // IGB.ensureComponentIsShowing() will display it when needed.
    Container cpane2 = frm.getContentPane();
    cpane2.setLayout(new BorderLayout());
    cpane2.add(BorderLayout.CENTER, abv);
    frm.setSize(new Dimension(400, 400));



    if (plugin_list == null || plugin_list.isEmpty()) {
      System.out.println("There are no plugins specified in preferences.");
    } else {
      Iterator iter = plugin_list.iterator();
      while (iter.hasNext()) {
        PluginInfo pi = (PluginInfo) iter.next();
        setUpPlugIn(pi);
      }
    }

    initialized = true;
  }

  /** Returns true if initialization has completed. */
  public boolean isInitialized() {
    return initialized;
  }

//  public Action detailsAction = new DetailsAction("Selection Details", gmodel);

  class DetailsAction extends AbstractAction implements SymSelectionListener {
    SymTableView stv = new SymTableView();
    final JFrame stv_frm = new JFrame("Selection Details");
    {
      Container cpane = stv_frm.getContentPane();
      cpane.setLayout(new BorderLayout());
      cpane.add("Center", stv);
      stv_frm.setSize(new Dimension(400, 400));
    }
    
    DetailsAction(String s, SingletonGenometryModel gmodel) {
      super(s);
      setEnabled(false);
      gmodel.addSymSelectionListener(this);
      gmodel.addSymSelectionListener(stv);
    }
    public void symSelectionChanged(SymSelectionEvent evt) {
      setEnabled(! evt.getSelectedSyms().isEmpty()); 
    }
    public void actionPerformed(ActionEvent e) {
      stv_frm.setVisible(true);
    }
  };
  
  /**
   *  Puts the given component either in the tab pane or in its own window,
   *  depending on saved user preferences.
   */
  void setUpPlugIn(PluginInfo pi) {

    if (! pi.shouldLoad()) return;

    String class_name = pi.getClassName();
    if (class_name == null || class_name.trim().length()==0) {
      ErrorHandler.errorPanel("Bad Plugin",
        "Cannot create plugin '"+pi.getPluginName()+"' because it has no class name.",
        this.frm);
      PluginInfo.getNodeForName(pi.getPluginName()).putBoolean("load", false);
      return;
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
      return;
    }

    ImageIcon icon = null;

    if (plugin instanceof IPlugin) {
      IPlugin plugin_view = (IPlugin) plugin;
      icon = (ImageIcon) plugin_view.getPluginProperty(IPlugin.TEXT_KEY_ICON);

      plugin_view.putPluginProperty(IPlugin.TEXT_KEY_IGB, this);
      plugin_view.putPluginProperty(IPlugin.TEXT_KEY_SEQ_MAP_VIEW, map_view);
      // An alternative to having IPlugin interface is checking for
      // these other interfaces ....
      //    if (plugin instanceof SymSelectionListener) { }
      //    if (plugin instanceof SeqSelectionListener) { }
      //    if (plugin instanceof GroupSelectionListener) { }

      // ... or plugins that need to know about SeqMapView or other components accessible via
      //     IGB class should access them via IGB singleton method calls
      //     and can add themselves as listeners for various events in their constructor ...
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
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == open_file_item) {
      open_file_action.actionPerformed(evt);
    }
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
    else if (src == view_ucsc_item) {
      map_view.invokeUcscView();
    }
    else if (src == about_item) {
      showAboutDialog();
    } else if (src == documentation_item) {
      showDocumentationDialog();
    }
    else if (src == console_item) {
      ConsoleView.showConsole();
    }
  }

  public void showAboutDialog() {
    JPanel message_pane = new JPanel();
    message_pane.setLayout(new BoxLayout(message_pane, BoxLayout.Y_AXIS));
    JTextArea about_text = new JTextArea();
    about_text.append(getApplicationName() + ", version: " + getVersion() + "\n");
    about_text.append("Copyright 2001-2007 Affymetrix Inc." + "\n");
    about_text.append("\n");
    about_text.append(getApplicationName() + " uses the Xerces\n");
    about_text.append("package from the Apache Software Foundation, \n");
    about_text.append("the Fusion SDK from Affymetrix,  \n");
    about_text.append("and the Vector Graphics package from java.FreeHEP.org \n");
    about_text.append("(released under the LGPL license).\n");
    about_text.append(" \n");

    String data_dir = UnibrowPrefsUtil.getAppDataDirectory();
    if (data_dir != null) {
      File data_dir_f = new File(data_dir);
      about_text.append("\nApplication data and cache stored in: \n  "+
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
    JPanel buttonP = new JPanel(new GridLayout(3,1));
    buttonP.add(licenseB);
    buttonP.add(apacheB);
    buttonP.add(freehepB);
    buttonP.add(fusionB);
    message_pane.add(buttonP);

    final JOptionPane pane = new JOptionPane(message_pane, JOptionPane.INFORMATION_MESSAGE,
     JOptionPane.DEFAULT_OPTION);
    final JDialog dialog = pane.createDialog(frm, "About " + getApplicationName());
    //dialog.setResizable(true);
    dialog.setVisible(true);
  }

  public void showDocumentationDialog() {
    JPanel message_pane = new JPanel();
    message_pane.setLayout(new BoxLayout(message_pane, BoxLayout.Y_AXIS));
    JTextArea about_text = new JTextArea();
    about_text.append(DocumentationView.getDocumentationText());
    message_pane.add(new JScrollPane(about_text));

    JButton affyB = new JButton("Go To IGB at Affymetrix");
    JButton sfB = new JButton("Go To IGB at SourceForge");
    affyB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          WebBrowserControl.displayURL("http://www.affymetrix.com/support/developer/tools/download_igb.affx");
        }
      } );
    sfB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          WebBrowserControl.displayURL("http://sourceforge.net/projects/genoviz/");
        }
      } );
    Box buttonP = Box.createHorizontalBox();
    buttonP.add(Box.createHorizontalGlue());
    buttonP.add(affyB);
    buttonP.add(Box.createHorizontalGlue());
    buttonP.add(sfB);
    buttonP.add(Box.createHorizontalGlue());
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
      URL url = GTCBrowser.class.getResource("trash-icon.png");
      if (url != null) {
        icon = Toolkit.getDefaultToolkit().getImage(url);
      }
    } catch (Exception e) {
      // It isn't a big deal if we can't find the icon, just return null
    }
    return icon;
  }

  private void exit() {
    boolean ask_before_exit = false;
    String message = "Really exit?";
    if ( (! ask_before_exit) || confirmPanel(message)) {
      saveWindowLocations();
      System.exit(0);
    }
  }

  
  static final String MAIN_WINDOW_PREF_NAME = "GTCBrowser main window";

  /**
   * Saves information about which plugins are in separate windows and
   * what their preferred sizes are.
   */
  private void saveWindowLocations() {
    // Save the main window location
    UnibrowPrefsUtil.saveWindowLocation(frm, MAIN_WINDOW_PREF_NAME);

    Iterator iter = comp2plugin.keySet().iterator();
    while (iter.hasNext()) {
      Component comp = (Component) iter.next();
      PluginInfo pi = (PluginInfo) comp2plugin.get(comp);
      Frame f = (Frame) comp2window.get(comp);
      if (f != null) {
        UnibrowPrefsUtil.saveWindowLocation(f, pi.getPluginName());
      }
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
  
  void addToPopupWindows(final JComponent comp, final String title) {
    JCheckBoxMenuItem popupMI = new JCheckBoxMenuItem(title);
    //popup_windowsM.add(popupMI);
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
    //popup_windowsM.add(new JCheckBoxMenuItem("foo"));
  }
  
  java.util.List getPluginsForGCViewer() {
    ArrayList plugin_list = new ArrayList(16);

    plugin_list.add(new PluginInfo(GCViewerBottomView.class.getName(), "Data", true));
    //plugin_list.add(new PluginInfo(SimpleGraphTab.class.getName(), "Graph Adjuster", true));
    plugin_list.add(new PluginInfo(SymTableView.class.getName(), "Selections", true));

    return plugin_list;
  }

  final Map styles = new HashMap();
  
  public IAnnotStyleExtended getStyleForMethod(String meth, boolean is_graph) {
    IAnnotStyleExtended style = (IAnnotStyleExtended) styles.get(meth);
    if (style == null) {
      style = new SimpleAnnotStyle(meth, false);
      styles.put(meth, style);
    }
    return style;
  }

  public String getApplicationName() {
    return getResourceBundle().getString("app_name");
  }
  
  public String getVersion() {
    return getResourceBundle().getString("app_version");
  }

  public ResourceBundle getResourceBundle() {
    return ResourceBundle.getBundle("com/affymetrix/igb/GTCBrowser");
  }
  
  public void setBookmarkManager(Object o) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
