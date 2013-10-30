package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.parsers.CytobandParser;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.prefs.TierPrefsView;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.swing.recordplayback.ScriptManager;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.StyledGlyph;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Color;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

/**
 * Model for table of features.
 */
public final class DataManagementTableModel extends AbstractTableModel implements ChangeListener, TableModelListener {

	private static final long serialVersionUID = 1L;
	private static final String[] columnNames = {"", "", "FG", "BG", "+/-", "Load Mode",
		"Track Name", ""};
	private final Map<String, LoadStrategy> reverseLoadStrategyMap;  // from friendly string to enum
	static final int REFRESH_FEATURE_COLUMN = 0;
	static final int HIDE_FEATURE_COLUMN = 1;
	static final int FOREGROUND_COLUMN = 2;
	static final int BACKGROUND_COLUMN = 3;
	static final int SEPARATE_COLUMN = 4;
	static final int LOAD_STRATEGY_COLUMN = 5;
	static final int TRACK_NAME_COLUMN = 6;
	static final int DELETE_FEATURE_COLUMN = 7;
	private final GeneralLoadView glv;
//	private final static featureTableComparator visibleFeatureComp = new featureTableComparator();
	private SeqMapView smv;
	private AffyLabelledTierMap map;
	private Map<Object, VirtualFeature> style2Feature = new HashMap<Object, VirtualFeature>();
	private List<TrackStyle> currentStyles;
	public List<VirtualFeature> virtualFeatures;
	public List<GenericFeature> features;
	private HashMap<GenericFeature, LoadStrategy> previousLoadStrategyMap = new HashMap<GenericFeature, LoadStrategy>(); // Remember the load strategy for un-hidden restoration				
	
