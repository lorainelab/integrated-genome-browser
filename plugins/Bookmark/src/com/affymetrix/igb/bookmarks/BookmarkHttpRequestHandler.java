/**
 * Copyright (c) 2007 Affymetrix, Inc.
 * 
* Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 * 
* The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.bookmarks;

import static com.affymetrix.igb.bookmarks.SimpleBookmarkServer.SERVLET_NAME;
import static com.affymetrix.igb.bookmarks.SimpleBookmarkServer.SERVLET_NAME_OLD;
import com.affymetrix.igb.osgi.service.IGBService;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.apache.commons.io.IOUtils;

class BookmarkHttpRequestHandler extends NanoHTTPD {

    private final IGBService igbService;
    private static final String GALAXY_REQUEST = "igbGalaxyDataView";
    private static final String DEFAULT_SCRIPT_EXTENSION = "igb";
    private static final String FAVICON_REQUEST = "favicon.ico";
    private static final String FOCUS_IGB_COMMAND = "bringIGBToFront";
    private static final Logger ourLogger
            = Logger.getLogger(BookmarkHttpRequestHandler.class.getPackage().getName());

    public BookmarkHttpRequestHandler(IGBService igbService, int port) {
        super(port);
        this.igbService = igbService;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response response;
        Method method = session.getMethod();
        if (method.equals(Method.GET)) {
            processRequest(session);
            response = new Response(getWelcomeMessage());
            response.setStatus(Response.Status.OK);
        } else if (method.equals(Method.POST)) {            
//            processPost(session);
//            response = new Response(getWelcomeMessage());
//            response.setStatus(Response.Status.OK);
            response = new Response(getNotSupportedMessage(method));
            response.setStatus(Response.Status.METHOD_NOT_ALLOWED);
        } else {
            response = new Response(getNotSupportedMessage(method));
            response.setStatus(Response.Status.METHOD_NOT_ALLOWED);
        }
        return response;
    }

    private String getWelcomeMessage() {
        StringBuilder msg = new StringBuilder("<html>");
        msg.append("<link rel=\"stylesheet\" href=\"//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css\">");
        msg.append("<body>");
        msg.append("     <div align='center'>"
                + "        <h1>"
                + "          Integrated Genome Browser"
                + "        </h1>"
                + "      <h2>"
                + "        Visualization for genome-scale data"
                + "      </h2>"
                + "    </div>"
                + "      <hr/>"
                + "    <div class='well' align='center'>"
                + "      <h3>"
                + "        Thank you for using IGB! "
                + "      </h3>"
                + "      <p>"
                + "        Your Data is Loading"
                + "      </p>"
                + "      <a class='btn btn-primary' href='http://localhost:7085/UnibrowControl?bringIGBToFront=true'>Click to go to IGB</a>"
                + "    </div>"
        );

        msg.append("</body></html>");
        return msg.toString();
    }

    private String getNotSupportedMessage(Method method) {
        StringBuilder msg = new StringBuilder("<html><body>");
        msg.append("<h2 style='display:inline-block'>");
        msg.append(method.name().toUpperCase());
        msg.append(" is not supported!</h2>");
        msg.append("</body></html>\n");
        return msg.toString();
    }

    private void processRequest(final IHTTPSession session) {
        String contextRoot = session.getUri().substring(1); //removes prefixed /

        if (contextRoot.equals(SERVLET_NAME_OLD) || contextRoot.equals(SERVLET_NAME)) {
            parseAndGoToBookmark(session);
        } else if (contextRoot.equals(GALAXY_REQUEST)) {
            //This exist to allow custom pipeline for galaxy requests if desired
            parseAndGoToBookmark(session);
        } else if (contextRoot.equals(FAVICON_REQUEST)) {
            //for now do nothing
        }
    }

    //This code can be deleted once it is confirmed we have no need to support post
    @Deprecated
    private void processPost(final IHTTPSession session) {
        String scriptContent = getRequestContent(session);
        igbService.runScriptString(scriptContent, DEFAULT_SCRIPT_EXTENSION);
    }
    
    //This code can be deleted once it is confirmed we have no need to support post
    @Deprecated
    private String getRequestContent(final IHTTPSession session) {
        String requestContent = null;
        try {
            requestContent = IOUtils.toString(session.getInputStream(), "UTF-8");
        } catch (IOException ex) {
            ourLogger.log(Level.SEVERE, "Could not extract request content", ex);
        }
        return requestContent;
    }

    
    private void parseAndGoToBookmark(final IHTTPSession session) throws NumberFormatException {
        String params = session.getQueryParameterString();
        ourLogger.log(Level.FINE, "Command = {0}", params);
        //TODO refactor all of this code... there is no need to manually parse the request
        Map<String, String[]> paramMap = new HashMap<String, String[]>();
        Bookmark.parseParametersFromQuery(paramMap, params, true);
        if (paramMap.containsKey(FOCUS_IGB_COMMAND)) {
            JFrame f = igbService.getFrame();
            boolean tmp = f.isAlwaysOnTop();
            f.setAlwaysOnTop(true);
            f.toFront();
            f.requestFocus();
            f.repaint();
            f.setAlwaysOnTop(tmp);
        } else {
            BookmarkUnibrowControlServlet.getInstance().goToBookmark(igbService, paramMap);
        }

    }

}
