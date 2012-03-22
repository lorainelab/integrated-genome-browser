package com.affymetrix.igb.view.load;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.genoviz.swing.recordplayback.JRPCheckBox;
import com.affymetrix.genoviz.swing.recordplayback.JRPTree;
import com.affymetrix.genoviz.util.Idable;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.sun.java.swing.plaf.windows.WindowsBorders.DashedBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
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
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.net.URI;
import java.util.StringTokenizer;

/**
 * View of genome features as a tree.
 */
public final class FeatureTreeView extends JComponent implements ActionListener, DragSourceListener {

	private static final long serialVersionUID = 1L;
	public final JScrollPane tree_scroller;
	private final JTree tree;
	private final JRPButton serverPrefsB;
	private static final String path_separator = "/";
	private final TreeCellRenderer tcr;
	private final TreeCellEditor tce;

	public FeatureTreeView() {
		this.setLayout(new BorderLayout());

		JLabel featuresLabel = new JLabel("Choose Data Sources and Data Sets:");
		featuresLabel.setPreferredSize(featuresLabel.getMinimumSize());
		featuresLabel.setAlignmentX(LEFT_ALIGNMENT);
		featuresLabel.setAlignmentY(TOP_ALIGNMENT);

		serverPrefsB = new JRPButton("DataAccess_configure", "Configure...");
		serverPrefsB.addActionListener(this);
		serverPrefsB.setToolTipText("Configure Data Sources");
		serverPrefsB.setMargin(new Insets(0, 0, 0, 0));
		serverPrefsB.setAlignmentY(TOP_ALIGNMENT);

		JPanel tree_panel = new JPanel();
		tree_panel.add(featuresLabel);
		tree_panel.setAlignmentX(LEFT_ALIGNMENT);
		tree_panel.setAlignmentY(TOP_ALIGNMENT);
		tree_panel.add(serverPrefsB);

		tree = new FeatureTree("FeatureTreeView_tree");

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
		tree_scroller.setAlignmentX(LEFT_ALIGNMENT);
		tree_scroller.setAlignmentY(TOP_ALIGNMENT);

		tree_panel.add(tree_scroller);

		GroupLayout layout = new GroupLayout(tree_panel);
		tree_panel.setLayout(layout);
		layout.setAutoCreateGaps(false);
		layout.setAutoCreateContainerGaps(false);

		layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(tree_scroller).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(featuresLabel)).addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(10)).addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(serverPrefsB)))));

		layout.setVerticalGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(featuresLabel).addComponent(serverPrefsB)).addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(tree_scroller)));

		this.add(tree_panel);
	}

	/**
	 * Handles clicking of server preferences button.
	 * @param evt
	 */
	public void actionPerformed(ActionEvent evt) {
		final Object src = evt.getSource();

		if (src == this.serverPrefsB) {
			// Go to server prefs tab.

			if (PreferencesPanel.TAB_DATALOAD_PREFS != -1) {
				PreferencesPanel pv = PreferencesPanel.getSingleton();
				pv.setTab(PreferencesPanel.TAB_DATALOAD_PREFS);	// Server preferences tab
				JFrame f = pv.getFrame();
				f.setVisible(true);
			} else {
				System.out.println("Data Load Preferences not instantiated");
			}
		}
	}

	/**
	 * Initialize (or simply refresh) the tree.
	 * If a node is already selected (this could happen if the user used a leaf checkbox), then we don't need to do this.
	 * @param features
	 */
	void initOrRefreshTree(final List<GenericFeature> features) {
		final TreeModel tmodel = new DefaultTreeModel(CreateTree(features), true);
		tree.setModel(tmodel);

		if (tree.getRowCount() > 0) {
			for (int i = 0; i < tree.getRowCount(); i++) {
				expand(tree, tree.getPathForRow(i));
			}
		}

		tree_scroller.invalidate();
	}

	private void expand(JTree tree, TreePath path) {
		if (path == null) {
			return;
		}

		TreeNode node = (TreeNode) path.getLastPathComponent();

		if (node.getChildCount() > 0) {
			@SuppressWarnings("unchecked")
			Enumeration<TreeNode> e = node.children();
			while (e.hasMoreElements()) {
				TreeNode n = e.nextElement();
				expand(tree, path.pathByAddingChild(n));
			}
		}

		if (node == null || !(node instanceof DefaultMutableTreeNode)) {
			return;
		}

		Object obj = ((DefaultMutableTreeNode) node).getUserObject();

		if (obj == null || !(obj instanceof TreeNodeUserInfo)) {
			return;
		}

		if (!((TreeNodeUserInfo) obj).checked) {
			return;
		}

		expand(path);
	}

	private void expand(TreePath path) {
		TreePath parentPath = path.getParentPath();

		if (parentPath != null) {
			expand(parentPath);
		}

		tree.expandPath(path);
	}

	/**
	 * Convert list of features into a tree.
	 * If a feature name has a slash (e.g. "a/b/c"), then it is to be represented as a series of nodes.
	 * Note that if a feature "a/b" is on server #1, and feature "a/c" is on server #2, then
	 * these features have distinct parents.
	 * @param features
	 * @return root which is of the type DefaultMutableTreeNode
	 */
	private static DefaultMutableTreeNode CreateTree(List<GenericFeature> features) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("");

		if (features == null || features.isEmpty()) {
			return root;
		}

		List<GenericServer> serverList = GeneralLoadUtils.getServersWithAssociatedFeatures(features);
		for (GenericServer server : serverList) {
			DefaultMutableTreeNode serverRoot = new DefaultMutableTreeNode(server.toString());

			serverRoot.setUserObject(new TreeNodeUserInfo(server));

			for (GenericFeature feature : features) {
				if (/*!feature.visible &&*/feature.gVersion.gServer.equals(server) /*&& canHandleFeature(feature)*/) {
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
	 * @param feature
	 * @param featureName
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

		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> en = root.children();	// no way to avoid compiler warning in Java 6

		while (en.hasMoreElements()) {
			DefaultMutableTreeNode candidate = en.nextElement();
			Object nodeData = candidate.getUserObject();
			if (nodeData instanceof TreeNodeUserInfo) {
				nodeData = ((TreeNodeUserInfo) nodeData).genericObject;
			}
			GenericFeature candidateFeature = (GenericFeature) nodeData;
			String candidateName = candidateFeature.featureName;
			// See if this can go under a previous node.  Be sure we're working with the same version/server.
			if (candidateName.equals(featureLeft) && candidateFeature.gVersion.equals(feature.gVersion)) {
				// Make sure we are really dealing with a non-leaf node.  This will
				// fix bug caused by name collision when a folder and feature are
				// named the same thing.
				if (candidate.getAllowsChildren()) {
					addOrFindNode(candidate, feature, featureRight);
					return;

				}
			}
		}

		boolean autoload = PreferenceUtils.getBooleanParam(
				PreferenceUtils.AUTO_LOAD, PreferenceUtils.default_auto_load);
		// Couldn't find matching node.  Add new one.
		// John -- not really sure what the following code is for. ?
		GenericFeature dummyFeature = new GenericFeature(featureLeft, null, feature.gVersion, null, null, autoload);
		TreeNodeUserInfo dummyFeatureUInfo = new TreeNodeUserInfo(dummyFeature);
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(dummyFeatureUInfo);
		root.add(newNode);
		addOrFindNode(newNode, feature, featureRight);
	}

	public void dragEnter(DragSourceDragEvent dsde) {
	}

	public void dragOver(DragSourceDragEvent dsde) {
	}

	public void dropActionChanged(DragSourceDragEvent dsde) {
	}

	public void dragExit(DragSourceEvent dse) {
	}

	public void dragDropEnd(DragSourceDropEvent dsde) {
	}

	//Ref : http://www.javalobby.org/java/forums/t19857.html
	public String getState() {
		int row = 0;
		StringBuilder buf = new StringBuilder();
		int rowCount = tree.getRowCount();
		for (int i = 0; i < rowCount; i++) {
			TreePath path = tree.getPathForRow(i);
			if (tree.isExpanded(path)) {
				buf.append(",").append(String.valueOf(i - row));
			}
		}

		buf.append(":");

		for (int i = 0; i < rowCount; i++) {
			TreePath path = tree.getPathForRow(i);
			if (!tree.isExpanded(path)) {
				buf.append(",").append(String.valueOf(i - row));
			}
		}
		return buf.toString();
	}

	public void restoreState(String state) {
		int colon = state.indexOf(":");
		String expand = state.substring(0, colon);
		String collapse = state.substring(colon + 1, state.length());

		// Collapse
		StringTokenizer stok = new StringTokenizer(collapse, ",");
		while (stok.hasMoreTokens()) {
			int token = Integer.parseInt(stok.nextToken());
			tree.collapseRow(token);
		}

		// Expand
		stok = new StringTokenizer(expand, ",");
		while (stok.hasMoreTokens()) {
			int token = Integer.parseInt(stok.nextToken());
			tree.expandRow(token);
		}
	}

	private class TreeMouseListener implements MouseListener, MouseMotionListener {

		private final Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
		private final Cursor defaultCursor = null;

		public void mouseClicked(MouseEvent e) {

			int x = e.getX();
			int y = e.getY();
			URL friendlyURL = getURLAt((JTree) e.getSource(), x, y);
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

	/**
	 * See if there is a hyperlink at this location.
	 * @param tree
	 * @param x
	 * @param y
	 * @return URL
	 */
	private static URL getURLAt(JTree tree, int x, int y) {

		TreePath path = tree.getClosestPathForLocation(x, y);
		if (path == null) {
			return null;
		}

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
		if (node == null) {
			return null;
		}

		Rectangle bounds = tree.getPathBounds(path);
		if (bounds == null) {
			return null;
		}
		if (!bounds.contains(x, y)) {
			return null;
		}

		Object nodeData = node.getUserObject();
		if (nodeData instanceof TreeNodeUserInfo) {
			nodeData = ((TreeNodeUserInfo) nodeData).genericObject;
		}

		if (nodeData instanceof GenericServer) {
			return serverFriendlyURL((GenericServer) nodeData, tree, bounds, x, y);
		}
		if (nodeData instanceof GenericFeature) {
			return featureFriendlyURL((GenericFeature) nodeData, bounds, x, y);
		}
		return null;

	}

	/**
	 * Find hyperlink for the feature name.
	 * @param gFeature
	 * @param bounds
	 * @param x
	 * @param y
	 * @return hyerlink for the feature name
	 */
	private static URL featureFriendlyURL(GenericFeature gFeature, Rectangle bounds, int x, int y) {
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

	/**
	 * Find hyperlink for the server name.
	 * @param gServer
	 * @param thetree
	 * @param bounds
	 * @param x
	 * @param y
	 * @return hyperlink of the server name
	 */
	private static URL serverFriendlyURL(GenericServer gServer, JTree thetree, Rectangle bounds, int x, int y) {
		if (!gServer.serverType.hasFriendlyURL()) {
			return null;	// TODO - hack to ignore server hyperlinks for DAS/1.
		}
		if (gServer.friendlyURL != null) {
			Rectangle2D linkBound = thetree.getFontMetrics(thetree.getFont()).getStringBounds(gServer.serverName, thetree.getGraphics());
			bounds.width = (int) linkBound.getWidth();
			if (gServer.getFriendlyIcon() != null) {
				bounds.x += gServer.getFriendlyIcon().getIconWidth() + 1;
			} else {
				bounds.x += 16;
			}

			if (bounds.contains(x, y)) {
				return gServer.friendlyURL;
			}
		}
		return null;


	}

	/*
	 * Some changes to enable checkboxes are from:
	 * http://www.experts-exchange.com/Programming/Languages/Java/Q_23851420.html
	 *
	 */
	private final static class FeatureTreeCellRenderer extends DefaultTreeCellRenderer {

		private static final long serialVersionUID = 1L;
		private static final Insets insets = new Insets(0, 0, 0, 0);
		private final JRPCheckBox leafCheckBox = new JRPCheckBox("FeatureTreeView_FeatureTreeCellRenderer") {

			private static final long serialVersionUID = 1L;

			@Override
			public Insets getInsets() {
				return insets;
			}
		};
		private final Color selectionBorderColor, selectionForeground;
		private final Color selectionBackground, textForeground, textBackground;

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

		public JRPCheckBox getLeafFeatureRenderer() {
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
				return renderServer((GenericServer) genericData, tree, sel, expanded, leaf, row, hasFocus);
			}
			if (leaf && genericData instanceof GenericFeature) {
				return renderFeature(tree, value, sel, expanded, leaf, row, hasFocus, (GenericFeature) genericData, nodeUObject);
			}

			return super.getTreeCellRendererComponent(
					tree, value, sel,
					expanded, leaf, row,
					hasFocus);
		}

		private Component renderFeature(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus, GenericFeature gFeature, Object nodeUObject) {
			// You must call super before each return.
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			boolean isChecked = ((TreeNodeUserInfo) nodeUObject).checked;
			String featureName = gFeature.featureName;
			String featureText = featureName.substring(featureName.lastIndexOf(path_separator) + 1);
			leafCheckBox.setId("FeatureTreeView_" + featureText.replaceAll(" ", "_"));
			featureText = "<html>" + featureText;
			if (gFeature.friendlyURL != null) {
				ImageIcon infoIcon = CommonUtils.getInstance().getIcon("images/info.png");
				featureText += " <img src='" + infoIcon + "' width=13' height='13'/>";
			}
			leafCheckBox.setText(featureText);
			leafCheckBox.setToolTipText(gFeature.description());
			leafCheckBox.setSelected(isChecked);
			//leafCheckBox.setEnabled(tree.isEnabled() && !isChecked);
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

		private Component renderServer(GenericServer gServer, JTree tree, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			String serverNameString = "";
			if (gServer.friendlyURL != null && gServer.serverType.hasFriendlyURL()) {
				// TODO - hack to ignore server hyperlinks for DAS/1.
				serverNameString = "<a href='" + gServer.friendlyURL + "'><b>" + gServer.serverName + "</b></a>";
			} else {
				serverNameString = "<b>" + gServer.serverName + "</b>";
			}
			serverNameString = "<html>" + serverNameString + " (" + gServer.serverType.getName() + ")";
			super.getTreeCellRendererComponent(tree, serverNameString, sel, expanded, leaf, row, hasFocus);
			if (gServer.getFriendlyIcon() != null) {
				setIcon(gServer.getFriendlyIcon());
			}
			return this;
		}
	}

	private final class FeatureTreeCellEditor extends AbstractCellEditor implements TreeCellEditor {

		private static final long serialVersionUID = 1L;

		public class FeatureLoadAction extends GenericAction {

			private static final long serialVersionUID = 1L;
			private final JRPCheckBox checkbox;

			private FeatureLoadAction(JRPCheckBox checkbox) {
				super();
				this.checkbox = checkbox;
			}

			@Override
			public String getText() {
				return null;
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				super.actionPerformed(e);
				Object nodeData = editedNode.getUserObject();
				if (nodeData instanceof TreeNodeUserInfo) {
					((TreeNodeUserInfo) nodeData).setChecked(checkbox.isSelected());
					TreeNodeUserInfo tn = (TreeNodeUserInfo) nodeData;
					if (tn.genericObject instanceof GenericFeature) {
						GenericFeature feature = (GenericFeature) tn.genericObject;
						if (checkbox.isSelected()) {
							GeneralLoadView.getLoadView().addFeature(feature);
						} else {
							String message = "Unchecking " + feature.featureName + " will remove all loaded data. \nDo you want to continue? ";
							if (feature.getMethods().isEmpty() || Application.confirmPanel(message, PreferenceUtils.getTopNode(),
									PreferenceUtils.CONFIRM_BEFORE_DELETE, PreferenceUtils.default_confirm_before_delete)) {
								GeneralLoadView.getLoadView().removeFeature(feature, true, false);
							} else {
								tn.setChecked(true);
							}
						}
					}
				}
			}

			@Override
			public Object getExtraInfo() {
				String extraInfo = "";
				Object nodeData = editedNode.getUserObject();
				if (nodeData instanceof TreeNodeUserInfo) {
					TreeNodeUserInfo tn = (TreeNodeUserInfo) nodeData;
					if (tn.genericObject instanceof GenericFeature) {
						GenericFeature feature = (GenericFeature) tn.genericObject;
						extraInfo = feature.gVersion.gServer.serverType + ":" + feature.gVersion.gServer.serverName + "." + feature.featureName;
					}
				}
				return extraInfo;
			}
		}
		FeatureTreeCellRenderer renderer;
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

						if (renderer == null) {
							renderer = new FeatureTreeCellRenderer();
							final JRPCheckBox checkbox = renderer.getLeafFeatureRenderer();
							checkbox.addActionListener(new FeatureLoadAction(checkbox));
						}
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
			return editedNode.getUserObject();
		}
	}

	public JTree getTree() {
		if (tree != null) {
			return tree;
		}
		return null;
	}

	public JRPButton getConfigureButton() {
		if (serverPrefsB != null) {
			return serverPrefsB;
		}
		return null;
	}

	private final static class TreeNodeUserInfo implements Idable {

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
			checked = newValue;
		}

		public String getId() {
			if (genericObject instanceof GenericServer) {
				return ((GenericServer) genericObject).serverType + ":" + ((GenericServer) genericObject).serverName;
			} else if (genericObject instanceof GenericFeature) {
				return ((GenericFeature) genericObject).featureName;
			}
			return null;
		}
	}

	private static class TransferableTreeNode implements Transferable {

		private DefaultMutableTreeNode data;
		private DataFlavor flavors[] = {DataFlavor.stringFlavor};

		public TransferableTreeNode(DefaultMutableTreeNode data) {
			this.data = data;
		}

		public Object getTransferData(DataFlavor flavor)
				throws UnsupportedFlavorException, IOException {
			Object returnObject = null;
			if (flavor.equals(DataFlavor.stringFlavor)) {
				TreeNodeUserInfo nodeData = (TreeNodeUserInfo) data.getUserObject();
				if (nodeData.genericObject instanceof GenericFeature) {
					GenericFeature feature = (GenericFeature) nodeData.genericObject;
					returnObject = "fromTree:" + feature.getURI().toString();
				}
			}

			return returnObject;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			if (flavor.equals(DataFlavor.stringFlavor)) {
				return true;
			}
			return false;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return flavors;
		}
	}

	private class FeatureTree extends JRPTree implements DragSourceListener, DragGestureListener {

		private static final long serialVersionUID = 1L;
		private final DragSource source;

		FeatureTree(String id) {
			super(id);

			source = new DragSource();
			source.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
		}

		public void dragGestureRecognized(DragGestureEvent dge) {
			TreePath path = getSelectionPath();

			if (path != null) {
				DefaultMutableTreeNode selection = (DefaultMutableTreeNode) path.getLastPathComponent();

				if (selection.isLeaf()) {

					TransferableTreeNode t = new TransferableTreeNode(selection);

					source.startDrag(dge, DragSource.DefaultMoveDrop, t, this);
				}
			}
		}

		public void dragEnter(DragSourceDragEvent dsde) {
		}

		public void dragOver(DragSourceDragEvent dsde) {
		}

		public void dropActionChanged(DragSourceDragEvent dsde) {
		}

		public void dragExit(DragSourceEvent dse) {
		}

		public void dragDropEnd(DragSourceDropEvent dsde) {
		}
	}

	public void updateTree(String url) throws URISyntaxException {
		URI uri = new URI(url);
		updateTree(uri);
	}

	public void updateTree(URI uri) {
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
		DefaultMutableTreeNode node;
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> nodes = root.breadthFirstEnumeration();
		GenericFeature feature = null;
		while (nodes.hasMoreElements()) {
			node = nodes.nextElement();
			Object nodeData = node.getUserObject();
			if (nodeData instanceof TreeNodeUserInfo) {
				TreeNodeUserInfo tn = (TreeNodeUserInfo) nodeData;
				if (tn.genericObject instanceof GenericFeature) {
					feature = (GenericFeature) tn.genericObject;
					if (!feature.isVisible()) {
						URI fUri = feature.getURI();
						if (uri.equals(fUri)) {
							GeneralLoadView.getLoadView().addFeature(feature);
						}
					}
				}
			}
		}
	}

	public GenericFeature isLoaded(URI uri) {
		List<GenericFeature> gFeatureList = GeneralLoadView.getLoadView().getLoadModeDataTableModel().features;

		if (gFeatureList == null) {
			return null;
		}

		for (GenericFeature gFeature : gFeatureList) {
			if (gFeature.getURI().equals(uri)) {
				return gFeature;
			}
		}

		return null;
	}

	public GenericFeature isContained(AnnotatedSeqGroup loadGroup, URI uri) {
		for (GenericVersion version : loadGroup.getAllVersions()) {
			for (GenericFeature feature : version.getFeatures()) {
				if (uri.equals(feature.getURI())) {
					return feature;
				}
			}
		}
		return null;
	}
}
