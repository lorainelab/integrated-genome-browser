package com.affymetrix.igb.external;

import com.google.gson.Gson;
import com.google.common.base.Charsets;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;

/**
 * Check that UCSC JSON REST API is available and working as required.
 */
public class EndPointTest {
    
    private static URL url = null;
    private static HttpURLConnection connection = null;
    private static String data = null;

    @BeforeAll
    public static void beforeAllTestMethods() {
        try {
            url = new URL(UCSCViewAction.UCSC_JSON_ENDPOINT);
        } catch (MalformedURLException ex) {
            Assertions.fail("UCSC JSON API endpoint should be a URL.");
        }
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException ex) {
            Assertions.fail("UCSC JSON API endpoint should allow opening a connection.");
        }
        try {
            data = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException ex) {
            Assertions.fail("UCSC JSON API endpoint should provide content.");
        }
    }

    @Test
    public void testResponseCode() {
        String message = "Response code should be HTTP_OK.";
        int right_answer = HttpURLConnection.HTTP_OK;
        int given_answer = -1;
        try {
            given_answer = connection.getResponseCode();
        } catch (IOException ex) {
            Assertions.fail(message);
        }
        Assertions.assertEquals(right_answer, given_answer, message);
    }

    @Test
    public void testEndpointContentType() {
        String right_answer = "application/json";
        String message = "Content should be " + right_answer + ".";
        String given_answer = connection.getContentType();
        Assertions.assertEquals(right_answer, given_answer, message);
    }

    @Test
    public void testContent() {
        String message = "Content should be parseable JSON";
        Map<String, Object> map = new Gson().fromJson(
                data, new TypeToken<HashMap<String, Object>>() {}.getType()
        );
        Assertions.assertNotNull(map, message);
    }

    @Test
    public void testHasRightKey() {
        String message = "Content should contain key ucscGenomes.";
        Map<String, Object> map = new Gson().fromJson(
                data, new TypeToken<HashMap<String, Object>>() {}.getType()
        );
        boolean result = map != null && map.containsKey("ucscGenomes");
        Assertions.assertTrue(result, message);
    }
    
    @Test
    public void testSomeGenomes() {
        String message = "Genome versions should be available.";
        Map<String, Object> map = new Gson().fromJson(
                data, new TypeToken<HashMap<String, Object>>() {}.getType()
        ); 
        Map submap = null;
        if (map != null && map.containsKey("ucscGenomes")) {
            submap = (Map) map.get("ucscGenomes");
            if (submap != null) {
                submap = (Map) submap.get("hg19");
            }
        }
        Assertions.assertNotNull(submap, message);
    }
}
