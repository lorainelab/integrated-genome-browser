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

import com.affymetrix.igb.shared.IPrefEditorComponent;
import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genoviz.swing.BooleanTableCellRenderer;
import com.affymetrix.genoviz.swing.ButtonTableCellEditor;
import com.affymetrix.genoviz.swing.LabelTableCellRenderer;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.igb.shared.StyledJTable;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import javax.swing.GroupLayout.Group;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;

public abstract class ServerPrefsView extends IPrefEditorComponent {

	private static final long serialVersionUID = 2l;
	private static final Icon refresh_icon = CommonUtils.getInstance().getIcon("16x16/actions/refresh.png");
	protected final JPanel sourcePanel;
	protected final GroupLayout layout;
	protected ServerList serverList;
	protected StyledJTable sourcesTable;
	protected JScrollPane sourcesScrollPane;
	protected JRPButton addServerButton;
	protected JRPButton removeServerButton;
	public final SourceTableModel sourceTableModel;

	public ServerPrefsView(ServerList serverList_) {
		layout = new GroupLayout(this);
		serverList = serverList_;
		sourceTableModel = new SourceTableModel(serverList);

		sourcePanel = initSourcePanel(getViewName());

		this.setName(getViewName());
		this.setToolTipText(getToolTip());

		this.setLayout(layout);

		layout.setAutoCreateGaps(
				true);
		layout.setAutoCreateContainerGaps(
				true);
	}

	public void refreshServers() {
		((SourceTableModel)sourcesTable.getModel()).init();
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

		addServerButton = createButton("ServerPrefsView_addServerButton", "Add\u2026", new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				sourcesTable.stopCellEditing();

				AddSource.getSingleton().init(false, enableCombo(), "Add Source", null, null, null);
			}
		});

		removeServerButton = createButton("ServerPrefsView_removeServerButton", "Remove", new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				sourcesTable.stopCellEditing();
				if (confirmDelete()) {
					Object url = sourcesTable.getModel().getValueAt(
							sourcesTable.convertRowIndexToModel(sourcesTable.getSelectedRow()),
							((SourceTableModel) sourcesTable.getModel()).getColumnIndex(SourceTableModel.SourceColumn.URL));
					removeDataSource(url.toString());
					sourceTableModel.init();
				}
			}
		});
		removeServerButton.setEnabled(false);

		sourcesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent event) {
				enableServerButtons(false);

				if (sourcesTable.getSelectedRowCount() == 1) {
					Object url = sourcesTable.getModel().getValueAt(
							sourcesTable.convertRowIndexToModel(sourcesTable.getSelectedRow()),
							((SourceTableModel) sourcesTable.getModel()).getColumnIndex(SourceTableModel.SourceColumn.URL));
					GenericServer server = ServerList.getServerInstance().getServer((String) url);

					if (server == null) {
						server = ServerList.getRepositoryInstance().getServer((String) url);
					}

					if (!server.isDefault()) {
						enableServerButtons(true);
					}
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

			private static final long serialVersionUID = -1l;

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

			private static final long serialVersionUID = -1l;

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

	/**
	 * Add the URL/Directory and server name to the preferences.
	 *
	 * @param url
	 * @param type
	 * @param name
	 */
	public boolean addDataSource(ServerTypeI type, String name, String url, int order, boolean isDefault, String mirrorURL) {
		if (url == null || url.isEmpty() || url.equals("http://") || name == null || name.isEmpty()) {
			return false;
		}

		GenericServer server = GeneralLoadUtils.addServer(serverList,
				type, name, url, order, isDefault, mirrorURL); //qlmirror

		if (server == null) {

			return false;
		}

		ServerList.getServerInstance().addServerToPrefs(server, order, isDefault);
		sourceTableModel.init();
		if(server.isEnabled()) {
			return true;
		}
		return false;
	}

	public boolean confirmRefresh() {
		String message = "Warning:\n"
				+ "Refreshing the server will force IGB to re-read configuration files from the server.\n"
				+ "This means all data sets currently loaded from the server will be deleted.\n"
				+ "This is useful mainly for setting up or configuring a QuickLoad site.";

		return Application.confirmPanel(DataLoadPrefsView.getSingleton(),
				message, PreferenceUtils.getTopNode(),
				PreferenceUtils.CONFIRM_BEFORE_REFRESH,
				PreferenceUtils.default_confirm_before_refresh);
	}

	public boolean confirmDelete() {
		String message = "Warning:\n"
				+ "Disabling or removing a server will cause any"
				+ " currently loaded data from that server to be removed from IGB.\n";

		return Application.confirmPanel(DataLoadPrefsView.getSingleton(),
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

		if (serverList.getServer(url).serverType == null) {
			IGBServiceImpl.getInstance().getRepositoryChangerHolder().repositoryRemoved(serverList.getServer(url).URL);
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
	
	protected abstract void updateSource(String url, ServerTypeI type, String name, String newUrl, String mirrorURL);
}
