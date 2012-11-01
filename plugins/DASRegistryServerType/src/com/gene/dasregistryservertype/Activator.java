package com.gene.dasregistryservertype;

import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.BundleActivator;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.ServiceRegistrar;
import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.util.ServerTypeI;

public class Activator extends ServiceRegistrar implements BundleActivator {
	
	@Override
	protected ServiceRegistration<?>[] registerService(IGBService igbService) throws Exception {
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ServerTypeI.class);
		DASRegistryServerType drst = new DASRegistryServerType(igbService);
		if (GenometryModel.getGenometryModel().getSelectedSeqGroup() != null) {
			drst.setGroup(GenometryModel.getGenometryModel().getSelectedSeqGroup());
		}
		
		return new ServiceRegistration[] {
			bundleContext.registerService(ServerTypeI.class, drst, null)
		};
	}
}
