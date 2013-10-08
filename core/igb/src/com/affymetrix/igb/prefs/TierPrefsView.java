package com.affymetrix.igb.prefs;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.symmetry.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.genoviz.swing.recordplayback.JRPTextField;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor.TrackStylePropertyListener;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TrackConstants;
import com.affymetrix.igb.tiers.TrackConstants.DIRECTION_TYPE;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.view.AltSpliceView;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author nick
 */
public class TierPrefsView extends TrackPreferences implements ListSelectionListener, TrackStylePropertyListener {

	public static final long serialVersionUID = 1l;
	private static TierPrefsView singleton;
	public static final String TRACK_NAME = "Track Name";
	public static final String FOREGROUND = "FG";
	public static final String BACKGROUND = "BG";
	public static final String TRACK_NAME_SIZE = "Size";
	public final static String[] col_headings = {
		TRACK_NAME,
		BACKGROUND, FOREGROUND,
		TRACK_NAME_SIZE //    GRAPH_TIER,
	};
	//subclass variables
	public static final int COL_TRACK_NAME = 0;
	public static final int COL_VIEW_MODE = 12;
	public static final String PREF_AUTO_REFRESH = "Auto-Apply Track Customizer Changes";
	public static final boolean default_auto_refresh = true;
	public static final String AUTO_REFRESH = "Auto Refresh";
	public ListSelectionModel lsm;
	public SeqMapView smv;
	public List<TierLabelGlyph> selectedTiers;
	public int selectedRow;
	public List<TierGlyph> currentTiers;
	public List<TrackStyle> currentStyles;
	public JRPTextField displayNameTextField;
	public JButton applyDisplayNameButton;
	public ButtonGroup showStrandButtonGroup;
	public JLabel applyToAllTip;

	public static synchronized TierPrefsView getSingleton() {
		if (singleton == null) {
			singleton = new TierPrefsView();
			singleton.model.addTableModelListener(TrackstylePropertyMonitor.getPropertyTracker());
			TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(singleton);
		}
		return singleton;
	}

	private TierPrefsView() {
		title = "Track List";
		b1Text = "Select All";
		b2Text = "Restore to Default";
		track = "Track Name";
		Application igb = Application.getSingleton();
		if (igb != null) {
			smv = igb.getMapView();
		}
		initCommonComponents();
		initComponents();
	}

