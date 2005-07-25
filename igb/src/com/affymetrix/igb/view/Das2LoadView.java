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
import java.util.*;
import java.util.prefs.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;

import com.affymetrix.igb.IGB;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.igb.das2.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.event.*;
import com.affymetrix.swing.threads.SwingWorker;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.util.GenometryViewer;

import javax.swing.event.*;  // temporary visualization till hooked into IGB

public class Das2LoadView extends JComponent
  implements ItemListener, ActionListener, TableModelListener,
             // ComponentListener,  turned off pending change mechanism for now
	     SeqSelectionListener, GroupSelectionListener  {

  static Das2TypesTableModel empty_table_model = new Das2TypesTableModel(new ArrayList());

  boolean THREAD_FEATURE_REQUESTS = true;
  boolean USE_SIMPLE_VIEW = false;
  SeqMapView gviewer = null;
  GenometryViewer simple_viewer = null;

  JComboBox typestateCB;
  JComboBox das_serverCB;
  JComboBox das_sourceCB;
  JComboBox das_versionCB;
  JButton load_featuresB;
  JTable types_table;
  JScrollPane table_scroller;
  Map das_servers;
  Map version2typestates = new LinkedHashMap();
  LinkedHashMap checkbox2type;

  Das2LoadView myself = null;
  Das2ServerInfo current_server;
  Das2Source current_source;
  Das2VersionedSource current_version;
  Das2Region current_region;

  SingletonGenometryModel gmodel = IGB.getGenometryModel();
  AnnotatedSeqGroup current_group = null;
  AnnotatedBioSeq current_seq = null;

  String server_filler = "Choose a DAS2 server";
  String source_filler = "Choose a DAS2 source";
  String version_filler = "Choose a DAS2 version";

  // turned off pending change mechanism for now
  //  boolean pending_group_change = false;
  //  boolean pending_seq_change = false;

  public Das2LoadView() {
    this(false);
  }

  /**
   *  choices for DAS2 annot loading range:
   *    whole genome
   *    whole chromosome
   *    specified range on current chromosome
   *    gviewer's view bounds on current chromosome
   */
  public Das2LoadView(boolean simple_view) {
    myself = this;
    USE_SIMPLE_VIEW = simple_view;
    if (!USE_SIMPLE_VIEW) {
      gviewer = IGB.getSingletonIGB().getMapView();
    }


    das_serverCB = new JComboBox();
    das_sourceCB = new JComboBox();
    das_versionCB = new JComboBox();
    load_featuresB = new JButton("Load Features");

    typestateCB = new JComboBox();
    String[] load_states = Das2TypeState.LOAD_STRINGS;
    for (int i=0; i<load_states.length; i++) {
      typestateCB.addItem(load_states[i]);
    }

    types_table = new JTable();
    types_table.setModel(empty_table_model);
    table_scroller = new JScrollPane(types_table);

    checkbox2type = new LinkedHashMap();
    das_serverCB.addItem(server_filler);

    das_servers = Das2Discovery.getDas2Servers();
    Iterator iter = das_servers.keySet().iterator();
    while (iter.hasNext()) {
      String server_name = (String)iter.next();
      das_serverCB.addItem(server_name);
    }

    this.setLayout(new BorderLayout());
    JPanel panA = new JPanel();
    panA.setLayout(new GridLayout(3, 2));

    panA.add(new JLabel("DAS2 Server: "));
    panA.add(das_serverCB);
    panA.add(new JLabel("DAS2 Source: " ));
    panA.add(das_sourceCB);
    panA.add(new JLabel("DAS2 Version: "));
    panA.add(das_versionCB);

    JPanel middle_panel = new JPanel(new BorderLayout());
    middle_panel.setBorder(new TitledBorder("Available Annotation Types"));
    middle_panel.add("Center", table_scroller);

    this.add("North", panA);
    this.add("Center", middle_panel);
    this.add("South", load_featuresB);

    das_serverCB.addItemListener(this);
    das_sourceCB.addItemListener(this);
    das_versionCB.addItemListener(this);
    //    das_regionCB.addItemListener(this);
    load_featuresB.addActionListener(this);

    //    this.addComponentListener(this);  turned off pending change mechanism for now
    gmodel.addSeqSelectionListener(this);
    gmodel.addGroupSelectionListener(this);
  }

  public void itemStateChanged(ItemEvent evt) {
    //    System.out.println("Das2LoadView received ItemEvent: " + evt);
    Object src = evt.getSource();

    // selection of DAS server
    if ((src == das_serverCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      System.out.println("Das2LoadView received SELECTED ItemEvent on server combobox");
      String server_name = (String)evt.getItem();
      if (server_name == server_filler) {
	// clear source and version choice boxes, and types panel
      }
      else {
	System.out.println("DAS server selected: " + server_name);
	current_server = (Das2ServerInfo)das_servers.get(server_name);
	System.out.println(current_server);
	setSources();
      }
    }

    // selection of DAS source
    else if ((src == das_sourceCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      System.out.println("Das2LoadView received SELECTED ItemEvent on source combobox");
      String source_name = (String)evt.getItem();
      if (source_name == source_filler) {
	// clear version choice box and types panel
      }
      else  {
	System.out.println("source name: " + source_name);
	Map sources = current_server.getSources();
	current_source = (Das2Source)sources.get(source_name);
	System.out.println(current_source);
	//	System.out.println("  genome id: " + current_source.getGenome().getID());
	//	setRegionsAndTypes();
	setVersions();
      }
    }

    else if ((src == das_versionCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      System.out.println("Das2LoadView received SELECTED ItemEvent on version combobox");
      String version_name = (String)evt.getItem();
      if (version_name == version_filler) {
	// clear types panel
      }
      if (version_name != version_filler) {
	System.out.println("version name: " + version_name);
	Map versions = current_source.getVersions();
	current_version = (Das2VersionedSource)versions.get(version_name);
	System.out.println(current_version);
	System.out.println("  version id: " + current_version.getGenome().getID());
	setRegionsAndTypes();
      }
    }

    // selection of DAS region point
    /*
    else if ((src == das_regionCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      System.out.println("Das2LoadView received SELECTED ItemEvent on region combobox");
      String region_name = (String)evt.getItem();
      if (region_name != region_filler) {
	System.out.println("region seq: " + region_name);
	Map regions = current_version.getRegions();
	current_region = (Das2Region)regions.get(region_name);
      }
    }
    */
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == load_featuresB) {
      System.out.println("Das2LoadView received ActionEvent on load features button");
      loadFeaturesInView();
    }
  }

  public void setVersions() {
    das_versionCB.removeItemListener(this);
    das_versionCB.removeAllItems();
    //    das_regionCB.removeAllItems();
    checkbox2type.clear();

    types_table.setModel(empty_table_model);
    types_table.validate();
    types_table.repaint();

    das_serverCB.setEnabled(false);
    das_sourceCB.setEnabled(false);
    das_versionCB.setEnabled(false);
    //    das_regionCB.setEnabled(false);

    // don't need to put getVersions() call on separate thread, since a source's versions
    //    will have already been initialized in a previous server.getSources() call
    Map versions = current_source.getVersions();
    das_versionCB.addItem(version_filler);
    Iterator iter = versions.values().iterator();
    while (iter.hasNext()) {
      Das2VersionedSource version = (Das2VersionedSource)iter.next();
      das_versionCB.addItem(version.getID());
    }

    das_versionCB.addItemListener(this);
    das_serverCB.setEnabled(true);
    das_sourceCB.setEnabled(true);
    das_versionCB.setEnabled(true);
    //	  das_regionCB.setEnabled(true);

  }

  public void setSources() {
    das_sourceCB.removeItemListener(this);
    das_versionCB.removeItemListener(this);
    das_sourceCB.removeAllItems();
    das_versionCB.removeAllItems();
    checkbox2type.clear();

    types_table.setModel(empty_table_model);
    types_table.validate();
    types_table.repaint();

    das_serverCB.setEnabled(false);
    das_sourceCB.setEnabled(false);
    das_versionCB.setEnabled(false);

    final SwingWorker worker = new SwingWorker() {
	Map sources = null;
	public Object construct() {
	  sources = current_server.getSources();
	  if (current_group == null) {
	    current_version = null;
	  }
	  else {
	    current_version = current_server.getVersionedSource(current_group);
	  }
	  if (current_version == null)  {
	    current_source = null;
	  }
	  else {
	    current_source = current_version.getSource();
	  }
	  return null;  // or could have it return types, shouldn't matter...
	}
	public void finished() {
	  das_sourceCB.addItem(source_filler);
	  Iterator iter = sources.values().iterator();
	  while (iter.hasNext()) {
	    Das2Source source = (Das2Source)iter.next();
	    //      das_sourceCB.addItem(source.getName() + "(" + source.getID() + ")");
	    das_sourceCB.addItem(source.getID());
	  }
	  if (current_source != null) {
	    das_sourceCB.setSelectedItem(current_source.getID());
	    das_versionCB.addItem(version_filler);
	    Iterator versions = current_source.getVersions().values().iterator();
	    while (versions.hasNext()) {
	      Das2VersionedSource version = (Das2VersionedSource)versions.next();
	      das_versionCB.addItem(version.getID());
	    }
	  }
	  if (current_version != null) {
	    das_versionCB.setSelectedItem(current_version.getID());
	  }
	  //	  das_sourceCB.addItemListener(listener);
	  das_sourceCB.addItemListener(myself);
	  das_versionCB.addItemListener(myself);
	  das_serverCB.setEnabled(true);
	  das_sourceCB.setEnabled(true);
	  das_versionCB.setEnabled(true);
	  //	  das_regionCB.setEnabled(true);

	  if (current_version != null) {
	    setRegionsAndTypes();
	  }
	}
      };
    worker.start();
  }


  public void setRegionsAndTypes() {
    das_serverCB.setEnabled(false);
    das_sourceCB.setEnabled(false);
    das_versionCB.setEnabled(false);
    load_featuresB.setEnabled(false);

    checkbox2type.clear();

    types_table.setModel(empty_table_model);
    types_table.validate();
    types_table.repaint();

    // alternative would be to use a QueuedExecutor (from ~.concurrent package)
    //    and two runnables, one for entries and one for types...
    final SwingWorker worker = new SwingWorker() {
	Map seqs = null;
	Map types = null;
	public Object construct() {
	  seqs = current_version.getRegions();
	  types = current_version.getTypes();
	  return null;
	}
	public void finished() {
	  /**
	   *  assumes that the types available for a given versioned source do not change
	   *    during the session
	   */
	  java.util.List type_states = 	(java.util.List)version2typestates.get(current_version);
	  if (type_states == null) {
	    type_states = new ArrayList();
	    Iterator iter = types.values().iterator();
	    while (iter.hasNext()) {
	      // need a map of Das2Type to Das2TypeState that persists for entire session,
	      //    and reuse Das2TypeStates when possible (because no guarantee that
	      //    Das2TypeState backing store has been updated during session
	      Das2Type dtype = (Das2Type)iter.next();
	      Das2TypeState tstate = new Das2TypeState(dtype);
	      type_states.add(tstate);
	    }
	    version2typestates.put(current_version, type_states);
	  }
	  Das2TypesTableModel new_table_model = new Das2TypesTableModel(type_states);
	  types_table.setModel(new_table_model);
	  new_table_model.addTableModelListener(myself);
	  TableColumn col = types_table.getColumnModel().getColumn(Das2TypesTableModel.LOAD_STRATEGY_COLUMN);
	  col.setCellEditor(new DefaultCellEditor(typestateCB));

	  types_table.validate();
	  types_table.repaint();
	  das_serverCB.setEnabled(true);
	  das_sourceCB.setEnabled(true);
	  das_versionCB.setEnabled(true);
	  load_featuresB.setEnabled(true);
	}
      };
    worker.start();
  }

  public void loadFeaturesInView() {
    MutableAnnotatedBioSeq selected_seq = gmodel.getSelectedSeq();
    final SeqSpan overlap = gviewer.getVisibleSpan();
    final MutableAnnotatedBioSeq visible_seq = (MutableAnnotatedBioSeq)overlap.getBioSeq();
    //    MutableAnnotatedBioSeq current_seq = current_region.getAnnotatedSeq();
    if (current_version != null) {
      if (current_seq != null) {
	current_region = current_version.getRegion(current_seq);
      }
      else {
	current_region = current_version.getRegion(visible_seq);
      }
    }

    if (visible_seq != selected_seq) { System.out.println("WARNING, VISIBLE SPAN DOES NOT MATCH GMODEL'S SELECTED SEQ!!!"); }
    System.out.println("seq = " + visible_seq.getID() +
		       ", min = " + overlap.getMin() + ", max = " + overlap.getMax());

    // iterate through Das2TypeStates
    //    if off, ignore
    //    if load_in_visible_range, do range in view annotation request
    //    if per-seq, do full seq annotation request
    // maybe add a fully_loaded flag so know which ones to skip because they're done?

    // could probably add finer resolution of threading here,
    //  so every request (one per type) launches on its own thread
    //  But for now putting them all on same (non-event) thread controlled by SwingWorker
    if (THREAD_FEATURE_REQUESTS) {
      final SwingWorker worker = new SwingWorker() {
	  int selected_type_count = 0;
	  public Object construct() {

	    java.util.List type_states = (java.util.List)version2typestates.get(current_version);
	    Iterator titer = type_states.iterator();
	    while (titer.hasNext()) {
	      Das2TypeState tstate = (Das2TypeState)titer.next();
	      Das2Type dtype = tstate.getDas2Type();
	      if (tstate.getLoadStrategy() == Das2TypeState.VISIBLE_RANGE) {
		System.out.println("type to load for visible range: " + dtype.getID());
		Das2FeatureRequestSym request_sym =
		  new Das2FeatureRequestSym(dtype, current_region, overlap, null);
		Iterator formats = request_sym.getDas2Type().getFormats().keySet().iterator();
		while (formats.hasNext()) {
		  String format = (String)formats.next();
		  String mime_type = (String)request_sym.getDas2Type().getFormats().get(format);
		  System.out.println("   format = " + format + ", mime_type = " + mime_type);
		}
		java.util.List optimized_requests = Das2ClientOptimizer.loadFeatures(request_sym);
		selected_type_count++;
		//	  current_region.getFeatures(request_sym);
		//   adding request_sym to annotated seq is now handled by Das2ClientOptimizer
		//	  synchronized (visible_seq) { visible_seq.addAnnotation(request_sym); }
	      }
	    }
	    return null;
	  }
	  public void finished() {
	    if (selected_type_count > 0) {
	      if (USE_SIMPLE_VIEW) {
		if (simple_viewer == null) { simple_viewer = GenometryViewer.displaySeq(visible_seq, false); }
		simple_viewer.setAnnotatedSeq(visible_seq);
	      }
	      else if (gviewer != null) {
		gviewer.setAnnotatedSeq(visible_seq, true, true);
	      }
	    }
	  }
	};
      worker.start();
    }

    else {  // non-threaded feature request
      int selected_type_count = 0;
      java.util.List type_states = (java.util.List)version2typestates.get(current_version);
      Iterator titer = type_states.iterator();
      while (titer.hasNext()) {
	Das2TypeState tstate = (Das2TypeState)titer.next();
	Das2Type dtype = tstate.getDas2Type();
	if (tstate.getLoadStrategy() == Das2TypeState.VISIBLE_RANGE) {
	  System.out.println("type to load for visible range: " + dtype.getID());
	  Das2FeatureRequestSym request_sym =
	    new Das2FeatureRequestSym(dtype, current_region, overlap, null);
	  Iterator formats = request_sym.getDas2Type().getFormats().keySet().iterator();
	  while (formats.hasNext()) {
	    String format = (String)formats.next();
	    String mime_type = (String)request_sym.getDas2Type().getFormats().get(format);
	    System.out.println("   format = " + format + ", mime_type = " + mime_type);
	  }
	  java.util.List optimized_requests = Das2ClientOptimizer.loadFeatures(request_sym);
	  selected_type_count++;
	  //	  current_region.getFeatures(request_sym);
	  //   adding request_sym to annotated seq is now handled by Das2ClientOptimizer
	  //	  synchronized (visible_seq) { visible_seq.addAnnotation(request_sym); }
	}
      }

      if (selected_type_count > 0) {
	if (USE_SIMPLE_VIEW) {
	  if (simple_viewer == null) { simple_viewer = GenometryViewer.displaySeq(visible_seq, false); }
	  simple_viewer.setAnnotatedSeq(visible_seq);
	}
	else if (gviewer != null) {
	  gviewer.setAnnotatedSeq(visible_seq, true, true);
	}
      }
    }  // end non-threaded feature request

  }

  /**
   *  When selected sequence changed, want to go through all previously visited
   *     DAS/2 versioned sources that share the seq's AnnotatedSeqGroup,
   *     For each (similar_versioned_source)
   *         for each type
   *            if (Das2TypeState set to AUTO_PER_SEQUENCE loading) && ( !state.fullyLoaded(seq) )
   *                 Do full feature load for seq
   *  For now assume that if a type's load state is not AUTO_PER_SEQUENCE, then no auto-loading, only
   *    manual loading, which is handled in another method...
   */
  public void seqSelectionChanged(SeqSelectionEvent evt) {
    if (IGB.DEBUG_EVENTS) {
      System.out.println(
          "Das2LoadView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
    }
    AnnotatedBioSeq newseq = evt.getSelectedSeq();
    if (current_seq != newseq) {
      current_seq = newseq;
      if (current_version != null) {
	current_region = current_version.getRegion(current_seq);
      }
    }
  }

  /**
   *  When selected group changed, want to go through all previously visited
   *     DAS/2 servers (starting with the current one), and try and find
   *     a versioned source that shares the selected AnnotatedSeqGroup
   *  If found, take first found and set versioned source, source, and server accordingly
   *  If not found, blank out versioned source and source, and switch server to "Choose a server"
   *
   *  For now, just looking at current server
   */
  public void groupSelectionChanged(GroupSelectionEvent evt) {
    //    if (IGB.DEBUG_EVENTS)  {
      System.out.println("Das2LoadView received GroupSelectionEvent: " + evt);
      //    }
    java.util.List groups = evt.getSelectedGroups();    
    if (groups != null && groups.size() > 0) {
      AnnotatedSeqGroup newgroup = (AnnotatedSeqGroup)groups.get(0);
      if (current_group != newgroup) {
        current_group = newgroup;
        if (current_server != null)  {
          current_version = current_server.getVersionedSource(current_group);
	  if (current_version == null) {
	    // reset
	    das_serverCB.setSelectedItem(server_filler);
	    das_sourceCB.setSelectedItem(source_filler);
	    das_versionCB.setSelectedItem(version_filler);
	    // need to reset table also...
	  }
	  else {
	    current_source = current_version.getSource();
	    System.out.println("   new das source: " + current_source.getID() +
			       ",  new das version: " + current_version.getID());
	    //	  das_sourceCB.removeItemListener(this);
	    das_sourceCB.setSelectedItem(current_source.getID());
	    das_versionCB.setSelectedItem(current_version.getID());
	    //	  das_sourceCB.addItemListener(this);
	  }
        }
      }
    }
  }


  public void tableChanged(TableModelEvent evt) {
    System.out.println("Das2LoadView received table model changed event: " + evt);
   //  JTable tab = (JTable)evt.getSource();
   // if (tab != types_table) {
   //   System.out.println("   table event received, but not on types table???");
   // }
    int col = evt.getColumn();
    int firstrow = evt.getFirstRow();
    int lastrow = evt.getLastRow();
    if (col == Das2TypesTableModel.LOAD_STRATEGY_COLUMN) {
      Object val = types_table.getValueAt(firstrow, col);
      System.out.println("value of changed table cell: " + val);
    }
  }

/** ComponentListener implementation, to allow putting off changes
 *     triggered by changing seq or seq group unless and until Das2LoadView is actually visible
 *
 *  turned this off, because really want Das2LoadView to respond regardless of whether it's
 *     visible or not.  May want to revisit at some point, because GUI itself doesn't need
 *     to respond if not visible, rather the auto-loading part needs to respond

  public void componentResized(ComponentEvent e) { }
  public void componentMoved(ComponentEvent e) { }
  public void componentHidden(ComponentEvent e) { }

  public void componentShown(ComponentEvent e) {
    System.out.println("Das2LoadView was just made visible");
    if (pending_group_change) {
      handleGroupChange();
      pending_group_change = false;
    }
    if (pending_seq_change)  {
      handleSeqChange();
      pending_seq_change = false;
    }
  }
*/

  public static void main(String[] args) {
    Das2LoadView testview = new Das2LoadView(true);
    JFrame frm = new JFrame();
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", testview);
    frm.setSize(new Dimension(400, 400));
    frm.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent evt) { System.exit(0);}
    });
    frm.show();
  }

}

