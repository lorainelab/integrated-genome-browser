/**
 * Copyright (c) 2001-2006 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.genoviz.swing.TreeTransferHandler;
import com.affymetrix.genoviz.swing.recordplayback.JRPTextField;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.FileTracker;
import com.google.common.collect.ImmutableListMultimap;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.undo.UndoManager;
import org.apache.commons.lang3.StringUtils;

/**
 * A panel for viewing and re-arranging bookmarks in a hierarchy.
 */
public final class BookmarkManagerView {

	private static JFileChooser static_chooser = null;
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bookmark");
	public JTree tree;
	public BottomThing thing;
	public final DefaultTreeModel tree_model = new DefaultTreeModel(null, true);
	// refresh_action is an action that is useful during debugging, but should go away later.
	public final Action properties_action;
	
	public JButton forwardButton = new JButton();
	public JButton backwardButton = new JButton();
	public JButton addBookmarkButton = new JButton();
	public JButton addSeparatorButton = new JButton();
	public JButton addFolderButton = new JButton();
	public JButton deleteBookmarkButton = new JButton();
	public List<TreePath> bookmark_history;
	public FileFilter ff = new TextExportFileFilter(new UniFileFilter(new String[]{"txt"}, "TEXT Files"));
	public FileFilter ff1 = new HTMLExportFileFilter(new UniFileFilter(new String[]{"html", "htm", "xhtml"}, "HTML Files"));
	public int history_pointer = -1;
	private final BookmarkTreeCellRenderer renderer;
	private static BookmarkManagerView singleton;
	protected int last_selected_row = -1;  // used by dragUnderFeedback()
	private boolean doNotShowWarning = false;
	private final BookmarkPropertiesGUI bpGUI;
	private BookmarkList selected_bl = null;
	public final IGBService igbService;
	
	private KeyAdapter kl = new KeyAdapter() {
		
		@Override
		public void keyReleased(KeyEvent ke) {
			if (ke.getKeyChar() == KeyEvent.VK_DELETE) {
				deleteAction();
			} else if (ke.getKeyCode() == KeyEvent.VK_ENTER) { // Only if ENTER is pressed
				goToAction();
			}
		}
	};
	
	private TreeSelectionListener tsl = new TreeSelectionListener() {
		
		@Override
		public void valueChanged(TreeSelectionEvent e) {
			if (e.getSource() != tree) {
				return;
			}

			if (tree.getSelectionCount() > 0) {
				if (tree.isRowSelected(0)) {
					deleteBookmarkButton.setEnabled(false);
				} else {
					deleteBookmarkButton.setEnabled(true);
				}
			}
			
			properties_action.setEnabled(false);
		
			TreePath[] selections = tree.getSelectionPaths();
			if (selections != null && selections.length == 1) {
				selected_bl = (BookmarkList) selections[0].getLastPathComponent();
				if (selected_bl != null && selected_bl.getUserObject() instanceof Bookmark) {
					properties_action.setEnabled(true);
				}
				thing.valueChanged();
			}
		}
	};
	
	private TableModelListener tml = new TableModelListener(){
		@Override
		public void tableChanged(TableModelEvent e) {
			thing.setInfoTableFromBookmark();
			thing.setDataListTableFromBookmark();
		}
	};
	
	public static void init(IGBService _igbService) {
		if (singleton == null) {
			singleton = new BookmarkManagerView(_igbService);
		}
	}

	public static synchronized BookmarkManagerView getSingleton() {
		return singleton;
	}

	/**
	 * Creates a new instance of Class
	 */
	private BookmarkManagerView(IGBService igbService) {
		this.igbService = igbService;
		tree = new JTree();
		tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new TreeTransferHandler());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
		tree.setModel(tree_model);
		bookmark_history = new ArrayList<TreePath>();

		thing = new BottomThing();
		bpGUI = new BookmarkPropertiesGUI(tml);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
		tree.setRootVisible(true);
		tree.setShowsRootHandles(true);
		tree.setEditable(false);

		renderer = new BookmarkTreeCellRenderer();
		tree.setCellRenderer(renderer);

		ToolTipManager.sharedInstance().registerComponent(tree);

		properties_action = makePropertiesAction();
	
		forwardButton.setEnabled(false);
		backwardButton.setEnabled(false);
		properties_action.setEnabled(false);

		initPopupMenu();

