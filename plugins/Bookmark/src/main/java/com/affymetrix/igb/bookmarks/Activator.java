package com.affymetrix.igb.bookmarks;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.swing.AMenuItem;
import com.affymetrix.igb.swing.MenuUtil;
import com.affymetrix.igb.swing.JRPMenu;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.bookmarks.action.AddBookmarkAction;
import com.affymetrix.igb.bookmarks.action.BookmarkActionManager;
import com.affymetrix.igb.bookmarks.action.ExportBookmarkAction;
import com.affymetrix.igb.bookmarks.action.ImportBookmarkAction;
import com.affymetrix.igb.bookmarks.action.LoadSessionAction;
import com.affymetrix.igb.bookmarks.action.SaveSessionAction;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IWindowRoutine;
import com.affymetrix.igb.osgi.service.XServiceRegistrar;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

	private static final Logger ourLogger = Logger.getLogger(Activator.class.getPackage().getName());
	private static final String WILDCARD = "*";

	public Activator() {
		super(IGBService.class);
	}

	@Override
	protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
		SaveSessionAction.createAction(igbService);
		LoadSessionAction.createAction(igbService);

		// assuming last file menu item is Exit, leave it there
		JRPMenu file_menu = igbService.getMenu("file");
		final int index = file_menu.getItemCount() - 1;
		file_menu.insertSeparator(index);

		return new ServiceRegistration[]{
			bundleContext.registerService(IGBTabPanel.class, getPage(bundleContext, igbService), null),
			bundleContext.registerService(GenericAction.class, SaveSessionAction.getAction(), null),
			bundleContext.registerService(GenericAction.class, LoadSessionAction.getAction(), null),
			bundleContext.registerService(AMenuItem.class, new AMenuItem(new JRPMenuItem("Bookmark_saveSession", SaveSessionAction.getAction()), "file", index), null),
			bundleContext.registerService(AMenuItem.class, new AMenuItem(new JRPMenuItem("Bookmark_loadSession", LoadSessionAction.getAction()), "file", index), null),};
	}

	private IGBTabPanel getPage(BundleContext bundleContext, IGBService igbService) {
		ResourceBundle BUNDLE = ResourceBundle.getBundle("bookmark");

		// Need to let the QuickLoad system get started-up before starting
		//   the control server that listens to ping requests?
		// Therefore start listening for http requests only after all set-up is done.
		String[] args = CommonUtils.getInstance().getArgs(bundleContext);
		String url = CommonUtils.getInstance().getArg("-href", args);

		if (StringUtils.equals(url,WILDCARD)) {
			url = null;
		}
		if (StringUtils.isNotBlank(url)) {
			ourLogger.log(Level.INFO, "Loading bookmark {0}", url);
			new BookMarkCommandLine(bundleContext, igbService, url, true);
		} else {
			url = CommonUtils.getInstance().getArg("-home", args);
			if (StringUtils.isNotBlank(url)) {
				ourLogger.log(Level.INFO, "Loading home {0}", url);
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
		JRPMenu bookmark_menu = igbService.addTopMenu("Bookmark_bookmarksMenu", BUNDLE.getString("bookmarksMenu"));
		bookmark_menu.setMnemonic(BUNDLE.getString("bookmarksMenuMnemonic").charAt(0));
		MenuUtil.addToMenu(bookmark_menu, new JRPMenuItem("Bookmark_add_pos", AddBookmarkAction.getAction()));
		MenuUtil.addToMenu(bookmark_menu, new JRPMenuItem("Bookmark_import", ImportBookmarkAction.getAction()));
		MenuUtil.addToMenu(bookmark_menu, new JRPMenuItem("Bookmark_export", ExportBookmarkAction.getAction()));
		bookmark_menu.addSeparator();

		BookmarkActionManager.init(igbService, bookmark_menu, main_bookmark_list);
		final BookmarkActionManager bmark_action = BookmarkActionManager.getInstance();
		bundleContext.registerService(IWindowRoutine.class.getName(),
				new IWindowRoutine() {
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
		super.start(bundleContext);
	}

}