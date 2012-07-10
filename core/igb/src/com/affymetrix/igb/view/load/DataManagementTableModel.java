package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.parsers.CytobandParser;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.prefs.TierPrefsView;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.swing.recordplayback.ScriptManager;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Color;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

/**
 * Model for table of features.
 */
public final class DataManagementTableModel extends AbstractTableModel implements ChangeListener {

	private static final long serialVersionUID = 1L;
	private static final String[] columnNames = {"", "", "FG", "BG", "2 Track",
		"Track Name", "Load Mode", "Data Set/File Name", ""};
	private final Map<String, LoadStrategy> reverseLoadStrategyMap;  // from friendly string to enum
	static final int REFRESH_FEATURE_COLUMN = 0;
	static final int HIDE_FEATURE_COLUMN = 1;
	static final int FOREGROUND_COLUMN = 2;
	static final int BACKGROUND_COLUMN = 3;
	static final int SEPARATE_COLUMN = 4;
	static final int TRACK_NAME_COLUMN = 5;
	static final int LOAD_STRATEGY_COLUMN = 6;
	static final int FEATURE_NAME_COLUMN = 7;
	static final int DELETE_FEATURE_COLUMN = 8;
	private final GeneralLoadView glv;
	private final static featureTableComparator visibleFeatureComp = new featureTableComparator();
	private SeqMapView smv;
	private List<TrackStyle> currentStyles;
	public List<VirtualFeature> virtualFeatures;
	public List<GenericFeature> features;

	DataManagementTableModel(GeneralLoadView glv) {
		this.glv = glv;
		this.features = null;
		this.virtualFeatures = new ArrayList<VirtualFeature>();
		Application igb = Application.getSingleton();
		if (igb != null) {
			smv = igb.getMapView();
		}

		// Here we map the friendly string back to the LoadStrategy.
		this.reverseLoadStrategyMap = new HashMap<String, LoadStrategy>(3);
		for (LoadStrategy strategy : EnumSet.allOf(LoadStrategy.class)) {
			this.reverseLoadStrategyMap.put(strategy.toString(), strategy);
		}
	}

	public void clearFeatures() {
		if (features != null && virtualFeatures != null) {
			virtualFeatures.clear();

			features.clear();

			fireTableDataChanged();

			TierPrefsView.getSingleton().clearTable();
		}
	}

	void createVirtualFeatures(List<GenericFeature> features) {
		// Sort these features so the features to be loaded are at the top.
		Collections.sort(features, visibleFeatureComp);

		this.features = features;
		if (virtualFeatures != null) {
			virtualFeatures.clear();
		}
		for (GenericFeature gFeature : features) {
			createPrimaryVirtualFeatures(gFeature);
		}

		fireTableDataChanged();
//		System.out.println(this.getClass().getName() + ".createVirtualFeatures: twice");
	}

	/*
	 * Some file formats might have multiple tracks, try load GFF1_example.gff
	 */
	void createPrimaryVirtualFeatures(GenericFeature gFeature) {
		VirtualFeature vFeature = new VirtualFeature(gFeature, null);
		virtualFeatures.add(vFeature);
		currentStyles = getCurrentStyles();
		for (ITrackStyleExtended style : currentStyles) {
			if (style.getFeature() == gFeature) {
				vFeature.setStyle(style);
				if (gFeature.getMethods().size() > 1) {
					createSecondaryVirtualFeatures(vFeature);
					break;
				}
				if (!style.isGraphTier()) {
					break;
				}
			}
		}
	}

	void createSecondaryVirtualFeatures(VirtualFeature vFeature) {
		boolean isPrimary = vFeature.isPrimary();
		VirtualFeature subVfeature;
		for (ITrackStyleExtended style : currentStyles) {
			if (style.getFeature() == vFeature.getFeature()) {
				subVfeature = new VirtualFeature(vFeature.getFeature(), style);
				// The first tract will be removed and added back again as a primary track
				if (isPrimary) {
					virtualFeatures.remove(vFeature);
					isPrimary = false;
				} else {
					subVfeature.setPrimary(false);
				}
				virtualFeatures.add(subVfeature);
			}
		}
	}

