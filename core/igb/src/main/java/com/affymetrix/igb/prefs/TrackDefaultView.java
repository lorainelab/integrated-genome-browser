package com.affymetrix.igb.prefs;

import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.PropertyConstants;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.shared.StyledJTable;
import com.affymetrix.igb.stylesheet.AssociationElement;
import com.affymetrix.igb.stylesheet.PropertyMap;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.tiers.TrackConstants;
import com.affymetrix.igb.tiers.TrackConstants.DIRECTION_TYPE;
import com.affymetrix.igb.tiers.TrackStyle;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author dcnorris Modified by nick
 */
public final class TrackDefaultView extends TrackPreferences implements ListSelectionListener {

	private static final String FILE_TYPE = "File Type";
	private static final String FOREGROUND = "FG";
	private static final String BACKGROUND = "BG";
	private static final String TRACK_NAME_SIZE = "Size";
	private final static String[] col_headings = {
		FILE_TYPE,
		BACKGROUND, FOREGROUND,
		TRACK_NAME_SIZE,};
	public static final int COL_TRACK_DEFAULT = 0;
	private static TrackStyle default_annot_style = IGBStateProvider.getDefaultInstance();
	private Object[] temp;
	private String[] trackDefaults;
	private String[] graphFormats = {"bar", "bgr", "egr", "egr.txt", "sin", "gr", "sgr", "useq", "wig"};
	private String[] sequenceFormats = {"2bit", "fa", "fasta", "bnib"};
	private ArrayList<String> list = new ArrayList<String>();
	private String allowedTrackDefaults;
	private String AddButtonTitle = "Add Track Default";
	private String selectedTrackDefaultType;
	private TrackStyle selectedStyle;
	private JRPButton addTrackDefaultButton;
	private JRPButton removeTrackDefaultButton;
	private JTextField trackDefaultTextField;
	private static TrackDefaultView singleton;

	public static synchronized TrackDefaultView getSingleton() {
		if (singleton == null) {
			singleton = new TrackDefaultView();
		}

		return singleton;
	}

	/**
	 * Creates new form FileTypeViewNew
	 */
	private TrackDefaultView() {
		model = new TrackDefaultPrefTableModel();
		title = "Track Default List";
		b1Text = "Add";
		b2Text = "Delete";
		track = "Track Type";
		((TrackDefaultPrefTableModel) model).setElements(XmlStylesheetParser.getUserFileTypeAssociation());
		initializeFileTypes();
		initCommonComponents();
		initComponents();
		table.setRowSelectionInterval(0, 0);
	}

	private void initializeFileTypes() {
		list.clear();
		temp = FileTypeHolder.getInstance().getNameToExtensionMap(null).values().toArray();
		for (int i = 0; i < temp.length; i++) {
			StringTokenizer tokens = new StringTokenizer(temp[i].toString(), ",");
			while (tokens.hasMoreElements()) {
				allowedTrackDefaults = tokens.nextToken();
				allowedTrackDefaults = allowedTrackDefaults.replace("[", "");
				allowedTrackDefaults = allowedTrackDefaults.replace("]", "");
				allowedTrackDefaults = allowedTrackDefaults.trim();
				if (!Arrays.asList(sequenceFormats).contains(allowedTrackDefaults)) {
					list.add(allowedTrackDefaults);
				}
			}
		}
		Collections.sort(list);
		trackDefaults = list.toArray(new String[list.size()]);
	}

	private void initComponents() {
		trackDefaultTextField = new JTextField();
		trackDefaultTextField.setEnabled(false);
		table = new StyledJTable();
		table.setCellSelectionEnabled(false);
		TableCellEditor editor = table.getDefaultEditor(String.class);
		((DefaultCellEditor) editor).setClickCountToStart(2);
		table.setDefaultEditor(String.class, editor);
		
		table.list.add(TierPrefsView.COL_BACKGROUND);
		table.list.add(TierPrefsView.COL_FOREGROUND);
		table.list.add(TierPrefsView.COL_LABEL_COLOR);
		
		addTrackDefaultButton = new JRPButton("TrackDefaultView_addTrackDefaultButton");
		removeTrackDefaultButton = new JRPButton("TrackDefaultView_removeTrackDefaultButton");
		initTable();
		addTrackDefaultButton.setText("Add");
		removeTrackDefaultButton.setText("Remove");
	}

