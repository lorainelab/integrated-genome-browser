package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class is designed to create and add new bookmark to the list.
 *
 * @author Nick & David
 */
public abstract class BookmarkAction extends GenericAction {

	private static final long serialVersionUID = 1L;

	public BookmarkAction(String text, String iconPath, String largeIconPath) {
		super(text, iconPath, largeIconPath);
	}

	/**
	 * add node to the tree.
	 *
	 * @param node (bookmark, folder or separator)
	 */
	protected void addNode(DefaultMutableTreeNode node) {
		BookmarkManagerView.getSingleton().insert(node);
	}
}
