package com.affymetrix.igb.bookmarks.action;

import java.net.MalformedURLException;
import java.net.URLEncoder;

import javax.swing.JOptionPane;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.bookmarks.BookmarkController;
import com.affymetrix.igb.bookmarks.BookmarkJMenuItem;
import com.affymetrix.igb.bookmarks.BookmarkList;

public abstract class AddBookmarkAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final boolean DEBUG = false;

	protected void bookmarkCurrentPosition(boolean include_sym_and_props) {
		if (include_sym_and_props && !BookmarkController.hasSymmetriesOrGraphs()){
			ErrorHandler.errorPanel("Error: No Symmetries or graphs to bookmark.");
			return;
		}
		Bookmark bookmark = null;
		try {
			bookmark = BookmarkController.getCurrentBookmark(include_sym_and_props, BookmarkActionManager.getInstance().getVisibleSpan());
		}
	    catch (MalformedURLException m) {
	    	ErrorHandler.errorPanel("Couldn't add bookmark", m);
	    	return;
	    }
		if (bookmark == null) {
			ErrorHandler.errorPanel("Error", "Nothing to bookmark");
			return;
		}
		String default_name = bookmark.getName();
		String bookmark_name = (String) JOptionPane.showInputDialog(BookmarkActionManager.getInstance().getBookmarkManagerView(),
				"Enter name for bookmark", "Input",
				JOptionPane.PLAIN_MESSAGE, null, null, default_name);
		if (bookmark_name == null) {
			if (DEBUG) {
				System.out.println("bookmark action cancelled");
			}
		} else {
			if (bookmark_name.trim().length() == 0) {
				bookmark_name = default_name;
			}
			if (DEBUG) {
				System.out.println("bookmark name: " + bookmark_name);
			}
			bookmark.setName(bookmark_name);
			addBookmark(bookmark);
		}
	}

	private JRPMenuItem addBookmark(Bookmark bm) {
		JRPMenuItem markMI = null;
		JRPMenu parent_menu = (JRPMenu) BookmarkActionManager.getInstance().getComponentHash().get(BookmarkActionManager.getInstance().getMainBookmarkList());
		if (parent_menu == null) {
			ErrorHandler.errorPanel("Couldn't add bookmark. Lost reference to menu");
			return null;
		}
		addBookmarkMI(parent_menu, bm);
		BookmarkList bl = BookmarkActionManager.getInstance().getMainBookmarkList().addBookmark(bm);

		BookmarkActionManager.getInstance().updateBookmarkManager();
		if (BookmarkActionManager.getInstance().getBookmarkManagerView() != null) {
			BookmarkActionManager.getInstance().getBookmarkManagerView().addBookmarkToHistory(bl);
		}
		return markMI;
	}

	private JRPMenuItem addBookmarkMI(JRPMenu parent_menu, Bookmark bm) {
		JRPMenuItem markMI = (JRPMenuItem) BookmarkActionManager.getInstance().getComponentHash().get(bm);
		if (markMI != null) {
			return markMI;
		}
		markMI = new BookmarkJMenuItem(getIdFromName(bm.getName()), bm);
		BookmarkActionManager.getInstance().getComponentHash().put(bm, markMI);
		parent_menu.add(markMI);
		markMI.addActionListener(this);
		return markMI;
	}

	private String getIdFromName(String name) {
		String id = "";
		try {
			id = "Bookmark_" + URLEncoder.encode("UTF-8", name);
		} catch (Exception x) {
		}
		return id;
	}
}
