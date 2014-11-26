package com.affymetrix.igb.bookmarks.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.bookmarks.BookmarkEditor;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;
import com.affymetrix.igb.osgi.service.IGBService;

public class AddBookmarkAction extends BookmarkAction {

    private static final long serialVersionUID = 1L;
    private static AddBookmarkAction ACTION;
    private final IGBService igbService;

    public static void createAction(IGBService igbService) {
        ACTION = new AddBookmarkAction(igbService);
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static AddBookmarkAction getAction() {
        return ACTION;
    }

    private AddBookmarkAction(IGBService igbService) {
        super(BUNDLE.getString("addBookmark"), "16x16/actions/bookmark-new.png",
                "22x22/actions/bookmark-new.png");
        this.igbService = igbService;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        BookmarkEditor.run(igbService.getSeqMapView().getVisibleSpan());
    }
}
