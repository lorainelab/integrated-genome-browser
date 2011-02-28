package com.affymetrix.igb.debug;

import java.util.ArrayList;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Debug {
	private static final ArrayList<BundleActivator> activators = new ArrayList<BundleActivator>();
	static {
		activators.add(new com.affymetrix.igb.window.service.def.Activator());
		activators.add(new com.affymetrix.igb.Activator());
		activators.add(new com.affymetrix.igb.external.Activator());
		activators.add(new com.affymetrix.igb.graph.Activator());
//		activators.add(new com.affymetrix.igb.plugins.Activator());
		activators.add(new com.affymetrix.igb.property.Activator());
		activators.add(new com.affymetrix.igb.selectioninfo.Activator());
		activators.add(new com.affymetrix.igb.restrictions.Activator());
		activators.add(new com.affymetrix.igb.search.Activator());
	}
	/**
	 * Start the program.
	 */
	public static void main(final String[] args) {
		BundleContext context = new DummyContext();
		try {
			for (BundleActivator activator : activators) {
				activator.start(context);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
