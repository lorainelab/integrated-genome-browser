package com.affymetrix.igb.bookmarks.action;

import java.net.MalformedURLException;
import java.net.URLEncoder;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.bookmarks.BookmarkController;
import com.affymetrix.igb.bookmarks.BookmarkList;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public abstract class AddBookmarkAction extends GenericAction {

	private static final long serialVersionUID = 1L;

	protected void bookmarkCurrentPosition(boolean include_sym_and_props) {
		if (include_sym_and_props && !BookmarkController.hasSymmetriesOrGraphs()) {
			ErrorHandler.errorPanel("Error: No Symmetries or graphs to bookmark.");
			return;
		}
		Bookmark bookmark = null;
		try {
			bookmark = BookmarkController.getCurrentBookmark(include_sym_and_props, BookmarkActionManager.getInstance().getVisibleSpan());
		} catch (MalformedURLException m) {
			ErrorHandler.errorPanel("Couldn't add bookmark", m);
			return;
		}
		if (bookmark == null) {
			ErrorHandler.errorPanel("Error", "Nothing to bookmark");
			return;
		}

		BookmarkEditor editor = new BookmarkEditor(bookmark);
		editor.run();
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
		TreePath path = tree.getSelectionModel().getSelectionPath();
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
		javax.swing.JTextField nameField;
		javax.swing.JTextArea commentField = new javax.swing.JTextArea("", 5, 8);

		public BookmarkEditor(Bookmark b) {
			bookmark = b;
			nameField = new javax.swing.JTextField(b.getName());
		}

		void run() {
			javax.swing.JScrollPane scrollpane = new javax.swing.JScrollPane(commentField);
			scrollpane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			Object[] msg = {"Name:", nameField, "Comment:", scrollpane};
			javax.swing.JOptionPane op = new javax.swing.JOptionPane(
					msg,
					javax.swing.JOptionPane.PLAIN_MESSAGE,
					javax.swing.JOptionPane.CANCEL_OPTION,
					null,
					null);

			javax.swing.JDialog dialog = op.createDialog("Enter Bookmark Information...");
			dialog.setVisible(true);
			dialog.setPreferredSize(new java.awt.Dimension(250, 250));
			dialog.setDefaultCloseOperation(javax.swing.JDialog.HIDE_ON_CLOSE);
			dialog.setAlwaysOnTop(true);
			dialog.setResizable(true);
			dialog.pack();

			int result = JOptionPane.CANCEL_OPTION;

			if(op.getValue() != null)
			{
				result = (Integer) op.getValue();
			}
			
			if (result == JOptionPane.OK_OPTION) {
				String name;
				String comment;
				name = nameField.getText();
				comment = commentField.getText();

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
