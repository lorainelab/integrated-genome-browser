package com.lorainelab.igb.preprocessor;

import com.affymetrix.igb.osgi.service.SimpleServiceRegistrar;
import com.affymetrix.igb.shared.GlyphPreprocessorI;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author dcnorris
 */
public class Activator extends SimpleServiceRegistrar implements BundleActivator {

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext) throws Exception {
        PairedReadPreprocessor pairedReadGlyphFactory = new PairedReadPreprocessor();
        return new ServiceRegistration[]{
            bundleContext.registerService(GlyphPreprocessorI.class, pairedReadGlyphFactory, null)
        };
    }

}
