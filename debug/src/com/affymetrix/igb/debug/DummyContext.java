package com.affymetrix.igb.debug;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

public class DummyContext implements BundleContext {
	private static final String FILTER_PREFIX = "(objectClass=";
	private static final String FILTER_SUFFIX = ")";
	private Map<String, List<Object>> servicesMap = new HashMap<String, List<Object>>();
	private Set<BundleListener> bundleListeners = new HashSet<BundleListener>();
	private Set<ServiceListener> serviceListeners = new HashSet<ServiceListener>();
	private Map<String, Set<ServiceListener>> filteredServiceListeners = new HashMap<String, Set<ServiceListener>>();

	private final Properties properties;

	public DummyContext(Properties properties) {
		super();
		this.properties = properties;
	}

	@Override
	public void addFrameworkListener(FrameworkListener listener) {
		throw new RuntimeException("not implemented");
	}

	private boolean filterMatches(String filter, Object service) {
		if (filter == null) {
			return true;
		}
		if (filter.startsWith(FILTER_PREFIX) && filter.endsWith(FILTER_SUFFIX)) {
			String className = filter.substring(FILTER_PREFIX.length(), filter.length() - FILTER_SUFFIX.length());
			try {
				Class<?> clazz = Class.forName(className);
				return (clazz.isAssignableFrom(service.getClass()) || Arrays.asList(service.getClass().getInterfaces()).contains(clazz));
			}
			catch (ClassNotFoundException x) {}
		}
		return false;
	}
////////////////////////////////////////////
	@Override
	public String getProperty(String key){
		return properties.getProperty(key);
	}
	@Override
	public Bundle getBundle(){
		throw new RuntimeException("not implemented");
	}
	@Override
	public Bundle installBundle(String location, InputStream input)
				throws BundleException{
		throw new RuntimeException("not implemented");
	}
	@Override
	public Bundle installBundle(String location) throws BundleException{
		throw new RuntimeException("not implemented");
	}
	@Override
	public Bundle getBundle(long id){
		return new DummyBundle(this);
	}
	@Override
	public Bundle[] getBundles(){
		return new Bundle[]{};
	}
	@Override
	public void addServiceListener(ServiceListener listener, String filter)
				throws InvalidSyntaxException{
		Set<ServiceListener> listeners = filteredServiceListeners.get(filter);
		if (listeners == null) {
			listeners = new HashSet<ServiceListener>();
			filteredServiceListeners.put(filter, listeners);
		}
//		private static final String SERVICE_FILTER = "(objectClass=" + IGBTabPanel.class.getName() + ")";
		listeners.add(listener);
	}
	@Override
	public void addServiceListener(ServiceListener listener){
		serviceListeners.add(listener);
	}
	@Override
	public void removeServiceListener(ServiceListener listener){
		serviceListeners.remove(listener);
	}
	@Override
	public void addBundleListener(BundleListener listener){
		bundleListeners.add(listener);
	}
	@Override
	public void removeBundleListener(BundleListener listener){
		bundleListeners.remove(listener);
	}
	@Override
	public void removeFrameworkListener(FrameworkListener listener){
		throw new RuntimeException("not implemented");
	}
	@Override
	public ServiceRegistration< ? > registerService(String[] clazzes, Object service,
				Dictionary<String, ? > properties){
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
	public ServiceRegistration< ? > registerService(String clazz, Object service,
				Dictionary<String, ? > properties){
		return registerService(new String[]{clazz},
				service, properties);
	}
	@SuppressWarnings("unchecked")
	@Override
	public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service,
				Dictionary<String, ? > properties){
		return (ServiceRegistration<S>) registerService(clazz.getName(), service, properties);
	}
	@Override
	public ServiceReference< ? >[] getServiceReferences(String clazz, String filter)
				throws InvalidSyntaxException{
		return getAllServiceReferences(clazz, filter);
	}
	@Override
	public ServiceReference< ? >[] getAllServiceReferences(String clazz, String filter)
				throws InvalidSyntaxException{
		List<Object> servicesList = servicesMap.get(clazz);
		ArrayList<Object> filteredServices = new ArrayList<Object>();
		if (servicesList != null) {
			for (Object service : servicesList) {
				if (filterMatches(filter, service)) {
					filteredServices.add(service);
				}
			}
		}
		ServiceReference< ? >[] serviceReferenceArray = new ServiceReference[filteredServices.size()];
		for (int i = 0; i < filteredServices.size(); i++) {
			serviceReferenceArray[i] = new DummyServiceReference(this, filteredServices.get(i));
		}
		return serviceReferenceArray;
	}
	@Override
	public ServiceReference< ? > getServiceReference(String clazz){
		if (servicesMap.get(clazz) == null) {
			return null;
		}
		return new DummyServiceReference(this, servicesMap.get(clazz).get(0));
	}
	@SuppressWarnings("unchecked")
	@Override
	public <S> ServiceReference<S> getServiceReference(Class<S> clazz){
		return (ServiceReference<S>) getServiceReference(clazz.getName());
	}
	@SuppressWarnings("unchecked")
	@Override
	public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz,
				String filter) throws InvalidSyntaxException{
		ServiceReference<?>[] serviceReferenceArray = getServiceReferences(clazz.getName(), filter);
		Collection<ServiceReference<S>> serviceReferences = new ArrayList<ServiceReference<S>>();
		for (ServiceReference<?> serviceReference : serviceReferenceArray) {
			serviceReferences.add((ServiceReference<S>)serviceReference);
		}
		return serviceReferences;
	}
	@SuppressWarnings("unchecked")
	@Override
	public <S> S getService(ServiceReference<S> reference){
		return (S) ((DummyServiceReference<S>)reference).getService();
	}
	@Override
	public boolean ungetService(ServiceReference<?> reference){
		return false;
	}
	@Override
	public File getDataFile(String filename){
		throw new RuntimeException("not implemented");
	}
	@Override
	public Filter createFilter(String filter) throws InvalidSyntaxException{
//		throw new RuntimeException("not implemented");
		return new Filter() {
			
			@Override
			public boolean matchCase(Dictionary<String, ?> dictionary) {
				return false;
			}
			
			@Override
			public boolean match(Dictionary<String, ?> dictionary) {
				return false;
			}
			
			@Override
			public boolean match(ServiceReference< ? > reference) {
				return false;
			}

			@Override
			public boolean matches(Map<String, ?> map) {
				// TODO Auto-generated method stub
				return false;
			}
		};
	}
	@Override
	public Bundle getBundle(String location) {
		return new DummyBundle(this);
	}
}