/**
 *  relates a Das2Type to it's status in IGB
 *    (whether it's load strategy is set to full sequence or visible range,
 *      possibly other details)
 */
class Das2TypeState {
  //  static String[] LOAD_STRINGS = new String[3];
  static String[] LOAD_STRINGS = new String[3];
  static int OFF = 0;
  static int VISIBLE_RANGE = 1;   // MANUAL_VISIBLE_RANGE
  static int WHOLE_SEQUENCE = 2;  // AUTO_WHOLE_SEQUENCE
  static int default_load_strategy = OFF;

  /**
   *  Want to retrieve type state from Preferences if possible
   *    node: ~/das2/server.root.url/typestate
   *    key: [typeid+"_loadstate"]  value: [load_state]     (load state is an integer??)
   */

  static Preferences root_node = UnibrowPrefsUtil.getTopNode();
  static Preferences das2_node = root_node.node("das2");

  static {
    LOAD_STRINGS[OFF] = "Off";
    LOAD_STRINGS[VISIBLE_RANGE] = "Visible Range";
    LOAD_STRINGS[WHOLE_SEQUENCE] = "Whole Sequence";
  }

  int load_strategy;
  Das2Type type;
  Preferences lnode;

  public Das2TypeState(Das2Type dtype) {
    this.type = dtype;
    Das2VersionedSource version = type.getVersionedSource();
    Das2Source source = version.getSource();
    Das2ServerInfo server = source.getServerInfo();
    String server_root_url = server.getRootUrl();
    if (server_root_url.startsWith("http://")) { server_root_url = server_root_url.substring(7); }
    if (server_root_url.indexOf("//") > -1) {
      System.out.println("need to replace all double slashes in path!");
    }
    String subnode = server_root_url + "/" + source.getID() + "/" + version.getID() + "/type_load_strategy";
    System.out.println("subnode = " + subnode);
    System.out.println("    length: " + subnode.length());
    lnode = das2_node.node(subnode);
    load_strategy = lnode.getInt(type.getID(), default_load_strategy);
  }

