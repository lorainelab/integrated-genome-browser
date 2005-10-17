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
import com.affymetrix.igb.parsers.LiftParser;
import com.affymetrix.igb.parsers.ChromInfoParser;
import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.igb.IGB;

public class QuickLoadView2 extends JComponent
         implements ItemListener, ActionListener, GroupSelectionListener, SeqSelectionListener  {

  static boolean DEBUG_EVENTS = false;
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static final String DEFAULT_QUICKLOAD_URL = "http://netaffxdas.affymetrix.com/quickload_data/";
  static final String DEFUNCT_SERVER = "205.217.46.81";
  static final String SELECT_A_GENOME = "Select a genome to load";
  public static final String PREF_QUICKLOAD_URL = "QuickLoad URL";
  //  static boolean LOAD_DEFAULT_ANNOTS = true;

  JComboBox serverCB;
  JComboBox genomeCB;
  JPanel types_panel;
  Map url2quickload = new LinkedHashMap();   // maps quickload root url to QuickLoadServerModel object
  QuickLoadServerModel current_server;
  AnnotatedSeqGroup current_group;
  Map cb2filename = new HashMap();
  SeqMapView gviewer;

  public QuickLoadView2() {
    gviewer = IGB.getSingletonIGB().getMapView();
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

    String http_root = getQuickLoadUrl();
    System.out.println("Setting QuickLoad server: " + http_root);
    current_server = new QuickLoadServerModel(http_root);
    url2quickload.put(http_root, current_server);
    refreshGenomeChoices();
  }

  public void actionPerformed(ActionEvent evt)  {
    Object src = evt.getSource();
    if (src instanceof JCheckBox) {
      if (DEBUG_EVENTS)  { System.out.println("QuickLoadView2 received action on JCheckBox"); }
      JCheckBox cbox = (JCheckBox)src;
      String filename = (String)cb2filename.get(cbox);
      boolean selected = cbox.isSelected();
      if (selected)  {
	current_server.loadAnnotations(current_group, filename);
	gviewer.setAnnotatedSeq(gmodel.getSelectedSeq(), true, true);
	cbox.setEnabled(false);
      }
      else {
	// if deselected, what should happen -- delete the annots?  or just hide them?
	//	boolean loaded = current_server.getLoadState(current_group, filename);
      }
    }

  }

  public void itemStateChanged(ItemEvent evt) {
    Object src = evt.getSource();
    if (DEBUG_EVENTS)  { System.out.println("QuickLoadView2 received itemStateChanged event: " + evt); }
    if (src == serverCB) {

    }
    else if ((src == genomeCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      String genome_name = (String)genomeCB.getSelectedItem();
      if (! genome_name.equals(SELECT_A_GENOME)) {
	AnnotatedSeqGroup group = gmodel.getSeqGroup(genome_name);
	if (gmodel.getSelectedSeqGroup() != group) {
	  // need to initialize genome before setting it as selected seq group, in
	  //    case it hasn't been seen before
	  current_server.initGenome(genome_name);
	  // calling gmodel.setSelectedSeqGroup() should also bounce event back to this.groupSelectionChanged()
	  gmodel.setSelectedSeq(null);
	  gmodel.setSelectedSeqGroup(group);
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
    if (current_group != group) {
      cb2filename.clear();
      Component[] comps = types_panel.getComponents();
      for (int i=0; i<comps.length; i++) {
	JCheckBox cb = (JCheckBox)comps[i];
	// really shouldn't need to do this, since old checkboxes should get gc'd, but just to make sure...
        cb.removeActionListener(this);
      }
      types_panel.removeAll();

      current_group = group;
      String genome_name = current_server.getGenomeName(group);
      if (genome_name == null) {
	// what should behavior be if no genome in quickload server matches selected AnnotatedSeqGroup?
	refreshGenomeChoices();
      }
      else {
	current_server.initGenome(genome_name);
	genomeCB.setSelectedItem(genome_name);
	Map load_states = current_server.getLoadStates(genome_name);
	Iterator iter = load_states.entrySet().iterator();
	// populate list of checkboxes for annotation types
	while (iter.hasNext()) {
	  Map.Entry ent = (Map.Entry)iter.next();
	  String filename = (String)ent.getKey();
//          if (filename == null || filename.equals(""))  { continue; }
	  Boolean boo = (Boolean)ent.getValue();
	  String annot_name = filename.substring(0, filename.indexOf("."));
	  JCheckBox cb = new JCheckBox(annot_name);
	  cb2filename.put(cb, filename);
	  cb.setSelected(boo == Boolean.TRUE);
	  types_panel.add(cb);
	  cb.addActionListener(this);
	}
      }

    }
  }

  public void seqSelectionChanged(SeqSelectionEvent evt) {
    if (DEBUG_EVENTS) { System.out.println("QuickLoadView2.seqSelectionChanged() called"); }
  }


  void refreshGenomeChoices() {
    genomeCB.removeItemListener(this);
    genomeCB.removeAllItems();
    genomeCB.addItem(SELECT_A_GENOME);

    Iterator genome_names = current_server.getGenomeNames().keySet().iterator();
    while (genome_names.hasNext()) {
      String genome_name = (String) genome_names.next();
      AnnotatedSeqGroup group = current_server.getSeqGroup(genome_name);
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



class QuickLoadServerModel {
  public static final String PREF_QUICKLOAD_CACHE_USAGE = "quickload_cache_usage";
  public static final String PREF_QUICKLOAD_CACHE_RESIDUES = "quickload_cache_residues";
  public static final String PREF_QUICKLOAD_CACHE_ANNOTS = "quickload_cache_annots";

  static int CACHE_USAGE_DEFAULT = LocalUrlCacher.NORMAL_CACHE;
  static boolean CACHE_RESIDUES_DEFAULT = false;
  static boolean CACHE_ANNOTS_DEFAULT = true;

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static int cache_usage =
    UnibrowPrefsUtil.getIntParam(PREF_QUICKLOAD_CACHE_USAGE, CACHE_USAGE_DEFAULT);
  static boolean cache_residues =
    UnibrowPrefsUtil.getBooleanParam(PREF_QUICKLOAD_CACHE_RESIDUES, CACHE_RESIDUES_DEFAULT);
  static boolean cache_annots =
    UnibrowPrefsUtil.getBooleanParam(PREF_QUICKLOAD_CACHE_ANNOTS, CACHE_ANNOTS_DEFAULT);

  static Pattern tab_regex = Pattern.compile("\t");
  static Map default_types = new HashMap();
  static String default_annot_name  = "refseq";

  String root_url;
  // name2group maps genome "name" from quickload contents.txt file to AnnotatedSeqGroup
  //   shouldn't need this, since should be able to go from genome name to AnnotatedSeqGroup via genometry model
  //   could replace with just a list of genome names that this quickload server knows about
  Map name2group = new LinkedHashMap();
  Map group2name = new HashMap();
  //  Map group2loaded = new HashMap();
  Map genome2init = new HashMap();
  /**
   *  map of AnnotatedSeqGroup to a load state map
   *     each load state map is a map of an annotation file to Boolean for whether it has already been loaded or not
   */
  Map group2states = new HashMap();


  static {
    default_types.put(default_annot_name, default_annot_name);
  }

  public QuickLoadServerModel(String url) {
    root_url = url;
    loadGenomeNames();
  }

  public String getRootUrl() { return root_url; }
  public Map getGenomeNames() { return name2group; }
  public Map getSeqGroups() { return group2name; }
  public AnnotatedSeqGroup getSeqGroup(String genome_name) { return (AnnotatedSeqGroup)name2group.get(genome_name); }
  public String getGenomeName(AnnotatedSeqGroup group) {
    return (String)group2name.get(group);
  }
  /** returns map of annotation type name to Boolean, true iff annotation type is already loaded */
  public Map getLoadStates(String genome_name) {
    AnnotatedSeqGroup group = getSeqGroup(genome_name);
    return (Map)group2states.get(group);
  }

  public boolean getLoadState(String genome_name, String file_name) {
    Map load_states = getLoadStates(genome_name);
    Boolean boo = (Boolean)load_states.get(file_name);
    if (boo == null) { return false; }
    else { return (boo == Boolean.TRUE); }
  }

  public boolean getLoadState(AnnotatedSeqGroup group, String file_name) {
    return getLoadState(this.getGenomeName(group), file_name);
  }

  public void setLoadState(AnnotatedSeqGroup group, String file_name, boolean loaded) {
    Map load_states = getLoadStates(this.getGenomeName(group));
    load_states.put(file_name, loaded ? Boolean.TRUE : Boolean.FALSE);
  }

  public void initGenome(String genome_name) {
    if (genome_name == null) { return; }
    Boolean init = (Boolean)genome2init.get(genome_name);
    if (init != Boolean.TRUE) {
      System.out.println("initializing data for genome: " + genome_name);
      boolean seq_init = loadSeqInfo(genome_name);
      boolean annot_init = loadAnnotationNames(genome_name);
      if (seq_init && annot_init) { genome2init.put(genome_name, Boolean.TRUE); }
    }
  }

  /**
   *  Looks for ~genome_dir/annots.txt file which lists annotation files
   *     available in same directory.  If found, returns the list.  If no
   *     annots.txt file is found, returns an empty list.
   */
  public boolean loadAnnotationNames(String genome_name) {
    boolean success = false;
    String genome_root = root_url + genome_name + "/";
    AnnotatedSeqGroup group = gmodel.getSeqGroup(genome_name);
    System.out.println("loading list of available annotations for genome: " + genome_name);
    String filename = genome_root + "annots.txt";
    Map load_states = new LinkedHashMap();
    try {
      InputStream istr =
	LocalUrlCacher.getInputStream(filename, cache_usage, cache_annots);
      InputStreamReader ireader = new InputStreamReader(istr);
      BufferedReader br = new BufferedReader(ireader);
      String line;
      while ((line = br.readLine()) != null) {
	String[] fields = tab_regex.split(line);
	if (fields.length >= 1) {
	  String annot_file_name = fields[0];
	  //	  System.out.println("    " + annot_file_name);
	  load_states.put(annot_file_name, Boolean.FALSE);
	}
      }
      group2states.put(group, load_states);
      istr.close();
      ireader.close();
      br.close();
      success = true;
    }
    catch (Exception ex) {
      System.out.println("Couldn't find file "+filename);
    }
    return success;
  }



  public boolean loadSeqInfo(String genome_name) {
    boolean success = false;
    String genome_root = root_url + genome_name + "/";
    AnnotatedSeqGroup group = gmodel.getSeqGroup(genome_name);
    System.out.println("loading list of chromosomes for genome: " + genome_name);
    try {
      InputStream lift_stream = null;
      InputStream cinfo_stream = null;

      System.out.println("lift URL: " + genome_root + "liftAll.lft");
      String lift_path = genome_root + "liftAll.lft";
      String cinfo_path = genome_root + "mod_chromInfo.txt";
      try {
	lift_stream = LocalUrlCacher.getInputStream(lift_path, cache_usage, cache_annots);
      }
      catch (Exception ex) {
	System.out.println("couldn't find lift file, looking instead for mod_chromInfo file");
	lift_stream = null;
      }
      if (lift_stream == null) {
	try {
	  cinfo_stream = LocalUrlCacher.getInputStream(cinfo_path, cache_usage, cache_annots);
	}
	catch (Exception ex) {
	  System.err.println("ERROR: could find neither liftAll.txt nor mod_chromInfo.txt files");
	  cinfo_stream = null;
	}
      }

      boolean annot_contigs = false;
      if (lift_stream != null) {
	LiftParser lift_loader = new LiftParser();
	group = lift_loader.parseGroup(lift_stream, genome_name, annot_contigs);
      }
      else if (cinfo_stream != null) {
	ChromInfoParser chrominfo_loader = new ChromInfoParser();
	group = chrominfo_loader.parseGroup(cinfo_stream, genome_name);
      }
      System.out.println("group: " + (group == null ? null : group.getID()) + ", " + group);
      //      gmodel.setSelectedSeqGroup(group);

      if (lift_stream != null)  { lift_stream.close(); }
      if (cinfo_stream != null) { cinfo_stream.close(); }
      success = true;
    }
    catch (Exception ex) {
      IGB.errorPanel("Error loading data:\n"+ex.toString());
      ex.printStackTrace();
    }
    return success;
  }


  public java.util.List loadGenomeNames() {
    ArrayList glist = null;
    try {
      InputStream istr = null;
      try {
        istr = LocalUrlCacher.getInputStream(root_url + "contents.txt", cache_usage, cache_annots);
      } catch (Exception e) {
        istr = null; // dealt with below
      }
      if (istr == null) {
        System.out.println("Could not load contents.txt file from\n" + root_url + "contents.txt");
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
	  group2name.put(group, genome_name);
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

  public void loadAnnotations(AnnotatedSeqGroup current_group, String filename) {
    boolean loaded = getLoadState(current_group, filename);
    if (loaded) {
      System.out.println("already loaded: " + filename);
    }
    else {
      String annot_url = root_url + getGenomeName(current_group) + "/" + filename;
      System.out.println("need to load: " + annot_url);
      try {
	InputStream istr = LocalUrlCacher.getInputStream(annot_url, cache_usage, cache_annots);
	BufferedInputStream bis = new BufferedInputStream(istr);
	// really should remove LoadFileAction's requirement for SeqMapView argument...
	LoadFileAction lfa = new LoadFileAction(IGB.getSingletonIGB().getMapView(), null);
	lfa.load(IGB.getSingletonIGB().getMapView(), bis, filename, gmodel.getSelectedSeq());
	bis.close();
	istr.close();
      }
      catch (Exception ex) {
	System.out.println("problem loading requested url: " + annot_url);
	ex.printStackTrace();
      }
      setLoadState(current_group, filename, true);
    }
  }



}
