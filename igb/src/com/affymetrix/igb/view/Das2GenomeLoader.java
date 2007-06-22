package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.das2.*;

public class Das2GenomeLoader extends JComponent implements ActionListener {

  JButton okayB;
  JButton cancelB;
  JTree tree;  // DAS/2 server / source / version tree for selecting a genome
  DefaultMutableTreeNode treetop = null;
  JDialog dial;

  public static void showGenomeChooserDialog() {
    final JDialog dialog = new JDialog(IGB.getSingleton().getFrame(), "Genome Chooser", true);
    Das2GenomeLoader loader = new Das2GenomeLoader(dialog);
    dialog.setSize(new Dimension(300, 600));
    System.out.println("***** in showDasDialog(), showing dialog");
    dialog.show();
  }

  public Das2GenomeLoader(JDialog dialog) {
      dial = dialog;
      tree = new JTree();
      tree.setRootVisible(false);
      tree.setShowsRootHandles(true);
      treetop = new DefaultMutableTreeNode("DAS/2 Genome Servers");
      DefaultTreeModel tmodel = new DefaultTreeModel(treetop, true);
      tree.setModel(tmodel);
      tree.getSelectionModel().setSelectionMode
	(TreeSelectionModel.SINGLE_TREE_SELECTION);
      Iterator servers = Das2Discovery.getDas2Servers().values().iterator();
      while (servers.hasNext()) {
	Das2ServerInfo server = (Das2ServerInfo)servers.next();
	ServerNode server_node = new ServerNode(server);
	//	treetop.add(server_node);
	tmodel.insertNodeInto(server_node, treetop, treetop.getChildCount());
	if (server == Das2Discovery.getDas2Server(Das2Discovery.DEFAULT_DAS2_SERVER_NAME)) {
	  TreeNode[] path_array = server_node.getPath();
	  TreePath server_path = new TreePath(path_array);
	  expandAll(tree, server_path);
	}
      }

      okayB = new JButton("OK");
      cancelB = new JButton("Cancel");
      okayB.addActionListener(this);
      cancelB.addActionListener(this);
      JPanel pan1 = new JPanel(new GridLayout(1, 2));
      pan1.add(okayB);
      pan1.add(cancelB);
      JPanel ok_cancel_panel = new JPanel();
      ok_cancel_panel.add(pan1);
      dial.getContentPane().add("Center", tree);
      dial.getContentPane().add("South", ok_cancel_panel);
  }

  public void actionPerformed(ActionEvent evt) {
    SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    AnnotatedSeqGroup prev_genome = gmodel.getSelectedSeqGroup();
    Object src = evt.getSource();
    if (src == okayB) {
      dial.dispose();
      TreePath path = tree.getSelectionPath();
      if (path != null) {
	Object selection = path.getLastPathComponent();
	System.out.println("selection: " + selection);
	if (selection instanceof VersionNode) {
	  VersionNode vnode = (VersionNode)selection;
	  Das2VersionedSource version = vnode.getVersion();
	  // call to Das2VersionedSource.getSegments() triggers population of genome group with sequences if not already populated
	  Iterator segments = version.getSegments().values().iterator();
	  Das2Region first_region = (Das2Region)segments.next();
	  MutableAnnotatedBioSeq first_seq = first_region.getAnnotatedSeq();
	  AnnotatedSeqGroup new_genome = version.getGenome();
	  if (new_genome != prev_genome) {
	    System.out.println("CHOOSING GENOME: " + version);
	    gmodel.setSelectedSeqGroup(new_genome);
	    gmodel.setSelectedSeq(first_seq);
	  }
	}
      }
    }
    else if (src == cancelB) {
      dial.dispose();
    }
  }


/**
 *  TreeNode wrapper around a Das2ServerInfo object.
 */
class ServerNode extends LazyPopulatingTreeNode {
  Das2ServerInfo server;
  boolean populated = false;

