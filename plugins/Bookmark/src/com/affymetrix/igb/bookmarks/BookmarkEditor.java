package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.bookmarks.action.AddBookmarkAction;
import com.affymetrix.igb.bookmarks.action.BookmarkActionManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
	private static JButton cancelButton;
	private static JButton okButton;
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

			cancelButton = new JButton("Cancel");
			okButton = new JButton("OK");
			okButton.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					addBookmark();
					dialog.dispose();
				}
			});

			cancelButton.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					dialog.dispose();
				}
			});

			useDefaultName = PreferenceUtils.createCheckBox(PREF_USE_DEFAULT_NAME, PreferenceUtils.getTopNode(),
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
			scrollpane.setMinimumSize(new Dimension(100, 120));
			op = new JOptionPane(null, JOptionPane.PLAIN_MESSAGE,
					JOptionPane.CANCEL_OPTION, null, null);
		}
	}

	/**
	 * Used JDialog as display panel and initialized it.
	 */
	private static void initDialog() {
		setNameField();
		commentField.setText("");
		dialog = new JDialog();

		dialog.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		dialog.setTitle("Enter Bookmark Information...");
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(0, 5, 0, 0);
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		dialog.add(useDefaultName, c);
		c.gridx = 0;
		c.gridy = 1;
		c.insets = new Insets(0, 8, 0, 0);
		c.anchor = GridBagConstraints.LINE_START;
		dialog.add(new JLabel("Name:"), c);
		c.gridx = 0;
		c.gridy = 2;
		c.insets = new Insets(0, 5, 0, 5);
		c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 1.0;
		c.gridwidth = 5;
		c.fill = GridBagConstraints.HORIZONTAL;
		dialog.add(nameField, c);
		c.gridx = 0;
		c.gridy = 3;
		c.insets = new Insets(0, 8, 0, 0);
		c.anchor = GridBagConstraints.LINE_START;
		dialog.add(new JLabel("Comment:"), c);
		c.gridx = 0;
		c.gridy = 4;
		c.insets = new Insets(0, 8, 0, 8);
		c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.BOTH;
		dialog.add(scrollpane, c);
		c.gridx = 0;
		c.gridy = 5;
		c.weighty = 0;
		c.insets = new Insets(0, 5, 0, 0);
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.NONE;
		dialog.add(positionOnlyB, c);
		c.gridx = 0;
		c.gridy = 6;
		c.insets = new Insets(0, 5, 0, 0);
		c.weighty = 0;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.NONE;
		dialog.add(positionDataB, c);

		JPanel okCancelButtonPanel = new JPanel(new FlowLayout());
		okCancelButtonPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		okCancelButtonPanel.add(cancelButton);
		okCancelButtonPanel.add(okButton);
		c.gridx = 0;
		c.gridy = 7;
		c.weighty = 0;
		c.anchor = GridBagConstraints.LAST_LINE_END;
		dialog.add(okCancelButtonPanel, c);

		dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		dialog.setLocationRelativeTo(null);
		dialog.setAlwaysOnTop(true);
		dialog.setResizable(true);
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
		AddBookmarkAction.addBookmark(bookmark);
	}
}
