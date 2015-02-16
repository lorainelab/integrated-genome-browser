package com.affymetrix.igb.tableview;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.table.TableModel;

import edu.umn.genomics.table.ExceptionHandler;
import edu.umn.genomics.table.LoadTable;
import edu.umn.genomics.table.TableView;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genoviz.swing.AMenuItem;

import com.lorainelab.igb.service.api.SimpleServiceRegistrar;
import org.osgi.framework.BundleContext;

public class Activator extends SimpleServiceRegistrar implements BundleActivator {

    private static final int MENU_POS = 3;

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext) throws Exception {
        final TableView tv = new TableView();
        final JFrame frame = new JFrame("TableView");
        frame.getContentPane().add(tv, BorderLayout.CENTER);
        frame.setLocation(100, 100);
        frame.pack();

        JMenuItem menuItem = new JMenuItem("Open with TableView...", CommonUtils.getInstance().getIcon(TableView.class, "TableView16.png"));
        menuItem.addActionListener(
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
                        } catch (Exception ex) {
                            ExceptionHandler.popupException("" + ex);
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

        return new ServiceRegistration[]{
            bundleContext.registerService(AMenuItem.class, new AMenuItem(menuItem, "file", MENU_POS), null)
        };
    }
}
