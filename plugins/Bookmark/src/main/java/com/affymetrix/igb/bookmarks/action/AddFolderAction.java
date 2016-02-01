package com.affymetrix.igb.bookmarks.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.bookmarks.BookmarkList;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;

/**
 *
 * @author lorainelab
 */
public class AddFolderAction extends BookmarkAction {

    private static final long serialVersionUID = 1L;
    private static final AddFolderAction ACTION = new AddFolderAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static AddFolderAction getAction() {
        return ACTION;
    }

    private AddFolderAction() {
        super(BUNDLE.getString("addBookmarkFolder"), null, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        addNode(new BookmarkList("Folder"));
    }
}
