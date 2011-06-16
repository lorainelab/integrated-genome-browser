package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.GenometryModel;
import java.net.MalformedURLException;

import javax.swing.SwingUtilities;

import com.affymetrix.genometryImpl.event.GenericServerInitEvent;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;

import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.osgi.service.IGBService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 */
public class BookMarkCommandLine implements GenericServerInitListener{

	private final IGBService igbService;
	private final String url;
	private final boolean force;

	BookMarkCommandLine(IGBService igbService, String url, boolean force){
		this.igbService = igbService;
		this.url = url;
		this.force = force;
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
		GenometryModel gmodel = GenometryModel.getGenometryModel();

		// If it is -home then do not force to switch unless no species is selected.
		if(!force && gmodel.getSelectedSeqGroup() != null && gmodel.getSelectedSeq() != null){
			Logger.getLogger(BookMarkCommandLine.class.getName()).log(Level.WARNING,"Previous speceis already loaded. Home {0} will be not loaded", url);
			return;
		}

		try {
			final Bookmark bm = new Bookmark(null, url);
			if (bm.isUnibrowControl()) {
				SwingUtilities.invokeLater(new Runnable() {

					public void run() {
						Logger.getLogger(BookMarkCommandLine.class.getName()).log(Level.INFO, "Loading bookmark: {0}", url);
						BookmarkController.viewBookmark(igbService, bm);
					}
				});
			} else {
				Logger.getLogger(BookMarkCommandLine.class.getName()).log(Level.SEVERE, "ERROR: URL given with -href argument is not a valid bookmark: \n{0}", url);
			}
		} catch (MalformedURLException mue) {
			mue.printStackTrace(System.err);
		}
	}
}
