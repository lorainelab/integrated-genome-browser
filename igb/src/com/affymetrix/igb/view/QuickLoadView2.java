/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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
import java.util.*;
import java.util.prefs.*;
import javax.swing.*;
import java.net.URL;
import java.util.regex.Pattern;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.util.*;

import com.affymetrix.igb.IGB;
import com.affymetrix.igb.das.DasDiscovery;
import com.affymetrix.igb.event.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.util.*;
import com.affymetrix.igb.parsers.LiftParser;
import com.affymetrix.igb.parsers.ChromInfoParser;
import com.affymetrix.igb.parsers.BedParser;
import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.igb.parsers.NibbleResiduesParser;
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

  public static final String PREF_DAS_DNA_SERVER_URL = "DAS DNA Server URL";
  public static final String DEFAULT_DAS_DNA_SERVER = "http://genome.cse.ucsc.edu/cgi-bin/das";

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

  static {
    default_types.put(default_annot_name, default_annot_name);
  }

  public QuickLoadView2() {

    PreferencesPanel pp = PreferencesPanel.getSingleton();
    pref_tab_number = pp.addPrefEditorComponent(new DataLoadPrefsView());
    UnibrowPrefsUtil.getLocationsNode().addPreferenceChangeListener(preferemce_change_listener);

    if (IGB.getSingletonIGB() != null) {
      gviewer = IGB.getSingletonIGB().getMapView();
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
      if (current_seq==null) { ErrorHandler.errorPanel("Error", "No sequence selected.", gviewer); return; }
      SeqSpan viewspan = gviewer.getVisibleSpan();
      if (viewspan.getBioSeq() != current_seq) {
        System.err.println("Error in QuickLoaderView: " +
                           "SeqMapView seq and QuickLoaderView current_seq not the same!");
      } else {
        loadPartialResidues(viewspan);
      }
    }
    else if (src == all_residuesB) {
      if (current_seq==null) { ErrorHandler.errorPanel("Error", "No sequence selected.", gviewer); return; }
      String seq_name = current_seq.getID();
      loadAllResidues(seq_name);
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
        gviewer.setAnnotatedSeq(gmodel.getSelectedSeq(), true, true);
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
    if (DEBUG_EVENTS)  { System.out.println("QuickLoadView2 received itemStateChanged event: " + evt); }

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

      AnnotatedSeqGroup group = gmodel.getSeqGroup(old_group_id);
      gmodel.setSelectedSeqGroup(group); // causes a GroupSelectionEvent
      if (group != null && group == gmodel.getSelectedSeqGroup()) {
        MutableAnnotatedBioSeq seq = group.getSeq(old_seq_id);
        gmodel.setSelectedSeq(seq); // causes a SeqSelectionEvent
      }
      types_panel.invalidate(); // make sure display gets updated
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
  }
  
  public String getCheckboxTitle(boolean prev_loaded, String filename) {
    String checkbox_title;
    String annot_name = getAnnotName(filename);
    if (prev_loaded) {
      checkbox_title = annot_name + " [Loaded]";
    } else {
      String full_name = current_server.root_url + current_genome_name + "/" + filename;      
      String load_type = LocalUrlCacher.getLoadType(full_name, LocalUrlCacher.getPreferredCacheUsage());
      checkbox_title = annot_name + " [" + load_type + "]";
    }
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

        java.util.List file_names = current_server.getFilenames(current_genome_name);
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
              
              // Now force display of the current seq (or the first seq in the group)
              AnnotatedBioSeq seq = gmodel.getSelectedSeq();
              if (seq == null && current_group.getSeqCount() > 0) { 
                seq =  current_group.getSeq(0); 
              }
              if (seq != null) {
                gviewer.setAnnotatedSeq(seq, true, true);
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
  public static String getQuickLoadUrl() {
    return getUrlLastUsed();
  }

  /**
   *   Optionally call this after IGB has been set-up.
   *   Resets the server to the one that was in use the last time the program
   *   was shut down.
   */
  void initialize() {
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
          ErrorHandler.errorPanel("Error", "Couldn't find das source genome '"+current_genome_name
            + "'\n on DAS server:\n"+ das_dna_server, gviewer);
          return;
        }
        String das_seqid = DasUtils.findDasSeqID(das_dna_server, das_dna_source, seqid);
        if (das_seqid == null)  {
          ErrorHandler.errorPanel("No sequence",
            "Couldn't access sequence residues on DAS server\n" +
            " seqid: '" + seqid +"'\n"+
            " genome: '"+current_genome_name + "'\n" +
            " DAS server: " + das_dna_server,
            gviewer);
          return;
        }
         residues = DasUtils.getDasResidues(das_dna_server, das_dna_source, das_seqid,
                                                  min, max);
        System.out.println("DAS DNA request length: " + length);
        System.out.println("DAS DNA response length: " + residues.length());
      }
      catch (Exception ex) {
        ErrorHandler.errorPanel("No sequence",
          "Couldn't access sequence residues on DAS server\n" +
          " seqid: '" + seqid +"'\n"+
          " genome: '"+current_genome_name + "'\n" +
          " DAS server: " + das_dna_server,
          gviewer, ex);
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
          //System.err.println("composite symmetry is null!");
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
          //        System.out.println("adding to composition: " );
          //        SeqUtils.printSymmetry(compsym);
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
      InputStream istr = null;
      //String http_root = getQuickLoadUrl();
      String http_root = current_server.getRootUrl();
      try {
        String url_path = http_root + current_genome_name + "/" + seq_name + ".bnib";
        System.out.println("location of bnib file: " + url_path);
        System.out.println("current seq: id = " + current_seq.getID() + ", " + current_seq);
        istr = LocalUrlCacher.getInputStream(url_path, QuickLoadServerModel.getCacheResidues());
        //        istr = (new URL(url_path)).openStream();
        // NibbleResiduesParser handles creating a BufferedInputStream from the input stream
        current_seq = NibbleResiduesParser.parse(istr, gmodel.getSelectedSeqGroup());
      }
      catch(Exception ex) {
        ErrorHandler.errorPanel("Error", "cannot access sequence:\n" +
          "seq = '" + seq_name + "'\n" +
          "version = '" + current_genome_name +"'\n" +
          "server = " + http_root,
        gviewer, ex);
      }
      finally {
        try { istr.close(); } catch (Exception e) {}
      }

      gviewer.setAnnotatedSeq(current_seq, true, true);
    }
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
  
  PreferenceChangeListener preferemce_change_listener = new PreferenceChangeListener() {
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
      result = QuickLoadServerModel.getQLModelForURL(the_url);

      if (result != null) {
        SynonymLookup.getDefaultLookup().loadSynonyms(result.getRootUrl()+"synonyms.txt");
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

class QuickLoadServerModel {
  public static final String PREF_QUICKLOAD_CACHE_RESIDUES = "quickload_cache_residues";
  public static final String PREF_QUICKLOAD_CACHE_ANNOTS = "quickload_cache_annots";
  static String ENCODE_FILE_NAME = "encodeRegions.bed";
  static String ENCODE_FILE_NAME2 = "encode.bed";

  static boolean CACHE_RESIDUES_DEFAULT = false;
  static boolean CACHE_ANNOTS_DEFAULT = true;

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  static Pattern tab_regex = Pattern.compile("\t");

  String root_url;
  java.util.List genome_names = new ArrayList();

  Map group2name = new HashMap();
  Map genome2init = new HashMap();

  // A map from String genome name to a List of filenames on the server for that group
  Map genome2file_names = new HashMap();

  /**
   *  Map of AnnotatedSeqGroup to a load state map.
   *  Each load state map is a map of an annotation type name to Boolean for
   *  whether it has already been loaded or not
   */
  static Map group2states = new HashMap();


  QuickLoadServerModel(String url) {
    root_url = url;
    if (! root_url.endsWith("/")) {
      root_url = root_url + "/";
    }
    java.util.List xxx = loadGenomeNames();
    if (xxx == null || xxx.isEmpty()) {
      // do what?
    }
  }

  static Map url2quickload = new HashMap();

  public static QuickLoadServerModel getQLModelForURL(URL url) {
    String ql_http_root = url.toExternalForm();
    if (! ql_http_root.endsWith("/")) {
      ql_http_root = ql_http_root + "/";
    }
    QuickLoadServerModel ql_server = (QuickLoadServerModel) url2quickload.get(ql_http_root);
    if (ql_server == null) {
      ql_server = new QuickLoadServerModel(ql_http_root);
      url2quickload.put(ql_http_root, ql_server);
      SynonymLookup.getDefaultLookup().loadSynonyms(ql_http_root+"synonyms.txt");
    }
    return ql_server;
  }

  static boolean getCacheResidues() {
    return UnibrowPrefsUtil.getBooleanParam(PREF_QUICKLOAD_CACHE_RESIDUES, CACHE_RESIDUES_DEFAULT);
  }
  
  static boolean getCacheAnnots() {
    return UnibrowPrefsUtil.getBooleanParam(PREF_QUICKLOAD_CACHE_ANNOTS, CACHE_ANNOTS_DEFAULT);
  }

  public String getRootUrl() { return root_url; }
  public java.util.List getGenomeNames() { return genome_names; }
  //public Map getSeqGroups() { return group2name; }
  public static AnnotatedSeqGroup getSeqGroup(String genome_name) { return gmodel.addSeqGroup(genome_name);  }

  /** Returns the name that this QuickLoad server uses to refer to the given AnnotatedSeqGroup.
   *  Because of synonyms, different QuickLoad servers may use different names to
   *  refer to the same genome.
   */
  public String getGenomeName(AnnotatedSeqGroup group) {
    return (String)group2name.get(group);
  }

  public static String stripFilenameExtensions(String name) {
    String new_name = name;
    if (name.indexOf('.') > 0) {
      new_name = name.substring(0, name.lastIndexOf('.'));
    }
    return new_name;
  }

  /**
   *  Returns the list of String filenames that this QuickLoad server has
   *  for the genome with the given name.
   *  The list may (rarely) be empty, but never null.
   */
  public java.util.List getFilenames(String genome_name) {
    initGenome(genome_name);
    java.util.List filenames = (java.util.List) genome2file_names.get(genome_name);
    if (filenames == null) return Collections.EMPTY_LIST;
    else return filenames;
  }

  /** Returns Map of annotation type name to Boolean, true iff annotation type is already loaded */
  public static Map getLoadStates(AnnotatedSeqGroup group) {
    return (Map)group2states.get(group);
  }

  public static boolean getLoadState(AnnotatedSeqGroup group, String file_name) {
    Map load_states = getLoadStates(group);
    if (load_states == null) { return false; /* shouldn't happen */}
    Boolean boo = (Boolean)load_states.get(stripFilenameExtensions(file_name));
    if (boo == null) { return false; }
    else { return boo.booleanValue(); }
  }
  
  public static void setLoadState(AnnotatedSeqGroup group, String file_name, boolean loaded) {
    Map load_states = (Map) group2states.get(group);
    if (load_states == null) {
      load_states = new LinkedHashMap();
      group2states.put(group, load_states);
    }
    load_states.put(stripFilenameExtensions(file_name), Boolean.valueOf(loaded));
  }

  public void initGenome(String genome_name) {
    if (genome_name == null) { return; }
    Boolean init = (Boolean)genome2init.get(genome_name);
    if (init != Boolean.TRUE) {
      System.out.println("initializing data for genome: " + genome_name);
      boolean seq_init = loadSeqInfo(genome_name);
      boolean annot_init = loadAnnotationNames(genome_name);
      if (seq_init && annot_init) {
	genome2init.put(genome_name, Boolean.TRUE);
      }
    }
  }

  /**
   *  Determines the list of annotation files available in the genome directory.
   *  Looks for ~genome_dir/annots.txt file which lists annotation files
   *  available in same directory.  Returns true or false depending on
   *  whether the file is sucessfully loaded.
   *  You can retrieve the filenames with {@link #getFilenames(String)}
   */
  public boolean loadAnnotationNames(String genome_name) {
    boolean success = true;
    String genome_root = root_url + genome_name + "/";
    AnnotatedSeqGroup group = gmodel.getSeqGroup(genome_name);
    System.out.println("loading list of available annotations for genome: " + genome_name);
    String filename = genome_root + "annots.txt";

    InputStream istr = null;
    BufferedReader br = null;
    try {
      istr = LocalUrlCacher.getInputStream(filename, getCacheAnnots());
      br = new BufferedReader(new InputStreamReader(istr));
      String line;
      while ((line = br.readLine()) != null) {
        String[] fields = tab_regex.split(line);
        if (fields.length >= 1) {
          String annot_file_name = fields[0];
          //          System.out.println("    " + annot_file_name);
          java.util.List file_names = (java.util.List) genome2file_names.get(genome_name);
          if (file_names == null) {
            file_names = new ArrayList();
            genome2file_names.put(genome_name, file_names);
          }
          file_names.add(annot_file_name);
	  if (QuickLoadView2.build_virtual_encode &&
	      (annot_file_name.equalsIgnoreCase(ENCODE_FILE_NAME) || annot_file_name.equalsIgnoreCase(ENCODE_FILE_NAME2)) &&
	      (group.getSeq(QuickLoadView2.ENCODE_REGIONS_ID) == null) ) {
	    addEncodeVirtualSeq(group, (genome_root + annot_file_name));
	  }
        }
      }
      success = true;
    }
    catch (Exception ex) {
      System.out.println("Couldn't find or couldn't process file "+filename);
    } finally {
      if (istr != null) try { istr.close(); } catch (Exception e) {}
      if (br != null) try {br.close(); } catch (Exception e) {}
    }
    return success;
  }

  /**
   *  using negative start coord for virtual genome seq because (at least for human genome)
   *     whole genome start/end/length can't be represented with positive 4-byte ints (limit is +/- 2.1 billion)
   */
  double default_genome_min = -2100200300;
  boolean DEBUG_VIRTUAL_GENOME = false;
  public void addGenomeVirtualSeq(AnnotatedSeqGroup group) {
    int seq_count = group.getSeqCount();
    if (seq_count <= 1) {
      // no need to make a virtual "genome" seq if there is only a single chromosome
      return;
    }
    
    System.out.println("$$$$$ adding virtual genome seq to seq group");
    if (QuickLoadView2.build_virtual_genome &&
	(group.getSeq(QuickLoadView2.GENOME_SEQ_ID) == null) ) {
      SmartAnnotBioSeq genome_seq = group.addSeq(QuickLoadView2.GENOME_SEQ_ID, 0);
      for (int i=0; i<seq_count; i++) {
	BioSeq seq = group.getSeq(i);
	if (seq != genome_seq) {
	  double glength = genome_seq.getLengthDouble();
	  int clength = seq.getLength();
	  int spacer = (clength > 5000000) ? 5000000 : 100000;
	  double new_glength = glength + clength + spacer;
	  //	genome_seq.setLength(new_glength);
	  genome_seq.setBoundsDouble(default_genome_min, default_genome_min + new_glength);
	  if (DEBUG_VIRTUAL_GENOME)  {
	    System.out.println("added seq: " + seq.getID() + ", new genome bounds: min = " + genome_seq.getMin() +
			       ", max = " + genome_seq.getMax() + ", length = " + genome_seq.getLengthDouble());
	  }

	  MutableSeqSymmetry child = new SimpleMutableSeqSymmetry();
	  MutableSeqSymmetry mapping = (MutableSeqSymmetry)genome_seq.getComposition();
	  if (mapping == null) {
	    mapping = new SimpleMutableSeqSymmetry();
	    mapping.addSpan(new MutableDoubleSeqSpan(default_genome_min, default_genome_min + clength, genome_seq));
	    genome_seq.setComposition(mapping);
	  }
	  else {
	    MutableDoubleSeqSpan mspan = (MutableDoubleSeqSpan)mapping.getSpan(genome_seq);
	    mspan.setDouble(default_genome_min, default_genome_min + new_glength, genome_seq);
	  }

	  // using doubles for coords, because may end up with coords > MAX_INT
	  child.addSpan(new MutableDoubleSeqSpan(glength + default_genome_min, glength + clength + default_genome_min, genome_seq));
	  child.addSpan(new MutableDoubleSeqSpan(0, clength, seq));
	  if (DEBUG_VIRTUAL_GENOME) {
	    SeqUtils.printSpan(child.getSpan(0));
	    SeqUtils.printSpan(child.getSpan(1));
	  }
	  mapping.addChild(child);
	}
      }  // end loop through group's seqs
    }
  }

  /**
   *  addEncodeVirtualSeq.
   *  adds virtual CompositeBioSeq which is composed from all the ENCODE regions.
   *  assumes urlpath resolves to bed file for ENCODE regions
   */
  public void addEncodeVirtualSeq(AnnotatedSeqGroup seq_group, String urlpath)  {
    System.out.println("$$$$$ adding virtual encode seq to seq group");
    // assume it's a bed file...
    BedParser parser = new BedParser();
    try {
      InputStream istr= LocalUrlCacher.getInputStream(urlpath, getCacheAnnots());
      //      BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(filepath)));
      java.util.List regions = parser.parse(istr, seq_group, false, QuickLoadView2.ENCODE_REGIONS_ID, false);
      int rcount = regions.size();
      //      System.out.println("Encode regions: " + rcount);
      SmartAnnotBioSeq virtual_seq = seq_group.addSeq(QuickLoadView2.ENCODE_REGIONS_ID, 0);
      MutableSeqSymmetry mapping = new SimpleMutableSeqSymmetry();

      int min_base_pos = 0;
      int current_base = min_base_pos;
      int spacer = 20000;
      for (int i=0; i<rcount; i++) {
	SeqSymmetry esym = (SeqSymmetry)regions.get(i);
	SeqSpan espan = esym.getSpan(0);
	int elength = espan.getLength();

	SimpleSymWithProps child = new SimpleSymWithProps();
	String cid = esym.getID();
	if (cid != null) { child.setID(cid); }
	child.addSpan(espan);
	child.addSpan(new SimpleSeqSpan(current_base, current_base + elength, virtual_seq));
	mapping.addChild(child);
	current_base = current_base + elength + spacer;
      }
      virtual_seq.setBounds(min_base_pos, current_base);
      mapping.addSpan(new SimpleSeqSpan(min_base_pos, current_base, virtual_seq));
      virtual_seq.setComposition(mapping);
    }
    catch (Exception ex) {  ex.printStackTrace(); }
    return;
  }


  public boolean loadSeqInfo(String genome_name) {
    boolean success = false;
    String genome_root = root_url + genome_name + "/";
    AnnotatedSeqGroup group = gmodel.getSeqGroup(genome_name);
    System.out.println("loading list of chromosomes for genome: " + genome_name);
    InputStream lift_stream = null;
    InputStream cinfo_stream = null;
    try {

      System.out.println("lift URL: " + genome_root + "liftAll.lft");
      String lift_path = genome_root + "liftAll.lft";
      String cinfo_path = genome_root + "mod_chromInfo.txt";
      try {
        lift_stream = LocalUrlCacher.getInputStream(lift_path, getCacheAnnots());
      }
      catch (Exception ex) {
        System.out.println("couldn't find lift file, looking instead for mod_chromInfo file");
        lift_stream = null;
      }
      if (lift_stream == null) {
        try {
          cinfo_stream = LocalUrlCacher.getInputStream(cinfo_path,  getCacheAnnots());
        }
        catch (Exception ex) {
          System.err.println("ERROR: could find neither liftAll.txt nor mod_chromInfo.txt files");
          cinfo_stream = null;
        }
      }

      boolean annot_contigs = false;
      if (lift_stream != null) {
        LiftParser lift_loader = new LiftParser();
        group = lift_loader.parse(lift_stream, genome_name, annot_contigs);
      }
      else if (cinfo_stream != null) {
        ChromInfoParser chrominfo_loader = new ChromInfoParser();
        group = chrominfo_loader.parse(cinfo_stream, genome_name);
      }
      System.out.println("group: " + (group == null ? null : group.getID()) + ", " + group);
      //      gmodel.setSelectedSeqGroup(group);
      success = true;
      if (QuickLoadView2.build_virtual_genome) {  addGenomeVirtualSeq(group); }
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("ERROR", "Error loading data for genome '"+ genome_name +"'", ex);
    }
    finally {
      if (lift_stream != null)  try { lift_stream.close(); } catch (Exception e) {}
      if (cinfo_stream != null) try { cinfo_stream.close(); } catch (Exception e) {}
    }
    return success;
  }


  public java.util.List loadGenomeNames() {
    ArrayList glist = null;
    try {
      InputStream istr = null;
      try {
        istr = LocalUrlCacher.getInputStream(root_url + "contents.txt", getCacheAnnots());
      } catch (Exception e) {
        System.out.println("ERROR: Couldn't open '"+root_url+"contents.txt\n:  "+e.toString());
        istr = null; // dealt with below
      }
      if (istr == null) {
        System.out.println("Could not load QuickLoad contents from\n" + root_url + "contents.txt");
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
          genome_names.add(genome_name);
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
      ErrorHandler.errorPanel("ERROR", "Error loading genome names", ex);
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
      InputStream istr = null;
      BufferedInputStream bis = null;
      
      try {
        istr = LocalUrlCacher.askAndGetInputStream(annot_url, getCacheAnnots());
        if (istr != null) {
          bis = new BufferedInputStream(istr);
          // really should remove LoadFileAction's requirement for SeqMapView argument...
          LoadFileAction lfa = new LoadFileAction(IGB.getSingletonIGB().getMapView(), null);
          lfa.load(IGB.getSingletonIGB().getMapView(), bis, filename, gmodel.getSelectedSeq());
          setLoadState(current_group, filename, true);
        }
      }
      catch (Exception ex) {
        ErrorHandler.errorPanel("ERROR", "Problem loading requested url:\n" + annot_url, ex);
        // keep load state false so we can load this annotation from a different server
        setLoadState(current_group, filename, false);
      } finally {
        if (bis != null) try {bis.close();} catch (Exception e) {}
        if (istr != null) try {istr.close();} catch (Exception e) {}
      }
    }
  }
  
  public String toString() {
    return "QuickLoadServerModel: url='" + getRootUrl() + "'";
  }
}
