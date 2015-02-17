package com.affymetrix.igb.bookmarks.service;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.igb.bookmarks.BookmarkController;
import com.affymetrix.igb.bookmarks.model.Bookmark;
import com.google.common.base.Optional;
import com.lorainelab.igb.service.api.IgbService;

/**
 *
 * @author dcnorris
 */
@Component(name = DefaultBookmarkService.COMPONENT_NAME, immediate = true, provide = BookmarkService.class)
public class DefaultBookmarkService implements BookmarkService {

    public static final String COMPONENT_NAME = "DefaultBookmarkService";
    private IgbService igbService;

    @Reference(optional = false)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Override
    public Optional<Bookmark> getCurrentBookmark() {
        return BookmarkController.getCurrentBookmark(true, igbService.getSeqMapView().getVisibleSpan());
    }

    @Override
    public void loadBookmark(Bookmark bookmark) {
        BookmarkController.viewBookmark(igbService, bookmark);
    }

}
