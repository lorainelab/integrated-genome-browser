package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.bookmarks.action.AddBookmarkAction;
import com.affymetrix.igb.bookmarks.action.BookmarkActionManager;
import java.net.MalformedURLException;
import java.util.logging.Level;
import javax.swing.*;

/**
 * This class is the implementation of bookmark editor panel.
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
	private static JCheckBox useDefaultName;
	private static ButtonGroup group;
	private static JOptionPane op;
	private static JScrollPane scrollpane;
	private static JDialog dialog;
	public static final boolean defaultUseDefaultName = true;
	public static final String PREF_USE_DEFAULT_NAME = "Use Default Name";
	
	/**
	 * Initialize all the components in the panel by passed bookmark.
	 *
	 * @param b
	 */
	public static void init(Bookmark b) {
		bookmark = b;

		if (singleton == null) {
			singleton = new BookmarkEditor();
			nameField = new JTextField(40);
			commentField = new JTextArea(5, 8);
			commentField.setLineWrap(true);
			commentField.setWrapStyleWord(true);
			positionOnlyB = new JRadioButton("Position Only");
			positionDataB = new JRadioButton("Position and Data", true);
			useDefaultName = PreferenceUtils.createCheckBox(PREF_USE_DEFAULT_NAME,
					PREF_USE_DEFAULT_NAME, defaultUseDefaultName);
			useDefaultName.addActionListener(new java.awt.event.ActionListener() {

				public void actionPerformed(java.awt.event.ActionEvent evt) {
					setNameField();
				}
			});
			group = new ButtonGroup();
			group.add(positionOnlyB);
			group.add(positionDataB);
			scrollpane = new JScrollPane(commentField);
			scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			op = new JOptionPane(null, JOptionPane.PLAIN_MESSAGE,
					JOptionPane.CANCEL_OPTION, null, null);
			op.addPropertyChangeListener("value", new java.beans.PropertyChangeListener() {
				public void propertyChange(java.beans.PropertyChangeEvent evt) {
					addBookmark();
				}
			});
		}
	}

	/**
	 * Used JDialog as display panel and initialized it.
	 */
	private static void initDialog() {
		setNameField();
		commentField.setText("");
		op.setMessage(new Object[]{"", useDefaultName,
					"Name:", nameField, "Comment:", scrollpane,
					positionOnlyB, positionDataB});
		dialog = op.createDialog("Enter Bookmark Information...");
		dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		dialog.setResizable(true);
		dialog.setAlwaysOnTop(false);
		dialog.setModal(false);
		dialog.setVisible(true);
		dialog.pack();
	}

	private static void setNameField() {
		if (useDefaultName.isSelected()) {
			nameField.setText(bookmark.getName());
		} else {
			nameField.setText("");
		}
	}

	/**
	 * Activate the panel and complete adding a bookmark by user's operation.
	 */
	public static void run() {
		initDialog();
	}
	
	private static void addBookmark() {
		int result = JOptionPane.CANCEL_OPTION;

		if (op.getValue() != null && op.getValue() instanceof Integer) {
			result = (Integer) op.getValue();
		}

		if (result == JOptionPane.OK_OPTION) {
			if (positionDataB.isSelected()) {
				// create a new bookmark includes position and data
				// otherwise, the bookmark is just position only
				try {
					bookmark = BookmarkController.getCurrentBookmark(true,
							BookmarkActionManager.getInstance().getVisibleSpan());
				} catch (MalformedURLException m) {
					ErrorHandler.errorPanel("Couldn't add bookmark", m, Level.SEVERE);
					return;
				}
			}

			String name = nameField.getText();
			String comment = commentField.getText();

			if (name.trim().length() == 0) {
				name = "IGB BOOKMARK";
			}

			bookmark.setName(name);
			bookmark.setComment(comment);
			AddBookmarkAction.getAction().addBookmark(bookmark);
		}
	}
}