	@Override
	public void trackNameSizeComboBox() {
		if (applyChanged) { 
			trackNameSize = Float.parseFloat(trackNameSizeComboBox.getSelectedItem().toString());
			model.setValueAt(trackNameSize, selectedRows[0], COL_TRACK_NAME_SIZE);
		}
	}

	@SuppressWarnings("unchecked")
	public void addTrackDefaultButton() {
		initializeFileTypes();
		selectedTrackDefaultType = (String) JOptionPane.showInputDialog(null, "Select file type:", AddButtonTitle,
				JOptionPane.PLAIN_MESSAGE, null, trackDefaults, trackDefaults[0]);

		if (selectedTrackDefaultType == null) {
			return;
		}

		if (XmlStylesheetParser.getUserFileTypeAssociation().get(selectedTrackDefaultType) != null) {
			ErrorHandler.errorPanel("Duplicate Entry", "File type " + selectedTrackDefaultType + " exists");
			return;
		}

		AssociationElement element = AssociationElement.getFileTypeAssocation(selectedTrackDefaultType);
		AssociationElement sysElement = XmlStylesheetParser.getSystemFileTypeAssociation().get(selectedTrackDefaultType);
		if (sysElement != null) {
			PropertyMap props = sysElement.getPropertyMap();
			element.getPropertyMap().putAll(props);
		}

		Map<String, AssociationElement> elements = XmlStylesheetParser.getUserFileTypeAssociation();
		elements.put(selectedTrackDefaultType, element);
		((TrackDefaultPrefTableModel) model).file2types = elements.entrySet().toArray(new Entry[elements.size()]);
		((TrackDefaultPrefTableModel) model).addElement(selectedTrackDefaultType, element);
		model.fireTableDataChanged();
	}

	public void deleteTrackDefaultButton() {
		if (table.getSelectedRow() != -1) {
			List<TrackStyle> styles = new ArrayList<TrackStyle>();
			for (int i : selectedRows) {
				styles.add(((TrackDefaultPrefTableModel) model).tier_styles.get(i));
			}

			for (TrackStyle style : styles) {
				if (style != default_annot_style) {
					XmlStylesheetParser.getUserFileTypeAssociation().remove(style.getTrackName());
					((TrackDefaultPrefTableModel) model).removeElement(style.getTrackName());
				}
			}

			model.fireTableDataChanged();
		}
	}

	private void setEnabled(boolean b) {
		labelFieldComboBox.setEnabled(b);
		maxDepthTextField.setEnabled(b);
		show2TracksCheckBox.setEnabled(b);
		collapsedCheckBox.setEnabled(b);
		possitiveColorComboBox.setEnabled(b);
		negativeColorComboBox.setEnabled(b);
		arrowCheckBox.setEnabled(b);
		colorCheckBox.setEnabled(b);
	}

	/**
	 * Called when the user selects a row of the table.
	 *
	 * @param evt
	 */
	public void valueChanged(ListSelectionEvent evt) {
		selectedRows = table.getSelectedRows();
		if (selectedRows.length > 0) {
			initializationDetector = true;
			applyChanged = false;

			setEnabled(true);

			selectedStyle = ((TrackDefaultPrefTableModel) model).getStyles().get(selectedRows[0]);

			if (selectedStyle == default_annot_style) {
				removeTrackDefaultButton.setEnabled(false);
			} else {
				removeTrackDefaultButton.setEnabled(true);
			}

			trackDefaultTextField.setText(selectedStyle.getTrackName());
			bgColorComboBox.setSelectedColor(selectedStyle.getBackground());
			fgColorComboBox.setSelectedColor(selectedStyle.getForeground());
			labelColorComboBox.setSelectedColor(selectedStyle.getLabelForeground());
			trackNameSizeComboBox.setSelectedItem(selectedStyle.getTrackNameSize());

			if (Arrays.asList(graphFormats).contains(selectedStyle.getTrackName())) {
				setEnabled(false);
				labelFieldComboBox.setSelectedItem(null);
				maxDepthTextField.setText("");
				show2TracksCheckBox.setSelected(false);
				collapsedCheckBox.setSelected(false);
				colorCheckBox.setSelected(false);
				arrowCheckBox.setSelected(false);
				possitiveColorComboBox.setSelectedColor(null);
				negativeColorComboBox.setSelectedColor(null);
			} else {
				possitiveColorComboBox.setSelectedColor(selectedStyle.getForwardColor());
				negativeColorComboBox.setSelectedColor(selectedStyle.getReverseColor());
				labelFieldComboBox.setSelectedItem(selectedStyle.getLabelField());
				maxDepthTextField.setText(String.valueOf(selectedStyle.getMaxDepth()));
				show2TracksCheckBox.setSelected(!selectedStyle.getSeparate());
				collapsedCheckBox.setSelected(selectedStyle.getCollapsed());

				switch (DIRECTION_TYPE.valueFor(selectedStyle.getDirectionType())) {
					case NONE:
						colorCheckBox.setSelected(false);
						arrowCheckBox.setSelected(false);
						break;
					case ARROW:
						colorCheckBox.setSelected(false);
						arrowCheckBox.setSelected(true);
						break;
					case COLOR:
						colorCheckBox.setSelected(true);
						arrowCheckBox.setSelected(false);
						break;
					case BOTH:
						colorCheckBox.setSelected(true);
						arrowCheckBox.setSelected(true);
						break;
					default:
						System.out.println("Unknown enum selected");
						break;
				}
			}

			applyChanged = true;
			initializationDetector = false;
		}
	}
	public JRPButton getAddTrackDefaultButton() {
		return addTrackDefaultButton;
	}

