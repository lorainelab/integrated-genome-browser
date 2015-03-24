package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.bookmarks.BookmarkEditor;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;
import com.lorainelab.igb.services.IgbService;
import java.awt.event.ActionEvent;

public class AddBookmarkAction extends BookmarkAction {

    private static final long serialVersionUID = 1L;
    private static AddBookmarkAction ACTION;
    private final IgbService igbService;
    private final int TOOLBAR_INDEX = 9;

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
        setKeyStrokeBinding("ctrl D");
        this.igbService = igbService;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        BookmarkEditor.run(igbService.getSeqMapView().getVisibleSpan());
    }
    
    @Override
    public boolean isToolbarDefault() {
        return true; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getToolbarIndex() {
        return TOOLBAR_INDEX; //To change body of generated methods, choose Tools | Templates.
    }
}
