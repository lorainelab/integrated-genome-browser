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

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.genoviz.swing.recordplayback.JRPTextField;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.igb.action.AutoLoadAction;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.GroupLayout.Group;
import javax.swing.border.TitledBorder;

import static com.affymetrix.genometryImpl.util.LocalUrlCacher.CacheUsage;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static javax.swing.JFileChooser.FILES_AND_DIRECTORIES;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public final class DataLoadPrefsView extends ServerPrefsView {
	private static final long serialVersionUID = 2l;

	private static final String PREF_SYN_FILE_URL = "Synonyms File URL";

	private static final JCheckBox autoload = AutoLoadAction.getAction();

	protected JRPButton editAuthButton;
	protected JRPButton rankUpButton;
	protected JRPButton rankDownButton;
	// for add source dialog
	private JLabel typeLabel;
	private JRPButton openDir;
	private JComboBox type;

	public DataLoadPrefsView() {
		super(ServerList.getServerInstance());
		final JPanel synonymsPanel = initSynonymsPanel(this);
		final JPanel cachePanel = initCachePanel();

		layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(sourcePanel)
				.addComponent(synonymsPanel)
				.addComponent(cachePanel));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(sourcePanel)
				.addComponent(synonymsPanel)
				.addComponent(cachePanel));
	}

	@Override
	protected JPanel initSourcePanel(String viewName) {
		editAuthButton = createButton("DataLoadPrefsView_editAuthButton", "Authentication\u2026", new ActionListener() {
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
		rankUpButton = new JRPButton("DataLoadPrefsView_rankUpButton", up_icon);
		rankUpButton.setToolTipText("Increase sequence server priority");
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
		rankDownButton = new JRPButton("DataLoadPrefsView_rankDownButton", down_icon);
		rankDownButton.setToolTipText("Decrease sequence server priority");
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
		rankUpButton.setEnabled(enable && sourcesTable.getSelectedRow() > 0);
		rankDownButton.setEnabled(enable && sourcesTable.getSelectedRow() < sourcesTable.getRowCount() - 1);
		editAuthButton.setEnabled(enable);
	}

	@Override
	protected boolean isSortable() {
		return false;
	}

	@Override
	protected Group addServerComponents(Group group1, Group group2) {
		return group1
		.addComponent(sourcesScrollPane)
		.addComponent(autoload)
		.addGroup(group2
			.addComponent(rankUpButton)
			.addComponent(rankDownButton)
			.addComponent(addServerButton)
			.addComponent(editAuthButton)
			.addComponent(removeServerButton));
	}

	@Override
	protected Group getServerButtons(Group group) {
		return group
		.addComponent(rankUpButton)
		.addComponent(rankDownButton)
		.addComponent(addServerButton)
		.addComponent(editAuthButton)
		.addComponent(removeServerButton);
	}

	private static JPanel initSynonymsPanel(final JPanel parent) {
		final JPanel synonymsPanel = new JPanel();
		final GroupLayout layout = new GroupLayout(synonymsPanel);
		final JLabel synonymsLabel= new JLabel("Synonyms File");
		final JRPTextField synonymFile = new JRPTextField("DataLoadPrefsView_synonymFile", PreferenceUtils.getLocationsNode().get(PREF_SYN_FILE_URL, ""));
		final JRPButton openFile = new JRPButton("DataLoadPrefsView_openFile", "\u2026");
		final ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == openFile) {
					File file = fileChooser(FILES_AND_DIRECTORIES, parent);
					try {
						if (file != null) {
							synonymFile.setText(file.getCanonicalPath());
						}
					} catch (IOException ex) {
						Logger.getLogger(DataLoadPrefsView.class.getName()).log(Level.SEVERE, null, ex);
					}
				}

				if (synonymFile.getText().isEmpty() || loadSynonymFile(synonymFile)) {
					PreferenceUtils.getLocationsNode().put(PREF_SYN_FILE_URL, synonymFile.getText());
				} else {
					ErrorHandler.errorPanel(
					"Unable to Load Synonyms",
					"Unable to load personal synonyms from " + synonymFile.getText() + ".");
				}
			}
		};

		openFile.setToolTipText("Open Local Directory");
		openFile.addActionListener(listener);
		synonymFile.addActionListener(listener);



		synonymsPanel.setLayout(layout);
		synonymsPanel.setBorder(new TitledBorder("Personal Synonyms"));
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addComponent(synonymsLabel)
				.addComponent(synonymFile)
				.addComponent(openFile));

		layout.setVerticalGroup(layout.createParallelGroup(BASELINE)
				.addComponent(synonymsLabel)
				.addComponent(synonymFile)
				.addComponent(openFile));

		/* Load the synonym file from preferences on startup */
		loadSynonymFile(synonymFile);

		return synonymsPanel;
	}

	private static JPanel initCachePanel() {
		final JPanel cachePanel = new JPanel();
		final GroupLayout layout = new GroupLayout(cachePanel);
		final JLabel usageLabel = new JLabel("Cache Behavior");
		final JLabel emptyLabel = new JLabel();
		final JComboBox	cacheUsage = new JComboBox(CacheUsage.values());
		final JRPButton clearCache = new JRPButton("DataLoadPrefsView_clearCache", "Empty Cache");
		clearCache.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearCache.setEnabled(false);
				try {
					Thread.sleep(300);
				} catch (InterruptedException ex) {
					Logger.getLogger(DataLoadPrefsView.class.getName()).log(Level.SEVERE, null, ex);
				}
				LocalUrlCacher.clearCache();
				clearCache.setEnabled(true);
			}
		});

		cacheUsage.setSelectedItem(LocalUrlCacher.getCacheUsage(LocalUrlCacher.getPreferredCacheUsage()));
		cacheUsage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalUrlCacher.setPreferredCacheUsage(((CacheUsage)cacheUsage.getSelectedItem()).usage);
			}
		});

		cachePanel.setLayout(layout);
		cachePanel.setBorder(new TitledBorder("Cache Settings"));
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		layout.linkSize(usageLabel, emptyLabel);

		layout.setHorizontalGroup(layout.createParallelGroup(LEADING)
				.addGroup(layout.createSequentialGroup()
					.addComponent(usageLabel)
					.addComponent(cacheUsage))
				.addGroup(layout.createSequentialGroup()
					.addComponent(emptyLabel)
					.addComponent(clearCache)));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(BASELINE)
					.addComponent(usageLabel)
					.addComponent(cacheUsage))
				.addGroup(layout.createParallelGroup(BASELINE)
					.addComponent(emptyLabel)
					.addComponent(clearCache)));

		return cachePanel;
	}

	private static boolean loadSynonymFile(JRPTextField synonymFile) {
		File file = new File(synonymFile.getText());

		if (!file.isFile() || !file.canRead()) { return false; }

		FileInputStream fis = null;
		try {
			synonymFile.setText(file.getCanonicalPath());
			fis = new FileInputStream(file);
			SynonymLookup.getDefaultLookup().loadSynonyms(fis);
		} catch (IOException ex) {
			return false;
		} finally {
			GeneralUtils.safeClose(fis);
		}

		return true;
	}

	@Override
	protected String getViewName() {
		return "Data Sources";
	}

	@Override
	protected String getToolTip() {
		return "Edit data sources and preferences";
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
		openDir = new JRPButton("DataLoadPrefsView_openDir", "\u2026");

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
	protected void setSize(GroupLayout layout, JRPTextField name) {
		layout.linkSize(nameLabel, urlLabel, typeLabel);
		name.setPreferredSize(new Dimension(300, name.getPreferredSize().height));
		layout.linkSize(name, type);
		layout.linkSize(SwingConstants.VERTICAL, name, type, url);
	}
}