  public ServerNode(Das2ServerInfo server) {
    super(server);
    this.server = server;
  }

  public Das2ServerInfo getServer() { return server; }
  protected boolean isPopulated() { return populated; }

  /**
   *  First time children are accessed, this will trigger dynamic access to DAS2 server if not already accessed
   */
  protected void populate() {
    if (! populated) {
      populated = true;
      Map sources = server.getSources();
      Iterator iter = sources.values().iterator();
      while (iter.hasNext()) {
	Das2Source source = (Das2Source)iter.next();
	SourceNode source_node = new SourceNode(source);
	add(source_node);
      }
    }
  }

}

/**
 *  TreeNode wrapper around a Das2Source object.
 */
class SourceNode extends LazyPopulatingTreeNode {
  Das2Source source;
  boolean populated = false;

  public SourceNode(Das2Source source) {
    super(source);
    this.source = source;
  }

  public Das2Source getSource() { return source; }
  protected boolean isPopulated() { return populated; }

  /**
   *  First time children are accessed, this will trigger dynamic access to DAS2 server if not already accessed
   */
  protected void populate() {
    if (! populated) {
      populated = true;
      Iterator versions = source.getVersions().values().iterator();
      while (versions.hasNext()) {
	Das2VersionedSource version = (Das2VersionedSource)versions.next();
	VersionNode version_node = new VersionNode(version);
	add(version_node);
      }
    }
  }

}

/** TreeNode for representing Das2VersionedSource */
class VersionNode extends DefaultMutableTreeNode {
  Das2VersionedSource version;

  public VersionNode(Das2VersionedSource version) {
    super(version);
    this.version = version;
    this.setAllowsChildren(false);
  }

  public Das2VersionedSource getVersion() { return version; }
}


  abstract class LazyPopulatingTreeNode extends DefaultMutableTreeNode {
    public LazyPopulatingTreeNode(Object userobj) {
      super(userobj);
    }
    public int getChildCount() {
      if (! isPopulated()) { populate(); }
      return super.getChildCount();
    }
    public TreeNode getChildAt(int childIndex) {
      if (! isPopulated()) { populate(); }
      return super.getChildAt(childIndex);
    }
    public Enumeration children() {
      if (! isPopulated()) { populate(); }
      return super.children();
    }
    abstract protected void populate();
    abstract protected boolean isPopulated();
  }


  /**
   *expand recursively a parent node and all its descendants
   */
  protected static void expandAll(JTree tree, TreePath parent) {
    // Traverse children
    TreeNode node = (TreeNode)parent.getLastPathComponent();
    if (node.getChildCount() >= 0) {
      for (Enumeration e=node.children(); e.hasMoreElements(); ) {
	TreeNode n = (TreeNode)e.nextElement();
	TreePath path = parent.pathByAddingChild(n);
	expandAll(tree, path);
      }
    }
    // Expansion or collapse must be done bottom-up
    tree.expandPath(parent);
  }

}


/*
class VersionNode extends DefaultMutableTreeNode {
  Das2VersionedSource version;

  public VersionNode(Das2VersionedSource version) { this.version = version; }
  public Das2VersionedSource getVersionedSource() { return version; }
  public String toString() { return version.getName(); }

  // using Vector instead of generic List because TreeNode interface requires children() to return Enumeration
  Vector child_nodes = null;

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
      Map types = version.getTypes();
      child_nodes = new Vector(types.size());
      Iterator iter = types.values().iterator();
      while (iter.hasNext()) {
	Das2Type type = (Das2Type)iter.next();
	TypeNode child = new TypeNode(type);
	child_nodes.add(child);
      }
    }
  }

  public boolean getAllowsChildren() { return true; }
  public boolean isLeaf() { return false; }
  public int getIndex(TreeNode node) {
    System.out.println("Das2VersionNode.getIndex() called: " + toString());
    return -1;
  }
}
*/
