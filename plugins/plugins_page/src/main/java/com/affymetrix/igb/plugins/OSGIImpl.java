package com.affymetrix.igb.plugins;

import org.osgi.framework.BundleContext;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.RepositoryAdmin;

/**
 *
 * @author hiralv
 */
public interface OSGIImpl {

    Capability getCapability();

    RepositoryAdmin getRepositoryAdmin(BundleContext bundleContext);
}
