package org.lorainelab.igb.plugin.manager.repos.view;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.CommonUtils;
import com.affymetrix.genoviz.swing.BooleanTableCellRenderer;
import com.affymetrix.genoviz.swing.ButtonTableCellEditor;
import com.affymetrix.genoviz.swing.LabelTableCellRenderer;
import com.affymetrix.igb.swing.JRPJPanel;
import com.affymetrix.igb.swing.jide.StyledJTable;
import com.google.common.base.Charsets;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Resources;
import org.lorainelab.igb.plugin.manager.repos.PluginRepositoryList;
import org.lorainelab.igb.plugin.manager.repos.events.PluginRepositoryEventPublisher;
import org.lorainelab.igb.plugin.manager.repos.events.ShowBundleRepositoryPanelEvent;
import org.lorainelab.igb.services.IgbService;
import org.lorainelab.igb.services.window.HtmlHelpProvider;
import org.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import java.io.IOException;
import java.util.Enumeration;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = BundleRepositoryPrefsView.COMPONENT_NAME, immediate = true, provide = PreferencesPanelProvider.class)
public class BundleRepositoryPrefsView extends JRPJPanel implements PreferencesPanelProvider, HtmlHelpProvider {

    public static final String COMPONENT_NAME = "BundleRepositoryPrefsView";
    public static final String TAB_NAME = "App Repositories";
    private static final int TAB_POSITION = 7;
    private BundleRepositoryTableModel tableModel;
    private PluginRepositoryList pluginRepositoryList;
    private final Icon refresh_icon;
    private AddBundleRepositoryFrame addBundleRepositoryFrame;
    private StyledJTable table;
    private IgbService igbService;
    private static final Logger logger = LoggerFactory.getLogger(BundleRepositoryPrefsView.class);

    public BundleRepositoryPrefsView() {
        super(BundleRepositoryPrefsView.class.getName());
        refresh_icon = CommonUtils.getInstance().getIcon("16x16/actions/refresh.png");
    }

    @Activate
    public void activate() {
        tableModel = pluginRepositoryList.getBundleRepositoryTableModel();
        initializeTable();
        addBundleRepositoryFrame = new AddBundleRepositoryFrame(this, pluginRepositoryList);
        initComponents();
        initializeSelectionListener();
    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Reference
    public void setEventBus(PluginRepositoryEventPublisher eventManager) {
        eventManager.getPluginRepositoryEventBus().register(this);
    }

    @Subscribe
    public void showTabPanelEvent(ShowBundleRepositoryPanelEvent event) {
        int tabIndex = igbService.getPreferencesPanelTabIndex(this); //should probably be the same as TAB_POSITION but might not be
        if (tabIndex != -1) {
            igbService.openPreferencesPanelTab(BundleRepositoryPrefsView.class);
        }
    }

    private void initializeSelectionListener() {
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent event) {
                ListSelectionModel lsm = (ListSelectionModel) event.getSource();
                tableModel.setSelectedRow(lsm.getMinSelectionIndex());
            }
        });
    }

    private void initializeTable() {
        table = new StyledJTable(tableModel);

        table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
        TableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {

                int modelRow = table.convertRowIndexToModel(row);
                this.setEnabled((Boolean) tableModel.getValueAt(modelRow, tableModel.getColumnIndex(BundleRepositoryTableModel.ENABLED_COLOMN)));
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            }
        };
        table.setDefaultRenderer(String.class, renderer);
        TableCellRenderer refresh_renderer = new LabelTableCellRenderer(refresh_icon, true) {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {

                java.awt.Component ret = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                int modelRow = table.convertRowIndexToModel(row);
                this.setEnabled((Boolean) tableModel.getValueAt(modelRow, tableModel.getColumnIndex(BundleRepositoryTableModel.ENABLED_COLOMN)));
                return ret;
            }
        };

        for (Enumeration<TableColumn> e = table.getColumnModel().getColumns(); e.hasMoreElements();) {
            TableColumn column = e.nextElement();
            switch ((String) column.getHeaderValue()) {
                case BundleRepositoryTableModel.REFRESH_COLOMN:
                    column.setMaxWidth(20);
                    column.setCellRenderer(refresh_renderer);
                    column.setCellEditor(new ButtonTableCellEditor(refresh_icon));
                    break;
                case BundleRepositoryTableModel.NAME_COLOMN:
                    column.setPreferredWidth(100);
                    break;
                case BundleRepositoryTableModel.URL_COLOMN:
                    column.setPreferredWidth(300);
                    break;
                case BundleRepositoryTableModel.ENABLED_COLOMN:
                    column.setPreferredWidth(30);
                    break;
                default:
                    column.setPreferredWidth(50);
                    break;
            }
        }
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setCellSelectionEnabled(false);
        table.setRowSelectionAllowed(true);
    }

    @Reference(optional = false)
    public void setPluginRepositoryList(PluginRepositoryList pluginRepositoryList) {
        this.pluginRepositoryList = pluginRepositoryList;
    }

    @Override
    public String getHelpHtml() {
        String htmlText = null;
        try {
            htmlText = Resources.toString(BundleRepositoryPrefsView.class.getResource("/help/bundleRepositoryPrefsView.html"), Charsets.UTF_8);
        } catch (IOException ex) {
            logger.error("Help file not found ", ex);
        }
        return htmlText;
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        pluginRepositoryTable = table;
        removeButton = new javax.swing.JButton();
        addButton = new javax.swing.JButton();
        done = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createTitledBorder("Plugin Repositories"));

        pluginRepositoryTable.setModel(tableModel);
        jScrollPane1.setViewportView(pluginRepositoryTable);

        removeButton.setText("Remove");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        addButton.setText("Add...");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        done.setText("Done");
        done.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doneActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 617, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(addButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(removeButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(done)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(removeButton)
                    .addComponent(addButton)
                    .addComponent(done))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        addBundleRepositoryFrame.init(false, null);
    }//GEN-LAST:event_addButtonActionPerformed

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        // TODO add your handling code here:
        if (tableModel.getSelectedPluginRepository() != null) {
            pluginRepositoryList.removePluginRepository(tableModel.getSelectedPluginRepository());
        }
    }//GEN-LAST:event_removeButtonActionPerformed

    private void doneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_doneActionPerformed
        igbService.dismissPreferences();
    }//GEN-LAST:event_doneActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JButton done;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable pluginRepositoryTable;
    private javax.swing.JButton removeButton;
    // End of variables declaration//GEN-END:variables

    @Override
    public String getName() {
        return TAB_NAME;
    }

    @Override
    public int getWeight() {
        return TAB_POSITION;
    }

    @Override
    public JRPJPanel getPanel() {
        return this;
    }

    @Override
    public void refresh() {
        //do nothing
    }

}
