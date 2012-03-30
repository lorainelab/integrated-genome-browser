package com.affymetrix.main;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import com.affymetrix.igb.bookmarks.SimpleBookmarkServer;


/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

/**
 *  Main class for the Integrated Genome Browser (IGB, pronounced ig-bee).
 *
 */
public final class Main {
	/**
	 * Start the program. Nothing to it, just start OSGi and
	 * all the bundles.
	 */
	public static void main(final String[] args) {
		try {
			//check to see if IGB is already running, if so bring it to the front, if not launch it.
			if (isIGBRunning() == false) {
				OSGiHandler.getInstance().startOSGi(args);
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}
	
	/**Check to see if port 7085, the default IGB bookmarks port is open.  
	 * If so returns true AND send IGBControl a message to bring IGB's JFrame to the front.
	 * If not returns false.
	 * @author davidnix*/
	public static boolean isIGBRunning(){
		Socket sock = null;
		int port = SimpleBookmarkServer.default_server_port;
		try {
		    sock = new Socket("localhost", port);
		    if (sock.isBound()) {
		    	System.err.println("\nPort "+port+" is in use! Thus an IGB instance is likely running. Sending command to bring IGB to front. Aborting startup.\n");
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
