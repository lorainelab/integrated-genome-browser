package com.affymetrix.igb.debug;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class DummyServiceReference<S> implements ServiceReference<S> {
	private final Object service;
	private final BundleContext bundleContext;

	public DummyServiceReference(BundleContext bundleContext, Object service) {
		super();
		this.service = service;
		this.bundleContext = bundleContext;
	}

	@Override
	public Object getProperty(String key) {
		throw new RuntimeException("not implemented");
//		return null;
	}

	@Override
	public String[] getPropertyKeys() {
		throw new RuntimeException("not implemented");
//		return null;
	}

	@Override
	public Bundle getBundle() {
//		throw new RuntimeException("not implemented");
		return new DummyBundle(bundleContext);
	}

	@Override
	public Bundle[] getUsingBundles() {
		throw new RuntimeException("not implemented");
//		return null;
	}

	@Override
	public boolean isAssignableTo(Bundle bundle, String className) {
		throw new RuntimeException("not implemented");
//		return false;
	}

	@Override
	public int compareTo(Object reference) {
		throw new RuntimeException("not implemented");
//		return 0;
	}

	public Object getService() {
		return service;
	}
}
