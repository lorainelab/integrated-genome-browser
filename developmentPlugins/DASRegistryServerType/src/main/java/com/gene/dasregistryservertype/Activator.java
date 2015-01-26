package com.gene.dasregistryservertype;

import com.affymetrix.common.ExtensionPointHandler;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.BundleActivator;

import com.affymetrix.igb.service.api.IGBService;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.util.ServerTypeI;
import com.affymetrix.igb.service.api.XServiceRegistrar;
import org.osgi.framework.BundleContext;

public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

    public Activator() {
        super(IGBService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ServerTypeI.class);
        DASRegistryServerType drst = new DASRegistryServerType(igbService);
        if (GenometryModel.getInstance().getSelectedSeqGroup() != null) {
            drst.setGroup(GenometryModel.getInstance().getSelectedSeqGroup());
        }

        return new ServiceRegistration[]{
            bundleContext.registerService(ServerTypeI.class, drst, null)
        };
    }
}
