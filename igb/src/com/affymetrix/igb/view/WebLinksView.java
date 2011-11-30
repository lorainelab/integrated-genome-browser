package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.genometryImpl.util.PreferenceUtils;

import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.igb.shared.FileTracker;
import java.util.List;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @modified by nick
 */
public final class WebLinksView implements ListSelectionListener {

	private static final long serialVersionUID = 1L;
	private static WebLinksView singleton;
	public JTable sysTable;
	public JTable localTable;
	public WebLinksTableModel sysModel;
	public WebLinksTableModel localModel;
	public ListSelectionModel lsm;
	private static JFileChooser static_chooser = null;
	public static final String LINK_NAME = "Name";
	public static final String URL = "URL";
	public final static String[] col_headings = {
		LINK_NAME,
		URL
	};
	public static final int COL_LINK_NAME = 0;
	public static final int COL_URL = 1;
	public static final int COL_REGEX = 2;
	public int[] selectedRows;
	public boolean initializationDetector; //Test to detect action events triggered by clicking a row in the table.
	public boolean settingValueFromTable;  //Test to prevent action events triggered by the setValueAt method from calling the method again.  This improves efficiency.
	public JTextField nameTextField;
	public JTextField urlTextField;
	public JTextField regexTextField;
	public JRadioButton allTiersRadioButton;
	public JRadioButton matchTierRadioButton;
	private final ButtonGroup button_group = new ButtonGroup();
	private final WebLinkEditorPanel edit_panel;
	private int previousSelectedRow;
	private String previousName;
	private String previousUrl;
	private String previousRegex;

	// initialize the static_panel early, because this will cause the
	// accelerator
	// key-strokes to be configured early through the PreferenceUtils and thus
	// for them to be visible in the KeyStrokesView
	public static synchronized WebLinksView getSingleton() {
		if (singleton == null) {
			singleton = new WebLinksView();
		}
		return singleton;
	}

	private WebLinksView() {
		super();

		sysTable = new JTable();
		localTable = new JTable();

		sysModel = new WebLinksTableModel();
		sysModel.addTableModelListener(new javax.swing.event.TableModelListener() {

			public void tableChanged(javax.swing.event.TableModelEvent e) {
				// do nothing.
			}
		});

		localModel = new WebLinksTableModel();
		localModel.addTableModelListener(new javax.swing.event.TableModelListener() {

			public void tableChanged(javax.swing.event.TableModelEvent e) {
				// do nothing.
			}
		});

		initTable(sysTable);
		initTable(localTable);

		nameTextField = new JTextField();
		urlTextField = new JTextField();
		regexTextField = new JTextField();
		allTiersRadioButton = new JRadioButton();
		matchTierRadioButton = new JRadioButton();
		button_group.add(allTiersRadioButton);
		button_group.add(matchTierRadioButton);
		allTiersRadioButton.setSelected(true);
		regexTextField.setText("Display link for all tiers (uneditable)");
		regexTextField.setEnabled(false);
		matchTierRadioButton.setSelected(false);
		edit_panel = new WebLinkEditorPanel();
	}

	private void initTable(JTable table) {
		lsm = table.getSelectionModel();
		lsm.addListSelectionListener(this);
		lsm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		if (table.equals(localTable)) {
			table.setModel(localModel);
			localModel.setLinks(WebLink.getLocalWebList());
		} else {
			table.setModel(sysModel);
			sysModel.setLinks(WebLink.getSysWebList());
		}

		table.setRowSelectionAllowed(true);
		table.getColumnModel().getColumn(COL_LINK_NAME).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_LINK_NAME).setMaxWidth(400);

