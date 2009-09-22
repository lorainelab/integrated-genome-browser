package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.sun.java.swing.plaf.windows.WindowsBorders.DashedBorder;
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
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * View of genome features as a tree.
 */
public final class FeatureTreeView extends JComponent {

	public final JScrollPane tree_scroller;
	private final JTree tree;
	private DefaultMutableTreeNode treetop = null;
	private static final String path_separator = "/";
	private static final Pattern path_separator_regex = Pattern.compile(path_separator);
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

		 //Enable tool tips.
		ToolTipManager.sharedInstance().registerComponent(tree);

		tcr = new FeatureTreeCellRenderer();
		tree.setCellRenderer(tcr);

		tce = new FeatureTreeCellEditor();
		tree.setCellEditor(tce);
		tree.setEditable(true);

		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);

		TreeMouseListener tree_mouse_listener = new TreeMouseListener();
		tree.addMouseListener(tree_mouse_listener);
		tree.addMouseMotionListener(tree_mouse_listener);

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
				if (/*!feature.visible &&*/feature.gVersion.gServer.equals(server)) {
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
			TreeNodeUserInfo featureUInfo = new TreeNodeUserInfo(feature, feature.isVisible());
			DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(featureName);
			newNode.setUserObject(featureUInfo);
			newNode.setAllowsChildren(false);	// this is a leaf.
			root.add(newNode);
			return;
		}

		// the recursive adding of non leaves
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
		GenericFeature dummyFeature = new GenericFeature(featureLeft, null, feature.gVersion);
		TreeNodeUserInfo dummyFeatureUInfo = new TreeNodeUserInfo(dummyFeature);
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(dummyFeatureUInfo);
		root.add(newNode);
		addOrFindNode(newNode, feature, featureRight);
	}

	class TreeMouseListener implements MouseListener, MouseMotionListener {

		private Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
		private Cursor defaultCursor = null;

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
				return serverFriendlyURL(nodeData, thetree, bounds, x, y);
			}
			if (nodeData instanceof GenericFeature) {
				return featureFriendlyURL(nodeData, bounds, x, y);
			}
			return null;

		}

		private URL featureFriendlyURL(Object nodeData, Rectangle bounds, int x, int y) {
			GenericFeature gFeature = (GenericFeature) nodeData;
			if (gFeature.friendlyURL != null) {
				int iconWidth = 10 + 2 * 4;
				bounds.x += bounds.width - iconWidth;
				bounds.width = iconWidth;
				if (bounds.contains(x, y)) {
					return gFeature.friendlyURL;
				}
			}
			return null;
		}

		private URL serverFriendlyURL(Object nodeData, JTree thetree, Rectangle bounds, int x, int y) {
			GenericServer gServer = (GenericServer) nodeData;
			if (gServer.friendlyURL != null) {
				Rectangle2D linkBound = thetree.getFontMetrics(thetree.getFont()).getStringBounds(gServer.serverName, thetree.getGraphics());
				bounds.width = (int) linkBound.getWidth();
				if (gServer.friendlyIcon != null) {
					bounds.x += gServer.friendlyIcon.getIconWidth() + 1;
				} else {
					bounds.x += 16;
				}
				if (bounds.contains(x, y)) {
					return gServer.friendlyURL;
				}
			}
			return null;
		}

		public void mouseClicked(MouseEvent e) {

			int x = e.getX();
			int y = e.getY();
			URL friendlyURL = getURLAt(e.getSource(), x, y);
			if (friendlyURL != null) {
				GeneralUtils.browse(friendlyURL.toString());
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
		private Color selectionBorderColor, selectionForeground;
		private Color selectionBackground, textForeground, textBackground;

		public FeatureTreeCellRenderer() {
			Font fontValue;
			fontValue = UIManager.getFont("Tree.font");
			if (fontValue != null) {
				leafCheckBox.setFont(fontValue);
			}

			setLeafIcon(null);
			
			selectionBorderColor = UIManager.getColor("Tree.selectionBorderColor");
			selectionForeground = UIManager.getColor("Tree.selectionForeground");
			selectionBackground = UIManager.getColor("Tree.selectionBackground");
			textForeground = UIManager.getColor("Tree.textForeground");
			textBackground = UIManager.getColor("Tree.textBackground");

			Boolean drawsFocusBorderAroundIcon = (Boolean) UIManager.get("Tree.drawsFocusBorderAroundIcon");
			leafCheckBox.setFocusPainted((drawsFocusBorderAroundIcon != null) && (drawsFocusBorderAroundIcon.booleanValue()));
			
			String osName = System.getProperty("os.name");
			if (osName != null && osName.indexOf("Windows") != -1) {
				leafCheckBox.setBorderPaintedFlat(true);
				leafCheckBox.setBorder(new DashedBorder(selectionBorderColor));
			}

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



			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			Object nodeUObject = node.getUserObject();
			Object genericData = nodeUObject;
			if (nodeUObject instanceof TreeNodeUserInfo) {
				genericData = ((TreeNodeUserInfo) nodeUObject).genericObject;
			}


			if (genericData instanceof GenericServer) {
				return renderServer(genericData, tree, sel, expanded, leaf, row, hasFocus);
			}
			if (leaf && genericData instanceof GenericFeature) {
				return renderFeature(tree, value, sel, expanded, leaf, row, hasFocus, genericData, nodeUObject);
			}

			return super.getTreeCellRendererComponent(
					tree, value, sel,
					expanded, leaf, row,
					hasFocus);
		}

		private Component renderFeature(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus, Object genericData, Object nodeUObject) {
			// You must call super before each return.
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			GenericFeature gFeature = (GenericFeature) genericData;
			boolean isChecked = ((TreeNodeUserInfo) nodeUObject).checked;
			String featureName = gFeature.featureName;
			String featureText = featureName.substring(featureName.lastIndexOf(path_separator) + 1);
			featureText = "<html>" + featureText;
			if (gFeature.friendlyURL != null) {
				java.net.URL imgURL = com.affymetrix.igb.IGB.class.getResource("info_icon.gif");
				if (imgURL != null) {
					ImageIcon infoIcon = new ImageIcon(imgURL);
					featureText += " <img src='" + infoIcon + "' width='10' height='10'/>";
				}
			}
			leafCheckBox.setText(featureText);
			leafCheckBox.setToolTipText(gFeature.description());
			leafCheckBox.setSelected(isChecked);
			leafCheckBox.setEnabled(tree.isEnabled() && !isChecked);
			if (selected) {
				leafCheckBox.setForeground(selectionForeground);
				leafCheckBox.setBackground(selectionBackground);
				leafCheckBox.setBorderPainted(true);
			} else {
				leafCheckBox.setForeground(textForeground);
				leafCheckBox.setBackground(textBackground);
				leafCheckBox.setBorderPainted(false);
			}
			return leafCheckBox;
		}

		private Component renderServer(Object genericData, JTree tree, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			GenericServer gServer = (GenericServer) genericData;
			String serverNameString = "";
			if (gServer.friendlyURL != null) {
				serverNameString = "<a href='" + gServer.friendlyURL + "'><b>" + gServer.serverName + "</b></a>";
			} else {
				serverNameString = "<b>" + gServer.serverName + "</b>";
			}
			serverNameString = "<html>" + serverNameString + " (" + gServer.serverType.toString() + ")";
			super.getTreeCellRendererComponent(tree, serverNameString, sel, expanded, leaf, row, hasFocus);
			if (gServer.friendlyIcon != null) {
				setIcon(gServer.friendlyIcon);
			}
			return this;
		}
	}

	private class FeatureTreeCellEditor extends AbstractCellEditor implements TreeCellEditor {

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

						JCheckBox checkbox = renderer.getLeafFeatureRenderer();
						checkbox.setText("");

						returnValue = editedNode.isLeaf() && nodeData instanceof GenericFeature && x > 0 && x < checkbox.getPreferredSize().width;
					}
				}
			}
			return returnValue;
		}

		public Component getTreeCellEditorComponent(final JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {

			Component editor = renderer.getTreeCellRendererComponent(tree, value, true, expanded, leaf,
					row, true);

			ItemListener itemListener = new ItemListener() {

				public void itemStateChanged(ItemEvent itemEvent) {
					Object nodeData = editedNode.getUserObject();
					if (nodeData instanceof TreeNodeUserInfo) {
						nodeData = ((TreeNodeUserInfo) nodeData).genericObject;
					}
					if (nodeData instanceof GenericFeature) {
						((GenericFeature) nodeData).setVisible();
						glv.createFeaturesTable(false);
					}
					tree.repaint();
					fireEditingStopped();
				}
			};

			if (editor instanceof JCheckBox) {
				((JCheckBox) editor).addItemListener(itemListener);
			}
			return editor;
		}

		public Object getCellEditorValue() {
			JCheckBox checkbox = renderer.getLeafFeatureRenderer();
			Object nodeData = editedNode.getUserObject();
			if (nodeData instanceof TreeNodeUserInfo) {
				((TreeNodeUserInfo) nodeData).setChecked(checkbox.isSelected());
			}
			return nodeData;
		}
	}

	static class TreeNodeUserInfo {

		private final Object genericObject;
		private boolean checked;

		public TreeNodeUserInfo(Object genericObject) {
			this(genericObject, false);
		}

		public TreeNodeUserInfo(Object genericObject, boolean checked) {
			this.checked = checked;
			this.genericObject = genericObject;
		}

		@Override
		public String toString() {
			return genericObject.toString();
		}

		public void setChecked(boolean newValue) {
			if (!checked) {
				checked = newValue;
			}
		}

		public boolean isChecked() {
			return checked;
		}
	}
}



