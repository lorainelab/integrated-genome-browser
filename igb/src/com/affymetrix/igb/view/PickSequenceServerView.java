/**
 *   Copyright (c) 2006 Affymetrix, Inc.
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
import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.prefs.SourceTableModel;
import com.affymetrix.igb.prefs.SourceTableModel.SourceColumn;
import com.affymetrix.igb.util.IGBAuthenticator;

import java.awt.Dimension;
import java.awt.event.*;
import java.io.*;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.Authenticator.RequestorType;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.GroupLayout.Group;

import static javax.swing.JFileChooser.DIRECTORIES_ONLY;

/**
 *
 * @author lfrohman
 * @version $Id: PickSequenceServerView.java 7258 2010-12-17 21:40:02Z lfrohman $
 */
public final class PickSequenceServerView extends ServerPrefsView {
	private static final long serialVersionUID = 2l;

	protected JButton editAuthButton;
	protected JButton rankUpButton;
	protected JButton rankDownButton;
	// for add source dialog
	private JLabel typeLabel;
	private JButton openDir;
	private JComboBox type;

	public PickSequenceServerView() {
		super(ServerList.getSequenceServerInstance());
	
		layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(sourcePanel));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(sourcePanel));
		populateServerData();
	}

	protected JPanel initSourcePanel(String viewName) {
		editAuthButton = createButton("Authentication\u2026", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object url = sourcesTable.getModel().getValueAt(
						sourcesTable.convertRowIndexToModel(sourcesTable.getSelectedRow()),
						((SourceTableModel)sourcesTable.getModel()).getColumnIndex(SourceColumn.URL));
				try {
					URL u = new URL((String) url);
					IGBAuthenticator.resetAuth((String) url);
					Authenticator.requestPasswordAuthentication(
							u.getHost(),
							null,
							u.getPort(),
							u.getProtocol(),
							"Server Credentials",
							null,
							u,
							RequestorType.SERVER);
				} catch (MalformedURLException ex) {
					Logger.getLogger(ServerPrefsView.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});
		editAuthButton.setEnabled(false);
	    ImageIcon up_icon = MenuUtil.getIcon("toolbarButtonGraphics/navigation/Up16.gif");
		rankUpButton = new JButton(up_icon);
		rankUpButton.setToolTipText("Move sequence server up");
		rankUpButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int row = sourcesTable.getSelectedRow();
					if (row >= 1 && row < sourcesTable.getRowCount()) {
						((SourceTableModel)sourcesTable.getModel()).switchRows(row - 1);
						sourcesTable.getSelectionModel().setSelectionInterval(row - 1, row - 1);
						int column = ((SourceTableModel)sourcesTable.getModel()).getColumnIndex(SourceColumn.URL);
						String URL = sourcesTable.getModel().getValueAt(row - 1, column).toString();
						serverList.setServerOrder(URL, row - 1);
						URL = sourcesTable.getModel().getValueAt(row, column).toString();
						serverList.setServerOrder(URL, row);
					}
				}
			}
		);
		rankUpButton.setEnabled(false);
	    ImageIcon down_icon = MenuUtil.getIcon("toolbarButtonGraphics/navigation/Down16.gif");
		rankDownButton = new JButton(down_icon);
		rankDownButton.setToolTipText("Move sequence server down");
		rankDownButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int row = sourcesTable.getSelectedRow();
					if (row >= 0 && row < sourcesTable.getRowCount() - 1) {
						((SourceTableModel)sourcesTable.getModel()).switchRows(row);
						sourcesTable.getSelectionModel().setSelectionInterval(row + 1, row + 1);
						int column = ((SourceTableModel)sourcesTable.getModel()).getColumnIndex(SourceColumn.URL);
						String URL = sourcesTable.getModel().getValueAt(row, column).toString();
						serverList.setServerOrder(URL, row);
						URL = sourcesTable.getModel().getValueAt(row + 1, column).toString();
						serverList.setServerOrder(URL, row + 1);
					}
				}
			}
		);
		rankDownButton.setEnabled(false);
		return super.initSourcePanel(viewName);
	}

	@Override
	protected void enableServerButtons(boolean enable) {
		super.enableServerButtons(enable);
		editAuthButton.setEnabled(enable);
		rankUpButton.setEnabled(enable && sourcesTable.getSelectedRow() > 0);
		rankDownButton.setEnabled(enable && sourcesTable.getSelectedRow() < sourcesTable.getRowCount() - 1);
	}

	protected boolean isSortable() {
		return false;
	}

	@Override
	protected Group addServerComponents(Group group1, Group group2) {
		return group1
		.addComponent(sourcesScrollPane)
		.addGroup(group2
			.addComponent(rankUpButton)
			.addComponent(rankDownButton)
			.addComponent(addServerButton)
			.addComponent(editAuthButton)
			.addComponent(removeServerButton));
	}

	@Override
	protected Group getServerButtons(Group group) {
		return group.addComponent(addServerButton)
		.addComponent(editAuthButton)
		.addComponent(removeServerButton);
	}

	@Override
	protected String getViewName() {
		return "Pick Sequence Server";
	}

	@Override
	protected String getToolTip() {
		return "pick and order the sequence load servers";
	}

	@Override
	protected Group addAddSourceComponents(AddSourceGroupCreator addSourceGroupCreator, GroupLayout layout) {
		return addSourceGroupCreator.createGroup1(layout)
		.addComponent(messageContainer)
		.addGroup(addSourceGroupCreator.createGroup2(layout)
			.addComponent(nameLabel)
			.addComponent(name))
		.addGroup(addSourceGroupCreator.createGroup2(layout)
			.addComponent(typeLabel)
			.addComponent(type))
		.addGroup(addSourceGroupCreator.createGroup2(layout)
			.addComponent(urlLabel)
			.addComponent(url)
			.addComponent(openDir));
	}

	@Override
	protected JPanel createAddSourceDialog() {
		typeLabel = new JLabel("Type");
		type = new JComboBox(LoadUtils.ServerType.values());

		if (type != null) {
			type.removeItem(LoadUtils.ServerType.LocalFiles);
			type.setSelectedItem(LoadUtils.ServerType.QuickLoad);	// common default
		}
		openDir = new JButton("\u2026");

		openDir.setToolTipText("Open Local Directory");
		openDir.setEnabled(type != null && type.getSelectedItem() == LoadUtils.ServerType.QuickLoad);

		type.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openDir.setEnabled(type.getSelectedItem() == LoadUtils.ServerType.QuickLoad);
			}
		});

		final JPanel addServerPanel = super.createAddSourceDialog();
		openDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File f = fileChooser(DIRECTORIES_ONLY, addServerPanel);
				if (f != null && f.isDirectory()) {
					try {
						url.setText(f.toURI().toURL().toString());
					} catch (MalformedURLException ex) {
						Logger.getLogger(ServerPrefsView.class.getName()).log(Level.WARNING, "Unable to convert File '" + f.getName() + "' to URL", ex);
					}
				}
			}
		});

		return addServerPanel;
	}

	@Override
	protected void addDataSource() {
		addDataSource((ServerType)type.getSelectedItem(), name.getText(), url.getText());
	}

	@Override
	protected void setSize(GroupLayout layout, JTextField name) {
		layout.linkSize(nameLabel, urlLabel, typeLabel);
		name.setPreferredSize(new Dimension(300, name.getPreferredSize().height));
		layout.linkSize(name, type);
		layout.linkSize(SwingConstants.VERTICAL, name, type, url);
	}

	/**
	 * Discover servers, species, etc., asynchronously.
	 * @param loadGenome parameter to check if genomes should be loaded from
	 * actual server or not.
	 */
	private void populateServerData() {
		for (final GenericServer gServer : ServerList.getSequenceServerInstance().getEnabledServers()) {
			Executor vexec = Executors.newSingleThreadExecutor();
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				protected Void doInBackground() throws Exception {
					ServerList.getSequenceServerInstance().discoverServer(gServer);
					return null;
				}
			};

			vexec.execute(worker);
		}
	}

}
