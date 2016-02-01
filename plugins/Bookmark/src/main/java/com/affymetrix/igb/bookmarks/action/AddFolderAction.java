package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.bookmarks.BookmarkList;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;
import java.awt.event.ActionEvent;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author lorainelab
 */
public class AddFolderAction extends GenericAction {

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
        addNode(new BookmarkList("Folder"));
    }

    protected void addNode(DefaultMutableTreeNode node) {
        BookmarkManagerView.getSingleton().insert(node);
    }
}
