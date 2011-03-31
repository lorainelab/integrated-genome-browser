package com.affymetrix.igb.graph;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.affymetrix.genometryImpl.util.FloatTransformer;
import com.affymetrix.genometryImpl.util.IdentityTransform;
import com.affymetrix.genometryImpl.util.InverseLogTransform;
import com.affymetrix.genometryImpl.util.LogTransform;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.window.service.WindowActivator;

import com.affymetrix.igb.graph.operator.*;
public class Activator extends WindowActivator implements BundleActivator {
	private static final String TRANSFORMER_SERVICE_FILTER = "(objectClass=" + FloatTransformer.class.getName() + ")";
	private static final String OPERATOR_SERVICE_FILTER = "(objectClass=" + GraphOperator.class.getName() + ")";

	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		final SimpleGraphTab simpleGraphTab = new SimpleGraphTab(igbService);
		try {
			bundleContext.addServiceListener(
				new ServiceListener() {
					@Override
					public void serviceChanged(ServiceEvent event) {
						Set<FloatTransformer> floatTransformers = new HashSet<FloatTransformer>();
						try {
							ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences(FloatTransformer.class.getName(), null);
							for (ServiceReference serviceReference : serviceReferences) {
								floatTransformers.add((FloatTransformer)bundleContext.getService(serviceReference));
							}
							simpleGraphTab.getAdvancedPanel().loadTransforms(floatTransformers);
						}
						catch (InvalidSyntaxException x) {
							Logger.getLogger(getClass().getName()).log(Level.WARNING, "error loading FloatTransforms 1", x.getMessage());
						}
					}
				}
			, TRANSFORMER_SERVICE_FILTER);
			bundleContext.addServiceListener(
				new ServiceListener() {
					@Override
					public void serviceChanged(ServiceEvent event) {
						Set<GraphOperator> graphOperators = new HashSet<GraphOperator>();
						try {
							ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences(GraphOperator.class.getName(), null);
							for (ServiceReference serviceReference : serviceReferences) {
								graphOperators.add((GraphOperator)bundleContext.getService(serviceReference));
							}
							simpleGraphTab.getAdvancedPanel().loadOperators(graphOperators);
						}
						catch (InvalidSyntaxException x) {
							Logger.getLogger(getClass().getName()).log(Level.WARNING, "error loading GraphOperators 1", x.getMessage());
						}
					}
				}
			, OPERATOR_SERVICE_FILTER);
		}
		catch (InvalidSyntaxException x) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "error loading 2", x.getMessage());
		}
		initTransformers();
		initOperators();
		return simpleGraphTab;
	}

	private void initTransformers() {
		bundleContext.registerService(FloatTransformer.class.getName(), new IdentityTransform(), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new LogTransform(2.0), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new LogTransform(10.0), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new LogTransform(Math.E), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new LogTransform(), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new InverseLogTransform(2.0), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new InverseLogTransform(10.0), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new InverseLogTransform(Math.E), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new InverseLogTransform(), new Properties());
	}

	private void initOperators() {
		bundleContext.registerService(GraphOperator.class.getName(), new DiffOperator(), new Properties());
		bundleContext.registerService(GraphOperator.class.getName(), new ProductOperator(), new Properties());
		bundleContext.registerService(GraphOperator.class.getName(), new RatioOperator(), new Properties());
		bundleContext.registerService(GraphOperator.class.getName(), new SumOperator(), new Properties());
		bundleContext.registerService(GraphOperator.class.getName(), new MinOperator(), new Properties());
		bundleContext.registerService(GraphOperator.class.getName(), new MaxOperator(), new Properties());
		bundleContext.registerService(GraphOperator.class.getName(), new MeanOperator(), new Properties());
		bundleContext.registerService(GraphOperator.class.getName(), new MedianOperator(), new Properties());
	}
}

