package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.action.AddBookmarkAction;
import com.affymetrix.igb.bookmarks.action.BookmarkActionManager;
import java.net.MalformedURLException;
import javax.swing.*;

/**
 *
 * @author nick
 */
public class BookmarkEditor {

	private static BookmarkEditor singleton;
	private static Bookmark bookmark;
	private static JTextField nameField;
	private static JTextArea commentField;
	private static JRadioButton positionOnlyB;
	private static JRadioButton positionDataB;
	private static ButtonGroup group;

	public static void init(Bookmark b) {
		bookmark = b;
		
		if (singleton == null) {
			singleton = new BookmarkEditor();
			nameField = new JTextField(b.getName());
			commentField = new JTextArea("", 5, 8);
			commentField.setLineWrap(true);
			commentField.setWrapStyleWord(true);
			positionOnlyB = new JRadioButton("Position Only");
			positionDataB = new JRadioButton("Position and Data", true);
			group = new ButtonGroup();
			group.add(positionOnlyB);
			group.add(positionDataB);
		}
	}

	public static void run() {
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
			AddBookmarkAction.addBookmark(bookmark);
		}

	}
}
