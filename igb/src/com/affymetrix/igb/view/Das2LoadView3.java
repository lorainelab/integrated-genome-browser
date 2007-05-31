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

import java.awt.*;
import java.awt.event.*;
import java.net.URI;
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
import com.affymetrix.igb.tiers.AnnotStyle;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.swing.threads.SwingWorker;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import javax.swing.event.*;

import skt.swing.tree.check.CheckTreeManager;
import skt.swing.tree.check.CheckTreeSelectionModel;
import skt.swing.tree.check.TreePathSelectable;

/**
 *  New strategy for handling DAS/2 data
 *
 *  Choosing which genome to view is left to a different component
 *  Das2LoadView3 focuses on accessing annotation data (DAS/2 features and their types)
 *  Two main windows (possibly three)
 *    A) JTree
 *         Tree hierarchy DAS/2 server->source->version->types for all servers Das2Discover class knows about
 *         but filtered to only show those relevant to currently viewed genome
 *         Tree is pruned to only show paths with versions matching current genome
 *         Leafs are types with checkboxes for toggling loading
 *         Possibly checkboxes on version for allowing all/some/none of version's types to be loaded?
 *
 *    B) JTable
 *         Table of DAS/2 types that are marked for loading AND are from version that matches current genome,
 *         these are compiled from across all DAS/2 servers that Das2Discovery class knows about
 *         Synchronized selection between A) and B) ?
 *         includes load checkbox, unchecking removes type from table (or just unchecks?)
 *    [ C)  maybe ]
 *       XML renderer (tree) showing full XML for type selected in tree view (A)
 *
 *   The above strategy requires changes to how preferences for DAS2 servers/sources/versions/types are handled
 *     via Preferences nodes and UnibrowPrefsUtil
 */
