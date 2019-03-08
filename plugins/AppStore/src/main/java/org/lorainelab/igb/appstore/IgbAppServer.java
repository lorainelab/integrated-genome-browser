package org.lorainelab.igb.appstore;

import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.lang3.StringUtils;
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
    private static final String IGB_STATUS_CHECK = "igbStatusCheck";
    private static final String INSTALL_APP = "installApp";
    private static final String ACCESS_CONTROL_HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_HEADER = "Access-Control-Allow-Headers";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
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
        Response response;
        Method method = session.getMethod();
        if (method.equals(Method.GET)) {
            response = processRequest(session);
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
            case INSTALL_APP:
                logger.info("contextRoot: {}",contextRoot);
                response = installApp(session);
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
     * which should also be availabnle to install or un-install via
     * the IGB App Manager GUI.
     * 
     * @param session - HTTP request triggering this action
     * @return Response with code OK if App was installed without
     * error, BAD_REQUEST if not. 
     */
    private Response installApp(final IHTTPSession session){
        Response toReturn;
        Map<String, String> queryParams = session.getParms();
        
        if(StringUtils.isNotBlank(queryParams.get("symbolicName"))) {
            String symbolicName = queryParams.get("symbolicName");
            boolean isAppInstalled = pluginManagerService.installApp(symbolicName); // how to check that it worked?
            //IGBF-1608 : Send more informative response to the app installation request
            if(isAppInstalled) {
                String outcome = String.format("Installed %s",symbolicName);
                toReturn = new Response(outcome);
                toReturn.setStatus(Response.Status.OK);
            } else {
                toReturn = new Response("App not found");
                toReturn.setStatus(Response.Status.NOT_FOUND);
            }
            //IGBF-1608 : end
        }
        else {
            toReturn = new Response("No symbolic name.");
            toReturn.setStatus(Response.Status.BAD_REQUEST);
        }
        return toReturn;
    }
    
}
