package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.shared.StyledJTable;
import com.affymetrix.igb.prefs.WebLink.RegexType;
import com.affymetrix.igb.shared.FileTracker;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.util.List;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;

/**
 * All the function codes for web links panel are implemented in this class.
 * 
 * @modified by nick
 */
public final class WebLinksView implements ListSelectionListener {

	private static WebLinksView singleton;
	public StyledJTable serverTable;
	public StyledJTable localTable;
	public WebLinksTableModel serverModel;
	public WebLinksTableModel localModel;
	public ListSelectionModel lsm;
	private static JFileChooser static_chooser = null;
	public static final String NAME = "Name";
	public static final String URL = "URL Pattern";
	public static final String REGEX = "Regular Expression";
	public static final String TYPE = "Matches";
	public final static String[] col_headings = {
		NAME,
		REGEX,
		URL,
		TYPE
	};
	public static final int COL_NAME = 0;
	public static final int COL_REGEX = 1;
	public static final int COL_URL = 2;
	public static final int COL_TYPE = 3;
	public int[] selectedRows;
	public boolean initializationDetector; //Test to detect action events triggered by clicking a row in the table.
	public JTextField nameTextField;
	public JTextField urlTextField;
	public JTextField regexTextField;
	public JRadioButton nameRadioButton;
	public JRadioButton idRadioButton;
	private final ButtonGroup button_group = new ButtonGroup();
	public int previousSelectedRow;
	public final static Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
	public final static Cursor defaultCursor = null;

	public static synchronized WebLinksView getSingleton() {
		if (singleton == null) {
			singleton = new WebLinksView();
		}
		return singleton;
	}

	private WebLinksView() {
		super();

		serverTable = new StyledJTable();
		localTable = new StyledJTable();

		serverModel = new WebLinksTableModel();
		localModel = new WebLinksTableModel();

		initTable(serverTable);
		initTable(localTable);

		nameTextField = new JTextField();
		urlTextField = new JTextField();
		regexTextField = new JTextField();
		nameRadioButton = new JRadioButton();
		idRadioButton = new JRadioButton();
		button_group.add(nameRadioButton);
		button_group.add(idRadioButton);

		if (localTable.getRowCount() > 0) {
			localTable.setRowSelectionInterval(0, 0);
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
			table.setModel(serverModel);
			serverModel.setLinks(WebLink.getServerWebList());
		}

		table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(120);
		table.getColumnModel().getColumn(COL_NAME).setMaxWidth(400);
		table.getColumnModel().getColumn(COL_REGEX).setPreferredWidth(130);
		table.getColumnModel().getColumn(COL_REGEX).setMaxWidth(400);
		table.getColumnModel().getColumn(COL_TYPE).setPreferredWidth(90);
		table.getColumnModel().getColumn(COL_TYPE).setMaxWidth(90);
	}

