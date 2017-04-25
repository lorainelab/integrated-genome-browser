package com.affymetrix.igb.view.load;

import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.parsers.CytobandParser;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.prefs.TierPrefsView;
import com.affymetrix.igb.swing.script.ScriptManager;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.genometry.style.ITrackStyle;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.igb.view.SeqMapView;
import com.google.common.collect.Maps;
import org.lorainelab.igb.genoviz.extensions.glyph.StyledGlyph;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

/**
 * Model for table of features.
 * 
 * When the DataManagementTable is generated, a row is made for each DataSet 
 * and this object is used to retrieve the style info to use for each row 
 * including (foreground color, background color, hidden/visible, and the 
 * name to display). As of IGBF-201, rows are generated the same way, BUT THEN 
 * additional rows are made for each style in joinedGraphStyleReference using 
 * only the style info, with no corresponding DataSet (so in those rows, there 
 * is no option to refresh or delete the track or change the load mode).
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
    private final SeqMapView smv;
    private final AffyLabelledTierMap map;
    private final Map<DataSet, TrackStyle> feature2StyleReference; //holds styles that are associated with a single DataSet
    private final List<ITrackStyleExtended> joinedGraphStyleReference; //added to accomodate joined graphs <Ivory Blakley> IGBF-201
    public List<DataSet> features; // holds the features that are associated with the styles in feature2StyleReference

    DataManagementTableModel(GeneralLoadView glv) {
        this.glv = glv;
        this.features = new ArrayList<>();
        feature2StyleReference = Maps.newHashMap();
        joinedGraphStyleReference = new ArrayList<>(); // <Ivory Blakley> IGBF-201
        IGB igb = IGB.getInstance();
        smv = igb.getMapView();
        map = (AffyLabelledTierMap) smv.getSeqMap();
        map.addTierOrderListener(this);
        // Here we map the friendly string back to the LoadStrategy.
        this.reverseLoadStrategyMap = new HashMap<>(3);
        for (LoadStrategy strategy : EnumSet.allOf(LoadStrategy.class)) {
            this.reverseLoadStrategyMap.put(strategy.toString(), strategy);
        }
    }

    public void clearFeatures() {
        features.clear();
        feature2StyleReference.clear();
        joinedGraphStyleReference.clear(); // <Ivory Blakley> IGBF-201
        fireTableDataChanged();
        TierPrefsView.getSingleton().clearTable();
    }

    void generateFeature2StyleReference(List<DataSet> theFeatures) {        
        feature2StyleReference.clear();
        joinedGraphStyleReference.clear();
        // associate styles with the new features
        theFeatures.forEach(this::createFeature2StyleReference);
        // catch styles that do not return a single DataSet, intended for joined graph styles
        createJoinedGraphStyleReference();
        // use the keyset from the style hashmap to create the list of features to display in the table
        features = new ArrayList<>(feature2StyleReference.keySet());
        // sort the features
        Collections.sort(features, new featureTableComparator());
        //refresh the table
        fireTableDataChanged();
    }

    /**
     * Some file formats might have multiple tracks, try load GFF1_example.gff
     *
     * Some styles are not TrackStyle objects. For instance, joined graphs use SimpleTrackStyle.
     * In feature2StyleReference, only store the styles that link to exactly one DataSet
     * 
     * DataSets do not include a pointer to the corresponding style, 
     * but styles (well, some styles) include a pointer to the corresponding 
     * DataSet. generateFeature2StyleReference() works essentially like an 
     * outer for loop, with createFeature2StyleReference() as an inner for loop.
     * pseudo-code:
     * for every DataSet that is passed in:
     * -- look at all of the current styles, 
     * -- for each of these styles:
     * -- -- if it is the style that goes with this DataSet, 
     * -- -- -- put this DataSet and this style into feature2styleReference as a key/value pair.
     * 
     * <Ivory Blakley> IGBF-201
     */
    private void createFeature2StyleReference(DataSet gFeature) {
        for (ITrackStyleExtended style : getTierGlyphStyles()) {
            if (style.getFeature() == gFeature && style instanceof TrackStyle) {
                feature2StyleReference.put(gFeature, (TrackStyle) style);
                break; //once you find the style that goes with a given feature, stop looping through styles.
            }
        }
    }
    
    /**
     * Catch the styles that are for joined graphs.
     * <Ivory Blakley> IGBF-201
     */
    private void createJoinedGraphStyleReference(){
        for (ITrackStyleExtended style : getTierGlyphStyles()) {
            if (style.getFeature() == null) {
                joinedGraphStyleReference.add(style);
            }
        }
    }

    private final static class featureTableComparator implements Comparator<DataSet> {

        @Override
        public int compare(DataSet left, DataSet right) {
            // Keep the features in logical groups. Within groups, sort by name.
            // Sort first by where the data is coming from, then by the full name of the dataset.
            // Dataset names include the file-system-like structure that we see in the data-access panel.

            int comp = left.getDataContainer().getDataProvider().getName().compareTo(right.getDataContainer().getDataProvider().getName());
            if (comp != 0) {
                return comp;
            }
            
            return left.getDataSetName().compareTo(right.getDataSetName());
        }
    }

    /**
     * There may or may not be a feature (a DataSet) for this row. 
     * If there are n features, the first n rows correspond to those features.
     * For those n rows, return the nth feature.  For any rows beyond that,
     * return null.
     * @param row
     * @return 
     */
    public DataSet getRowFeature(int row) {
        // some rows in the table may be after the last feature. For those, return null for the feature.
        return (features.size() <= row) ? null : features.get(row);
    }

    public TrackStyle getStyleFromFeature(DataSet feature) {
        return feature2StyleReference.get(feature);
    }
    
    /**
     * If there is a feature for this row, get the feature data, 
     * and use that to get the associated style from feature2StyleReference.
     * This is the case for most tracks.
     * Otherwise, (if row is greater than the number of features),
     * get a style from joinedGraphStyleReference. This is the case for joined graphs.
     * <Ivory Blakley> IGBF-201
     * @param row
     * @return 
     */
    public ITrackStyleExtended getStyleFromRow(int row){
        if (row < features.size()){
            // get the DataSet from features and use that to get the style from feature2StyleReference
            return feature2StyleReference.get(features.get(row));
        } else{
        int styleIndex = row - features.size();
        return joinedGraphStyleReference.get(styleIndex);
        }
    }

    private int getRow(DataSet feature) {
        return features.indexOf(feature);

    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        // Make a row for each DataSet that is associated with a style (ie, not a joined graph) make a row for that dataset.
        // Then make rows for each style that is not linked to a feature (ie, each joined graph). <Ivory Blakley> IGBF-201
        return features.size() + joinedGraphStyleReference.size();
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (features.isEmpty() && joinedGraphStyleReference.isEmpty()) {
            // Indicate to user that there's no data.
            if (row == 0 && col == 2) {
                return "No feature data found";
            }
            return "";
        }

        DataSet feature;
        ITrackStyleExtended style; // changed from TrackStyle to ITrackStyleExtended, <Ivory Blakley> IGBF-201
        feature = getRowFeature(row);
        style = getStyleFromRow(row);

        // Modified to accommodate the possibility that feature is null and information should come from the style only
        // <Ivory Blakley> IGBF-201
        switch (col) {
            case REFRESH_FEATURE_COLUMN:
                return "";
            case LOAD_STRATEGY_COLUMN:
                if (feature == null) {
                    return "";
                } else {
                    return feature.getLoadStrategy().toString();
                }
            case TRACK_NAME_COLUMN:
                // Get the track name from the style. 
                // If there is no style and you have to get the track name from the feature, 
                // then there are some conditions that might have to be handled specially.
                if (style == null) {
                    if (feature == null) {
                        return "";
                    } else {
                        if (feature.getDataSetName().equals(CytobandParser.CYTOBAND_TIER_NAME)
                                || feature.getDataSetName().equalsIgnoreCase(CytobandParser.CYTOBAND)
                                || feature.getDataSetName().equalsIgnoreCase(CytobandParser.CYTOBANDS)) {
                            try {
                                return URLDecoder.decode(feature.getDataSetName(), "UTF-8");
                            } catch (UnsupportedEncodingException ex) {
                                return feature.getDataSetName();
                            }
                        } else {
                            return feature.getDataSetName();
                        }
                    }
                }
                return style.getTrackName();
            case FOREGROUND_COLUMN:
                // add support SimpleTrackStyle objects, which return null for getMethodName().  <Ivory Blakley> IGBF-201
                if (style == null || (style.getMethodName() != null && style.getMethodName().matches(CytobandParser.CYTOBAND_TIER_NAME))) {
                    return Color.WHITE;
                }
                return style.getForeground();
            case BACKGROUND_COLUMN:
                // add support SimpleTrackStyle objects, which return null for getMethodName().  <Ivory Blakley> IGBF-201
                if (style == null || (style.getMethodName() != null && style.getMethodName().matches(CytobandParser.CYTOBAND_TIER_NAME))) {
                    return Color.WHITE;
                }
                return style.getBackground();
            case SEPARATE_COLUMN:
                // add support SimpleTrackStyle objects, which return null for getMethodName().  <Ivory Blakley> IGBF-201
                if (style == null || (style.getMethodName() != null && style.getMethodName().matches(CytobandParser.CYTOBAND_TIER_NAME))) {
                    return false;
                }
                if (style.isGraphTier() || !style.getSeparable()) {
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
        DataSet feature = getRowFeature(row);
        ITrackStyleExtended style = getStyleFromRow(row); // <Ivory Blakley> IGBF-201

        if ((style == null)
                && (col == TRACK_NAME_COLUMN
                || col == BACKGROUND_COLUMN || col == FOREGROUND_COLUMN
                || col == SEPARATE_COLUMN || col == HIDE_FEATURE_COLUMN)) {
            return false;
        // add support SimpleTrackStyle objects, which return null for getMethodName().  <Ivory Blakley> IGBF-201
        } else if (style != null && style.getMethodName() != null && style.getMethodName().matches(CytobandParser.CYTOBAND_TIER_NAME)) {
            return col == HIDE_FEATURE_COLUMN || col == REFRESH_FEATURE_COLUMN || col == DELETE_FEATURE_COLUMN;
        } //		else if ((col == DELETE_FEATURE_COLUMN || col == REFRESH_FEATURE_COLUMN)
        //				&& !vFeature.isCacheServer()) {
        //			return false;
        //		}
        else if (smv.getFloaterGlyph().getChildren() != null
                && col != DELETE_FEATURE_COLUMN && col != FOREGROUND_COLUMN) {
            for (GlyphI i : smv.getFloaterGlyph().getChildren()) {
                if (((StyledGlyph) i).getAnnotStyle() == style) {
                    return false;
                }
            }
        } else if ((style != null && (style.isGraphTier() || !style.getSeparable()))
                && (col == SEPARATE_COLUMN)) {
            return false;
        }

        if (col == REFRESH_FEATURE_COLUMN) {
            if (smv.getAnnotatedSeq() == null || IGBConstants.GENOME_SEQ_ID.equals(smv.getAnnotatedSeq().getId())) {
                return false;
            }
            return true;
        }
        if (col == SEPARATE_COLUMN) {
            return !style.isShowAsPaired();
        }
        if (col == DELETE_FEATURE_COLUMN
                || col == HIDE_FEATURE_COLUMN || col == TRACK_NAME_COLUMN
                || col == BACKGROUND_COLUMN || col == FOREGROUND_COLUMN) {
            return true;
        } else if (getRowFeature(row) == null) {
            return false;
        }

        // This cell is only editable if the feature isn't already fully loaded.
        return (getRowFeature(row).getLoadStrategy() != LoadStrategy.GENOME
                && getRowFeature(row).getLoadChoices().size() > 1);
    }

    @Override
    /**
     * Modified to accommodate the possibility that feature is null and information should come from the style only
     * <Ivory Blakley> IGBF-201
     */
    public void setValueAt(Object value, int row, int col) {
        DataSet feature = getRowFeature(row);
        ITrackStyleExtended style = getStyleFromRow(row); // <Ivory Blakley> IGBF-201
        if (value == null) {
            return;
        }

        switch (col) {
            case DELETE_FEATURE_COLUMN:
                if (feature != null) {
                    String message = "Really remove entire " + feature.getDataSetName() + " data set ?";
                    if (ScriptManager.SCRIPTING.equals(value) || ModalUtils.confirmPanel(message,
                            PreferenceUtils.CONFIRM_BEFORE_DELETE, PreferenceUtils.default_confirm_before_delete)) {
                        features.stream().filter(gFeature -> gFeature.equals(feature)).forEach(gFeature -> {
                            GeneralLoadView.getLoadView().removeDataSet(gFeature, true);
                        });
                        this.fireTableDataChanged(); //clear row selection
                    }
                }
                break;
            case REFRESH_FEATURE_COLUMN:
                if (feature != null){
                if (feature.getLoadStrategy() != LoadStrategy.NO_LOAD
                        && feature.getLoadStrategy() != LoadStrategy.GENOME) {
                    GeneralLoadView.getLoadView().setShowLoadingConfirm(true);
                    features.stream().filter(gFeature -> gFeature.equals(feature)).forEach(GeneralLoadUtils::loadAndDisplayAnnotations);
                }
                }
                break;
            case LOAD_STRATEGY_COLUMN:
                if (feature != null){
                if (feature.getLoadStrategy() == LoadStrategy.GENOME) {
                    return;	// We can't change strategies once we've loaded the entire genome.
                }
                if (feature.getLoadChoices().size() <= 1) {
                    return;
                }
                String valueString = value.toString();
                if (!feature.getLoadStrategy().toString().equals(valueString)) {
                    // strategy changed.  Update the feature object.
                    feature.setLoadStrategy(reverseLoadStrategyMap.get(valueString));
                    updatedStrategy(row, col, feature);
                }
                }
                break;
            case HIDE_FEATURE_COLUMN:
                if (style != null) {
                    setVisibleTracks(row); // <Ivory Blakley> IGBF-201
                }
                break;
            case BACKGROUND_COLUMN:
                if (style != null) {
                    style.setBackground((Color) value);
                }
                break;
            case FOREGROUND_COLUMN:
                if (style != null) {
                    style.setForeground((Color) value);
                }
                break;
            case SEPARATE_COLUMN:
                if (style != null) {
                    style.setSeparate(!((Boolean) value));
                    smv.getPopup().refreshMap(false, true);
                    smv.getTierManager().sortTiers();
                }
                break;
            case TRACK_NAME_COLUMN:
                if (style != null && !style.getTrackName().equals(value)) {//TK
                    style.setTrackName((String) value);

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
        } else if (col == HIDE_FEATURE_COLUMN) {
            smv.getSeqMap().repackTheTiers(false, false, true);
        } else if (col == FOREGROUND_COLUMN) {
            smv.updatePanel();
        }
    }

    // take a row instead a feature <Ivory Blakley> IGBF-201
    private void setVisibleTracks(int row) {
        ITrackStyle style = getStyleFromRow(row);
        if (style.getShow()) {
            style.setShow(false);
        } else {
            style.setShow(true);
        }
    }

    final Predicate<? super TierGlyph> tierHasDirection = tier -> tier.getDirection() != StyledGlyph.Direction.AXIS;

    // Use type ITrackStyleExtended rather than TrackStyle <Ivory Blakley> IGBF-201
    private Set<ITrackStyleExtended> getTierGlyphStyles() {
        return smv.getSeqMap().getTiers().stream()
                .filter(tierHasDirection)
                .map(tier -> tier.getAnnotStyle())
                .filter(style -> style instanceof ITrackStyleExtended)
                .map(style -> (ITrackStyleExtended) style)
                .collect(Collectors.toSet());
    }

    /**
     * The strategy was changed. Update the table, and if necessary, load the
     * annotations and change the button statuses.
     *
     * @param row
     * @param col
     * @param gFeature
     */
    private void updatedStrategy(int row, int col, DataSet gFeature) {
        fireTableCellUpdated(row, col);

        if (gFeature.getLoadStrategy() == LoadStrategy.GENOME) {
            GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);
        } else if (gFeature.getLoadStrategy() == LoadStrategy.AUTOLOAD) {
            // This would trigger auto load
            smv.getAutoLoadAction().loadData();
        }

        //  Whatever feature strategy changed, it may have affected
        // the enable status of the "load visible" button
        this.glv.changeVisibleDataButtonIfNecessary(features);
    }

    @Override
    public void stateChanged(ChangeEvent evt) {//????
        Object src = evt.getSource();
        if (src instanceof DataSet) {
            int row = getRow((DataSet) src);
            if (row >= 0) {  // if typestate is present in table, then send notification of row change
                fireTableRowsUpdated(row, row);
            }
        }
    }

    @Override
    public void tableChanged(TableModelEvent e) {
    }
}
