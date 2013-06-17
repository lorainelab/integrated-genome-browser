package com.affymetrix.igb.bookmarks.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.tree.DefaultMutableTreeNode;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.bookmarks.BookmarkList;
import com.affymetrix.igb.bookmarks.Separator;
/**
 *
 * @author lorainelab
 */
public class AddSeparatorAction extends BookmarkAction {

	private static final long serialVersionUID = 1L;
	private static final AddSeparatorAction ACTION = new AddSeparatorAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
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
	
	/**
	 * add a separator to bookmark tree.
	 */
	public static void addSeparator() {
		Separator s = new Separator();
		BookmarkList bl = new BookmarkList(s);
		DefaultMutableTreeNode node = bl;
		addNode(node);
	}
}
