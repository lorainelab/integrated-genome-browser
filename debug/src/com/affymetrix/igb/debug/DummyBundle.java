package com.affymetrix.igb.debug;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

@SuppressWarnings("rawtypes")
public class DummyBundle implements Bundle {
	private final BundleContext bundleContext;
	public DummyBundle(BundleContext bundleContext) {
		super();
		this.bundleContext = bundleContext;
	}

	private static final Dictionary DummyDictionary = new Dictionary() {

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
	public Enumeration keys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration elements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object get(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object put(Object key, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object remove(Object key) {
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
	public Dictionary getHeaders() {
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
	public ServiceReference[] getRegisteredServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceReference[] getServicesInUse() {
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
	public Dictionary getHeaders(String locale) {
		return DummyDictionary;
	}

	@Override
	public String getSymbolicName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class loadClass(String name) throws ClassNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration getResources(String name) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration getEntryPaths(String path) {
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
	public Enumeration findEntries(String path, String filePattern,
			boolean recurse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BundleContext getBundleContext() {
		return bundleContext;
	}

	@Override
	public Map getSignerCertificates(int signersType) {
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