		Font f = new Font("SansSerif", Font.BOLD, 12);
		table.getTableHeader().setFont(f);
	}

	private static void setAccelerator(Action a) {
		KeyStroke ks = PreferenceUtils.getAccelerator("Web Links Manager / "
				+ a.getValue(Action.NAME));
		a.putValue(Action.ACCELERATOR_KEY, ks);
	}

	public void localDelete() throws HeadlessException {
		if (localTable.isFocusOwner()) {
			selectedRows = localTable.getSelectedRows();
			Container frame = SwingUtilities.getAncestorOfClass(JFrame.class, null);

			int yes = JOptionPane.showConfirmDialog(frame, "Delete these "
					+ selectedRows.length + " selected link(s)?", "Delete?",
					JOptionPane.YES_NO_OPTION);

			if (yes == JOptionPane.YES_OPTION) {
				for (int i : selectedRows) {
					WebLink link = localModel.webLinks.get(i);
					WebLink.removeWebLink(link);
				}
			}

			refreshList();
		}
	}

	public void localAdd() {
		WebLink link = new WebLink();
		edit_panel.setWebLink(link);
		boolean ok = edit_panel.showDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, null));
		if (ok) {
			edit_panel.setLinkPropertiesFromGUI();
			link.setType("local");
			WebLink.addWebLink(link);
		}

		refreshList();
	}

	private static boolean isEmpty(String s) {
		return (s == null || s.trim().length() == 0);
	}

	private void refreshList() {
		previousSelectedRow = localTable.getSelectedRow();
		localModel.setLinks(WebLink.getLocalWebList());
		localModel.fireTableDataChanged();
		if (localTable.getRowCount() > 0 && previousSelectedRow < localTable.getRowCount()
				&& previousSelectedRow != -1) {
			localTable.setRowSelectionInterval(previousSelectedRow, previousSelectedRow);
		}
	}

	public void nameTextField() {
		if (!settingValueFromTable) {
			if (isEmpty(nameTextField.getText())) {
				ErrorHandler.errorPanel("The name cannot be blank");
				nameTextField.setText(previousName);
				nameTextField.grabFocus();
			} else {
				localModel.setValueAt(nameTextField.getText(), selectedRows[0], COL_LINK_NAME);
			}
		}
	}

	public void urlTextField() {
		if (!settingValueFromTable) {
			if (isEmpty(urlTextField.getText())) {
				ErrorHandler.errorPanel("The URL cannot be blank");
				urlTextField.setText(previousUrl);
				urlTextField.grabFocus();
			} else {
				try {
					new URL(urlTextField.getText());
					localModel.setValueAt(urlTextField.getText(), selectedRows[0], COL_URL);
				} catch (MalformedURLException e) {
					ErrorHandler.errorPanel("Malformed URL",
							"The given URL appears to be invalid.\n" + e.getMessage(),
							urlTextField);
					urlTextField.setText(previousUrl);
					urlTextField.grabFocus();
				}
			}
		}
	}

	public void regexTextField() {
		if (!settingValueFromTable) {
			if (matchTierRadioButton.isSelected()) {
				try {
					Pattern.compile(regexTextField.getText());
					localModel.setValueAt(regexTextField.getText(), selectedRows[0], COL_REGEX);
				} catch (PatternSyntaxException pse) {
					ErrorHandler.errorPanel("Bad Regular Expression",
							"Error in regular expression:\n" + pse.getMessage(), regexTextField);
					regexTextField.setText(previousRegex);
					regexTextField.grabFocus();
				}
			}
		}
	}

	/** Gets a static re-usable file chooser that prefers "html" files. */
	private static JFileChooser getJFileChooser() {
		if (static_chooser == null) {
			static_chooser = UniFileChooser.getFileChooser("XML file", "xml");
			static_chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
		}
		static_chooser.rescanCurrentDirectory();
		return static_chooser;
	}

	/**
	 * Tries to import weblinks.
	 */
	public void importWebLinks() {
		JFileChooser chooser = getJFileChooser();
		chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
		Container frame = SwingUtilities.getAncestorOfClass(JFrame.class, null);
		int option = chooser.showOpenDialog(frame);
		if (option == JFileChooser.APPROVE_OPTION) {
			FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
			File fil = chooser.getSelectedFile();
			try {
				WebLink.importWebLinks(fil);
			} catch (FileNotFoundException fe) {
				ErrorHandler.errorPanel(
						"Error",
						"Error importing web links: File Not Found "
						+ fil.getAbsolutePath(), null, fe);
			} catch (Exception ex) {
				ErrorHandler.errorPanel("Error", "Error importing web links",
						null, ex);
			}
		}

		refreshList();
	}

	public void exportWebLinks() {
		Container frame = SwingUtilities.getAncestorOfClass(JFrame.class, null);
		
		if (localTable.getRowCount() == 0) {
			ErrorHandler.errorPanel("Error", "No web links to save", frame);
			return;
		}
		
		JFileChooser chooser = getJFileChooser();
		chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
		int option = chooser.showSaveDialog(frame);
		if (option == JFileChooser.APPROVE_OPTION) {
			try {
				FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
				File fil = chooser.getSelectedFile();
				String full_path = fil.getCanonicalPath();

				if (!full_path.endsWith(".xml")) {
					fil = new File(full_path + ".xml");
				}
				WebLink.exportWebLinks(fil, false);
			} catch (Exception ex) {
				ErrorHandler.errorPanel("Error", "Error exporting web links",
						frame, ex);
			}
		}
	}

	/** Called when the user selects a row of the table.
	 *  @param evt
	 */
	public void valueChanged(ListSelectionEvent evt) {
		setEnabled(true);

		JTable table;
		WebLinksTableModel model;
		if (localTable.isFocusOwner()) {
			table = localTable;
			model = localModel;
		} else {
			table = sysTable;
			model = sysModel;
		}

		selectedRows = table.getSelectedRows();

		initializationDetector = true;

		if (table.getRowCount() == 0) {
			setEnabled(false);
			return;
		} else if (selectedRows.length == 1) {
			WebLink selectedLink = model.getLinks().get(selectedRows[0]);

			previousName = selectedLink.getName();
			nameTextField.setText(previousName);
			previousUrl = selectedLink.getUrl();
			urlTextField.setText(previousUrl);
			String regex = selectedLink.getRegex();
			previousRegex = regex;
			if (regex == null) {
				regexTextField.setText("Display link for all tiers (uneditable)");
				allTiersRadioButton.setSelected(true);
				regexTextField.setEnabled(false);
			} else {
				if (regex.startsWith("(?i)")) {
					regex = regex.substring(4);
				}
				regexTextField.setText(regex);
				matchTierRadioButton.setSelected(true);
			}

			if (!selectedLink.getType().equalsIgnoreCase("local")) {
				nameTextField.setText(selectedLink.getName()
						+ "   (" + selectedLink.getType() + " web link - uneditable)");
				setEnabled(false);
			}
		} else {
			setEnabled(false);
		}

		initializationDetector = false;
	}

	private void setEnabled(boolean b) {
		nameTextField.setEnabled(b);
		urlTextField.setEnabled(b);
		regexTextField.setEnabled(b);
		allTiersRadioButton.setEnabled(b);
		matchTierRadioButton.setEnabled(b);
	}

	class WebLinksTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		List<WebLink> webLinks;

		WebLinksTableModel() {
			this.webLinks = Collections.<WebLink>emptyList();
		}

		public void setLinks(List<WebLink> webLinks) {
			this.webLinks = webLinks;
		}

		public List<WebLink> getLinks() {
			return this.webLinks;
		}

		@Override
		public Class<?> getColumnClass(int c) {
			Object tempObject = getValueAt(0, c);
			if (tempObject == null) {
				return Object.class;
			} else {
				return tempObject.getClass();
			}
		}

		@Override
		public String getColumnName(int columnIndex) {
			return col_headings[columnIndex];
		}

		public Object getValueAt(int row, int column) {
			WebLink webLink;
			webLink = webLinks.get(row);
			switch (column) {
				case COL_LINK_NAME:
					return webLink.getName();
				case COL_URL:
					return webLink.getUrl();
				default:
					return null;
			}
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			settingValueFromTable = true;
			if (value != null && !initializationDetector) {
				try {
					WebLink webLink = webLinks.get(row);
					switch (col) {
						case COL_LINK_NAME:
							webLink.setName((String) value);
							nameTextField.setText((String) value);
							break;
						case COL_URL:
							webLink.setUrl((String) value);
							urlTextField.setText((String) value);
							break;
						case COL_REGEX:
							if (matchTierRadioButton.isSelected()) {
								webLink.setRegex((String) value);
							}
							break;
						default:
							System.out.println("Unknown column selected: " + col);
					}
					fireTableCellUpdated(row, col);
				} catch (Exception e) {
					// exceptions should not happen, but must be caught if they do
					System.out.println("Exception in WebLinksView.setValueAt(): " + e);
				}

				refreshList();

			}
			settingValueFromTable = false;
		}

		public int getRowCount() {
			return webLinks.size();
		}

		public int getColumnCount() {
			return col_headings.length;
		}
	};
}
