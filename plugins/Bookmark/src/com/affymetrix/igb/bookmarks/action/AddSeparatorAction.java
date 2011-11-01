/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.bookmarks.BookmarkList;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import com.affymetrix.igb.bookmarks.Separator;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author lorainelab
 */
public class AddSeparatorAction extends GenericAction {

	private static final long serialVersionUID = 1L;
	private static BookmarkManagerView bmv;
	private static final AddSeparatorAction ACTION = new AddSeparatorAction();

	public static AddSeparatorAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent ae) {
		super.actionPerformed(ae);
		bmv = BookmarkManagerView.getSingleton();

		TreePath path = bmv.tree.getSelectionModel().getSelectionPath();
		if (path == null) {
			Logger.getLogger(BookmarkManagerView.class.getName()).log(
					Level.SEVERE, "No selection");
			return;
		}
		Separator s = new Separator();
		BookmarkList bl = new BookmarkList(s);
		if (bl != null) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) bl;
			bmv.insert(bmv.tree, path, new DefaultMutableTreeNode[]{node});
		}
	}

	@Override
	public String getText() {
		return "New Separator";
	}

	@Override
	public String getIconPath() {
		return "images/separator16.png";
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
