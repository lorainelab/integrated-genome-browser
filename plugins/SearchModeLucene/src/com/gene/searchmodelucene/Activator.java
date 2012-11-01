package com.gene.searchmodelucene;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.igb.osgi.service.ServiceRegistrar;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.IKeyWordSearch;

public class Activator extends ServiceRegistrar implements BundleActivator {

	@Override
	protected ServiceRegistration<?>[] registerService(IGBService igbService) throws Exception {
		return new ServiceRegistration[] { 
			bundleContext.registerService(IKeyWordSearch.class, new SearchModeLucene(igbService), null)
		};
	}
}
