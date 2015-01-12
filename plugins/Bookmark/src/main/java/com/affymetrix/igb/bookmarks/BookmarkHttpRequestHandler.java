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

import static com.affymetrix.igb.bookmarks.BookmarkConstants.FAVICON_REQUEST;
import static com.affymetrix.igb.bookmarks.BookmarkConstants.GALAXY_REQUEST;
import static com.affymetrix.igb.bookmarks.BookmarkConstants.SERVLET_NAME;
import static com.affymetrix.igb.bookmarks.BookmarkConstants.SERVLET_NAME_OLD;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.DataManagementTableInfo;
import com.google.common.collect.ListMultimap;
import fi.iki.elonen.NanoHTTPD;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import javax.swing.JFrame;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BookmarkHttpRequestHandler extends NanoHTTPD {

    private final IGBService igbService;
    private static final String IGB_STATUS_CHECK = "igbStatusCheck";
    private static final String FOCUS_IGB_COMMAND = "bringIGBToFront";
    private static final String ACCESS_CONTROL_HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_HEADER = "Access-Control-Allow-Headers";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final Logger logger = LoggerFactory.getLogger(BookmarkHttpRequestHandler.class);    

    public BookmarkHttpRequestHandler(IGBService igbService, int port) {
        super(port);
        this.igbService = igbService;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response response;
        Method method = session.getMethod();
        if (method.equals(Method.GET)) {
            response = processRequest(session);
        } else if (method.equals(Method.POST)) {
            response = new Response(getNotSupportedMessage(method));
            response.setStatus(Response.Status.METHOD_NOT_ALLOWED);
        } else {
            response = new Response(getNotSupportedMessage(method));
            response.setStatus(Response.Status.METHOD_NOT_ALLOWED);
        }
        response.addHeader(ACCESS_CONTROL_HEADER_ALLOW_ORIGIN, "*");
        response.addHeader(ACCESS_CONTROL_ALLOW_HEADER, "Origin, Content-Type");
        response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, "GET");
        return response;
    }

    private String getNotSupportedMessage(Method method) {
        StringBuilder msg = new StringBuilder("<html><body>");
        msg.append("<h2 style='display:inline-block'>");
        msg.append(method.name().toUpperCase());
        msg.append(" is not supported!</h2>");
        msg.append("</body></html>\n");
        return msg.toString();
    }

    private Response processRequest(final IHTTPSession session) {
        String contextRoot = session.getUri().substring(1); //removes prefixed /
        Response response;
        switch (contextRoot) {
            case SERVLET_NAME_OLD:
            case SERVLET_NAME:
                parseAndGoToBookmark(session, false);
                response = new Response(getWelcomeMessage());
                response.setStatus(Response.Status.NO_CONTENT);
                return response;
            case GALAXY_REQUEST:
                //This exist to allow custom pipeline for galaxy requests if desired
                parseAndGoToBookmark(session, true);
                response = new Response(getWelcomeMessage());
                response.setStatus(Response.Status.OK);
                return response;
            case FAVICON_REQUEST:
                //do nothing send back welcome message
                response = new Response(getWelcomeMessage());
                response.setStatus(Response.Status.OK);
            return response;
            case FOCUS_IGB_COMMAND:
                response = new Response("OK");
                bringIgbToFront();
                response.setStatus(Response.Status.NO_CONTENT);
            return response;
            case IGB_STATUS_CHECK:
                response = new Response(handleStatusCheckRequests(session));
                response.setStatus(Response.Status.OK);
                return response;
            default:
                response = new Response(getBadRequestMessage());
                response.setStatus(Response.Status.BAD_REQUEST);
            return response;
        }
    }

    private String handleStatusCheckRequests(final IHTTPSession session) {
        Map<String, String> queryParams = session.getParms();
        if (queryParams.isEmpty()) {
            return getIgbJs();
        } else if (StringUtils.isNotBlank(queryParams.get("checkLoadStatusForDataSet"))) {
            String featureName = queryParams.get("checkLoadStatusForDataSet");
            featureName = StringUtils.substringAfterLast(featureName, "/");
            if (isDataSetLoaded(featureName)) {
                return "complete";
            }
        } else if (StringUtils.isNotBlank(queryParams.get("query_url"))) {
            String query_url = queryParams.get("query_url");
            if (remoteFileExists(query_url)) {
                return "var remoteFileExists = true";
            }
            return "";
        }
        return "";
    }

    private static boolean remoteFileExists(String url) {
        try {
            HttpURLConnection.setFollowRedirects(true);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("HEAD");
            if (con.getResponseCode() == 307) {
                //try https before failing
                url = url.replace("http://", "https://");
                con = (HttpURLConnection) new URL(url).openConnection();
                return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
            } else {
                return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isDataSetLoaded(String featureName) {
        if (StringUtils.isBlank(featureName)) {
            return false;
        }
        return DataManagementTableInfo.getDataManagementTableTrackNames().contains(featureName);
    }

    private String getIgbJs() {
        return "var igbIsRunning = true";
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
                + "      <a class='btn btn-primary' href='http://localhost:7085/UnibrowControl?bringIGBToFront=true'>Click to go to IGB</a>"
                + "    </div>"
        );

        msg.append("</body></html>");
        return msg.toString();
    }

    private String getBadRequestMessage() {
        StringBuilder msg = new StringBuilder("<html><body>");
        msg.append("<h2 style='display:inline-block'>");
        msg.append(" Invalid Request!</h2>");
        msg.append("</body></html>\n");
        return msg.toString();
    }

    private void parseAndGoToBookmark(final IHTTPSession session, boolean isGalaxyBookmark) throws NumberFormatException {
        String params = session.getQueryParameterString();
        logger.trace("Command = {}", params);
        //TODO refactor all of this code... there is no need to manually parse the request
        ListMultimap<String, String> paramMap = Bookmark.parseParametersFromQuery(params);
        if (paramMap.containsKey(FOCUS_IGB_COMMAND)) {
            bringIgbToFront();
            if (paramMap.size() == 1) {
                return;
            }
        }
        BookmarkUnibrowControlServlet.getInstance().goToBookmark(igbService, paramMap, isGalaxyBookmark);

    }

    private void bringIgbToFront() {
        JFrame f = igbService.getFrame();
        boolean tmp = f.isAlwaysOnTop();
        f.setAlwaysOnTop(true);
        f.toFront();
        f.requestFocus();
        f.repaint();
        f.setAlwaysOnTop(tmp);
    }

}
