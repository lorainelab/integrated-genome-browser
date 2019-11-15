package org.lorainelab.igb.appstore;



import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import java.io.IOException;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

import org.lorainelab.igb.plugin.manager.service.PluginManagerService;

/**
 * HTTP server that implements REST services for installing IGB Apps.
 *
 * @author Kiran Korey, Ann Loraine, others TBN
 * 
 * 
 * Not an SCR Component -- creation managed by "launcher" class that creats
 * and starts the server in a thread
 */
class IgbAppServer extends NanoHTTPD {

    private static final int PORT = 7090;
    private static final String MANAGE_APP = "manageApp";
    private static final String ACCESS_CONTROL_HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
    private static final Logger logger = LoggerFactory.getLogger(IgbAppServer.class);
    

    PluginManagerService pluginManagerService;
    
    public IgbAppServer() {
        super(PORT);
    }
    
    //@Reference
    /**
    protected void setWebAppManager(WebAppManager app) {
        this.webAppManager=app;
    }
    */
    
    protected void setPluginManagerService(PluginManagerService pluginManagerService) {
        this.pluginManagerService = pluginManagerService;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response response = null;
        Method method = session.getMethod();
        switch(method) {
            case OPTIONS: 
                response = new NanoHTTPD.Response(Status.OK, MIME_PLAINTEXT ,"");
                break;
            case GET:
            case POST:
                response = processRequest(session);
                break;
            case PUT:
            case DELETE:
            case HEAD:
            default :
                break;
        }
        if(response != null) {
        response.addHeader(ACCESS_CONTROL_HEADER_ALLOW_ORIGIN, "*");
        response.addHeader(ACCESS_CONTROL_MAX_AGE, "3628800");
        response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
        response.addHeader(ACCESS_CONTROL_ALLOW_HEADERS, "*");
        }
        return response;
    }

    /**
     * Process the given request from a client attempting to install, update,
     * or get information about an IGB App that can be installed and run.
     * 
     * @param session - Contains REST endpoint request. 
     * @return - Response object indicating result of performing requested action. 
     */
    private Response processRequest(final IHTTPSession session) {
        String contextRoot = session.getUri().substring(1); //removes prefixed /
        Response response;
        switch (contextRoot) {
            case MANAGE_APP:
                logger.info("contextRoot: {}",contextRoot);
                response = manageApp(session);
                break; //IGBF-1608 : Add break statement to prevent getting default message always
            default:
                response = new Response("Igb is running.");
                response.setStatus(Response.Status.OK);
        }
        return response;
    }

    /**
     * Identify which App our user wants to install and install it.
     * Note that IGB should already "know" about the requested App,
     * which should also be available to install or un-install via
     * the IGB App Manager GUI.
     * 
     * @param session - HTTP request triggering this action
     * @return Response with code OK if App was installed without
     * error, BAD_REQUEST if not. 
     */
    private Response manageApp(final IHTTPSession session){
        Map<String, String> requestParams = new HashMap<>();
        try {
            session.parseBody(requestParams);
        } catch (IOException ioe) {
            return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
        } catch (ResponseException re) {
            return new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
        }
        JsonObject body = new JsonParser().parse(session.getQueryParameterString()).getAsJsonObject();
        return pluginManagerService.manageApp(body); 
       
    }
    
}
