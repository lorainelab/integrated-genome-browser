package com.affymetrix.igb.osgi.service;

import org.osgi.framework.BundleActivator;

/**
 * This is the main Activator for all bundles.
 * Those bundles have an Activator that extends this class
 * and they only need to implement the registerService() method
 */
public abstract class ServiceRegistrar extends XServiceRegistrar<IGBService> implements BundleActivator {
	
	public ServiceRegistrar(){
		super(IGBService.class);
	}
}

