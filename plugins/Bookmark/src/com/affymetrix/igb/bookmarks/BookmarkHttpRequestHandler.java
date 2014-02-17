/**
*   Copyright (c) 2007 Affymetrix, Inc.
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
package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.osgi.service.IGBService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

class BookmarkHttpRequestHandler implements Runnable {

  private final Socket socket;
  private final IGBService igbService;
  private static final Logger ourLogger
		  = Logger.getLogger(BookmarkHttpRequestHandler.class.getPackage().getName());

  public BookmarkHttpRequestHandler(IGBService igbService, Socket socket) {
    this.socket = socket;
    this.igbService = igbService;
  }

  @Override
  public void run() {
    try {
      processRequest();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

 private void processRequest() throws IOException {
		BufferedReader reader = null;
		PrintWriter out = null;
		
		try {
			String line;
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream());
			
			String command = null;
			line = reader.readLine();
			/*
			Headers are formated as follows Example
			GET /
			name: value
			name: value
			
			content
			content content 
			content
			*/
			// we need to process only the GET header line of the input, which will look something like this:
			// 'GET /IGBControl?version=hg18&seqid=chr17&start=43966897&end=44063310 HTTP/1.1'
			
			if(line.startsWith("GET ") || line.startsWith("POST ")){
				String[] requestLine = line.split(" ");
				String path = requestLine[1];
				if(path.startsWith("/favicon.ico")){ //TODO Add a favicon.ico
						return; //note finally will still be called!!
				}
				String[] getCommand = line.substring(4).split(" ");
				if(path.equals("/")){//send a simple message for the user to see 
						out.println("HTTP/1.1 200 OK");
						out.println("Content-Type: text/html");
						out.println("\r\n");
						out.println("<html><head><title>IGB</title></head><body>");
						out.println("<h2 style='display:inline-block'>IGB is running!</h2> <a href='http://localhost:7085/UnibrowControl?bringIGBToFront=true'>bringing it to the front!</a><br>");
						out.println("<h4>Helpful Links:</h4>");
						out.println("<ul>");
						out.println("<li>If you are trying to run IGB commands head over to <a href='http://wiki.transvar.org/confluence/display/igbman/Controlling+IGB+using+IGB+Links'>How to use IGB links</a></li>");
						out.println("<li>Data sets can be found at <a href='http://igbquickload.org/'>igbquickload.org</a></li>");
						out.println("<li>Newer versions of IGB can be downloaded on <a href='http://bioviz.org'>bioviz.org</a></li>");
						out.println("<li>For the latest news go over to the <a href='https://twitter.com/igbbioviz'>Twitter page</a></li>");
						out.println("<li>If you need more help there are plenty of \"How Tos\" over on our <a href='http://www.youtube.com/channel/UC0DA2d3YdbQ55ljkRKHRBkg'>Youtube channel</a></li>");
						out.println("</ul>");
						out.println("<h4><a href='http://bioviz.org/igb/cite.html'>How to cite IGB</a> in your research</h4>");
						out.println("<div style='position:absolute;top:1em;right:1em;'><a href='https://wiki.transvar.org/confluence/display/igbman/Quick+start'>Users Guide and Help</a></div>");
						out.println("<div style='position:absolute;bottom:0;right:0;left:0;background:white;text-align:center;width:100%;padding-bottom:1em'>IGB is a product of the <a href='http://transvar.org/'>Loraine Lab</a> and is <a href='http://sourceforge.net/projects/genoviz/'>open source</a></div>");
						out.println("</body></html>");
						out.flush();
						return;
					}
					//feature not approved yet! -kts
					//else if(path.startsWith("/IGB.js")){//send javascript file and close socket
					//	out.println("HTTP/1.1 200 OK");
					//	out.println("Content-Type: text/javascript");
					//	out.println("\r\n");
					//	out.println("var IGB={};");
					//	out.flush();
					//	break;
					//}
					else if(getCommand.length > 0) {
						command = getCommand[0];
						out.write(SimpleBookmarkServer.http_response);
						out.flush();
						parseAndGoToBookmark(command);
					}else{
						out.println("HTTP/1.1 400 Bad Request");
						out.println("Content-Type: text/html");
						out.println("\r\n");
						out.println("<h2>Error with IGB command!</h2><pre>>> " + line +"</pre>");
					    out.flush();
						return;
					}
				
			do{//skip rest of header
			}while ((line = reader.readLine()) != null || line.trim().length() > 0); //same as looking for "\r\n\r\n" which indicates end of header
			}
			
			if(line!=null){
				do{
					igbService.runScriptString(line, "igb");
//					output.write(SimpleBookmarkServer.prompt);
				}while ((line = reader.readLine()) != null); //same as looking for "\r\n\r\n" which indicates end of header
			}
			
		
		} finally {

			GeneralUtils.safeClose(out);
			GeneralUtils.safeClose(reader);
			try {
				socket.close();
			} catch (Exception e) {
				// do nothing
			}
		}

	}

	private void parseAndGoToBookmark(String command) throws NumberFormatException {
		ourLogger.log(Level.FINE, "Command = {0}", command);
		// at this point, the command will look something like this:
		// '/IGBControl?version=hg18&seqid=chr17&start=43966897&end=44063310'
		//TODO: We could check to see that the command is "IGBControl" or "UnibrowControl",
		// but since that is the only command we ever expect, we can just assume for now.
		int index = command.indexOf('?');
		if (index >= 0 && index < command.length()) {
			String params = command.substring(index + 1);
			Map<String, String[]> paramMap = new HashMap<String, String[]>();
			Bookmark.parseParametersFromQuery(paramMap, params, true);
			if(paramMap.containsKey("bringIGBToFront")){
				JFrame f = igbService.getFrame();
				boolean tmp = f.isAlwaysOnTop();
				f.setAlwaysOnTop(true);
				f.toFront();
				f.requestFocus();
				f.repaint();
				f.setAlwaysOnTop(tmp);
			}
			BookmarkUnibrowControlServlet.getInstance().goToBookmark(igbService, paramMap);
		}
	}
}
