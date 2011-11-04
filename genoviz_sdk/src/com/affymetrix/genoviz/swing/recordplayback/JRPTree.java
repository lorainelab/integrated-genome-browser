package com.affymetrix.genoviz.swing.recordplayback;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.affymetrix.common.Idable;

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
		RecordPlaybackHolder.getInstance().addWidget(this);
		addTreeSelectionListener(new TreeSelectionListener() {
			@Override
	        public void valueChanged(TreeSelectionEvent e) {
	            DefaultMutableTreeNode nodes =
	            	(DefaultMutableTreeNode)getLastSelectedPathComponent();

	        
	            if (nodes == null) return;

	    
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

    private Object getLoopSubObject(String[] ids, DefaultMutableTreeNode node) {
    	for (int i = 0; i < node.getChildCount(); i++) {
    		DefaultMutableTreeNode subNode = (DefaultMutableTreeNode)node.getChildAt(i);
       		Idable idable = (Idable)subNode.getUserObject();
       		if (idable != null && idable.getId().equals(ids[0])) {
       			if (subNode.isLeaf()) {
       				return idable;
       			}
       			else {
       				String[] subIds = Arrays.copyOfRange(ids, 1, ids.length);
       				return getLoopSubObject(subIds, subNode);
       			}
       		}
    	}
		return null;
    }
   
	public Object getSubObject(String subId) {
    	String[] ids = subId.split("\\");
    	DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)getModel().getRoot();
    	return getLoopSubObject(ids, rootNode);
	}

	@Override
	public JComponent getSubComponent(String subId) {
    	String[] ids = subId.split("\\");
    	TreePath path = new TreePath(ids);
    	expandPath(path);
		return null;
	}
}
