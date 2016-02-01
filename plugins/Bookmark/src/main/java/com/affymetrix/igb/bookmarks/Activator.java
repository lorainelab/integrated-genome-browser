package com.affymetrix.igb.bookmarks;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.igb.bookmarks.action.BookmarkActionManager;
import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.commons.lang3.StringUtils;
import org.lorainelab.igb.services.IgbService;
import static org.lorainelab.igb.services.ServiceComponentNameReference.BOOKMARK_TAB;
import static org.lorainelab.igb.services.ServiceComponentNameReference.COMPONENT_NAME;
import org.lorainelab.igb.services.XServiceRegistrar;
import org.lorainelab.igb.services.window.WindowServiceLifecycleHook;
import org.lorainelab.igb.services.window.tabs.IgbTabPanel;
import org.lorainelab.igb.services.window.tabs.IgbTabPanelI;
import org.lorainelab.igb.synonymlookup.services.GenomeVersionSynonymLookup;
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
        Dictionary<String, String> props = new Hashtable<>();
        props.put("service.pid", "BookmarkManagerViewGUI");
        props.put(COMPONENT_NAME, BOOKMARK_TAB);
        return new ServiceRegistration[]{
            bundleContext.registerService(IgbTabPanelI.class, getPage(bundleContext, igbService), props)
        };
    }

    private IgbTabPanel getPage(BundleContext bundleContext, IgbService igbService) {
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

        BookmarkList mainBookmarkList = new BookmarkList("Bookmarks");

        BookmarkActionManager.init(igbService, bookmarkMenu, mainBookmarkList);
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
        BookmarkManagerViewGUI.getSingleton().getBookmarkManagerView().setBList(mainBookmarkList);
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
