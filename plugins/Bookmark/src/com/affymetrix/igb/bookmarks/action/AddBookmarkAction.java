package com.affymetrix.igb.bookmarks.action;

import java.net.MalformedURLException;
import java.net.URLEncoder;
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

	public JRPMenuItem addBookmark(Bookmark bm) {
		JRPMenuItem markMI = null;
		JRPMenu parent_menu = (JRPMenu) BookmarkActionManager.getInstance().getComponentHash().get(BookmarkActionManager.getInstance().getMainBookmarkList());
		if (parent_menu == null) {
			ErrorHandler.errorPanel("Couldn't add bookmark. Lost reference to menu");
			return null;
		}
		addBookmarkMI(parent_menu, bm);
		BookmarkList bl = BookmarkActionManager.getInstance().getMainBookmarkList().addBookmark(bm);

		BookmarkActionManager.getInstance().updateBookmarkManager();
		if (BookmarkActionManager.getInstance().getBookmarkManagerViewGUI() != null) {
			BookmarkActionManager.getInstance().getBookmarkManagerViewGUI().getBookmarkManagerView().addBookmarkToHistory(bl);
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
					javax.swing.JOptionPane.OK_CANCEL_OPTION,
					null,
					null);

			javax.swing.JDialog dialog = op.createDialog("Enter Bookmark Information...");
			dialog.setVisible(true);
			dialog.setPreferredSize(new java.awt.Dimension(250, 250));
			dialog.setDefaultCloseOperation(javax.swing.JDialog.HIDE_ON_CLOSE);
			dialog.setAlwaysOnTop(true);
			dialog.setResizable(true);
			dialog.pack();

			int result = javax.swing.JOptionPane.OK_OPTION;

			if (result == javax.swing.JOptionPane.OK_OPTION) {
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
