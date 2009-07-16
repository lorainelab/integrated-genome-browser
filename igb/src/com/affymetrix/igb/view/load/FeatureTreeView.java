package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
//import skt.swing.tree.check.CheckTreeManager;
//import skt.swing.tree.check.TreePathSelectable;



/**
 * View of genome features as a tree.
 */
public final class FeatureTreeView extends JComponent {

	public final JScrollPane tree_scroller;
	private final JTree tree;
	private DefaultMutableTreeNode treetop = null;
	private static final String path_separator = "/";
	private static final Pattern path_separator_regex = Pattern.compile(path_separator);
//	private CheckTreeManager check_tree_manager; // manager for tree with checkboxes
//	private final TreeCheckListener tree_check_listener = new TreeCheckListener();
	private final GeneralLoadView glv;
	private TreePath selectedPath;
	private TreeCellRenderer tcr;
	private TreeCellEditor tce;

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
		tcr = new FeatureTreeCellRenderer();

		tree.setCellRenderer(tcr);

		tce = new FeatureTreeCellEditor();
		tree.setCellEditor(tce);
		tree.setEditable(true);


//		//if possible, hide leaf icons (since have checkboxes too)
//		if (tcr instanceof DefaultTreeCellRenderer) {
//			DefaultTreeCellRenderer dtcr = (DefaultTreeCellRenderer) tcr;
//			dtcr.setLeafIcon(null);
//		}

		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		//tree.setEditable(false);
		TreeMouseListener tree_mouse_listener = new TreeMouseListener();
		tree.addMouseListener(tree_mouse_listener);
		tree.addMouseMotionListener(tree_mouse_listener);

		// tree.addMouseMotionListener(tree_mouse_listener)


		//tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		//add a tree selection listener to the data access tree
//		tree.addTreeSelectionListener(new TreeSelectionListener() {
//
//			public void valueChanged(TreeSelectionEvent e) {
//				DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
//				if (node == null) {
//					return;
//				}
//				if (node.isLeaf()) {
//					//trigger the event
//					SingletonGenometryModel.getGenometryModel().setSelectedFeature(e.getPath());
//				}
//			}
//		});


//		// Only put checkboxes on leaf nodes.
//		TreePathSelectable only_leaf_nodes = new TreePathSelectable() {
//
//			public boolean isSelectable(TreePath path) {
//				DefaultMutableTreeNode last_node = (DefaultMutableTreeNode) path.getLastPathComponent();
//				return !last_node.getAllowsChildren();
//			}
//		};

