package com.affymetrix.igb.view;

import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genometry.util.UniFileChooser;
import com.affymetrix.genometry.weblink.WebLink;
import com.affymetrix.genometry.weblink.WebLink.RegexType;
import com.affymetrix.igb.Application;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.prefs.WebLinkUtils;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.igb.shared.StyledJTable;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

/**
 * All the function codes for web links panel are implemented in this class.
 *
 * modified by nick
 */
public final class WebLinksView {

	private static WebLinksView singleton;
	public StyledJTable serverTable;
	public StyledJTable localTable;
	public WebLinksTableModel serverModel;
	public WebLinksTableModel localModel;
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
	public boolean initializationDetector; //Test to detect action events triggered by clicking a row in the table.
	public JTextField nameTextField;
	public JTextField urlTextField;
	public JTextField regexTextField;
	public JTextField startWithTextField;
	public JTextField endWithTextField;
	public JTextField containsTextField;
	public JCheckBox ignoreCaseCheckBox;
	public JRadioButton nameRadioButton;
	public JRadioButton idRadioButton;
	public JButton deleteButton;
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
	
		serverModel = new WebLinksTableModel();
		localModel = new WebLinksTableModel();
		
		serverTable = new StyledJTable(serverModel);
		localTable = new StyledJTable(localModel);
		
		ListSelectionListener serverListener = new ServerListSelectionListener(serverTable);
		ListSelectionListener localListener = new LocalListSelectionListener(localTable);

		initTable(serverTable, WebLinkUtils.getServerList().getWebLinkList(), serverListener);
		initTable(localTable, WebLinkUtils.getLocalList().getWebLinkList(), localListener);

		nameTextField = new JTextField();
		urlTextField = new JTextField();
		regexTextField = new JTextField();
		startWithTextField = new JTextField();
		endWithTextField = new JTextField();
		containsTextField = new JTextField();
		ignoreCaseCheckBox = new JCheckBox();
		nameRadioButton = new JRadioButton();
		idRadioButton = new JRadioButton();
		deleteButton = new JButton();
		button_group.add(nameRadioButton);
		button_group.add(idRadioButton);

