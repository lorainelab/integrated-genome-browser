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
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.prefs.IPrefEditorComponent;
import com.affymetrix.igb.prefs.SourceTableModel;
import com.affymetrix.igb.prefs.SourceTableModel.SourceColumn;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.genoviz.swing.BooleanTableCellRenderer;
import com.affymetrix.igb.util.IGBAuthenticator;
import com.affymetrix.igb.util.LocalUrlCacher;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.*;
import java.io.*;
import java.net.Authenticator;
import java.net.Authenticator.RequestorType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import static com.affymetrix.igb.util.LocalUrlCacher.CacheUsage;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static javax.swing.JFileChooser.FILES_AND_DIRECTORIES;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public final class DataLoadPrefsView extends IPrefEditorComponent {
	private static final long serialVersionUID = 2l;

	private static final String PREF_SYN_FILE_URL = "Synonyms File URL";

	private static final String[] OPTIONS = new String[]{"Add Server", "Cancel"};

	public DataLoadPrefsView() {
		final GroupLayout layout = new GroupLayout(this);
		final JPanel sourcePanel = initSourcePanel();
		final JPanel synonymsPanel = initSynonymsPanel(this);
		final JPanel cachePanel = initCachePanel();

		this.setName("Data Sources");
		this.setToolTipText("Edit data sources and preferences");

		this.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(sourcePanel)
				.addComponent(synonymsPanel)
				.addComponent(cachePanel));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(sourcePanel)
				.addComponent(synonymsPanel)
				.addComponent(cachePanel));
	}

	private static JPanel initSourcePanel() {
		final JPanel sourcePanel = new JPanel();
		final GroupLayout layout = new GroupLayout(sourcePanel);
		final SourceTableModel sourceTableModel = new SourceTableModel();
		final JTable sourcesTable = createSourcesTable(sourceTableModel);
		final JScrollPane sourcesScrollPane = new JScrollPane(sourcesTable);

		sourcePanel.setLayout(layout);
		sourcePanel.setBorder(new TitledBorder("Data Sources"));
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		final JButton addServerButton = createButton("Add\u2026", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showAddSourceDialog();
				sourceTableModel.init();
			}
		});

		final JButton removeServerButton = createButton("Remove", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object url = sourcesTable.getModel().getValueAt(sourcesTable.getSelectedRow(), SourceColumn.URL.ordinal());

				removeDataSource(url.toString());
				sourceTableModel.init();
			}
		});
		removeServerButton.setEnabled(false);

		final JButton editAuthButton = createButton("Authentication\u2026", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object url = sourcesTable.getModel().getValueAt(sourcesTable.getSelectedRow(), SourceColumn.URL.ordinal());
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
					Logger.getLogger(DataLoadPrefsView.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});
		editAuthButton.setEnabled(false);

		sourcesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				int viewRow = sourcesTable.getSelectedRow();
				if (viewRow >= 0 && ServerList.inServerPrefs((String)sourcesTable.getValueAt(viewRow, SourceColumn.URL.ordinal()))) {
					removeServerButton.setEnabled(true);
					editAuthButton.setEnabled(true);
				} else {
					removeServerButton.setEnabled(false);
					editAuthButton.setEnabled(false);
				}
			}
		});

		layout.setHorizontalGroup(layout.createParallelGroup(TRAILING)
				.addComponent(sourcesScrollPane)
				.addGroup(layout.createSequentialGroup()
					.addComponent(addServerButton)
					.addComponent(editAuthButton)
					.addComponent(removeServerButton)));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(sourcesScrollPane)
				.addGroup(layout.createParallelGroup(BASELINE)
					.addComponent(addServerButton)
					.addComponent(editAuthButton)
					.addComponent(removeServerButton)));

		return sourcePanel;
	}

	private static JPanel initSynonymsPanel(final JPanel parent) {
		final JPanel synonymsPanel = new JPanel();
		final GroupLayout layout = new GroupLayout(synonymsPanel);
		final JLabel synonymsLabel= new JLabel("Synonyms File");
		final JTextField synonymFile = UnibrowPrefsUtil.createTextField(UnibrowPrefsUtil.getLocationsNode(), PREF_SYN_FILE_URL, "");
		final JButton openFile = createButton("\u2026", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File f = fileChooser(FILES_AND_DIRECTORIES, parent);
				try {
					if (f != null) {
						synonymFile.setText(f.getCanonicalPath());
						addSynonymFile(synonymFile.getText());
					}
				} catch (IOException ex) {
					Logger.getLogger(DataLoadPrefsView.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});

		synonymFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addSynonymFile(synonymFile.getText());
			}
		});

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

		return synonymsPanel;
	}

	private static JPanel initCachePanel() {
		final JPanel cachePanel = new JPanel();
		final GroupLayout layout = new GroupLayout(cachePanel);
		final JLabel usageLabel = new JLabel("Caching Mode");
		final JComboBox	cacheUsage = new JComboBox(CacheUsage.values());
		final JButton clearCache = createButton("Clear Cache", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalUrlCacher.clearCache();
			}
		});

		cacheUsage.setSelectedItem(LocalUrlCacher.getCacheUsage(LocalUrlCacher.getPreferredCacheUsage()));
		cacheUsage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalUrlCacher.setPreferredCacheUsage(((CacheUsage)cacheUsage.getSelectedItem()).usage);
			}
		});

		cachePanel.setLayout(layout);
		cachePanel.setBorder(new TitledBorder("Cache Setttings"));
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addComponent(usageLabel)
				.addComponent(cacheUsage)
				.addComponent(clearCache));

		layout.setVerticalGroup(layout.createParallelGroup(BASELINE)
				.addComponent(usageLabel)
				.addComponent(cacheUsage)
				.addComponent(clearCache));

		return cachePanel;
	}

	private static JTable createSourcesTable(SourceTableModel sourceTableModel) {
		//sourceTableModel = new SourceTableModel();
		final JTable table = new JTable(sourceTableModel);

		table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
		table.setDefaultRenderer(String.class,  new DefaultTableCellRenderer() {
			private static final long serialVersionUID = -5433598077871623855l;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int row, int col) {

				this.setEnabled((Boolean) table.getModel().getValueAt(row, SourceColumn.Enabled.ordinal()));
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
			}
		});

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

	private static JPanel createAddSourceDialog(final JTextField name, final JTextField url, final JComboBox  type) {
		JPanel messageContainer = new JPanel();

		final JPanel addServerPanel = new JPanel();
		final JLabel nameLabel = new JLabel("Name");
		final JLabel urlLabel = new JLabel("URL");
		final JLabel typeLabel = new JLabel("Type");
		final JButton openDir = new JButton("\u2026");

		openDir.setToolTipText("Open Local Directory");

		final GroupLayout layout = new GroupLayout(addServerPanel);

		addServerPanel.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		layout.linkSize(nameLabel, urlLabel, typeLabel);
		name.setPreferredSize(new Dimension(300, name.getPreferredSize().height));
		layout.linkSize(name, type);

		layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(messageContainer)
				.addGroup(layout.createSequentialGroup()
					.addComponent(nameLabel)
					.addComponent(name))
				.addGroup(layout.createSequentialGroup()
					.addComponent(urlLabel)
					.addComponent(url)
					.addComponent(openDir))
				.addGroup(layout.createSequentialGroup()
					.addComponent(typeLabel)
					.addComponent(type)));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(messageContainer)
				.addGroup(layout.createParallelGroup(BASELINE)
					.addComponent(nameLabel)
					.addComponent(name))
				.addGroup(layout.createParallelGroup(BASELINE)
					.addComponent(urlLabel)
					.addComponent(url)
					.addComponent(openDir))
				.addGroup(layout.createParallelGroup(BASELINE)
					.addComponent(typeLabel)
					.addComponent(type)));

		messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));

		openDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File f = fileChooser(DIRECTORIES_ONLY, addServerPanel);
				if (f != null && f.isDirectory()) {
					try {
						url.setText(f.toURI().toURL().toString());
					} catch (MalformedURLException ex) {
						Logger.getLogger(DataLoadPrefsView.class.getName()).log(Level.WARNING, "Unable to convert File '" + f.getName() + "' to URL", ex);
					}
				}
			}
		});

		return addServerPanel;
	}

	private static void showAddSourceDialog() {
		JTextField name = new JTextField();
		JTextField url = new JTextField();
		JComboBox  type = new JComboBox(LoadUtils.ServerType.values());

		int result = JOptionPane.showOptionDialog(
				null,
				createAddSourceDialog(name, url, type),
				"Add Data Source",
				OK_CANCEL_OPTION,
				PLAIN_MESSAGE,
				null,
				OPTIONS,
				OPTIONS[0]);

		if (result == OK_OPTION) {
			addDataSource(url.getText(), (ServerType)type.getSelectedItem(), name.getText());
		}
	}

	/**
	 * Add the URL/Directory and server name to the preferences.
	 * @param url
	 * @param type
	 * @param name
	 */
	private static void addDataSource(String url, ServerType type, String name) {
		if (url == null || url.isEmpty() || name == null || name.isEmpty()) {
			return;
		}
		
		GenericServer server = ServerList.addServer(type, name, url);

		if (server == null) {
			ErrorHandler.errorPanel(
					"Unable to Load Data Source",
					"Unable to load " + type + " data source '" + url + "'.");
			return;
		}

		ServerList.addServerToPrefs(server);
	}

	private static void removeDataSource(String url) {
		if (ServerList.getServer(url) == null) {
			Logger.getLogger(DataLoadPrefsView.class.getName()).log(Level.SEVERE, "Can not remove Server '" + url +"': it does not exist in ServerList");
			return;
		}

		ServerList.removeServerFromPrefs(url);
		ServerList.removeServer(url);
	}

	private static void addSynonymFile(String file) {
		if (file == null || file.isEmpty()) {
			UnibrowPrefsUtil.getLocationsNode().put(PREF_SYN_FILE_URL, "");
			return;
		}
		
		File f = new File(file);
		if (!f.isFile()) {
			ErrorHandler.errorPanel(
					"File not Found",
					"The personal synonyms file '" + file + "' does not exist.");
			return;
		}

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			SynonymLookup.getDefaultLookup().loadSynonyms(fis);
			UnibrowPrefsUtil.getLocationsNode().put(PREF_SYN_FILE_URL, f.getCanonicalPath());
		} catch (IOException ex) {
			ErrorHandler.errorPanel(
					"Unable to Load Synonym File",
					"Unable to load the personal synonym file '" + file + "'.",
					ex);
		} finally {
			GeneralUtils.safeClose(fis);
		}
	}
	
	private static JButton createButton(String name, ActionListener listener) {
		final JButton button = new JButton(name);

		button.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		button.addActionListener(listener);

		return button;
	}

	private static File fileChooser(int mode, Component parent) throws HeadlessException {
		JFileChooser chooser = new JFileChooser();
		int option;
		
		chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
		chooser.setFileSelectionMode(mode);
		chooser.setDialogTitle("Choose " + (mode == DIRECTORIES_ONLY ? "Directory" : "File"));
		chooser.setAcceptAllFileFilterUsed(mode != DIRECTORIES_ONLY);
		chooser.rescanCurrentDirectory();
		
		option = chooser.showOpenDialog(parent);
		
		if (option != APPROVE_OPTION) { return null; }

		return chooser.getSelectedFile();
	}

	public void refresh() { }
}
