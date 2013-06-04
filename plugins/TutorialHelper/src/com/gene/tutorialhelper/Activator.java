package com.gene.tutorialhelper;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genoviz.swing.recordplayback.JRPWidgetDecorator;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.ServiceRegistrar;
import org.osgi.framework.BundleContext;

public class Activator extends ServiceRegistrar implements BundleActivator {

	@Override
	protected ServiceRegistration<?>[] registerService(BundleContext bundleContext, IGBService igbService) throws Exception {
		return new ServiceRegistration[]{
			bundleContext.registerService(JRPWidgetDecorator.class, new WidgetIdTooltip(), null)
		};
	}
}
