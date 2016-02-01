package com.affymetrix.igb.bookmarks.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.bookmarks.BookmarkList;
import com.affymetrix.igb.bookmarks.Separator;

/**
 *
 * @author lorainelab
 */
public class AddSeparatorAction extends BookmarkAction {

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
        super.actionPerformed(e);
        addNode(new BookmarkList(new Separator()));
    }
}
