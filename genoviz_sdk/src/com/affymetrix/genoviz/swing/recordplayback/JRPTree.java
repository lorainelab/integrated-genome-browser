package com.affymetrix.genoviz.swing.recordplayback;

import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

public class JRPTree extends JTree implements JRPWidget {
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

	    
	            Object nodeInfos = nodes.getUserObject();


	        }
	    });
	}

	@Override
	public String getId() {
		return id;
	}
}
