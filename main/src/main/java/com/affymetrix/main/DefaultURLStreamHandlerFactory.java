/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.affymetrix.main;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class DefaultURLStreamHandlerFactory implements URLStreamHandlerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultURLStreamHandlerFactory.class);
    private URLStreamHandlerFactory delegate;

    private Map<String, URLStreamHandler> defaultHandlers;

    public DefaultURLStreamHandlerFactory() {
        defaultHandlers = new HashMap<>();
        // Cache default handlers for standard protocols
        cacheDefaultHandler("http");
        cacheDefaultHandler("https");
        cacheDefaultHandler("file");
        cacheDefaultHandler("ftp");

    }

    private void cacheDefaultHandler(String protocol) {
        try {
            URL url = new URL(protocol + "://fakeurl.com");
            URLStreamHandler handler = getURLStreamHandler(url);
            defaultHandlers.put(protocol, handler);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (delegate != null && !isDefaultScheme(protocol)) {
            return delegate.createURLStreamHandler(protocol);
        }
        return defaultHandlers.get(protocol);
    }

    private boolean isDefaultScheme(String protocol) {
        return "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)
                || "file".equalsIgnoreCase(protocol) || "ftp".equalsIgnoreCase(protocol);
    }

    public void setDelegate(URLStreamHandlerFactory delegate) {
        this.delegate = delegate;
    }

    private static URLStreamHandler getURLStreamHandler(URL url) throws Exception {
        Field handlerField = URL.class.getDeclaredField("handler");
        handlerField.setAccessible(true);
        return (URLStreamHandler) handlerField.get(url);
    }

}
