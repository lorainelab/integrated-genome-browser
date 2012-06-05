package com.affymetrix.igb.trackAdjuster;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.window.service.WindowActivator;
import org.osgi.framework.BundleActivator;

public class Activator extends WindowActivator implements BundleActivator {

	@Override
	protected IGBTabPanel getPage(final IGBService igbService) {
		TrackAdjusterTabGUI.init(igbService);
		final TrackAdjusterTabGUI trackAdjusterTabGUI = TrackAdjusterTabGUI.getSingleton();
		return trackAdjusterTabGUI;
	}
}