	DataManagementTableModel(GeneralLoadView glv) {
		this.glv = glv;
		this.features = null;
		this.virtualFeatures = new ArrayList<VirtualFeature>();
		Application igb = Application.getSingleton();
		if (igb != null) {
			smv = igb.getMapView();
			map = (AffyLabelledTierMap)smv.getSeqMap();
		}
		map.addTierOrderListener(this);
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
			
			style2Feature.clear();

			fireTableDataChanged();

			TierPrefsView.getSingleton().clearTable();
		}
	}

	void createVirtualFeatures(List<GenericFeature> features) {
		// Sort these features so the features to be loaded are at the top.
		Collections.sort(features, new featureTableComparator());

		this.features = features;
		if (virtualFeatures != null) {
			virtualFeatures.clear();
		}
		style2Feature.clear();
		
		for (GenericFeature gFeature : features) {
			createPrimaryVirtualFeatures(gFeature);
		}
		
		fireTableDataChanged();
//		sort();
//		System.out.println(this.getClass().getName() + ".createVirtualFeatures: twice");
	}

	/*
	 * Some file formats might have multiple tracks, try load GFF1_example.gff
	 */
	private void createPrimaryVirtualFeatures(GenericFeature gFeature) {
		VirtualFeature vFeature = new VirtualFeature(gFeature, null);
		virtualFeatures.add(vFeature);
		currentStyles = getCurrentStyles();
		for (ITrackStyleExtended style : currentStyles) {
			if (style.getFeature() == gFeature) {
				vFeature.setStyle(style);
				style2Feature.put(style, vFeature);
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

	private void createSecondaryVirtualFeatures(VirtualFeature vFeature) {
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
				style2Feature.put(style, subVfeature);
			}
		}
	}

//	private void sort() {
//		Collections.sort(virtualFeatures, new FeatureTierComparator());
//		fireTableDataChanged();
//	}
//
//	private class FeatureTierComparator implements Comparator<VirtualFeature> {
//		Map<VirtualFeature,Integer> vfToPos;
//		
//		FeatureTierComparator() {
//			vfToPos = new HashMap<VirtualFeature,Integer>();
//			
//			int pos = 0;
//			List<TierLabelGlyph> orderedGlyphs = map.getOrderedTierLabels();
//			int size = orderedGlyphs.size();
//
//			VirtualFeature vf;
//			for (int i = 0; i < size; i++) {
//				TierGlyph tg = orderedGlyphs.get(i).getReferenceTier();
//				ITrackStyleExtended style = tg.getAnnotStyle();
//				//Only consider positive track.
//				if (style.getSeparate() && tg.getDirection() == TierGlyph.Direction.REVERSE) {
//					continue;
//				}
//
//				// Fix for joined graphs disappears from DMT when click on joined track label or remove feature
//				if (style.isGraphTier() && tg.getChildCount() > 1) {
//					for (int j = 0; j < tg.getChildCount(); j++) {
//						GlyphI g = tg.getChild(j);
//						if (!(g instanceof GraphGlyph)) {
//							continue;
//						}
//						vf = style2Feature.get(((GraphGlyph) g).getAnnotStyle());
//						if (vf != null && !vfToPos.containsKey(vf)) {
//							vfToPos.put(vf, pos++);
//						}
//					}
//					continue;
//				}
//
//				vf = style2Feature.get(style);
//				if (vf != null && !vfToPos.containsKey(vf)) {
//					vfToPos.put(vf, pos++);
//				}
//			}
//
//			// Fix for link.psl files
//			List<TierLabelGlyph> tierGlyphs = map.getTierLabels();
//			size = tierGlyphs.size();
//			for (int i = 0; i < size; i++) {
//				TierGlyph tg = tierGlyphs.get(i).getReferenceTier();
//				// If tier is invisible and it has no children then do not add it.
//				if (!tg.isVisible() && tg.getChildCount() == 0) {
//					continue;
//				}
//
//				ITrackStyleExtended style = tg.getAnnotStyle();
//				//Only consider positive track.
//				if (style.getSeparate() && tg.getDirection() == TierGlyph.Direction.REVERSE) {
//					continue;
//				}
//
//				vf = style2Feature.get(style);
//				if (vf != null && !vfToPos.containsKey(vf)) {
//					vfToPos.put(vf, pos++);
//				}
//			}
//
//			for (VirtualFeature tempVirtualFeature : virtualFeatures) {
//				//virtualFeatures.add(tempVirtualFeature);
//				if (tempVirtualFeature.getFeature().featureName.equals(CytobandParser.CYTOBAND_TIER_NAME)
//						|| tempVirtualFeature.getFeature().featureName.equalsIgnoreCase(CytobandParser.CYTOBAND)
//						|| tempVirtualFeature.getFeature().featureName.equalsIgnoreCase(CytobandParser.CYTOBANDS)) {
//					vfToPos.put(tempVirtualFeature, pos++);
//					continue;
//				}
//			
//				//Temporary fix to show joined graph features when sequence is changed.
//				vf = style2Feature.get(tempVirtualFeature.getStyle());
//				if (vf != null && vf.getStyle().getJoin() && !vfToPos.containsKey(vf)) {
//					vfToPos.put(vf, pos++);
//				}
//			}
//
//
//			for (VirtualFeature tempVirtualFeature : virtualFeatures) {
//				if (!vfToPos.containsKey(tempVirtualFeature)) {
//					vfToPos.put(tempVirtualFeature, pos++);
//				}
//			}
//			
//			List<GraphGlyph> floatingGraphGlyphs = smv.getFloatingGraphGlyphs();
//			size = floatingGraphGlyphs.size();
//			for (int i = 0; i < size; i++) {
//				ITrackStyleExtended style = floatingGraphGlyphs.get(i).getAnnotStyle();
//				vf = style2Feature.get(style);
//				if (vf != null && !vfToPos.containsKey(vf)) {
//					vfToPos.put(vf, pos++);
//				}
//			}
//		}
//		
//		@Override
//		public int compare(VirtualFeature o1, VirtualFeature o2) {
//			return vfToPos.get(o1).compareTo(vfToPos.get(o2));
//		}
//		
//	}
	
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
//				if (!vFeature.isPrimary()) {
//					return "";
//				}
				return vFeature.getLoadStrategy().toString();
			case TRACK_NAME_COLUMN:
				if (vFeature.getFeature().featureName.equals(CytobandParser.CYTOBAND_TIER_NAME) ||
					vFeature.getFeature().featureName.equalsIgnoreCase(CytobandParser.CYTOBAND) || 
					vFeature.getFeature().featureName.equalsIgnoreCase(CytobandParser.CYTOBANDS)){
					return vFeature.getFeature().featureName;
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
				if(style.isGraphTier() || !style.getSeparable()){
					return false;
				}
				return !style.getSeparate();
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
		} 
//		else if ((col == DELETE_FEATURE_COLUMN || col == REFRESH_FEATURE_COLUMN)
//				&& !vFeature.isPrimary()) {
//			return false;
//		} 
		else if (smv.getFloaterGlyph().getChildren() != null
				&& col != DELETE_FEATURE_COLUMN && col != FOREGROUND_COLUMN) {
			for (GlyphI i : smv.getFloaterGlyph().getChildren()) {
				if (((StyledGlyph) i).getAnnotStyle() == style) {
					return false;
				}
			}
		} else if ((vFeature.getStyle() != null && (vFeature.getStyle().isGraphTier() || !vFeature.getStyle().getSeparable()))
				&& (col == SEPARATE_COLUMN)) {
			return false;
		}

		if (col == REFRESH_FEATURE_COLUMN){
			if (smv.getAnnotatedSeq() == null || IGBConstants.GENOME_SEQ_ID.equals(smv.getAnnotatedSeq().getID())) {
				return false; 
			}
			return true;
		} if (col == DELETE_FEATURE_COLUMN 
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
				if (ScriptManager.SCRIPTING.equals(value) || Application.confirmPanel(message, 
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
					setVisibleTracks(vFeature);
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
					vFeature.getStyle().setSeparate(!((Boolean) value));
					smv.getPopup().refreshMap(false, true);
					smv.getTierManager().sortTiers();
				}
				break;
			case TRACK_NAME_COLUMN:
				if (vFeature.getStyle() != null && !vFeature.getStyle().getTrackName().equals((String) value)) {//TK
					vFeature.getStyle().setTrackName((String) value);
					
				}
				break;
			default:
				System.out.println("Unknown column selected: " + col);
		}

		fireTableCellUpdated(row, col);
		update(col);
		//if (!ScriptManager.SCRIPTING.equals(value)) {
		//	TierPrefsView.getSingleton().setRowSelection(vFeature.getStyle());
		//}
	}

	private void update(int col) {
		if (col == BACKGROUND_COLUMN || col == TRACK_NAME_COLUMN) {
			smv.getSeqMap().updateWidget();
		} else if (col == HIDE_FEATURE_COLUMN){
			smv.getSeqMap().repackTheTiers(false, true, true);
		} else if (col == FOREGROUND_COLUMN) {
			smv.updatePanel();
		}
	}

	private void setVisibleTracks(VirtualFeature vFeature) {
		LoadStrategy currentLoadStrategy = vFeature.getFeature().getLoadStrategy();
		ITrackStyleExtended style = vFeature.getStyle();
		
		if (style.getShow()) {
			style.setShow(false);
			if (!(currentLoadStrategy == LoadStrategy.GENOME)) {
				previousLoadStrategyMap.put(vFeature.getFeature(), currentLoadStrategy); // Store the load strategy for using below when un-hidden
				vFeature.getFeature().setLoadStrategy(LoadStrategy.NO_LOAD);
			}
		} else {
			style.setShow(true);
			if (!(currentLoadStrategy == LoadStrategy.GENOME)) {
				LoadStrategy previousLoadStrategy = previousLoadStrategyMap.get(vFeature.getFeature());
				vFeature.getFeature().setLoadStrategy((previousLoadStrategy == null) ? LoadStrategy.VISIBLE : previousLoadStrategy);
			}
		}
	}

	private List<TrackStyle> getCurrentStyles() {
		ArrayList<TrackStyle> currentStyleList = new ArrayList<TrackStyle>();
		if (smv != null) {
			LinkedHashMap<TrackStyle, TrackStyle> stylemap = new LinkedHashMap<TrackStyle, TrackStyle>();
			for (TierGlyph tier : new CopyOnWriteArrayList<TierGlyph>(smv.getSeqMap().getTiers())) {
				ITrackStyle style = tier.getAnnotStyle();
				if (style instanceof TrackStyle) {
					if (tier.getDirection() != TierGlyph.Direction.AXIS) {
						stylemap.put((TrackStyle) style, (TrackStyle) style);
					} else if (CytobandParser.CYTOBAND_TIER_NAME.equals(style.getMethodName())) {
						stylemap.put((TrackStyle) style, (TrackStyle) style);
					} else if (smv.getFloaterGlyph().getChildren() != null) {
						for (GlyphI g : smv.getFloaterGlyph().getChildren()) {
							StyledGlyph j = (StyledGlyph) g;
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
			smv.getAutoLoadAction().loadData();
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
	
	@Override
	public void tableChanged(TableModelEvent e) {
		if(virtualFeatures == null)
			return;
		
//		sort();
	}
}
