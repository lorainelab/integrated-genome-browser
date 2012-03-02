package com.affymetrix.igb.prefs;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.symmetry.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.BooleanTableCellRenderer;
import com.affymetrix.genoviz.swing.ColorTableCellRenderer;
import com.affymetrix.genoviz.swing.StyledJTable;
import com.affymetrix.genoviz.swing.recordplayback.*;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.prefs.TierPrefsView.TierPrefsTableModel;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor.TrackStylePropertyListener;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TrackConstants;
import com.affymetrix.igb.tiers.TrackConstants.DIRECTION_TYPE;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.viewmode.MapViewModeHolder;
import com.jidesoft.combobox.ColorComboBox;
import com.jidesoft.grid.ColorCellEditor;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 *
 * @author nick
 */
public class TierPrefsView extends TrackPreferences implements ListSelectionListener, TrackStylePropertyListener {

	public static final long serialVersionUID = 1l;
	private static TierPrefsView singleton;
	public static final String TRACK_NAME = "Track Name";
	public static final String FOREGROUND = "Foreground";
	public static final String BACKGROUND = "Background";
	public static final String TRACK_NAME_SIZE = "Name Size";
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
	public static StyledJTable table;
	public SeqMapView smv;
	public List<TierLabelGlyph> selectedTiers;
	public int selectedRow;
	public TrackStyle selectedStyle;
	public List<TierGlyph> currentTiers;
	public List<TrackStyle> currentStyles;
	public JRPButton applyToAllButton;
	public JCheckBox autoRefreshCheckBox;
	public JRPTextField displayNameTextField;
	public JButton applyDisplayNameButton;
	public JRPButton refreshButton;
	public ButtonGroup showStrandButtonGroup;
	public JRPComboBox viewModeCB;
	public JLabel applyToAllTip;

	public static synchronized TierPrefsView getSingleton() {
		if (singleton == null) {
			singleton = new TierPrefsView();
			singleton.model.addTableModelListener(TrackstylePropertyMonitor.getPropertyTracker());
			TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(singleton);
		}
		return singleton;
	}

	public TierPrefsView() {
		Application igb = Application.getSingleton();
		if (igb != null) {
			smv = igb.getMapView();
		}

		initComponents();
	}
	
