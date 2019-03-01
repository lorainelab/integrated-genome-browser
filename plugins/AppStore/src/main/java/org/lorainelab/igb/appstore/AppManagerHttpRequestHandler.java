/**
 * Copyright (c) 2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package org.lorainelab.igb.appstore;

import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.lang3.StringUtils;
import org.lorainelab.igb.services.IgbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.JFrame;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 *
 * @author kkorey
 */
class AppManagerHttpRequestHandler extends NanoHTTPD {

    private final IgbService igbService;
    private static final String IGB_STATUS_CHECK = "igbStatusCheck";
    private static final String FOCUS_IGB_COMMAND = "bringIGBToFront";
    private static final String INSTALL_APP = "installApp";
    private static final String ACCESS_CONTROL_HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_HEADER = "Access-Control-Allow-Headers";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final Logger logger = LoggerFactory.getLogger(AppManagerHttpRequestHandler.class);
    private WebAppManager webAppManager;

    public AppManagerHttpRequestHandler(IgbService igbService, int port, WebAppManager webAppManager) {
        super(port);
        this.igbService = igbService;
        this.webAppManager= webAppManager;
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
            case IGB_STATUS_CHECK:
                response = new Response(handleStatusCheckRequests(session));
                response.setStatus(Response.Status.OK);
                return response;
            case FOCUS_IGB_COMMAND:
                response = new Response("OK");
                bringIgbToFront();
                response.setStatus(Response.Status.NO_CONTENT);
                return response;
            case INSTALL_APP:
                response = new Response(installHelper(session));
                bringIgbToFront();
                response.setStatus(Response.Status.NO_CONTENT);
                return response;
            default:
                response = new Response(getBadRequestMessage());
                response.setStatus(Response.Status.BAD_REQUEST);
                return response;
        }
    }

    private String installHelper(final IHTTPSession session){
        Map<String, String> queryParams = session.getParms();
        if(StringUtils.isNotBlank(queryParams.get("symbolicName"))) {
            String featureName = queryParams.get("symbolicName");
            webAppManager.installApp(featureName);
        }
        return "Installed";
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
        return igbService.getLoadedFeatureNames().contains(featureName);
    }

    private String getIgbJs() {
        return "igbIsRunning";
    }


    private String getBadRequestMessage() {
        StringBuilder msg = new StringBuilder("<html><body>");
        msg.append("<h2 style='display:inline-block'>");
        msg.append(" Invalid Request!</h2>");
        msg.append("</body></html>\n");
        return msg.toString();
    }


    private void bringIgbToFront() {
        JFrame f = igbService.getApplicationFrame();
        boolean tmp = f.isAlwaysOnTop();
        f.setAlwaysOnTop(true);
        f.toFront();
        f.requestFocus();
        f.repaint();
        f.setAlwaysOnTop(tmp);
    }

}
