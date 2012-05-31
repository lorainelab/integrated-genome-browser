package com.affymetrix.igb.trackOperations;

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
		TrackOperationsTabGUI.init(igbService);
		final TrackOperationsTabGUI simpleTrackTabGUI = TrackOperationsTabGUI.getSingleton();
		ExtensionPointHandler<Operator> operatorExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, Operator.class);
		operatorExtensionPoint.addListener(new ExtensionPointListener<Operator>() {

			@Override
			public void addService(Operator operator) {
				simpleTrackTabGUI.trackOpTab.addOperator(operator);
			}

			@Override
			public void removeService(Operator operator) {
				simpleTrackTabGUI.trackOpTab.removeOperator(operator);
			}
		});
		return simpleTrackTabGUI;
	}
}
