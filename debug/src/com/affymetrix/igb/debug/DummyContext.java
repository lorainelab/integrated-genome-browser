package com.affymetrix.igb.debug;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.util.FloatTransformer;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;

public class DummyContext implements BundleContext {
	private Map<String, List<Object>> servicesMap = new HashMap<String, List<Object>>();
	private Set<BundleListener> bundleListeners = new HashSet<BundleListener>();
	private Set<ServiceListener> serviceListeners = new HashSet<ServiceListener>();
	private Map<String, Set<ServiceListener>> filteredServiceListeners = new HashMap<String, Set<ServiceListener>>();
	private static final String IGB_SERVICE_FILTER = "(objectClass=" + IGBService.class.getName() + ")";
	private static final String TAB_SERVICE_FILTER = "(objectClass=" + IGBTabPanel.class.getName() + ")";
	private static final String TRANSFORMER_SERVICE_FILTER = "(objectClass=" + FloatTransformer.class.getName() + ")";

	private final Properties properties;

	public DummyContext(Properties properties) {
		super();
		this.properties = properties;
	}

	@Override
	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	@Override
	public Bundle getBundle() {
		throw new RuntimeException("not implemented");
//		return null;
	}

	@Override
	public Bundle installBundle(String location, InputStream input)
			throws BundleException {
		throw new RuntimeException("not implemented");
//		return null;
	}

	@Override
	public Bundle installBundle(String location) throws BundleException {
		throw new RuntimeException("not implemented");
//		return null;
	}

	@Override
	public Bundle getBundle(long id) {
		return new DummyBundle(this);
	}

	@Override
	public Bundle[] getBundles() {
		return new Bundle[]{};
	}

	@Override
	public void addServiceListener(ServiceListener listener, String filter)
			throws InvalidSyntaxException {
		Set<ServiceListener> listeners = filteredServiceListeners.get(filter);
		if (listeners == null) {
			listeners = new HashSet<ServiceListener>();
			filteredServiceListeners.put(filter, listeners);
		}
//		private static final String SERVICE_FILTER = "(objectClass=" + IGBTabPanel.class.getName() + ")";
		listeners.add(listener);
	}

	@Override
	public void addServiceListener(ServiceListener listener) {
		serviceListeners.add(listener);
	}

	@Override
	public void removeServiceListener(ServiceListener listener) {
		serviceListeners.remove(listener);
	}

	@Override
	public void addBundleListener(BundleListener listener) {
		bundleListeners.add(listener);
	}

	@Override
	public void removeBundleListener(BundleListener listener) {
		bundleListeners.remove(listener);
	}

	@Override
	public void addFrameworkListener(FrameworkListener listener) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void removeFrameworkListener(FrameworkListener listener) {
		throw new RuntimeException("not implemented");
	}

	private boolean filterMatches(String filter, Object service) {
		if (IGB_SERVICE_FILTER.equals(filter)) {
			return service instanceof IGBService;
		}
		if (TAB_SERVICE_FILTER.equals(filter)) {
			return service instanceof IGBTabPanel;
		}
		if (TRANSFORMER_SERVICE_FILTER.equals(filter)) {
			return service instanceof FloatTransformer;
		}
		if (filter == null) {
			return true;
		}
		throw new RuntimeException("not implemented");
	}

	@Override
	public ServiceRegistration registerService(String[] clazzes,
			Object service, @SuppressWarnings("rawtypes") Dictionary properties) {
		for (String clazz : clazzes) {
			List<Object> services = servicesMap.get(clazz);
			if (services == null) {
				services = new ArrayList<Object>();
				servicesMap.put(clazz, services);
			}
			services.add(service);
		}
		ServiceEvent event = new ServiceEvent(ServiceEvent.REGISTERED, new DummyServiceReference(this, service));
		for (ServiceListener serviceListener : serviceListeners) {
			serviceListener.serviceChanged(event);
		}
		for (String filter : filteredServiceListeners.keySet()) {
			if (filterMatches(filter, service)) {
				for (ServiceListener serviceListener : filteredServiceListeners.get(filter)) {
					serviceListener.serviceChanged(event);
				}
			}

		}
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceRegistration registerService(String clazz, Object service,
			@SuppressWarnings("rawtypes") Dictionary properties) {
		return registerService(new String[]{clazz},
				service, properties);
	}

	@Override
	public ServiceReference[] getServiceReferences(String clazz, String filter)
			throws InvalidSyntaxException {
		return getAllServiceReferences(clazz, filter);
	}

	@Override
	public ServiceReference[] getAllServiceReferences(String clazz,
			String filter) throws InvalidSyntaxException {

		List<Object> servicesList = servicesMap.get(clazz);
		ArrayList<Object> filteredServices = new ArrayList<Object>();
		if (servicesList != null) {
			for (Object service : servicesList) {
				if (filterMatches(filter, service)) {
					filteredServices.add(service);
				}
			}
		}
		ServiceReference[] serviceReferenceArray = new ServiceReference[filteredServices.size()];
		for (int i = 0; i < filteredServices.size(); i++) {
			serviceReferenceArray[i] = new DummyServiceReference(this, filteredServices.get(i));
		}
		return serviceReferenceArray;
	}

	@Override
	public ServiceReference getServiceReference(String clazz) {
		if (servicesMap.get(clazz) == null) {
			return null;
		}
		return new DummyServiceReference(this, servicesMap.get(clazz).get(0));
	}

	@Override
	public Object getService(ServiceReference reference) {
		return ((DummyServiceReference)reference).getService();
	}

	@Override
	public boolean ungetService(ServiceReference reference) {
		return false;
	}

	@Override
	public File getDataFile(String filename) {
		throw new RuntimeException("not implemented");
//		return null;
	}

	@Override
	public Filter createFilter(String filter) throws InvalidSyntaxException {
//		throw new RuntimeException("not implemented");
		return new Filter() {

			@Override
			public boolean matchCase(@SuppressWarnings("rawtypes") Dictionary dictionary) {
				return false;
			}

			@Override
			public boolean match(@SuppressWarnings("rawtypes") Dictionary dictionary) {
				return false;
			}

			@Override
			public boolean match(ServiceReference reference) {
				return false;
			}
		};
	}

}