		if (localTable.getRowCount() > 0) {
			localTable.setRowSelectionInterval(0, 0);
		}else{
			setEnabled(false);
		}
	}

	private void initTable(JTable table, List<WebLink> weblinks, ListSelectionListener listener) {
		ListSelectionModel lsm = table.getSelectionModel();
		lsm.addListSelectionListener(listener);
		lsm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		((WebLinksTableModel)table.getModel()).setLinks(weblinks);
		
		table.setCellSelectionEnabled(false);
		table.setRowSelectionAllowed(true);
		
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
	public void delete(JTable table) throws HeadlessException {
		if (table.getSelectedRow() != -1) {
			int[] selectedTableRows = table.getSelectedRows();
			if (confirmDelete(table.getSelectedRowCount())) {
				List<WebLink> links = new ArrayList<>();
				for (int i : selectedTableRows) {
					links.add(((WebLinksTableModel)table.getModel()).webLinks.get(i));
				}

				for (WebLink l : links) {
					WebLinkUtils.getLocalList().removeWebLink(l);
				}

			}

			refreshList();
		}
	}

	/*
	 * A confirmation window for delete operation
	 */
	public boolean confirmDelete(int numberOfRows) {
		String message = "Delete these " + numberOfRows
				+ " selected link(s)?\n";

		return Application.confirmPanel(WebLinksViewGUI.getSingleton(),
				message, PreferenceUtils.getTopNode(),
				PreferenceUtils.CONFIRM_BEFORE_DELETE,
				PreferenceUtils.default_confirm_before_delete);
	}

	/*
	 * Create a new weblink by default values
	 */
	public void add() {
		WebLink link = new WebLink();
		link.setName(BUNDLE.getString("default_name"));
		link.setUrl(BUNDLE.getString("default_url"));
		link.setRegex(BUNDLE.getString("default_regex"));
		link.setType(WebLink.LOCAL);
		WebLinkUtils.getLocalList().addWebLink(link);

		refreshList();

		int row = 0;
		for (WebLink l : localModel.webLinks) {
			if (l == link) {
				break;
			}

			row++;
		}

		// Highlight row of created web link
		resetRow(row);

		setEnabled(true);
		nameRadioButton.setSelected(true);
		nameTextField.grabFocus();
	}

	private boolean isEmpty(String s) {
		return (s == null || s.trim().length() == 0);
	}

	private void refreshList() {
		localModel.setLinks(WebLinkUtils.getLocalList().getWebLinkList());
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
				localModel.setValueAt(name, localTable.getSelectedRows()[0], COL_NAME);
			}
		}
	}

	public void urlTextField() {
		if (localTable.getSelectedRow() != -1) {
			String url = urlTextField.getText();
			if (!isEmpty(url)) {
				localModel.setValueAt(url, localTable.getSelectedRows()[0], COL_URL);
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
				try {
					Pattern.compile(regex);
					regexTextField.setForeground(Color.BLACK);
					localModel.setValueAt(regex, localTable.getSelectedRows()[0], COL_REGEX);
					ignoreCaseCheckBox.setSelected((".*".equals(regex) || regex.startsWith("(?i)")));
				}catch (PatternSyntaxException e){
					regexTextField.setForeground(Color.RED);
				}	
			}
		}
	}
	
	/**
	 * This method will check every user input field to compose regular expression
	 * 
	 * The 'reverse engineering' (translate regex) is not available now due to the complexiity 
	 */
	
	public void composeRegex() {
		if (localTable.getSelectedRow() != -1) {
			String startWith = startWithTextField.getText();
			String endWith = endWithTextField.getText();
			String contains = containsTextField.getText();
			
			String regex = ".*";
			
			if(!startWith.trim().isEmpty() && endWith.trim().isEmpty() && contains.trim().isEmpty()) {
				regex = "^" + startWith + ".*";
			} else if(startWith.trim().isEmpty() && !endWith.trim().isEmpty() && contains.trim().isEmpty()) {
				regex = ".*" + endWith + "$";
			} else if(startWith.trim().isEmpty() && endWith.trim().isEmpty() && !contains.trim().isEmpty()) {
				regex = ".*" + contains + ".*";
			} else if(!startWith.trim().isEmpty() && !endWith.trim().isEmpty() && contains.trim().isEmpty()) {
				regex = "^" + startWith + ".*" + endWith + "$";
			} else if(!startWith.trim().isEmpty() && endWith.trim().isEmpty() && !contains.trim().isEmpty()) {
				regex = "^" + startWith + ".*" + contains + ".*";
			} else if(startWith.trim().isEmpty() && !endWith.trim().isEmpty() && !contains.trim().isEmpty()) {
				regex = ".*" + contains + ".*" + endWith + "$";
			} else if(!startWith.trim().isEmpty() && !endWith.trim().isEmpty() && !contains.trim().isEmpty()) {
				regex = "^" + startWith + ".*" + contains + ".*" + endWith + "$";
			} else {
				localModel.setValueAt(regex, localTable.getSelectedRows()[0], COL_REGEX);
				regexTextField.setText(regex);
				return;
			}
			
			regex = (ignoreCaseCheckBox.isSelected()) ? "(?i)" + regex : regex;
			localModel.setValueAt(regex, localTable.getSelectedRows()[0], COL_REGEX);
			regexTextField.setText(regex);
		}
	}
	
	public void ignoreCaseCheckBoxStateChanged() {
		String regex = regexTextField.getText();
		
		if(!regex.equals(".*")) {
			if(ignoreCaseCheckBox.isSelected()) {
				regex = "(?i)" + regex;
			} else if(regex.startsWith("(?i)")) {
				regex = regex.substring(4);
			}
		}
		
		regexTextField.setText(regex);
		localModel.setValueAt(regex, localTable.getSelectedRows()[0], COL_REGEX);
	}

	public void idRadioButton() {
		localModel.setValueAt(WebLink.RegexType.ID, localTable.getSelectedRows()[0], COL_TYPE);
	}

	public void nameRadioButton() {
		localModel.setValueAt(WebLink.RegexType.TYPE, localTable.getSelectedRows()[0], COL_TYPE);
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
				WebLinkUtils.importWebLinks(fil);
			} catch (FileNotFoundException fe) {
				ErrorHandler.errorPanel("Importing web links: File Not Found "
						+ fil.getAbsolutePath(), fe, Level.SEVERE);
			} catch (Exception ex) {
				ErrorHandler.errorPanel("Importing web links", ex, Level.SEVERE);
			}
		}

		refreshList();
		resetRow(0);
	}

	public void exportWebLinks() {
		Container frame = SwingUtilities.getAncestorOfClass(JFrame.class, null);

		if (localTable.getRowCount() == 0) {
			ErrorHandler.errorPanel("Error", "No web links to save", Level.WARNING);
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
				WebLinkUtils.exportWebLinks(fil, false);
			} catch (Exception ex) {
				ErrorHandler.errorPanel("Error exporting web links", ex, Level.SEVERE);
			}
		}
	}

	public void clear() {
		nameTextField.setText("");
		urlTextField.setText("");
		regexTextField.setText("");
		startWithTextField.setText("");
		endWithTextField.setText("");
		containsTextField.setText("");
		ignoreCaseCheckBox.setSelected(true);
	}

	private void setEnabled(boolean b) {
		setEnabled(b, b);
	}
	
	private void setEnabled(boolean b, boolean enableDeleteButton) {
		nameTextField.setEnabled(b);
		urlTextField.setEnabled(b);
		regexTextField.setEnabled(b);
		nameRadioButton.setEnabled(b);
		idRadioButton.setEnabled(b);
		startWithTextField.setEnabled(b);
		endWithTextField.setEnabled(b);
		containsTextField.setEnabled(b);
		ignoreCaseCheckBox.setEnabled(b);
		deleteButton.setEnabled(enableDeleteButton);
	}
	
	class WebLinksTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		List<WebLink> webLinks;

		WebLinksTableModel() {
			this.webLinks = Collections.<WebLink>emptyList();
		}

		public void setLinks(List<WebLink> webLinks) {
			Collections.sort(webLinks);
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
			WebLink webLink = null;
			if (value != null && !initializationDetector) {
				try {
					webLink = webLinks.get(row);
					switch (col) {
						case COL_NAME:
							webLink.setName((String) value);
							break;
						case COL_REGEX:
							webLink.setRegex((String) value);
							break;
						case COL_URL:
							webLink.setUrl((String) value);
							break;
						case COL_TYPE:
							webLink.setRegexType((RegexType) value);
							break;
						default:
							System.out.println("Unknown column selected: " + col);
					}
				} catch (PatternSyntaxException e) {
					Logger.getLogger(WebLinksView.class.getName()).log(Level.WARNING,
							MessageFormat.format("Invalid regular expression {0} for {1}",
							new Object[]{webLink.getRegexType(), webLink.getName()}));

				}catch (Exception e) {
					// exceptions should not happen, but must be caught if they do
					System.out.println("Exception in WebLinksView.setValueAt(): " + e);
				}

				previousSelectedRow = localTable.getSelectedRow();

				setLinks(WebLinkUtils.getLocalList().getWebLinkList());
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
	}

	class LocalListSelectionListener implements ListSelectionListener {
		
		private JTable table;
		
		public LocalListSelectionListener(JTable table) {
			this.table = table;
		}

		public JTable getTable() {
			return this.table;
		}
		
		@Override
		public void valueChanged(ListSelectionEvent lse) {
			setEnabled(true);
			
			int[] selectedRows = table.getSelectedRows();

			initializationDetector = true;

			if (table.getSelectedRowCount() == 0) {
				setEnabled(false);
				clear();
			} else if (selectedRows.length == 1) {
				showWebLinksDetail(table);
			} else {
				// Clear fields for multiple selection but enable delete button
				setEnabled(false, true);
				clear();
			}
			
			initializationDetector = false;
		}
	}
	
	class ServerListSelectionListener implements ListSelectionListener {

		private JTable table;
		
		public ServerListSelectionListener(JTable table) {
			this.table = table;
		}
		
		public JTable getTable() {
			return this.table;
		}
		
		@Override
		public void valueChanged(ListSelectionEvent lse) {
			int[] selectedRows = table.getSelectedRows();

			if(selectedRows.length == 1) {
				showWebLinksDetail(table);
			} else {
				// Clear fields for multiple selection
				clear();
			}
			
			// Always disable Web Links Builder for system Weblinks table
			setEnabled(false);
		}
	}
	
	
	// Filling up the necessary fields when table row selected
	private void showWebLinksDetail(JTable table) {
		WebLink link = ((WebLinksTableModel) table.getModel()).getLinks().get(table.getSelectedRows()[0]);

		nameTextField.setText(link.getName());
		urlTextField.setText(link.getUrl());
		regexTextField.setText(link.getRegex());

		// Clear builder fields since reverse engineering for regex is hard
		startWithTextField.setText("");
		endWithTextField.setText("");
		containsTextField.setText("");
		
		ignoreCaseCheckBox.setSelected((".*".equals(link.getRegex()) || (link.getRegex()).startsWith("(?i)")));

		if (link.getPattern() == null && !".*".equals(link.getRegex()) && !"(?i).*".equals(link.getRegex())) {
			regexTextField.setForeground(Color.red);
		} else {
			regexTextField.setForeground(Color.BLACK);
		}

		if (link.getRegexType() == RegexType.TYPE) {
			nameRadioButton.setSelected(true);
		} else if (link.getRegexType() == RegexType.ID) {
			idRadioButton.setSelected(true);
		}
	}
}