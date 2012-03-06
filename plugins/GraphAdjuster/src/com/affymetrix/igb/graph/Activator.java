package com.affymetrix.igb.graph;

import org.osgi.framework.BundleActivator;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		SimpleGraphTabGUI.init(igbService);
		final SimpleGraphTabGUI simpleGraphTabGUI = SimpleGraphTabGUI.getSingleton();
/*
		ExtensionPointHandler<FloatTransformer> floatTransformerExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, FloatTransformer.class);
		floatTransformerExtensionPoint.addListener(new ExtensionPointListener<FloatTransformer>() {
			
			@Override
			public void removeService(FloatTransformer floatTransformer) {
				simpleGraphTabGUI.sgt.removeFloatTransformer(floatTransformer);
			}
			
			@Override
			public void addService(FloatTransformer floatTransformer) {
				simpleGraphTabGUI.sgt.addFloatTransformer(floatTransformer);
			}
		});
		ExtensionPointHandler<GraphOperator> graphOperatorExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, GraphOperator.class);
		graphOperatorExtensionPoint.addListener(new ExtensionPointListener<GraphOperator>() {

			@Override
			public void addService(GraphOperator graphOperator) {
				simpleGraphTabGUI.sgt.addGraphOperator(graphOperator);
			}

			@Override
			public void removeService(GraphOperator graphOperator) {
				simpleGraphTabGUI.sgt.removeGraphOperator(graphOperator);
			}
		});
		*/
		ExtensionPointHandler<Operator> operatorExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, Operator.class);
		operatorExtensionPoint.addListener(new ExtensionPointListener<Operator>() {

			@Override
			public void addService(Operator operator) {
				simpleGraphTabGUI.sgt.addOperator(operator);
			}

			@Override
			public void removeService(Operator operator) {
				simpleGraphTabGUI.sgt.removeOperator(operator);
			}
		});
		return simpleGraphTabGUI;
	}
}
