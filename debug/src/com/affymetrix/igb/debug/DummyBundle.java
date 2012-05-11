package com.affymetrix.igb.debug;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class DummyBundle implements Bundle {
	private final BundleContext bundleContext;
	public DummyBundle(BundleContext bundleContext) {
		super();
		this.bundleContext = bundleContext;
	}

	private static final Dictionary<String, String> DummyDictionary = new Dictionary<String, String>() {

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Enumeration<String> keys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<String> elements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String get(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String put(String key, String value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String remove(Object key) {
		// TODO Auto-generated method stub
		return null;
	}
	};

	@Override
	public int getState() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void start(int options) throws BundleException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() throws BundleException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop(int options) throws BundleException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() throws BundleException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(InputStream input) throws BundleException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update() throws BundleException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void uninstall() throws BundleException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Dictionary<String, String> getHeaders() {
		return DummyDictionary;
	}

	@Override
	public long getBundleId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceReference<?>[] getRegisteredServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceReference<?>[] getServicesInUse() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasPermission(Object permission) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public URL getResource(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dictionary<String, String> getHeaders(String locale) {
		return DummyDictionary;
	}

	@Override
	public String getSymbolicName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<String> getEntryPaths(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getEntry(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getLastModified() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Enumeration<URL> findEntries(String path, String filePattern,
			boolean recurse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BundleContext getBundleContext() {
		return bundleContext;
	}

	@Override
	public Map<X509Certificate,List<X509Certificate>> getSignerCertificates(int signersType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Version getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int compareTo(Bundle o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <A> A adapt(Class<A> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getDataFile(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
