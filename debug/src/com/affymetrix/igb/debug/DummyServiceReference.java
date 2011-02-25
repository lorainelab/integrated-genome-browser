package com.affymetrix.igb.debug;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class DummyServiceReference implements ServiceReference {
	private final Object service;

	public DummyServiceReference(Object service) {
		super();
		this.service = service;
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
		throw new RuntimeException("not implemented");
//		return null;
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
