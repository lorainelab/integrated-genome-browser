package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TrackConstants;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

/**
 * Model for table of features.
 */
public final class LoadModeDataTableModel extends AbstractTableModel implements ChangeListener {

	private static final long serialVersionUID = 1L;
	private static final String[] columnNames = {"Hide", "Refresh", "Info", "Choose Load Mode", "Data Set/File Name", "Track Name", "BG", "FG", "Trash"};
	private final Map<String, LoadStrategy> reverseLoadStrategyMap;  // from friendly string to enum
	static final int HIDE_FEATURE_COLUMN = 0;
	static final int REFRESH_FEATURE_COLUMN = 1;
	static final int INFO_FEATURE_COLUMN = 2;
	static final int LOAD_STRATEGY_COLUMN = 3;
	static final int FEATURE_NAME_COLUMN = 4;
	static final int TRACK_NAME_COLUMN = 5;
	static final int COL_BACKGROUND = 6;
	static final int COL_FOREGROUND = 7;
	static final int DELETE_FEATURE_COLUMN = 8;
	private final GeneralLoadView glv;
	private final static featureTableComparator visibleFeatureComp = new featureTableComparator();
	private SeqMapView smv;
	private List<TrackStyle> currentStyles;
	public List<GenericFeature> features;
	public List<GenericFeature> subFeatures;
	public List<GenericFeature> allFeatures;

