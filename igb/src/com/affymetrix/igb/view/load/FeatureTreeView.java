
package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import skt.swing.tree.check.CheckTreeManager;
import skt.swing.tree.check.TreePathSelectable;

/**
 * View of genome features as a tree.
 */
public final class FeatureTreeView extends JComponent {
	public final JScrollPane tree_scroller;
	private final JTree tree;
	private DefaultMutableTreeNode treetop = null;
	private static final String path_separator = "/";
	private static final Pattern path_separator_regex = Pattern.compile(path_separator);
	private CheckTreeManager check_tree_manager; // manager for tree with checkboxes
	private final TreeCheckListener tree_check_listener = new TreeCheckListener();
	private final GeneralLoadView glv;
	private TreePath selectedPath;
	private TreeCellRenderer tcr;

	public FeatureTreeView(GeneralLoadView glv) {
		this.glv = glv;	// used to see feature table, which this is linked to.
		this.setLayout(new BorderLayout());

		JPanel tree_panel = new JPanel();
		tree_panel.setLayout(new BoxLayout(tree_panel, BoxLayout.Y_AXIS));
		tree_panel.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));
		JLabel genome_features_label = new JLabel("Choose Data Sources and Data Sets:");
		genome_features_label.setAlignmentX(0.0f);
		tree_panel.add(genome_features_label);
		tree_panel.setAlignmentX(0.0f);

		tree = new JTree();
		tcr = tree.getCellRenderer();
		// if possible, hide leaf icons (since have checkboxes too)
		if (tcr instanceof DefaultTreeCellRenderer) {
			DefaultTreeCellRenderer dtcr = (DefaultTreeCellRenderer) tcr;
			dtcr.setLeafIcon(null);
		}
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setEditable(false);

		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		// Only put checkboxes on leaf nodes.
		TreePathSelectable only_leaf_nodes = new TreePathSelectable(){
			public boolean isSelectable(TreePath path) {
				DefaultMutableTreeNode last_node = (DefaultMutableTreeNode)path.getLastPathComponent();
				return !last_node.getAllowsChildren();
			}
		} ;

		check_tree_manager = new CheckTreeManager(tree, false, only_leaf_nodes);
		check_tree_manager.getSelectionModel().addTreeSelectionListener(tree_check_listener);


		tree_scroller = new JScrollPane(tree);
		//tree_scroller.setMinimumSize(new Dimension(300, 0));
		tree_scroller.setPreferredSize(new Dimension(300, 0));
		tree_scroller.setAlignmentX(0.0f);
		clearTreeView();

		tree_panel.add(tree_scroller);
		this.add(tree_panel);

	}

	/**
	 * Clear the tree view.
	 */
	synchronized void clearTreeView() {
		treetop = new DefaultMutableTreeNode("");
		TreeModel tmodel = new DefaultTreeModel(treetop, true);
		tree.setModel(tmodel);
		tree_scroller.invalidate();
	}

	/**
	 * Initialize (or simply refresh) the tree.
	 * If a node is already selected (this could happen if the user used a leaf checkbox), then we don't need to do this.
	 * @param features
	 */
	public void initOrRefreshTree(List <GenericFeature>features) {
		if (selectedPath != null) {
			selectedPath = null;
			return;
			// Don't want to re-create tree when we have a selected node.
		}
		DefaultMutableTreeNode root = CreateTree(features);
		//this.setVisible(root != null && (root.getChildCount() > 0));
		treetop = root;
		TreeModel tmodel = new DefaultTreeModel(treetop, true);
		tree.setModel(tmodel);

		// Sometimes the text of the nodes is too small.  This is a hack to deal with that.
		if (tcr instanceof DefaultTreeCellRenderer) {
			DefaultTreeCellRenderer dtcr = (DefaultTreeCellRenderer) tcr;

			// determine longest component name (similar to tree depth, but more useful here).
			int preferredSize = 0;
			for (GenericFeature gFeature : features) {
				preferredSize = Math.max(preferredSize, gFeature.toString().length());
			}
			preferredSize+=100;	//hack
			preferredSize = Math.max(200, preferredSize);	// 200 is reasonable even for a small tree.

			dtcr.setPreferredSize(new Dimension(preferredSize, 20));
			dtcr.setMaximumSize(dtcr.getPreferredSize());
		}
		tree_scroller.invalidate();
	}


	/**
	 * Convert list of features into a tree.
	 * If a feature name has a slash (e.g. "a/b/c"), then it is to be represented as a series of nodes.
	 * Note that if a feature "a/b" is on server #1, and feature "a/c" is on server #2, then
	 * these features have distinct parents.
	 * @param features
	 * @return
	 */
	static DefaultMutableTreeNode CreateTree(List<GenericFeature> features) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("");

		if (features == null) {
			return root;
		}

		List<GenericServer> serverList = GeneralLoadUtils.getServersWithAssociatedFeatures(features);
		for (GenericServer server : serverList) {
			DefaultMutableTreeNode serverRoot = new DefaultMutableTreeNode(server.toString());
			serverRoot.setUserObject(server);
			for (GenericFeature feature : features) {
				if (!feature.visible && feature.gVersion.gServer.equals(server)) {
					addOrFindNode(serverRoot, feature, feature.featureName);
				}
			}
			if (serverRoot.getChildCount() > 0) {
				root.add(serverRoot);
			}

		}

		return root;
	}

	/**
	 * See if a node already exists for this feature's first "/".
	 * @param root
	 * @param featureName
	 * @return
	 */
	private static void addOrFindNode(DefaultMutableTreeNode root, GenericFeature feature, String featureName) {
		if (!featureName.contains(path_separator)) {
			//System.out.println("adding leaf : " + featureName);
			DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(feature);
			newNode.setAllowsChildren(false);	// this is a leaf.
			root.add(newNode);
			return;
		}
		String featureLeft = featureName.substring(0, featureName.indexOf(path_separator));
		String featureRight = featureName.substring(featureName.indexOf(path_separator) + 1);
		Enumeration en = root.children();
		while (en.hasMoreElements()) {
			DefaultMutableTreeNode candidate = (DefaultMutableTreeNode) en.nextElement();
			GenericFeature candidateFeature = (GenericFeature) candidate.getUserObject();
			String candidateName = candidateFeature.featureName;
			// See if this can go under a previous node.  Be sure we're working with the same version/server.
			if (candidateName.equals(featureLeft) && candidateFeature.gVersion.equals(feature.gVersion)) {
				addOrFindNode(candidate, feature, featureRight);
				return;
			}
		}

		// Couldn't find matching node.  Add new one.
		GenericFeature dummyFeature = new GenericFeature(featureLeft, feature.gVersion);
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(dummyFeature);
		root.add(newNode);
		addOrFindNode(newNode, feature, featureRight);
	}

	/**
	 * Remove node and move it to features table, if it's checked.
	 */
	class TreeCheckListener implements TreeSelectionListener {

		public void valueChanged(TreeSelectionEvent evt) {
			TreePath path = evt.getPath();
			DefaultMutableTreeNode tnode = (DefaultMutableTreeNode) path.getLastPathComponent();
			GenericFeature gFeature = (GenericFeature) tnode.getUserObject();
			gFeature.visible = true;

			// Remove the node.
			DefaultMutableTreeNode validNode = getNextValidNode(tnode);
			TreePath validPath = null;
			tnode.removeFromParent();

			//  Select the next valid node, if one exists.
			if (validNode != null) {
				validPath = new TreePath(validNode);
				selectedPath = validPath;
				tree.expandPath(selectedPath);
				tree.setSelectionPath(selectedPath);
				tree.scrollPathToVisible(selectedPath);
			} else {
				selectedPath = null;
			}
			
			tree.updateUI();
			

			// Need to trigger a change in the Features Table.
			glv.createFeaturesTable();		
			
		}

		/**
		 * Find the next valid node.  Note that if there are no leaf nodes, we must return null.
		 * @param currentNode
		 * @return
		 */
		private DefaultMutableTreeNode getNextValidNode(DefaultMutableTreeNode currentNode) {
			if (currentNode == null) {
				return null;
			}
			DefaultMutableTreeNode validNode = currentNode.getNextSibling();
			if (validNode == null) {
				validNode = currentNode.getPreviousSibling();
			}
			return validNode;
		}
	}
}



