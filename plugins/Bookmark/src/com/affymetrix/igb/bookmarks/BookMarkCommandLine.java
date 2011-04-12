package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.GenometryModel;
import java.net.MalformedURLException;

import javax.swing.SwingUtilities;

import com.affymetrix.genometryImpl.event.GenericServerInitEvent;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;

import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.osgi.service.IGBService;
/**
 *
 * @author hiralv
 */
public class BookMarkCommandLine implements GenericServerInitListener{

	private final IGBService igbService;
	private final String url;

	BookMarkCommandLine(IGBService igbService, String url){
		this.igbService = igbService;
		this.url = url;
		
		// If all server are not initintialized then add listener else load bookmark.
		if(!ServerList.getServerInstance().areAllServersInited()){
			ServerList.getServerInstance().addServerInitListener(this);
		}else{
			gotoBookmark();
		}

	}

	public void genericServerInit(GenericServerInitEvent evt) {
		boolean areAllServersInited = ServerList.getServerInstance().areAllServersInited();	// do this first to avoid race condition

		if(!areAllServersInited)
			return;

		ServerList.getServerInstance().removeServerInitListener(this);

		gotoBookmark();
	}

	// If the command line contains a parameter "-href http://..." where
	// the URL is a valid IGB control bookmark, then go to that bookmark.
	private void gotoBookmark(){

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
}
