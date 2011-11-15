package com.affymetrix.igb.bookmarks.action;

import java.net.MalformedURLException;
import java.net.URLEncoder;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.bookmarks.BookmarkController;
import com.affymetrix.igb.bookmarks.BookmarkList;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author Nick & David
 */
public abstract class AddBookmarkAction extends GenericAction {

	private static final long serialVersionUID = 1L;

	protected void bookmarkCurrentPosition() {
		if (!BookmarkController.hasSymmetriesOrGraphs()) {
			ErrorHandler.errorPanel("Error: No Symmetries or graphs to bookmark.");
			return;
		}
		Bookmark bookmark = null;
		try {
			bookmark = BookmarkController.getCurrentBookmark(false, BookmarkActionManager.getInstance().getVisibleSpan());
		} catch (MalformedURLException m) {
			ErrorHandler.errorPanel("Couldn't add bookmark", m);
			return;
		}
		if (bookmark == null) {
			ErrorHandler.errorPanel("Error", "Nothing to bookmark");
			return;
		}

		BookmarkEditor editor = new BookmarkEditor(bookmark);
		editor.initDialog();
	}

	public void addBookmarkFolder() {
		BookmarkList bl = new BookmarkList("Folder");
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) bl;
		addNode(node);
	}

	public void addBookmark(Bookmark bm) {
		BookmarkList parent_list = BookmarkManagerView.getSingleton().thing.selected_bl;
		BookmarkManagerView.getSingleton().addBookmarkToHistory(parent_list);// need changed

		BookmarkList bl = new BookmarkList(bm);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) bl;
		addNode(node);
	}

	private void addNode(DefaultMutableTreeNode node) {
		JTree tree = BookmarkManagerView.getSingleton().tree;
		TreePath path;
		if (tree.getSelectionCount() > 0) {
			path = tree.getSelectionModel().getSelectionPath();
		} else {
			tree.setSelectionRow(0);
			path = tree.getSelectionModel().getSelectionPath();
		}
		BookmarkManagerView.getSingleton().insert(tree, path, new DefaultMutableTreeNode[]{node});
		BookmarkActionManager.getInstance().rebuildMenus();
	}

	private String getIdFromName(String name) {
		String id = "";
		try {
			id = "Bookmark_" + URLEncoder.encode("UTF-8", name);
		} catch (Exception x) {
		}
		return id;
	}

	class BookmarkEditor {

		Bookmark bookmark;
		JTextField nameField;
		JTextArea commentField;
		JRadioButton positionOnlyB;
		JRadioButton positionDataB;
		ButtonGroup group;

		public BookmarkEditor(Bookmark b) {
			bookmark = b;
			init();
		}

		void init() {
			nameField = new JTextField(bookmark.getName());
			commentField = new JTextArea("", 5, 8);
			commentField.setLineWrap(true);
			commentField.setWrapStyleWord(true);
			positionOnlyB = new JRadioButton("Position Only", true);
			positionDataB = new JRadioButton("Position and Data");
			group = new ButtonGroup();
			group.add(positionOnlyB);
			group.add(positionDataB);
		}

		public void initDialog() {
			JScrollPane scrollpane = new JScrollPane(commentField);
			scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			Object[] msg = {"Name:", nameField, "Comment:", scrollpane, positionOnlyB, positionDataB};
			JOptionPane op = new JOptionPane(msg, JOptionPane.PLAIN_MESSAGE,
					JOptionPane.CANCEL_OPTION, null, null);

			JDialog dialog = op.createDialog("Enter Bookmark Information...");
			dialog.setVisible(true);
			dialog.setPreferredSize(new java.awt.Dimension(250, 250));
			dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
			dialog.setAlwaysOnTop(true);
			dialog.setResizable(true);
			dialog.pack();

			int result = JOptionPane.CANCEL_OPTION;

			if (op.getValue() != null) {
				result = (Integer) op.getValue();
			}

			if (result == JOptionPane.OK_OPTION) {
				if (positionDataB.isSelected()) {
					try {
						bookmark = BookmarkController.getCurrentBookmark(true, BookmarkActionManager.getInstance().getVisibleSpan());
					} catch (MalformedURLException m) {
						ErrorHandler.errorPanel("Couldn't add bookmark", m);
						return;
					}
				}

				String name = nameField.getText();
				String comment = commentField.getText();

				if (name.trim().length() == 0) {
					name = bookmark.getName();
				}

				bookmark.setName(name);
				bookmark.setComment(comment);
				addBookmark(bookmark);
			}

		}
	}
}
