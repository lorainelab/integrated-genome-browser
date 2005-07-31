package com.affymetrix.igb.view;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import java.util.regex.Pattern;

import com.affymetrix.igb.event.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.util.*;

public class QuickLoadView2 extends JComponent
         implements ItemListener, GroupSelectionListener, SeqSelectionListener  {

  static boolean DEBUG_EVENTS = true;
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static final String DEFAULT_QUICKLOAD_URL = "http://netaffxdas.affymetrix.com/quickload_data/";
  static final String DEFUNCT_SERVER = "205.217.46.81";
  static final String SELECT_A_GENOME = "Select a genome to load";
  static Pattern tab_regex = Pattern.compile("\t");

  public static final String PREF_QUICKLOAD_URL = "QuickLoad URL";
  public static final String PREF_QUICKLOAD_CACHE_USAGE = "quickload_cache_usage";
  public static final String PREF_QUICKLOAD_CACHE_RESIDUES = "quickload_cache_residues";
  public static final String PREF_QUICKLOAD_CACHE_ANNOTS = "quickload_cache_annots";

  static int CACHE_USAGE_DEFAULT = LocalUrlCacher.NORMAL_CACHE;
  static boolean CACHE_RESIDUES_DEFAULT = false;
  static boolean CACHE_ANNOTS_DEFAULT = true;
  static boolean LOAD_DEFAULT_ANNOTS = true;

  JComboBox serverCB;
  JComboBox genomeCB;
  JPanel types_panel;
  String http_root;
  Map name2group = new LinkedHashMap();  // maps genome "name" from quickload contents.txt file to AnnotatedSeqGroup

  int cache_usage =
    UnibrowPrefsUtil.getIntParam(PREF_QUICKLOAD_CACHE_USAGE, CACHE_USAGE_DEFAULT);
  boolean cache_residues =
    UnibrowPrefsUtil.getBooleanParam(PREF_QUICKLOAD_CACHE_RESIDUES, CACHE_RESIDUES_DEFAULT);
  boolean cache_annots =
    UnibrowPrefsUtil.getBooleanParam(PREF_QUICKLOAD_CACHE_ANNOTS, CACHE_ANNOTS_DEFAULT);

  public QuickLoadView2() {
    this.setLayout(new BorderLayout());
    JPanel choice_panel = new JPanel();
    types_panel = new JPanel();
    types_panel.setLayout(new BoxLayout(types_panel, BoxLayout.Y_AXIS));

    choice_panel.setLayout(new GridLayout(2,2));
    serverCB = new JComboBox();
    serverCB.addItem("test");
    genomeCB = new JComboBox();
    genomeCB.addItem("genome test");
    choice_panel.add(new JLabel("Quickload Server:"));
    choice_panel.add(serverCB);
    choice_panel.add(new JLabel("Quickload Genome:"));
    choice_panel.add(genomeCB);
    JPanel panel2 = new JPanel(new BorderLayout());
    panel2.add("West", choice_panel);
    this.add("North", panel2);
    this.add("Center", new JScrollPane(types_panel));
    //    this.add("North", choice_panel);
    gmodel.addGroupSelectionListener(this);
    gmodel.addSeqSelectionListener(this);
    serverCB.addItemListener(this);
    genomeCB.addItemListener(this);

    http_root = getQuickLoadUrl();
    System.out.println("Setting QuickLoad server: " + http_root);
    loadGenomeNames();
    refreshGenomeChoices();

  }

  public void itemStateChanged(ItemEvent evt) {
    Object src = evt.getSource();
    if (src == serverCB) {

    }
    else if ((src == genomeCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      String genome_name = (String)genomeCB.getSelectedItem();
      if (! genome_name.equals(SELECT_A_GENOME)) {
	AnnotatedSeqGroup group = gmodel.getSeqGroup(genome_name);
	if (gmodel.getSelectedSeqGroup() != group) {
	  System.out.println("genome selection changed");
	  gmodel.setSelectedSeqGroup(group);
	  gmodel.setSelectedSeq(null);
	}
      }
    }
  }

  public void groupSelectionChanged(GroupSelectionEvent evt) {
    java.util.List glist = evt.getSelectedGroups();
    AnnotatedSeqGroup group = null;
    if (glist.size() > 0)  { group = (AnnotatedSeqGroup)glist.get(0); }
    if (DEBUG_EVENTS) { System.out.println("QuickLoadView2.groupSelectionChanged() called, group: " + group.getID()); }
    if (group == null)  { return; }

  }

  public void seqSelectionChanged(SeqSelectionEvent evt) {
    if (DEBUG_EVENTS) { System.out.println("QuickLoadView2.seqSelectionChanged() called"); }
  }

  public java.util.List loadGenomeNames() {
    ArrayList glist = null;
    try {
      InputStream istr = null;
      try {
        istr = LocalUrlCacher.getInputStream(http_root + "contents.txt", cache_usage, cache_annots);
      } catch (Exception e) {
        istr = null; // dealt with below
      }
      if (istr == null) {
        System.out.println("Could not load contents.txt file from\n" + http_root + "contents.txt");
        return Collections.EMPTY_LIST;
      }
      InputStreamReader ireader = new InputStreamReader(istr);
      BufferedReader br = new BufferedReader(ireader);
      String line;
      glist = new ArrayList();
      while ((line = br.readLine()) != null) {
	AnnotatedSeqGroup group = null;
	String[] fields = tab_regex.split(line);
	if (fields.length >= 1) {
	  String genome_name = fields[0];
	  glist.add(genome_name);
	  group = gmodel.addSeqGroup(genome_name);  // returns existing group if found, otherwise creates a new group
	  name2group.put(genome_name, group);
	  // System.out.println("added genome, name = " + line + ", group = " + group.getID() + ", " + group);
	}
	// if quickload server has description, and group is new or doesn't yet have description, add description to group
	if ((fields.length >= 2) && (group.getDescription() == null)) {
	  group.setDescription(fields[1]);
	}
      }
      istr.close();
      ireader.close();
      br.close();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    return glist;
  }


  void refreshGenomeChoices() {
    genomeCB.removeItemListener(this);
    genomeCB.removeAllItems();
    genomeCB.addItem(SELECT_A_GENOME);

    SingletonGenometryModel model = SingletonGenometryModel.getGenometryModel();
    //    Map groups = model.getSeqGroups();
    //    Iterator group_names = model.getSeqGroups().keySet().iterator();
    Iterator genome_names = name2group.keySet().iterator();
    while (genome_names.hasNext()) {
      String genome_name = (String) genome_names.next();
      AnnotatedSeqGroup group = (AnnotatedSeqGroup)name2group.get(genome_name);
      //      name2group.put(genome_name, group);
      genomeCB.addItem(genome_name);
    }
    genomeCB.setSelectedItem(SELECT_A_GENOME);
    genomeCB.addItemListener(this);
  }


  public static String getQuickLoadUrl() {
      String quickload_url = UnibrowPrefsUtil.getLocation(PREF_QUICKLOAD_URL, DEFAULT_QUICKLOAD_URL);
      if (quickload_url.indexOf(DEFUNCT_SERVER) >= 0) {
        System.out.println("WARNING: prefs pointed to deprecated default quickload server: " + DEFUNCT_SERVER);
	System.out.println("         updating to new default server: " + DEFAULT_QUICKLOAD_URL);
	UnibrowPrefsUtil.getLocationsNode().put(PREF_QUICKLOAD_URL, DEFAULT_QUICKLOAD_URL);
	quickload_url = DEFAULT_QUICKLOAD_URL;
      }
      if (! quickload_url.endsWith("/")) {
        quickload_url = quickload_url + "/";
	UnibrowPrefsUtil.getLocationsNode().put(PREF_QUICKLOAD_URL, quickload_url);
      }
    return quickload_url;
  }


}
