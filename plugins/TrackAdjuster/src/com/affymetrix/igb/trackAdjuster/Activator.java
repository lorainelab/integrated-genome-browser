package com.affymetrix.igb.trackAdjuster;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.shared.OperationsImpl;
import com.affymetrix.igb.window.service.WindowActivator;
import org.osgi.framework.BundleActivator;

public class Activator extends WindowActivator implements BundleActivator {

	@Override
	protected IGBTabPanel getPage(final IGBService igbService) {
		AnnotationTrackPanel tabPanel = new AnnotationTrackPanel(igbService);
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
