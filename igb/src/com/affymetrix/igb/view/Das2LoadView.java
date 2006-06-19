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
import java.util.*;
import java.util.prefs.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.tree.*;

import com.affymetrix.igb.IGB;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.igb.das2.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.event.*;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.swing.threads.SwingWorker;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.util.GenometryViewer;

import javax.swing.event.*;  // temporary visualization till hooked into IGB

public class Das2LoadView extends JComponent
  implements ActionListener, TableModelListener,
	     SeqSelectionListener, GroupSelectionListener,
             TreeSelectionListener {

  static boolean INCLUDE_NAME_SEARCH = false;
  static boolean USE_DAS2_OPTIMIZER = true;
  static Das2TypesTableModel empty_table_model = new Das2TypesTableModel(new ArrayList());
  static boolean DEBUG_EVENTS = false;

  boolean THREAD_FEATURE_REQUESTS = true;
  boolean USE_SIMPLE_VIEW = false;
  SeqMapView gviewer = null;
  GenometryViewer simple_viewer = null;

  JTabbedPane tpane = new JTabbedPane();
  JTextField searchTF = new JTextField(40);
  JComboBox typestateCB;
  JButton load_featuresB;
  JTable types_table;
  JScrollPane table_scroller;
  Map das_servers;
  Map version2typestates = new LinkedHashMap();

  Das2LoadView myself = null;
  Das2ServerInfo current_server;
  Das2Source current_source;
  Das2VersionedSource current_version;
  Das2Region current_region;

  SingletonGenometryModel gmodel = IGB.getGenometryModel();
  AnnotatedSeqGroup current_group = null;
  AnnotatedBioSeq current_seq = null;

  JTree tree;

  public Das2LoadView() {
    this(false);
  }

  /*
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

    DefaultMutableTreeNode top = new DefaultMutableTreeNode("DAS/2 Genome Servers");
    das_servers = Das2Discovery.getDas2Servers();
    Iterator iter = das_servers.values().iterator();
    while (iter.hasNext()) {
      Das2ServerInfo server = (Das2ServerInfo)iter.next();
      String server_name = server.getName();
      Das2ServerTreeNode snode = new Das2ServerTreeNode(server);
      top.add(snode);
    }
    tree = new JTree(top);

    load_featuresB = new JButton("Load Features");
    load_featuresB.setToolTipText("Load selected feature types for this region.");
    load_featuresB.setEnabled(false);
    typestateCB = new JComboBox();
    String[] load_states = Das2TypeState.LOAD_STRINGS;
    for (int i=1; i<load_states.length; i++) {
      typestateCB.addItem(load_states[i]);
    }
    JComponent load_features_box = Box.createHorizontalBox();
    load_features_box.add(Box.createHorizontalGlue());
    load_features_box.add(load_featuresB);
    load_features_box.add(Box.createHorizontalGlue());

    types_table = new JTable();
    types_table.setModel(empty_table_model);
    table_scroller = new JScrollPane(types_table);

    this.setLayout(new BorderLayout());
    this.add("West", new JScrollPane(tree));

    JPanel types_panel = new JPanel(new BorderLayout());
    types_panel.setBorder(new TitledBorder("Available Annotation Types"));

    JPanel namesearchP = new JPanel();

    namesearchP.add(new JLabel("name search: "));
    namesearchP.add(searchTF);

    //    types_panel.add("North", namesearchP);
    types_panel.add("Center", table_scroller);
    types_panel.add("South", load_features_box);


    JSplitPane splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitpane.setOneTouchExpandable(true);
    //    splitpane.setDividerSize(8);
    //    splitpane.setDividerLocation(frm.getHeight() - (table_height + fudge));
    if (INCLUDE_NAME_SEARCH) {
      splitpane.setLeftComponent(new JScrollPane(tree));
      tpane.addTab("Types", types_panel);
      tpane.addTab("Name Search", namesearchP);
      splitpane.setRightComponent(tpane);
    }
    else {
      splitpane.setRightComponent(types_panel);
    }

    this.add("Center", splitpane);

    load_featuresB.addActionListener(this);
    //    this.addComponentListener(this);  turned off pending change mechanism for now
    gmodel.addSeqSelectionListener(this);
    gmodel.addGroupSelectionListener(this);

    tree.getSelectionModel().setSelectionMode
      (TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.addTreeSelectionListener(this);

    searchTF.addActionListener(this);
  }


  public void valueChanged(TreeSelectionEvent evt) {
    Object node = tree.getLastSelectedPathComponent();
    if (node == null) return;
    if (node instanceof Das2VersionTreeNode) {
      current_version = ((Das2VersionTreeNode)node).getVersionedSource();
      System.out.println(current_version);
      System.out.println("  clicked on Das2VersionTreeNode to select genome: " + current_version.getGenome().getID());
      setRegionsAndTypes();
    }
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == load_featuresB) {
      System.out.println("Das2LoadView received ActionEvent on load features button");
      loadFeaturesInView();
    }
    else if (src == searchTF) {
      String name = searchTF.getText();
      System.out.println("trying to search for annotation name: " + name);
      loadFeaturesByName(name);
      MutableAnnotatedBioSeq aseq = gmodel.getSelectedSeq();
      gviewer.setAnnotatedSeq(aseq, true, true);
    }
  }


  public void setRegionsAndTypes() {
    load_featuresB.setEnabled(false);
    types_table.setModel(empty_table_model);
    types_table.validate();
    types_table.repaint();

    final SwingWorker worker = new SwingWorker() {
	Map seqs = null;
	Map types = null;
	public Object construct() {
	  seqs = current_version.getRegions();
	  types = current_version.getTypes();
	  return null;
	}
	public void finished() {
	  /*
	   *  assumes that the types available for a given versioned source do not change
	   *    during the session
	   */
	  java.util.List type_states = 	(java.util.List)version2typestates.get(current_version);
	  if (type_states == null) {
	    type_states = new ArrayList();
            if (types != null) {
              Iterator iter = types.values().iterator();
              while (iter.hasNext()) {
                // need a map of Das2Type to Das2TypeState that persists for entire session,
                //    and reuse Das2TypeStates when possible (because no guarantee that
                //    Das2TypeState backing store has been updated during session)
                Das2Type dtype = (Das2Type)iter.next();
                Das2TypeState tstate = new Das2TypeState(dtype);
                type_states.add(tstate);
              }
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
          if (gviewer != null) {
            load_featuresB.setEnabled(true);
          }
	  // need to do this here within finished(), otherwise may get threading issues where
	  //    GroupSelectionEvents are being generated before group gets populated with seqs
	  System.out.println("gmodel selected group:  " + gmodel.getSelectedSeqGroup());
	  System.out.println("current_vers.getGenome: " + current_version.getGenome());
	  if (gmodel.getSelectedSeqGroup() != current_version.getGenome()) {
	    gmodel.setSelectedSeq(null);
	    System.out.println("setting selected group to : " + current_version.getGenome());
	    gmodel.setSelectedSeqGroup(current_version.getGenome());
	  }
	  else {
	    current_seq = gmodel.getSelectedSeq();
	    loadWholeSequenceAnnots();
	  }
	}
      };
    worker.start();
  }

  public void loadFeaturesByName(String name) {
    if (current_version != null) {
      // Das2VersionedSource.getFeaturesByName() should also add features as annotations to seqs...
      java.util.List feats = current_version.getFeaturesByName(name);
    }
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

    if (selected_seq == null) {
      ErrorHandler.errorPanel("ERROR", "You must first choose a sequence to display.");
      return;
    }
    if (visible_seq != selected_seq) {
      System.out.println("ERROR, VISIBLE SPAN DOES NOT MATCH GMODEL'S SELECTED SEQ!!!");
      return;
    }
    System.out.println("seq = " + visible_seq.getID() +
		       ", min = " + overlap.getMin() + ", max = " + overlap.getMax());

    // iterate through Das2TypeStates
    //    if off, ignore
    //    if load_in_visible_range, do range in view annotation request
    //    if per-seq, then should already be loaded?
    // maybe add a fully_loaded flag so know which ones to skip because they're done?

    java.util.List type_states = (java.util.List) version2typestates.get(current_version);
    ArrayList requests = new ArrayList();
    if (type_states != null) {
      Iterator titer = type_states.iterator();
      while (titer.hasNext()) {
        Das2TypeState tstate = (Das2TypeState)titer.next();
        Das2Type dtype = tstate.getDas2Type();
        if (tstate.getLoad() && tstate.getLoadStrategy() == Das2TypeState.VISIBLE_RANGE) {
          System.out.println("type to load for visible range: " + dtype.getID());
          Das2FeatureRequestSym request_sym =
            new Das2FeatureRequestSym(dtype, current_region, overlap, null);
          requests.add(request_sym);
        }
      }
    }
    if (requests.size() == 0) {
      ErrorHandler.errorPanel("Select some data", "You must first zoom in to " +
        "your area of interest and then select some data types "
        +"from the table above before pressing the \"Load\" button."); 
    }
    else {
      processFeatureRequests(requests, true);
    }
  }


  /**
   *  Takes a list of Das2FeatureRequestSyms, and pushes them through the Das2ClientOptimizer to
   *     make DAS/2 feature requests and load annotations from the response documents.
   *  Uses SwingWorker to run requests on a separate thread
   *  If update_display, then updates IGB's main view after annotations are loaded (on GUI thread)
   *
   *  could probably add finer resolution of threading here,
   *  so every request (one per type) launches on its own thread
   *  But for now putting them all on same (non-event) thread controlled by SwingWorker
   */
  public void processFeatureRequests(java.util.List requests, final boolean update_display) {
    final java.util.List request_syms = requests;
    if ((request_syms == null) || (request_syms.size() == 0)) { return; }
    SwingWorker worker = new SwingWorker() {
	public Object construct() {
	  for (int i=0; i<request_syms.size(); i++) {
	    Das2FeatureRequestSym request_sym = (Das2FeatureRequestSym)request_syms.get(i);
	    if (USE_DAS2_OPTIMIZER) {
	      Das2ClientOptimizer.loadFeatures(request_sym);
	    }
	    else {
	      request_sym.getRegion().getFeatures(request_sym);
	      MutableAnnotatedBioSeq aseq = request_sym.getRegion().getAnnotatedSeq();
	      aseq.addAnnotation(request_sym);
	    }
	  }
	  return null;
	}
	public void finished() {
	  if (update_display) {
	    if (USE_SIMPLE_VIEW) {
	      Das2FeatureRequestSym request_sym = (Das2FeatureRequestSym)request_syms.get(0);
	      MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)request_sym.getOverlapSpan().getBioSeq();
	      if (simple_viewer == null) { simple_viewer = GenometryViewer.displaySeq(aseq, false); }
	      simple_viewer.setAnnotatedSeq(aseq);
	    }
	    else if (gviewer != null) {
	      MutableAnnotatedBioSeq aseq = gmodel.getSelectedSeq();
	      gviewer.setAnnotatedSeq(aseq, true, true);
	    }
	  }
	}
      };

    if (THREAD_FEATURE_REQUESTS) {
      worker.start();
    }
    else {
      // if not threaded, then want to execute code in above subclass of SwingWorker, but within this thread
      //   so just ignore the thread features of SwingWorker and call construct() and finished() directly to
      //   to execute in this thread
      try {
	worker.construct();
	worker.finished();
      }
      catch (Exception ex) { ex.printStackTrace(); }
    }
  }

  /**
   *  Called when selected sequence is changed.
   *  Want to go through all previously visited
   *     DAS/2 versioned sources that share the seq's AnnotatedSeqGroup,
   *     For each (similar_versioned_source)
   *         for each type
   *            if (Das2TypeState set to AUTO_PER_SEQUENCE loading) && ( !state.fullyLoaded(seq) )
   *                 Do full feature load for seq
   *  For now assume that if a type's load state is not AUTO_PER_SEQUENCE, then no auto-loading, only
   *    manual loading, which is handled in another method...
   */
  public void seqSelectionChanged(SeqSelectionEvent evt) {
    if (DEBUG_EVENTS) {
      System.out.println("Das2LoadView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
    }
    AnnotatedBioSeq newseq = evt.getSelectedSeq();
    if (current_seq != newseq) {
      current_seq = newseq;
      loadWholeSequenceAnnots();
    }
  }

  protected void loadWholeSequenceAnnots() {
    if (current_seq == null)  { return; }
    if (current_version != null) {
      SeqSpan overlap = new SimpleSeqSpan(0, current_seq.getLength(), current_seq);
      current_region = current_version.getRegion(current_seq);
      java.util.List type_states = (java.util.List)version2typestates.get(current_version);
      Iterator titer = type_states.iterator();
      ArrayList requests = new ArrayList();
      while (titer.hasNext()) {
	Das2TypeState tstate = (Das2TypeState)titer.next();
	Das2Type dtype = tstate.getDas2Type();
	if (tstate.getLoad() && tstate.getLoadStrategy() == Das2TypeState.WHOLE_SEQUENCE)  {
	  System.out.println("type to load for entire sequence range: " + dtype.getID());
	  Das2FeatureRequestSym request_sym =
	    new Das2FeatureRequestSym(dtype, current_region, overlap, null);
	  requests.add(request_sym);
	}
      }

      if (requests.size() > 0) {
	processFeatureRequests(requests, true);
      }
    }
  }

  /**
   *  When selected group changed, want to go through all previously visited
   *     DAS/2 servers (starting with the current one), and try and find
   *     a versioned source that shares the selected AnnotatedSeqGroup.
   *  If found, take first found and set versioned source, source, and server accordingly
   *  If not found, blank out versioned source and source, and switch server to "Choose a server"
   *
   *  For now, just looking at current server
   */
  public void groupSelectionChanged(GroupSelectionEvent evt) {
    if (DEBUG_EVENTS)  {
      System.out.println("Das2LoadView received GroupSelectionEvent: " + evt);
    }
    AnnotatedSeqGroup newgroup = evt.getSelectedGroup();
    if (current_group != newgroup) {
      current_group = newgroup;
      if (current_server != null)  {
        current_version = current_server.getVersionedSource(current_group);
        if (current_version == null) {
          // reset
          current_server = null;
          current_source = null;
          // need to reset table also...
          types_table.setModel(empty_table_model);
          types_table.validate();
          types_table.repaint();
        }
        else {
          current_source = current_version.getSource();
          current_server = current_source.getServerInfo();
          System.out.println("   new das source: " + current_source.getID() +
                             ",  new das version: " + current_version.getID());
        }
      }
    }
  }


  public void tableChanged(TableModelEvent evt) {
    if (DEBUG_EVENTS)  {
      System.out.println("Das2LoadView received table model changed event: " + evt);
    }
    Das2TypesTableModel type_model = (Das2TypesTableModel)evt.getSource();
    int col = evt.getColumn();
    int firstrow = evt.getFirstRow();
    int lastrow = evt.getLastRow();
    Das2TypeState  tstate = type_model.getTypeState(firstrow);

    if ((col == Das2TypesTableModel.LOAD_STRATEGY_COLUMN) && (current_seq != null)) {
      // All attributes of TableModelEvent are in the TableModel coordinates, not
      // necessarily the same as the JTable coordinates, so use the model
      //      Object val = type_model.getValueAt(firstrow, col);
      //      System.out.println("value of changed table cell: " + val);

      SeqSpan overlap = new SimpleSeqSpan(0, current_seq.getLength(), current_seq);
      current_region = current_version.getRegion(current_seq);

      Das2Type dtype = tstate.getDas2Type();
      if (tstate.getLoad() && tstate.getLoadStrategy() == Das2TypeState.WHOLE_SEQUENCE)  {
	System.out.println("type to load for entire sequence range: " + dtype.getID());
	Das2FeatureRequestSym request_sym =
	  new Das2FeatureRequestSym(dtype, current_region, overlap, null);
	ArrayList requests = new ArrayList();
	requests.add(request_sym);
	processFeatureRequests(requests, true);
      }
    }
  }


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
 *  Relates a Das2Type to it's status in IGB.
 *  For example, whether it's load strategy is set to "full sequence"
 *  or "visible range", and possibly other details.
 */
class Das2TypeState {
  static boolean default_load = false;
  static String[] LOAD_STRINGS = new String[3];
  static int OFF = 0;
  static int VISIBLE_RANGE = 1;   // MANUAL_VISIBLE_RANGE
  static int WHOLE_SEQUENCE = 2;  // AUTO_WHOLE_SEQUENCE
  static int default_load_strategy = VISIBLE_RANGE;

  /*
   *  Want to retrieve type state from Preferences if possible
   *    node: ~/das2/server.root.url/typestate
   *    key: [typeid+"_loadstate"]  value: [load_state]     (load state is an integer??)
   */

  static Preferences root_node = UnibrowPrefsUtil.getTopNode();
  static Preferences das2_node = root_node.node("das2");

  static {
    LOAD_STRINGS[OFF] = "Off"; // OFF strategy is deprecated; use load=false
    LOAD_STRINGS[VISIBLE_RANGE] = "Visible Range";
    LOAD_STRINGS[WHOLE_SEQUENCE] = "Whole Sequence";
  }

  boolean load;
  int load_strategy;
  Das2Type type;
  Preferences lnode_strategy;
  Preferences lnode_load;

  public Das2TypeState(Das2Type dtype) {
    this.type = dtype;
    Das2VersionedSource version = type.getVersionedSource();
    Das2Source source = version.getSource();
    Das2ServerInfo server = source.getServerInfo();
    String server_root_url = server.getID();
    if (server_root_url.startsWith("http://")) { server_root_url = server_root_url.substring(7); }
    if (server_root_url.indexOf("//") > -1) {
      System.out.println("need to replace all double slashes in path!");
    }
    // String subnode_strategy = server_root_url + "/" + source.getID() + "/" + version.getID() + "/type_load_strategy";
    // String subnode_load = server_root_url + "/" + source.getID() + "/" + version.getID() + "/type_load";
    String base_node_id = version.getID();
    base_node_id = base_node_id.replaceAll("/{2,}", "/");
//    base_node_id.
    String subnode_strategy = base_node_id + "/type_load_strategy";
    String subnode_load = base_node_id + "/type_load";
    // System.out.println("subnode_strategy = " + subnode_strategy);
    //    System.out.println("subnode_load = " + subnode_load);
    //        System.out.println("subnode = " + subnode);
    //    System.out.println("    length: " + subnode.length());
    lnode_load = das2_node.node(subnode_load);
    lnode_strategy = das2_node.node(subnode_strategy);
    load = lnode_load.getBoolean(type.getID(), default_load);
    load_strategy = lnode_strategy.getInt(type.getID(), default_load_strategy);
    if (load_strategy == OFF) {
      // OFF strategy has been deprecated but may still exist in some user's prefs
      setLoadStrategy(default_load_strategy);
      setLoad(false);
    }
  }

  public void setLoad(boolean b) {
    load = b;
    lnode_load.putBoolean(type.getID(), load);
  }

  public boolean getLoad() {
    return load;
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
    lnode_strategy.putInt(type.getID(), strategy);
  }

  public int getLoadStrategy() { return load_strategy; }
  public String getLoadString() { return LOAD_STRINGS[load_strategy]; }
  public Das2Type getDas2Type() { return type; }
}


class Das2TypesTableModel extends AbstractTableModel   {
  static String[] column_names = { "load", "name", "ID", "ontology", "source", "load strategy" };
  static int LOAD_BOOLEAN_COLUMN = 0;
  static int NAME_COLUMN = 1;
  static int ID_COLUMN = 2;
  static int ONTOLOGY_COLUMN = 3;
  static int SOURCE_COLUMN = 4;
  static int LOAD_STRATEGY_COLUMN = 5;

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

  public Das2TypeState getTypeState(int row) {
    return (Das2TypeState)type_states.get(row);
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
    Das2TypeState state = getTypeState(row);
    Das2Type type = state.getDas2Type();
    if (col == NAME_COLUMN) {
      return type.getName();
    }
    else if (col == ID_COLUMN) {
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
    else if (col == LOAD_BOOLEAN_COLUMN) {
      return (state.getLoad() ? Boolean.TRUE : Boolean.FALSE);
    }
    return null;
  }

  public Class getColumnClass(int c) {
    return getValueAt(0, c).getClass();
  }

  public boolean isCellEditable(int row, int col) {
    if (col == LOAD_STRATEGY_COLUMN || col == LOAD_BOOLEAN_COLUMN) { return true; }
    else { return false; }
  }

  public void setValueAt(Object value, int row, int col) {
    //      System.out.println("Das2TypesTableModel.setValueAt() called, row = " + row +
    //			 ", col = " + col + "val = " + value.toString());
    Das2TypeState state = (Das2TypeState)type_states.get(row);
    if (col == LOAD_STRATEGY_COLUMN)  {
      state.setLoadStrategy(value.toString());
    }

    else if (col == LOAD_BOOLEAN_COLUMN) {
      Boolean bool = (Boolean)value;
      state.setLoad(bool.booleanValue());
    }

    fireTableCellUpdated(row, col);
  }
}


/**
 *  TreeNode wrapper around a Das2ServerInfo object.
 */
class Das2ServerTreeNode extends DataSourcesAbstractNode {
  Das2ServerInfo server;
  // using Vector instead of generic List because TreeNode interface requires children() to return Enumeration
  Vector child_nodes = null;

  public Das2ServerTreeNode(Das2ServerInfo server) {
    this.server = server;
  }

  public int getChildCount() {
    if (child_nodes == null) { populate(); }
    return child_nodes.size();
  }

  public TreeNode getChildAt(int childIndex) {
    if (child_nodes == null) { populate(); }
    return (TreeNode)child_nodes.get(childIndex);
  }

  public Enumeration children() {
    if (child_nodes == null) { populate(); }
    return child_nodes.elements();
  }

  /**
   *  First time children are accessed, this will trigger dynamic access to DAS2 server.
   */
  protected void populate() {
    if (child_nodes == null) {
      Map sources = server.getSources();
      child_nodes = new Vector(sources.size());
      Iterator iter = sources.values().iterator();
      while (iter.hasNext()) {
	Das2Source source = (Das2Source)iter.next();
	Das2SourceTreeNode child = new Das2SourceTreeNode(source);
	child_nodes.add(child);
      }
    }
  }

  public boolean getAllowsChildren() { return true; }
  public boolean isLeaf() { return false; }
  public String toString() { return server.getName(); }
  /** NOT YET IMPLEMENTED */
  public int getIndex(TreeNode node) {
    System.out.println("Das2ServerTreeNode.getIndex() called: " + toString());
    return -1;
  }
}

/**
 *  TreeNode wrapper around a Das2Source object.
 */
class Das2SourceTreeNode extends DataSourcesAbstractNode {
  Das2Source source;
  Vector version_nodes;

  public Das2SourceTreeNode(Das2Source source) {
    this.source = source;
    Map versions = source.getVersions();
    version_nodes = new Vector(versions.size());
    Iterator iter = versions.values().iterator();
    while (iter.hasNext()) {
      Das2VersionedSource version = (Das2VersionedSource)iter.next();
      Das2VersionTreeNode child = new Das2VersionTreeNode(version);
      version_nodes.add(child);
    }
  }
  public Das2Source getSource() { return source; }
  public int getChildCount() { return version_nodes.size(); }
  public TreeNode getChildAt(int childIndex) { return (TreeNode)version_nodes.get(childIndex); }
  public Enumeration children() { return version_nodes.elements(); }
  public boolean getAllowsChildren() { return true; }
  public boolean isLeaf() { return false; }
  public String toString() { return source.getName(); }
  /** NOT YET IMPLEMENTED */
  public int getIndex(TreeNode node) {
    System.out.println("Das2ServerTreeNode.getIndex() called: " + toString());
    return -1;
  }

}

/**
 * TreeNode wrapper around a Das2VersionedSource object.
 * Maybe don't really need this, since Das2VersionedSource could itself serve
 * as a leaf.
 */
class Das2VersionTreeNode extends DataSourcesAbstractNode {
  Das2VersionedSource version;

  public Das2VersionTreeNode(Das2VersionedSource version) { this.version = version; }
  public Das2VersionedSource getVersionedSource() { return version; }
  public String toString() { return version.getName(); }
  public boolean getAllowsChildren() { return false; }
  public boolean isLeaf() { return true; }
  // Das2VersionTreeNode cannot have children, so some TreeNode methods are just stubs
  public int getChildCount() { return 0; }
  public TreeNode getChildAt(int childIndex) { return null; }
  public Enumeration children() { return null; }
  public int getIndex(TreeNode node) { return -1; }
}

/**
 *   Stubs out MutableTreeNode methods that aren't used for Das2*Node objects.
 */
abstract class DataSourcesAbstractNode implements MutableTreeNode {
  TreeNode parent;
  public void insert(MutableTreeNode child, int index)  {}
  public void remove(int index) {}
  public void remove(MutableTreeNode node) {}
  public void removeFromParent() {}
  public void setParent(MutableTreeNode newParent) {
    this.parent = parent;
  }
  public TreeNode getParent() {
    return parent;
  }
  public void setUserObject(Object object) {}
}

