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

import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.prefs.IPrefEditorComponent;
import com.affymetrix.igb.prefs.SourceTableModel;
import com.affymetrix.igb.prefs.SourceTableModel.SourceColumn;
import com.affymetrix.igb.util.UnibrowPrefsUtil;

import com.affymetrix.igb.view.load.GeneralLoadView;

import com.affymetrix.swing.BooleanTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;


/**
 *
 * @version $Id$
 */
public final class DataLoadPrefsView extends JPanel implements IPrefEditorComponent {
	private static final long serialVersionUID = -2897919705543641511l;

	private static final String PREF_SYN_FILE_URL = "Synonyms File URL";
	private static Map<String, Integer> cache_usage_options;
	private static Map<Integer, String> usage2str;
	private JComboBox cache_usage_selector;
	private static final String AddServerTitle = "Add Server";
	private final GeneralLoadView glv;
	private SourceTableModel sourceTableModel = null;
	private JButton removeServerButton = null;
	private JTextField serverLoginTF = null;
	private JLabel serverLoginLabel = null;
	private JPasswordField serverPasswordTF = null;
	private JLabel serverPasswordLabel = null;

	static {
		String norm = "Normal Usage";
		String ignore = "Ignore Cache";
		String only = "Use Only Cache";
		Integer normal = new Integer(LocalUrlCacher.NORMAL_CACHE);
		Integer ignore_cache = new Integer(LocalUrlCacher.IGNORE_CACHE);
		Integer cache_only = new Integer(LocalUrlCacher.ONLY_CACHE);
		cache_usage_options = new LinkedHashMap<String, Integer>();
		cache_usage_options.put(norm, normal);
		cache_usage_options.put(ignore, ignore_cache);
		cache_usage_options.put(only, cache_only);
		usage2str = new LinkedHashMap<Integer, String>();
		usage2str.put(normal, norm);
		usage2str.put(ignore_cache, ignore);
		usage2str.put(cache_only, only);
	}

	public DataLoadPrefsView(GeneralLoadView glv) {
		this.glv = glv;
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.setBorder(BorderFactory.createEtchedBorder());

		synonymsBox();

		addServerBox();
		addSourcesBox();

		cacheBox();

		this.add(Box.createVerticalGlue());
	}

	private File performChoose(int FileSelectionMode) throws HeadlessException {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
		chooser.setFileSelectionMode(FileSelectionMode);
		if (FileSelectionMode == JFileChooser.DIRECTORIES_ONLY) {
			chooser.setDialogTitle("Choose Directory");
		} else {
			chooser.setDialogTitle("Choose File");
		}
		chooser.setAcceptAllFileFilterUsed(FileSelectionMode != JFileChooser.DIRECTORIES_ONLY);
		chooser.rescanCurrentDirectory();
		int option = chooser.showOpenDialog(DataLoadPrefsView.this);
		if (option == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile();
		}
		return null;
	}

