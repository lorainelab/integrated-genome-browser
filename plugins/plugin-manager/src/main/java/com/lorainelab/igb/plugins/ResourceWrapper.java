package com.lorainelab.igb.plugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * this class implements the Bundle interface for Resource instances loaded by
 * OBR - uninstalled bundles. This is a wrapper for the Resource - the Decorator
 * pattern.
 */
@SuppressWarnings("rawtypes")
public class ResourceWrapper implements Bundle {

    private static final HashMap<String, String> KEY_CONVERTER = new HashMap<>();

    static {
        KEY_CONVERTER.put(Resource.DESCRIPTION, Constants.BUNDLE_DESCRIPTION);
        KEY_CONVERTER.put(Resource.PRESENTATION_NAME, Constants.BUNDLE_NAME);
        KEY_CONVERTER.put(Resource.SYMBOLIC_NAME, Constants.BUNDLE_SYMBOLICNAME);
        KEY_CONVERTER.put(Resource.VERSION, Constants.BUNDLE_VERSION);
        KEY_CONVERTER.put(Resource.COPYRIGHT, Constants.BUNDLE_COPYRIGHT);
        KEY_CONVERTER.put(Resource.DOCUMENTATION_URI, Constants.BUNDLE_DOCURL);
    }
    private final Resource resource;

    public ResourceWrapper(Resource resource) {
        super();
        this.resource = resource;
    }

    /**
     * @return the wrapped Resource
     */
    public Resource getResource() {
        return resource;
    }

    @Override
    public Enumeration<URL> findEntries(String arg0, String arg1, boolean arg2) {
        return null;
    }

    @Override
    public BundleContext getBundleContext() {
        return null;
    }

    @Override
    public long getBundleId() {
        return Long.MAX_VALUE;
    }

    @Override
    public URL getEntry(String arg0) {
        return null;
    }

    @Override
    public Enumeration<String> getEntryPaths(String arg0) {
        return null;
    }

    @Override
    public Dictionary<String, String> getHeaders() {
        Hashtable<String, String> dictionary = new Hashtable<>();
        Map resourceProperties = resource.getProperties();
        for (Object resourceKey : resourceProperties.keySet()) {
            String bundleKey = KEY_CONVERTER.get(resourceKey);
            if (bundleKey != null) {
                dictionary.put(bundleKey, resourceProperties.get(resourceKey).toString());
            }
        }
        return dictionary;
    }

    @Override
    public Dictionary<String, String> getHeaders(String arg0) {
        return null;
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public String getLocation() {
        return resource.getURI().toString();
    }

    @Override
    public ServiceReference< ?>[] getRegisteredServices() {
        return null;
    }

    @Override
    public URL getResource(String arg0) {
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String arg0) throws IOException {
        return null;
    }

    @Override
    public ServiceReference< ?>[] getServicesInUse() {
        return null;
    }

    @Override
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int arg0) {
        return null;
    }

    @Override
    public int getState() {
        return Bundle.UNINSTALLED;
    }

    @Override
    public String getSymbolicName() {
        return resource.getSymbolicName();
    }

    @Override
    public Version getVersion() {
        return resource.getVersion();
    }

    @Override
    public boolean hasPermission(Object arg0) {
        return false;
    }

    @Override
    public Class< ?> loadClass(String arg0) throws ClassNotFoundException {
        return null;
    }

    @Override
    public void start() throws BundleException {
    }

    @Override
    public void start(int arg0) throws BundleException {
    }

    @Override
    public void stop() throws BundleException {
    }

    @Override
    public void stop(int arg0) throws BundleException {
    }

    @Override
    public void uninstall() throws BundleException {
    }

    @Override
    public void update() throws BundleException {
    }

    @Override
    public void update(InputStream arg0) throws BundleException {
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((resource == null) ? 0 : resource.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ResourceWrapper other = (ResourceWrapper) obj;
        if (resource == null) {
            if (other.resource != null) {
                return false;
            }
        } else if (!resource.equals(other.resource)) {
            return false;
        }
        return true;
    }

    public int compareTo(Bundle o) {
        return 0;
    }

    public <A> A adapt(Class<A> type) {
        return null;
    }

    public File getDataFile(String filename) {
        return null;
    }
}
