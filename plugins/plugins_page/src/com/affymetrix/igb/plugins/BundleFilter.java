package com.affymetrix.igb.plugins;

import org.osgi.framework.Bundle;

/**
 * interface for filtering bundles.
 */
public interface BundleFilter {
	public boolean filterBundle(Bundle bundle);
}
