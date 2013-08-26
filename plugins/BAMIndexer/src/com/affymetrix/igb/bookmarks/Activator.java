package com.affymetrix.igb.bookmarks;

import java.util.logging.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genoviz.swing.AMenuItem;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.XServiceRegistrar;

public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {
	private static final Logger ourLogger = Logger.getLogger(Activator.class.getPackage().getName());

	public Activator(){
		super(IGBService.class);
	}
	
	@Override
	protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception{
		
		// assuming last file menu item is Exit, leave it there
		JRPMenu file_menu = igbService.getMenu("file");
		final int index = file_menu.getItemCount() - 1;
		file_menu.insertSeparator(index);
	
		return new ServiceRegistration[] {
			bundleContext.registerService(AMenuItem.class, new AMenuItem(new JRPMenuItem("BAMIndexer", BAMIndexer.getAction()), "file", index), null),
		};
	}
}
