package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.bookmarks.BookmarkList;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import com.affymetrix.igb.bookmarks.Separator;
import java.awt.event.ActionEvent;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author lorainelab
 */
public class AddSeparatorAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final AddSeparatorAction ACTION = new AddSeparatorAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static AddSeparatorAction getAction() {
        return ACTION;
    }

    private AddSeparatorAction() {
        super("New Separator", null, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        addNode(new BookmarkList(new Separator()));
    }

    void addNode(DefaultMutableTreeNode node) {
        BookmarkManagerView.getSingleton().insert(node);
    }
}
