package com.affymetrix.igb.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Debug {
	private static final ArrayList<BundleActivator> activators = new ArrayList<BundleActivator>();
	static {
		activators.add(new com.affymetrix.igb.window.service.def.Activator());
		activators.add(new com.affymetrix.igb.Activator());
		activators.add(new com.affymetrix.igb.bookmarks.Activator());
		activators.add(new com.affymetrix.igb.external.Activator());
		activators.add(new com.affymetrix.igb.graph.Activator());
		activators.add(new com.affymetrix.igb.plugins.Activator());
		activators.add(new com.affymetrix.igb.property.Activator());
		activators.add(new com.affymetrix.igb.restrictions.Activator());
		activators.add(new com.affymetrix.igb.search.Activator());
		activators.add(new com.affymetrix.genometryImpl.Activator());
		activators.add(new com.affymetrix.genoviz.Activator());
		activators.add(new com.affymetrix.igb.searchmodeidorprops.Activator());
		activators.add(new com.affymetrix.igb.searchmoderesidue.Activator());
		activators.add(new com.affymetrix.igb.tutorial.Activator());
		activators.add(new com.affymetrix.searchmodesymmetryfilter.Activator());
	}

	/**
	 * Start the program.
	 */
	public static void main(final String[] args) {
		Properties properties = new Properties();
		String argArray = Arrays.toString(args);
		properties.put("args", argArray.substring(1, argArray.length() - 1));
		BundleContext context = new DummyContext(properties);
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
