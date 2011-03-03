package com.affymetrix.igb.bookmarks;

import java.net.MalformedURLException;
import java.util.ResourceBundle;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import org.osgi.framework.BundleActivator;

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
		ResourceBundle BUNDLE = ResourceBundle.getBundle("bookmark");
		// Need to let the QuickLoad system get started-up before starting
		//   the control server that listens to ping requests?
		// Therefore start listening for http requests only after all set-up is done.
		startControlServer();

		if (bundleContext.getProperty("args") != null) {
			String[] args = bundleContext.getProperty("args").split(",");
			final String url = igbService.get_arg("-href", args);
			if (url != null && url.length() > 0) {
				goToBookmark(url);
			}
		}
		JMenu bookmark_menu = MenuUtil.getMenu(BUNDLE.getString("bookmarksMenu"));
		bookmark_menu.setMnemonic(BUNDLE.getString("bookmarksMenuMnemonic").charAt(0));
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
				new SimpleBookmarkServer(null);
			}
		};

		final Thread t = new Thread(r);

		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				t.start();
			}
		});
	}


	private void goToBookmark(final String url) {
		// If the command line contains a parameter "-href http://..." where
		// the URL is a valid IGB control bookmark, then go to that bookmark.
		try {
			final Bookmark bm = new Bookmark(null, url);
			if (bm.isUnibrowControl()) {
				SwingUtilities.invokeLater(new Runnable() {

					public void run() {
						System.out.println("Loading bookmark: " + url);
						BookmarkController.viewBookmark(igbService, bm);
					}
				});
			} else {
				System.out.println("ERROR: URL given with -href argument is not a valid bookmark: \n" + url);
			}
		} catch (MalformedURLException mue) {
			mue.printStackTrace(System.err);
		}
	}

	public void stop() {
		if (bmark_action != null) {
			bmark_action.autoSaveBookmarks();
		}
	}
}
