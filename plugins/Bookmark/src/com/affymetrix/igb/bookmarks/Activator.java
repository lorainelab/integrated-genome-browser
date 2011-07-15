package com.affymetrix.igb.bookmarks;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IStopRoutine;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	private BookMarkAction bmark_action;
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
			String url = igbService.get_arg("-href", args);
			if (url != null && url.length() > 0) {
				Logger.getLogger(Activator.class.getName()).log(Level.INFO,"Loading bookmark {0}",url);
				new BookMarkCommandLine(igbService, url, true);
			}else{
				url = igbService.get_arg("-home", args);
				if (url != null && url.length() > 0) {
					Logger.getLogger(Activator.class.getName()).log(Level.INFO,"Loading home {0}",url);
					new BookMarkCommandLine(igbService, url, false);
				}
			}
		}

		// assuming last file menu item is Exit, leave it there
		JRPMenu file_menu = igbService.getFileMenu();
		int index = file_menu.getItemCount() - 1;
		file_menu.insertSeparator(index);
		MenuUtil.insertIntoMenu(file_menu, new JRPMenuItem("Bookmark.saveSession", new SaveSessionAction(igbService)), index);
		MenuUtil.insertIntoMenu(file_menu, new JRPMenuItem("Bookmark.loadSession", new LoadSessionAction(igbService)), index);

		JRPMenu bookmark_menu = MenuUtil.getRPMenu("Bookmark.bookmarksMenu", BUNDLE.getString("bookmarksMenu"));
		bookmark_menu.setMnemonic(BUNDLE.getString("bookmarksMenuMnemonic").charAt(0));
		bmark_action = new BookMarkAction(igbService, bookmark_menu);
		igbService.addStopRoutine(
			new IStopRoutine() {
				@Override
				public void stop() {
					bmark_action.autoSaveBookmarks();
				}
			}
		);
		BookmarkManagerView bmv = new BookmarkManagerView(igbService, bmark_action);
		bmark_action.setBmv(bmv);
		return bmv;
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
