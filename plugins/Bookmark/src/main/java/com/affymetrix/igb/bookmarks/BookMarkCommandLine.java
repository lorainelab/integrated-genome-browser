package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.igb.bookmarks.model.Bookmark;
import org.lorainelab.igb.igb.services.IgbService;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import javax.swing.Timer;
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

    private Timer timer;

    BookMarkCommandLine(final BundleContext bundleContext, final IgbService igbService, final String url, final boolean force) {
        this.igbService = igbService;
        this.url = url;
        this.force = force;

        if (igbService.areAllServersInited()) {
            gotoBookmark();
        } else {
            timer = new Timer(1000, new TimerAction());
            timer.start();

        }
    }

    class TimerAction implements ActionListener {

        //only try for 10 cycles then just stop
        int countDown = 10;

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (igbService.areAllServersInited()) {
                gotoBookmark();
                timer.stop();
            }
            countDown--;
            if (countDown == 0) {
                timer.stop();
            }
        }

    }

    // If the command line contains a parameter "-href http://..." where
    // the URL is a valid IGB control bookmark, then go to that bookmark.
    private void gotoBookmark() {
        GenometryModel gmodel = GenometryModel.getInstance();

        // If it is -home then do not force to switch unless no species is selected.
        if (!force && gmodel.getSelectedGenomeVersion() != null && gmodel.getSelectedSeq() != null) {
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