	private void synonymsBox() {
		Box syn_box = Box.createVerticalBox();
		syn_box.setBorder(new javax.swing.border.TitledBorder("Add Personal Synonyms File"));
		final JTextField syn_file_TF = UnibrowPrefsUtil.createTextField(UnibrowPrefsUtil.getLocationsNode(), PREF_SYN_FILE_URL, "");
		syn_file_TF.setMaximumSize(new Dimension(syn_file_TF.getMaximumSize().width, syn_file_TF.getPreferredSize().height));
		syn_file_TF.setAlignmentX(0.0f);
		syn_box.add(syn_file_TF);

		Box syn_box_line2 = new Box(BoxLayout.X_AXIS);
		syn_box_line2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		ActionListener browse_for_syn_file_al = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				File f = performChoose(JFileChooser.FILES_AND_DIRECTORIES);
				if (f != null) {
					UnibrowPrefsUtil.getLocationsNode().put(PREF_SYN_FILE_URL, f.getPath());
				}
			}
		};


		JButton browse_for_syn_fileB = new JButton("Browse");
		browse_for_syn_fileB.addActionListener(browse_for_syn_file_al);
		syn_box_line2.add(browse_for_syn_fileB);

		syn_box_line2.add(Box.createRigidArea(new Dimension(10, 0)));

		JButton load_synonymsB = new JButton("Load Synonyms");
		syn_box_line2.add(load_synonymsB);
		syn_box_line2.setAlignmentX(0.0f);
		syn_box.add(syn_box_line2);

		syn_box.setAlignmentX(0.0f);

		this.add(syn_box);
		this.add(Box.createRigidArea(new Dimension(0, 5)));

		load_synonymsB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				processSynFile(syn_file_TF.getText());
			}
		});

		// Load the personal synonyms file, if one is specified.
		// It isn't crucial that this be run with SwingUtilities, but it doesn't hurt.
		Runnable r = new Runnable() {

			public void run() {
				String second_synonym_path = UnibrowPrefsUtil.getLocationsNode().get(PREF_SYN_FILE_URL, "");
				if (second_synonym_path.trim().length() == 0) {
					return;
				}
				File f = new File(second_synonym_path);
				if (!f.exists()) {
					return;
				}

				System.out.println("Loading personal synonyms from: " + second_synonym_path);
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(f);
					SynonymLookup.getDefaultLookup().loadSynonyms(fis);
				} catch (IOException ioe) {
					System.out.println("Error trying to read synonyms from: " + second_synonym_path);
					System.out.println("" + ioe.toString());
				} finally {
					GeneralUtils.safeClose(fis);
				}
			}
		};

		SwingUtilities.invokeLater(r);

	}

	private void processSynFile(String path) {
		UnibrowPrefsUtil.getLocationsNode().put(PREF_SYN_FILE_URL, path);
		File f = new File(path);
		if (!f.exists()) {
			ErrorHandler.errorPanel("File Not Found",
					"Synonyms file not found at the specified path\n" + path, this);
			return;
		}
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			SynonymLookup.getDefaultLookup().loadSynonyms(fis);

			JOptionPane.showMessageDialog(this, "Loaded synonyms from: " + f.getName(),
					"Loaded Synonyms", JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException ioe) {
			ErrorHandler.errorPanel("ERROR",
					"Exception while reading from file\n" + path,
					this, ioe);
		} finally {
			GeneralUtils.safeClose(fis);
		}
	}

	@SuppressWarnings("fallthrough")
	private void addSourcesBox() {

		final Box sourceBox = Box.createVerticalBox();
		sourceBox.setBorder(new TitledBorder("Current Servers       (To edit, double-click on cell)"));



		sourceTableModel = new SourceTableModel();
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
				case Password:
					JPasswordField password = new JPasswordField();
					password.setBorder(new LineBorder(Color.BLACK));
					column.setCellEditor(new DefaultCellEditor(password));

					column.setPreferredWidth(50);
					break;
				default:
					column.setPreferredWidth(50);
					break;
			}
		}

		table.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {

					public void valueChanged(ListSelectionEvent event) {
						int viewRow = table.getSelectedRow();
						if (viewRow >= 0 && ServerList.inServerPrefs((String)table.getValueAt(viewRow, SourceColumn.URL.ordinal()))) {
							removeServerButton.setEnabled(true);
						} else {
							removeServerButton.setEnabled(false);
						}
					}
				});


		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		sourceBox.add(scrollPane);

		sourceBox.add(Box.createRigidArea(new Dimension(0, 5)));

		ActionListener remove_server_entry = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Object url = table.getModel().getValueAt(table.getSelectedRow(), SourceColumn.URL.ordinal());
				Object serverType = table.getModel().getValueAt(table.getSelectedRow(), SourceColumn.Type.ordinal());
				Object serverName = table.getModel().getValueAt(table.getSelectedRow(), SourceColumn.Name.ordinal());

				removePreference(url.toString(), serverType.toString(), serverName.toString());
			}
		};

		Box buttonBox = Box.createHorizontalBox();
		buttonBox.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		buttonBox.add(Box.createRigidArea(new Dimension(10, 0)));
		removeServerButton = new JButton("Remove");
		removeServerButton.setEnabled(false);
		removeServerButton.addActionListener(remove_server_entry);
		removeServerButton.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		buttonBox.add(removeServerButton);
		sourceBox.add(buttonBox);



		this.add(sourceBox);
	}

	private void addServerBox() {

		final Box serverBox = Box.createVerticalBox();
		final TitledBorder tBorder = new TitledBorder(AddServerTitle);
		serverBox.setBorder(tBorder);

		// ServerBox1
		final Box serverBox1 = Box.createHorizontalBox();
		serverBox1.setAlignmentX(0.0f);
		final JTextField serverTF = new JTextField("", 50);
		serverTF.setMaximumSize(new Dimension(serverTF.getPreferredSize()));
		serverTF.setAlignmentX(Component.CENTER_ALIGNMENT);
		final JLabel serverLabel = new JLabel("Server URL");
		serverLabel.setPreferredSize(new Dimension(80, serverLabel.getPreferredSize().height));
		serverLabel.setMaximumSize(new Dimension(80, serverLabel.getPreferredSize().height));
		serverLabel.setMinimumSize(new Dimension(80, serverLabel.getPreferredSize().height));
		serverLabel.setAlignmentX(0.0f);
		serverLabel.setLabelFor(serverTF);
		serverBox1.add(serverLabel);
		serverBox1.add(serverTF);
		serverBox1.add(Box.createRigidArea(new Dimension(10, 0)));
		final JButton browse_for_QuickLoad_fileB = new JButton("Local Directory");
		browse_for_QuickLoad_fileB.setToolTipText("Specify local directory for " + ServerType.QuickLoad.toString() + " loading.");
		serverBox1.add(browse_for_QuickLoad_fileB);

		int preferredWidth = serverTF.getMaximumSize().width + serverLabel.getMaximumSize().width + 10 + browse_for_QuickLoad_fileB.getMaximumSize().width;
		int preferredHeight = serverBox1.getPreferredSize().height;

		serverBox1.setPreferredSize(new Dimension(preferredWidth, preferredHeight));

		// ServerBox2
		final Box serverBox2 = Box.createHorizontalBox();
		serverBox2.setAlignmentX(0.0f);
		final JTextField serverNameTF = new JTextField("", 50);
		serverNameTF.setMaximumSize(new Dimension(serverTF.getPreferredSize()));
		serverNameTF.setAlignmentX(Component.CENTER_ALIGNMENT);
		final JLabel serverNameLabel = new JLabel("Server name");
		serverNameLabel.setPreferredSize(new Dimension(80, serverNameLabel.getPreferredSize().height));
		serverNameLabel.setMaximumSize(new Dimension(80, serverNameLabel.getPreferredSize().height));
		serverNameLabel.setMinimumSize(new Dimension(80, serverNameLabel.getPreferredSize().height));
		serverNameLabel.setAlignmentX(0.0f);
		serverNameLabel.setLabelFor(serverNameTF);
		serverBox2.add(serverNameLabel);
		serverBox2.add(serverNameTF);



		// ServerBox3
		final Box serverBox3 = Box.createHorizontalBox();
		serverBox3.setAlignmentX(0.0f);
		final Vector<String> serverTypes = new Vector<String>(3);
		serverTypes.add(ServerType.QuickLoad.toString());
		serverTypes.add(ServerType.DAS2.toString());
		serverTypes.add(ServerType.DAS.toString());
		final JComboBox serverTypeCB = new JComboBox(serverTypes);
		serverTypeCB.setPreferredSize(new Dimension(100, serverTypeCB.getPreferredSize().height));
		serverTypeCB.setMaximumSize(new Dimension(100, serverTypeCB.getPreferredSize().height));
		serverTypeCB.setMinimumSize(new Dimension(100, serverTypeCB.getPreferredSize().height));
		serverTypeCB.setAlignmentX(Component.CENTER_ALIGNMENT);
		final JLabel serverTypeLabel = new JLabel("Server type ");
		serverTypeLabel.setPreferredSize(new Dimension(80, serverTypeLabel.getPreferredSize().height));
		serverTypeLabel.setMaximumSize(new Dimension(80, serverTypeLabel.getPreferredSize().height));
		serverTypeLabel.setMinimumSize(new Dimension(80, serverTypeLabel.getPreferredSize().height));
		serverTypeLabel.setAlignmentX(0.0f);
		serverTypeLabel.setLabelFor(serverTypeCB);
		serverBox3.add(serverTypeLabel);
		serverBox3.add(serverTypeCB);


		// ServerBox for login
		final Box serverBoxLogin = Box.createHorizontalBox();
		serverBoxLogin.setAlignmentX(0.0f);
		serverLoginTF = new JTextField("", 20);
		serverLoginTF.setMaximumSize(new Dimension(serverLoginTF.getPreferredSize()));
		serverLoginTF.setAlignmentX(Component.CENTER_ALIGNMENT);
		serverLoginTF.setEnabled(false);
		serverLoginTF.setToolTipText("login (only used by secure DAS/2 servers");
		serverLoginLabel = new JLabel("Login");
		serverLoginLabel.setPreferredSize(new Dimension(80, serverLoginLabel.getPreferredSize().height));
		serverLoginLabel.setMaximumSize(new Dimension(80, serverLoginLabel.getPreferredSize().height));
		serverLoginLabel.setMinimumSize(new Dimension(80, serverLoginLabel.getPreferredSize().height));
		serverLoginLabel.setAlignmentX(0.0f);
		serverLoginLabel.setLabelFor(serverLoginTF);
		serverLoginLabel.setEnabled(false);
		serverLoginLabel.setToolTipText("login (only used by secure DAS/2 servers");
		serverBoxLogin.add(serverLoginLabel);
		serverBoxLogin.add(serverLoginTF);

		// ServerBox for password
		final Box serverBoxPassword = Box.createHorizontalBox();
		serverBoxPassword.setAlignmentX(0.0f);
		serverPasswordTF = new JPasswordField("", 20);
		serverPasswordTF.setMaximumSize(new Dimension(serverPasswordTF.getPreferredSize()));
		serverPasswordTF.setAlignmentX(Component.CENTER_ALIGNMENT);
		serverPasswordTF.setEnabled(false);
		serverPasswordTF.setToolTipText("password (only used by secure DAS/2 servers");
		serverPasswordLabel = new JLabel("Password");
		serverPasswordLabel.setPreferredSize(new Dimension(80, serverPasswordLabel.getPreferredSize().height));
		serverPasswordLabel.setMaximumSize(new Dimension(80, serverPasswordLabel.getPreferredSize().height));
		serverPasswordLabel.setMinimumSize(new Dimension(80, serverPasswordLabel.getPreferredSize().height));
		serverPasswordLabel.setAlignmentX(0.0f);
		serverPasswordLabel.setLabelFor(serverPasswordTF);
		serverPasswordLabel.setEnabled(false);
		serverPasswordLabel.setToolTipText("password (only used by secure DAS/2 servers");
		serverBoxPassword.add(serverPasswordLabel);
		serverBoxPassword.add(serverPasswordTF);



		// ServerBox4
		final JButton addServerB = new JButton("Add");
		addServerB.setAlignmentX(0.0f);
		final Box serverBox4 = new Box(BoxLayout.X_AXIS);
		serverBox4.setAlignmentX(0.0f);
		serverBox4.add(Box.createRigidArea(new Dimension(10, 0)));
		serverBox4.add(addServerB);
		serverBox.add(serverBox1);
		serverBox.add(serverBox2);
		serverBox.add(serverBox3);
		serverBox.add(serverBoxLogin);
		serverBox.add(serverBoxPassword);
		serverBox.add(serverBox4);
		serverBox.setAlignmentX(0.0f);
		this.add(serverBox);

		//this.add(Box.createRigidArea(new Dimension(0, 5)));

		// Make the label self-explanatory
		serverTypeCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				String serverType = (String) serverTypeCB.getSelectedItem();
				browse_for_QuickLoad_fileB.setEnabled(serverType.equals(ServerType.QuickLoad.toString()));
				serverLoginTF.setEnabled(serverType.equals(ServerType.DAS2.toString()));
				serverLoginLabel.setEnabled(serverType.equals(ServerType.DAS2.toString()));
				serverPasswordTF.setEnabled(serverType.equals(ServerType.DAS2.toString()));
				serverPasswordLabel.setEnabled(serverType.equals(ServerType.DAS2.toString()));
			}
		});
		addServerB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// Convert password into clear text
                String passwordText = new String("");
                for (int i = 0; i < serverPasswordTF.getPassword().length; i++)
                {
                	passwordText += serverPasswordTF.getPassword()[i];
                }
                
				// Add the server to IGB
				addServer(serverTF.getText().trim(),
						(String) serverTypeCB.getSelectedItem(),
						serverNameTF.getText(),
						serverLoginTF.getText(),
						passwordText);

				// New clear out the server fields in the add panel
				serverTF.setText("");
				serverNameTF.setText("");
				serverLoginTF.setText("");
				serverPasswordTF.setText("");
			}
		});

		browse_for_QuickLoad_fileB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				File f = performChoose(JFileChooser.DIRECTORIES_ONLY);
				if (f != null && f.isDirectory()) {
					serverTF.setText("file://" + f.getAbsolutePath());
				}
			}
		});
	}

	private void addServer(String DirectoryOrURL, String serverTypeStr, String serverName, String login, String password) {
		if (serverName == null || serverName.length() == 0) {
			ErrorHandler.errorPanel("Blank server name", "Server name must be specified", this);
			return;
		}
		ServerType serverType = ServerType.valueOf(serverTypeStr);
		if (serverType.equals(ServerType.QuickLoad)) {
			File f = new File(DirectoryOrURL);
			if (f.isDirectory()) {
				try {
					URL url = new URL(f.toString());
					addToPreferences(url.toString(), serverType, serverName, login, password);
				} catch (MalformedURLException ex) {
					String errorTitle = "Invalid URL" + (serverType.equals(ServerType.QuickLoad) ? "/Directory" : "");
					String errorMessage = "'file://" + f + "' is not a valid " + ServerType.QuickLoad.toString() + " directory";
					ErrorHandler.errorPanel(errorTitle, errorMessage, this);
					return;
				}
				// file exists -- add to preferences
				return;
			}
		}
		try {
			// it's a URL, hopefully
			URL url = new URL(DirectoryOrURL);
			addToPreferences(DirectoryOrURL, serverType, serverName, login, password);
		} catch (MalformedURLException ex) {
			String errorTitle = "Invalid URL" + (serverType.equals(ServerType.QuickLoad) ? "/Directory" : "");
			String errorMessage =
					"'" + DirectoryOrURL + "' is not a valid URL" + (serverType.equals(ServerType.QuickLoad) ? " or directory" : "");
			ErrorHandler.errorPanel(errorTitle, errorMessage, this);
			return;
		}
	}

	/**
	 * Add the URL/Directory and server name to the preferences.
	 * @param DirectoryOrURL
	 * @param serverTypeStr
	 * @param serverName
	 */
	private void addToPreferences(String DirectoryOrURL, ServerType serverType, String serverName, String login, String password) {
		// Add to GeneralLoadView and validate
		if (!this.glv.addServer(serverName, DirectoryOrURL, serverType, login, password)) {
			ErrorHandler.errorPanel(
					"Error loading server",
					serverType + " server " + serverName + " at " + DirectoryOrURL + " was not successfully loaded.\nPlease check that directory/URL is valid, and you have a working network connection.");
			return;
		}

		sourceTableModel.init();
		removeServerButton.setEnabled(false);

		boolean authEnabled = (login != null && password != null) && !(login.isEmpty() || password.isEmpty());
		ServerList.addServerToPrefs(DirectoryOrURL, serverName, serverType, authEnabled, login, password, true);

		serverDialog(serverName, serverType);
	}

	private void removePreference(String DirectoryOrURL, String serverType, String serverName) {
		ServerList.removeServerFromPrefs(DirectoryOrURL);
		GeneralLoadView.removeServer(serverName, DirectoryOrURL, ServerType.valueOf(serverType));
		sourceTableModel.init();
	}

	private void serverDialog(String serverName, ServerType serverType) {
		JOptionPane.showMessageDialog(null,
				"The " + serverType + " server '" + serverName + "' has been added.  If you do not\n"
				+ "see the new server, please restart the application",
				"New server added",
				JOptionPane.INFORMATION_MESSAGE);
	}

	private void cacheBox() {
		/*cache_annotsCB = UnibrowPrefsUtil.createCheckBox("Cache Annotations",
		UnibrowPrefsUtil.getTopNode(),
		QuickLoadServerModel.PREF_QUICKLOAD_CACHE_ANNOTS,
		QuickLoadServerModel.CACHE_ANNOTS_DEFAULT);
		cache_residuesCB = UnibrowPrefsUtil.createCheckBox("Cache DNA Residues",
		UnibrowPrefsUtil.getTopNode(),
		QuickLoadServerModel.PREF_QUICKLOAD_CACHE_RESIDUES,
		QuickLoadServerModel.CACHE_RESIDUES_DEFAULT);*/
		JButton clear_cacheB = new JButton("Clear Cache");
		cache_usage_selector = new JComboBox();
		for (String str : cache_usage_options.keySet()) {
			cache_usage_selector.addItem(str);
		}
		cache_usage_selector.setSelectedItem(usage2str.get(new Integer(LocalUrlCacher.getPreferredCacheUsage())));
		Dimension d = new Dimension(cache_usage_selector.getPreferredSize());
		cache_usage_selector.setMaximumSize(d);
		JPanel cache_options_box = new JPanel();
		cache_options_box.setLayout(new BoxLayout(cache_options_box, BoxLayout.Y_AXIS));
		cache_options_box.setAlignmentX(0.0f);
		this.add(cache_options_box);
		//cache_annotsCB.setAlignmentX(0.0f);
		//cache_options_box.add(cache_annotsCB);
		//cache_residuesCB.setAlignmentX(0.0f);
		//cache_options_box.add(cache_residuesCB);
		JComponent usageP = Box.createHorizontalBox();
		usageP.add(Box.createRigidArea(new Dimension(5, 0)));
		usageP.add(new JLabel("Cache Usage"));
		usageP.add(Box.createRigidArea(new Dimension(5, 0)));
		usageP.add(cache_usage_selector);
		usageP.add(Box.createHorizontalGlue());
		usageP.setAlignmentX(0.0f);
		usageP.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
		cache_options_box.add(usageP);
		Box clear_cache_box = Box.createHorizontalBox();
		clear_cache_box.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
		//clear_cache_box.add(Box.createHorizontalGlue());
		clear_cache_box.add(Box.createRigidArea(new Dimension(5, 0)));
		clear_cache_box.add(clear_cacheB);
		clear_cache_box.add(Box.createHorizontalGlue());
		clear_cache_box.setAlignmentX(0.0f);
		cache_options_box.add(clear_cache_box);

		// the cache_usage_selector will probably go away later
		ItemListener cache_usage_al = new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				String usage_str = (String) cache_usage_selector.getSelectedItem();
				int usage = (DataLoadPrefsView.cache_usage_options.get(usage_str)).intValue();
				//QuickLoadServerModel.setCacheBehavior(usage, cache_annotsCB.isSelected(), cache_residuesCB.isSelected());
				LocalUrlCacher.setPreferredCacheUsage(usage);
				// UnibrowPrefsUtil.saveIntParam(LocalUrlCacher.PREF_CACHE_USAGE, usage);
			}
		};

		cache_usage_selector.addItemListener(cache_usage_al);

		ActionListener clear_cache_al = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				LocalUrlCacher.clearCache();
			}
		};


		clear_cacheB.addActionListener(clear_cache_al);
	}

	@Override
	public String getName() {
		return "Data Sources";
	}

	public void refresh() {
		// the checkboxes and text fields refresh themselves automatically....
		cache_usage_selector.setSelectedItem(usage2str.get(
				new Integer(LocalUrlCacher.getPreferredCacheUsage())));
	}

	public Icon getIcon() {
		return null;
	}

	public String getToolTip() {
		return "Edit data sources and preferences";
	}

	public String getHelpTextHTML() {
		StringBuffer sb = new StringBuffer();

		sb.append("<h1>" + this.getName() + "</h1>\n");
		sb.append("<p>\n");
		sb.append("This panel allows you to change settings for data sources.  ");
		sb.append("It is not necessary to restart the program for these changes to take effect.  ");
		sb.append("NOTE: Temporarily, the Add Server functionality may require a restart to take effect.");
		sb.append("</p>\n");

		sb.append("<p>\n");
		sb.append("<h2>Add Server</h2>\n");
		sb.append("Add an additional server for loading of genomes, sequences, etc.");
		sb.append("The user needs to specify if this is a Quickload, DAS, or DAS/2 server");
		sb.append("</p>\n");

		sb.append("<p>\n");
		sb.append("<h2>Add Personal Synonyms File</h2>\n");
		sb.append("The location of a synonyms file to use to help resolve cases where ");
		sb.append("different data files refer to the same genome or chromosome by different names. ");
		sb.append("For instance 'hg16' = 'ncbi.v34' and 'chrM' = 'chrMT' and 'chr1' = 'CHR1'. ");
		sb.append("This is simply a tab-delimited file where entries on the same row are all synonymous. ");
		sb.append("Synonyms will be <b>merged</b> from the servers, preference files, and the file listed here. ");
		sb.append("</p>\n");

		sb.append("<p>\n");
		sb.append("<h2>Cache</h2>\n");
		sb.append("IGB stores files downloaded over the network in a local cache. ");
		sb.append("Files loaded from a local filesystem or network filesystem are not cached. ");
		sb.append("We recommend that you leave the 'Cache Usage' setting on 'Normal' and ");
		sb.append("that you choose true for both 'Cache Annotations' and 'Cache DNA Residues'.");
		sb.append("If disk storage space is a problem, you can press the 'Clear Cache' button. ");
		sb.append("You may also choose to turn the cache off, though performance will degrade. ");
		sb.append("Some, but not all, users find it necessary to turn off the cache when they ");
		sb.append("are not connected to the internet.  For most users, this is not necessary as ");
		sb.append("long as the cache already contains a few essential files. ");
		sb.append("</p>\n");

		return sb.toString();
	}

	public String getInfoURL() {
		return null;
	}

}
