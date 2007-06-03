package com.affymetrix.igb.view;

import java.util.*;
import javax.swing.treetable.*;
import javax.swing.table.*;
import javax.swing.tree.*;

import com.affymetrix.igb.das2.*;

public class Das2TypesTreeTableModel extends AbstractTreeTableModel  {
  static String[] column_names = { "name", "load", "ID", "ontology", "source", "range" };
  static int LOAD_BOOLEAN_COLUMN = 1;
  static int NAME_COLUMN = 0;
  static int ID_COLUMN = 2;
  static int ONTOLOGY_COLUMN = 3;
  static int SOURCE_COLUMN = 4;
  static int LOAD_STRATEGY_COLUMN = 5;

  static int model_count = 0;

  static DefaultMutableTreeNode default_root;

  static {
    default_root = new DefaultMutableTreeNode("Root Node");
    DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("Child1");
    DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("Child1");
    DefaultMutableTreeNode grandchildA = new DefaultMutableTreeNode("GrandChildA");
    child2.add(grandchildA);

    Das2ServerInfo dserver= (Das2ServerInfo)Das2Discovery.getDas2Servers().get("NetAffx");
    Das2Source dsrc = (Das2Source)dserver.getSources().get("http://netaffxdas.affymetrix.com/das2/H_sapiens");
    Das2VersionedSource vsrc = (Das2VersionedSource)dsrc.getVersions().get("http://netaffxdas.affymetrix.com/das2/H_sapiens_May_2004");
    Map dtypes = vsrc.getTypes();
    Iterator iter = dtypes.values().iterator();
    while  (iter.hasNext())  {
        Das2Type dtype = (Das2Type)iter.next();
        OldDas2TypeState dstate = new OldDas2TypeState(dtype);
        DefaultMutableTreeNode typenode = new DefaultMutableTreeNode(dstate);
        default_root.add(typenode);
    }

    System.out.println("@@@@@@@ DAS2VERSIONEDSOURCE: " + vsrc);

    default_root.add(child1);
    default_root.add(child2);
  }

  int model_num;
  java.util.List type_states;
  //  DefaultMutableTreeNode root = new DefaultMutableTreeNode("Annotation Types");

  public Das2TypesTreeTableModel(java.util.List states)  {
    super(default_root);
    model_num = model_count;
    model_count++;
    type_states = states;
    int col_count = column_names.length;
    int row_count = states.size();
  }

  // public void addTreeModelListener(TreeModelListener_l)   // handled in AbstractTreeTableModel
  // public void removeTreeModelListener(TreeModelListener_l)  // handled in AbstractTreeTableModel
  // Returns the child of parent at index index in the parent's child array.�
  public Object getChild(Object parent, int index)  {
    return ((TreeNode)parent).getChildAt(index);
  }
  // Returns the number of children of parent.
  public int getChildCount(Object parent) {
    return ((TreeNode)parent).getChildCount();
  }
  // Returns the index of child in parent.
  public int getIndexOfChild(Object parent, Object child)  {
    return ((TreeNode)parent).getIndex((TreeNode)child);
  }
  // Returns the root of the tree.
  //  public Object getRoot()  {
  //    return root;
  //  }
  //�Returns true if node is a leaf.
  public boolean isLeaf(Object node)  {
    System.out.println("in Das2TypeTreeTableModel.isLeaf(): " + node);
    if (node == null) { return true; }
    return ((TreeNode)node).isLeaf();
  }

  // Messaged when the user has altered the value for the item identified by path to newValue.
  public void valueForPathChanged(TreePath path, Object newValue)  {
  }

  /*
  public Das2TypeState getTypeState(int row) {
    return (Das2TypeState)type_states.get(row);
  }
  */

  public int getColumnCount() {
    return column_names.length;
  }

  public int getRowCount() {
    return type_states.size();
  }

  public String getColumnName(int col) {
    return column_names[col];
  }

  //  public Object getValueAt(int row, int col) {
  public Object getValueAt(Object node, int col) {
    Object result = null;
    if (node instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode tnode = (DefaultMutableTreeNode)node;
      Object obj = tnode.getUserObject();
      if (obj instanceof OldDas2TypeState) {
	//      Das2TypeState state = getTypeState(row);
	OldDas2TypeState state = (OldDas2TypeState)obj;
	if (state != null) {
	  Das2Type type = state.getDas2Type();
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
	}
      }
      //      else {
      //
      //      }
    }
    return result;
  }

  public Class getColumnClass(int col) {
    if (col == LOAD_BOOLEAN_COLUMN) { return Boolean.class; }
    else if (col == NAME_COLUMN) { return TreeTableModel.class; }
    else { return String.class; }
  }

  public boolean isCellEditable(int row, int col) {
    if (col == LOAD_STRATEGY_COLUMN || col == LOAD_BOOLEAN_COLUMN) { return true; }
    else { return false; }
  }

  public void setValueAt(Object value, int row, int col) {
    System.out.println("Das2TypesTableModel.setValueAt() called, row = " + row +
		       ", col = " + col + "val = " + value.toString());
    OldDas2TypeState state = (OldDas2TypeState)type_states.get(row);
    if (col == LOAD_STRATEGY_COLUMN)  {
      state.setLoadStrategy(value.toString());
    }

    else if (col == LOAD_BOOLEAN_COLUMN) {
      Boolean bool = (Boolean)value;
      state.setLoad(bool.booleanValue());
    }

    // fireTableCellUpdated(row, col);
  }
}
