package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.genometryImpl.util.PreferenceUtils;

import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.igb.prefs.WebLink.RegexType;
import com.affymetrix.igb.shared.FileTracker;
import java.util.List;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
	public JTable defaultTable;
	public JTable localTable;
	public WebLinksTableModel defaultModel;
	public WebLinksTableModel localModel;
	public ListSelectionModel lsm;
	private static JFileChooser static_chooser = null;
	public static final String NAME = "Name";
	public static final String URL = "URL";
	public static final String REGEX = "Regular Expression";
	public final static String[] col_headings = {
		NAME,
		REGEX,
		URL
	};
	public static final int COL_NAME = 0;
	public static final int COL_REGEX = 1;
	public static final int COL_URL = 2;
	public int[] selectedRows;
	public boolean initializationDetector; //Test to detect action events triggered by clicking a row in the table.
	public boolean settingValueFromTable;  //Test to prevent action events triggered by the setValueAt method from calling the method again.  This improves efficiency.
	public JTextField nameTextField;
	public JTextField urlTextField;
	public JTextField regexTextField;
	public JRadioButton nameRadioButton;
	public JRadioButton idRadioButton;
	private final ButtonGroup button_group = new ButtonGroup();
	private final WebLinkEditorPanel edit_panel;
	public int previousSelectedRow;
	private String previousName;
	private String previousUrl;
	private String previousRegex;

	public static synchronized WebLinksView getSingleton() {
		if (singleton == null) {
			singleton = new WebLinksView();
		}
		return singleton;
	}

	private WebLinksView() {
		super();

		defaultTable = new JTable();
		localTable = new JTable();

		defaultModel = new WebLinksTableModel();
		localModel = new WebLinksTableModel();

		initTable(defaultTable);
		initTable(localTable);

		nameTextField = new JTextField();
		urlTextField = new JTextField();
		regexTextField = new JTextField();
		nameRadioButton = new JRadioButton();
		idRadioButton = new JRadioButton();
		button_group.add(nameRadioButton);
		button_group.add(idRadioButton);
		edit_panel = new WebLinkEditorPanel();

		if (localTable.getRowCount() > 0) {
			localTable.setRowSelectionInterval(0, 0);
		} else {
			defaultTable.setRowSelectionInterval(0, 0);
		}
	}

	private void initTable(JTable table) {
		lsm = table.getSelectionModel();
		lsm.addListSelectionListener(this);
		lsm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		if (table.equals(localTable)) {
			table.setModel(localModel);
			localModel.setLinks(WebLink.getLocalWebList());
		} else {
			table.setModel(defaultModel);
			defaultModel.setLinks(WebLink.getSysWebList());
		}

		table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_NAME).setMaxWidth(400);
		table.getColumnModel().getColumn(COL_REGEX).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_REGEX).setMaxWidth(400);

		Font f = new Font("SansSerif", Font.BOLD, 12);
		table.getTableHeader().setFont(f);

		table.setCellSelectionEnabled(settingValueFromTable);
		table.setRowSelectionAllowed(true);
	}

	private static void setAccelerator(Action a) {
		KeyStroke ks = PreferenceUtils.getAccelerator("Web Links Manager / "
				+ a.getValue(Action.NAME));
		a.putValue(Action.ACCELERATOR_KEY, ks);
	}

	/*
	 *  Only allow to delete local web links
	 */
	public void delete() throws HeadlessException {
		if (localTable.getSelectedRow() != -1) {
			selectedRows = localTable.getSelectedRows();
			Container frame = SwingUtilities.getAncestorOfClass(JFrame.class, null);

			int yes = JOptionPane.showConfirmDialog(frame, "Delete these "
					+ selectedRows.length + " selected link(s)?", "Delete?",
					JOptionPane.YES_NO_OPTION);

			if (yes == JOptionPane.YES_OPTION) {
				List<WebLink> links = new ArrayList<WebLink>();
				for (int i : selectedRows) {
					links.add(localModel.webLinks.get(i));
				}

				for (WebLink l : links) {
					WebLink.removeLocalWebLink(l);
				}
				
			}

			refreshList();
		}
	}

	public void add() {
		WebLink link = new WebLink();
		edit_panel.setWebLink(link);
		boolean ok = edit_panel.showDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, null));
		if (ok) {
			edit_panel.setLinkPropertiesFromGUI();
			link.setType(WebLink.LOCAL);
			WebLink.addWebLink(link);
		} else {
			return;
		}

		refreshList();

		int row = 0;
		for (WebLink l : localModel.webLinks) {
			if (l == link) {
				break;
			}
			
			row++;
		}

		resetRow(row);
	}

	private boolean isEmpty(String s) {
		return (s == null || s.trim().length() == 0);
	}

	private void refreshList() {
		localModel.setLinks(WebLink.getLocalWebList());
		localModel.fireTableDataChanged();
	}

	public void resetRow(int row) {
		if (row < localTable.getRowCount()
				&& row != -1) {
			localTable.setRowSelectionInterval(row, row);
		}
	}

	public void nameTextField() {
		if (!settingValueFromTable && localTable.getSelectedRow() != -1) {
			if (isEmpty(nameTextField.getText())) {
				ErrorHandler.errorPanel("The name cannot be blank");
				nameTextField.setText(previousName);
			} else {
				localModel.setValueAt(nameTextField.getText(), selectedRows[0], COL_NAME);
			}

			nameTextField.grabFocus();
		}
	}

	public void urlTextField() {
		if (!settingValueFromTable && localTable.getSelectedRow() != -1) {
			if (isEmpty(urlTextField.getText())) {
				ErrorHandler.errorPanel("The URL cannot be blank");
				urlTextField.setText(previousUrl);
			} else {
				try {
					new URL(urlTextField.getText());
					localModel.setValueAt(urlTextField.getText(), selectedRows[0], COL_URL);
				} catch (MalformedURLException e) {
					ErrorHandler.errorPanel("Malformed URL",
							"The given URL appears to be invalid.\n" + e.getMessage(),
							urlTextField);
					urlTextField.setText(previousUrl);
				}
			}

			urlTextField.grabFocus();
		}
	}

	public void regexTextField() {
		if (!settingValueFromTable && localTable.getSelectedRow() != -1) {
			if (isEmpty(regexTextField.getText())) {
				ErrorHandler.errorPanel("The regular expression cannot be blank");
			}

			try {
				Pattern.compile(regexTextField.getText());
				localModel.setValueAt(regexTextField.getText(), selectedRows[0], COL_REGEX);
			} catch (PatternSyntaxException pse) {
				ErrorHandler.errorPanel("Bad Regular Expression",
						"Error in regular expression:\n" + pse.getMessage(), regexTextField);
				regexTextField.setText(previousRegex);
			}

			regexTextField.grabFocus();
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
	 * Tracks to import weblinks.
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
				ErrorHandler.errorPanel("Importing web links: File Not Found "
						+ fil.getAbsolutePath(), null, fe);
			} catch (Exception ex) {
				ErrorHandler.errorPanel("Importing web links", null, ex);
			}
		}

		refreshList();
		resetRow(0);
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

		if (localTable.getSelectedRow() != -1) {
			table = localTable;
			model = localModel;
		} else {
			table = defaultTable;
			model = defaultModel;
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

			if (regex == null) {
				regex = "";
			} else if (regex.startsWith("(?i)")) {
				regex = regex.substring(4);
			}
			previousRegex = regex;
			regexTextField.setText(regex);
			if (selectedLink.getRegexType() == RegexType.TYPE) {
				nameRadioButton.setSelected(true);
			} else if (selectedLink.getRegexType() == RegexType.ID) {
				idRadioButton.setSelected(true);
			}

			if (!selectedLink.getType().equals(WebLink.LOCAL)) {
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
		nameRadioButton.setEnabled(b);
		idRadioButton.setEnabled(b);
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
				case COL_NAME:
					return webLink.getName();
				case COL_REGEX:
					return webLink.getRegex();
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
						case COL_NAME:
							webLink.setName((String) value);
							nameTextField.setText((String) value);
							break;
						case COL_REGEX:
							if (idRadioButton.isSelected()) {
								webLink.setRegexType(WebLink.RegexType.ID);
							} else {
								webLink.setRegexType(WebLink.RegexType.TYPE);
							}
							webLink.setRegex((String) value);
							break;
						case COL_URL:
							webLink.setUrl((String) value);
							urlTextField.setText((String) value);
							break;
						default:
							System.out.println("Unknown column selected: " + col);
					}
				} catch (Exception e) {
					// exceptions should not happen, but must be caught if they do
					System.out.println("Exception in WebLinksView.setValueAt(): " + e);
				}

				previousSelectedRow = localTable.getSelectedRow();

				refreshList();
				resetRow(previousSelectedRow);
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
