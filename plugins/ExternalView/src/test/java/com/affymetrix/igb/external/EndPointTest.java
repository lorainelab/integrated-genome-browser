/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.external;

import com.google.gson.Gson;
import java.util.Collections;
import com.google.common.base.Charsets;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;
import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;
import static junit.framework.Assert.assertTrue;

/**
 *
 * @author aloraine
 */
public class EndPointTest {
    public static HttpURLConnection connection = null;
    public static URL url = null;
    public static String json = null;
    public static Object content = null;
    public static String data = null;

    @BeforeClass
    public static void beforeAllTestMethods() {
        try {
            url = new URL(UCSCViewAction.UCSC_JSON_ENDPOINT);
        } catch (MalformedURLException ex) {
            fail("UCSC JSON API endpoint should be a URL.");
        }

        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException ex) {
            fail("UCSC JSON API endpoint should allow opening a connection.");
        }
        try {
            data = Resources.toString(url, Charsets.UTF_8);

        } catch (IOException ex) {
            fail("UCSC JSON API endpoint should provide content.");
        }
        /**
         * try { content = url.getContent(); } catch (IOException ex) {
         * fail("UCSC JSON API endpoint should provide content."); }
         */

    }

    @Test
    public void testResponseCode() {
        String message = "Response code should be HTTP_OK";
        int right_answer = HttpURLConnection.HTTP_OK;
        int given_answer = -1;
        try {
            given_answer = connection.getResponseCode();
        } catch (IOException ex) {
            fail(message);
        }
        assertEquals(message, right_answer, given_answer);
    }

    @Test
    public void testEndpointContentType() {
        String message = "Content should be JSON";
        String right_answer = "application/json";
        String given_answer = connection.getContentType();
        assertEquals(message, right_answer, given_answer);
    }

    @Test
    public void testContent() {
        String message = "Content should be parseable JSON";
        Map<String, Object> map = new Gson().fromJson(
                data, new TypeToken<HashMap<String, Object>>() {
                }.getType()
        );
        assertNotNull(map);
    }

    @Test
    public void testHasRightKey() {
        String message = "Content should contain key ucscGenomes.";
        Map<String, Object> map = new Gson().fromJson(
                data, new TypeToken<HashMap<String, Object>>() {
                }.getType()
        );
        boolean result = false;
        try {
            result = map.containsKey("ucscGenomes");
        } catch (NullPointerException ex) {
            fail(message);
        }
        assertTrue(result);
    }

}