	private final static class featureTableComparator implements Comparator<GenericFeature> {

		public int compare(GenericFeature feature1, GenericFeature feature2) {
			if (feature1.getLoadStrategy() != feature2.getLoadStrategy()) {
				return (feature1.getLoadStrategy().compareTo(feature2.getLoadStrategy()));
			}
			if (feature1.featureName.compareTo(feature2.featureName) != 0) {
				return feature1.featureName.compareTo(feature2.featureName);
			}
			return feature1.gVersion.gServer.serverType.compareTo(
					feature2.gVersion.gServer.serverType);
		}
	}

	VirtualFeature getFeature(int row) {
		return (getRowCount() <= row) ? null : virtualFeatures.get(row);
	}

	private int getRow(VirtualFeature feature) {
		return (virtualFeatures == null) ? -1 : virtualFeatures.indexOf(feature);

	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return (virtualFeatures == null) ? 0 : virtualFeatures.size();
	}

	public void updateFeatureColumn() {
		for (int row = 0; row < virtualFeatures.size(); row++) {
			if (row >= 0) {
				fireTableCellUpdated(row, FEATURE_NAME_COLUMN);
			}
		}
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		if (virtualFeatures == null || virtualFeatures.isEmpty()) {
			// Indicate to user that there's no data.
			if (row == 0 && col == 2) {
				return "No feature data found";
			}
			return "";
		}

		VirtualFeature vFeature;
		ITrackStyleExtended style;
		if (getFeature(row) == null) {
			return "";
		} else {
			vFeature = getFeature(row);
			style = vFeature.getStyle();
		}

		switch (col) {
			case REFRESH_FEATURE_COLUMN:
				return "";
			case LOAD_STRATEGY_COLUMN:
				// return the load strategy
				if (!vFeature.isPrimary()) {
					return "";
				}
				return vFeature.getLoadStrategy().toString();
			case FEATURE_NAME_COLUMN:
				// the friendly feature name removes slashes.  Clip it here.
				if (vFeature.getServer() == ServerTypeI.QuickLoad) {
					return vFeature.getFeature().featureName;
				} else if (!vFeature.isPrimary()) {
					return "";
				}
				return vFeature.getFeature().featureName;
			case TRACK_NAME_COLUMN:
				if (vFeature.getFeature().featureName.equals(CytobandParser.CYTOBAND_TIER_NAME)) {
					return "";
				} else if (style == null) {
					return "";
				}
				return style.getTrackName();
			case FOREGROUND_COLUMN:
				if (style == null || style.getMethodName().matches(CytobandParser.CYTOBAND_TIER_NAME)) {
					return Color.WHITE;
				}
				return style.getForeground();
			case BACKGROUND_COLUMN:
				if (style == null || style.getMethodName().matches(CytobandParser.CYTOBAND_TIER_NAME)) {
					return Color.WHITE;
				}
				return style.getBackground();
			case SEPARATE_COLUMN:
				if (style == null || style.getMethodName().matches(CytobandParser.CYTOBAND_TIER_NAME)) {
					return false;
				}
				return style.getSeparate();
			case DELETE_FEATURE_COLUMN:
				return "";
			case HIDE_FEATURE_COLUMN:
				return "";
			default:
				System.out.println("Shouldn't reach here: " + row + " " + col);
				return null;
		}
	}

