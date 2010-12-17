package com.affymetrix.igb.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.obr.Resource;

@SuppressWarnings("rawtypes")
public class ResourceWrapper implements Bundle {
	private static final HashMap<String, String> KEY_CONVERTER = new HashMap<String, String>();
	static {
		KEY_CONVERTER.put(Resource.DESCRIPTION, Constants.BUNDLE_DESCRIPTION);
		KEY_CONVERTER.put(Resource.SYMBOLIC_NAME, Constants.BUNDLE_SYMBOLICNAME);
		KEY_CONVERTER.put(Resource.VERSION, Constants.BUNDLE_VERSION);
		KEY_CONVERTER.put(Resource.COPYRIGHT, Constants.BUNDLE_COPYRIGHT);
		KEY_CONVERTER.put(Resource.DOCUMENTATION_URL, Constants.BUNDLE_DOCURL);
	}
	private final Resource resource;

	public ResourceWrapper(Resource resource) {
		super();
		this.resource = resource;
	}

	public Resource getResource() {
		return resource;
	}

	@Override
	public Enumeration findEntries(String arg0, String arg1, boolean arg2) {
		return null;
	}

	@Override
	public BundleContext getBundleContext() {
		return null;
	}

	@Override
	public long getBundleId() {
		return 0;
	}

	@Override
	public URL getEntry(String arg0) {
		return null;
	}

	@Override
	public Enumeration getEntryPaths(String arg0) {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Dictionary getHeaders() {
		Hashtable dictionary = new Hashtable();
		Map resourceProperties = resource.getProperties();
		for (Object resourceKey : resourceProperties.keySet()) {
			String bundleKey = KEY_CONVERTER.get(resourceKey);
			if (bundleKey != null) {
				dictionary.put(bundleKey, resourceProperties.get(resourceKey));
			}
		}
		return dictionary;
	}

	@Override
	public Dictionary getHeaders(String arg0) {
		return null;
	}

	@Override
	public long getLastModified() {
		return 0;
	}

	@Override
	public String getLocation() {
		return resource.getURL().toString();
	}

	@Override
	public ServiceReference[] getRegisteredServices() {
		return null;
	}

	@Override
	public URL getResource(String arg0) {
		return null;
	}

	@Override
	public Enumeration getResources(String arg0) throws IOException {
		return null;
	}

	@Override
	public ServiceReference[] getServicesInUse() {
		return null;
	}

	@Override
	public Map getSignerCertificates(int arg0) {
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
	public Class loadClass(String arg0) throws ClassNotFoundException {
		return null;
	}

	@Override
	public void start() throws BundleException {}

	@Override
	public void start(int arg0) throws BundleException {}

	@Override
	public void stop() throws BundleException {}

	@Override
	public void stop(int arg0) throws BundleException {}

	@Override
	public void uninstall() throws BundleException {}

	@Override
	public void update() throws BundleException {}

	@Override
	public void update(InputStream arg0) throws BundleException {}
}
