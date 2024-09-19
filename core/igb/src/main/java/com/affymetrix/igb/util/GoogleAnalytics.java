package com.affymetrix.igb.util;

import com.affymetrix.common.PreferenceUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.UUID;

public class GoogleAnalytics {
    private static final Logger logger = LoggerFactory.getLogger(GoogleAnalytics.class);
    public static final String ENGAGEMENT_TIME_DEFAULT_VALUE = "10000"; //Defaulting the value to some constant milliseconds as this variable is needed to get the active users in the report
    private final String igbName;
    private final String igbVersion;
    private final String googleAnalyticsId;
    private final String googleAnalyticsApiSecret;
    public static final int HTTP_RESPONSE_OKAY = 200;
    public static final int HTTP_NO_CONTENT = 204;
    private static final String IGB_INSTANCE_FILE_NAME = "igb_instance.txt";

    public GoogleAnalytics(String igbName, String igbVersion, String googleAnalyticsId, String googleAnalyticsApiSecret) {
        this.igbName = igbName;
        this.igbVersion = igbVersion;
        this.googleAnalyticsId = googleAnalyticsId;
        this.googleAnalyticsApiSecret = googleAnalyticsApiSecret;
    }

    /**
     * This method uses Google Analytics' Measurement Protocol for GA4, which allows you to manually send
     * event data from any environment. The Measurement Protocol is useful for tracking events from
     * non-browser-based (desktop) applications like IGB. It uses client_id, which initially is a randomly
     * generated UUID but after the initial generation it is stored in a file inside .igb folder and then
     * from the next time the stored value is used. By using this approach, we ensure that we are tracking
     * the users properly as the client_id should be unique for each Client.
     * For more details, refer to the official Google Analytics Measurement Protocol documentation:
     * https://developers.google.com/analytics/devguides/collection/protocol/ga4/sending-events
     */
    public void trackEvent() {
        String clientId = getIgbInstance();
        String sessionId = UUID.randomUUID().toString();
        String url = "https://www.google-analytics.com/mp/collect?measurement_id=" + googleAnalyticsId + "&api_secret=" + googleAnalyticsApiSecret;
        JsonObject requestPayload = buildPayload(clientId, "page_view", sessionId);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new StringEntity(requestPayload.toString(), ContentType.APPLICATION_JSON));
            httpPost.setHeader("Content-Type", "application/json");

            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HTTP_RESPONSE_OKAY || statusCode == HTTP_NO_CONTENT) {
                logger.info("Session started");
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static File getIgbInstanceFile() {
        String app_dir = PreferenceUtils.getAppDataDirectory();
        File f = new File(app_dir, IGB_INSTANCE_FILE_NAME);
        return f;
    }

    private String getIgbInstance() {
        File igbInstanceFile = getIgbInstanceFile();
        String igbInstance;
        if(igbInstanceFile.exists())
            igbInstance = readIgbInstanceFromFile(igbInstanceFile);
        else {
            igbInstance = UUID.randomUUID().toString();
            saveIgbInstanceToFile(igbInstance, igbInstanceFile);
        }

        return igbInstance;
    }
    private static String readIgbInstanceFromFile(File igbInstanceFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(igbInstanceFile))) {
            return reader.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private static void saveIgbInstanceToFile(String clientId, File igbInstanceFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(igbInstanceFile))) {
            writer.write(clientId);
        } catch (IOException e) {
            logger.error("Unable to create IGB instance file, {}", e.getMessage());
        }
    }
    public JsonObject buildPayload(String clientId, String eventName, String sessionId) {
        JsonObject params = new JsonObject();
        params.addProperty("session_id", sessionId);
        params.addProperty("engagement_time_msec", ENGAGEMENT_TIME_DEFAULT_VALUE);
        params.addProperty("page_title", igbName + "-" + igbVersion);
        params.addProperty("language", "en-us");

        JsonObject event = new JsonObject();
        event.addProperty("name", eventName);
        event.add("params", params);

        JsonArray events = new JsonArray();
        events.add(event);

        JsonObject jsonPayload = new JsonObject();
        jsonPayload.addProperty("client_id", clientId);
        jsonPayload.add("events", events);

        return jsonPayload;
    }
}
