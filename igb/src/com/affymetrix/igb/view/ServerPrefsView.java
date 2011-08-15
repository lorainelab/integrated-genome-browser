/**
 *   Copyright (c) 2010 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.prefs.IPrefEditorComponent;
import com.affymetrix.igb.prefs.SourceTableModel;
import com.affymetrix.igb.prefs.SourceTableModel.SourceColumn;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.genoviz.swing.BooleanTableCellRenderer;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.GroupLayout.Group;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;

public abstract class ServerPrefsView extends IPrefEditorComponent {
	private static final long serialVersionUID = 2l;

	private static final String[] OPTIONS = new String[]{"Add Server", "Cancel"};
	protected final JPanel sourcePanel;
	protected final GroupLayout layout;
	protected ServerList serverList;
	protected JTable sourcesTable;
	protected JScrollPane sourcesScrollPane;
	protected JButton addServerButton;
	protected JButton removeServerButton;
	// for add source dialog
	protected JLabel nameLabel;
	protected JLabel urlLabel;
	protected JTextField name;
	protected JTextField url;
	protected JPanel messageContainer = new JPanel();

	public ServerPrefsView(ServerList serverList_) {
		layout = new GroupLayout(this);
		serverList = serverList_;
		sourcePanel = initSourcePanel(getViewName());
	
		this.setName(getViewName());
		this.setToolTipText(getToolTip());

		this.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
	}

	protected JPanel initSourcePanel(String viewName) {
		final JPanel sourcePanel = new JPanel();
		final GroupLayout layout = new GroupLayout(sourcePanel);
		final SourceTableModel sourceTableModel = new SourceTableModel(serverList);
		sourcesTable = createSourcesTable(sourceTableModel, isSortable());
		sourcesScrollPane = new JScrollPane(sourcesTable);

		sourcePanel.setLayout(layout);
		sourcePanel.setBorder(new TitledBorder(viewName));
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		addServerButton = createButton("Add\u2026", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showAddSourceDialog();
				sourceTableModel.init();
			}
		});

		removeServerButton = createButton("Remove", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object url = sourcesTable.getModel().getValueAt(
						sourcesTable.convertRowIndexToModel(sourcesTable.getSelectedRow()),
						((SourceTableModel)sourcesTable.getModel()).getColumnIndex(SourceColumn.URL));
				removeDataSource(url.toString());
				sourceTableModel.init();
			}
		});
		removeServerButton.setEnabled(false);

		sourcesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				int viewRow = sourcesTable.getSelectedRow();
				boolean enable = viewRow >= 0;
				
				enableServerButtons(enable);
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

	private static JTable createSourcesTable(SourceTableModel sourceTableModel, boolean sortable) {
		final JTable table = new JTable(sourceTableModel);

		table.setFillsViewportHeight(true);
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
				this.setEnabled((Boolean) table.getModel().getValueAt(modelRow, ((SourceTableModel)table.getModel()).getColumnIndex(SourceColumn.Enabled)));
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
			}
		};
		table.setDefaultRenderer(String.class,  renderer);
		table.setDefaultRenderer(ServerType.class, renderer);

		for (Enumeration<TableColumn> e = table.getColumnModel().getColumns(); e.hasMoreElements(); ) {
			TableColumn column = e.nextElement();
			SourceColumn current = SourceColumn.valueOf((String)column.getHeaderValue());

			switch (current) {
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

		return table;
	}

	protected abstract void addDataSource();

	private void showAddSourceDialog() {
		int result = JOptionPane.showOptionDialog(
				null,
				createAddSourceDialog(),
				"Add Data Source",
				OK_CANCEL_OPTION,
				PLAIN_MESSAGE,
				null,
				OPTIONS,
				OPTIONS[0]);

		if (result == OK_OPTION) {
			addDataSource();
		}
	}

	/**
	 * Add the URL/Directory and server name to the preferences.
	 * @param url
	 * @param type
	 * @param name
	 */
	protected void addDataSource(ServerType type, String name, String url) {
		if (url == null || url.isEmpty() || name == null || name.isEmpty()) {
			return;
		}
		
		GenericServer server = GeneralLoadUtils.addServer(serverList, type, name, url);

		if (server == null) {
			ErrorHandler.errorPanel(
					"Unable to Load Data Source",
					"Unable to load " + type + " data source '" + url + "'.");
			return;
		}

		//ServerList.addServerToPrefs(server);
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
	
	protected static JButton createButton(String name, ActionListener listener) {
		final JButton button = new JButton(name);
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
		
		if (chooser.showOpenDialog(parent) != APPROVE_OPTION) { return null; }

		return chooser.getSelectedFile();
	}

	protected interface AddSourceGroupCreator {
		public Group createGroup1(GroupLayout layout);
		public Group createGroup2(GroupLayout layout);
	}

	protected abstract Group addAddSourceComponents(AddSourceGroupCreator addSourceGroupCreator, GroupLayout layout);

	protected JPanel createAddSourceDialog() {
		final JPanel addServerPanel = new JPanel();
		messageContainer = new JPanel();
		nameLabel = new JLabel("Name");
		urlLabel = new JLabel("URL");
		name = new JTextField("Your " + serverList.getTextName() + " name");
		url = new JTextField("http://");

		final GroupLayout layout = new GroupLayout(addServerPanel);

		addServerPanel.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		setSize(layout, name);

		layout.setHorizontalGroup(
			addAddSourceComponents(
				new AddSourceGroupCreator() {
					public Group createGroup1(GroupLayout layout) {
						return layout.createParallelGroup();
					}
					public Group createGroup2(GroupLayout layout) {
						return layout.createSequentialGroup();
					}
				},
				layout
			)
		);

		layout.setVerticalGroup(
			addAddSourceComponents(
				new AddSourceGroupCreator() {
					public Group createGroup1(GroupLayout layout) {
						return layout.createSequentialGroup();
					}
					public Group createGroup2(GroupLayout layout) {
						return layout.createParallelGroup(BASELINE);
					}
				},
				layout
			)
		);

		messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));

		return addServerPanel;
	}

	protected abstract void setSize(GroupLayout layout, JTextField name);

	public void refresh() { }

	protected abstract String getViewName();

	protected abstract String getToolTip();
}