	private void initComponents() {
		model = new TierPrefsTableModel();

		initTable();

		displayNameTextField = new JRPTextField("TierPrefsView_displayNameTextField");
		applyDisplayNameButton = new JRPButton("TierPrefsView_applyDisplayNameButton");
		fgColorComboBox = new ColorComboBox();
		bgColorComboBox = new ColorComboBox();
		trackNameSizeComboBox = new JRPComboBox("TierPrefsView_trackNameSizeComboBox");
		maxDepthTextField = new JRPNumTextField("TierPrefsView_maxDepthTextField");
		labelFieldComboBox = new JRPComboBox("TierPrefsView_labelFieldComboBox");
		show2TracksCheckBox = new JRPCheckBox("TierPrefsView_show2TracksCheckBox");
		connectedCheckBox = new JRPCheckBox("TierPrefsView_connectedCheckBox");
		collapsedCheckBox = new JRPCheckBox("TierPrefsView_collapsedCheckBox");
		showStrandButtonGroup = new javax.swing.ButtonGroup();
		possitiveColorComboBox = new ColorComboBox();
		negativeColorComboBox = new ColorComboBox();
		colorCheckBox = new JRPCheckBox("TierPrefsView_colorCheckBox");
		arrowCheckBox = new JRPCheckBox("TierPrefsView_arrowCheckBox");
		viewModeCB = new JRPComboBox("TierPrefsView_viewModeCB");
		applyToAllButton = new JRPButton("TierPrefsView_applyToAllButton");
		refreshButton = new JRPButton("TierPrefsView_refreshButton");

		displayNameTextField.setEnabled(false);
		applyDisplayNameButton.setEnabled(false);
		bgColorComboBox.setEnabled(false);
		fgColorComboBox.setEnabled(false);
		trackNameSizeComboBox.setEnabled(false);
		labelFieldComboBox.setEnabled(false);
		maxDepthTextField.setEnabled(false);
		show2TracksCheckBox.setEnabled(false);
		connectedCheckBox.setEnabled(false);
		collapsedCheckBox.setEnabled(false);
		colorCheckBox.setEnabled(false);
		arrowCheckBox.setEnabled(false);
		possitiveColorComboBox.setEnabled(false);
		negativeColorComboBox.setEnabled(false);
		viewModeCB.setEnabled(false);
		applyToAllButton.setEnabled(false);

		trackNameSizeComboBox.setModel(new DefaultComboBoxModel(TrackConstants.SUPPORTED_SIZE));

		labelFieldComboBox.setModel(new DefaultComboBoxModel(TrackConstants.LABELFIELD));

		viewModeCB.setModel(new DefaultComboBoxModel(TrackConstants.VIEWMODE));

		ImageIcon infoIcon = CommonUtils.getInstance().getIcon("images/info.png");

		applyToAllTip = new javax.swing.JLabel();
		applyToAllTip.setToolTipText("Apply Background, Foreground, and Name Size to all tracks.");
		applyToAllTip.setIcon(infoIcon);

		// Add a "refresh map" button, if there is an instance of IGB
		if (smv != null) {
			refreshButton.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent evt) {
					refreshSeqMapView();
				}
			});

			autoRefreshCheckBox = PreferenceUtils.createCheckBox(AUTO_REFRESH,
					PreferenceUtils.getTopNode(), PREF_AUTO_REFRESH,
					default_auto_refresh);
			autoRefreshCheckBox.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent evt) {
					if (refreshButton != null) {
						refreshButton.setEnabled(!autoRefreshCheckBox.isSelected());
						if (autoRefreshCheckBox.isSelected()) {
							refreshSeqMapView();
						}
					}
				}
			});
			refreshButton.setEnabled(!autoRefreshCheckBox.isSelected());
		}
	}

	private void initTable() {
		table = new StyledJTable(model);
		table.list.add(TierPrefsView.COL_BACKGROUND);
		table.list.add(TierPrefsView.COL_FOREGROUND);
		
		lsm = table.getSelectionModel();
		lsm.addListSelectionListener(this);
		lsm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		table.setRowSelectionAllowed(true);

		ColorCellEditor cellEditor = new ColorCellEditor() {

			private static final long serialVersionUID = 1L;

			@Override
			protected ColorComboBox createColorComboBox() {
				final ColorComboBox combobox = new ColorComboBox();
				combobox.setColorValueVisible(false);
				combobox.setCrossBackGroundStyle(false);
				combobox.setButtonVisible(false);
				combobox.setStretchToFit(true);
				return combobox;
			}
		};
		table.setDefaultRenderer(Color.class, new ColorTableCellRenderer());
		table.setDefaultEditor(Color.class, cellEditor);
		table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
		table.setDefaultEditor(Float.class, new DefaultCellEditor(new JComboBox(TrackConstants.SUPPORTED_SIZE)));
		table.setDefaultEditor(TrackConstants.DIRECTION_TYPE.class, new DefaultCellEditor(new JComboBox(TrackConstants.DIRECTION_TYPE.values())));

		table.getColumnModel().getColumn(COL_FOREGROUND).setPreferredWidth(80);
		table.getColumnModel().getColumn(COL_FOREGROUND).setMinWidth(80);
		table.getColumnModel().getColumn(COL_FOREGROUND).setMaxWidth(80);
		table.getColumnModel().getColumn(COL_BACKGROUND).setPreferredWidth(82);
		table.getColumnModel().getColumn(COL_BACKGROUND).setMinWidth(82);
		table.getColumnModel().getColumn(COL_BACKGROUND).setMaxWidth(82);
		table.getColumnModel().getColumn(COL_TRACK_NAME_SIZE).setPreferredWidth(110);
		table.getColumnModel().getColumn(COL_TRACK_NAME_SIZE).setMinWidth(110);
		table.getColumnModel().getColumn(COL_TRACK_NAME_SIZE).setMaxWidth(110);

		Font f = new Font("SansSerif", Font.BOLD, 12);
		table.getTableHeader().setFont(f);
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
		}
	}

	public void setRowSelection(ITrackStyle style) {
		for (int i = 0; i < table.getRowCount(); i++) {
			if (model != null && style != null) {
				if (model.getValueAt(i, 0).equals(style.getTrackName())) {
					table.addRowSelectionInterval(i, i);
				}
			}
		}
	}

	/**
	 * Whether or not changes to the trackOptionsTable should automatically be
	 * applied to the view.
	 */
	public boolean autoApplyChanges() {
		return PreferenceUtils.getBooleanParam(PREF_AUTO_REFRESH,
				default_auto_refresh);
	}

	public void refreshSeqMapView() {
		if (smv != null) {
			smv.setAnnotatedSeq(smv.getAnnotatedSeq(), true, true, true);
		}
	}

	public void refreshList() {
		if (currentStyles == null) {
			currentStyles = new ArrayList<TrackStyle>();
		}

		boolean isContained = true; // If add a new track, its style will not contain...

		if (smv != null) {
			currentTiers = smv.getSeqMap().getTiers();
			LinkedHashMap<TrackStyle, TrackStyle> stylemap = new LinkedHashMap<TrackStyle, TrackStyle>();
			Iterator<TierGlyph> titer = currentTiers.iterator();
			while (titer.hasNext()) {
				TierGlyph tier = titer.next();
				ITrackStyle style = tier.getAnnotStyle();

				if (!currentStyles.contains(style)) {
					isContained = false;
				}

				if (style instanceof TrackStyle
						&& style.getShow()
						&& tier.getChildCount() > 0) {
					stylemap.put((TrackStyle) style, (TrackStyle) style);
				}
			}

			currentStyles.clear();
			currentStyles.addAll(stylemap.values());
		}

		ArrayList<TrackStyle> customizables = new ArrayList<TrackStyle>(currentStyles.size());
		for (int i = 0; i < currentStyles.size(); i++) {
			TrackStyle the_style = currentStyles.get(i);
			if (the_style.getCustomizable()) {
				// if graph tier style then only include if include_graph_styles toggle is set (app is _not_ IGB)
				//if ((!the_style.isGraphTier())) {
				customizables.add(the_style);
				//}
			}
		}

		if (!isContained) {
			((TierPrefsTableModel)model).setStyles(customizables);
			model.fireTableDataChanged();
		}
	}

	private void updateTable() {
		model.fireTableDataChanged();
	}

	public void clearTable() {
		((TierPrefsTableModel)model).clear();
		//also refresh options panel to ensure coordinate track colors stay synchronized
		OtherOptionsView.getSingleton().refresh();
	}

	/**
	 * Called when the user selects a row of the table.
	 *
	 * @param evt
	 */
	@Override
	public void valueChanged(ListSelectionEvent evt) {
		displayNameTextField.setEnabled(true);
		applyDisplayNameButton.setEnabled(true);
		viewModeCB.setEnabled(true);
		labelFieldComboBox.setEnabled(true);
		maxDepthTextField.setEnabled(true);
		connectedCheckBox.setEnabled(true);
		collapsedCheckBox.setEnabled(true);
		colorCheckBox.setEnabled(true);
		arrowCheckBox.setEnabled(true);
		possitiveColorComboBox.setEnabled(true);
		negativeColorComboBox.setEnabled(true);
		show2TracksCheckBox.setEnabled(true);
		bgColorComboBox.setEnabled(true);
		fgColorComboBox.setEnabled(true);
		trackNameSizeComboBox.setEnabled(true);
		applyToAllButton.setEnabled(true);
		labelFieldComboBox.setEnabled(true);
		maxDepthTextField.setEnabled(true);
		selectedRows = table.getSelectedRows();

		initializationDetector = true;

		if (table.getRowCount() == 0) {
			displayNameTextField.setEnabled(false);
			applyDisplayNameButton.setEnabled(false);
			viewModeCB.setEnabled(false);
			labelFieldComboBox.setEnabled(false);
			maxDepthTextField.setEnabled(false);
			connectedCheckBox.setEnabled(false);
			collapsedCheckBox.setEnabled(false);
			colorCheckBox.setEnabled(false);
			arrowCheckBox.setEnabled(false);
			possitiveColorComboBox.setEnabled(false);
			negativeColorComboBox.setEnabled(false);
			show2TracksCheckBox.setEnabled(false);
			bgColorComboBox.setEnabled(false);
			fgColorComboBox.setEnabled(false);
			trackNameSizeComboBox.setEnabled(false);
			applyToAllButton.setEnabled(false);
			labelFieldComboBox.setEnabled(false);
			maxDepthTextField.setEnabled(false);
		}

		if (selectedRows.length > 1) {
			displayNameTextField.setEnabled(false);
			applyDisplayNameButton.setEnabled(false);
			viewModeCB.setEnabled(false);
			bgColorComboBox.setSelectedColor(null);
			fgColorComboBox.setSelectedColor(null);
			trackNameSizeComboBox.setSelectedItem("");
			labelFieldComboBox.setSelectedIndex(-1);
			maxDepthTextField.setText(null);
			show2TracksCheckBox.setSelected(false);
			connectedCheckBox.setSelected(false);
			collapsedCheckBox.setSelected(false);
			colorCheckBox.setSelected(false);
			arrowCheckBox.setSelected(false);
			possitiveColorComboBox.setSelectedColor(null);
			negativeColorComboBox.setSelectedColor(null);
		}

		if (selectedRows.length == 1) {
			selectedStyle = ((TierPrefsTableModel)model).getStyles().get(selectedRows[0]);

			if (selectedStyle.getTrackName().equalsIgnoreCase(TrackConstants.NAME_OF_COORDINATE_INSTANCE)
					|| selectedStyle.isGraphTier()) {
				if (!selectedStyle.isGraphTier()) {
					displayNameTextField.setEnabled(false);
					applyDisplayNameButton.setEnabled(false);
				}
				viewModeCB.setEnabled(false);
				labelFieldComboBox.setEnabled(false);
				maxDepthTextField.setEnabled(false);
				connectedCheckBox.setEnabled(false);
				collapsedCheckBox.setEnabled(false);
				colorCheckBox.setEnabled(false);
				arrowCheckBox.setEnabled(false);
				possitiveColorComboBox.setEnabled(false);
				negativeColorComboBox.setEnabled(false);
				show2TracksCheckBox.setEnabled(false);
			} else {
				initialLabelField();
			}

			possitiveColorComboBox.setSelectedColor(selectedStyle.getForwardColor());
			negativeColorComboBox.setSelectedColor(selectedStyle.getReverseColor());
			String file_type = selectedStyle.getFileType();
			viewModeCB.removeAllItems();

			if (!selectedStyle.getTrackName().equalsIgnoreCase(TrackConstants.NAME_OF_COORDINATE_INSTANCE) && !selectedStyle.isGraphTier()) {
				viewModeCB.setModel(new javax.swing.DefaultComboBoxModel(MapViewModeHolder.getInstance().getAllViewModesFor(selectedStyle.getFileTypeCategory())));
				String view_mode = selectedStyle.getViewMode();
				if (view_mode == null) {
					viewModeCB.setSelectedIndex(0);
				} else {
					viewModeCB.setSelectedItem(view_mode);
				}
			}

			displayNameTextField.setText(selectedStyle.getTrackName());
			bgColorComboBox.setSelectedColor(selectedStyle.getBackground());
			fgColorComboBox.setSelectedColor(selectedStyle.getForeground());
			trackNameSizeComboBox.setSelectedItem(selectedStyle.getTrackNameSize());
			labelFieldComboBox.setSelectedItem(selectedStyle.getLabelField());
			maxDepthTextField.setText(String.valueOf(selectedStyle.getMaxDepth()));
			show2TracksCheckBox.setSelected(selectedStyle.getSeparate());
			connectedCheckBox.setSelected(selectedStyle.getConnected());
			collapsedCheckBox.setSelected(selectedStyle.getCollapsed());

			DIRECTION_TYPE direction = selectedStyle.getDirectionName();
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

		initializationDetector = false;
	}

	private void initialLabelField() {
		if (selectedStyle != null && smv != null) {
			if (smv.getAnnotatedSeq() != null) { //Fixes NPE
				SeqSymmetry sym = smv.getAnnotatedSeq().getAnnotation(selectedStyle.getMethodName());
				if (sym != null && sym.getChildCount() > 0) {
					SeqSymmetry child = sym.getChild(0);
					SeqSymmetry original = getMostOriginalSymmetry(child);
					if (original instanceof SymWithProps) {
						Map<String, Object> props = ((SymWithProps) original).getProperties();

						ArrayList<String> properties = new ArrayList<String>();
						properties.add((String) TrackConstants.LABELFIELD[0]);
						properties.addAll(props.keySet());
						labelFieldComboBox.setModel(new javax.swing.DefaultComboBoxModel(properties.toArray()));
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

				public void run() {
					refreshSeqMapView();
				}
			});
		}
	}

	public void selectAll() {
		if (table.getRowCount() > 1) {
			table.setRowSelectionInterval(0, table.getRowCount() - 2);
		}
	}
	public void displayNameTextField() {
		if (!settingValueFromTable) {
			model.setValueAt(displayNameTextField.getText(), 0, COL_TRACK_NAME);
		}
	}
	@Override
	public void trackNameSizeComboBox() {
		if (!settingValueFromTable
				&& !initializationDetector) {   // !initializationDetector condition is for the initialization when multiple rows are selected to prevent null exception
			if (!"".equals(trackNameSizeComboBox.getSelectedItem().toString())) { //Fixes NumberFormatException in special cases
				float trackNameSize = Float.parseFloat(trackNameSizeComboBox.getSelectedItem().toString());
				model.setValueAt(trackNameSize, 0, COL_TRACK_NAME_SIZE);
			}
		}
	}
	public void viewModeCB() {
		if (!settingValueFromTable) {
			for (int i = 0; i < selectedRows.length; i++) {
				model.setValueAt(viewModeCB.getSelectedItem(),
						0, COL_VIEW_MODE);
			}
		}
	}

	public void restoreToDefault() {
		if (selectedRows != null) { //Prevents NPE when no tracks have been generated.
			int row;
			TrackStyle style;
			int[] previousSelectedRows = selectedRows;
			for (int i = 0; i < selectedRows.length; i++) {
				row = selectedRows[i];
				style = ((TierPrefsTableModel)model).getStyles().get(row);
				style.restoreToDefault();
			}

			model.fireTableDataChanged();

			refreshSeqMapView();

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
			this.tier_styles = Collections.<TrackStyle>emptyList();
		}

		public void setStyles(List<TrackStyle> tier_styles) {
			this.tier_styles = tier_styles;
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
			TrackStyle style;
			style = tier_styles.get(row);
			switch (column) {
				case COL_FOREGROUND:
					return style.getForeground();
				case COL_BACKGROUND:
					return style.getBackground();
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
			settingValueFromTable = true;
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
							trackNameSizeComboBox.setSelectedItem((Float) value);
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
						case Col_Show_2_Tracks:
							style.setSeparate(((Boolean) value).booleanValue());
							break;
						case COL_CONNECTED:
							if (Boolean.TRUE.equals(value)) {
								style.setGlyphDepth(2);
							} else {
								style.setGlyphDepth(1);
							}
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
						case COL_VIEW_MODE:
							style.setViewMode((String) value);
							break;
						default:
							System.out.println("Unknown column selected: " + col);
					}
					fireTableCellUpdated(row, col);
				} catch (Exception e) {
					// exceptions should not happen, but must be caught if they do
					System.out.println("Exception in TierPrefsView.setValueAt(): " + e);
				}

				if (autoApplyChanges() && apply) {
					if (col == COL_BACKGROUND || col == COL_TRACK_NAME_SIZE
							|| col == COL_TRACK_NAME || col == COL_COLLAPSED
							|| col == COL_MAX_DEPTH) {
						if (col == COL_COLLAPSED || col == COL_MAX_DEPTH) {
							smv.getSeqMap().setTierStyles();
							smv.getSeqMap().repackTheTiers(true, true, false);
						}

						if (col == COL_TRACK_NAME) {
							smv.getSeqMap().setTierLabels();
						}

						smv.getSeqMap().updateWidget();
					} else {
						refreshSeqMapView();
					}
				}
			}
			setRowSelection(style);
			settingValueFromTable = false;
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