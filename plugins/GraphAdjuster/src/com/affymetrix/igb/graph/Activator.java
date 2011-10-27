package com.affymetrix.igb.graph;

import org.osgi.framework.BundleActivator;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.operator.graph.GraphOperator;
import com.affymetrix.genometryImpl.operator.transform.FloatTransformer;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		SimpleGraphTabGUI.init(igbService);
		final SimpleGraphTabGUI simpleGraphTabGUI = SimpleGraphTabGUI.getSingleton();
		ExtensionPointHandler.addExtensionPoint(bundleContext,
			new ExtensionPointHandler<FloatTransformer>() {
				@Override
				public void addService(FloatTransformer floatTransformer) {
					simpleGraphTabGUI.sgt.addFloatTransformer(floatTransformer);
				}
				@Override
				public void removeService(FloatTransformer floatTransformer) {
					simpleGraphTabGUI.sgt.removeFloatTransformer(floatTransformer);
				}
			}
		);
		ExtensionPointHandler.addExtensionPoint(bundleContext,
			new ExtensionPointHandler<GraphOperator>() {
				@Override
				public void addService(GraphOperator graphOperator) {
					simpleGraphTabGUI.sgt.addGraphOperator(graphOperator);
				}
				@Override
				public void removeService(GraphOperator graphOperator) {
					simpleGraphTabGUI.sgt.removeGraphOperator(graphOperator);
				}
			}
		);
		return simpleGraphTabGUI;
	}
}
