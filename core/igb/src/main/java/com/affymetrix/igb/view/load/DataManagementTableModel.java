package com.affymetrix.igb.view.load;

import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.parsers.CytobandParser;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.prefs.TierPrefsView;
import com.affymetrix.igb.swing.script.ScriptManager;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.view.SeqMapView;
import com.google.common.collect.Maps;
import com.lorainelab.igb.genoviz.extensions.glyph.StyledGlyph;
import com.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import java.awt.Color;
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
    private final Map<DataSet, TrackStyle> feature2StyleReference;
    public List<DataSet> features;

    DataManagementTableModel(GeneralLoadView glv) {
        this.glv = glv;
        this.features = new ArrayList<>();
        feature2StyleReference = Maps.newHashMap();
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
        fireTableDataChanged();
        TierPrefsView.getSingleton().clearTable();
    }

    void generateFeature2StyleReference(List<DataSet> features) {
        // Sort these features so the features to be loaded are at the top.
        Collections.sort(features, new featureTableComparator());
        this.features = features;
        feature2StyleReference.clear();
        features.forEach(this::createPrimaryVirtualFeatures);
        fireTableDataChanged();
    }

    /*
     * Some file formats might have multiple tracks, try load GFF1_example.gff
     */
    private void createPrimaryVirtualFeatures(DataSet gFeature) {
        for (TrackStyle style : getTierGlyphStyles()) {
            if (style.getFeature() == gFeature) {
                feature2StyleReference.put(gFeature, style);
                if (!style.isGraphTier()) {
                    break;
                }
            }
        }
        features = new ArrayList<>(feature2StyleReference.keySet());
    }

    private final static class featureTableComparator implements Comparator<DataSet> {

        @Override
        public int compare(DataSet left, DataSet right) {
            if (left.getLoadStrategy() != right.getLoadStrategy()) {
                return (left.getLoadStrategy().compareTo(right.getLoadStrategy()));
            }
            if (left.getDataSetName().compareTo(right.getDataSetName()) != 0) {
                return left.getDataSetName().compareTo(right.getDataSetName());
            }

            return left.getDataContainer().getDataProvider().getName().compareTo(right.getDataContainer().getDataProvider().getName());
        }
    }

    public DataSet getRowFeature(int row) {
        return (getRowCount() <= row) ? null : features.get(row);
    }

    public TrackStyle getStyleFromFeature(DataSet feature) {
        return feature2StyleReference.get(feature);
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
        return features.size();
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (features.isEmpty()) {
            // Indicate to user that there's no data.
            if (row == 0 && col == 2) {
                return "No feature data found";
            }
            return "";
        }

        DataSet feature;
        TrackStyle style;
        if (getRowFeature(row) == null) {
            return "";
        } else {
            feature = getRowFeature(row);
            style = feature2StyleReference.get(feature);
        }

        switch (col) {
            case REFRESH_FEATURE_COLUMN:
                return "";
            case LOAD_STRATEGY_COLUMN:
                // return the load strategy
//				if (!vFeature.isCacheServer()) {
//					return "";
//				}
                return feature.getLoadStrategy().toString();
            case TRACK_NAME_COLUMN:
                if (feature.getDataSetName().equals(CytobandParser.CYTOBAND_TIER_NAME)
                        || feature.getDataSetName().equalsIgnoreCase(CytobandParser.CYTOBAND)
                        || feature.getDataSetName().equalsIgnoreCase(CytobandParser.CYTOBANDS)) {
                    return feature.getDataSetName();
                } else if (style == null) {
                    return feature.getDataSetName();
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
        TrackStyle style = feature2StyleReference.get(feature);

        if ((style == null)
                && (col == TRACK_NAME_COLUMN
                || col == BACKGROUND_COLUMN || col == FOREGROUND_COLUMN
                || col == SEPARATE_COLUMN || col == HIDE_FEATURE_COLUMN)) {
            return false;
        } else if (style != null && style.getMethodName().matches(CytobandParser.CYTOBAND_TIER_NAME)) {
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
    public void setValueAt(Object value, int row, int col) {
        DataSet feature = getRowFeature(row);
        TrackStyle style = feature2StyleReference.get(feature);
        if (value == null || feature == null) {
            return;
        }

        switch (col) {
            case DELETE_FEATURE_COLUMN:
                String message = "Really remove entire " + feature.getDataSetName() + " data set ?";
                if (ScriptManager.SCRIPTING.equals(value) || ModalUtils.confirmPanel(message,
                        PreferenceUtils.CONFIRM_BEFORE_DELETE, PreferenceUtils.default_confirm_before_delete)) {
                    features.stream().filter(gFeature -> gFeature.equals(feature)).forEach(gFeature -> {
                        GeneralLoadView.getLoadView().removeDataSet(gFeature, true);
                    });
                    this.fireTableDataChanged(); //clear row selection
                }
                break;
            case REFRESH_FEATURE_COLUMN:
                if (feature.getLoadStrategy() != LoadStrategy.NO_LOAD
                        && feature.getLoadStrategy() != LoadStrategy.GENOME) {
                    GeneralLoadView.getLoadView().setShowLoadingConfirm(true);
                    features.stream().filter(gFeature -> gFeature.equals(feature)).forEach(GeneralLoadUtils::loadAndDisplayAnnotations);
                }
                break;
            case LOAD_STRATEGY_COLUMN:
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
                break;
            case HIDE_FEATURE_COLUMN:
                if (style != null) {
                    setVisibleTracks(feature);
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

    private void setVisibleTracks(DataSet feature) {
        TrackStyle style = feature2StyleReference.get(feature);
        if (style.getShow()) {
            style.setShow(false);
        } else {
            style.setShow(true);
        }
    }

    final Predicate<? super TierGlyph> tierHasDirection = tier -> tier.getDirection() != StyledGlyph.Direction.AXIS;

    private Set<TrackStyle> getTierGlyphStyles() {
        return smv.getSeqMap().getTiers().stream()
                .filter(tierHasDirection)
                .map(tier -> tier.getAnnotStyle())
                .filter(style -> style instanceof TrackStyle)
                .map(style -> (TrackStyle) style)
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
