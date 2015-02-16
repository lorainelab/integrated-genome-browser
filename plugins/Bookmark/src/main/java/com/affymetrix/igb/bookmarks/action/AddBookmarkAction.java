package com.affymetrix.igb.bookmarks.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.bookmarks.BookmarkEditor;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;
import com.lorainelab.igb.service.api.IgbService;

public class AddBookmarkAction extends BookmarkAction {

    private static final long serialVersionUID = 1L;
    private static AddBookmarkAction ACTION;
    private final IgbService igbService;

    public static void createAction(IgbService igbService) {
        ACTION = new AddBookmarkAction(igbService);
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static AddBookmarkAction getAction() {
        return ACTION;
    }

    private AddBookmarkAction(IgbService igbService) {
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
