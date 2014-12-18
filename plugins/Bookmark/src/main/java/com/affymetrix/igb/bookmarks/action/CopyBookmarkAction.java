/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

/**
 *
 * @author tkanapar
 */
public class CopyBookmarkAction extends GenericAction {

    private static final long serialVersionUID = 1L;

    private CopyBookmarkAction() {
        super(BUNDLE.getString("copyBookmarkToClipboard"), "16x16/actions/edit-paste.png",
                "22x22/actions/edit-paste.png");
    }

    public static CopyBookmarkAction getAction() {
        return CopyBookmarkHolder.INSTANCE;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        Bookmark bookmark = BookmarkManagerView.getSingleton().getSelectedBookmark();
        if (bookmark != null) {
            StringSelection data = new StringSelection(bookmark.getURL().toString());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(data, data);
        } else {
            ErrorHandler.errorPanel("No bookmark to copy");
        }
    }

    private static class CopyBookmarkHolder {
        private static final CopyBookmarkAction INSTANCE = new CopyBookmarkAction();
    }

}
