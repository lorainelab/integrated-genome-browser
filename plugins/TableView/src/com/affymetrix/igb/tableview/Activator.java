/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.tableview;

import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.GlyphProcessor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	private ServiceRegistration<GlyphProcessor> tableViewRegistration;
	private BundleContext bundleContext;
	private JMenuItem mi = new JMenuItem("Load in TableView");


	private void registerServices(final IGBService igbService) {
		JMenu file_menu = igbService.getMenu("file");
		file_menu.add(mi);
		mi.addActionListener(
	    	new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
				}
			}
	    );
    }

	@Override
	public void start(BundleContext bundleContext_) throws Exception {
		this.bundleContext = bundleContext_;
    	ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);

        if (igbServiceReference != null)
        {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
        	registerServices(igbService);
        }
        else
        {
        	ServiceTracker<IGBService,Object> serviceTracker = new ServiceTracker<IGBService,Object>(bundleContext, IGBService.class, null) {
        	    public Object addingService(ServiceReference<IGBService> igbServiceReference) {
                	IGBService igbService = bundleContext.getService(igbServiceReference);
                   	registerServices(igbService);
                    return super.addingService(igbServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
    }

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);
        if (igbServiceReference != null) {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
    		MenuUtil.removeFromMenu(igbService.getMenu("file"), mi);
        }
	}
}

