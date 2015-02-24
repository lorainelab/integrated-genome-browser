package com.gene.dasregistryservertype;

import com.affymetrix.common.ExtensionPointHandler;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.BundleActivator;

import com.lorainelab.igb.services.IgbService;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.util.ServerTypeI;
import com.lorainelab.igb.services.XServiceRegistrar;
import org.osgi.framework.BundleContext;

public class Activator extends XServiceRegistrar<IgbService> implements BundleActivator {

    public Activator() {
        super(IgbService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IgbService igbService) throws Exception {
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
