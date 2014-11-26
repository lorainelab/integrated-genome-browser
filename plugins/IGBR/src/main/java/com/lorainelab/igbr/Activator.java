/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
        s.start();

    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        s = null;
    }

}