	private void initComponents() {
		model = new TierPrefsTableModel();

		initTable();

		displayNameTextField = new JRPTextField("TierPrefsView_displayNameTextField");
		showStrandButtonGroup = new javax.swing.ButtonGroup();
		refreshButton = new JRPButton("TierPrefsView_refreshButton");

		setEnabled(false);

		trackNameSizeComboBox.setModel(new DefaultComboBoxModel(TrackConstants.SUPPORTED_SIZE));

		labelFieldComboBox.setModel(new DefaultComboBoxModel(TrackConstants.LABELFIELD));

		ImageIcon infoIcon = CommonUtils.getInstance().getIcon("16x16/actions/info.png");

		applyToAllTip = new javax.swing.JLabel();
		applyToAllTip.setToolTipText("Apply Background, Foreground, and Name Size to all tracks.");
		applyToAllTip.setIcon(infoIcon);

		// Add a "refresh map" button, if there is an instance of IGB
		if (smv != null) {
			refreshButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent evt) {
					refreshSeqMapViewAndSlicedView();
				}
			});

			autoRefreshCheckBox = PreferenceUtils.createCheckBox(AUTO_REFRESH, PREF_AUTO_REFRESH,
					default_auto_refresh);
			autoRefreshCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent evt) {
					if (refreshButton != null) {
						refreshButton.setEnabled(!autoRefreshCheckBox.isSelected());
						if (autoRefreshCheckBox.isSelected()) {
							refreshSeqMapViewAndSlicedView();
						}
					}
				}
			});
			refreshButton.setEnabled(!autoRefreshCheckBox.isSelected());
		}
	}

	public void setTier_label_glyphs(List<TierLabelGlyph> tier_label_glyphs) {
		selectedTiers = tier_label_glyphs;

		if (table.getRowCount() > 0) {
			//set Selected Rows
			ITrackStyle style;
			TierGlyph tier;
			table.removeRowSelectionInterval(0, table.getRowCount() - 1);
			for (TierLabelGlyph tlg : selectedTiers) {
				tier = (TierGlyph) tlg.getInfo();
				style = tier.getAnnotStyle();

				setRowSelection(style);
			}
			table.setColumnSelectionInterval(0, 0);
			table.requestFocus();
		}
	}

	private void setRowSelection(ITrackStyle style) {
		for (int i = 0; i < table.getRowCount(); i++) {
			if (model != null && style != null) {
				if (model.getValueAt(i, 0).equals(style.getTrackName())) {
					table.addRowSelectionInterval(i, i);
				}
			}
		}
	}

	public JTextField getTrackDefaultTextField() {
		return displayNameTextField;
	}

	/**
	 * Whether or not changes to the trackOptionsTable should automatically be
	 * applied to the view.
	 */
	public boolean autoApplyChanges() {
		return PreferenceUtils.getBooleanParam(PREF_AUTO_REFRESH,
				default_auto_refresh);
	}

	public void refreshSeqMapViewAndSlicedView() {
		refreshSeqMapView();

		AltSpliceView.getSingleton().refreshView();
	}

	public void refreshSeqMapView() {
		if (smv != null) {
			smv.updatePanel();
		}
	}

	public void refreshList() {
		if (currentStyles == null) {
			currentStyles = new ArrayList<TrackStyle>();
		}

		boolean isContained = true; // If add a new track, its style will not contain...

		if (smv != null) {
			currentTiers = smv.getSeqMap().getTiers();
			List<TrackStyle> styles = new ArrayList<TrackStyle>();
			Iterator<TierGlyph> titer = currentTiers.iterator();
			while (titer.hasNext()) {
				TierGlyph tier = titer.next();
				ITrackStyle style = tier.getAnnotStyle();

				if (!currentStyles.contains(style)) {
					isContained = false;
				}

				if (style instanceof TrackStyle
						&& style.getShow()
						&& tier.getChildCount() > 0) { // Added on Sep 17, 2012 to fix bug - track prefs out of sync
					styles.add((TrackStyle) style);
				}
			}

			currentStyles.clear();
			currentStyles.addAll(styles);
		}

		Set<TrackStyle> customizables = new HashSet<TrackStyle>(currentStyles.size());
		for (int i = 0; i < currentStyles.size(); i++) {
			TrackStyle the_style = currentStyles.get(i);
			if (the_style.getCustomizable()) {
				// if graph tier style then only include if include_graph_styles toggle is set (app is _not_ IGB)
				//if ((!the_style.isGraphTier())) {
				customizables.add(the_style);
				//}
			}
		}

		if (!isContained || ((TierPrefsTableModel) model).getStyles().size() > currentStyles.size()) {
			((TierPrefsTableModel) model).setStyles(customizables);
			model.fireTableDataChanged();
		}
	}

	public void clearTable() {
		((TierPrefsTableModel) model).clear();
	}

	private void setEnabled(boolean b) {
		displayNameTextField.setEnabled(b);
		displayNameTextField.setEditable(b);
		labelFieldComboBox.setEnabled(b);
		maxDepthTextField.setEnabled(b);
		collapsedCheckBox.setEnabled(b);
		colorCheckBox.setEnabled(b);
		arrowCheckBox.setEnabled(b);
		possitiveColorComboBox.setEnabled(b);
		negativeColorComboBox.setEnabled(b);
		show2TracksCheckBox.setEnabled(b);
		bgColorComboBox.setEnabled(b);
		fgColorComboBox.setEnabled(b);
		labelColorComboBox.setEnabled(b);
		trackNameSizeComboBox.setEnabled(b);
		applyButton.setEnabled(b);
	}

	/**
	 * Called when the user selects a row of the table.
	 *
	 * @param evt
	 */
	@Override
	public void valueChanged(ListSelectionEvent evt) {
		selectedRows = table.getSelectedRows();

		initializationDetector = true;
		applyChanged = false;

		setEnabled(true);

		if (selectedRows.length > 1 && ((TierPrefsTableModel) model).getStyles().size() >= selectedRows.length) {
			resetValueBySelectedRows();
		} else if (selectedRows.length == 1 && ((TierPrefsTableModel) model).getStyles().size() >= selectedRows[0]) {
			resetValueBySelectedRow();
		} else {
			setEnabled(false);
		}

		applyChanged = true;
		initializationDetector = false;
	}

	private void resetValueBySelectedRows() {
		
		setEnableByAxisOrGraphForRows(((TierPrefsTableModel) model).getStyles());
		
		displayNameTextField.setText("");
		displayNameTextField.setEnabled(false);

		bgColorComboBox.setSelectedColor((Color) getValueAt(COL_BACKGROUND));
		fgColorComboBox.setSelectedColor((Color) getValueAt(COL_FOREGROUND));
		labelColorComboBox.setSelectedColor((Color) getValueAt(COL_LABEL_COLOR));
		trackNameSizeComboBox.setSelectedItem(getValueAt(COL_TRACK_NAME_SIZE));
		labelFieldComboBox.setSelectedItem(getValueAt(COL_LABEL_FIELD));
		maxDepthTextField.setText(String.valueOf(getValueAt(COL_MAX_DEPTH)));
		show2TracksCheckBox.setSelected(!(Boolean) getValueAt(COL_SHOW_2_TRACKS));
		collapsedCheckBox.setSelected((Boolean) getValueAt(COL_COLLAPSED));

		DIRECTION_TYPE type = (DIRECTION_TYPE) getValueAt(COL_DIRECTION_TYPE);
		if (type == null) {
			colorCheckBox.setSelected(false);
			arrowCheckBox.setSelected(false);
		} else {
			setSelectedByDirection(type);
		}

		possitiveColorComboBox.setSelectedColor((Color) getValueAt(COL_POS_STRAND_COLOR));
		negativeColorComboBox.setSelectedColor((Color) getValueAt(COL_NEG_STRAND_COLOR));

	}

	/**
	 * If one of the values in the same column is different, return null
	 * Otherwise return value
	 *
	 * @param col
	 */
	private Object getValueAt(int col) {
		TrackStyle style, temp;
		Object object = null;

		for (int i = 0; i < selectedRows.length - 1; i++) {
			style = ((TierPrefsTableModel) model).getStyles().get(selectedRows[i]);
			temp = ((TierPrefsTableModel) model).getStyles().get(selectedRows[i + 1]);

			switch (col) {
				case COL_BACKGROUND:
					object = checkBG(style, temp);
					if (object == null) {
						break;
					}
					break;
				case COL_FOREGROUND:
					object = checkFG(style, temp);
					if (object == null) {
						break;
					}
					break;
				case COL_TRACK_NAME_SIZE:
					object = checkSize(style, temp);
					if (object == null) {
						break;
					}
					break;
				case COL_LABEL_FIELD:
					object = checkLabel(style, temp);
					if (object == null) {
						break;
					}
					break;
				case COL_MAX_DEPTH:
					object = checkDepth(style, temp);
					if (object == null) {
						object = "";
						break;
					}
					break;
				case COL_SHOW_2_TRACKS:
					object = check2Tracks(style, temp);
					if (object == null) {
						object = Boolean.FALSE;
						break;
					}
					break;
				case COL_COLLAPSED:
					object = checkCollapsed(style, temp);
					if (object == null) {
						object = Boolean.FALSE;
						break;
					}
					break;
				case COL_DIRECTION_TYPE:
					object = checkDirection(style, temp);
					if (object == null) {
						break;
					}
					break;
				case COL_POS_STRAND_COLOR:
					object = checkForwardColor(style, temp);
					if (object == null) {
						break;
					}
					break;
				case COL_NEG_STRAND_COLOR:
					object = checkReverseColor(style, temp);
					if (object == null) {
						break;
					}
					break;
				case COL_LABEL_COLOR:
					object = checkLabelColor(style, temp);
					if (object == null) {
						break;
					}
					break;
			}
		}

		return object;
	}

	private Color checkBG(TrackStyle style, TrackStyle temp) {
		Color value = style.getBackground();
		if (!value.equals(temp.getBackground())) {
			return null;
		}

		return value;
	}

	private Color checkFG(TrackStyle style, TrackStyle temp) {
		Color value = style.getForeground();
		if (!value.equals(temp.getForeground())) {
			return null;
		}

		return value;
	}
	
	private Color checkLabelColor(TrackStyle style, TrackStyle temp) {
		Color value = style.getLabelForeground();
		if (!value.equals(temp.getLabelForeground())) {
			return null;
		}

		return value;
	}

	private Float checkSize(TrackStyle style, TrackStyle temp) {
		Float value = style.getTrackNameSize();
		if (value != temp.getTrackNameSize()) {
			return null;
		}

		return value;
	}

	private String checkLabel(TrackStyle style, TrackStyle temp) {
		String value = style.getLabelField();
		if (!value.equals(temp.getLabelField())) {
			return null;
		}

		return value;
	}

	private Integer checkDepth(TrackStyle style, TrackStyle temp) {
		Integer value = style.getMaxDepth();
		if (value != temp.getMaxDepth()) {
			return null;
		}

		return value;
	}

	private Boolean check2Tracks(TrackStyle style, TrackStyle temp) {
		Boolean value = (style.isGraphTier() || !style.getSeparable()) ? false : style.getSeparate();
		if (value != temp.getSeparate()) {
			return null;
		}

		return value;
	}

	private Boolean checkCollapsed(TrackStyle style, TrackStyle temp) {
		Boolean value = style.getCollapsed();
		if (value != temp.getCollapsed()) {
			return null;
		}

		return value;
	}

	private DIRECTION_TYPE checkDirection(TrackStyle style, TrackStyle temp) {
		DIRECTION_TYPE value = style.getDirectionName();
		if (!value.equals(temp.getDirectionName())) {
			return null;
		}

		return value;
	}

	private Color checkForwardColor(TrackStyle style, TrackStyle temp) {
		Color value = style.getForwardColor();
		if (!value.equals(temp.getForwardColor())) {
			return null;
		}

		return value;
	}

	private Color checkReverseColor(TrackStyle style, TrackStyle temp) {
		Color value = style.getReverseColor();
		if (!value.equals(temp.getReverseColor())) {
			return null;
		}

		return value;
	}

	private void resetValueBySelectedRow() {
		TrackStyle style = ((TierPrefsTableModel) model).getStyles().get(selectedRows[0]);

		setEnabledByAxisOrGraph(style);

		possitiveColorComboBox.setSelectedColor(style.getForwardColor());
		negativeColorComboBox.setSelectedColor(style.getReverseColor());

		displayNameTextField.setText(style.getTrackName());
		bgColorComboBox.setSelectedColor(style.getBackground());
		fgColorComboBox.setSelectedColor(style.getForeground());
		labelColorComboBox.setSelectedColor(style.getLabelForeground());
		trackNameSizeComboBox.setSelectedItem(style.getTrackNameSize());
		labelFieldComboBox.setSelectedItem(style.getLabelField());
		maxDepthTextField.setText((style.getTrackName().equalsIgnoreCase(TrackConstants.NAME_OF_COORDINATE_INSTANCE) || style.isGraphTier()) ? "" : String.valueOf(style.getMaxDepth()));
		show2TracksCheckBox.setSelected(!(style.getTrackName().equalsIgnoreCase(TrackConstants.NAME_OF_COORDINATE_INSTANCE) || ((style.isGraphTier() || !style.getSeparable()) ? false : style.getSeparate())));
		collapsedCheckBox.setSelected(style.getCollapsed());

		setSelectedByDirection(style.getDirectionName());
	}

	private void setSelectedByDirection(DIRECTION_TYPE direction) {
		switch (direction) {
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
				System.out.println("Wrong Direction Type");
		}
	}

	private void setEnabledByAxisOrGraph(TrackStyle style) {
		if (style.getTrackName().equalsIgnoreCase(TrackConstants.NAME_OF_COORDINATE_INSTANCE)
				|| (style.isGraphTier() || !style.getSeparable())) {
			if(style.isGraphTier() || style.getTrackName().equalsIgnoreCase(TrackConstants.NAME_OF_COORDINATE_INSTANCE)){
				maxDepthTextField.setEnabled(false);
				applyButton.setEnabled(false);
				collapsedCheckBox.setEnabled(false);
				if(style.getTrackName().equalsIgnoreCase(TrackConstants.NAME_OF_COORDINATE_INSTANCE)){
					displayNameTextField.setEnabled(false);
				}
			}
			
			labelFieldComboBox.setEnabled(false);
			colorCheckBox.setEnabled(false);
			arrowCheckBox.setEnabled(false);
			possitiveColorComboBox.setEnabled(false);
			negativeColorComboBox.setEnabled(false);
			show2TracksCheckBox.setEnabled(false);
		} else {
			resetLabelField(style);
		}
	}
	
	private void setEnableByAxisOrGraphForRows(List<TrackStyle> rows){

		for (int i = 0; i < selectedRows.length; i++) {
			TrackStyle style = rows.get(selectedRows[i]);
			setEnabledByAxisOrGraph(style);
		}
	}

	private void resetLabelField(TrackStyle style) {
		if (style != null && smv != null) {
			if (smv.getAnnotatedSeq() != null) { //Fixes NPE
				SeqSymmetry sym = smv.getAnnotatedSeq().getAnnotation(style.getMethodName());
				if (sym != null && sym.getChildCount() > 0) {
					SeqSymmetry child = sym.getChild(0);
					SeqSymmetry original = getMostOriginalSymmetry(child);
					if (original instanceof SymWithProps) {
						Map<String, Object> props = ((SymWithProps) original).getProperties();

						ArrayList<String> properties = new ArrayList<String>();
						properties.add((String) TrackConstants.LABELFIELD[0]);
						properties.addAll(props.keySet());
						labelFieldComboBox.setModel(new DefaultComboBoxModel(properties.toArray()));
					}
				}
			}
		}
	}

	private static SeqSymmetry getMostOriginalSymmetry(SeqSymmetry sym) {
		if (sym instanceof DerivedSeqSymmetry) {
			return getMostOriginalSymmetry(((DerivedSeqSymmetry) sym).getOriginalSymmetry());
		}
		return sym;
	}

	/**
	 * Call this whenver this component is removed from the view, due to the tab
	 * pane closing or the window closing. It will decide whether it is
	 * necessary to update the SeqMapView in response to changes in settings in
	 * this panel.
	 */
	public void removedFromView() {
		// if autoApplyChanges(), then the changes were already applied,
		// otherwise apply changes as needed.
		if (!autoApplyChanges()) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					refreshSeqMapViewAndSlicedView();
				}
			});
		}
	}

	public void selectAll() {
		if (table.getRowCount() > 1) {
			table.setRowSelectionInterval(0, table.getRowCount() - 1);
			table.setColumnSelectionInterval(0, 0);
			for(int i=0;i<table.getRowCount();i++){
				if(!table.isCellEditable(i, 0)){
					ListSelectionModel selModel = table.getSelectionModel();
					selModel.removeSelectionInterval(i, i);
					break;
				}
			}
			table.requestFocus();
		}
	}

	public void displayNameTextField() {
		if (applyChanged) {
			model.setValueAt(displayNameTextField.getText(), 0, COL_TRACK_NAME);
		}
	}

	@Override
	public void trackNameSizeComboBox() {
		if (applyChanged) {
			Float trackNameSize = null;

			if (trackNameSizeComboBox.getSelectedItem() != null) {
				trackNameSize = Float.parseFloat(trackNameSizeComboBox.getSelectedItem().toString());
			}

			model.setValueAt(trackNameSize, 0, COL_TRACK_NAME_SIZE);
		}
	}

	public void restoreToDefault() {
		if (selectedRows != null) { //Prevents NPE when no tracks have been generated.
			int row;
			TrackStyle style;
			int[] previousSelectedRows = selectedRows;
			for (int i = 0; i < selectedRows.length; i++) {
				row = selectedRows[i];
				style = ((TierPrefsTableModel) model).getStyles().get(row);
				style.restoreToDefault();
			}

			model.fireTableDataChanged();

			refreshSeqMapViewAndSlicedView();
			smv.getPopup().refreshMap(false, true);
			if(previousSelectedRows.length >= 1)
				table.setRowSelectionInterval(previousSelectedRows[0], previousSelectedRows[0]);
		}
	}

	public void trackstylePropertyChanged(EventObject eo) {
		if (eo.getSource() == table.getModel()) {
			return;
		}

		table.repaint();
		valueChanged(null);
	}

	class TierPrefsTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		List<TrackStyle> tier_styles;
		private Object tempObject;
		private int tempInt;

		TierPrefsTableModel() {
			this.tier_styles = new ArrayList<TrackStyle>();
		}

		public void setStyles(Collection<TrackStyle> tier_styles) {
			this.tier_styles.clear();
			this.tier_styles.addAll(tier_styles);
		}

		public List<TrackStyle> getStyles() {
			return this.tier_styles;
		}

		public void clear() {
			for (Iterator<TrackStyle> iter = tier_styles.iterator(); iter.hasNext();) {
				TrackStyle style = iter.next();
				if (!style.getTrackName().equals(TrackConstants.NAME_OF_COORDINATE_INSTANCE)) {
					iter.remove();
				}
			}
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			TrackStyle style = tier_styles.get(row);
			if(style.getTrackName().equals(TrackConstants.NAME_OF_COORDINATE_INSTANCE) && (column == COL_TRACK_NAME)){
				return false;
			}
			//if (column == COL_TRACK_NAME) {
			//	return false;
			//}

			return true;
		}

		@Override
		public Class<?> getColumnClass(int c) {
			tempObject = getValueAt(0, c);
			if (tempObject == null) {
				return Object.class;
			} else {
				return tempObject.getClass();
			}
		}

		public int getColumnCount() {
			return col_headings.length;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return col_headings[columnIndex];
		}

		public int getRowCount() {
			return tier_styles.size();
		}

		public Object getValueAt(int row, int column) {
			// set all components unable to edit, if no track has been selected.
			if (table.getSelectedRow() == -1
					&& row == 0 && column == 0) {
				setEnabled(false);
			}

			TrackStyle style;
			style = tier_styles.get(row);
			switch (column) {
				case COL_FOREGROUND:
					return style.getForeground();
				case COL_BACKGROUND:
					return style.getBackground();
				case COL_LABEL_COLOR:
					return style.getLabelForeground();
				case COL_TRACK_NAME_SIZE:
					return style.getTrackNameSize();
				case COL_TRACK_NAME:
					return style.getTrackName();
				default:
					return null;
			}
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			for (int i : selectedRows) {
				// Do refresh sequence map view after changing the last selected row.
				if (i == selectedRows[selectedRows.length - 1]) {
					setValueAt(value, i, col, true);
				} else {
					setValueAt(value, i, col, false);
				}
			}
		}

		public void setValueAt(Object value, int row, int col, boolean apply) {
			applyChanged = false;
			TrackStyle style = tier_styles.get(row);
			if (value != null && !initializationDetector) {
				try {
					switch (col) {
						case COL_TRACK_NAME:
							//Test prevents a bug allowing Coordinate track to be renamed when multi-selecting
							if (!style.getTrackName().equalsIgnoreCase(TrackConstants.NAME_OF_COORDINATE_INSTANCE)) {
								style.setTrackName((String) value);
								displayNameTextField.setText((String) value);
							}
							break;
						case COL_FOREGROUND:
							style.setForeground((Color) value);
							fgColorComboBox.setSelectedColor((Color) value);
							break;
						case COL_BACKGROUND:
							style.setBackground((Color) value);
							bgColorComboBox.setSelectedColor((Color) value);
							break;
						case COL_TRACK_NAME_SIZE:
							style.setTrackNameSize((Float) value);
							trackNameSizeComboBox.setSelectedItem(value);
							break;
						case COL_LABEL_FIELD:
							style.setLabelField((String) value);
							labelFieldComboBox.setSelectedItem(value);
							break;
						case COL_MAX_DEPTH: {
							tempInt = parseInteger(((String) value), 0, style.getMaxDepth());
							style.setMaxDepth(tempInt);
						}
						break;
						case COL_DIRECTION_TYPE:
							style.setDirectionType((TrackConstants.DIRECTION_TYPE) value);
							break;
						case COL_SHOW_2_TRACKS:
							style.setSeparate(!(((Boolean) value).booleanValue()));
							smv.getPopup().refreshMap(false, true);
							break;
						case COL_COLLAPSED:
							style.setCollapsed(((Boolean) value).booleanValue());
							break;
						case COL_POS_STRAND_COLOR:
							style.setForwardColor((Color) value);
							break;
						case COL_NEG_STRAND_COLOR:
							style.setReverseColor((Color) value);
							break;
						case COL_LABEL_COLOR:
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

				if ((autoApplyChanges() && apply)) {
					update(col);
				}
			}
			setRowSelection(style);
			applyChanged = true;
		}

		public void update(int col) {
			if (col == COL_BACKGROUND || col == COL_TRACK_NAME_SIZE
					|| col == COL_TRACK_NAME || col == COL_COLLAPSED
					|| col == COL_MAX_DEPTH) {
				if (col == COL_COLLAPSED || col == COL_MAX_DEPTH) {
					smv.getSeqMap().setTierStyles();
					smv.getSeqMap().repackTheTiers(true, true);
				}

				smv.getSeqMap().updateWidget();
			} else {
				refreshSeqMapViewAndSlicedView();
			}
		}

		/**
		 * Parse an integer, using the given fallback if any exception occurrs.
		 *
		 * @param s The String to parse.
		 * @param empty_string the value to return if the input is an empty
		 * string.
		 * @param fallback the value to return if the input String is
		 * unparseable.
		 */
		int parseInteger(String s, int empty_string, int fallback) {
			try {
				if ("".equals(s.trim())) {
					return empty_string;
				} else {
					return Integer.parseInt(s);
				}
			} catch (Exception e) {
				//System.out.println("Exception: " + e);
				// don't report the error, use the fallback value
			}
			return fallback;
		}
	};
}
