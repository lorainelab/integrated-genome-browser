package com.affymetrix.igb.plugins;

import org.osgi.framework.Bundle;

public interface BundleFilter {
	public boolean filterBundle(Bundle bundle);
}
