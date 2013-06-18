package com.affymetrix.igb.bookmarks.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.tree.DefaultMutableTreeNode;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.bookmarks.BookmarkEditor;
import com.affymetrix.igb.bookmarks.BookmarkList;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;

public class AddBookmarkAction extends BookmarkAction {

	private static final long serialVersionUID = 1L;
	private static final AddBookmarkAction ACTION = new AddBookmarkAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static AddBookmarkAction getAction() {
		return ACTION;
	}

	private AddBookmarkAction() {
		super(BUNDLE.getString("addBookmark"), null, "16x16/actions/bookmark-new.png",
				"22x22/actions/bookmark-new.png", KeyEvent.VK_UNDEFINED, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		bookmarkCurrentPosition();
	}
	
	/**
	 * add a bookmark to bookmark tree.
	 *
	 * @param bm
	 */
	public void addBookmark(Bookmark bm) {
		BookmarkList parent_list = BookmarkManagerView.getSingleton().thing.selected_bl;
		BookmarkManagerView.getSingleton().addBookmarkToHistory(parent_list);

		BookmarkList bl = new BookmarkList(bm);
		DefaultMutableTreeNode node = bl;
		addNode(node);
	}
	
	/**
	 * Generate a bookmark editor panel for adding a new bookmark.
	 */
	protected void bookmarkCurrentPosition() {
		Bookmark bookmark = getCurrentPosition(false);
		if (bookmark != null) {
			BookmarkEditor.run(bookmark);
		}
	}
}