  public void setLoadStrategy(String strat) {
    for (int i=0; i<LOAD_STRINGS.length; i++) {
      if (strat.equals(LOAD_STRINGS[i])) {
	setLoadStrategy(i);
	break;
      }
    }
  }

  public void setLoadStrategy(int strategy) {
    load_strategy = strategy;
    lnode.putInt(type.getID(), strategy);
  }

  public int getLoadStrategy() { return load_strategy; }
  public String getLoadString() { return LOAD_STRINGS[load_strategy]; }
  public Das2Type getDas2Type() { return type; }
}


class Das2TypesTableModel extends AbstractTableModel   {
  //  static String[] type_columns = { "type ID", "load", "ontology", "source", "load strategy" };
  static String[] column_names = { "type ID", "ontology", "source", "load strategy" };
  static int ID_COLUMN = 0;
  static int ONTOLOGY_COLUMN = 1;
  static int SOURCE_COLUMN = 2;
  static int LOAD_STRATEGY_COLUMN = 3;
  //  static int LOAD_BOOLEAN_COLUMN = 1;

  static int model_count = 0;

  int model_num;
  java.util.List type_states;

  public Das2TypesTableModel(java.util.List states) {
    model_num = model_count;
    model_count++;
    type_states = states;
    int col_count = column_names.length;
    int row_count = states.size();
  }

