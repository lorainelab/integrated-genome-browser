package com.affymetrix.igb.debug;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
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

import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.operator.annotation.AnnotationOperator;
import com.affymetrix.genometryImpl.operator.graph.GraphOperator;
import com.affymetrix.genometryImpl.operator.transform.FloatTransformer;
import com.affymetrix.genoviz.swing.recordplayback.JRPWidgetDecorator;
import com.affymetrix.genoviz.swing.recordplayback.RecordPlaybackHolder;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.searchmodeidorprops.RemoteSearchI;
import com.affymetrix.igb.shared.GlyphProcessor;
import com.affymetrix.igb.shared.ExtendedMapViewGlyphFactoryI;
import com.affymetrix.igb.shared.ISearchMode;
import com.affymetrix.igb.shared.TrackClickListener;
import com.affymetrix.igb.window.service.IWindowService;

public class DummyContext implements BundleContext {
	private Map<String, List<Object>> servicesMap = new HashMap<String, List<Object>>();
	private Set<BundleListener> bundleListeners = new HashSet<BundleListener>();
	private Set<ServiceListener> serviceListeners = new HashSet<ServiceListener>();
	private Map<String, Set<ServiceListener>> filteredServiceListeners = new HashMap<String, Set<ServiceListener>>();
	private static final String IGB_SERVICE_FILTER = "(objectClass=" + IGBService.class.getName() + ")";
	private static final String TAB_SERVICE_FILTER = "(objectClass=" + IGBTabPanel.class.getName() + ")";
	private static final String TRANSFORMER_SERVICE_FILTER = "(objectClass=" + FloatTransformer.class.getName() + ")";
	private static final String GRAPH_OPERATOR_SERVICE_FILTER = "(objectClass=" + GraphOperator.class.getName() + ")";
	private static final String ANNOTATION_OPERATOR_SERVICE_FILTER = "(objectClass=" + AnnotationOperator.class.getName() + ")";
	private static final String FILETYPEHANDLER_FACTORY_SERVICE_FILTER = "(objectClass=" + FileTypeHandler.class.getName() + ")";
	private static final String TRACK_CLICK_LISTENER_FILTER = "(objectClass=" + TrackClickListener.class.getName() + ")";
	private static final String GLYPH_PROCESSOR_FILTER = "(objectClass=" + GlyphProcessor.class.getName() + ")";
	private static final String SEARCH_MODE_FILTER = "(objectClass=" + ISearchMode.class.getName() + ")";
	private static final String MAP_VIEW_GLYPH_FACTORY_FILTER = "(objectClass=" + ExtendedMapViewGlyphFactoryI.class.getName() + ")";
	private static final String WIDGET_DECORATOR = "(objectClass=" + JRPWidgetDecorator.class.getName() + ")";
	private static final String WINDOW_SERVICE_FILTER = "(objectClass=" + IWindowService.class.getName() + ")";
	private static final String RPH_FILTER = "(objectClass=" + RecordPlaybackHolder.class.getName() + ")";
	private static final String REMOTE_SEARCH = "(objectClass=" + RemoteSearchI.class.getName() + ")";

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
/*
		if (filter.startsWith("(objectClass=") && filter.endsWith(")")) {
			String className = filter.substring("(objectClass=".length(), filter.length() - 1);
			Class<?> clazz = Class.forName(className);
			return (service instanceof clazz);
		}
*/
		if (IGB_SERVICE_FILTER.equals(filter)) {
			return service instanceof IGBService;
		}
		if (TAB_SERVICE_FILTER.equals(filter)) {
			return service instanceof IGBTabPanel;
		}
		if (TRANSFORMER_SERVICE_FILTER.equals(filter)) {
			return service instanceof FloatTransformer;
		}
		if (GRAPH_OPERATOR_SERVICE_FILTER.equals(filter)) {
			return service instanceof GraphOperator;
		}
		if (ANNOTATION_OPERATOR_SERVICE_FILTER.equals(filter)) {
			return service instanceof AnnotationOperator;
		}
		if (FILETYPEHANDLER_FACTORY_SERVICE_FILTER.equals(filter)) {
			return service instanceof FileTypeHandler;
		}
		if (TRACK_CLICK_LISTENER_FILTER.equals(filter)) {
			return service instanceof TrackClickListener;
		}
		if (GLYPH_PROCESSOR_FILTER.equals(filter)) {
			return service instanceof GlyphProcessor;
		}
		if (SEARCH_MODE_FILTER.equals(filter)) {
			return service instanceof ISearchMode;
		}
		if (MAP_VIEW_GLYPH_FACTORY_FILTER.equals(filter)) {
			return service instanceof ExtendedMapViewGlyphFactoryI;
		}
		if (WIDGET_DECORATOR.equals(filter)) {
			return service instanceof JRPWidgetDecorator;
		}
		if (WINDOW_SERVICE_FILTER.equals(filter)) {
			return service instanceof IWindowService;
		}
		if (RPH_FILTER.equals(filter)) {
			return service instanceof RecordPlaybackHolder;
		}
		if (REMOTE_SEARCH.equals(filter)) {
			return service instanceof RemoteSearchI;
		}
		if (filter == null) {
			return true;
		}
		throw new RuntimeException("not implemented");
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
