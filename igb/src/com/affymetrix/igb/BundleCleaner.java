package com.affymetrix.igb;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IStopRoutine;

public class BundleCleaner implements IStopRoutine {
	private final BundleContext bundleContext;
	private final IGBService igbService;

	public BundleCleaner(BundleContext bundleContext, IGBService igbService) {
		super();
		this.bundleContext = bundleContext;
		this.igbService = igbService;
	}

	@Override
	public void stop() {
        try
        {
            if (bundleContext != null)
            {
            	// do not cache included (tier 1 and tier 2) bundles, so that they are reloaded with any updates
               	for (Bundle bundle : bundleContext.getBundles()) {
               		int tier = igbService.getTier(bundle);
               		if (tier > 0 && tier < 3) {
               			bundle.uninstall();
               		}
               	}
            }
        }
        catch (Exception ex)
        {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Could not uninstall bundles", ex.getMessage());
        }
	}
}
