package com.affymetrix.igb.debug;

import java.util.Dictionary;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class DummyServiceRegistration<S> implements ServiceRegistration<S> {

	@Override
	public ServiceReference<S> getReference() {
		throw new RuntimeException("not implemented");
//		return null;
	}

	@Override
	public void setProperties(Dictionary<String, ? > properties) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void unregister() {
		throw new RuntimeException("not implemented");
	}
}