		tree.addKeyListener(kl);
		tree.addTreeSelectionListener(tsl);
	}

	public void insert(DefaultMutableTreeNode node) {
		if (tree.getSelectionCount() <= 0) {
			tree.setSelectionRow(0);
		}
		
		TreePath tree_path = tree.getSelectionModel().getSelectionPath();
		if (tree_path == null) {
			return;
		}
		
		DefaultMutableTreeNode tree_node = (DefaultMutableTreeNode) tree_path.getLastPathComponent();

		if (tree_node == null) {
			return;
		}

		DefaultMutableTreeNode parent;
		int row = tree.getRowForPath(tree_path);
		if (tree_node.getAllowsChildren() && dropInto(row)) {
			parent = tree_node;
		} else {
			parent = (DefaultMutableTreeNode) tree_node.getParent();
		}

		int index = parent.getChildCount();

		try {
			((DefaultTreeModel) tree.getModel()).insertNodeInto(node, parent, index);
			TreePath path = new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(node));
			tree.setSelectionPath(path);
		} catch (IllegalStateException e) {
			// Cancelled by user
		}
	}

	public void setBList(BookmarkList blist) {
		tree_model.setRoot(blist);
		tree.setSelectionRow(0);
		tree.clearSelection();
	}

	private void initPopupMenu() {
		final JPopupMenu popup = new JPopupMenu() {

			private static final long serialVersionUID = 1L;

			@Override
			public JMenuItem add(Action a) {
				JMenuItem menu_item = super.add(a);
				menu_item.setToolTipText(null);
				return menu_item;
			}
		};

		popup.add(properties_action);

		MouseAdapter mouse_adapter = new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				if (processDoubleClick(e)) {
					return;
				}

				if (popup.isPopupTrigger(e)) {
					TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
					if(path != null){
						tree.setSelectionPath(path);
						popup.show(tree, e.getX(), e.getY());
					}
				}
			}

			private boolean processDoubleClick(MouseEvent e) {
				if (e.getClickCount() != 2) {
					return false;
				}
				goToAction();
				return true;
			}
		};
		tree.addMouseListener(mouse_adapter);
	}

	/**
	 * Tries to import bookmarks into Unibrow. Makes use of {@link BookmarksParser#parse(BookmarkList, File)}.
	 */
	public void importBookmarks() {
		BookmarkList bookmark_list = new BookmarkList("Import");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String createdTime = dateFormat.format(Calendar.getInstance().getTime());
		bookmark_list.setComment("Created Time: " + createdTime);
		
		
		JFileChooser chooser = getJFileChooser(false);
		chooser.setDialogTitle("Import");
		chooser.setCurrentDirectory(getLoadDirectory());
		int option = chooser.showOpenDialog(null);
		if (option == JFileChooser.APPROVE_OPTION) {
			setLoadDirectory(chooser.getCurrentDirectory());
			try {
				File fil = chooser.getSelectedFile();
				BookmarksParser.parse(bookmark_list, fil);
				insert(bookmark_list);
			} catch (Exception ex) {
				ErrorHandler.errorPanel("Error importing bookmarks", ex, Level.SEVERE);
			}
		}
	}

	public void exportBookmarks() {
		BookmarkList main_bookmark_list;
		TreePath path = tree.getSelectionPath();
		//try {
	//		main_bookmark_list = (BookmarkList) path.getLastPathComponent(); // Export selected node
	//	} catch(NullPointerException ex) {
			main_bookmark_list = (BookmarkList) tree_model.getRoot(); // Export whole bookmarks if nothing selected#tK
	//	}
		
		if (main_bookmark_list == null) { // Support exporting from any node 
			ErrorHandler.errorPanel("No bookmarks to save", (Exception) null, Level.SEVERE);
			return;
		}
		JFileChooser chooser = getJFileChooser(true);
		chooser.setDialogTitle("Export");
		chooser.setCurrentDirectory(getLoadDirectory());
		int option = chooser.showSaveDialog(null);
		if (option == JFileChooser.APPROVE_OPTION) {
			try {
				setLoadDirectory(chooser.getCurrentDirectory());
				((ExportFileFilter) chooser.getFileFilter()).export(main_bookmark_list, chooser.getSelectedFile());
			} catch (Exception ex) {
				ErrorHandler.errorPanel("Error exporting bookmarks", ex, Level.SEVERE);
			}
		}
	}

	public void deleteAction() {
		TreePath[] selectionPaths = tree.getSelectionPaths();
		if (selectionPaths == null) {
			return;
		}
		int total = 0;
		for (TreePath path : selectionPaths) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			if(node.isLeaf()){
				total += 1;
			}else{
				total += node.getChildCount();
			}
		}
		Container frame = SwingUtilities.getAncestorOfClass(JFrame.class, tree);
		JCheckBox checkbox = PreferenceUtils.createCheckBox("Do not show this message again.", "BookmarkManagerView_showDialog", false);
		String message = "Delete these " + total + " selected items?";
		Object[] params = {message, checkbox};
		doNotShowWarning = checkbox.isSelected();
		if (!doNotShowWarning) {
			int yes = JOptionPane.showConfirmDialog(frame, params, "Delete?", JOptionPane.YES_NO_OPTION);
			doNotShowWarning = checkbox.isSelected();
			if (yes == JOptionPane.YES_OPTION) {
				for (int i = 0; i < selectionPaths.length; i++) {
					TreePath path = selectionPaths[i];
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
					if (node.getParent() != null) {
						tree_model.removeNodeFromParent(node);
						removeBookmarkFromHistory(path);
					}
				}
			}
		} else {
			for (int i = 0; i < selectionPaths.length; i++) {
				TreePath path = selectionPaths[i];
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				if (node.getParent() != null) {
					tree_model.removeNodeFromParent(node);
					removeBookmarkFromHistory(path);
				}
			}
		}
	}

	public Action getPropertiesAction() {
		return properties_action;
	}

	private Action makePropertiesAction() {
		Action a = new GenericAction("Properties ...", null, null) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent ae) {
				super.actionPerformed(ae);
				bpGUI.displayPanel(selected_bl);
			}
		};
		return a;
	}

	private void goToAction() {
		if (selected_bl != null && selected_bl.getUserObject() instanceof Bookmark) {
			Bookmark bm = (Bookmark) selected_bl.getUserObject();
			addBookmarkToHistory(tree.getSelectionPath());
			BookmarkController.viewBookmark(igbService, bm);
		}
	}
		
	private static TreePath getPath(TreeNode treeNode) {
		List<Object> nodes = new ArrayList<Object>();
		if (treeNode != null) {
			nodes.add(treeNode);
			treeNode = treeNode.getParent();
			while (treeNode != null) {
				nodes.add(0, treeNode);
				treeNode = treeNode.getParent();
			}
		}
		return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
	}

	private void addBookmarkToHistory(TreePath tp) {
		if (tp == null) {
			return;
		}
		int lastEntryIndex = bookmark_history.size() - 1;
		if (!bookmark_history.isEmpty()) {
			if (!bookmark_history.get(lastEntryIndex).equals(tp)) {
				bookmark_history.add(tp);
				history_pointer = bookmark_history.size() - 1;
				forwardButton.setEnabled(true);
				backwardButton.setEnabled(bookmark_history.size() > 1);
			}
		} else if (bookmark_history.isEmpty()) {
			bookmark_history.add(tp);
			history_pointer = bookmark_history.size() - 1;
			forwardButton.setEnabled(true);
			backwardButton.setEnabled(bookmark_history.size() > 1);
		}
	}

	private void removeBookmarkFromHistory(TreePath tp) {
		if (tp == null) {
			return;
		}
		int remove_pos = bookmark_history.indexOf(tp);
		if (remove_pos > -1) {
			if (history_pointer >= remove_pos) {
				history_pointer--;
			}
			bookmark_history.remove(remove_pos);
			forwardButton.setEnabled(history_pointer < bookmark_history.size() - 1);
			backwardButton.setEnabled(history_pointer > 0);
		}
	}

	private File getLoadDirectory() {
		return FileTracker.DATA_DIR_TRACKER.getFile();
	}

	private void setLoadDirectory(File file) {
		FileTracker.DATA_DIR_TRACKER.setFile(file);
	}

	/**
	 * Gets a static re-usable file chooser that prefers "html" files.
	 */
	private JFileChooser getJFileChooser(boolean export) {
		if (static_chooser == null) {
			static_chooser = new JFileChooser() {

				private static final long serialVersionUID = 1L;

				@Override
				public void approveSelection() {
					File f = getSelectedFile();
					String path;
					if ((f.getAbsolutePath().indexOf('.')) < 0) {
						if (static_chooser.getFileFilter().equals(ff)) {
							path = f.getAbsolutePath() + ".txt";
						} else {
							path = f.getAbsolutePath() + ".html";
						}
						f = new File(path);
					}
					if (f.exists() && getDialogType() == SAVE_DIALOG) {
						int result = JOptionPane.showConfirmDialog(this,
								"The file exists, overwrite?", "Existing file",
								JOptionPane.YES_NO_OPTION);
						switch (result) {
							case JOptionPane.YES_OPTION:
								super.approveSelection();
								return;
							case JOptionPane.NO_OPTION:
								return;
						}
					}
					super.approveSelection();
				}
			};
			ff1 = new HTMLExportFileFilter(new UniFileFilter(
					new String[]{"html", "htm", "xhtml"}, "HTML Files"));
			static_chooser.setAcceptAllFileFilterUsed(false);
			static_chooser.setCurrentDirectory(getLoadDirectory());
			static_chooser.addChoosableFileFilter(ff1);

		}
		if (export) {
			static_chooser.addChoosableFileFilter(ff);
		} else {
			static_chooser.removeChoosableFileFilter(ff);
		}
		static_chooser.setFileFilter(ff1);
		static_chooser.rescanCurrentDirectory();
		return static_chooser;
	}

	/**
	 * Returns true or false to indicate that if an item is inserted at the
	 * given row it will be inserted "into" (true) or "after" (false) the item
	 * currently at that row. Will return true only if the given row contains a
	 * folder and that folder is currently expanded or empty or is the root
	 * node.
	 */
	private boolean dropInto(int row) {
		boolean into = false;
		TreePath path = tree.getPathForRow(row);
		if (path == null) {
			// not necessarily an error
			return false;
		}
		if (row == 0) { // node is root [see DefaultMutableTreeNode.isRoot()]
			into = true;
		} else if (tree.isExpanded(path)) {
			into = true;
		} else {
			TreeNode node = (TreeNode) path.getLastPathComponent();
			if (node.getAllowsChildren()) {
				into = true;
			}
		}
		return into;
	}

	public void destroy() {
//    this.setApplication(null);
		tree.removeTreeSelectionListener(tsl);
		thing = null;
		tree = null;
	}

	public void addTreeModelListener(TreeModelListener tml){
		tree_model.addTreeModelListener(tml);
	}
	
	/**
	 * A JPanel that listens for TreeSelectionEvents, displays the name(s) of
	 * the selected item(s), and may allow you to edit them.
	 */
	public class BottomThing {

		private static final long serialVersionUID = 1L;
		public final JRPTextField name_text_field;
		public final JTextArea comment_text_area;
		public final UndoManager undoManager;
		public final BookmarkPropertyTableModel infoModel;
		public final BookmarkPropertyTableModel datalistModel;
	
		private DocumentListener dl = new DocumentListener(){

			public void insertUpdate(DocumentEvent e)  { updateBookmarkData(); }
			public void removeUpdate(DocumentEvent e)  { updateBookmarkData(); }
			public void changedUpdate(DocumentEvent e) { updateBookmarkData(); }
			
		};
		
		BottomThing() {
			undoManager = new UndoManager();			
			
			name_text_field = new JRPTextField("BookmarkManagerView_name_text_area");
			name_text_field.getDocument().addUndoableEditListener(undoManager);
			((AbstractDocument)name_text_field.getDocument()).addDocumentListener(dl);
			name_text_field.setEnabled(false);
			
			comment_text_area = new JTextArea();
			comment_text_area.getDocument().addUndoableEditListener(undoManager);
			((AbstractDocument)comment_text_area.getDocument()).addDocumentListener(dl);
			comment_text_area.setEnabled(false);
			
			final List<String> info_list = new ArrayList<String>(6);
			info_list.add("version");
			info_list.add("seqid");
			info_list.add("start");
			info_list.add("end");
			info_list.add("create");
			info_list.add("modified");
			
			infoModel = new BookmarkPropertyTableModel(){
				@Override
				protected boolean shouldInclude(String key) {
					return info_list.contains(key);
				}

				@Override
				public boolean isCellEditable(int row, int col) {
					return false;
				}
			};
			
			datalistModel = new BookmarkPropertyTableModel() {
				@Override
				protected boolean shouldInclude(String key) {
					return !info_list.contains(key);
				}

				@Override
				public boolean isCellEditable(int row, int col) {
					return false;
				}
			};
		}

		/**
		 * Method triggered when tree selection changed.
		 *
		 * @param e
		 */
		public void valueChanged() {
			comment_text_area.setEnabled(false);
			name_text_field.setEnabled(false);
			((AbstractDocument)name_text_field.getDocument()).removeDocumentListener(dl);
			((AbstractDocument)comment_text_area.getDocument()).removeDocumentListener(dl);
			
			if (selected_bl != null) {
				Object user_object = selected_bl.getUserObject();
				name_text_field.setText(selected_bl.getName());
				comment_text_area.setText(selected_bl.getComment());
				infoModel.clear();
				datalistModel.clear();

				if (user_object instanceof Bookmark) {
					comment_text_area.setEnabled(true);
					name_text_field.setEnabled(true);
					comment_text_area.setText(((Bookmark) user_object).getComment());
					setInfoTableFromBookmark();
					setDataListTableFromBookmark();
				} else if (user_object instanceof Separator) {
					name_text_field.setText("Separator");
					comment_text_area.setText("Uneditable");
				} else if (selected_bl == tree_model.getRoot()) {
					comment_text_area.setText("Uneditable");
				} else {
					name_text_field.setEnabled(true);
					comment_text_area.setEnabled(true);
				}
			} else {
				name_text_field.setText("");
				comment_text_area.setText("");
			}
			
			((AbstractDocument)name_text_field.getDocument()).addDocumentListener(dl);
			((AbstractDocument)comment_text_area.getDocument()).addDocumentListener(dl);
		}

		/*
		 * Auto save name and comments when bookmark edit action performed.
		 */
		private void updateBookmarkData() {
			if (selected_bl == tree_model.getRoot()) {
				// I do not allow re-naming the root node currently
				return;
			}
			
			String name = name_text_field.getText();
			String comment = comment_text_area.getText();
			
			if (StringUtils.isBlank(name)) {
				return;
			}
			Object user_object = selected_bl.getUserObject();
			if (user_object instanceof Bookmark) {
				Bookmark bm = (Bookmark) user_object;
				bm.setName(name);
				bm.setComment(comment);
			} else if (user_object instanceof String) {
				selected_bl.setUserObject(name);
				selected_bl.setComment(comment);
			}

			tree_model.nodeChanged(selected_bl);
		}

		private void setInfoTableFromBookmark() {
			setTableFromBookmark(infoModel, selected_bl);
		}

		private void setDataListTableFromBookmark() {
			setTableFromBookmark(datalistModel, selected_bl);
		}
	
		private void setTableFromBookmark(BookmarkPropertyTableModel model, BookmarkList bl) {
			Bookmark bm = (Bookmark) bl.getUserObject();
			if (bm == null) {
				model.setValuesFromMap(ImmutableListMultimap.<String, String>builder().build());
			} else {
				URL url = bm.getURL();
				model.setValuesFromMap(Bookmark.parseParameters(url));
			}
		}
	}
	
	private abstract class ExportFileFilter extends javax.swing.filechooser.FileFilter {

		final UniFileFilter filter;

		public ExportFileFilter(UniFileFilter filter) {
			this.filter = filter;
		}

		@Override
		public boolean accept(File f) {
			return filter.accept(f);
		}

		@Override
		public String getDescription() {
			return filter.getDescription();
		}

		public void export(BookmarkList main_bookmark_list, File fil) throws Exception {
			String full_path = fil.getCanonicalPath();
			boolean extSet = false;
			for (String ext : filter.getExtensions()) {
				if (full_path.endsWith("." + ext)) {
					extSet = true;
					break;
				}
			}
			if (!extSet) {
				fil = new File(full_path + "." + filter.getExtensions().iterator().next());
			}
			write(main_bookmark_list, fil);
		}

		protected abstract void write(BookmarkList main_bookmark_list, File fil) throws Exception;
	}

	private class HTMLExportFileFilter extends ExportFileFilter {

		public HTMLExportFileFilter(UniFileFilter filter) {
			super(filter);
		}

		@Override
		public void write(BookmarkList main_bookmark_list, File fil) throws Exception {
			BookmarkList.exportAsHTML(main_bookmark_list, fil);
		}
	}

	private class TextExportFileFilter extends ExportFileFilter {

		public TextExportFileFilter(UniFileFilter filter) {
			super(filter);
		}

		@Override
		public void write(BookmarkList main_bookmark_list, File fil) throws Exception {
			BookmarkList.exportAsTEXT(main_bookmark_list, fil);
		}
	}
}
