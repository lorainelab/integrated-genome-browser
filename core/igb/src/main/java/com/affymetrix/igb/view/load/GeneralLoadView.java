package com.affymetrix.igb.view.load;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.BioSeqUtils;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.quickload.QuickLoadSymLoader;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symloader.ResidueTrackSymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoaderInst;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleSymWithResidues;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.prefs.TierPrefsView;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.view.SeqGroupViewI;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;
import com.lorainelab.igb.genoviz.extensions.api.StyledGlyph;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.JTree;

/**
 * a class for initializing components and background methods implementation.
 *
 * @author nick & david
 */
@Component(name = GeneralLoadView.COMPONENT_NAME)
public final class GeneralLoadView {

    public static final String COMPONENT_NAME = "GeneralLoadView";
    private static final boolean DEBUG_EVENTS = false;
    private GenometryModel gmodel;
    private static SeqMapView gviewer;
    private static GenericAction refreshDataAction;
    private static JRPButton partial_residuesB;
    private static DataManagementTableModel tableModel;
    private FeatureTreeView featureTreeView;
   
    //gui components
    private static JTableX table;
    private static JTree tree;
    private boolean showLoadingConfirm = false;

    private GeneralLoadUtilsService generalLoadUtils;
    private IGBService igbService;
    private SeqGroupViewI seqGroupView;

    /**
     * Creates new form GeneralLoadView
     */
    @Activate
    public void activate() {
        gmodel = GenometryModel.getInstance();
        gviewer = Application.getSingleton().getMapView();
        initComponents();
        generalLoadUtils.loadServerMapping();
        PreferencesPanel.getSingleton();
    }

    private void initComponents() {
        featureTreeView = new FeatureTreeView();
        tree = featureTreeView.getTree();
        tableModel = new DataManagementTableModel(this);
        tableModel.addTableModelListener(TrackstylePropertyMonitor.getPropertyTracker());
        table = new JTableX("GeneralLoadView_DataManagementTable", tableModel);
        table.setCellSelectionEnabled(false);
        TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(table);
        initDataManagementTable();
        refreshDataAction = gviewer.getRefreshDataAction();
        partial_residuesB = gviewer.getPartial_residuesButton();
        partial_residuesB.setEnabled(false);
        refreshDataAction.setEnabled(false);
    }

    public JTree getTree() {
        return tree;
    }

    public DataManagementTableModel getTableModel() {
        return tableModel;
    }

    public JTable getTable() {
        return table;
    }



    public void refreshTreeView() {

        ThreadUtils.runOnEventQueue(new Runnable() {

            @Override
            public void run() {
                refreshTree();
            }
        });
    }

    private void refreshTree() {
        final List<GenericFeature> features = generalLoadUtils.getSelectedVersionFeatures();
        if (features == null || features.isEmpty()) {
            tableModel.clearFeatures();
        }
        featureTreeView.initOrRefreshTree(features);
    }

    public void refreshTreeViewAndRestore() {
        ThreadUtils.runOnEventQueue(new Runnable() {

            @Override
            public void run() {
                String state = featureTreeView.getState();
                refreshTree();
                featureTreeView.restoreState(state);
            }
        });
    }

    public void refreshDataManagementTable(final List<GenericFeature> visibleFeatures) {

        ThreadUtils.runOnEventQueue(new Runnable() {

            @Override
            public void run() {
                table.stopCellEditing();
                tableModel.createVirtualFeatures(visibleFeatures);
                DataManagementTable.setComboBoxEditors(table, !isGenomeSequence());
            }
        });
    }

    public void refreshDataManagementView() {
        final List<GenericFeature> visibleFeatures = generalLoadUtils.getVisibleFeatures();
        refreshDataManagementTable(visibleFeatures);

        disableButtonsIfNecessary();
        changeVisibleDataButtonIfNecessary(visibleFeatures);
    }

