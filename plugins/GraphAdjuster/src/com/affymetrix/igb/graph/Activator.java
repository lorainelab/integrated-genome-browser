package com.affymetrix.igb.graph;

import javax.swing.JComponent;

import org.osgi.framework.BundleActivator;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	@Override
	protected JComponent getPage(IGBService igbService) {
		return new SimpleGraphTab(igbService);
	}

	@Override
	protected String getName() {
        return SimpleGraphTab.BUNDLE.getString("graphAdjusterTab");
	}

	@Override
	protected String getTitle() {
        return SimpleGraphTab.BUNDLE.getString("graphAdjusterTab");
	}
}
