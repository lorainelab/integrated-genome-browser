package com.gene.thousandgenomesservertype;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.igb.service.api.IGBService;
import com.affymetrix.igb.service.api.XServiceRegistrar;
import org.osgi.framework.BundleContext;

public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

//	private static final String _1000_GENOMES_US = "ftp://ftp-trace.ncbi.nih.gov/1000genomes/ftp/";
    static final String _1000_GENOMES_US = "ftp://ftp-trace.ncbi.nih.gov/1000genomes/ftp/phase1/";
//	private static final String _1000_GENOMES_EUROPE = "ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/";

    public Activator() {
        super(IGBService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ServerTypeI.class);
        igbService.addServer(ThousandGenomesServerType.getInstance(), "1000 Genomes", _1000_GENOMES_US, Integer.MAX_VALUE);
        return new ServiceRegistration[]{
            bundleContext.registerService(ServerTypeI.class, ThousandGenomesServerType.getInstance(), null)
        };
    }

}
