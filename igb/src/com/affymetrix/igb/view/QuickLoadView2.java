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
package com.affymetrix.igb.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.prefs.*;
import javax.swing.*;
import java.net.URL;
import java.util.regex.Pattern;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.util.*;

import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.GeneralBioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.event.*;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.das.DasDiscovery;
import com.affymetrix.igb.event.*;
import com.affymetrix.igb.util.*;
import com.affymetrix.genometryImpl.parsers.LiftParser;
import com.affymetrix.genometryImpl.parsers.ChromInfoParser;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.igb.menuitem.OpenGraphAction;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;
import com.affymetrix.igb.prefs.PreferencesPanel;

public class QuickLoadView2 extends JComponent
         implements ItemListener, ActionListener, GroupSelectionListener, SeqSelectionListener  {

  static boolean DEBUG_EVENTS = false;
  static public boolean build_virtual_genome = true;
  static public boolean build_virtual_encode = true;
  // hardwiring names for genome and encode virtual seqs, need to generalize this soon
  static public String GENOME_SEQ_ID = "genome";
  static public String ENCODE_REGIONS_ID = "encode_regions";


  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static final String DEFAULT_QUICKLOAD_URL = "http://netaffxdas.affymetrix.com/quickload_data/";

  static final String SELECT_A_GENOME = "Select a genome";
  static final String SELECT_A_SERVER = "Select a server";

  // Names for the server selection box
  static final String SERVER_NAME_DEFAULT = "NetAffx";
  static final String SERVER_NAME_USER_DEFINED = "Personal";
  static final String SERVER_NAME_FROM_IGB_PREFS_FILE = "Auxiliary";

  // constants for remembering state
  public static final String PREF_LAST_QUICKLOAD_URL = "QuickLoad: Last URL";
  public static final String PREF_LAST_SERVER_NAME = "QuickLoad: Last Server";
  public static final String PREF_LAST_GENOME = "QuickLoad: Last Genome";
  public static final String PREF_LAST_SEQ = "QuickLoad: Last Seq Name";

  public static final String PREF_USER_DEFINED_QUICKLOAD_URL = "QuickLoad URL";


  static boolean LOAD_DEFAULT_ANNOTS = true;
  static Map default_types = new HashMap();
  static String default_annot_name  = "refseq";

  JComboBox serverCB;
  JComboBox genomeCB;
  JPanel types_panel;
  QuickLoadServerModel current_server;
  AnnotatedSeqGroup current_group;
  String current_genome_name;
  AnnotatedBioSeq current_seq;
  Map cb2filename = new HashMap();
  SeqMapView gviewer;

  JButton all_residuesB;
  JButton partial_residuesB;
  JButton optionsB;
  DataLoadPrefsView optionsP;

  int pref_tab_number = -1;

  boolean auto_select_first_seq_in_group = true;

  static {
    default_types.put(default_annot_name, default_annot_name);
    default_types.put("cytoBand", "cytoBand");
  }

  public QuickLoadView2() {

    PreferencesPanel pp = PreferencesPanel.getSingleton();
    pref_tab_number = pp.addPrefEditorComponent(new DataLoadPrefsView());
    UnibrowPrefsUtil.getLocationsNode().addPreferenceChangeListener(preference_change_listener);

    if (Application.getSingleton() != null) {
      gviewer = Application.getSingleton().getMapView();
    }
    this.setLayout(new BorderLayout());
    types_panel = new JPanel();
    types_panel.setLayout(new BoxLayout(types_panel, BoxLayout.Y_AXIS));

    JPanel choice_panel = new JPanel();
    choice_panel.setLayout(new BoxLayout(choice_panel, BoxLayout.X_AXIS));
    choice_panel.setBorder(BorderFactory.createEmptyBorder(2,4,4,4));

    serverCB = new JComboBox();
    choice_panel.add(new JLabel("Server:"));
    choice_panel.add(Box.createHorizontalStrut(5));
    choice_panel.add(serverCB);
    choice_panel.add(Box.createHorizontalStrut(20));

    genomeCB = new JComboBox();
    choice_panel.add(new JLabel("Genome:"));
    choice_panel.add(Box.createHorizontalStrut(5));
    choice_panel.add(genomeCB);
    choice_panel.add(Box.createHorizontalGlue());

    JPanel buttonP = new JPanel();
    buttonP.setLayout(new GridLayout(1, 3));
    if (IGB.isSequenceAccessible()) {
      all_residuesB = new JButton("Load All Sequence");
      all_residuesB.addActionListener(this);
      buttonP.add(all_residuesB);
      partial_residuesB = new JButton("Load Sequence in View");
      if (IGB.ALLOW_PARTIAL_SEQ_LOADING) {
        partial_residuesB.addActionListener(this);
        buttonP.add(partial_residuesB);
      }
    }
    else {
    buttonP.add(Box.createRigidArea(new Dimension(5,0)));
      buttonP.add(new JLabel("No sequence available", JLabel.CENTER));
    }
    optionsB = new JButton("QuickLoad Options");
    buttonP.add(optionsB);
    buttonP.setBorder(BorderFactory.createEmptyBorder(4,2,2,2));
    optionsB.addActionListener(this);

    this.add("North", choice_panel);
    this.add("Center", new JScrollPane(types_panel));
    this.add("South", buttonP);
    this.setBorder(BorderFactory.createEtchedBorder());

    gmodel.addGroupSelectionListener(this);
    gmodel.addSeqSelectionListener(this);

    // Prototypes can be used to set the "preferred" display size
    //serverCB.setPrototypeDisplayValue("XXXX");
    //genomeCB.setPrototypeDisplayValue("XXXX");
    genomeCB.addItem(SELECT_A_GENOME);

    initializeQLServerList();

    serverCB.addItemListener(this);
    genomeCB.addItemListener(this);
  }

  public void actionPerformed(ActionEvent evt)  {
    Object src = evt.getSource();
    /* handles residues loading based on partial or full sequence load buttons */
    if (src == partial_residuesB) {
      SeqSpan viewspan = gviewer.getVisibleSpan();
      if (current_group==null) { ErrorHandler.errorPanel("Error", "No sequence group selected.", gviewer); }
      else if (current_seq==null) { ErrorHandler.errorPanel("Error", "No sequence selected.", gviewer); }
      else if (viewspan.getBioSeq() != current_seq) {
        System.err.println("Error in QuickLoaderView: " +
                           "SeqMapView seq and QuickLoaderView current_seq not the same!");
      } else {
	SeqResiduesLoader.loadPartialResidues(viewspan, current_group);
      }
    }
    else if (src == all_residuesB) {
      if (current_group==null) { ErrorHandler.errorPanel("Error", "No sequence group selected.", gviewer); }
      else if (current_seq==null) { ErrorHandler.errorPanel("Error", "No sequence selected.", gviewer); }
      if (! (current_seq instanceof SmartAnnotBioSeq)) {
	ErrorHandler.errorPanel("Error", "Can't do optimized full residues retrieval for this sequence.", gviewer); 
      }
      else {
	SeqResiduesLoader.loadAllResidues((SmartAnnotBioSeq)current_seq);
      }
    }
    else if (src == optionsB) {
      showOptions();
    }
    else if (src instanceof JCheckBox) {  // must put this after cache modification branch, since some of those are JCheckBoxes
      if (DEBUG_EVENTS)  { System.out.println("QuickLoadView2 received annotation load action"); }
      JCheckBox cbox = (JCheckBox)src;
      String filename = (String)cb2filename.get(cbox);
      boolean selected = cbox.isSelected();
      // probably need to make this threaded (see QuickLoaderView)
      if (selected)  {
        current_server.loadAnnotations(current_group, filename);
        gviewer.setAnnotatedSeq(gmodel.getSelectedSeq(), true, true, false);
        boolean loaded = QuickLoadServerModel.getLoadState(current_group, filename);
        cbox.setEnabled(! loaded);
        cbox.setSelected(loaded);
        cbox.setText(getCheckboxTitle(loaded, filename));
      }
      else {
        // never happens.  We don't allow the checkbox to be un-selected
      }
    }
  }

  public void itemStateChanged(ItemEvent evt) {
    Object src = evt.getSource();
    if (DEBUG_EVENTS)  {
      System.out.println("####### QuickLoadView2 received itemStateChanged event: " + evt);
    }

    try {

    if ((src == serverCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      String selection = (String) serverCB.getSelectedItem();

      current_server = getQLModelForName(selection);
      if (current_server != null && current_server.getRootUrl() != null) {
        UnibrowPrefsUtil.getLocationsNode().put(PREF_LAST_QUICKLOAD_URL, current_server.getRootUrl());
        UnibrowPrefsUtil.getLocationsNode().put(PREF_LAST_SERVER_NAME, selection);
      }

      refreshGenomeChoices();

      // optional: try to set back to the same old group and sequence on the new server
      String old_group_id = UnibrowPrefsUtil.getLocationsNode().get(PREF_LAST_GENOME, null);
      String old_seq_id = UnibrowPrefsUtil.getLocationsNode().get(PREF_LAST_SEQ, null);

      auto_select_first_seq_in_group = false;
        // Don't let the group selection trigger an automatic seq selection
        // because that could happen during start-up and would always force the breif display
        // of chr1 (or whatever chr is first in the group) before going to the old_group_id.
      AnnotatedSeqGroup group = gmodel.getSeqGroup(old_group_id);
      gmodel.setSelectedSeqGroup(group); // causes a GroupSelectionEvent
      if (group != null && group == gmodel.getSelectedSeqGroup()) {
        MutableAnnotatedBioSeq seq = group.getSeq(old_seq_id);
        if (seq != null) {
          gmodel.setSelectedSeq(seq); // causes a SeqSelectionEvent
        } else {
          if (group.getSeqCount() > 0) {
            gmodel.setSelectedSeq(group.getSeq(0));
          }
        }
      }
      types_panel.invalidate(); // make sure display gets updated
      auto_select_first_seq_in_group = true;
    }

    else if ((src == genomeCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      String genome_name = (String)genomeCB.getSelectedItem();
      if (DEBUG_EVENTS) { System.out.println("Selected genome: " + genome_name); }

      if (genome_name.equals(SELECT_A_GENOME)) {
        current_genome_name = genome_name;
        gmodel.setSelectedSeq(null);
        gmodel.setSelectedSeqGroup(null);
      } else {
        AnnotatedSeqGroup group = gmodel.getSeqGroup(genome_name);
        if (gmodel.getSelectedSeqGroup() != group) {
          // need to initialize genome before setting it as selected seq group, in
          //    case it hasn't been seen before
          current_genome_name = genome_name;
          current_server.initGenome(genome_name);
          // calling gmodel.setSelectedSeqGroup() should also bounce event back to this.groupSelectionChanged()
          gmodel.setSelectedSeq(null);
          gmodel.setSelectedSeqGroup(group);
        }

        if (group != null) {
          UnibrowPrefsUtil.getLocationsNode().put(PREF_LAST_GENOME, group.getID());
        }
      }
    }

    } catch (Throwable t) {
      // some out-of-memory errors could happen during this code, so
      // this catch block will report that to the user.
      ErrorHandler.errorPanel("Error ", t);
    }
  }

  public String getCheckboxTitle(boolean prev_loaded, String filename) {
    String annot_name = getAnnotName(filename);
    String checkbox_title = annot_name;

//    // unfortunately, this code slows things down too much....
//    if (prev_loaded) {
//      checkbox_title = annot_name + " [Loaded]";
//    } else {
//      String full_name = current_server.root_url + current_genome_name + "/" + filename;
//      // We would like to use the LocalUrlCacher.getPreferredCacheUsage(), but
//      // that would slow down the startup considerably.
//      // Using ONLY_CACHE still lets us tell the difference between a cached file
//      // and a local one, but doesn't know if the cache is stale or if the remote
//      // file is unavailable
//      String load_type = LocalUrlCacher.getLoadType(full_name, LocalUrlCacher.ONLY_CACHE);
//      checkbox_title = annot_name + " [" + load_type + "]";
//    }

    return checkbox_title;
  }

  String getAnnotName(String filename) {
    int pindex = filename.indexOf('.');
    if (pindex < 0)  { return filename; }
    return filename.substring(0, pindex);
  }

  public void groupSelectionChanged(GroupSelectionEvent evt) {
    // Implementation of GroupSelectionListener
    //  This gets called when something external, such as a bookmark, causes
    //  the genome to change.  Internally, when the genome combo box is changed,
    //  that causes a call to SingletonGenomeModel.setSelectedSeqGroup(), and that
    //  causes a call to here.

    AnnotatedSeqGroup group = evt.getSelectedGroup();
    if (DEBUG_EVENTS) { System.out.println("QuickLoadView2.groupSelectionChanged() called, group: " + (group == null ? null : group.getID())); }
    if (current_group != group) {
      cb2filename.clear();
      Component[] comps = types_panel.getComponents();
      for (int i=0; i<comps.length; i++) {
        if (comps[i] instanceof JCheckBox) {
          JCheckBox cb = (JCheckBox)comps[i];
          // really shouldn't need to do this, since old checkboxes should get gc'd, but just to make sure...
          cb.removeActionListener(this);
        }
      }
      types_panel.removeAll();

      current_group = group;
      if (DEBUG_EVENTS)  { System.out.println("current server: " + current_server); }

      if (current_group == null || current_server == null) {
        current_genome_name = null;
      } else {
        current_genome_name = current_server.getGenomeName(group);
      }

      if (current_genome_name == null) {
        // if no genome in quickload server matches selected AnnotatedSeqGroup,
        // then clear the types_panel and un-select the item in the genomeCB
        types_panel.add(new JLabel("The selected genome is not included in this QuickLoad server."));
        genomeCB.setSelectedIndex(-1);
      }
      else {
        current_server.initGenome(current_genome_name);
        genomeCB.setSelectedItem(current_genome_name);

        List file_names = current_server.getFilenames(current_genome_name);
        Iterator iter = file_names.iterator();

        // populate list of checkboxes for annotation types
        while (iter.hasNext()) {
          String filename = (String) iter.next();

          if (filename == null || filename.equals(""))  { continue; }
          boolean prev_loaded = QuickLoadServerModel.getLoadState(group, filename);

          String checkbox_title = getCheckboxTitle(prev_loaded, filename);
          JCheckBox cb = new JCheckBox(checkbox_title);

          cb2filename.put(cb, filename);
          cb.setSelected(prev_loaded);
          cb.setEnabled(! prev_loaded);
          cb.addActionListener(this);
          types_panel.add(cb);
        }

        if (LOAD_DEFAULT_ANNOTS) {
          // if any of the checkboxes correspond to default types, then try to load them
          Iterator cbiter = cb2filename.keySet().iterator();
          while (cbiter.hasNext()) {
            JCheckBox cb = (JCheckBox) cbiter.next();
            String filename = (String) cb2filename.get(cb);

            boolean prev_loaded = QuickLoadServerModel.getLoadState(group, filename);
            boolean is_a_default_type = (default_types.get(getAnnotName(filename)) != null);

            if ((! prev_loaded) && is_a_default_type) {
              //            cb.setSelected(true);  // rely on checkbox selection event to trigger loading...
              current_server.loadAnnotations(current_group, filename);
              boolean loaded = QuickLoadServerModel.getLoadState(current_group, filename);
              cb.setEnabled(! loaded);
              cb.setSelected(loaded);
              cb.setText(getCheckboxTitle(loaded, filename));

              if (auto_select_first_seq_in_group) {
                AnnotatedBioSeq seq = gmodel.getSelectedSeq();
                if (seq == null && current_group.getSeqCount() > 0) {
                  seq =  current_group.getSeq(0);
                }
                if (seq != null) {
                  gviewer.setAnnotatedSeq(seq, true, true, false);
                }
              }
            }
          }
        }

      }
    }
    types_panel.invalidate(); // make sure display gets updated (even if this is the same group as before.)
    types_panel.repaint();
  }

  public void seqSelectionChanged(SeqSelectionEvent evt) {
    if (DEBUG_EVENTS) { System.out.println("QuickLoadView2.seqSelectionChanged() called"); }
    current_seq = evt.getSelectedSeq();
    if (current_seq != null) {
        UnibrowPrefsUtil.getLocationsNode().put(PREF_LAST_SEQ, current_seq.getID());
    }
    if (current_seq != null
        && ! ENCODE_REGIONS_ID.equals(current_seq.getID())
        && ! GENOME_SEQ_ID.equals(current_seq.getID())) {
      all_residuesB.setEnabled(true);
      partial_residuesB.setEnabled(true);
    }
    else {
      all_residuesB.setEnabled(false);
      partial_residuesB.setEnabled(false);
    }
  }


  void refreshGenomeChoices() {
    genomeCB.removeItemListener(this);
    genomeCB.removeAllItems();
    genomeCB.addItem(SELECT_A_GENOME);

    if (current_server != null) {
      Iterator genome_names = current_server.getGenomeNames().iterator();
      while (genome_names.hasNext()) {
        String genome_name = (String) genome_names.next();
        //AnnotatedSeqGroup group = current_server.getSeqGroup(genome_name);
        genomeCB.addItem(genome_name);
      }
    }
    genomeCB.setSelectedIndex(-1); // deselect everything, so later selection will send event
    genomeCB.addItemListener(this);
    genomeCB.setSelectedItem(SELECT_A_GENOME);
  }

  void initializeQLServerList() {
    serverCB.addItem(SELECT_A_SERVER);

    serverCB.addItem(SERVER_NAME_DEFAULT);
    serverCB.addItem(SERVER_NAME_USER_DEFINED);

    // Add a choice for the server from the prefs file only if it is not the
    // same server as the Default one or the user-defined one
    String url_from_prefs_file = getUrlFromPrefsFile();
    if (url_from_prefs_file != null && url_from_prefs_file.trim().length() > 0) {
      if ((! compareURLs(url_from_prefs_file, DEFAULT_QUICKLOAD_URL)) &&
          (! compareURLs(url_from_prefs_file, getUrlFromPersistentPrefs()))) {
        serverCB.addItem(SERVER_NAME_FROM_IGB_PREFS_FILE);
      }
    }

    serverCB.setSelectedItem(SELECT_A_SERVER);
  }

  static boolean compareURLs(String url1, String url2) {
    if (url1==url2) {return true;}
    if (url1==null || url2==null) {return false;}

    url1 = url1.toLowerCase().trim();
    url2 = url2.toLowerCase().trim();
    while (url1.endsWith("/")) { url1 = url1.substring(0, url1.length()-1); }
    while (url2.endsWith("/")) { url2 = url2.substring(0, url2.length()-1); }
    return url1.equals(url2);
  }

  // this one is always the hard-coded netaffx server
  static String getUrlDefault() {
    return DEFAULT_QUICKLOAD_URL;
  }

  // this one comes from persistent prefs system, this is the one set in the QuickLoad options
  static String getUrlFromPersistentPrefs() {
    return UnibrowPrefsUtil.getLocation(PREF_USER_DEFINED_QUICKLOAD_URL, "");
  }

  // this one is set by the user in the igb_prefs file.  It can be null
  // we sort-of hope to phase this out
  static String getUrlFromPrefsFile() {
     return (String) IGB.getIGBPrefs().get("QuickLoadUrl");
  }

  static String getUrlLastUsed() {
    String url = UnibrowPrefsUtil.getLocationsNode().get(PREF_LAST_QUICKLOAD_URL, DEFAULT_QUICKLOAD_URL);
    if ("".equals(url)) { url = DEFAULT_QUICKLOAD_URL; }
    return url;
  }

  static String getLastServerName() {
    return UnibrowPrefsUtil.getLocationsNode().get(PREF_LAST_SERVER_NAME, SERVER_NAME_DEFAULT);
  }

  // equivalent to getURLLastUsed()
  // modified to first look if one was specified in the igb_prefs.xml
  public static String getQuickLoadUrl() {
	String u = getUrlFromPrefsFile();
	if (u != null) return u;
    return getUrlLastUsed();
  }

  /**
   *   Optionally call this after IGB has been set-up.
   *   Resets the server to the one that was in use the last time the program
   *   was shut down.
   */
  public void initialize() {
    //    System.out.println("##### called QuickLoadView2.intialize()");
    // tries to reset the quickload server back to the one used when the program last shut-down
    // Must be done only after IGB has finished initializing, so the SeqMapView is ready
    // Run on the Swing Thread, so we are sure necessary initialization is finished

    String last_name = getLastServerName();
    setSelectedServerEventually(last_name);
  }

  void setSelectedServerEventually(final String last_name) {
    final JComboBox combo_box = serverCB;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          combo_box.setSelectedItem(last_name);
        } catch (Exception e) {
          // no exceptions are expected, but if there are any, ignore them.
          System.out.println("WARNING: Problem resetting to last QL server: " + e.toString());
          e.printStackTrace();
        }
      }
    });
  }

  public DataLoadPrefsView getOptionsPanel() {
    if (optionsP == null) {
      optionsP = new DataLoadPrefsView();
    }
    return optionsP;
  }

  public void showOptions() {
    PreferencesPanel pv = PreferencesPanel.getSingleton();
    pv.setTab(pref_tab_number);
    JFrame f = pv.getFrame();
    f.setVisible(true);
  }

  PreferenceChangeListener preference_change_listener = new PreferenceChangeListener() {
    public void preferenceChange(PreferenceChangeEvent evt) {
      String value = evt.getNewValue();
      if (evt.getKey().equals(PREF_USER_DEFINED_QUICKLOAD_URL)) {
        // If the user is currently looking at the user-defined QL server, must force it to reload
        if (SERVER_NAME_USER_DEFINED.equals(serverCB.getSelectedItem())) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              serverCB.setSelectedIndex(-1); // deselect everything, so later selection will send event
              serverCB.setSelectedItem(SERVER_NAME_USER_DEFINED);
            }
          });
        }
      }
    }
  };

  /** Adds the DAS servers from the file on the quickload server to the
   *  persistent list managed by DasDiscovery.  If the file doesn't exist,
   *  or can't be loaded, a warning is printed to stdout, but that is all,
   *  since it isn't a fatal error.
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

  // This is used when the user selects a server name from the combo box
  static QuickLoadServerModel getQLModelForName(String ql_server_name) {
    String the_url_string = null;
    if (SERVER_NAME_FROM_IGB_PREFS_FILE.equals(ql_server_name)) {
      the_url_string = getUrlFromPrefsFile();
    } else if (SERVER_NAME_USER_DEFINED.equals(ql_server_name)) {
      the_url_string = getUrlFromPersistentPrefs();
    } else if (SERVER_NAME_DEFAULT.equals(ql_server_name)) {
      the_url_string = getUrlDefault();
    } else if (SELECT_A_SERVER.equals(ql_server_name)) {
      the_url_string = null;
    }

    QuickLoadServerModel result = null;
    if (the_url_string != null && ! "".equals(the_url_string.trim())) try {
      URL the_url = new URL(the_url_string);
      result = QuickLoadServerModel.getQLModelForURL(gmodel, the_url);

      if (result != null) {
        LocalUrlCacher.loadSynonyms(SynonymLookup.getDefaultLookup(), result.getRootUrl()+"synonyms.txt");
      }
    } catch (Exception e) {
      ErrorHandler.errorPanel("ERROR", "Error opening QuickLoad server:\n server='"+the_url_string+"'"
       + "\n" + e.toString());
    }

    if (result != null) {
      processDasServersList(result.getRootUrl());
    }

    return result;
  }
}

