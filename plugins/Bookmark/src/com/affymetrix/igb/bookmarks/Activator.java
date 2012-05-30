package com.affymetrix.igb.bookmarks;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;
import com.affymetrix.igb.bookmarks.action.BookmarkActionManager;
import com.affymetrix.igb.bookmarks.action.LoadSessionAction;
import com.affymetrix.igb.bookmarks.action.SaveSessionAction;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IStopRoutine;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	private BookmarkActionManager bmark_action;

	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		ResourceBundle BUNDLE = ResourceBundle.getBundle("bookmark");
		// Need to let the QuickLoad system get started-up before starting
		//   the control server that listens to ping requests?
		// Therefore start listening for http requests only after all set-up is done.

		if (bundleContext.getProperty("args") != null) {
			String[] args = bundleContext.getProperty("args").split(", ");
			String url = CommonUtils.getInstance().getArg("-href", args);
			if (url != null && url.length() > 0) {
				Logger.getLogger(Activator.class.getName()).log(Level.INFO,"Loading bookmark {0}",url);
				new BookMarkCommandLine(igbService, url, true);
			}else{
				url = CommonUtils.getInstance().getArg("-home", args);
				if (url != null && url.length() > 0) {
					Logger.getLogger(Activator.class.getName()).log(Level.INFO,"Loading home {0}",url);
					new BookMarkCommandLine(igbService, url, false);
				}
			}
    		String portString = CommonUtils.getInstance().getArg("-port", args);
    		if (portString != null) {
    			SimpleBookmarkServer.setServerPort(portString);
    		}
    		SimpleBookmarkServer.init(igbService);
		}

		// assuming last file menu item is Exit, leave it there
		JRPMenu file_menu = igbService.getMenu("file");
		int index = file_menu.getItemCount() - 1;
		file_menu.insertSeparator(index);
		MenuUtil.insertIntoMenu(file_menu, new JRPMenuItem("Bookmark_saveSession", new SaveSessionAction(igbService)), index);
		MenuUtil.insertIntoMenu(file_menu, new JRPMenuItem("Bookmark_loadSession", new LoadSessionAction(igbService)), index);

		JRPMenu bookmark_menu = igbService.addTopMenu("Bookmark_bookmarksMenu", BUNDLE.getString("bookmarksMenu"));
		bookmark_menu.setMnemonic(BUNDLE.getString("bookmarksMenuMnemonic").charAt(0));
		BookmarkActionManager.init(igbService, bookmark_menu);
		bmark_action = BookmarkActionManager.getInstance();
		bundleContext.registerService(IStopRoutine.class.getName(), 
			new IStopRoutine() {
				@Override
				public void stop() {
					bmark_action.autoSaveBookmarks();
				}
			},
			null
		);
		BookmarkManagerViewGUI.init(igbService);
		bmark_action.setBmv(BookmarkManagerViewGUI.getSingleton());
		return BookmarkManagerViewGUI.getSingleton();
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception {
		super.stop(_bundleContext);
		if (bmark_action != null) {
			bmark_action.autoSaveBookmarks();
		}
	}
}
