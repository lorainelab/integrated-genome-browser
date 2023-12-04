/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.affymetrix.main;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 *
 * @author dcnorris
 */
public class DefaultURLStreamHandlerFactory implements URLStreamHandlerFactory {

    private URLStreamHandlerFactory delegate;

    public DefaultURLStreamHandlerFactory(URLStreamHandlerFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (!isDefaultScheme(protocol)) {
            return delegate.createURLStreamHandler(protocol);
        }
        return null;
    }

    private boolean isDefaultScheme(String protocol) {
        return "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol) || "file".equalsIgnoreCase(protocol) || "ftp".equalsIgnoreCase(protocol);
    }

}