public class Das2LoadView3 extends JComponent
  implements ActionListener, TableModelListener,
	     SeqSelectionListener, GroupSelectionListener,
             TreeSelectionListener, DataRequestListener {

  static boolean INCLUDE_NAME_SEARCH = false;
  static boolean USE_DAS2_OPTIMIZER = true;
  static boolean DEBUG_EVENTS = false;
  static boolean DEFAULT_THREAD_FEATURE_REQUESTS = true;

  static Das2TypesTableModel empty_table_model = new Das2TypesTableModel(new ArrayList());

  static SeqMapView gviewer = null;

  JTabbedPane tpane = new JTabbedPane();
  JTextField searchTF = new JTextField(40);
  JComboBox typestateCB;
  JTable types_table;
  JTable types_tree_table;
  JScrollPane table_scroller;
  JScrollPane tree_table_scroller;
  JTree tree;
  CheckTreeManager check_tree_manager; // manager for tree with checkboxes

  Map das_servers;
  Map version2typestates = new LinkedHashMap();

  Das2LoadView3 myself = null;
  Das2ServerInfo current_server;
  Das2Source current_source;
  Das2VersionedSource current_version;
  Das2Region current_region;

  static SingletonGenometryModel gmodel = IGB.getGenometryModel();
  AnnotatedSeqGroup current_group = null;
  AnnotatedBioSeq current_seq = null;

  public Das2LoadView3()  {
    myself = this;
    gviewer = IGB.getSingletonIGB().getMapView();
    gviewer.addDataRequestListener(this);

    tree = new JTree();
    /**
     *  If set TREE_DIG = true, then will need to debug 
     *    skt.swing.tree.check.CheckTreeSelectionModel.isDescendant(), which is throwing 
     *    ArrayOutOfBoundsExceptions when CheckTreeSelectionModel.addSelectionPaths() is called
     *    (looks like it needs a path length comparison added)
     */
    boolean TREE_DIG = false;
    TreePathSelectable threeplus = new TreePathSelectable(){
      public boolean isSelectable(TreePath path){
	return path.getPathCount() >= 5;
      }
    } ;
    check_tree_manager = new CheckTreeManager(tree, TREE_DIG, threeplus);

    typestateCB = new JComboBox();
    String[] load_states = Das2TypeState.LOAD_STRINGS;
    for (int i=1; i<load_states.length; i++) {
      typestateCB.addItem(load_states[i]);
    }
    types_table = new JTable();
    types_table.setModel(empty_table_model);
    table_scroller = new JScrollPane(types_table);


    ArrayList test_states = new ArrayList();
    JPanel types_tree_panel = null;

    this.setLayout(new BorderLayout());

    JPanel types_panel = new JPanel(new BorderLayout());
    types_panel.setBorder(new TitledBorder("Available Annotation Types"));

    JPanel namesearchP = new JPanel();

    namesearchP.add(new JLabel("name search: "));
    namesearchP.add(searchTF);
    types_panel.add("Center", table_scroller);
    final JSplitPane splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitpane.setOneTouchExpandable(true);
    splitpane.setLeftComponent(new JScrollPane(tree));
    splitpane.setRightComponent(types_panel);

    // As soon as this component becomes visible, set the splitpane position
    this.addComponentListener(new ComponentAdapter() {
      public void componentShown(ComponentEvent evt) {
        splitpane.setDividerLocation(0.35);
        // but only do this the FIRST time this component is made visible
        Das2LoadView3.this.removeComponentListener(this);
      }
    });

    this.add("Center", splitpane);

    gmodel.addSeqSelectionListener(this);
    gmodel.addGroupSelectionListener(this);

    tree.getSelectionModel().setSelectionMode
      (TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.addTreeSelectionListener(this);

    check_tree_manager.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
	public void valueChanged(TreeSelectionEvent evt) {
	  TreePath checkedPaths[] = check_tree_manager.getSelectionModel().getSelectionPaths();
	  int pcount = checkedPaths == null ? 0 : checkedPaths.length;
	  System.out.println("checked path count: " + pcount);
	  TreePath[] changed_paths = evt.getPaths();
	  int change_count = changed_paths.length;
	  System.out.println("    changed selection count: " + change_count); 
	  for (int i=0; i<change_count; i++) {
	    TreePath path = changed_paths[i];
	    boolean node_checked = evt.isAddedPath(i);
	    Object change_node = path.getLastPathComponent();
	    if (change_node instanceof DefaultMutableTreeNode) {
	      DefaultMutableTreeNode tnode = (DefaultMutableTreeNode)change_node;
	      Object userobj = tnode.getUserObject();
	      if (userobj instanceof Das2TypeState) {
		Das2TypeState tstate = (Das2TypeState)userobj;
		System.out.println("setting load state: " + node_checked + ", for type: " + tstate);
		tstate.setLoad(node_checked);
	      }
	    }
	  }
	}
      } ) ;

    searchTF.addActionListener(this);
  }


  public void valueChanged(TreeSelectionEvent evt) {
    //    System.out.println("TreeSelectionEvent: " + evt);
    Object node = tree.getLastSelectedPathComponent();

    // to get the multiple paths that are currently checked:
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
    if (src == searchTF) {
      String name = searchTF.getText();
      System.out.println("trying to search for annotation name: " + name);
      loadFeaturesByName(name);
      MutableAnnotatedBioSeq aseq = gmodel.getSelectedSeq();
      gviewer.setAnnotatedSeq(aseq, true, true);
    }
  }


  public void setRegionsAndTypes() {
    //    load_featuresB.setEnabled(false);
    types_table.setModel(empty_table_model);
    types_table.validate();
    types_table.repaint();

    final SwingWorker worker = new SwingWorker() {
	Map seqs = null;
	Map types = null;
	public Object construct() {
	  seqs = current_version.getSegments();
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
	  //          if (gviewer != null) { load_featuresB.setEnabled(true); }

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
    loadFeaturesInView(false);
  }

  public void loadFeaturesInView(boolean restrict_to_current_vsource) {
    MutableAnnotatedBioSeq selected_seq = gmodel.getSelectedSeq();
    if (! (selected_seq instanceof SmartAnnotBioSeq)) {
      ErrorHandler.errorPanel("ERROR", "selected seq is not appropriate for loading DAS2 data");
      return;
    }
    final SeqSpan overlap = gviewer.getVisibleSpan();
    final MutableAnnotatedBioSeq visible_seq = (MutableAnnotatedBioSeq)overlap.getBioSeq();
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
    SmartAnnotBioSeq aseq = (SmartAnnotBioSeq)selected_seq;
    AnnotatedSeqGroup genome = aseq.getSeqGroup();
    java.util.List vsources;

    // iterate through Das2TypeStates
    //    if off, ignore
    //    if load_in_visible_range, do range in view annotation request

    // maybe add a fully_loaded flag so know which ones to skip because they're done?
    if (restrict_to_current_vsource) {
      vsources = new ArrayList();
      vsources.add(current_version);
    }
    else {
      boolean FORCE_SERVER_LOAD = false;
      vsources = Das2Discovery.getVersionedSources(genome, true);
    }

    ArrayList requests = new ArrayList();

    for (int i=0; i<vsources.size(); i++) {
      Das2VersionedSource vsource = (Das2VersionedSource)vsources.get(i);
      if (vsource == null) { continue; }
      java.util.List type_states = (java.util.List) version2typestates.get(vsource);
      if (type_states == null) { continue; }
      Das2Region region = vsource.getSegment(aseq);
      Iterator titer = type_states.iterator();
      while (titer.hasNext()) {
	Das2TypeState tstate = (Das2TypeState)titer.next();
	Das2Type dtype = tstate.getDas2Type();
	//  only add to request list if set for loading and strategy is VISIBLE_RANGE loading
	if (tstate.getLoad() && tstate.getLoadStrategy() == Das2TypeState.VISIBLE_RANGE) {
	  System.out.println("type to load for visible range: " + dtype.getID());
	  Das2FeatureRequestSym request_sym =
	    new Das2FeatureRequestSym(dtype, region, overlap, null);
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
  public static void processFeatureRequests(java.util.List requests, final boolean update_display) {
    processFeatureRequests(requests, update_display, DEFAULT_THREAD_FEATURE_REQUESTS);
  }

  public static void processFeatureRequests(java.util.List requests, final boolean update_display, boolean thread_requests) {
    final java.util.List request_syms = requests;
    final java.util.List result_syms = new ArrayList();

    if ((request_syms == null) || (request_syms.size() == 0)) { return; }
    SwingWorker worker = new SwingWorker() {
	public Object construct() {
	  for (int i=0; i<request_syms.size(); i++) {
	    Das2FeatureRequestSym request_sym = (Das2FeatureRequestSym)request_syms.get(i);

            // Create an AnnotStyle so that we can automatically set the
            // human-readable name to the DAS2 name, rather than the ID, which is a URI
            Das2Type type = request_sym.getDas2Type();
            AnnotStyle style = AnnotStyle.getInstance(type.getID());
            style.setHumanName(type.getName());

            if (USE_DAS2_OPTIMIZER) {
	      result_syms.addAll(Das2ClientOptimizer.loadFeatures(request_sym));
	    }
	    else {
	      request_sym.getRegion().getFeatures(request_sym);
	      MutableAnnotatedBioSeq aseq = request_sym.getRegion().getAnnotatedSeq();
	      aseq.addAnnotation(request_sym);
              result_syms.add(request_sym);
	    }
	  }
	  return null;
	}

        public void finished() {
	  if (update_display && gviewer != null) {
	    MutableAnnotatedBioSeq aseq = gmodel.getSelectedSeq();
	    gviewer.setAnnotatedSeq(aseq, true, true);
	  }
	}
      };

    if (thread_requests) {
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
      System.out.println("Das2LoadView3 received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
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
      current_region = current_version.getSegment(current_seq);
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
    AnnotatedSeqGroup newgroup = evt.getSelectedGroup();
    if (newgroup == null)  {
        System.out.println("%%%%%%% Das2LoadView3 received GroupSelectionEvent:, group " + newgroup);
    }
    else {
        System.out.println( "%%%%%%% Das2LoadView3 received GroupSelectionEvent, group: " +
                newgroup.getID());
    }
    if (current_group != newgroup) {
      current_group = newgroup;
      redoTreeView();
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
      System.out.println("Das2LoadView3 received table model changed event: " + evt);
    }
    Das2TypesTableModel type_model = (Das2TypesTableModel)evt.getSource();
    int col = evt.getColumn();
    int firstrow = evt.getFirstRow();
    int lastrow = evt.getLastRow();
    Das2TypeState  tstate = type_model.getTypeState(firstrow);

    if ((current_seq != null) && (col == Das2TypesTableModel.LOAD_STRATEGY_COLUMN ||
         col == Das2TypesTableModel.LOAD_BOOLEAN_COLUMN)) {
      // All attributes of TableModelEvent are in the TableModel coordinates, not
      // necessarily the same as the JTable coordinates, so use the model
      //      Object val = type_model.getValueAt(firstrow, col);
      //      System.out.println("value of changed table cell: " + val);

      SeqSpan overlap = new SimpleSeqSpan(0, current_seq.getLength(), current_seq);
      current_region = current_version.getSegment(current_seq);

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

  public boolean dataRequested(DataRequestEvent evt) {
    System.out.println("Das2LoadView3 recieved DataRequestEvent: " + evt);
    loadFeaturesInView();
    return false;
  }

  /**
   *  Replace old JTree model with new model,
   *     filter based on currently selected genome (AnnotatedSeqGroup)
   */
  public void redoTreeView() {
    System.out.println("############## Das2LoadView3.redoTreeView() called #############");
    java.util.List versions = Das2Discovery.getVersionedSources(current_group, true);
    Iterator iter = versions.iterator();
    Set servers = new LinkedHashSet();
    Set sources = new LinkedHashSet();
    Map server2node = new LinkedHashMap();
    Map source2node = new LinkedHashMap();
    while (iter.hasNext()) {
      Das2VersionedSource version = (Das2VersionedSource)iter.next();
      System.out.println("####   version match: " + version.getID());
      Das2Source source = version.getSource();
      Das2ServerInfo server = source.getServerInfo();
      servers.add(server);
      sources.add(source);
    }

    DefaultMutableTreeNode top = new DefaultMutableTreeNode("DAS/2 Genome Servers");

    Iterator serveriter = servers.iterator();
    while (serveriter.hasNext()) {
      Das2ServerInfo server = (Das2ServerInfo)serveriter.next();
      //      Das2ServerTreeNode snode = new Das2ServerTreeNode(server);
      DefaultMutableTreeNode server_node = new DefaultMutableTreeNode(server);
      top.add(server_node);
      server2node.put(server, server_node);
    }

    Iterator sourceiter = sources.iterator();
    while (sourceiter.hasNext()) {
      Das2Source source = (Das2Source)sourceiter.next();
      Das2ServerInfo server = source.getServerInfo();
      DefaultMutableTreeNode server_node = (DefaultMutableTreeNode)server2node.get(server);
      //      Das2SourceNode source_node = new Das2SourceNode(source);
      DefaultMutableTreeNode source_node = new DefaultMutableTreeNode(source);
      server_node.add(source_node);
      source2node.put(source, source_node);
    }

    Iterator viter = versions.iterator();
    while (viter.hasNext()) {
      Das2VersionedSource version = (Das2VersionedSource)viter.next();
      Das2Source source = version.getSource();
      DefaultMutableTreeNode source_node = (DefaultMutableTreeNode)source2node.get(source);
      Das2VersionTreeNode version_node = new Das2VersionTreeNode(version, check_tree_manager);
      source_node.add(version_node);
    }
    TreeModel tmodel = new DefaultTreeModel(top, true);
    tree.setModel(tmodel);
  }

}  // END Das2LoadView3 class


/**
 *  Relates a Das2VersionedSource to it's status in IGB.
 *  For example, whether _any_ annotations should be loaded from it
 *    (overrides Das2TypeState.load for any Das2Types served from that Das2VersionedSource)
 */
class Das2VersionState {
  static Preferences root_node = UnibrowPrefsUtil.getTopNode();
  static Preferences das2_node = root_node.node("das2");
  static boolean default_load = true;

}

/**
 *  Relates a Das2Type to it's status in IGB.
 *  For example, whether it's load strategy is set to "full sequence"
 *  or "visible range", and possibly other details.
 */
class Das2TypeState {
  static boolean default_load = false;
  static String[] LOAD_STRINGS = new String[3];
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
    String base_node_id = version.getID();
    base_node_id = base_node_id.replaceAll("/{2,}", "/");
    String subnode_strategy = base_node_id + "/type_load_strategy";
    String subnode_load = base_node_id + "/type_load";
    // System.out.println("subnode_strategy = " + subnode_strategy);
    //    System.out.println("subnode_load = " + subnode_load);
    //        System.out.println("subnode = " + subnode);
    //    System.out.println("    length: " + subnode.length());

    try {
      lnode_load = UnibrowPrefsUtil.getSubnode(das2_node, subnode_load);
      lnode_strategy = UnibrowPrefsUtil.getSubnode(das2_node, subnode_strategy);


      String[] keys = lnode_load.keys();
      // alternative if each type is a node:  types_node.childrenNames()

      load = lnode_load.getBoolean(UnibrowPrefsUtil.shortKeyName(type.getID()), default_load);
      load_strategy = lnode_strategy.getInt(UnibrowPrefsUtil.shortKeyName(type.getID()), default_load_strategy);
    }
    catch (Exception ex) { ex.printStackTrace(); }
  }

  public void setLoad(boolean b) {
    load = b;
    lnode_load.putBoolean(UnibrowPrefsUtil.shortKeyName(type.getID()), load);
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
    lnode_strategy.putInt(UnibrowPrefsUtil.shortKeyName(type.getID()), strategy);
  }

  public int getLoadStrategy() { return load_strategy; }
  public String getLoadString() { return LOAD_STRINGS[load_strategy]; }
  public Das2Type getDas2Type() { return type; }
  public String toString() { return getDas2Type().toString(); }
}


class Das2TypesTableModel extends AbstractTableModel   {
  static String[] column_names = { "load", "name", "ID", "ontology", "source", "range" };
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
/*
class Das2ServerTreeNode extends DefaultMutableTreeNode {
  Das2ServerInfo server;
  // using Vector instead of generic List because TreeNode interface requires children() to return Enumeration
  Vector child_nodes = null;
  boolean filter_by_genome;


  public Das2ServerTreeNode(Das2ServerInfo server) {
    this(server, false);
  }

//    If filter_by_genome
  public Das2ServerTreeNode(Das2ServerInfo server, boolean filter_by_genome) {
    this.server = server;
    this.filter_by_genome = filter_by_genome;
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

  //  First time children are accessed, this will trigger dynamic access to DAS2 server.
  protected void populate() {
    if (child_nodes == null) {
      Map sources = server.getSources();
      child_nodes = new Vector(sources.size());
      Iterator iter = sources.values().iterator();
      while (iter.hasNext()) {
	Das2Source source = (Das2Source)iter.next();
	//	Map versions = source.getVersions();
	//	Iterator iter = versions.values().iterator();
	//	while (iter.hasNext()) {
	//	  Das2VersionedSource version = (Das2VersionedSource)iter.next();
	//	  if (version
	//	  Das2VersionTreeNode child = new Das2VersionTreeNode(version);
	//	  version_nodes.add(child);
	//	}
	Das2SourceTreeNode child = new Das2SourceTreeNode(source);
	child_nodes.add(child);
      }
    }
  }

  public boolean getAllowsChildren() { return true; }
  public boolean isLeaf() { return false; }
  public String toString() { return server.getName(); }
  // NOT YET IMPLEMENTED 
  public int getIndex(TreeNode node) {
    System.out.println("Das2ServerTreeNode.getIndex() called: " + toString());
    return super.getIndex(node);
  }
}
*/

/**
 *  TreeNode wrapper around a Das2Source object.
 */
/*
class Das2SourceTreeNode extends DefaultMutableTreeNode {
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
  // NOT YET IMPLEMENTED
  public int getIndex(TreeNode node) {
    System.out.println("Das2SourceTreeNode.getIndex() called: " + toString());
    return super.getIndex(node);
  }
}
*/

/**
 * TreeNode wrapper around a Das2VersionedSource object.
 * Maybe don't really need this, since Das2VersionedSource could itself serve
 * as a leaf.
 */
class Das2VersionTreeNode extends DefaultMutableTreeNode {
  Das2VersionedSource version;
  boolean populated = false;
  CheckTreeManager ctm;

  public Das2VersionTreeNode(Das2VersionedSource version, CheckTreeManager ctm) {
    this.version = version; 
    this.ctm = ctm;
  }
  public Das2VersionedSource getVersionedSource() { return version; }
  public String toString() { return version.getName(); }
  public boolean getAllowsChildren() { return true; }
  public boolean isLeaf() { return false; }

  public int getChildCount() {
    if (! populated) { populate(); }
    return super.getChildCount();
  }

  public TreeNode getChildAt(int childIndex) {
    if (! populated) { populate(); }
    return super.getChildAt(childIndex);
  }

  public Enumeration children() {
    if (! populated) { populate(); }
    return super.children();
  }

  /**
   *  First time children are accessed, this will trigger dynamic access to DAS2 server.
   *
   *  Need to add hierarchical types structure for type names that can be treated as paths...
   */
  protected synchronized void populate() {
    if (! populated) {
      populated = true;
      Map types = version.getTypes();
      //      child_nodes = new Vector(types.size());
      Iterator iter = types.values().iterator();
      while (iter.hasNext()) {
	Das2Type type = (Das2Type)iter.next();
	Das2TypeState tstate = new Das2TypeState(type);
	System.out.println("type: " + tstate);
	//	Das2TypeTreeNode child = new Das2TypeTreeNode(type);
	//	DefaultMutableTreeNode child = new DefaultMutableTreeNode(type);
	DefaultMutableTreeNode child = new DefaultMutableTreeNode(tstate);
	child.setAllowsChildren(false);
	//	child_nodes.add(child);
	this.add(child);
	if (tstate.getLoad()) {
	  System.out.println("  setting type to loaded");
	  TreePath child_path = new TreePath(child.getPath());
	  TreePath[] paths = new TreePath[1];
          paths[0] = child_path;
	  CheckTreeSelectionModel ctmodel = ctm.getSelectionModel();
	  ctmodel.addSelectionPaths(paths);
	}
      }
    }
  }

}


/*

class Das2TypeTreeNode extends DefaultMutableTreeNode {
  //  Das2TypeState type_state;
  Das2Type type;
  public Das2TypeTreeNode(Das2Type type) { this.type = type; }
  //  public Das2TypeState getTypeState() { return type_state; }
  //  public Das2Type getDas2Type(}) { return type_state.getDas2Type(); }
  public String toString() { return type.getName(); }
  public int getChildCount() { return 0; }
  public TreeNode getChildAt(int index) { return null; }
  public Enumeration children() { return null; }
  public boolean getAllowsChildren() { return false; }
  public boolean isLeaf() { return true; }

  //  public int getIndex(TreeNode node) {
  //    System.out.println("Das2TypeTreeNode.getIndex() called: " + toString());
  //    return super.getIndex(node);
  //  }
}

*/