  public int getColumnCount() {
    return column_names.length;
  }

  public int getRowCount() {
    return type_states.size();
  }

  public String getColumnName(int col) {
    return column_names[col];
  }

  public Object getValueAt(int row, int col) {
    Das2TypeState state = (Das2TypeState)type_states.get(row);
    Das2Type type = state.getDas2Type();
    if (col == ID_COLUMN) {
      return type.getID();
    }
    else if (col == ONTOLOGY_COLUMN) {
      return type.getOntology();
    }
    else if (col == SOURCE_COLUMN) {
      return type.getDerivation();
    }
    else if (col == LOAD_STRATEGY_COLUMN) {
      return state.getLoadString();
    }
    //      else if (col == LOAD_BOOLEAN_COLUMN) {
    //	return ((state.getLoadStrategy() == Das2TypeState.OFF) ? Boolean.FALSE : Boolean.TRUE);
    //      }
    return null;
  }

  public Class getColumnClass(int c) {
    return getValueAt(0, c).getClass();
  }

  /*
   * Don't need to implement this method unless your table's
   * editable.
   */
  public boolean isCellEditable(int row, int col) {
    //      if (col == Das2LoadView.LOAD_STRATEGY_COLUMN ||
    //	  col == Das2LoadView.LOAD_BOOLEAN_COLUMN) { return true; }
    if (col == LOAD_STRATEGY_COLUMN) { return true; }
    else { return false; }
  }

  /*
   * Don't need to implement this method unless your table's
   * data can change.
   */
  public void setValueAt(Object value, int row, int col) {
    //      System.out.println("Das2TypesTableModel.setValueAt() called, row = " + row +
    //			 ", col = " + col + "val = " + value.toString());
    Das2TypeState state = (Das2TypeState)type_states.get(row);
    if (col == LOAD_STRATEGY_COLUMN)  {
      state.setLoadStrategy(value.toString());
    }
    /*
      else if (col == Das2LoadView.LOAD_BOOLEAN_COLUMN) {
      Boolean bool = (Boolean)value;
      if (bool == Boolean.TRUE) {  state.setLoadStrategy(Das2TypeState.VISIBLE_RANGE); }
      else { state.setLoadStrategy(Das2TypeState.OFF); }
      }
    */
    fireTableCellUpdated(row, col);
  }
}

