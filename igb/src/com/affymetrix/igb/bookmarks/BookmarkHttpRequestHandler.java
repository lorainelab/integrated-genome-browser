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

import com.affymetrix.igb.Application;
import java.io.*;
import java.net.*;
import java.util.*;

class BookmarkHttpRequestHandler implements Runnable {

  final static String CRLF = "\r\n";
  Socket socket;
  Application app;

  public BookmarkHttpRequestHandler(Application app, Socket socket) {
    this.socket = socket;
    this.app = app;
  }

  public static final String NO_CONTENT = "HTTP/1.x 204 No Content\r\n\r\n";

  public void run() {
    try {
      processRequest();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void processRequest() throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    OutputStream output = socket.getOutputStream();
    
    String line;
    while ((line = reader.readLine()) != null) {
      // we need to process only the GET header line of the input, which will
      // look something like this:
      // 'GET /IGBControl?version=hg18&seqid=chr17&start=43966897&end=44063310 HTTP/1.1'

      String command = null;
      if (line.length() >= 4 && line.substring(0,4).toUpperCase().equals("GET ")) {
        String[] getCommand = line.substring(4).split(" ");
        if (getCommand.length > 0) {
          command = getCommand[0];
        }
      }


      if (command != null) {

        Application.logDebug("Command = " + command);

        // at this point, the command will look something like this:
        // '/IGBControl?version=hg18&seqid=chr17&start=43966897&end=44063310'

        //TODO: We could check to see that the command is "IGBControl" or "UnibrowControl",
        // but since that is the only command we ever expect, we can just assume for now.
        String params = null;
        int index = command.indexOf('?');
        if (index >= 0 && index < command.length()) {
          params = command.substring(index+1);
          Map paramMap = new HashMap();

          Bookmark.parseParametersFromQuery(paramMap, params, true);
          UnibrowControlServlet.goToBookmark(app, paramMap);
        }
        
        output.write(NO_CONTENT.getBytes());
      }
    }

    try {
      output.close();
      reader.close();
      socket.close();
    } catch (Exception e) {
      // do nothing
    }
  }
}
