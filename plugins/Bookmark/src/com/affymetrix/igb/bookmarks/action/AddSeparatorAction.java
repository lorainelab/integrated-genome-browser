/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 *
 * @author lorainelab
 */
public class AddSeparatorAction extends AddBookmarkAction {

	private static final long serialVersionUID = 1L;
	private static BookmarkManagerView bmv;
	private static final AddSeparatorAction ACTION = new AddSeparatorAction();

	public static AddSeparatorAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent ae) {
		addSeparator();
	}

	@Override
	public String getText() {
		return "New Separator";
	}

	@Override
	public String getIconPath() {
		return null;
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_S;
	}

	@Override
	public String getTooltip() {
		return "New Separator";
	}
}
