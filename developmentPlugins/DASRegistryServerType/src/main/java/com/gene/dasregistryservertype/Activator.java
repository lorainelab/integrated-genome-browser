package com.gene.dasregistryservertype;

import com.affymetrix.common.ExtensionPointHandler;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.BundleActivator;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.igb.osgi.service.XServiceRegistrar;
import org.osgi.framework.BundleContext;

public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

    public Activator() {
        super(IGBService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ServerTypeI.class);
        DASRegistryServerType drst = new DASRegistryServerType(igbService);
        if (GenometryModel.getGenometryModel().getSelectedSeqGroup() != null) {
            drst.setGroup(GenometryModel.getGenometryModel().getSelectedSeqGroup());
        }

        return new ServiceRegistration[]{
            bundleContext.registerService(ServerTypeI.class, drst, null)
        };
    }
}
