package com.affymetrix.igb.bookmarks;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;
import com.affymetrix.igb.bookmarks.action.BookmarkActionManager;
import com.affymetrix.igb.bookmarks.action.LoadSessionAction;
import com.affymetrix.igb.bookmarks.action.SaveSessionAction;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IWindowRoutine;
import com.affymetrix.igb.osgi.service.ServiceRegistrar;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator extends ServiceRegistrar implements BundleActivator {
	private static final Logger ourLogger = Logger.getLogger(Activator.class.getPackage().getName());

	@Override
	protected ServiceRegistration<?>[] registerService(IGBService igbService) throws Exception{
		SaveSessionAction.createAction(igbService);
		LoadSessionAction.createAction(igbService);

		return new ServiceRegistration[] {
			bundleContext.registerService(IGBTabPanel.class, getPage(igbService), null),
			bundleContext.registerService(GenericAction.class, SaveSessionAction.getAction(), null),
			bundleContext.registerService(GenericAction.class, LoadSessionAction.getAction(), null)
		};
	}
	
	private IGBTabPanel getPage(IGBService igbService) {
		ResourceBundle BUNDLE = ResourceBundle.getBundle("bookmark");
	
		// Need to let the QuickLoad system get started-up before starting
		//   the control server that listens to ping requests?
		// Therefore start listening for http requests only after all set-up is done.

        String[] args = CommonUtils.getInstance().getArgs(bundleContext);
		String url = CommonUtils.getInstance().getArg("-href", args);
		if (url != null && url.length() > 0) {
			ourLogger.log(Level.INFO, "Loading bookmark {0}", url);
			new BookMarkCommandLine(igbService, url, true);
		}else{
			url = CommonUtils.getInstance().getArg("-home", args);
			if (url != null && url.length() > 0) {
				ourLogger.log(Level.INFO, "Loading home {0}", url);
				new BookMarkCommandLine(igbService, url, false);
			}
		}
		String portString = CommonUtils.getInstance().getArg("-port", args);
		if (portString != null) {
			SimpleBookmarkServer.setServerPort(portString);
		}
		SimpleBookmarkServer.init(igbService);

		// assuming last file menu item is Exit, leave it there
		JRPMenu file_menu = igbService.getMenu("file");
		int index = file_menu.getItemCount() - 1;
		file_menu.insertSeparator(index);
		MenuUtil.insertIntoMenu(file_menu, new JRPMenuItem("Bookmark_saveSession", SaveSessionAction.getAction()), index);
		MenuUtil.insertIntoMenu(file_menu, new JRPMenuItem("Bookmark_loadSession", LoadSessionAction.getAction()), index);

		JRPMenu bookmark_menu = igbService.addTopMenu("Bookmark_bookmarksMenu", BUNDLE.getString("bookmarksMenu"));
		bookmark_menu.setMnemonic(BUNDLE.getString("bookmarksMenuMnemonic").charAt(0));
		BookmarkActionManager.init(igbService, bookmark_menu);
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
		bmark_action.setBmv(BookmarkManagerViewGUI.getSingleton());
		return BookmarkManagerViewGUI.getSingleton();
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
        if (CommonUtils.getInstance().isHelp(bundleContext)) {
			System.out.println("-port - bookmarks use the port specified");
			System.out.println("-single_instance - exits if a running instance of IGB is found");
        }
		//single instance?
        String[] args = CommonUtils.getInstance().getArgs(bundleContext);
		if (CommonUtils.getInstance().getArg("-single_instance", args) != null && isIGBRunning()) {
			System.out.println("\nPort "+SimpleBookmarkServer.default_server_port+" is in use! An IGB instance is likely running. Sending command to bring IGB to front. Aborting startup.\n");
			System.exit(0);
		}
		super.start(bundleContext);
	}

	/**Check to see if port 7085, the default IGB bookmarks port is open.  
	 * If so returns true AND send IGBControl a message to bring IGB's JFrame to the front.
	 * If not returns false.
	 * @author davidnix*/
	public boolean isIGBRunning(){
		Socket sock = null;
		int port = SimpleBookmarkServer.default_server_port;
		try {
		    sock = new Socket("localhost", port);
		    if (sock.isBound()) {
		    	//try to bring to front
		    	URL toSend = new URL ("http://localhost:"+port+"/IGBControl?bringIGBToFront=true");
		    	HttpURLConnection conn = (HttpURLConnection)toSend.openConnection();
		        conn.getResponseMessage();
		    	return true;
		    }
		} catch (Exception e) {
			//Don't do anything. isBound() throws an error when trying to bind a bound port
		} finally {
			try {
				if (sock != null) sock.close();
			} catch (IOException e) {}
		}
		return false;
	}

}