		//check_tree_manager = new CheckTreeManager(tree, false, only_leaf_nodes);
		//check_tree_manager.getSelectionModel().addTreeSelectionListener(tree_check_listener);



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
	public void initOrRefreshTree(List<GenericFeature> features) {
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

//		// Sometimes the text of the nodes is too small.  This is a hack to deal with that.
//		if (tcr instanceof DefaultTreeCellRenderer) {
//			DefaultTreeCellRenderer dtcr = (DefaultTreeCellRenderer) tcr;
//
//			// determine longest component name (similar to tree depth, but more useful here).
//			int preferredSize = 0;
//			for (GenericFeature gFeature : features) {
//				preferredSize = Math.max(preferredSize, gFeature.toString().length());
//			}
//			preferredSize += 100;	//hack
//			preferredSize = Math.max(200, preferredSize);	// 200 is reasonable even for a small tree.
//
//			dtcr.setPreferredSize(new Dimension(preferredSize, 20));
//			dtcr.setMaximumSize(dtcr.getPreferredSize());
//
//		}
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

			serverRoot.setUserObject(new TreeNodeUserInfo(server));

			for (GenericFeature feature : features) {
				if (/*!feature.visible &&*/ feature.gVersion.gServer.equals(server)) {
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
			//This code adds a leaf
			//System.out.println("adding leaf : " + featureName);
			TreeNodeUserInfo featureUInfo = new TreeNodeUserInfo(feature, feature.visible);
			DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(featureName);
			newNode.setUserObject(featureUInfo);
			newNode.setAllowsChildren(false);	// this is a leaf.
			root.add(newNode);
			return;
		}

		// the recursaive adding of non leaves
		String featureLeft = featureName.substring(0, featureName.indexOf(path_separator));
		String featureRight = featureName.substring(featureName.indexOf(path_separator) + 1);
		Enumeration en = root.children();
		while (en.hasMoreElements()) {
			DefaultMutableTreeNode candidate = (DefaultMutableTreeNode) en.nextElement();
			Object nodeData = candidate.getUserObject();
			if (nodeData instanceof TreeNodeUserInfo) {
				nodeData = ((TreeNodeUserInfo) nodeData).genericObject;
			}
			GenericFeature candidateFeature = (GenericFeature) nodeData;
			String candidateName = candidateFeature.featureName;
			// See if this can go under a previous node.  Be sure we're working with the same version/server.
			if (candidateName.equals(featureLeft) && candidateFeature.gVersion.equals(feature.gVersion)) {
				addOrFindNode(candidate, feature, featureRight);
				return;
			}
		}

		// Couldn't find matching node.  Add new one.
		GenericFeature dummyFeature = new GenericFeature(featureLeft, feature.gVersion);
		TreeNodeUserInfo dummyFeatureUInfo = new TreeNodeUserInfo(dummyFeature);
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(dummyFeatureUInfo);
		root.add(newNode);
		addOrFindNode(newNode, feature, featureRight);
	}

	/**
	 * Remove node and move it to features table, if it's checked.
	 */
//
//	class TreeCheckListener implements TreeSelectionListener {
//
//		public void valueChanged(TreeSelectionEvent evt) {
//			TreePath path = evt.getPath();
//			DefaultMutableTreeNode tnode = (DefaultMutableTreeNode) path.getLastPathComponent();
//			GenericFeature gFeature = (GenericFeature) tnode.getUserObject();
//			if (!gFeature.visible) {
//				if (gFeature.loadStrategy == LoadStrategy.NO_LOAD) {
//					if (gFeature.gVersion.gServer.serverType == ServerType.DAS ||
//							gFeature.gVersion.gServer.serverType == ServerType.DAS2) {
//						gFeature.loadStrategy = LoadStrategy.VISIBLE;	// Put in "load region in view" by default in DAS/1 and DAS/2
//					}
//				}
//				gFeature.visible = true;
//			}
//
//			// Remove the node.
//			DefaultMutableTreeNode validNode = getNextValidNode(tnode);
//			TreePath validPath = null;
//			tnode.removeFromParent();
//
//			//  Select the next valid node, if one exists.
//			if (validNode != null) {
//				validPath = new TreePath(validNode);
//				selectedPath = validPath;
//				tree.expandPath(selectedPath);
//				tree.setSelectionPath(selectedPath);
//				tree.scrollPathToVisible(selectedPath);
//			} else {
//				selectedPath = null;
//			}
//
//			tree.updateUI();
//
//
//			// Need to trigger a change in the Features Table.
//			glv.createFeaturesTable();
//		}
//
//		/**
//		 * Find the next valid node.  Note that if there are no leaf nodes, we must return null.
//		 * @param currentNode
//		 * @return
//		 */
//		private DefaultMutableTreeNode getNextValidNode(DefaultMutableTreeNode currentNode) {
//			if (currentNode == null) {
//				return null;
//			}
//			DefaultMutableTreeNode validNode = currentNode.getNextSibling();
//			if (validNode == null) {
//				validNode = currentNode.getPreviousSibling();
//			}
//			return validNode;
//		}
//	}
//
	class TreeMouseListener implements MouseListener, MouseMotionListener {

		private Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
		private Cursor defaultCursor = null;
		private JTree tree;

		private URL getURLAt(Object source, int x, int y) {

			JTree thetree = (JTree) source;

			TreePath path = thetree.getClosestPathForLocation(x, y);
			if (path == null) {
				return null;
			}

			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			if (node == null) {
				return null;
			}

			Rectangle bounds = thetree.getPathBounds(path);
			if (bounds == null) {
				return null;
			}

			Object nodeData = node.getUserObject();
			if (nodeData instanceof TreeNodeUserInfo) {
				nodeData = ((TreeNodeUserInfo) nodeData).genericObject;
			}

			if (nodeData instanceof GenericServer) {
				GenericServer gServer = (GenericServer) nodeData;

				Rectangle2D linkBound = thetree.getFontMetrics(thetree.getFont()).getStringBounds(gServer.serverName, thetree.getGraphics());
				bounds.width = (int) linkBound.getWidth();
				if (gServer.friendlyURL != null) {
					if (gServer.friendlyIcon != null) {
						bounds.x += gServer.friendlyIcon.getIconWidth() + 1;
					} else {
						bounds.x += 16;
					}

					if (bounds.contains(x, y)) {
						return gServer.friendlyURL;
					}

				}
			} else if (nodeData instanceof GenericFeature) {
				GenericFeature gFeature = (GenericFeature) nodeData;

			}

			return null;

		}

		public void mouseClicked(MouseEvent e) {

			int x = e.getX();
			int y = e.getY();
			URL friendlyURL = getURLAt(e.getSource(), x, y);
			if (friendlyURL != null) {
				com.affymetrix.igb.util.WebBrowserControl.displayURLEventually(friendlyURL.toString());
			}
		}

		public void mouseMoved(MouseEvent e) {

			int x = e.getX();
			int y = e.getY();
			JTree thetree = (JTree) e.getSource();

			URL friendlyURL = getURLAt(thetree, x, y);
			if (friendlyURL != null) {
				thetree.setCursor(handCursor);
			} else {
				if (thetree.getCursor() != defaultCursor) {
					thetree.setCursor(defaultCursor);
				}
			}
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseDragged(MouseEvent e) {
		}
	}


	/*
	 * Some changes to enable checkboxes are from:
	 * http://www.experts-exchange.com/Programming/Languages/Java/Q_23851420.html
	 *
	 */


	class FeatureTreeCellRenderer extends DefaultTreeCellRenderer {

		private JCheckBox leafCheckBox = new JCheckBox();
		private Color selectionBorderColor,  selectionForeground;
		private Color selectionBackground,  textForeground,  textBackground;

		public FeatureTreeCellRenderer() {
			Font fontValue;
			fontValue = UIManager.getFont("Tree.font");
			if (fontValue != null) {
				leafCheckBox.setFont(fontValue);
			}

			setLeafIcon(null);

			Boolean drawsFocusBorderAroundIcon = (Boolean) UIManager.get("Tree.drawsFocusBorderAroundIcon");
			leafCheckBox.setFocusPainted((drawsFocusBorderAroundIcon != null) && (drawsFocusBorderAroundIcon.booleanValue()));

			selectionBorderColor = UIManager.getColor("Tree.selectionBorderColor");
			selectionForeground = UIManager.getColor("Tree.selectionForeground");
			selectionBackground = UIManager.getColor("Tree.selectionBackground");
			textForeground = UIManager.getColor("Tree.textForeground");
			textBackground = UIManager.getColor("Tree.textBackground");
		}

		public JCheckBox getLeafFeatureRenderer() {
			return leafCheckBox;
		}

		@Override
		public Component getTreeCellRendererComponent(
				JTree tree,
				Object value,
				boolean sel,
				boolean expanded,
				boolean leaf,
				int row,
				boolean hasFocus) {

			Component defaultRender =
					super.getTreeCellRendererComponent(
					tree, value, sel,
					expanded, leaf, row,
					hasFocus);

			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			Object nodeData = node.getUserObject();
			Object nodeGeneric = nodeData;
			if (nodeData instanceof TreeNodeUserInfo) {
				nodeGeneric = ((TreeNodeUserInfo) nodeData).genericObject;
			}


			if (nodeGeneric instanceof GenericServer &&
					((GenericServer) nodeGeneric).friendlyIcon != null) {
				setIcon(((GenericServer) nodeGeneric).friendlyIcon);
			} else if (leaf && nodeGeneric instanceof GenericFeature) {

				String featureName = ((GenericFeature) nodeGeneric).featureName;
				String featureRight = featureName.substring(featureName.indexOf(path_separator) + 1);

				leafCheckBox.setText(featureRight);
				leafCheckBox.setSelected(((TreeNodeUserInfo) nodeData).selected);

				leafCheckBox.setEnabled(tree.isEnabled() && !((TreeNodeUserInfo) nodeData).selected);

				if (selected) {
					leafCheckBox.setForeground(selectionForeground);
					leafCheckBox.setBackground(selectionBackground);
				} else {
					leafCheckBox.setForeground(textForeground);
					leafCheckBox.setBackground(textBackground);
				}
				return leafCheckBox;
			}

			return defaultRender;
		}
	}

	class FeatureTreeCellEditor extends AbstractCellEditor implements TreeCellEditor {

		FeatureTreeCellRenderer renderer = new FeatureTreeCellRenderer();
		DefaultMutableTreeNode editedNode;

		@Override
		public boolean isCellEditable(EventObject e) {
			boolean returnValue = false;
			JTree thetree = (JTree) e.getSource();
			if (e instanceof MouseEvent) {
				MouseEvent mouseEvent = (MouseEvent) e;
				TreePath path = thetree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
				if (path != null) {
					Object node = path.getLastPathComponent();

					if ((node != null) && (node instanceof DefaultMutableTreeNode)) {
						editedNode = (DefaultMutableTreeNode) node;
						Object nodeData = editedNode.getUserObject();
						if (nodeData instanceof TreeNodeUserInfo) {
							nodeData = ((TreeNodeUserInfo) nodeData).genericObject;
						}

						Rectangle r = thetree.getPathBounds(path);
						int x = mouseEvent.getX() - r.x;
						int y = mouseEvent.getY() - r.y;

						JCheckBox checkbox = renderer.getLeafFeatureRenderer();
						checkbox.setText("");

						returnValue = editedNode.isLeaf() && nodeData instanceof GenericFeature && x > 0 && x < checkbox.getPreferredSize().width;
					}
				}
			}
			System.out.println("1. isCellEditable: " + Boolean.toString(returnValue));
			return returnValue;
		}

		public Component getTreeCellEditorComponent(final JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {

			Component editor = renderer.getTreeCellRendererComponent(tree, value, true, expanded, leaf,
					row, true);

			ItemListener itemListener = new ItemListener() {

				public void itemStateChanged(ItemEvent itemEvent) {
					System.out.println("3.itemStateChanged:	 " + itemEvent.toString());
					Object nodeData = editedNode.getUserObject();
					if (nodeData instanceof TreeNodeUserInfo) {
						nodeData = ((TreeNodeUserInfo) nodeData).genericObject;
					}
					if (nodeData instanceof GenericFeature) {
						((GenericFeature)nodeData).visible = true;
						glv.createFeaturesTable(false);
					}
					//TreePath path = new TreePath(editedNode.getPath());
					//SingletonGenometryModel.getGenometryModel().setSelectedFeature(path);
					tree.repaint();
					fireEditingStopped();
				}
			};

			if (editor instanceof JCheckBox) {
				((JCheckBox) editor).addItemListener(itemListener);
			}
			System.out.println("2. getTreeCellEditorComponent: " + editor.toString());
			return editor;
		}

		public Object getCellEditorValue() {
			JCheckBox checkbox = renderer.getLeafFeatureRenderer();
			Object nodeData = editedNode.getUserObject();
			if (nodeData instanceof TreeNodeUserInfo) {
				((TreeNodeUserInfo) nodeData).setSelected(checkbox.isSelected());
			}
			System.out.println("4. getCellEditorValue: " + nodeData);
			return nodeData;
		}
	}

	static class TreeNodeUserInfo {

		private final Object genericObject;
		private boolean selected;

		public TreeNodeUserInfo(Object genericObject) {
			this.selected = false;
			this.genericObject = genericObject;
		}

		public TreeNodeUserInfo(Object genericObject, boolean selected) {
			this.selected = selected;
			this.genericObject = genericObject;
		}
		@Override
		public String toString() {
			return genericObject.toString();
		}

		public void setSelected(boolean newValue) {
			if (!selected) {
				selected = newValue;
			}
		}

		public boolean isSelected() {
			return selected;
		}
	}
}