	@Override
	public Class<?> getColumnClass(int c) {
		if ((getValueAt(0, c)) == null) {
			System.out.println("Null Reference ERROR: column " + c);
		}
		return getValueAt(0, c).getClass();
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		VirtualFeature vFeature = virtualFeatures.get(row);
		ITrackStyleExtended style = vFeature.getStyle();

		if ((style == null)
				&& (col == TRACK_NAME_COLUMN
				|| col == BACKGROUND_COLUMN || col == FOREGROUND_COLUMN
				|| col == SEPARATE_COLUMN || col == HIDE_FEATURE_COLUMN)) {
			return false;
		} else if (style != null && style.getMethodName().matches(CytobandParser.CYTOBAND_TIER_NAME)) {
			if (col == HIDE_FEATURE_COLUMN || col == REFRESH_FEATURE_COLUMN || col == DELETE_FEATURE_COLUMN) {
				return true;
			}
			return false;
		} else if ((col == DELETE_FEATURE_COLUMN || col == REFRESH_FEATURE_COLUMN)
				&& !vFeature.isPrimary()) {
			return false;
		} else if (col == FEATURE_NAME_COLUMN) {
			switch (vFeature.getFeature().getLastRefreshStatus()) {
				case NO_DATA_LOADED:
					return true;
				default:
					return false;
			}
		} else if (smv.getPixelFloater().getChildren() != null
				&& col != DELETE_FEATURE_COLUMN && col != FOREGROUND_COLUMN) {
			for (GlyphI i : smv.getPixelFloater().getChildren()) {
				if (((ViewModeGlyph) i).getAnnotStyle() == style) {
					return false;
				}
			}
		} else if ((vFeature.getStyle() != null && vFeature.getStyle().isGraphTier())
				&& (col == SEPARATE_COLUMN)) {
			return false;
		}

		if (col == DELETE_FEATURE_COLUMN || col == REFRESH_FEATURE_COLUMN
				|| col == HIDE_FEATURE_COLUMN || col == TRACK_NAME_COLUMN
				|| col == BACKGROUND_COLUMN || col == FOREGROUND_COLUMN
				|| col == SEPARATE_COLUMN) {
			return true;
		} else if (getFeature(row) == null) {
			return false;
		}

		// This cell is only editable if the feature isn't already fully loaded.
		return (getFeature(row).getLoadStrategy() != LoadStrategy.GENOME
				&& getFeature(row).getLoadChoices().size() > 1);
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		VirtualFeature vFeature = getFeature(row);

		if (value == null || vFeature == null) {
			return;
		}

		switch (col) {
			case DELETE_FEATURE_COLUMN:
				String message = "Really remove entire " + vFeature.getFeature().featureName + " data set ?";
				if (ScriptManager.SCRIPTING.equals(value) || Application.confirmPanel(message, PreferenceUtils.getTopNode(),
						PreferenceUtils.CONFIRM_BEFORE_DELETE, PreferenceUtils.default_confirm_before_delete)) {
					for (GenericFeature gFeature : features) {
						if (gFeature.equals(vFeature.getFeature())) {
							GeneralLoadView.getLoadView().removeFeature(gFeature, true);
						}
					}
					this.fireTableDataChanged(); //clear row selection
				}
				break;
			case REFRESH_FEATURE_COLUMN:
				if (vFeature.getLoadStrategy() != LoadStrategy.NO_LOAD
						&& vFeature.getLoadStrategy() != LoadStrategy.GENOME) {
					GeneralLoadView.getLoadView().setShowLoadingConfirm(true);
					for (GenericFeature gFeature : features) {
						if (gFeature.equals(vFeature.getFeature())) {
							GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);
						}
					}
				}
				break;
			case LOAD_STRATEGY_COLUMN:
				if (vFeature.getFeature().getLoadStrategy() == LoadStrategy.GENOME) {
					return;	// We can't change strategies once we've loaded the entire genome.
				}
				if (vFeature.getFeature().getLoadChoices().size() <= 1) {
					return;
				}
				String valueString = value.toString();
				if (!vFeature.getFeature().getLoadStrategy().toString().equals(valueString)) {
					// strategy changed.  Update the feature object.
					vFeature.getFeature().setLoadStrategy(reverseLoadStrategyMap.get(valueString));
					updatedStrategy(row, col, vFeature.getFeature());
				}
				break;
			case HIDE_FEATURE_COLUMN:
				if (vFeature.getStyle() != null) {
					setVisibleTracks(vFeature.getStyle());
				}
				break;
			case BACKGROUND_COLUMN:
				if (vFeature.getStyle() != null) {
					vFeature.getStyle().setBackground((Color) value);
				}
				break;
			case FOREGROUND_COLUMN:
				if (vFeature.getStyle() != null) {
					vFeature.getStyle().setForeground((Color) value);
				}
				break;
			case SEPARATE_COLUMN:
				if (vFeature.getStyle() != null) {
					vFeature.getStyle().setSeparate((Boolean) value);
					smv.getPopup().refreshMap(false, true);
					smv.getTierManager().sortTiers();
				}
				break;
			case TRACK_NAME_COLUMN:
				if (vFeature.getStyle() != null) {
					vFeature.getStyle().setTrackName((String) value);
				}
				break;
			default:
				System.out.println("Unknown column selected: " + col);
		}