	public JRPButton getRemoveTrackDefaultButton() {
		return removeTrackDefaultButton;
	}

	public JTextField getTrackDefaultTextField() {
		return trackDefaultTextField;
	}
	class TrackDefaultPrefTableModel extends AbstractTableModel implements PropertyConstants {

		private static final long serialVersionUID = 1L;
		private List<TrackStyle> tier_styles;
		private AssociationElement element;
		private Entry<String, AssociationElement>[] file2types;
		private Entry<String, AssociationElement> entry;

		public TrackDefaultPrefTableModel() {
			this.tier_styles = new ArrayList<TrackStyle>();
		}

		public void setStyles(List<TrackStyle> tier_styles) {
			this.tier_styles = tier_styles;
		}

		public List<TrackStyle> getStyles() {
			return this.tier_styles;
		}

		@SuppressWarnings("unchecked")
		private void setElements(Map<String, AssociationElement> elements) {
			file2types = elements.entrySet().toArray(new Entry[elements.size()]);
			tier_styles.add(default_annot_style);
			for (Entry<String, AssociationElement> entries : file2types) {
				element = entries.getValue();

				addElement(entries.getKey().toString(), element);
			}
			fireTableDataChanged();
		}

		private void addElement(String filetype, AssociationElement element) {
			if (element.getPropertyMap() != null) {
				PropertyMap props = default_annot_style.getProperties();
				props.putAll(element.getPropertyMap());
				element.getPropertyMap().putAll(props);
				TrackStyle style = new TrackStyle(props);
				style.setTrackName(filetype);
				tier_styles.add(style);
			}
		}

		private void removeElement(String filetype) {
			Iterator<TrackStyle> iterator = tier_styles.iterator();
			TrackStyle style;
			while (iterator.hasNext()) {
				style = iterator.next();
				if (style.getTrackName().equals(filetype)) {
					iterator.remove();
				}
			}
		}

		// Allow editing most fields in normal rows, but don't allow editing some
		// fields in the "default" style row.
		@Override
		public boolean isCellEditable(int row, int column) {
			if (column == COL_TRACK_DEFAULT) {
				return false;
			}

			return true;
		}

		public int getRowCount() {
			return tier_styles.size();
		}

		@Override
		public String getColumnName(int columnIndex) {
			return col_headings[columnIndex];
		}

		public int getColumnCount() {
			return col_headings.length;
		}

		public Object getValueAt(int row, int column) {
			TrackStyle style = tier_styles.get(row);
			switch (column) {
				case COL_TRACK_DEFAULT:
					return style.getTrackName();
				case COL_FOREGROUND:
					return style.getForeground();
				case COL_BACKGROUND:
					return style.getBackground();
				case COL_LABEL_COLOR:
					return style.getLabelForeground();
				case COL_TRACK_NAME_SIZE:
					return style.getTrackNameSize();
				default:
					return null;
			}
		}

