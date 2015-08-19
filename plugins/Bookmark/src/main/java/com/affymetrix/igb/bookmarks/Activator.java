package com.affymetrix.igb.bookmarks;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.igb.bookmarks.action.AddBookmarkAction;
import com.affymetrix.igb.bookmarks.action.BookmarkActionManager;
import com.affymetrix.igb.bookmarks.action.CopyBookmarkAction;
import com.affymetrix.igb.bookmarks.action.ExportBookmarkAction;
import com.affymetrix.igb.bookmarks.action.ImportBookmarkAction;
import com.affymetrix.igb.swing.JRPMenu;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.swing.MenuUtil;
import com.lorainelab.igb.services.IgbService;
import com.lorainelab.igb.services.XServiceRegistrar;
import com.lorainelab.igb.services.window.WindowServiceLifecycleHook;
import com.lorainelab.igb.services.window.tabs.IgbTabPanel;
import com.lorainelab.igb.services.window.tabs.IgbTabPanelI;
import com.lorainelab.synonymlookup.services.GenomeVersionSynonymLookup;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.ResourceBundle;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends XServiceRegistrar<IgbService> implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);
    private static final String WILDCARD = "*";

    public Activator() {
        super(IgbService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IgbService igbService) throws Exception {

        // assuming last file menu item is Exit, leave it there
//        JRPMenu file_menu = igbService.getMenu("file");
//        final int index = file_menu.getItemCount() - 1;
//        file_menu.insertSeparator(index);
        Dictionary props = new Hashtable();
        props.put("service.pid", "BookmarkManagerViewGUI");
        return new ServiceRegistration[]{
            bundleContext.registerService(IgbTabPanelI.class, getPage(bundleContext, igbService), props)
        };
    }

    private IgbTabPanel getPage(BundleContext bundleContext, IgbService igbService) {
        ResourceBundle BUNDLE = ResourceBundle.getBundle("bookmark");

        // Need to let the QuickLoad system get started-up before starting
        //   the control server that listens to ping requests?
        // Therefore start listening for http requests only after all set-up is done.
        String[] args = CommonUtils.getInstance().getArgs(bundleContext);
        String url = CommonUtils.getInstance().getArg("-href", args);

        if (StringUtils.equals(url, WILDCARD)) {
            url = null;
        }
        if (StringUtils.isNotBlank(url)) {
            logger.info("Loading bookmark {}", url);
            new BookMarkCommandLine(bundleContext, igbService, url, true);
        } else {
            url = CommonUtils.getInstance().getArg("-home", args);
            if (StringUtils.isNotBlank(url)) {
                logger.info("Loading home {}", url);
                new BookMarkCommandLine(bundleContext, igbService, url, false);
            }
        }

        String portString = CommonUtils.getInstance().getArg("-port", args);
        if (portString != null) {
            SimpleBookmarkServer.setServerPort(portString);
        }
        SimpleBookmarkServer.init(igbService);

        AddBookmarkAction.createAction(igbService);

        BookmarkList main_bookmark_list = new BookmarkList("Bookmarks");
        JRPMenu bookmark_menu = igbService.addTopMenu("Bookmark_bookmarksMenu", BUNDLE.getString("bookmarksMenu"), 6);
        bookmark_menu.setMnemonic(BUNDLE.getString("bookmarksMenuMnemonic").charAt(0));
        MenuUtil.addToMenu(bookmark_menu, new JRPMenuItem("Bookmark_add_pos", AddBookmarkAction.getAction()));
        MenuUtil.addToMenu(bookmark_menu, new JRPMenuItem("Bookmark_import", ImportBookmarkAction.getAction()));
        MenuUtil.addToMenu(bookmark_menu, new JRPMenuItem("Bookmark_export", ExportBookmarkAction.getAction()));
        MenuUtil.addToMenu(bookmark_menu, new JRPMenuItem("Bookmark_clipboard", CopyBookmarkAction.getAction()));
        bookmark_menu.addSeparator();

        BookmarkActionManager.init(igbService, bookmark_menu, main_bookmark_list);
        final BookmarkActionManager bmark_action = BookmarkActionManager.getInstance();
        bundleContext.registerService(WindowServiceLifecycleHook.class.getName(),
                new WindowServiceLifecycleHook() {
                    @Override
                    public void stop() {
                        bmark_action.autoSaveBookmarks();
                    }

                    @Override
                    public void start() { /* Do Nothing */ }
                },
                null
        );
        BookmarkManagerViewGUI.init(igbService);
        BookmarkManagerView.getSingleton().addTreeModelListener(bmark_action);
        BookmarkManagerViewGUI.getSingleton().getBookmarkManagerView().setBList(main_bookmark_list);
        return BookmarkManagerViewGUI.getSingleton();
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        setupServiceDependencyTracker(bundleContext);
    }

    private void startBookmark(BundleContext bundleContext) {
        try {
            super.start(bundleContext);
        } catch (Exception ex) {
            logger.debug(ex.getMessage(), ex);
        }
    }

    private void setupServiceDependencyTracker(final BundleContext bundleContext) {
        ServiceTracker<GenomeVersionSynonymLookup, Object> dependencyTracker;
        dependencyTracker = new ServiceTracker<GenomeVersionSynonymLookup, Object>(bundleContext, GenomeVersionSynonymLookup.class, null) {

            @Override
            public Object addingService(ServiceReference<GenomeVersionSynonymLookup> reference) {
                startBookmark(bundleContext);
                return super.addingService(reference);
            }

        };
        dependencyTracker.open();
    }
}
