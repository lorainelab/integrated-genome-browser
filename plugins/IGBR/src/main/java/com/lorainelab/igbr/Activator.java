package com.lorainelab.igbr;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 *
 * @author dcnorris
 */
public class Activator implements BundleActivator {

    SocketCommandListener s;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        s = new SocketCommandListener();
        new Thread(s).start();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        s = null;
    }

}
