/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.bookmarks;

import com.affymetrix.genoviz.swing.DragDropTree;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 *
 * @author lorainelab
 */
public class BookmarkTree extends DragDropTree implements KeyListener {
	private static final long serialVersionUID = 1L;
	private BookmarkManagerView bookmarkManagerView;

	public BookmarkTree(BookmarkManagerView bmv) {
		super();
		bookmarkManagerView = bmv;
	}

	public void keyTyped(KeyEvent ke) {
		if (ke.getKeyChar() == KeyEvent.VK_DELETE) {
			bookmarkManagerView.deleteAction();
		}
	}

	public void keyPressed(KeyEvent ke) {
		//do nothing
	}

	public void keyReleased(KeyEvent ke) {
		//do nothing
	}
}