		fireTableCellUpdated(row, col);
		update(col);
		TierPrefsView.getSingleton().setRowSelection(vFeature.getStyle());
	}

	private void update(int col) {
		if (col == BACKGROUND_COLUMN || col == TRACK_NAME_COLUMN) {
			if (col == TRACK_NAME_COLUMN) {
				smv.getSeqMap().setTierLabels();
			}

			smv.getSeqMap().updateWidget();
		} else if (col == HIDE_FEATURE_COLUMN){
			smv.getSeqMap().repackTheTiers(false, true);
		} else if (col == FOREGROUND_COLUMN) {
			refreshSeqMapView();
		}
	}

	private void setVisibleTracks(ITrackStyleExtended style) {
		if (style.getShow()) {
			if (style.getShow()) {
				style.setShow(false);
			}
		} else {
			if (!style.getShow()) {
				style.setShow(true);
			}
		}
	}

	private void refreshSeqMapView() {
		if (smv != null) {
			smv.updatePanel();
		}
	}

	private List<TrackStyle> getCurrentStyles() {
		ArrayList<TrackStyle> currentStyleList = new ArrayList<TrackStyle>();
		if (smv != null) {
			LinkedHashMap<TrackStyle, TrackStyle> stylemap = new LinkedHashMap<TrackStyle, TrackStyle>();
			for (TierGlyph tier : new CopyOnWriteArrayList<TierGlyph>(smv.getSeqMap().getTiers())) {
				ITrackStyle style = tier.getAnnotStyle();
				if (style instanceof TrackStyle) {
					if (!tier.isGarbage() || tier.getDirection() == Direction.AXIS) {
						stylemap.put((TrackStyle) style, (TrackStyle) style);
					} else if (style.getMethodName().equals(CytobandParser.CYTOBAND_TIER_NAME)) {
						stylemap.put((TrackStyle) style, (TrackStyle) style);
					} else if (smv.getPixelFloater().getChildren() != null) {
						for (GlyphI g : smv.getPixelFloater().getChildren()) {
							ViewModeGlyph j = (ViewModeGlyph) g;
							if (j.getAnnotStyle() == style) {
								stylemap.put((TrackStyle) style, (TrackStyle) style);
							}
						}
					}
				}
			}
			currentStyleList.addAll(stylemap.values());
		}
		ArrayList<TrackStyle> customizables = new ArrayList<TrackStyle>(currentStyleList.size());
		for (int i = 0; i < currentStyleList.size(); i++) {
			TrackStyle the_style = currentStyleList.get(i);
			if (the_style.getCustomizable()) {
				customizables.add(the_style);
			}
		}

		return customizables;
	}

	/**
	 * The strategy was changed. Update the table, and if necessary, load the
	 * annotations and change the button statuses.
	 *
	 * @param row
	 * @param col
	 * @param gFeature
	 */
	private void updatedStrategy(int row, int col, GenericFeature gFeature) {
		fireTableCellUpdated(row, col);

		if (gFeature.getLoadStrategy() == LoadStrategy.GENOME) {
			GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);
		}else if (gFeature.getLoadStrategy() == LoadStrategy.AUTOLOAD){
			// This would trigger auto load
			smv.fireRangeChanged(smv.getVisibleSpan().getStart(), smv.getVisibleSpan().getEnd());
		}

		//  Whatever feature strategy changed, it may have affected
		// the enable status of the "load visible" button

		this.glv.changeVisibleDataButtonIfNecessary(features);
	}

	public void stateChanged(ChangeEvent evt) {//????
		Object src = evt.getSource();
		if (src instanceof GenericFeature) {
			int row = getRow((VirtualFeature) src);
			if (row >= 0) {  // if typestate is present in table, then send notification of row change
				fireTableRowsUpdated(row, row);
			}
		}
	}
}
