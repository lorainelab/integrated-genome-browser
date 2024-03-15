package org.lorainelab.igb.ucsc.rest.api.service.utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ApiResponseHandler implements ResponseHandler<String> {
    private static final Logger logger = LoggerFactory.getLogger(ApiResponseHandler.class);
    public static final int HTTP_RESPONSE_OKAY = 200;
    public static final int HTTP_MOVED_PERM = 301;
    public static final int HTTP_MOVED_TEMP = 302;
    public static final int HTTP_SEE_OTHER = 303;
    @Override
    public String handleResponse(HttpResponse response) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HTTP_RESPONSE_OKAY) {
                return EntityUtils.toString(response.getEntity());
            } else if (statusCode == HTTP_MOVED_PERM || statusCode == HTTP_MOVED_TEMP || statusCode == HTTP_SEE_OTHER) {
                String newApiUrl = response.getFirstHeader("Location").getValue();
                logger.info("API has moved to: " + newApiUrl);
                HttpGet redirectedRequest = new HttpGet(newApiUrl);
                return httpClient.execute(redirectedRequest, this);
            } else {
                throw new ClientProtocolException("Unexpected response status: " + statusCode);
            }
        }
    }
}