		@Override
		public Class<?> getColumnClass(int c) {
			Object val = getValueAt(0, c);
			if (val == null) {
				return Object.class;
			} else {
				return val.getClass();
			}
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			for (int i : selectedRows) {
				setValue(value, i, col);
			}
		}

		public void setValue(Object value, int row, int col) {
			applyChanged = false;
			if (value != null && !initializationDetector) {
				try {
					TrackStyle style = tier_styles.get(row);
					if (!style.equals(default_annot_style)) {
						entry = file2types[row - 1];
						element = entry.getValue();
					}
					switch (col) {
						case COL_FOREGROUND:
							if (!style.equals(default_annot_style)) {
								element.getPropertyMap().put(PROP_FOREGROUND, value);
							}
							style.setForeground((Color) value);
							fgColorComboBox.setSelectedColor((Color) value);
							break;
						case COL_BACKGROUND:
							if (!style.equals(default_annot_style)) {
								element.getPropertyMap().put(PROP_BACKGROUND, value);
							}
							style.setBackground((Color) value);
							bgColorComboBox.setSelectedColor((Color) value);
							break;
						case COL_TRACK_NAME_SIZE:
							if (!style.equals(default_annot_style)) {
								element.getPropertyMap().put(PROP_FONT_SIZE, value.toString());
								style.setTrackNameSize((Float) value);
							}
							style.setTrackNameSize((Float) value);
							trackNameSizeComboBox.setSelectedItem(value);
							break;
						case COL_LABEL_FIELD:
							if (!style.equals(default_annot_style)) {
								element.getPropertyMap().put(PROP_LABEL_FIELD, value.toString());
							}
							style.setLabelField((String) value);
							break;
						case COL_MAX_DEPTH: {
							int i = parseInteger(((String) value), 0, style.getMaxDepth());
							if (!style.equals(default_annot_style)) {
								element.getPropertyMap().put(PROP_MAX_DEPTH, value.toString());
							}
							style.setMaxDepth(i);
						}
						break;
						case COL_DIRECTION_TYPE:
							if (!style.equals(default_annot_style)) {
								element.getPropertyMap().put(PROP_DIRECTION_TYPE, value.toString());
							}
							style.setDirectionType((TrackConstants.DIRECTION_TYPE) value);
							break;
						case COL_SHOW_2_TRACKS:
							if (!style.equals(default_annot_style)) {
								element.getPropertyMap().put(PROP_SEPARATE, ((Object)(!(Boolean)value)).toString());
							}
							style.setSeparate(!(((Boolean) value).booleanValue()));
							break;
						case COL_COLLAPSED:
							if (!style.equals(default_annot_style)) {
								element.getPropertyMap().put(PROP_COLLAPSED, value.toString());
							}
							style.setCollapsed(((Boolean) value).booleanValue());
							break;
						case COL_POS_STRAND_COLOR:
							if (!style.equals(default_annot_style)) {
								element.getPropertyMap().put(PROP_START_COLOR, value);
							}
							style.setForwardColor((Color) value);
							break;
						case COL_NEG_STRAND_COLOR:
							if (!style.equals(default_annot_style)) {
								element.getPropertyMap().put(PROP_END_COLOR, value);
							}
							style.setReverseColor((Color) value);
							break;
						case COL_LABEL_COLOR:
							if (!style.equals(default_annot_style)) {
								element.getPropertyMap().put(PROP_LABEL_FOREGROUND, value);
							}
							style.setLabelForeground((Color) value);
							labelColorComboBox.setSelectedColor((Color) value);
							break;
						default:
							System.out.println("Unknown column selected: " + col);
					}
					fireTableCellUpdated(row, col);
				} catch (Exception e) {
					// exceptions should not happen, but must be caught if they do
					System.out.println("Exception in TierPrefsView.setValueAt(): " + e);
				}
			}
			applyChanged = true;
		}

		int parseInteger(String s, int empty_string, int fallback) {
			//System.out.println("Parsing string: '" + s + "'");
			int i = fallback;
			try {
				if ("".equals(s.trim())) {
					i = empty_string;
				} else {
					i = Integer.parseInt(s);
				}
			} catch (Exception e) {
				//System.out.println("Exception: " + e);
				// don't report the error, use the fallback value
			}
			return i;
		}
	}
}
