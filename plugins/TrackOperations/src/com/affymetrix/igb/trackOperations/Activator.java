package com.affymetrix.igb.trackOperations;

import org.osgi.framework.BundleActivator;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.shared.GraphPanelImpl;
import com.affymetrix.igb.shared.StylePanelImpl;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		
		GraphTrackPanel tabPanel = new GraphTrackPanel(igbService);
		final OperationsImpl trackOperation = new OperationsImpl(igbService);
		tabPanel.addPanel(trackOperation);
		
		ExtensionPointHandler<Operator> operatorExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, Operator.class);
		operatorExtensionPoint.addListener(new ExtensionPointListener<Operator>() {

			@Override
			public void addService(Operator operator) {
				trackOperation.addOperator(operator);
			}

			@Override
			public void removeService(Operator operator) {
				trackOperation.removeOperator(operator);
			}
		});
		
		
		return tabPanel;
	}
}
