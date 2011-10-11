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
			new ExtensionPointHandler(FloatTransformer.class) {
				@Override
				public void addService(Object o) {
					simpleGraphTabGUI.sgt.addFloatTransformer((FloatTransformer)o);
				}
				@Override
				public void removeService(Object o) {
					simpleGraphTabGUI.sgt.removeFloatTransformer((FloatTransformer)o);
				}
			}
		);
		ExtensionPointHandler.addExtensionPoint(bundleContext,
			new ExtensionPointHandler(GraphOperator.class) {
				@Override
				public void addService(Object o) {
					simpleGraphTabGUI.sgt.addGraphOperator((GraphOperator)o);
				}
				@Override
				public void removeService(Object o) {
					simpleGraphTabGUI.sgt.removeGraphOperator((GraphOperator)o);
				}
			}
		);
		return simpleGraphTabGUI;
	}
}
