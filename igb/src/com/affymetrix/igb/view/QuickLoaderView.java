/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import com.affymetrix.genoviz.util.Timer;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.symmetry.*;

import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.das.DasDiscovery;
import com.affymetrix.igb.genometry.NibbleBioSeq;
import com.affymetrix.igb.parsers.LiftParser;
import com.affymetrix.igb.parsers.ChromInfoParser;
import com.affymetrix.igb.parsers.NibbleResiduesParser;
import com.affymetrix.igb.util.DasUtils;
import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.event.SeqSelectionListener;
import com.affymetrix.igb.event.GroupSelectionListener;
import com.affymetrix.igb.event.SeqSelectionEvent;
import com.affymetrix.igb.event.GroupSelectionEvent;
import com.affymetrix.igb.event.ThreadProgressMonitor;
import com.affymetrix.igb.prefs.IPlugin;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.util.UnibrowPrefsUtil;

public class QuickLoaderView extends JComponent
  implements ListSelectionListener, ItemListener, ActionListener,
             SeqSelectionListener, GroupSelectionListener, IPlugin  {

  public static final String PREF_QUICKLOAD_URL = "QuickLoad URL";
  public static final String PREF_DAS_DNA_SERVER_URL = "DAS DNA Server URL";
  public static final String PREF_QUICKLOAD_CACHE_USAGE = "quickload_cache_usage";
  public static final String PREF_QUICKLOAD_CACHE_RESIDUES = "quickload_cache_residues";
  public static final String PREF_QUICKLOAD_CACHE_ANNOTS = "quickload_cache_annots";

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static final String SELECT_A_GENOME = "Select a genome to load";
  static int CACHE_USAGE_DEFAULT = LocalUrlCacher.NORMAL_CACHE;
  static boolean CACHE_RESIDUES_DEFAULT = false;
  static boolean CACHE_ANNOTS_DEFAULT = true;
  static boolean LOAD_DEFAULT_ANNOTS = true;
  static Pattern tab_regex = Pattern.compile("\t");

  static Map cache_usage_options;
  static Map cache_name2usage;
  static Map cache_usage2name;
  static Map usage2str;

  static {
    String norm = "Normal Usage";
    String ignore = "Ignore Cache";
    String only = "Use Only Cache";
    Integer normal = new Integer(LocalUrlCacher.NORMAL_CACHE);
    Integer ignore_cache = new Integer(LocalUrlCacher.IGNORE_CACHE);
    Integer cache_only = new Integer(LocalUrlCacher.ONLY_CACHE);

    cache_usage_options = new LinkedHashMap();
    cache_usage_options.put(norm, normal);
    cache_usage_options.put(ignore, ignore_cache);
    cache_usage_options.put(only, cache_only);

    usage2str = new LinkedHashMap();
    usage2str.put(normal, norm);
    usage2str.put(ignore_cache, ignore);
    usage2str.put(cache_only, only);
  }

  String default_annot_name  = "refseq";

  int cache_usage =
    UnibrowPrefsUtil.getIntParam(PREF_QUICKLOAD_CACHE_USAGE, CACHE_USAGE_DEFAULT);
  boolean cache_residues =
    UnibrowPrefsUtil.getBooleanParam(PREF_QUICKLOAD_CACHE_RESIDUES, CACHE_RESIDUES_DEFAULT);
  boolean cache_annots =
    UnibrowPrefsUtil.getBooleanParam(PREF_QUICKLOAD_CACHE_ANNOTS, CACHE_ANNOTS_DEFAULT);

  // switched over to relying on prefs to determine where to go for quickload data:
  // always access via a URL, but get URL from "QuickLoadUrl" tagval in preferences.
  // For example, for local access, use a url like "file:/c:/data/quickload_data/"
  // For remote access use a URL like "http://netaffxdas.affymetrix.com/quickload_data/"

  // as a last resort, use hard-wired default:
  static final String DEFAULT_QUICKLOAD_URL = "http://netaffxdas.affymetrix.com/quickload_data/";
  static final String DEFUNCT_SERVER = "205.217.46.81";
  static final String DEFAULT_DAS_DNA_SERVER = "http://genome.cse.ucsc.edu/cgi-bin/das";
  String http_root = null;

  JButton residuesB;
  JButton partial_residuesB;
  JTable seqtable;
  JComboBox genome_selector;
  JButton optionsB;
  ListSelectionModel seqtable_model;
  JPanel checklist;

  // components for options dialog
  JPanel optionsP;
  JButton clear_cacheB;
  JCheckBox cache_annotsCB;
  JCheckBox cache_residuesCB;
  JComboBox cache_usage_selector;

  AnnotatedBioSeq current_seq = null;
  String current_genome_name = null;
  String current_genome_root = null;

  SeqMapView gviewer;
  Map checkbox2url = new HashMap();  // maps JCheckBox for type to url for data of that type
  Map checkbox2filename = new HashMap();  // maps JCheckBox for type to file name (end of url) for data of that type
  Map group2name  = new HashMap();  // maps AnnotatedSeqGroup to genome "name" from quickload contents.txt file
  Map name2group = new HashMap();  // maps genome "name" from quickload contents.txt file to AnnotatedSeqGroup

  // genome2types is map of genome version id to list of JCheckBoxes for available annotation types
  Map genome2cbs = new HashMap();
  Map default_types = new HashMap();

  /** Returns a QuickLoad URL, as set in the preferences, but guaranteed to end with "/". */
  public static String getQuickLoadUrl() {
      String quick_load_url = UnibrowPrefsUtil.getLocation(PREF_QUICKLOAD_URL, DEFAULT_QUICKLOAD_URL);
      if (quick_load_url.indexOf(DEFUNCT_SERVER) >= 0) {
        System.out.println("");
        System.out.println("WARNING:");
        System.out.println("The Affymetrix quickload server at '"+DEFUNCT_SERVER+"' is no longer supported.");
        System.out.println("You are being directed to '"+DEFAULT_QUICKLOAD_URL+"' instead");
        System.out.println("Please update your preferences to reflect this change.");
        System.out.println("");
        quick_load_url = DEFAULT_QUICKLOAD_URL;
      }
      if (! quick_load_url.endsWith("/")) {
        quick_load_url = quick_load_url + "/";
      }
    return quick_load_url;
  }

  protected void setQuickLoadURL(String url) {
    this.http_root = url;
    System.out.println("Setting QuickLoad location: " + http_root);

    genome_selector.setSelectedItem(SELECT_A_GENOME);
    genome_selector.removeItemListener(this);
    genome_selector.removeAllItems();

    genome_selector.addItem(SELECT_A_GENOME);

    java.util.List genome_list = loadGenomeNames();
    if (genome_list != null) {
      for (int i=0; i<genome_list.size(); i++) {
	String genome_name = (String)genome_list.get(i);
	genome_selector.addItem(genome_name);
      }
    }
    genome_selector.addItemListener(this);
    genome_selector.setSelectedItem(SELECT_A_GENOME);
    //    System.out.println("loaded available genome info into QuickLoad");

    this.processDasServersList();
    //TODO: should process synonym list now as well
  }

  public QuickLoaderView() {
    //gviewer = IGB.getSingletonIGB().getMapView(); // now this gets set when IGB calls putPluginProperty()

    cache_usage = UnibrowPrefsUtil.getIntParam("quickload_cache_usage", CACHE_USAGE_DEFAULT);
    cache_residues = UnibrowPrefsUtil.getBooleanParam("quickload_cache_residues", CACHE_RESIDUES_DEFAULT);
    cache_annots = UnibrowPrefsUtil.getBooleanParam("quickload_cache_annots", CACHE_ANNOTS_DEFAULT);

    genome_selector = new JComboBox();
    setQuickLoadURL(getQuickLoadUrl());

    initOptionsDialog();

    default_types.put(default_annot_name, default_annot_name);
    this.setLayout(new BorderLayout());
    seqtable = new JTable();

    //    seqtable.setCellSelectionEnabled(true);
    //    JTableCutPasteAdapter cut_paster = new JTableCutPasteAdapter(seqtable);
    checklist = new JPanel();
    checklist.setLayout(new BoxLayout(checklist, BoxLayout.Y_AXIS));

    this.add("Center", new JScrollPane(seqtable));
    JScrollPane listscroll = new JScrollPane(checklist);
    listscroll.setMinimumSize(new Dimension(200, 100));
    listscroll.setPreferredSize(new Dimension(200, 100));
    this.add("East", listscroll);
    seqtable_model = seqtable.getSelectionModel();
    seqtable_model.addListSelectionListener(this);
    seqtable_model.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    JPanel pan1 = new JPanel();
    pan1.setLayout(new GridLayout(1, 4));
    pan1.add(genome_selector);
    if (IGB.isSequenceAccessible()) {
      residuesB = new JButton("Load All Sequence Residues");
      residuesB.addActionListener(this);
      pan1.add(residuesB);
      partial_residuesB = new JButton("Load Sequence Residues in View");
      if (IGB.ALLOW_PARTIAL_SEQ_LOADING) {
	partial_residuesB.addActionListener(this);
	pan1.add(partial_residuesB);
      }
    }
    else {
      pan1.add(new JLabel("No sequence residues available", JLabel.CENTER));
    }

    optionsB = new JButton("Quickload Options");
    pan1.add(optionsB);
    optionsB.addActionListener(this);

    this.add("South", pan1);

    gmodel.addSeqSelectionListener(this);
    gmodel.addGroupSelectionListener(this);

    this.processDasServersList();
  }

  /** Adds the DAS servers from the file on the quickload server to the
   *  persistent list managed by DasDiscovery.
   */
  void processDasServersList() {
    String server_loc_list = getQuickLoadUrl() + "das_servers.txt";
    try {
      DasDiscovery.addServersFromTabFile(server_loc_list);
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("QuickLoad Error", "QuickLoad Error:\nProblem loading DAS server list from '"
      +server_loc_list+"'\n"+ex.toString(), gviewer);
    }
  }

  /**
   *  Also adds genomes loaded as AnnotatedSeqGroups to the
   *    SingletonGenometryModel.
   */
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
        System.out.println("Could not load DAS contents.txt file from\n" + http_root + "contents.txt");
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
	  group = gmodel.addSeqGroup(genome_name);
	  group2name.put(group, genome_name);
	  name2group.put(genome_name, group);
	  // System.out.println("added genome, name = " + line + ", group = " + group.getID() + ", " + group);
	}
	if ((fields.length >= 2) && (group != null)) {
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


  /**
   *  Return list of available annotation files for a given genome.
   *  assumes genome_root has already been correctly set
   *    (looks for ~genome_dir/annots.txt file which lists annotation files
   *     available in same directory.  If found, returns the list.  If no
   *     annots.txt file is found, returns "default" annotation files that
   *     are expected to be present)
   */
  java.util.List loadAnnotationNames() {
    ArrayList alist = null;
    try {
      InputStream istr =
	LocalUrlCacher.getInputStream(current_genome_root + "annots.txt", cache_usage, cache_annots);
      InputStreamReader ireader = new InputStreamReader(istr);
      BufferedReader br = new BufferedReader(ireader);
      String line;
      alist = new ArrayList();
      while ((line = br.readLine()) != null) {
	//	String[] fields = tab_regex.split(line);
	alist.add(line);
      }
      istr.close();
      ireader.close();
      br.close();
    }
    catch (Exception ex) {
      System.out.println("couldn't find annots.txt file listing annotation files");
      ex.printStackTrace();
    }
    return alist;
  }



  /**
   *   QuickLoaderView is registered as a GroupSelectionListner on the SingletonGenometryModel.
   *   If receives a groupSelectionChanged event, changes genome_selector's selection to
   *        reflect new group selection,
   *      Also needs to trigger redoing annotation combo boxes to reflect annotations
   *        available (and loaded) for selected seq group
   */
  public void groupSelectionChanged(GroupSelectionEvent evt)  {
    //  public void seqGroupSelected(AnnotatedSeqGroup group) {
    java.util.List glist = evt.getSelectedGroups();
    AnnotatedSeqGroup group = null;
    if (glist.size() > 0)  {
      group = (AnnotatedSeqGroup)glist.get(0);
      if (IGB.DEBUG_EVENTS)  {
        System.out.println("QuickLoaderView received seqGroupSelected() call: " + group.getID() + ",  " + group);
      }
    }
    else {
      if (IGB.DEBUG_EVENTS)  { System.out.println("QuickLoaderView received seqGroupSelected() call, but group = null"); }
    }
    if (group == null)  { return; }

    if (! group.isSynonymous(current_genome_name)) {
      String name = (String)group2name.get(group);
      if (name == null)  {
        System.out.println("Quickload could not find group: " + group.getID());
      }
      else {
        // wrapping genome_selector selection setting with remove/re-add listener to
        //    prevent event bounce...
        String current_item = (String) genome_selector.getSelectedItem();
        if (!group.isSynonymous(current_item)) {
          System.out.println("calling genome_selector.setSelected() item");
          genome_selector.removeItemListener(this);
          genome_selector.setSelectedItem(name);
          genome_selector.addItemListener(this);
          System.out.println("done calling genome_selector.setSelected() item");
        }
        current_item = (String) genome_selector.getSelectedItem();
        System.out.println("name: " + current_item + ", index: " +
                           genome_selector.getSelectedIndex());
        //      System.out.println(genome_selector);
        //      int count = genome_selector.getItemCount();
        //      for (int i=0; i<count; i++)  { System.out.println(genome_selector.getItemAt(i)); }

        loadGenome(current_item);
      }
    }
  }


  /**
   *   QuickLoaderView is registered as a SeqSelectionListner on the SingletonGenometryModel.
   *   If receives a seqSelectionChanged event, changes seqtable_model's selection to
   *        reflect new selection
   */
  public void seqSelectionChanged(SeqSelectionEvent evt)  {
    //  public void seqSelected(SmartAnnotBioSeq seq) {
    if (IGB.DEBUG_EVENTS)  {
      System.out.println("QuickLoaderView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
    }
    java.util.List slist = evt.getSelectedSeqs();
    AnnotatedBioSeq seq = null;
    if ((slist.size() > 0) && (slist.get(0) != null))  {
      seq = (MutableAnnotatedBioSeq)slist.get(0);
//      System.out.println("QuickLoaderView received seqSelected() call: " + seq.getID() + ",  " + seq);
    }
    else {
//      System.out.println("QuickLoaderView received seqSelected() call, but seq = null");
    }
    if (current_seq == seq) {
      return;
    }
    else {
      //      select(seq);
      if (seq == null) { seqtable_model.clearSelection(); }
      else {
	String seqid = seq.getID();
	TableModel tmod = seqtable.getModel();
	int rowcount = tmod.getRowCount();
	for (int i=0; i<rowcount; i++) {
	  String curid = (String)tmod.getValueAt(i, 0);
	  //	System.out.println("curid = " + curid + ", seqid = " + seqid);
	  if (curid.equals(seqid)) {
	    seqtable_model.setSelectionInterval(i, i);  // should trigger a valueChanged() event...
	    current_seq = seq;
	    break;
	  }
	}
      }
    }
  }


  public void loadGenome(String genome_name) {
    current_genome_name = genome_name;
    current_genome_root = http_root + genome_name + "/";
    //    System.out.println("***** setting current_genome_root: " + current_genome_root + " *****");
    AnnotatedSeqGroup group = (AnnotatedSeqGroup)name2group.get(genome_name);
    //    System.out.println("group: " + group);  // should always be non-null...
    System.out.println("loading genomic data for genome: " + genome_name);

    checklist.removeAll();
    java.util.List checkboxes = (java.util.List)genome2cbs.get(current_genome_name);
    java.util.List defaults_to_load = new ArrayList();
    boolean prev_loaded = (checkboxes != null);
    // GAH 8-27-2004
    // IGB.clearSymHash() will cause problems if revisit previously loaded genome!
    //    need to move sym hash stuff out of Unibrow, make it genome-specific...
    IGB.clearSymHash();

    if (! prev_loaded) {
      checkboxes = new ArrayList();
      java.util.List annot_files = loadAnnotationNames();
      if (annot_files != null) {
	for (int i=0; i<annot_files.size(); i++) {
	  String file_name = (String)annot_files.get(i);
	  String url_path = current_genome_root + file_name;
	  String annot_name = file_name.substring(0, file_name.indexOf("."));
	  JCheckBox cb = new JCheckBox(annot_name);
	  if (LOAD_DEFAULT_ANNOTS && default_types.get(annot_name) != null) {
	    cb.setSelected(true);
	    defaults_to_load.add(cb);
	  }
	  else {
	    cb.setSelected(false);
	    cb.addActionListener(this);
	  }
	  checkboxes.add(cb);
	  checkbox2url.put(cb, url_path);
	  checkbox2filename.put(cb, file_name);
	}
      }
      genome2cbs.put(current_genome_name, checkboxes);
    }

    if (checkboxes != null) {
      int checkcount = checkboxes.size();
      for (int i=0; i<checkcount; i++) {
	JCheckBox cb = (JCheckBox)checkboxes.get(i);
	checklist.add(cb);
      }
    }

    checklist.repaint();

    if (! prev_loaded) {
      try {
	InputStream lift_stream = null;
	InputStream cinfo_stream = null;

	System.out.println("lift URL: " + current_genome_root + "liftAll.lft");
	String lift_path = current_genome_root + "liftAll.lft";
	String cinfo_path = current_genome_root + "mod_chromInfo.txt";
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
	    return;
	  }
	}

	LiftParser lift_loader = new LiftParser();
	ChromInfoParser chrominfo_loader = new ChromInfoParser();
	boolean annot_contigs = false;
	if (lift_stream != null) {
	  group = lift_loader.parseGroup(lift_stream, genome_name, annot_contigs);
	}
	else  {
	  group = chrominfo_loader.parseGroup(cinfo_stream, genome_name);
	}
	System.out.println("group: " + group.getID() + ", " + group);
	//      gmodel.setSelectedSeqGroup(group);

	if (lift_stream != null)  { lift_stream.close(); }
	if (cinfo_stream != null) { cinfo_stream.close(); }
      }
      catch (Exception ex) {
	ErrorHandler.errorPanel("Error", "Error loading data:\n"+ex.toString(), gviewer);
	ex.printStackTrace();
      }
    }

    Iterator iter = group.getSeqs().values().iterator();
    /**  turning off listening to table selections while table is being repopulated */
    seqtable_model.removeListSelectionListener(this);
    populateSeqTable(group);
    /**  turning back on listening to table selections */
    seqtable_model.addListSelectionListener(this);

    current_seq = null;

    if (! prev_loaded) {
      if (LOAD_DEFAULT_ANNOTS && defaults_to_load.size() > 0)  {
	for (int i=0; i<defaults_to_load.size(); i++) {
	  JCheckBox cb = (JCheckBox)defaults_to_load.get(i);
	  System.out.println("loading default annots: " + cb.getText());
	  loadAnnotations(cb);
	}
      }
    }

  }


  protected void populateSeqTable(AnnotatedSeqGroup group) {
    Collection seqs = group.getSeqs().values();
    Iterator iter = seqs.iterator();
    Vector allrows = new Vector();
    while (iter.hasNext()) {
      MutableAnnotatedBioSeq curseq = (MutableAnnotatedBioSeq)iter.next();
      //      System.out.println(curseq.getID());
      Vector seqrow = new Vector();
      seqrow.add(curseq.getID());
      seqrow.add(new Integer(curseq.getLength()));
      allrows.add(seqrow);
    }
    Vector column_names = new Vector();
    column_names.add("seq ID");
    column_names.add("length");
    DefaultTableModel mod = new DefaultTableModel(allrows, column_names) {
      public boolean isCellEditable(int row, int column) { return false; }
      public Class getColumnClass(int column) {
        if (column==1) return Integer.class;
        else return String.class;
      }
    };
    seqtable.setModel(mod);
  }


  /** trying a threaded version of load annotations */
  public void loadAnnotations(final JCheckBox cbox)  {
    //    final String file_name = (String)checkbox2file.get(cbox);
    final String url_path = (String)checkbox2url.get(cbox);
    final String annot_type = (String)checkbox2filename.get(cbox);
    boolean selected = cbox.isSelected();
    AnnotatedSeqGroup group  = gmodel.getSelectedSeqGroup();
    //    System.out.println("checkbox changed for url: " + url_path + ", selected = " + selected);
    if (selected &&
	//	(loaded_url_hash.get(url_path) == null) &&
	current_genome_root != null )  {
      //      final String url_path = current_genome_root + file_name;
      System.out.println("trying to load: " + url_path);
      Thread worker_thread = new Thread() {
	  public void run() {
	    final MutableAnnotatedBioSeq curseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
	    ThreadProgressMonitor monitor =
              new ThreadProgressMonitor(null, "Loading annotations...", "Loading annotations: " + url_path,
                                        null, false, false);
	    monitor.showDialogEventually();
	    try {
	      InputStream istr = LocalUrlCacher.getInputStream(url_path, cache_usage, cache_annots);
	      BufferedInputStream bis = new BufferedInputStream(istr);
	      LoadFileAction lfa = new LoadFileAction(gviewer, null);
	      System.out.println("annot_type: " + annot_type);
	      lfa.load(gviewer, bis, annot_type, curseq);
	      bis.close();
	      istr.close();

	      //	      loaded_url_hash.put(url_path, url_path);

	      // GAH 8-3-2004: seq modification event propagation not yet stable, so calling setAnnotatedSeq()
	      SwingUtilities.invokeLater(new Runnable() {
		  public void run() {
		    gviewer.setAnnotatedSeq(curseq, true, true);
		    cbox.setEnabled(false);
		  }
		} );
	    }
	    catch (Exception ex) {
	      System.out.println("coulnd't find requested file: " + url_path);
	    }
	    monitor.closeDialogEventually();
	    monitor = null;
	  }
	};
      worker_thread.start();
    }
  }


  /**
   *  Registered as an ItemListener on genome_selector list,
   *    to trigger genome selection (and default annotation loading)
   */
  public void itemStateChanged(ItemEvent evt) {
    Object src = evt.getSource();
    if (src == genome_selector) {
      if (evt.getStateChange() == ItemEvent.SELECTED) {
        String genome_name = (String)genome_selector.getSelectedItem();

	if (! genome_name.equals(SELECT_A_GENOME)) {
	  // System.out.println("genome selection changed: " + evt);
	  // load the specified genome
	  //          current_genome_name = temp;
	  //	  loadGenome(genome_name);
	  AnnotatedSeqGroup group = gmodel.getSeqGroup(genome_name);
	  if (gmodel.getSelectedSeqGroup() != group) {
	    //	    System.out.println("##### genome_selector event triggering new group selection: " + group.getID());
	    gmodel.setSelectedSeqGroup(group);
	    gmodel.setSelectedSeq(null);
	  }
	  else if (! genome_name.equals(current_genome_name)) {
	    //	    System.out.println("##### genome_selector event triggering loadGenome without new group selection");
	    loadGenome(genome_name);
	  }
          genome_selector.removeItem(SELECT_A_GENOME);
	}
      }
    }
  }


  /**
   *  Registered as a ListSelectionListener on seqtable_model,
   *    to trigger sequence selection
   */
  public void valueChanged(ListSelectionEvent evt) {
    //    System.out.println(" " + evt.getValueIsAdjusting() + ", QuickLoaderView received a list selection event: " + evt);
    Object src = evt.getSource();
    if (src == seqtable_model && (! evt.getValueIsAdjusting())) {
      int srow = seqtable.getSelectedRow();
      String chrom_name = (String)seqtable.getModel().getValueAt(srow, 0);
      if (chrom_name != null) {
	AnnotatedBioSeq temp = gmodel.getSelectedSeqGroup().getSeq(chrom_name);
        if (temp == null) {
          ErrorHandler.errorPanel("Error", "No data for chromosome '"+chrom_name+"'", gviewer);
        }
        else if (temp != gmodel.getSelectedSeq()) {
          current_seq = temp;
	  gmodel.setSelectedSeq((MutableAnnotatedBioSeq)current_seq);
        }
      }
      else {
	System.out.println("QuickLoaderView selection in seqtable, but seq name is null!");
	current_seq = null;
	//	gviewer.clear();
	//	gmodel.setSelectedSeq(null, this);
	gmodel.setSelectedSeq(null);
      }
    }
  }


  /**
   *   Registered as an ActionListener on residuesB and partial_residuesB to
   *     trigger sequence residues loading;
   *   Registered as an ActionListener on annotation type checkboxes to
   *     trigger annotation loading.
   */
  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == clear_cacheB) {
      System.out.println("clearing local cache");
      LocalUrlCacher.clearCache();
    }
    /* handles annotation loading based on checkbox clicks */
    else if ((src instanceof JCheckBox) && (checkbox2url.get(src) != null)) {
      loadAnnotations((JCheckBox)src);
    }
    /* handles residues loading based on partial or full sequence load buttons */
    else if (src == partial_residuesB) {
      if (current_seq==null) { ErrorHandler.errorPanel("Error", "No sequence selected.", gviewer); return; }
      String seq_name = current_seq.getID();
      SeqSpan viewspan = gviewer.getVisibleSpan();
      if (viewspan.getBioSeq() != current_seq) {
	System.err.println("Error in QuickLoaderView: " +
			   "SeqMapView seq and QuickLoaderView current_seq not the same!");
	System.exit(0);
      }
      loadPartialResidues(viewspan);
    }
    else if (src == residuesB) {
      if (current_seq==null) { ErrorHandler.errorPanel("Error", "No sequence selected.", gviewer); return; }
      String seq_name = current_seq.getID();
      loadAllResidues(seq_name);
    }
    else if (src == optionsB) {
      showOptions();
    } else if (src == reset_das_dna_serverB) {
      UnibrowPrefsUtil.getLocationsNode().put(PREF_DAS_DNA_SERVER_URL, DEFAULT_DAS_DNA_SERVER);
    } else if (src == reset_quickload_urlB) {
      String url_from_xml_file = (String) IGB.getIGBPrefs().get("QuickLoadUrl");
      if (url_from_xml_file != null) {
        UnibrowPrefsUtil.getLocationsNode().put(PREF_QUICKLOAD_URL, url_from_xml_file);
      } else {
        UnibrowPrefsUtil.getLocationsNode().put(PREF_QUICKLOAD_URL, DEFAULT_QUICKLOAD_URL);
      }
    }
  }

  JButton reset_das_dna_serverB = new JButton("Reset");
  JButton reset_quickload_urlB = new JButton("Reset");

  public void initOptionsDialog() {
    //    System.out.println("showing quickload options");
    if (optionsP == null) {
      optionsP = new JPanel();
      optionsP.setLayout(new BoxLayout(optionsP, BoxLayout.Y_AXIS));

      Box url_box = new Box(BoxLayout.X_AXIS);
      url_box.setBorder(new javax.swing.border.TitledBorder("QuickLoad URL"));
      JTextField quickload_url_TF = UnibrowPrefsUtil.createTextField(UnibrowPrefsUtil.getLocationsNode(), PREF_QUICKLOAD_URL, DEFAULT_QUICKLOAD_URL);
      url_box.add(quickload_url_TF);
      url_box.add(reset_quickload_urlB);
      reset_quickload_urlB.addActionListener(this);
      optionsP.add(url_box);
      optionsP.add(Box.createRigidArea(new Dimension(0, 5)));

      Box server_box = new Box(BoxLayout.X_AXIS);
      server_box.setBorder(new javax.swing.border.TitledBorder("Das DNA Server URL"));
      JTextField das_dna_server_TF = UnibrowPrefsUtil.createTextField(UnibrowPrefsUtil.getLocationsNode(), PREF_DAS_DNA_SERVER_URL, DEFAULT_DAS_DNA_SERVER);
      server_box.add(das_dna_server_TF);
      server_box.add(reset_das_dna_serverB);
      reset_das_dna_serverB.addActionListener(this);
      optionsP.add(server_box);
      optionsP.add(Box.createRigidArea(new Dimension(0, 5)));

      JPanel cache_options_box = new JPanel();
      cache_options_box.setBorder(new javax.swing.border.TitledBorder(""));
      cache_options_box.setLayout(new GridLayout(4, 1));
      optionsP.add(cache_options_box);

      cache_annotsCB = new JCheckBox("Cache Annotations", cache_annots);
      cache_residuesCB = new JCheckBox("Cache DNA Residues", cache_residues);
      clear_cacheB = new JButton("Clear Cache");
      cache_usage_selector = new JComboBox();
      Iterator iter = cache_usage_options.keySet().iterator();
      while (iter.hasNext()) {
	String str = (String)iter.next();
	cache_usage_selector.addItem(str);
      }
      cache_usage_selector.setSelectedItem(usage2str.get(new Integer(cache_usage)));

      cache_options_box.add(cache_annotsCB);
      cache_options_box.add(cache_residuesCB);
      JPanel usageP = new JPanel();
      usageP.setLayout(new GridLayout(1,2));
      usageP.add(new JLabel("Cache Usage"));
      usageP.add(cache_usage_selector);
      cache_options_box.add(usageP);
      cache_options_box.add(clear_cacheB);

      cache_annotsCB.addActionListener(this);
      cache_residuesCB.addActionListener(this);
      cache_usage_selector.addItemListener(this);
      clear_cacheB.addActionListener(this);
    }
  }

  public static void main(String[] args) {
    QuickLoaderView qlv = new QuickLoaderView();
    qlv.showOptions();
    System.exit(0);
  }

  public void showOptions() {
    //TODO: before showing the options dialog, need to reset its GUI to actual current values
    JOptionPane.showMessageDialog(this, optionsP, "Quickload Options", JOptionPane.PLAIN_MESSAGE);

    //TODO: Give the user a "Cancel" options as well as "OK"
    //int option = JOptionPane.showConfirmDialog(this, optionsP, "Quickload Options", JOptionPane.OK_CANCEL_OPTION);
    //if (option == JOptionPane.OK_OPTION) {
        String usage_str = (String)cache_usage_selector.getSelectedItem();
	int usage = ((Integer)cache_usage_options.get(usage_str)).intValue();
        setCacheBehavior(usage, cache_annotsCB.isSelected(), cache_residuesCB.isSelected());

        // Note that the preferred DAS_DNA_SERVER_URL gets set immediately when the JTextBox is changed
        // Note that the preferred QUICK_LOAD_URL gets set immediately when the JTextBox is changed
        //  ... but we have to update the GUI in response to changes in QUICK_LOAD_URL

        setQuickLoadURL(UnibrowPrefsUtil.getLocation(PREF_QUICKLOAD_URL, DEFAULT_QUICKLOAD_URL));
        setQuickLoadURL(getQuickLoadUrl());

    //}
  }

  /**
   *  Load sequence residues for a span along a sequence.
   *  Access residues via DAS reference server
   *
   *  DAS reference server can be specified in igb_prefs.xml file by setting DasDnaServer element
   *  Currently defaults to UCSC DAS reference server (this will cause problems if genome is not
   *     available at UCSC)
   */
  public void loadPartialResidues(SeqSpan span)  {
    String das_dna_server = UnibrowPrefsUtil.getLocation(PREF_DAS_DNA_SERVER_URL, DEFAULT_DAS_DNA_SERVER);
    AnnotatedBioSeq aseq = (AnnotatedBioSeq)span.getBioSeq();
    String seqid = aseq.getID();
    System.out.println("trying to load residues for span: " + SeqUtils.spanToString(span));
    System.out.println("current genome name: " + current_genome_name);
    //    System.out.println("seq_id: " + seqid);
    int min = span.getMin();
    int max = span.getMax();
    int length = span.getLength();

    if ((min <= 0) && (max >= aseq.getLength())) {
      System.out.println("loading all residues");
      loadAllResidues(aseq.getID());
    }
    else if (aseq instanceof NibbleBioSeq)  {
      String residues = null;
      try {
	String das_dna_source = DasUtils.findDasSource(das_dna_server, current_genome_name);
	if (das_dna_source == null)  {
	  ErrorHandler.errorPanel("Error", "Couldn't access sequence residues via DAS", gviewer);
	  return;
	}
	String das_seqid = DasUtils.findDasSeqID(das_dna_server, das_dna_source, seqid);
	if (das_seqid == null)  {
	  ErrorHandler.errorPanel("Error", "Couldn't access sequence residues via DAS", gviewer);
	  return;
	}
	 residues = DasUtils.getDasResidues(das_dna_server, das_dna_source, das_seqid,
						  min, max);
	System.out.println("DAS DNA request length: " + length);
	System.out.println("DAS DNA response length: " + residues.length());
      }
      catch (Exception ex) {
	ex.printStackTrace();
      }

      if (residues != null) {
	BioSeq subseq = new SimpleBioSeq(aseq.getID() + ":" + min + "-" + max, residues);

	SeqSpan span1 = new SimpleSeqSpan(0, length, subseq);
	SeqSpan span2 = span;
	MutableSeqSymmetry subsym = new SimpleMutableSeqSymmetry();
	subsym.addSpan(span1);
	subsym.addSpan(span2);

	NibbleBioSeq compseq = (NibbleBioSeq)aseq;
	MutableSeqSymmetry compsym = (MutableSeqSymmetry)compseq.getComposition();
	if (compsym == null) {
	  System.err.println("composite symmetry is null!");
	  compsym = new SimpleMutableSeqSymmetry();
	  compsym.addChild(subsym);
	  compsym.addSpan(new SimpleSeqSpan(span2.getMin(), span2.getMax(), aseq));
	  compseq.setComposition(compsym);
	}
	else {
	  compsym.addChild(subsym);
	  SeqSpan compspan = compsym.getSpan(aseq);
	  int compmin = Math.min(compspan.getMin(), min);
	  int compmax = Math.max(compspan.getMax(), max);
	  SeqSpan new_compspan = new SimpleSeqSpan(compmin, compmax, aseq);
	  compsym.removeSpan(compspan);
	  compsym.addSpan(new_compspan);
	  //	System.out.println("adding to composition: " );
	  //	SeqUtils.printSymmetry(compsym);
	  gviewer.setAnnotatedSeq(aseq, true, true);
	}
      }
    }
    else {
      System.err.println("quickloaded seq is _not_ a NibbleBioSeq: " + aseq);
    }
  }

  public void loadAllResidues(String seq_name) {
    System.out.println("processing request to load residues for sequence: " +
		       seq_name + ", version = " + current_genome_name);
    if (current_seq.isComplete()) {
      System.out.println("already have residues for " + seq_name);
      return;
    }
    else {
      try {
	String url_path = http_root + current_genome_name + "/" + seq_name + ".bnib";
	System.out.println("location of bnib file: " + url_path);
	System.out.println("current seq: id = " + current_seq.getID() + ", " + current_seq);
	InputStream istr = LocalUrlCacher.getInputStream(url_path, cache_usage, cache_residues);
	//	istr = (new URL(url_path)).openStream();
	// NibbleResiduesParser handles creating a BufferedInputStream from the input stream
	current_seq = NibbleResiduesParser.parse(istr, (NibbleBioSeq)current_seq);
	istr.close();
      }
      catch(Exception ex) {
	ex.printStackTrace();
	ErrorHandler.errorPanel("Error", "cannot access sequence for seq = " + seq_name +
			   ", version = " + current_genome_name, gviewer);
      }
      gviewer.setAnnotatedSeq(current_seq, true, true);
    }
  }

  protected void setCacheBehavior(int behavior, boolean annots, boolean residues) {
    if (behavior != cache_usage) {
      cache_usage = behavior;
      UnibrowPrefsUtil.saveIntParam("quickload_cache_usage", cache_usage);
      System.out.println("storing cache_usage behavior: " + cache_usage);
    }
    if (residues != cache_residues) {
      cache_residues = residues;
      UnibrowPrefsUtil.saveBooleanParam("quickload_cache_residues", cache_residues);
      System.out.println("storing cache_residues behavior: " + cache_residues);
    }
    if (annots != cache_annots) {
      cache_annots = annots;
      UnibrowPrefsUtil.saveBooleanParam("quickload_cache_annots", cache_annots);
      System.out.println("storing cache_annots behavior: " + cache_annots);
    }
  }

  public void putPluginProperty(Object key, Object value) {
    if (IPlugin.TEXT_KEY_SEQ_MAP_VIEW.equals(key)) {
      gviewer = (SeqMapView) value;
    }
  }

  public Object getPluginProperty(Object key) {
    if (IPlugin.TEXT_KEY_SEQ_MAP_VIEW.equals(key)) {
      return gviewer;
    }
    else return null;
  }

  public void destroy() {
    gviewer = null;
  }

}

