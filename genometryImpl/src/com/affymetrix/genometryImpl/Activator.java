package com.affymetrix.genometryImpl;

import java.util.Arrays;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;


/**
 * OSGi Activator for igb bundle
 */
public class Activator implements BundleActivator {
	protected BundleContext bundleContext;

	@Override
	public void start(BundleContext _bundleContext) throws Exception {
		this.bundleContext = _bundleContext;
		if (bundleContext.getProperty("args") != null) {
			String[] args = bundleContext.getProperty("args").split(", ");
			if (NibbleResiduesParser.FASTA_TO_BNIB.equals(args[0])) {
				String[] runArgs = Arrays.copyOfRange(args, 1, args.length);
				NibbleResiduesParser.main(runArgs);
				System.exit(0);
			}
		}
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception {}
}
