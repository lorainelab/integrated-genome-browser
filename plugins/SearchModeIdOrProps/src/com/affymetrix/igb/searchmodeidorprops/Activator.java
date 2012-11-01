package com.affymetrix.igb.searchmodeidorprops;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.ServiceRegistrar;
import com.affymetrix.igb.shared.IKeyWordSearch;
import com.affymetrix.igb.shared.ISearchHints;
import com.affymetrix.igb.shared.ISearchModeSym;

public class Activator extends ServiceRegistrar implements BundleActivator {
	
	@Override
	protected ServiceRegistration<?>[] registerService(IGBService igbService) throws Exception {
		SearchModeID smID = new SearchModeID(igbService);
		return new ServiceRegistration[] {
				bundleContext.registerService(ISearchModeSym.class, smID, null),
				bundleContext.registerService(IKeyWordSearch.class, new SearchModeProps(igbService), null),
				bundleContext.registerService(ISearchHints.class, new PropSearchHints(), null)
			};
	}
}

