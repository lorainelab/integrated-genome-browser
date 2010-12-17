package com.affymetrix.igb.util.classpath;

/*
 * http://stackoverflow.com/questions/861500/url-to-load-resources-from-the-classpath-in-java
 */

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {
    /** The classloader to find resources from. */
    private final ClassLoader classLoader;

    public Handler(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        final URL resourceUrl = classLoader.getResource(u.getPath());
        return resourceUrl.openConnection();
    }
}