	/*
	 * Only allow to delete local web links
	 */
	public void delete() throws HeadlessException {
		if (localTable.getSelectedRow() != -1) {
			selectedRows = localTable.getSelectedRows();
			if (confirmDelete()) {
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

	public boolean confirmDelete() {
		String message = "Delete these " + selectedRows.length
				+ " selected link(s)?\n";

		return Application.confirmPanel(WebLinksViewGUI.getSingleton(),
				message, PreferenceUtils.getTopNode(),
				PreferenceUtils.CONFIRM_BEFORE_DELETE,
				PreferenceUtils.default_confirm_before_delete);
	}

	public void add() {
		WebLink link = new WebLink();
		link.setName(BUNDLE.getString("default_name"));
		link.setUrl(BUNDLE.getString("default_url"));
		link.setRegex(BUNDLE.getString("default_regex"));
		link.setType(WebLink.LOCAL);
		WebLink.addWebLink(link);

		refreshList();

		int row = 0;
		for (WebLink l : localModel.webLinks) {
			if (l == link) {
				break;
			}

			row++;
		}

		resetRow(row);

		setEnabled(true);
		nameRadioButton.setSelected(true);
		nameTextField.grabFocus();
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

		if (serverTable.getSelectedRow() != -1
				&& localTable.getSelectedRow() != -1) {
			serverTable.removeRowSelectionInterval(0, serverTable.getRowCount() - 1);
		}
	}

	public void nameTextFieldKeyReleased() {
		if (localTable.getSelectedRow() != -1) {
			String name = nameTextField.getText();
			if (!isEmpty(name)) {
				localModel.setValueAt(name, selectedRows[0], COL_NAME);
			}
		}
	}

	public void urlTextField() {
		if (localTable.getSelectedRow() != -1) {
			String url = urlTextField.getText();
			if (!isEmpty(url)) {
				localModel.setValueAt(url, selectedRows[0], COL_URL);
			}
		}
	}

	public void regexTipMouseReleased() {
		GeneralUtils.browse(BUNDLE.getString("instruction_page"));
	}

	public void regexTextFieldKeyReleased() {
		if (localTable.getSelectedRow() != -1) {
			String regex = regexTextField.getText();
			if (!isEmpty(regex)) {
				localModel.setValueAt(regex, selectedRows[0], COL_REGEX);
			}
		}
	}

	public void idRadioButton() {
		localModel.setValueAt(WebLink.RegexType.ID, selectedRows[0], COL_TYPE);
	}

	public void nameRadioButton() {
		localModel.setValueAt(WebLink.RegexType.TYPE, selectedRows[0], COL_TYPE);
	}

	/**
	 * Gets a static re-usable file chooser that prefers "html" files.
	 */
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

	public void clear() {
		nameTextField.setText("");
		urlTextField.setText("");
		regexTextField.setText("");
	}

	/**
	 * Called when the user selects a row of the table.
	 *
	 * @param evt
	 */
	public void valueChanged(ListSelectionEvent evt) {
		setEnabled(true);

		selectedRows = localTable.getSelectedRows();

		initializationDetector = true;

		if (localTable.getSelectedRowCount() == 0) {
			setEnabled(false);
			clear();
		} else if (selectedRows.length == 1) {
			WebLink link = localModel.getLinks().get(selectedRows[0]);

			nameTextField.setText(link.getName());
			urlTextField.setText(link.getUrl());
			regexTextField.setText(link.getRegex());

			if (link.getRegexType() == RegexType.TYPE) {
				nameRadioButton.setSelected(true);
			} else if (link.getRegexType() == RegexType.ID) {
				idRadioButton.setSelected(true);
			}

			if (!link.getType().equals(WebLink.LOCAL)) {
				nameTextField.setText(link.getName()
						+ "   (" + link.getType() + " web link - uneditable)");
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
				case COL_TYPE:
					if (webLink.getRegexType() == RegexType.ID) {
						return "Annotation ID";
					} else {
						return "Track Name";
					}
				default:
					return null;
			}
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			if (value != null && !initializationDetector) {
				try {
					WebLink webLink = webLinks.get(row);
					switch (col) {
						case COL_NAME:
							webLink.setName((String) value);
							nameTextField.setText((String) value);
							break;
						case COL_REGEX:
							webLink.setRegex((String) value);
							break;
						case COL_URL:
							webLink.setUrl((String) value);
							urlTextField.setText((String) value);
							break;
						case COL_TYPE:
							webLink.setRegexType((RegexType) value);
							break;
						default:
							System.out.println("Unknown column selected: " + col);
					}
				} catch (Exception e) {
					// exceptions should not happen, but must be caught if they do
					System.out.println("Exception in WebLinksView.setValueAt(): " + e);
				}

				previousSelectedRow = localTable.getSelectedRow();

				setLinks(WebLink.getLocalWebList());
				fireTableCellUpdated(row, col);

				resetRow(previousSelectedRow);
			}
		}

		public int getRowCount() {
			return webLinks.size();
		}

		public int getColumnCount() {
			return col_headings.length;
		}
	};
}
