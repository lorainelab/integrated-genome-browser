package com.affymetrix.igb.plugins;

import org.osgi.framework.Bundle;

/**
 * interface for filtering bundles.
 */
public interface BundleFilter {

    /**
     * check a bundle to see if it passes the filter
     *
     * @param bundle the bundle to check
     * @return if the bundle passes the filter
     */
    public boolean filterBundle(Bundle bundle);
}
