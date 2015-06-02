package com.affymetrix.igb.bookmarks.action;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.igb.bookmarks.BookmarkEditor;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;
import com.lorainelab.igb.services.IgbService;
import java.awt.event.ActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = AddBookmarkAction.COMPONENT_NAME, immediate = true, provide = GenericAction.class)
public class AddBookmarkAction extends BookmarkAction {
    public static final String COMPONENT_NAME = "AddBookmarkAction";
    private static final long serialVersionUID = 1L;
    private static AddBookmarkAction ACTION;
    private IgbService igbService;
    private final int TOOLBAR_INDEX = 9;
    private static final Logger logger = LoggerFactory.getLogger(AddBookmarkAction.class);

    public static AddBookmarkAction getAction() {
        return ACTION;
    }
    
    @Reference(optional = false)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    public AddBookmarkAction() {
        super(BUNDLE.getString("addBookmark"), "16x16/actions/bookmark-new.png",
                "22x22/actions/bookmark-new.png");
        setKeyStrokeBinding("ctrl D");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        BookmarkEditor.run(igbService.getSeqMapView().getVisibleSpan());
    }
    
    @Override
    public boolean isToolbarDefault() {
        return true; 
    }

    @Override
    public int getToolbarIndex() {
        return TOOLBAR_INDEX; 
    }
}
