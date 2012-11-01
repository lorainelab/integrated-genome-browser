package com.affymetrix.searchmodesymmetryfilter;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.filter.SymmetryFilterProps;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.ServiceRegistrar;
import com.affymetrix.igb.shared.ISearchModeSym;

public class Activator extends ServiceRegistrar implements BundleActivator {
	
	@Override
	protected ServiceRegistration<?>[] registerService(IGBService igbService) throws Exception {
		return new ServiceRegistration[] {
			bundleContext.registerService(ISearchModeSym.class, new SearchModeSymmetryFilter(igbService, new SymmetryFilterProps(), 2000), null)
		};
	}
}
