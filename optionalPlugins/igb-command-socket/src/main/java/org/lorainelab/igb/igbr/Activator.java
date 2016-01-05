package org.lorainelab.igbr;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 *
 * @author dcnorris
 */
public class Activator implements BundleActivator {

    SocketCommandListener s;
    Thread t;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        s = new SocketCommandListener();
        t = new Thread(s);
        t.start();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        s.setStop(true);
        t.interrupt();
        s = null;
    }

}
