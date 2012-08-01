/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.tableview;

import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.GlyphProcessor;
import edu.umn.genomics.table.LoadTable;
import edu.umn.genomics.table.TableView;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.table.TableModel;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	private ServiceRegistration<GlyphProcessor> tableViewRegistration;
	private BundleContext bundleContext;
	private JMenuItem mi = new JMenuItem("Open with TableView...");


	private void registerServices(final IGBService igbService) {
		JMenu file_menu = igbService.getMenu("file");
		final TableView tv = new TableView();
		file_menu.add(mi, 3);
		mi.addActionListener(
	    	new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						LoadTable lt = tv.getLoadTable();
						TableModel newtm = lt.openLoadTableDialog((Frame) tv.getTopLevelAncestor());
						
						if (newtm != null) {
							JFrame frame = new JFrame("TableView");
							frame.getContentPane().add(tv, BorderLayout.CENTER);
							frame.setLocation(100,100);
							frame.pack();
							frame.setVisible(true);
							tv.setTableModel(newtm, lt.getTableSource());
						}
					}
				});
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
				@Override
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