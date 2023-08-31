package com.affymetrix.igb.bookmarks.service;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import com.affymetrix.igb.bookmarks.BookmarkController;
import com.affymetrix.igb.bookmarks.model.Bookmark;
import com.google.common.base.Optional;
import org.lorainelab.igb.services.IgbService;
import org.osgi.service.component.annotations.ReferenceCardinality;

/**
 *
 * @author dcnorris
 */
@Component(name = DefaultBookmarkService.COMPONENT_NAME, immediate = true, service = BookmarkService.class)
public class DefaultBookmarkService implements BookmarkService {

    public static final String COMPONENT_NAME = "DefaultBookmarkService";
    private IgbService igbService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
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
