package com.gene.searchmodelength;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.ServiceRegistrar;
import com.affymetrix.igb.shared.ISearchModeSym;
import com.affymetrix.searchmodesymmetryfilter.SearchModeSymmetryFilter;

public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {
	
	@Override
	protected ServiceRegistration<?>[] registerService(IGBService igbService) throws Exception {
		return new ServiceRegistration[] {
				bundleContext.registerService(ISearchModeSym.class, new SearchModeSymmetryFilter(igbService, new SymmetryFilterLength(), 9999999), null)
			};
        }
	}
}
