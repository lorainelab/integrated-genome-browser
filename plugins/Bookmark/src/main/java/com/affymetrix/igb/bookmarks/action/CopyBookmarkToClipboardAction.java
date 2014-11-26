package com.affymetrix.igb.bookmarks.action;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.util.logging.Level;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.bookmarks.BookmarkController;

import com.affymetrix.igb.osgi.service.IGBService;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;
import java.io.UnsupportedEncodingException;

public class CopyBookmarkToClipboardAction extends BookmarkAction {

    private static final long serialVersionUID = 1L;
    private static CopyBookmarkToClipboardAction ACTION;

    private final IGBService igbService;

    public static void createAction(IGBService igbService) {
        ACTION = new CopyBookmarkToClipboardAction(igbService);
    }

    public static CopyBookmarkToClipboardAction getAction() {
        return ACTION;
    }

    private CopyBookmarkToClipboardAction(IGBService igbService) {
        super(BUNDLE.getString("copyBookmarkToClipboard"), "16x16/actions/edit-paste.png",
                "22x22/actions/edit-paste.png");
        this.igbService = igbService;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        try {
            Bookmark bookmark = BookmarkController.getCurrentBookmark(true, igbService.getSeqMapView().getVisibleSpan());
            if (bookmark != null) {
                StringSelection data = new StringSelection(bookmark.getURL().toString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(data, data);
            } else {
                ErrorHandler.errorPanel("No bookmark to copy");
            }
        } catch (MalformedURLException m) {
            ErrorHandler.errorPanel("Couldn't add bookmark", m, Level.SEVERE);
        } catch (UnsupportedEncodingException ex) {
            ErrorHandler.errorPanel("Couldn't add bookmark", ex, Level.SEVERE);
        }
    }
}