	LoadModeDataTableModel(GeneralLoadView glv) {
		this.glv = glv;
		this.features = null;
		this.allFeatures = new ArrayList<GenericFeature>();
		this.subFeatures = new ArrayList<GenericFeature>();
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

	void clearFeatures() {
		if (this.features != null) {
			this.features.clear();
		}
		if (this.allFeatures != null) {
			this.allFeatures.clear();
		}
		if (this.subFeatures != null) {
			this.subFeatures.clear();
		}
		this.fireTableDataChanged();
	}

	void setFeatures(List<GenericFeature> features) {
		this.features = features;
		for (GenericFeature f : features) {
			if (!allFeatures.contains(f)) {
				allFeatures.add(f);
				if (f.getMethods().size() > 1) {
					System.out.println(subFeatures.size());
					for (GenericFeature subf : subFeatures) {
						allFeatures.add(subf);
					}
				}
			}
		}
		this.fireTableDataChanged();
	}

	/**
	 * Only want to display features with visible attribute set to true.
	 * @param features
	 * @return list of visible features
	 */
	static List<GenericFeature> getVisibleFeatures(List<GenericFeature> features) {
		if (features == null) {
			return null;
		}
		List<GenericFeature> visibleFeatures = new ArrayList<GenericFeature>();
		for (GenericFeature gFeature : features) {
			if (gFeature.isVisible()) {
				visibleFeatures.add(gFeature);
			}
		}

		Collections.sort(visibleFeatures, visibleFeatureComp);

		// Also sort these features so the features to be loaded are at the top.

		return visibleFeatures;
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

	GenericFeature getFeature(int row) {
		return (getRowCount() <= row) ? null : allFeatures.get(row);
	}

	private int getRow(GenericFeature feature) {
		return (allFeatures == null) ? -1 : allFeatures.indexOf(feature);

	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return (allFeatures == null) ? 0 : allFeatures.size();
	}

	public void genericFeatureRefreshed(GenericFeature feature) {
		int row = getRow(feature);
		if (row >= 0) {
			fireTableCellUpdated(row, INFO_FEATURE_COLUMN);
		}
	}

	public void createSubFeatures(GenericFeature gFeature, TrackStyle style) {
		if (gFeature.featureName.equalsIgnoreCase(style.getFeature().featureName)) {
			if (!gFeature.uniqueName.equalsIgnoreCase(style.getUniqueName())) {
				generateSubTrack(gFeature, style);
			}
		}

	}

	private void generateSubTrack(GenericFeature f, TrackStyle style) {
		boolean duplicateTest = false;
		GenericFeature subFeature = new GenericFeature(
				style.getFeature().featureName, f.featureProps, f.gVersion, f.symL, f.typeObj, f.autoLoad);
		subFeature.uniqueName = style.getUniqueName();
		subFeature.trackName = style.getTrackName();
		subFeature.Background = style.getBackground();
		subFeature.Foreground = style.getForeground();
		subFeature.setLoadStrategy(f.getLoadStrategy());
		for (int i = 0; i < allFeatures.size(); i++) {
			if (allFeatures.get(i).trackName.equalsIgnoreCase(subFeature.trackName)) {
				duplicateTest = true;
			}
		}
		if (!duplicateTest) {
			style.setFeature(subFeature);
			addSubFeatures(subFeature);
		}
	}

	private void addSubFeatures(GenericFeature f) {
		subFeatures.add(f);
		this.fireTableDataChanged();
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		if (allFeatures == null || allFeatures.isEmpty()) {
			// Indicate to user that there's no data.
			if (row == 0 && col == 2) {
				return "No feature data found";
			}
			return "";
		}
		GenericFeature gFeature;
		if (getFeature(row) == null) {
			return "";
		} else {
			gFeature = getFeature(row);
		}

		switch (col) {
			case INFO_FEATURE_COLUMN:
				return "";
			case REFRESH_FEATURE_COLUMN:
				return "";
			case LOAD_STRATEGY_COLUMN:
				// return the load strategy
				if (subFeatures.contains(gFeature)) {
					return "";
				}
				return gFeature.getLoadStrategy().toString();
			case FEATURE_NAME_COLUMN:
				// the friendly feature name removes slashes.  Clip it here.
				if (gFeature.gVersion.gServer.serverType == ServerType.QuickLoad) {
					return LoadUtils.stripFilenameExtensions(gFeature.featureName);
				}
				return gFeature.featureName;
			case TRACK_NAME_COLUMN:
				if (gFeature.trackName == null) {
					return "Data Not Loaded.";
				}
				return gFeature.trackName;
			case COL_BACKGROUND:
				if (gFeature.trackName == null) {
					return Color.WHITE;
				}
				return gFeature.Background;
			case COL_FOREGROUND:
				if (gFeature.trackName == null) {
					return Color.WHITE;
				}
				return gFeature.Foreground;
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
		GenericFeature gFeature = allFeatures.get(row);

		if (subFeatures.contains(gFeature)
				&& (col == DELETE_FEATURE_COLUMN)
				|| (col == REFRESH_FEATURE_COLUMN)) {
			return false;
		} else if (gFeature.trackName == null && (col == COL_BACKGROUND || col == COL_FOREGROUND)) {
			return false;
		} else if (col == DELETE_FEATURE_COLUMN || col == REFRESH_FEATURE_COLUMN
				|| col == HIDE_FEATURE_COLUMN || col == TRACK_NAME_COLUMN || col == COL_BACKGROUND || col == COL_FOREGROUND) {
			return true;
		} else if (col != LOAD_STRATEGY_COLUMN && col != INFO_FEATURE_COLUMN) {
			return false;
		} else if (col == INFO_FEATURE_COLUMN) {
			switch (gFeature.getLastRefreshStatus()) {
				case NO_DATA_LOADED:
					return true;
			}
			return false;
		} else if (getFeature(row) == null) {
			return false;
		}
		// This cell is only editable if the feature isn't already fully loaded.
		return (getFeature(row).getLoadStrategy() != LoadStrategy.GENOME);
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		GenericFeature gFeature = getFeature(row);
		TrackStyle style;

		if (value == null || gFeature == null) {
			return;
		}

		switch (col) {
			case INFO_FEATURE_COLUMN:
				ErrorHandler.errorPanel(gFeature.featureName, gFeature.getLastRefreshStatus().toString());
				break;
			case DELETE_FEATURE_COLUMN:
				String message = "Really remove entire " + gFeature.featureName + " data set ?";
				if (Application.confirmPanel(message, PreferenceUtils.getTopNode(),
						PreferenceUtils.CONFIRM_BEFORE_DELETE, PreferenceUtils.default_confirm_before_delete)) {
					GeneralLoadView.getLoadView().removeFeature(gFeature, true);
					removeAllFeature(gFeature);
					removeSubFeature(gFeature);
				}
				break;
			case REFRESH_FEATURE_COLUMN:
				if (gFeature.getLoadStrategy() != LoadStrategy.NO_LOAD
						&& gFeature.getLoadStrategy() != LoadStrategy.GENOME) {
					GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);
				}
				break;
			case LOAD_STRATEGY_COLUMN:
				if (gFeature.getLoadStrategy() == LoadStrategy.GENOME) {
					return;	// We can't change strategies once we've loaded the entire genome.
				}
				String valueString = value.toString();
				if (!gFeature.getLoadStrategy().toString().equals(valueString)) {
					// strategy changed.  Update the feature object.
					gFeature.setLoadStrategy(reverseLoadStrategyMap.get(valueString));
					updatedStrategy(row, col, gFeature);
				}
				break;
			case HIDE_FEATURE_COLUMN:
				style = getFeatureStyle(gFeature);
				if(style == null)
					return;
				setVisibleTracks(gFeature.trackName, style);
				break;
			case COL_BACKGROUND:
				style = getFeatureStyle(gFeature);
				if(style == null)
					return;
				style.setBackground((Color) value);
				gFeature.Background = ((Color) value);
				break;
			case COL_FOREGROUND:
				style = getFeatureStyle(gFeature);
				if(style == null)
					return;
				style.setForeground((Color) value);
				gFeature.Foreground = ((Color) value);
				break;
			case TRACK_NAME_COLUMN:
				getFeatureStyle(gFeature).setTrackName((String) value);
				gFeature.trackName = ((String) value);
				break;
			default:
				System.out.println("Unknown column selected: " + col);
		}

		fireTableCellUpdated(row, col);

		if (col != LOAD_STRATEGY_COLUMN && col != DELETE_FEATURE_COLUMN
				&& col != INFO_FEATURE_COLUMN
				&& col != FEATURE_NAME_COLUMN
				&& col != REFRESH_FEATURE_COLUMN) {
			refreshSeqMapView();
		}
	}

	//Remove From allFeatures
	private void removeAllFeature(GenericFeature gFeature) {
		Iterator<GenericFeature> iterator = allFeatures.iterator();
		String featureName;
		while (iterator.hasNext()) {
			featureName = iterator.next().featureName;
			if (featureName.equalsIgnoreCase(gFeature.featureName)) {
				iterator.remove();
			}
		}
	}

	//Remove From subFeatures
	private void removeSubFeature(GenericFeature gFeature) {
		Iterator<GenericFeature> iterator = subFeatures.iterator();
		String featureName;
		while (iterator.hasNext()) {
			featureName = iterator.next().featureName;
			if (featureName.equalsIgnoreCase(gFeature.featureName)) {
				iterator.remove();
			}
		}
	}

	private void setVisibleTracks(String trackName, TrackStyle style) {
		if (style.getShow()) {
			smv.getPopup().hideOneTier(style);
		} else {
			for (int i = 0; i < smv.getPopup().getShowMenu().getItemCount(); i++) {
				String text = smv.getPopup().getShowMenu().getItem(i).getText();
				if (text.length() > 29) {
					text = text.substring(0, 30);
				}
				if (trackName.length() > 29) {
					trackName = trackName.substring(0, 30);
				}
				if (text.equalsIgnoreCase(trackName)) {
					style.setShow(true);
					smv.getPopup().getShowMenu().remove(smv.getPopup().getShowMenu().getItem(i));
				}
			}
		}
	}

	private TrackStyle getFeatureStyle(GenericFeature f) {
		currentStyles = getCurrentStyles();
		for (TrackStyle style : currentStyles) {
			if (style.getUniqueName() != null) {
				if (style.getUniqueName().equalsIgnoreCase(
						f.uniqueName))//need changed
				{
					return style;
				}
			}
		}
		return null;
	}

	private void refreshSeqMapView() {
		if (smv != null) {
			smv.setAnnotatedSeq(smv.getAnnotatedSeq(), true, true, false);
		}
	}

	private List<TrackStyle> getCurrentStyles() {
		ArrayList<TrackStyle> currentStyleList = new ArrayList<TrackStyle>();
		if (smv != null) {
			List<TierGlyph> temp;
			temp = smv.getSeqMap().getTiers();
			LinkedHashMap<TrackStyle, TrackStyle> stylemap = new LinkedHashMap<TrackStyle, TrackStyle>();
			LinkedHashMap<TierGlyph, TierGlyph> tierMap = new LinkedHashMap<TierGlyph, TierGlyph>();
			Iterator<TierGlyph> titer = temp.iterator();
			int i = 0;
			while (titer.hasNext()) {
				TierGlyph tier = titer.next();
				ITrackStyle style = tier.getAnnotStyle();
				if ((style instanceof TrackStyle)
						&& (!tier.getAnnotStyle().getTrackName().equalsIgnoreCase(TrackConstants.NAME_OF_COORDINATE_INSTANCE))
						&& (tier.getChildCount() > 0)) {
					stylemap.put((TrackStyle) style, (TrackStyle) style);
					tierMap.put((tier), tier);
				}
			}
			currentStyleList.addAll(stylemap.values());
		}
		ArrayList<TrackStyle> customizables = new ArrayList<TrackStyle>(currentStyleList.size());
		for (int i = 0; i < currentStyleList.size(); i++) {
			TrackStyle the_style = currentStyleList.get(i);
			if (the_style.getCustomizable()) {
				// if graph tier style then only include if include_graph_styles toggle is set (app is _not_ IGB)
				//	if ((!the_style.isGraphTier())) {
				customizables.add(the_style);
				//	}
			}
		}
		return customizables;
	}

	/**
	 * The strategy was changed.  Update the table, and if necessary, load the annotations and change the button statuses.
	 * @param row
	 * @param col
	 * @param gFeature
	 */
	private void updatedStrategy(int row, int col, GenericFeature gFeature) {
		fireTableCellUpdated(row, col);

		if (gFeature.getLoadStrategy() == LoadStrategy.GENOME || gFeature.getLoadStrategy() == LoadStrategy.AUTOLOAD) {
			GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);
		}

		//  Whatever feature strategy changed, it may have affected
		// the enable status of the "load visible" button

		this.glv.changeVisibleDataButtonIfNecessary(features);
	}

	public void stateChanged(ChangeEvent evt) {
		Object src = evt.getSource();
		if (src instanceof GenericFeature) {
			int row = getRow((GenericFeature) src);
			if (row >= 0) {  // if typestate is present in table, then send notification of row change
				fireTableRowsUpdated(row, row);
			}
		}
	}
}