    private void initDataManagementTable() {
        final List<GenericFeature> visibleFeatures = generalLoadUtils.getVisibleFeatures();
        int maxFeatureNameLength = 1;
        for (GenericFeature feature : visibleFeatures) {
            maxFeatureNameLength = Math.max(maxFeatureNameLength, feature.featureName.length());
        }
        final int finalMaxFeatureNameLength = maxFeatureNameLength;	// necessary for threading
        table.stopCellEditing();
        tableModel.createVirtualFeatures(visibleFeatures);

        table.getColumnModel().getColumn(DataManagementTableModel.REFRESH_FEATURE_COLUMN).setPreferredWidth(20);
        table.getColumnModel().getColumn(DataManagementTableModel.REFRESH_FEATURE_COLUMN).setMinWidth(20);
        table.getColumnModel().getColumn(DataManagementTableModel.REFRESH_FEATURE_COLUMN).setMaxWidth(20);
        table.getColumnModel().getColumn(DataManagementTableModel.HIDE_FEATURE_COLUMN).setPreferredWidth(24);
        table.getColumnModel().getColumn(DataManagementTableModel.HIDE_FEATURE_COLUMN).setMinWidth(24);
        table.getColumnModel().getColumn(DataManagementTableModel.HIDE_FEATURE_COLUMN).setMaxWidth(24);
        table.getColumnModel().getColumn(DataManagementTableModel.LOAD_STRATEGY_COLUMN).setPreferredWidth(120);
        table.getColumnModel().getColumn(DataManagementTableModel.LOAD_STRATEGY_COLUMN).setMinWidth(110);
        table.getColumnModel().getColumn(DataManagementTableModel.LOAD_STRATEGY_COLUMN).setMaxWidth(130);
        table.getColumnModel().getColumn(DataManagementTableModel.TRACK_NAME_COLUMN).setPreferredWidth(finalMaxFeatureNameLength);
        table.getColumnModel().getColumn(DataManagementTableModel.TRACK_NAME_COLUMN).setMinWidth(110);
        //dataManagementTable.getColumnModel().getColumn(DataManagementTableModel.FEATURE_NAME_COLUMN).setMaxWidth(200);
        table.getColumnModel().getColumn(DataManagementTableModel.TRACK_NAME_COLUMN).setPreferredWidth(130);
        table.getColumnModel().getColumn(DataManagementTableModel.TRACK_NAME_COLUMN).setMinWidth(130);
        table.getColumnModel().getColumn(DataManagementTableModel.DELETE_FEATURE_COLUMN).setPreferredWidth(15);
        table.getColumnModel().getColumn(DataManagementTableModel.DELETE_FEATURE_COLUMN).setMinWidth(15);
        table.getColumnModel().getColumn(DataManagementTableModel.DELETE_FEATURE_COLUMN).setMaxWidth(15);
        table.getColumnModel().getColumn(DataManagementTableModel.BACKGROUND_COLUMN).setPreferredWidth(29);
        table.getColumnModel().getColumn(DataManagementTableModel.BACKGROUND_COLUMN).setMinWidth(29);
        table.getColumnModel().getColumn(DataManagementTableModel.BACKGROUND_COLUMN).setMaxWidth(29);
        table.getColumnModel().getColumn(DataManagementTableModel.FOREGROUND_COLUMN).setPreferredWidth(27);
        table.getColumnModel().getColumn(DataManagementTableModel.FOREGROUND_COLUMN).setMinWidth(27);
        table.getColumnModel().getColumn(DataManagementTableModel.FOREGROUND_COLUMN).setMaxWidth(27);
        table.getColumnModel().getColumn(DataManagementTableModel.SEPARATE_COLUMN).setPreferredWidth(35);
        table.getColumnModel().getColumn(DataManagementTableModel.SEPARATE_COLUMN).setMinWidth(35);
        table.getColumnModel().getColumn(DataManagementTableModel.SEPARATE_COLUMN).setMaxWidth(35);

        // Don't enable combo box for full genome sequence
        // Enabling of combo box for local files with unknown chromosomes happens in setComboBoxEditors()
        DataManagementTable.setComboBoxEditors(table, !GeneralLoadView.isGenomeSequence());
    }

    /**
     * Check if it is necessary to disable buttons.
     */
    public boolean getIsDisableNecessary() {
        boolean enabled = !isGenomeSequence();
        if (enabled) {
            BioSeq curSeq = gmodel.getSelectedSeq();
            enabled = curSeq.getSeqGroup() != null;	// Don't allow a null sequence group either.
            if (enabled) {		// Don't allow buttons for an "unknown" versionName
                Set<GenericVersion> gVersions = curSeq.getSeqGroup().getEnabledVersions();
                enabled = (!gVersions.isEmpty());
            }
        }
        return enabled;
    }

    /**
     * Don't allow buttons to be used if they're not valid.
     */
    private void disableButtonsIfNecessary() {
        // Don't allow buttons for a full genome sequence
        setAllButtons(getIsDisableNecessary());
    }

    public void disableAllButtons() {
        setAllButtons(false);
    }

    private void setAllButtons(final boolean enabled) {
        ThreadUtils.runOnEventQueue(new Runnable() {
            @Override
            public void run() {
                partial_residuesB.setEnabled(enabled);
                refreshDataAction.setEnabled(enabled);
            }
        });
    }

    /**
     * Accessor method. See if we need to enable/disable the refresh_dataB
     * button by looking at the features' load strategies.
     */
    void changeVisibleDataButtonIfNecessary(List<GenericFeature> features) {
        if (isGenomeSequence()) {
            return;
            // Currently not enabling this button for the full sequence.
        }
        boolean enabled = false;
        for (GenericFeature gFeature : features) {
            if (gFeature.getLoadStrategy() != LoadStrategy.NO_LOAD && gFeature.getLoadStrategy() != LoadStrategy.GENOME) {
                enabled = true;
                break;
            }
        }
        if (refreshDataAction.isEnabled() != enabled) {
            refreshDataAction.setEnabled(enabled);
        }
    }

    private boolean isGenomeSequence() {
        BioSeq curSeq = gmodel.getSelectedSeq();
        final String seqID = curSeq == null ? null : curSeq.getID();
        return (seqID == null || IGBConstants.GENOME_SEQ_ID.equals(seqID));
    }

    public String getSelectedSpecies() {
        return (String) seqGroupView.getSpeciesCB().getSelectedItem();
    }
    
       public FeatureTreeView getFeatureTree() {
        return featureTreeView;
    }

    public void setShowLoadingConfirm(boolean showLoadingConfirm) {
        this.showLoadingConfirm = showLoadingConfirm;
    }

    public boolean isLoadingConfirm() {
        return showLoadingConfirm;
    }

    @Reference
    public void setGeneralLoadUtils(GeneralLoadUtilsService generalLoadUtils) {
        this.generalLoadUtils = generalLoadUtils;
    }

}
