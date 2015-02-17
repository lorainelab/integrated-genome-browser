package com.affymetrix.igb.bookmarks.service;

import com.affymetrix.igb.bookmarks.model.Bookmark;
import com.google.common.base.Optional;

/**
 *
 * @author dcnorris
 */
public interface BookmarkService {

    public Optional<Bookmark> getCurrentBookmark();

    public void loadBookmark(Bookmark bookmark);
}
