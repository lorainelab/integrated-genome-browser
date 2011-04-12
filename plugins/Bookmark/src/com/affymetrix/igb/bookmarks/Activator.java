package com.affymetrix.igb.bookmarks;


import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IStopRoutine;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	private BookMarkAction bmark_action;
	private IGBService igbService;

	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		this.igbService = igbService;
		// Need to let the QuickLoad system get started-up before starting
		//   the control server that listens to ping requests?
		// Therefore start listening for http requests only after all set-up is done.
		startControlServer();

		if (bundleContext.getProperty("args") != null) {
			String[] args = bundleContext.getProperty("args").split(", ");
			final String url = igbService.get_arg("-href", args);
			if (url != null && url.length() > 0) {
				new BookMarkCommandLine(igbService, url);
			}
		}
		// add to main file menu
		JMenu file_menu = igbService.getFileMenu();
		int count = file_menu.getMenuComponentCount();
		// assumes last file menu items are separator and close
		MenuUtil.insertIntoMenu(file_menu, new JMenuItem(new SaveSessionAction(igbService)), count - 2);
		MenuUtil.insertIntoMenu(file_menu, new JMenuItem(new LoadSessionAction(igbService)), count - 2);
		// add main bookmark menu
		JMenu bookmark_menu = MenuUtil.getMenu(BookmarkManagerView.BUNDLE.getString("bookmarksMenu"));
		bookmark_menu.setMnemonic(BookmarkManagerView.BUNDLE.getString("bookmarksMenuMnemonic").charAt(0));
		bmark_action = new BookMarkAction(igbService, (SeqMapView)igbService.getMapView(), bookmark_menu);
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
