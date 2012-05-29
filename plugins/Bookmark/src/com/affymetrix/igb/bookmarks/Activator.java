package com.affymetrix.igb.bookmarks;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

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
	private IGBService igbService;

	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		this.igbService = igbService;
		ResourceBundle BUNDLE = ResourceBundle.getBundle("bookmark");
		// Need to let the QuickLoad system get started-up before starting
		//   the control server that listens to ping requests?
		// Therefore start listening for http requests only after all set-up is done.
		startControlServer();

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
    		String port = CommonUtils.getInstance().getArg("-port", args);
    		if (port != null) {
    			try {
    				IGBServerSocket.setDefaultServePort(Integer.parseInt(port));
    			}
    			catch (NumberFormatException x) {
    				Logger.getLogger(IGBServerSocket.class.getName()).log(Level.SEVERE, "Invalid number " + port + " for -port in command line arguments");
    			}
    		}
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

	private void startControlServer() {
		// Use the Swing Thread to start a non-Swing thread
		// that will start the control server.
		// Thus the control server will be started only after current GUI stuff is finished,
		// but starting it won't cause the GUI to hang.

		Runnable r = new Runnable() {

			public void run() {
				new SimpleBookmarkServer(igbService);
			}
		};

		final Thread t = new Thread(r);

		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				t.start();
			}
		});
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception {
		super.stop(_bundleContext);
		if (bmark_action != null) {
			bmark_action.autoSaveBookmarks();
		}
	}
}
