package com.affymetrix.genoviz.swing.recordplayback;

import java.awt.Rectangle;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.affymetrix.genoviz.util.Idable;

public class JRPTree extends JTree implements JRPHierarchicalWidget {
	private static final long serialVersionUID = 1L;
	private final String id;

	public JRPTree(String id) {
		super();
		this.id = id;
		init();
	}
	public JRPTree(String id, Hashtable<?,?> value) {
		super(value);
		this.id = id;
		init();
	}
	public JRPTree(String id, Object[] value) {
		super(value);
		this.id = id;
		init();
	}
	public JRPTree(String id, TreeModel newModel) {
		super(newModel);
		this.id = id;
		init();
	}
	public JRPTree(String id, TreeNode root) {
		super(root);
		this.id = id;
		init();
	}
	public JRPTree(String id, TreeNode root, boolean asksAllowsChildren) {
		super(root, asksAllowsChildren);
		this.id = id;
		init();
	}
	public JRPTree(String id, Vector<?> value) {
		super(value);
		this.id = id;
		init();
	}
    private void init() {
		ScriptManager.getInstance().addWidget(this);
		addTreeSelectionListener(new TreeSelectionListener() {
			@Override
	        public void valueChanged(TreeSelectionEvent e) {
	            DefaultMutableTreeNode nodes =
	            	(DefaultMutableTreeNode)getLastSelectedPathComponent();

	        
	            if (nodes == null) {
				}

	    
//	            Object nodeInfos = nodes.getUserObject();


	        }
	    });
	}

	@Override
	public String getId() {
		return id;
	}

    @Override
	public boolean consecutiveOK() {
		return true;
	}

    private DefaultMutableTreeNode getSubNode(DefaultMutableTreeNode node, String id) {
    	for (int i = 0; i < node.getChildCount(); i++) {
    		DefaultMutableTreeNode subNode = (DefaultMutableTreeNode)node.getChildAt(i);
       		Idable idable = (Idable)subNode.getUserObject();
       		if (idable != null && idable.getId().equals(id)) {
       			return subNode;
       		}
    	}
		return null;
    }
   
    private TreePath getTreePath(String subId) {
    	String[] ids = subId.split("\\.");
    	DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[ids.length + 1];
    	DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)getModel().getRoot();
    	nodes[0] = rootNode;
    	for (int i = 0; i < ids.length; i++) {
    		nodes[i + 1] = getSubNode(nodes[i], ids[i]);
    		if (nodes[i + 1] == null) {
    			return null;
    		}
    	}
    	return new TreePath(nodes);
	}

	@Override
	public SubRegionFinder getSubRegionFinder(String subId) {
    	final TreePath path = getTreePath(subId);
    	if (path == null) {
    		Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error sub path " + subId + "for JTree " + getClass().getName() + " was not found");
    		return null;
    	}
    	expandPath(path);
		return new SubRegionFinder() {
			@Override
			public Rectangle getRegion() {
				int row = getRowForPath(path);
				return getRowBounds(row);
			}
			
		};
	}
}
