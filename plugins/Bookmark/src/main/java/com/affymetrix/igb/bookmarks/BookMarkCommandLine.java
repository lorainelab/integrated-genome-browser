package com.affymetrix.igb.bookmarks;

import com.affymetrix.igb.bookmarks.model.Bookmark;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericServerInitListener;
import com.lorainelab.igb.services.IgbService;
import java.net.MalformedURLException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author hiralv
 */
public class BookMarkCommandLine {

    private final IgbService igbService;
    private ServiceRegistration registration;
    private final String url;
    private final boolean force;
    private static final Logger logger = LoggerFactory.getLogger(BookMarkCommandLine.class);

    BookMarkCommandLine(final BundleContext bundleContext, final IgbService igbService, final String url, final boolean force) {
        this.igbService = igbService;
        this.url = url;
        this.force = force;

        if (igbService.areAllServersInited()) {
            gotoBookmark();
        } else {
            GenericServerInitListener genericServerListener = evt -> {
                if (!igbService.areAllServersInited()) { // do this first to avoid race condition
                    return;
                }
                registration.unregister();
                registration = null;
                gotoBookmark();
            };
            registration = bundleContext.registerService(GenericServerInitListener.class, genericServerListener, null);
        }
    }

    // If the command line contains a parameter "-href http://..." where
    // the URL is a valid IGB control bookmark, then go to that bookmark.
    private void gotoBookmark() {
        GenometryModel gmodel = GenometryModel.getInstance();

        // If it is -home then do not force to switch unless no species is selected.
        if (!force && gmodel.getSelectedSeqGroup() != null && gmodel.getSelectedSeq() != null) {
            logger.warn("Previous species already loaded. Home {0} will be not loaded", url);
            return;
        }

        try {
            final Bookmark bm = new Bookmark(null, "", url);
            if (bm.isValidBookmarkFormat()) {

                logger.info("Loading bookmark: {0}", url);
                BookmarkController.viewBookmark(igbService, bm);

            } else {
                logger.error("Invalid bookmark given with -href argument: \n{0}", url);
            }
        } catch (MalformedURLException mue) {
            mue.printStackTrace(System.err);
        }
    }
}
