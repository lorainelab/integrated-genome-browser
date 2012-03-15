package com.affymetrix.igb.bookmarks.action;

import java.net.MalformedURLException;
import java.net.URLEncoder;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.bookmarks.BookmarkController;
import com.affymetrix.igb.bookmarks.BookmarkEditor;
import com.affymetrix.igb.bookmarks.BookmarkList;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import com.affymetrix.igb.bookmarks.Separator;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author Nick & David
 */
public class AddBookmarkAction extends GenericAction {

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
		
		BookmarkEditor.init(bookmark);
		BookmarkEditor.run();
	}

	public static void addBookmarkFolder() {
		BookmarkList bl = new BookmarkList("Folder");
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) bl;
		addNode(node);
	}

	public static void addBookmark(Bookmark bm) {
		BookmarkList parent_list = BookmarkManagerView.getSingleton().thing.selected_bl;
		BookmarkManagerView.getSingleton().addBookmarkToHistory(parent_list);

		BookmarkList bl = new BookmarkList(bm);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) bl;
		addNode(node);
	}

	public static void addSeparator() {
		Separator s = new Separator();
		BookmarkList bl = new BookmarkList(s);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) bl;
		addNode(node);
	}

	public static void addNode(DefaultMutableTreeNode node) {
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

	@Override
	public String getText() {
		return null;
	}
}
