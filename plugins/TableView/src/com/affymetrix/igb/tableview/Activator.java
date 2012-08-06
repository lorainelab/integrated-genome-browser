package com.affymetrix.igb.tableview;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.igb.osgi.service.IGBService;
import edu.umn.genomics.table.ExceptionHandler;
import edu.umn.genomics.table.LoadTable;
import edu.umn.genomics.table.TableView;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.table.TableModel;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	private BundleContext bundleContext;
	private JMenuItem mi;
	
	private void registerServices(final IGBService igbService) {
		mi = new JMenuItem("Open with TableView...", CommonUtils.getInstance().getIcon(TableView.class, "TableView16.png"));
		MenuUtil.insertIntoMenu(igbService.getMenu("file"), mi, 3);
		final TableView tv = new TableView();
		final JFrame frame = new JFrame("TableView");
		frame.getContentPane().add(tv, BorderLayout.CENTER);
		frame.setLocation(100, 100);
		frame.pack();

		mi.addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							LoadTable lt = tv.getLoadTable();
							TableModel newtm = lt.openLoadTableDialog((Frame) tv.getTopLevelAncestor());

							if (newtm != null) {
								if (frame.isVisible()) {
									frame.toFront();
								} else {
									frame.setVisible(true);
								}
								tv.setTableModel(newtm, lt.getTableSource());
							toFront();
							}
						}catch(Exception ex){
							ExceptionHandler.popupException(""+ex);
						}
					}

					private void toFront() {
						if ((frame.getExtendedState() & Frame.ICONIFIED) == Frame.ICONIFIED) {
							// de-iconify it while leaving the maximized/minimized state flags alone
							frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
						}
						if (!frame.isShowing()) {
							frame.setVisible(true);
						}
						frame.toFront();
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