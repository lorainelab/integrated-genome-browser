/**
 * Copyright (c) 2001-2004 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.bookmarks;

import com.affymetrix.igb.bookmarks.model.Bookmark;
import javax.swing.JMenuItem;
import org.apache.commons.lang3.StringUtils;

/**
 * A JMenuItem that contains a reference to a Bookmark object.
 */
public final class BookmarkMenuItem extends JMenuItem {

    private static final long serialVersionUID = 1L;
    private final Bookmark bookmark;

    /**
     * Creates a new instance of BookmarkJMenuItem. The name of the JMenuItem
     * will be created from the name of the Bookmark object. The bookmark may
     * change its Font and/or Icon based on whether the bookmark is designed to
     * open in Unibrow or in an external browser.
     */
    public BookmarkMenuItem(Bookmark b) {
        super(StringUtils.abbreviate(b.getName(), 60));
        this.bookmark = b;
    }

    public Bookmark getBookmark() {
        return this.bookmark;
    }
}
