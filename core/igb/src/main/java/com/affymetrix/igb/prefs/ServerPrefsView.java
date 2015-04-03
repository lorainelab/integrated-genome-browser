/**
 * Copyright (c) 2010 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.prefs;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometry.general.GenericServer;
import static com.affymetrix.genometry.general.GenericServerPrefKeys.SERVER_ORDER;
import com.affymetrix.genometry.util.FileTracker;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genometry.util.ServerTypeI;
import com.affymetrix.genoviz.swing.BooleanTableCellRenderer;
import com.affymetrix.genoviz.swing.ButtonTableCellEditor;
import com.affymetrix.genoviz.swing.LabelTableCellRenderer;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.swing.JRPJPanel;
import com.affymetrix.igb.swing.jide.StyledJTable;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.GroupLayout;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import javax.swing.GroupLayout.Group;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public abstract class ServerPrefsView extends JRPJPanel implements PreferencesPanelProvider {

    private static final long serialVersionUID = 1L;
    private static final Icon refresh_icon = CommonUtils.getInstance().getIcon("16x16/actions/refresh.png");
    protected final JPanel sourcePanel;
    protected final GroupLayout layout;
    protected ServerList serverList;
    protected StyledJTable sourcesTable;
    protected JScrollPane sourcesScrollPane;
    protected JRPButton addServerButton;
    protected JRPButton removeServerButton;
    public final SourceTableModel sourceTableModel;

    public ServerPrefsView(ServerList serverList) {
        super(ServerPrefsView.class.getName());
        layout = new GroupLayout(this.getPanel());
        this.serverList = serverList;
        sourceTableModel = new SourceTableModel(serverList);

        sourcePanel = initSourcePanel(getViewName());

        this.getPanel().setName(getViewName());
        this.getPanel().setToolTipText(getToolTip());

        this.getPanel().setLayout(layout);

        layout.setAutoCreateGaps(
                true);
        layout.setAutoCreateContainerGaps(
                true);
    }

    public void refreshServers() {
        ((SourceTableModel) sourcesTable.getModel()).init();
    }

    protected JPanel initSourcePanel(String viewName) {
        final JPanel sourcePanel = new JPanel();
        final GroupLayout layout = new GroupLayout(sourcePanel);

        sourcesTable = createSourcesTable(sourceTableModel, isSortable());
        sourcesScrollPane = new JScrollPane(sourcesTable);

        sourcePanel.setLayout(layout);
        sourcePanel.setBorder(new TitledBorder(viewName));
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        addServerButton = createButton("ServerPrefsView_addServerButton", "Add\u2026", e -> {
            sourcesTable.stopCellEditing();

            AddSource.getSingleton().init(false, enableCombo(), "Add Data Source", null, null, null);
        });

        removeServerButton = createButton("ServerPrefsView_removeServerButton", "Remove", e -> {
            sourcesTable.stopCellEditing();
            if (confirmDelete()) {
                Object url = sourcesTable.getModel().getValueAt(
                        sourcesTable.convertRowIndexToModel(sourcesTable.getSelectedRow()),
                        ((SourceTableModel) sourcesTable.getModel()).getColumnIndex(SourceTableModel.SourceColumn.URL));
                removeDataSource(url.toString());
                sourceTableModel.init();
            }
        });
        removeServerButton.setEnabled(false);

        sourcesTable.getSelectionModel().addListSelectionListener(event -> {
            enableServerButtons(false);

            if (sourcesTable.getSelectedRowCount() == 1) {
                Object url = sourcesTable.getModel().getValueAt(
                        sourcesTable.convertRowIndexToModel(sourcesTable.getSelectedRow()),
                        ((SourceTableModel) sourcesTable.getModel()).getColumnIndex(SourceTableModel.SourceColumn.URL));
                GenericServer server = ServerList.getServerInstance().getServer((String) url);

                if (server == null) {

                } else if (!server.isDefault()) {
                    enableServerButtons(true);
                }
            }
        });

        layout.setHorizontalGroup(addServerComponents(layout.createParallelGroup(TRAILING), layout.createSequentialGroup()));
        layout.setVerticalGroup(addServerComponents(layout.createSequentialGroup(), layout.createParallelGroup(BASELINE)));
        return sourcePanel;
    }

    protected void enableServerButtons(boolean enable) {
        removeServerButton.setEnabled(enable);
    }

    protected abstract boolean isSortable();

    protected abstract Group addServerComponents(Group group1, Group group2);

    protected abstract Group getServerButtons(Group group);

    private static StyledJTable createSourcesTable(SourceTableModel sourceTableModel, boolean sortable) {
        final StyledJTable table = new StyledJTable(sourceTableModel);
        table.setAutoCreateRowSorter(sortable);

        if (sortable) {
            table.getRowSorter().setSortKeys(SourceTableModel.SORT_KEYS);
        }
        table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
        TableCellRenderer renderer = new DefaultTableCellRenderer() {

            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {

                int modelRow = table.convertRowIndexToModel(row);
                this.setEnabled((Boolean) table.getModel().getValueAt(modelRow, ((SourceTableModel) table.getModel()).getColumnIndex(SourceTableModel.SourceColumn.Enabled)));
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            }
        };
        table.setDefaultRenderer(String.class, renderer);
        table.setDefaultRenderer(ServerTypeI.class, renderer);

        TableCellRenderer refresh_renderer = new LabelTableCellRenderer(refresh_icon, true) {

            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {

                Component ret = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                int modelRow = table.convertRowIndexToModel(row);
                this.setEnabled((Boolean) table.getModel().getValueAt(modelRow, ((SourceTableModel) table.getModel()).getColumnIndex(SourceTableModel.SourceColumn.Enabled)));
                return ret;
            }
        };

        for (Enumeration<TableColumn> e = table.getColumnModel().getColumns(); e.hasMoreElements();) {
            TableColumn column = e.nextElement();
            SourceTableModel.SourceColumn current = SourceTableModel.SourceColumn.valueOf((String) column.getHeaderValue());

            switch (current) {
                case Refresh:
                    column.setMaxWidth(20);
                    column.setCellRenderer(refresh_renderer);
                    column.setCellEditor(new ButtonTableCellEditor(refresh_icon));
                    break;
                case Name:
                    column.setPreferredWidth(100);
                    break;
                case URL:
                    column.setPreferredWidth(300);
                    break;
                case Enabled:
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

        return table;
    }

    public void addDataSource(GenericServer server) {
        Preferences node = PreferenceUtils.getServersNode().node(GenericServer.getHash(server.getUrlString()));
        int order = node.getInt(SERVER_ORDER, -1);
        boolean isDefault = serverList.getServer(server.getUrlString()).isDefault();
        GeneralLoadUtils.addServer(serverList, server.getServerType(), server.getServerName(), server.getUrlString(), order, isDefault, server.getMirrorUrl());
    }

    /**
     * Add the URL/Directory and server name to the preferences.
     *
     * @param url
     * @param type
     * @param name
     */
    public boolean addDataSource(ServerTypeI type, String name, String url, int order, boolean isDefault) {
        if (url == null || url.isEmpty() || url.equals("http://") || name == null || name.isEmpty()) {
            return false;
        }
        String mirrorUrl = null;
        GenericServer server = GeneralLoadUtils.addServer(serverList, type, name, url, order, isDefault, mirrorUrl);

        sourceTableModel.init();
        if (server == null) {

            return false;
        }

        ServerList.getServerInstance().addServerToPrefs(server, order, isDefault);
        if (server.isEnabled()) {
            return true;
        }
        return false;
    }

    public boolean confirmRefresh() {
        String message = "Warning:\n"
                + "Refreshing the server will force IGB to re-read configuration files from the server.\n"
                + "This means all data sets currently loaded from the server will be deleted.\n"
                + "This is useful mainly for setting up or configuring a QuickLoad site.";

        return ModalUtils.confirmPanel(DataLoadPrefsView.getSingleton(),
                message, PreferenceUtils.getTopNode(),
                PreferenceUtils.CONFIRM_BEFORE_REFRESH,
                PreferenceUtils.default_confirm_before_refresh);
    }

    public boolean confirmDelete() {
        String message = "Warning:\n"
                + "Disabling or removing a server will cause any"
                + " currently loaded data from that server to be removed from IGB.\n";

        return ModalUtils.confirmPanel(DataLoadPrefsView.getSingleton(),
                message, PreferenceUtils.getTopNode(),
                PreferenceUtils.CONFIRM_BEFORE_DELETE,
                PreferenceUtils.default_confirm_before_delete);
    }

    protected void removeDataSource(String url) {
        if (serverList.getServer(url) == null) {
            Logger.getLogger(ServerPrefsView.class.getName()).log(
                    Level.SEVERE, "Can not remove Server ''{0}'': it does not exist in ServerList", url);
            return;
        }

        serverList.removeServer(url);
        serverList.removeServerFromPrefs(url);	// this is done last; other methods can depend upon the preference node
    }

    protected static JRPButton createButton(String id, String name, ActionListener listener) {
        final JRPButton button = new JRPButton(id, name);
        button.addActionListener(listener);
        return button;
    }

    protected static File fileChooser(int mode, Component parent) throws HeadlessException {
        JFileChooser chooser = new JFileChooser();

        chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
        chooser.setFileSelectionMode(mode);
        chooser.setDialogTitle("Choose " + (mode == DIRECTORIES_ONLY ? "Directory" : "File"));
        chooser.setAcceptAllFileFilterUsed(mode != DIRECTORIES_ONLY);
        chooser.rescanCurrentDirectory();

        if (chooser.showOpenDialog(parent) != APPROVE_OPTION) {
            return null;
        }

        return chooser.getSelectedFile();
    }

    public void refresh() {
    }

    protected abstract String getViewName();

    protected abstract String getToolTip();

    protected abstract boolean enableCombo();

    protected abstract void updateSource(String url, ServerTypeI type, String name, String newUrl);

    protected abstract void updateSource(GenericServer server);
}
