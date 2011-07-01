package com.affymetrix.igb.graph;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.affymetrix.genometryImpl.operator.graph.GraphOperator;
import com.affymetrix.genometryImpl.operator.transform.FloatTransformer;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	private static final String TRANSFORMER_SERVICE_FILTER = "(objectClass=" + FloatTransformer.class.getName() + ")";
	private static final String OPERATOR_SERVICE_FILTER = "(objectClass=" + GraphOperator.class.getName() + ")";
	private GraphSelectionManager graph_manager;

	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		final SimpleGraphTab simpleGraphTab = new SimpleGraphTab(igbService);
		graph_manager = new GraphSelectionManager(igbService, simpleGraphTab);
		igbService.addSeqMapPopupListener(graph_manager);
		try {
			ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences(FloatTransformer.class.getName(), null);
			if (serviceReferences != null) {
				for (ServiceReference serviceReference : serviceReferences) {
					simpleGraphTab.addFloatTransformer((FloatTransformer)bundleContext.getService(serviceReference));
				}
			}
			bundleContext.addServiceListener(
				new ServiceListener() {
					@Override
					public void serviceChanged(ServiceEvent event) {
						ServiceReference serviceReference = event.getServiceReference();
						if (event.getType() == ServiceEvent.UNREGISTERING || event.getType() == ServiceEvent.MODIFIED || event.getType() == ServiceEvent.MODIFIED_ENDMATCH) {
							simpleGraphTab.removeFloatTransformer((FloatTransformer)bundleContext.getService(serviceReference));
						}
						if (event.getType() == ServiceEvent.REGISTERED || event.getType() == ServiceEvent.MODIFIED) {
							simpleGraphTab.addFloatTransformer((FloatTransformer)bundleContext.getService(serviceReference));
						}
					}
				}
			, TRANSFORMER_SERVICE_FILTER);
			serviceReferences = bundleContext.getAllServiceReferences(GraphOperator.class.getName(), null);
			if (serviceReferences != null) {
				for (ServiceReference serviceReference : serviceReferences) {
					simpleGraphTab.addGraphOperator((GraphOperator)bundleContext.getService(serviceReference));
				}
			}
			bundleContext.addServiceListener(
				new ServiceListener() {
					@Override
					public void serviceChanged(ServiceEvent event) {
						ServiceReference serviceReference = event.getServiceReference();
						if (event.getType() == ServiceEvent.UNREGISTERING || event.getType() == ServiceEvent.MODIFIED || event.getType() == ServiceEvent.MODIFIED_ENDMATCH) {
							simpleGraphTab.removeGraphOperator((GraphOperator)bundleContext.getService(serviceReference));
						}
						if (event.getType() == ServiceEvent.REGISTERED || event.getType() == ServiceEvent.MODIFIED) {
							simpleGraphTab.addGraphOperator((GraphOperator)bundleContext.getService(serviceReference));
						}
					}
				}
			, OPERATOR_SERVICE_FILTER);
		}
		catch (InvalidSyntaxException x) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "error loading 2", x.getMessage());
		}
		return simpleGraphTab;
	}
}
