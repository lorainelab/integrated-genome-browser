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
  implements ActionListener,
	     // TableModelListener,
	     // TreeSelectionListener,
	     SeqSelectionListener,
	     GroupSelectionListener,
	     DataRequestListener {

  static boolean INCLUDE_NAME_SEARCH = false;
  static boolean USE_DAS2_OPTIMIZER = true;
  static boolean DEBUG_EVENTS = false;
  static boolean DEFAULT_THREAD_FEATURE_REQUESTS = true;
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
  Das2TypesTableModel types_table_model;
  DefaultMutableTreeNode treetop = null;

  Map das_servers;
  Map server2node = new HashMap();
  Map source2node = new HashMap();
  Map version2node = new HashMap();
  Map tstate2node = new LinkedHashMap();

  static SingletonGenometryModel gmodel = IGB.getGenometryModel();
  AnnotatedSeqGroup current_group = null;
  AnnotatedBioSeq current_seq = null;
  Das2VersionedSource current_version = null;

  TypesTreeCheckListener tree_check_listener = new TypesTreeCheckListener();

  public Das2LoadView3()  {
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
    types_table_model = new Das2TypesTableModel(check_tree_manager);

    typestateCB = new JComboBox();
    String[] load_states = Das2TypeState.LOAD_STRINGS;
    for (int i=1; i<load_states.length; i++) {
      typestateCB.addItem(load_states[i]);
    }
    types_table = new JTable();
    types_table.setModel(types_table_model);
    table_scroller = new JScrollPane(types_table);

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
    //    tree.addTreeSelectionListener(this);
    check_tree_manager.getSelectionModel().addTreeSelectionListener(tree_check_listener);
    searchTF.addActionListener(this);
  }

  //
  //  it would really be cleaner to tie the checkbox selection (isPathSelected())
  //    directly to Das2TypeState load field (getLoad()), but that would involve
  //    modifying source code in the MySwing code base to allow easier subclassing
  //    of CheckTreeManager and CheckTreeSelectionModel...
  //
  class TypesTreeCheckListener implements TreeSelectionListener {
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
	    boolean load = tstate.getLoad();
	    if (tstate.getLoad() != node_checked) {
	      tstate.setLoad(node_checked);
	    }
	    if (node_checked) {
	      if (types_table_model.getRow(tstate) < 0) {
		types_table_model.addTypeState(tstate);
	      }
	    }
	    else {
	      // if want removal from table
	      //   types_table_model.removeTypeState(tstate);
	      // if don't want removal, but rather update table render to reflect unchecked status, then
	      //   can rely on table being ChangeListener on Das2TypeState...
	    }
	  }
	}
      }
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


  public void loadFeaturesByName(String name) {
    if (current_version != null) {
      // Das2VersionedSource.getFeaturesByName() should also add features as annotations to seqs...
      java.util.List feats = current_version.getFeaturesByName(name);
    }
  }

  public void loadFeaturesInView() {
    loadFeatures(Das2TypeState.VISIBLE_RANGE, false);
  }

  public void loadFeatures(int load_strategy) {
    loadFeatures(load_strategy, false);
  }

  public void loadFeatures(int load_strategy, boolean restrict_to_current_version) {
    MutableAnnotatedBioSeq selected_seq = gmodel.getSelectedSeq();
    MutableAnnotatedBioSeq visible_seq = (MutableAnnotatedBioSeq)gviewer.getViewSeq();
    SeqSpan overlap;
    if (selected_seq == null) {
      ErrorHandler.errorPanel("ERROR", "You must first choose a sequence to display.");
      return;
    }
    if (! (selected_seq instanceof SmartAnnotBioSeq)) {
      ErrorHandler.errorPanel("ERROR", "selected seq is not appropriate for loading DAS2 data");
      return;
    }
    if (visible_seq != selected_seq) {
      System.out.println("ERROR, VISIBLE SPAN DOES NOT MATCH GMODEL'S SELECTED SEQ!!!");
      return;
    }

    if (load_strategy == Das2TypeState.VISIBLE_RANGE)  {
      overlap = gviewer.getVisibleSpan();
    }
    else if (load_strategy == Das2TypeState.WHOLE_SEQUENCE)  {
      overlap = new SimpleSeqSpan(0, selected_seq.getLength(), selected_seq);
    }
    else {
      ErrorHandler.errorPanel("ERROR", "Requested load strategy not recognized: " + load_strategy);
      return;
    }

    System.out.println("seq = " + visible_seq.getID() +
		       ", min = " + overlap.getMin() + ", max = " + overlap.getMax());
    ArrayList requests = new ArrayList();

    Iterator tstates = types_table_model.getTypeStates().iterator();
    while (tstates.hasNext()) {
      Das2TypeState tstate = (Das2TypeState)tstates.next();
      Das2Type dtype = tstate.getDas2Type();
      Das2VersionedSource version = dtype.getVersionedSource();
      // if restricting to types from "current" version, then skip if verion != current_version
      if (restrict_to_current_version && (version != current_version)) { continue; }

      Das2Region region = version.getSegment(selected_seq);
      if ((region != null)  &&
	  (tstate.getLoad()) &&
	  (tstate.getLoadStrategy() == Das2TypeState.VISIBLE_RANGE)) {
	// maybe add a fully_loaded flag so know which ones to skip because they're done?
	Das2FeatureRequestSym request_sym =
	  new Das2FeatureRequestSym(dtype, region, overlap, null);
	requests.add(request_sym);
      }
    }
    if (requests.size() > 0) {
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
   *            if (Das2TypeState set to WHOLE_SEQUENCE loading) && ( !state.fullyLoaded(seq) )
   *                 Do full feature load for seq
   *  For now assume that if a type's load state is not WHOLE_SEQUENCE, then no auto-loading, only
   *    manual loading, which is handled in another method...
   */
  public void seqSelectionChanged(SeqSelectionEvent evt) {
    if (DEBUG_EVENTS) {
      System.out.println("Das2LoadView3 received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
    }
    AnnotatedBioSeq newseq = evt.getSelectedSeq();
    if (current_seq != newseq) {
      current_seq = newseq;
      loadFeatures(Das2TypeState.WHOLE_SEQUENCE);  // load features with selected types whose load_strategy is WHOLE_SEQUENCE
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
    if ((current_group != newgroup) || (newgroup == null)) {
      current_group = newgroup;

      // need to reset table before populating tree 
      //    (because tree population may trigger table population)
      System.out.println("********** resetting table **********");
      types_table_model = new Das2TypesTableModel(check_tree_manager);
      types_table.setModel(types_table_model);
      types_table.validate();
      types_table.repaint();

      clearTreeView();
      java.util.List versions = Das2Discovery.getVersionedSources(current_group, true);
      //      redoTreeView(versions);
      Iterator viter = versions.iterator();
      while (viter.hasNext()) {
	Das2VersionedSource version = (Das2VersionedSource)viter.next();
	Das2VersionTreeNode version_node = addVersionToTree(version);
	boolean type_load = Das2TypeState.checkLoadStatus(version);
	if (type_load) {
	  int tcount = version_node.getChildCount();  // trigger types retrieval if not already done
	  System.out.println("%%%%%%   some types have load set in version: " + version.getID() +
			     ", types: " + tcount);
	}
      }
    }
  }



  /*  listening to events on DAS/2 server/source/version/type tree
  public void valueChanged(TreeSelectionEvent evt) {
    //    System.out.println("TreeSelectionEvent: " + evt);
  }
  */


  /* listening to events on DAS/2 loaded (and recently loaded) types table
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
      Das2Region region = current_version.getSegment(current_seq);

      Das2Type dtype = tstate.getDas2Type();
      if (tstate.getLoad() && tstate.getLoadStrategy() == Das2TypeState.WHOLE_SEQUENCE)  {
	System.out.println("type to load for entire sequence range: " + dtype.getID());
	Das2FeatureRequestSym request_sym =
	  new Das2FeatureRequestSym(dtype, region, overlap, null);
	ArrayList requests = new ArrayList();
	requests.add(request_sym);
	processFeatureRequests(requests, true);
      }
    }
  }
  */

  public boolean dataRequested(DataRequestEvent evt) {
    System.out.println("Das2LoadView3 recieved DataRequestEvent: " + evt);
    loadFeaturesInView();
    return false;
  }

  public synchronized void clearTreeView() {
    System.out.println("*********** clearTreeView() called *********");
    server2node.clear();
    source2node.clear();
    version2node.clear();
    treetop = new DefaultMutableTreeNode("DAS/2 Genome Servers");
    TreeModel tmodel = new DefaultTreeModel(treetop, true);
    tree.setModel(tmodel);
  }


  public synchronized Das2VersionTreeNode addVersionToTree(Das2VersionedSource version) {
    // for adding nodes to tree,
    //   using DefaultTreeModel.insertNodeInto() instead of DefaultMutableTreeNode.add()
    //   to ensure that JTree rendering is updated to reflect tree model changes
    Das2Source source = version.getSource();
    Das2ServerInfo server = source.getServerInfo();
    DefaultMutableTreeNode server_node = (DefaultMutableTreeNode)server2node.get(server);
    DefaultMutableTreeNode source_node = (DefaultMutableTreeNode)source2node.get(source);
    Das2VersionTreeNode version_node = (Das2VersionTreeNode)version2node.get(version);
    DefaultTreeModel tmodel = (DefaultTreeModel)tree.getModel();
    if (server_node == null) {
      server_node = new DefaultMutableTreeNode(server);
      server2node.put(server, server_node);
      //      treetop.add(server_node);
      tmodel.insertNodeInto(server_node, treetop, treetop.getChildCount());
    }
    if (source_node == null) {
      source_node = new DefaultMutableTreeNode(source);
      source2node.put(source, source_node);
      //      server_node.add(source_node);
      tmodel.insertNodeInto(source_node, server_node, server_node.getChildCount());

    }
    if (version_node == null) {
      version_node = new Das2VersionTreeNode(version);
      version2node.put(version, version_node);
      //      source_node.add(version_node);
      tmodel.insertNodeInto(version_node, source_node, source_node.getChildCount());
    }
    return version_node;
  }


  /**
   *
   * Das2VersionTreeNode
   *
   * Subclassing DefaultMutableTreeNode for representing Das2VersionedSource nodes in tree
   *   main reason for subclassing is to get dynamic addition of nodes representing Das2TypeStates
   */
  class Das2VersionTreeNode extends DefaultMutableTreeNode {
    Das2VersionedSource version;
    boolean populated = false;

    ChangeListener check_changer = new ChangeListener() {
	public void stateChanged(ChangeEvent evt) {
	  Object src = evt.getSource();
	  if (src instanceof Das2TypeState) {
	    Das2TypeState tstate = (Das2TypeState)src;
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode)tstate2node.get(tstate);
	    TreePath node_path = new TreePath(node.getPath());
	    CheckTreeSelectionModel ctmodel = check_tree_manager.getSelectionModel();
	    boolean checked = ctmodel.isPathSelected(node_path);
	    boolean load = tstate.getLoad();
	    if (checked != load) {
	      if (load) { ctmodel.addSelectionPath(node_path); }
	      else { ctmodel.removeSelectionPath(node_path); }
	    }
	  }
	}
    };

    public Das2VersionTreeNode(Das2VersionedSource version) {
      this.version = version;
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
	Iterator iter = types.values().iterator();
	while (iter.hasNext()) {
	  Das2Type type = (Das2Type)iter.next();
	  Das2TypeState tstate = Das2TypeState.getState(type);
	  System.out.println("type: " + tstate);
	  //	Das2TypeTreeNode child = new Das2TypeTreeNode(type);
	  DefaultMutableTreeNode child = new DefaultMutableTreeNode(tstate);
	  tstate2node.put(tstate, child);
	  child.setAllowsChildren(false);
	  this.add(child);
	  if (tstate.getLoad()) {
	    System.out.println("  setting type to loaded");
	    TreePath child_path = new TreePath(child.getPath());
	    CheckTreeSelectionModel ctmodel = check_tree_manager.getSelectionModel();
	    ctmodel.addSelectionPath(child_path);
	  }
	  tstate.addChangeListener(check_changer);
	}
      }
    }

  } // END Das2VersionTreeNode


}  // END Das2LoadView3 class


/**
 *
 *  Das2TypeState
 *
 *  Relates a Das2Type to it's status in IGB.
 *  For example, whether it's load strategy is set to "full sequence"
 *  or "visible range", and possibly other details.
 */
class Das2TypeState {
  static String[] LOAD_STRINGS = new String[3];
  static int VISIBLE_RANGE = 1;   // MANUAL_VISIBLE_RANGE
  static int WHOLE_SEQUENCE = 2;  // AUTO_WHOLE_SEQUENCE
  static boolean DEFAULT_LOAD = false;
  static int DEFAULT_LOAD_STRATEGY = VISIBLE_RANGE;
  static String LOADKEY = "load";
  static String STRATEGYKEY = "load_strategy";
  //  static String IDKEY = "id";
  static String TYPES_NODE_NAME = "types";

  /*
   *  Want to retrieve type state from Preferences if possible
   *    node: ~/das2/server.root.url/typestate
   *    key: [typeid+"_loadstate"]  value: [load_state]     (load state is an integer??)
   */
  static Preferences root_node = UnibrowPrefsUtil.getTopNode();
  static Preferences das2_node = root_node.node("das2");
  static Map type2state = new LinkedHashMap();

  static {
    LOAD_STRINGS[VISIBLE_RANGE] = "Visible Range";
    LOAD_STRINGS[WHOLE_SEQUENCE] = "Whole Sequence";
  }

  boolean load = DEFAULT_LOAD;
  int load_strategy = DEFAULT_LOAD_STRATEGY;
  Das2Type type;
  Preferences type_node;
  Preferences types_node;
  String type_name;
  ArrayList listeners = new ArrayList();


  public static Das2TypeState getState(Das2Type dtype) {
    Das2TypeState tstate = (Das2TypeState)type2state.get(dtype);
    if (tstate == null) {
      tstate = new Das2TypeState(dtype);
      type2state.put(dtype, tstate);
    }
    return tstate;
  }

  /**
   *  checks previous status (in Preferences) of Das2TypeStates, returns true if
   *  _any_ DAS/2 type for given Das2VersionedSource have stored preference of { load = true }
   */
  public static boolean checkLoadStatus(Das2VersionedSource version) {
    System.out.println("Das2LoadView3.checkLoadStatus() called, version: " + version.getID());
    try {
      Das2Source source = version.getSource();
      Das2ServerInfo server = source.getServerInfo();

      String server_name = server.getID().replaceAll("/", "%");
      String source_name = source.getID().replaceAll("/", "%");
      String version_name = version.getID().replaceAll("/", "%");

      Preferences server_node = UnibrowPrefsUtil.getSubnode(das2_node, server_name);
      Preferences source_node = UnibrowPrefsUtil.getSubnode(server_node, source_name);
      Preferences version_node = UnibrowPrefsUtil.getSubnode(source_node, version_name);
      Preferences types_node = UnibrowPrefsUtil.getSubnode(version_node, TYPES_NODE_NAME);

      String[] types = types_node.childrenNames();
      for (int i=0; i<types.length; i++) {
	String type_name = types[i];
	System.out.println("type: " + type_name);
	Preferences tnode = UnibrowPrefsUtil.getSubnode(types_node, type_name);
	if (tnode.getBoolean(LOADKEY, false)) { return true; }
      }
    }
    catch (Exception ex) { return false; }
    return false;
  }

  protected Das2TypeState(Das2Type dtype) {
    this.type = dtype;
    Das2VersionedSource version = type.getVersionedSource();
    Das2Source source = version.getSource();
    Das2ServerInfo server = source.getServerInfo();

    String server_name = server.getID().replaceAll("/", "%");
    String source_name = source.getID().replaceAll("/", "%");
    String version_name = version.getID().replaceAll("/", "%");
    type_name = type.getID().replaceAll("/", "%");

    try {
      Preferences server_node = UnibrowPrefsUtil.getSubnode(das2_node, server_name);
      Preferences source_node = UnibrowPrefsUtil.getSubnode(server_node, source_name);
      Preferences version_node = UnibrowPrefsUtil.getSubnode(source_node, version_name);
      types_node = UnibrowPrefsUtil.getSubnode(version_node, TYPES_NODE_NAME);
      if (types_node.nodeExists(type_name)) {
	type_node = UnibrowPrefsUtil.getSubnode(types_node, type_name);
	//	String id = type_node.get("id", null);
	load = type_node.getBoolean(LOADKEY, DEFAULT_LOAD);
	load_strategy = type_node.getInt(STRATEGYKEY, DEFAULT_LOAD_STRATEGY);
      }
    }
    catch (Exception ex) { ex.printStackTrace(); }
  }

  public void setLoad(boolean b) {
    if (load != b) {
      load = b;
      if (type_node == null) {
	type_node = UnibrowPrefsUtil.getSubnode(types_node, type_name);
      }
      type_node.putBoolean(LOADKEY, load);
      notifyChangeListeners();
    }
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
    if (load_strategy != strategy) {
      load_strategy = strategy;
      if (type_node == null) {
	type_node = UnibrowPrefsUtil.getSubnode(types_node, type_name);
      }
      type_node.putInt(STRATEGYKEY, load_strategy);
      notifyChangeListeners();
    }

  }

  public int getLoadStrategy() { return load_strategy; }
  public String getLoadString() { return LOAD_STRINGS[load_strategy]; }
  public Das2Type getDas2Type() { return type; }
  public String toString() { return getDas2Type().toString(); }

  public void addChangeListener(ChangeListener listener) { listeners.add(listener); }
  public void removeChangeListener(ChangeListener listener) { listeners.remove(listener); }
  public void notifyChangeListeners() {
    ChangeEvent evt = new ChangeEvent(this);
    Iterator iter = listeners.iterator();
    while (iter.hasNext()) {
      ChangeListener listener = (ChangeListener)iter.next();
      listener.stateChanged(evt);
    }
  }

}  // END Das2TypeState



/**
 *
 *  Das2TypesTableModel
 *
 */
class Das2TypesTableModel extends AbstractTableModel implements ChangeListener  {
  static String[] column_names = { "load", "name", "ID", "ontology", "source", "range", "vsource", "server" };
  static int LOAD_BOOLEAN_COLUMN = 0;
  static int NAME_COLUMN = 1;
  static int ID_COLUMN = 2;
  static int ONTOLOGY_COLUMN = 3;
  static int SOURCE_COLUMN = 4;
  static int LOAD_STRATEGY_COLUMN = 5;
  static int VSOURCE_COLUMN = 6;
  static int SERVER_COLUMN = 7;

  java.util.List type_states = new ArrayList();

  CheckTreeManager ctm;

  public Das2TypesTableModel(CheckTreeManager ctm) {
    this.ctm = ctm;
  }

  public boolean addTypeState(Das2TypeState state) {
    System.out.println("called Das2TypesTableModel.addTypeState(), state = " + state);
    int index = type_states.indexOf(state);
    if (index >= 0) { return false; }  // given state is already present in table model
    type_states.add(state);
    state.addChangeListener(this);
    int insert_index = type_states.size()-1;
    System.out.println("    fireTableRowsInserted: " + insert_index);
    fireTableRowsInserted(insert_index, insert_index);
    return true;
  }

  public boolean removeTypeState(Das2TypeState state) {
    int index = type_states.indexOf(state);
    if (index < 0) { return false; }  // couldn't find given state in table model
    type_states.remove(state);
    state.removeChangeListener(this);
    fireTableRowsDeleted(index, index);
    return true;
  }

  public Das2TypeState getTypeState(int row) {
    return (Das2TypeState)type_states.get(row);
  }

  public int getRow(Das2TypeState state) {
    return type_states.indexOf(state);
  }

  public java.util.List getTypeStates() { return type_states; }

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
    Object result = "NOT_ASSIGNED";
    if (col == NAME_COLUMN) {
      result = type.getName();
    }
    else if (col == ID_COLUMN) {
      result = type.getID();
    }
    else if (col == ONTOLOGY_COLUMN) {
      result = type.getOntology();
    }
    else if (col == SOURCE_COLUMN) {
      result = type.getDerivation();
    }
    else if (col == LOAD_STRATEGY_COLUMN) {
      result = state.getLoadString();
    }
    else if (col == LOAD_BOOLEAN_COLUMN) {
      result = (state.getLoad() ? Boolean.TRUE : Boolean.FALSE);
    }
    else if (col == VSOURCE_COLUMN) {
      result = type.getVersionedSource().getName();
    }
    else if (col == SERVER_COLUMN) {
      result = type.getVersionedSource().getSource().getServerInfo().getName();
    }
    //    System.out.println("Das2TypesTableModel.getValueAt() called, row = " + row + ", col = " + col +
    //		       ", value = " + result);
    return result;
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
    boolean changed = false;
    Das2TypeState state = (Das2TypeState)type_states.get(row);
    if (col == LOAD_STRATEGY_COLUMN)  {
      String new_strategy = value.toString();
      if (! (state.getLoadString().equals(new_strategy))) {
	state.setLoadStrategy(new_strategy);
	changed = true;
      }
    }

    else if (col == LOAD_BOOLEAN_COLUMN) {
      boolean new_load = ((Boolean)value).booleanValue();
      if (state.getLoad() != new_load) {
	state.setLoad(new_load);
	System.out.println("trying to set load boolean for type: " + state + ", " + new_load);
	System.out.println(ctm);
	changed = true;
      }
    }
    //  else { change = ???
    if (changed) {
      fireTableCellUpdated(row, col);
    }
  }

  public void stateChanged(ChangeEvent evt) {
    Object src = evt.getSource();
    if (src instanceof Das2TypeState) {
      System.out.println("Das2TypesTableModel.stateChanged() called, source: " + src);
      Das2TypeState tstate = (Das2TypeState)src;
      int row = getRow(tstate);
      if (row >=0) {  // if typestate is present in table, then send notification of row change
	fireTableRowsUpdated(row, row);
      }
    }
  }

}  // END Das2TypesTableModel


