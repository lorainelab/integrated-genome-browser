package com.affymetrix.igb.bookmarks.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 *
 * @author lorainelab
 */
public class AddSeparatorAction extends AddBookmarkAction {

	private static final long serialVersionUID = 1L;
	private static final AddSeparatorAction ACTION = new AddSeparatorAction();

	public static AddSeparatorAction getAction() {
		return ACTION;
	}

	private AddSeparatorAction() {
		super("New Separator", "New Separator", null,
				null, KeyEvent.VK_S, null, false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		addSeparator();
	}
}
