package com.affymetrix.igb.view;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.comparator.AlphanumComparator;
import com.affymetrix.genometry.comparator.SeqSymIdComparator;
import com.affymetrix.genometry.comparator.StringVersionDateComparator;
import com.affymetrix.genometry.event.GenomeVersionSelectionEvent;
import com.affymetrix.genometry.event.GroupSelectionListener;
import com.affymetrix.genometry.event.SeqSelectionEvent;
import com.affymetrix.genometry.event.SeqSelectionListener;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.DisplayUtils;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.SpeciesLookup;
import com.affymetrix.genometry.util.ThreadUtils;
import com.affymetrix.igb.EventService;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.action.AutoLoadFeatureAction;
import com.affymetrix.igb.general.DataProviderManager.DataProviderServiceChangeEvent;
import com.affymetrix.igb.swing.JRPComboBox;
import com.affymetrix.igb.swing.JRPComboBoxWithSingleListener;
import com.affymetrix.igb.swing.jide.JRPStyledTable;
import com.affymetrix.igb.util.JComboBoxToolTipRenderer;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.igb.view.welcome.MainWorkspaceManager;
import com.google.common.eventbus.Subscribe;
import com.lorainelab.igb.services.IgbService;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lorainelab
 */
public class SeqGroupView implements ItemListener, ListSelectionListener,
        GroupSelectionListener, SeqSelectionListener {

    private static final Logger logger = LoggerFactory.getLogger(SeqGroupView.class);
    private static final NumberFormat nformat = NumberFormat.getIntegerInstance(Locale.ENGLISH);
    private static final boolean DEBUG_EVENTS = false;
    public static final String SELECT_SPECIES = IGBConstants.BUNDLE.getString("speciesCap");
    public static final String SELECT_GENOME = IGBConstants.BUNDLE.getString("genomeVersionCap");
    private static final GenometryModel gmodel = GenometryModel.getInstance();
    protected String[] columnToolTips = {null, BUNDLE.getString("sequenceHeaderLengthToolTip")};
    private final JRPStyledTable seqTable;
    private final ListSelectionModel lsm;
    private BioSeq selected_seq = null;
    private GenomeVersion previousGenomeVersion = null;
    private TableRowSorter<SeqGroupTableModel> sorter;
    private static SeqGroupView singleton;
    private JComboBoxToolTipRenderer versionCBRenderer;
    private JComboBoxToolTipRenderer speciesCBRenderer;
    private GenomeVersion curGroup = null;
    private volatile boolean lookForPersistentGenome = true;
    private static SeqMapView gviewer;
    private JRPComboBox speciesCB;
    private JRPComboBox versionCB;
    private final IgbService igbService;
    private SelectVersionPanel selectVersionPanel;
    private boolean defSort = true;

    SeqGroupView(IgbService igbService) {
        this.igbService = igbService;
        gviewer = IGB.getInstance().getMapView();
        selectVersionPanel = new SelectVersionPanel();
        seqTable = new JRPStyledTable("SeqGroupView_seqtable") {

            private static final long serialVersionUID = 1L;
            //Implement table header tool tips.

            @Override
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return columnToolTips[realIndex];
                    }
                };
            }
        };
        seqTable.setToolTipText(BUNDLE.getString("chooseSeq"));
        seqTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        seqTable.setCellSelectionEnabled(false);
        seqTable.setRowSelectionAllowed(true);

        seqTable.getTableHeader().addMouseListener(new MouseAdapter() {

            private int lastCol = 99;

            @Override
            public void mousePressed(MouseEvent e) {
                int col = seqTable.columnAtPoint(e.getPoint());
                if (col == lastCol) {
                    return;
                }
                lastCol = col;
                if (col == 0) {
                    sorter = new TableRowSorter<SeqGroupTableModel>((SeqGroupTableModel) seqTable.getModel()) {

                        @Override
                        public Comparator<?> getComparator(int column) {
                            return new BioSeqAlphanumComparator();
                        }
                    };
                } else {
                    sorter = new TableRowSorter<SeqGroupTableModel>((SeqGroupTableModel) seqTable.getModel()) {

                        @Override
                        public Comparator<?> getComparator(int column) {
                            return new SeqLengthComparator();
                        }
                    };
                }
                seqTable.setRowSorter(sorter);
                sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(col, SortOrder.ASCENDING)));
            }

        });

        lsm = seqTable.getSelectionModel();
        lsm.addListSelectionListener(this);

        speciesCB = new JRPComboBoxWithSingleListener("DataAccess_species");
        speciesCB.addItem(SELECT_SPECIES);
        speciesCB.setMaximumSize(new Dimension(speciesCB.getPreferredSize().width * 4, speciesCB.getPreferredSize().height));
        speciesCB.setEnabled(false);
        speciesCB.setEditable(false);
        speciesCB.setToolTipText("Choose" + " " + SELECT_SPECIES);
        speciesCBRenderer = new JComboBoxToolTipRenderer();
        speciesCB.setRenderer(speciesCBRenderer);
        speciesCBRenderer.setToolTipEntry(SELECT_SPECIES, "Choose" + " " + SELECT_SPECIES);

        versionCB = new JRPComboBoxWithSingleListener("DataAccess_version");
        versionCB.addItem(SELECT_GENOME);
        versionCB.setMaximumSize(new Dimension(versionCB.getPreferredSize().width * 4, versionCB.getPreferredSize().height));
        versionCB.setEnabled(false);
        versionCB.setEditable(false);
        versionCB.setToolTipText("Choose" + " " + SELECT_GENOME);
        versionCBRenderer = new JComboBoxToolTipRenderer();
        versionCB.setRenderer(versionCBRenderer);
        versionCBRenderer.setToolTipEntry(SELECT_GENOME, "Choose" + " " + SELECT_GENOME);

    }

    static void init(IgbService igbService) {
        singleton = new SeqGroupView(igbService);
        singleton.addListeners();
        EventService.getModuleEventBus().register(singleton);
        EventService.getModuleEventBus().post(new DataProviderServiceChangeEvent());
    }

    public static SeqGroupView getInstance() {
        return singleton;
    }

    /**
     * Refresh seqtable if more chromosomes are added, for example.
     */
    public void refreshTable() {
        final AbstractTableModel model = ((AbstractTableModel) seqTable.getModel());
        model.fireTableDataChanged();
        ThreadUtils.runOnEventQueue(() -> {
            if (seqTable.getTableHeader().getColumnModel().getColumnCount() > 0) {
                seqTable.getTableHeader().getColumnModel().getColumn(0).setHeaderValue(model.getColumnName(0));
                seqTable.getTableHeader().repaint();
            }
        });
    }

    // Scroll the table such that the selected row is visible
    void scrollTableLater(final JTable table, final int i) {
        SwingUtilities.invokeLater(() -> {
            // Check the row count first since this is multi-threaded
            if (table.getRowCount() >= i) {
                DisplayUtils.scrollToVisible(table, i, 0);
            }
        });
    }

    private final class SeqLengthComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            if (o1 == null || o2 == null) {
                return SeqSymIdComparator.compareNullIDs(o2, o1);	// null is last
            }
            if (o1.length() == 0 || o2.length() == 0) {
                return o2.compareTo(o1);	// empty string is last
            }

            // use valueOf to get a Long object versus a long primitive.
            return Long.valueOf(o1).compareTo(Long.parseLong(o2));
        }
    }

    static final class ColumnRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        public ColumnRenderer() {
            setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            if (value == null) { //fixes NPE when tabbing through table
                return null;
            }

            if (value.toString().length() == 0) {
                return super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            }

            return super.getTableCellRendererComponent(table, nformat.format(Double.valueOf(value.toString())),
                    isSelected, hasFocus, row, column);
        }
    }

    @Override
    public void itemStateChanged(ItemEvent evt) {
        Object src = evt.getSource();
        if (DEBUG_EVENTS) {
            System.out.println("####### GeneralLoadView received itemStateChanged event: " + evt);
        }
        try {
            if ((src == speciesCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
                speciesCBChanged(); // make sure display gets updated
                toogleView(false);
            } else if ((src == versionCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
                versionCBChanged();
            }
        } catch (Throwable t) {
            // some out-of-memory errors could happen during this code, so
            // this catch block will report that to the user.
            ErrorHandler.errorPanel("Error ", t, Level.SEVERE);
        }
    }

    /**
     * The species combo box changed. If the species changes to SELECT, the SelectedSeqGroup is set to null. If the
     * species changes to a specific organism and there's only one choice for the genome versionName, the
     * SelectedSeqGroup is set to that versionName. Otherwise, the SelectedSetGroup is set to null.
     */
    private void speciesCBChanged() {
        String speciesName = (String) speciesCB.getSelectedItem();
        if (speciesName.equals(SELECT_SPECIES)) {
            return;
        }
        // Populate the versionName CB
        refreshVersionCB(speciesName);

        // Select the null group (and the null seq), if it's not already selected.
        if (curGroup != null) {
            gmodel.setSelectedGenomeVersion(null); // This method is being called on purpose to fire group selection event.
            gmodel.setSelectedSeq(null);	  // which in turns calls refreshTreeView method.
        }
    }

    /**
     * This method is used to toggle view in the gViewer when the user has only selected the species and yet to choose
     * the version. The method considers that the gviewer has the seqmap as the last component added.
     *
     * @param isVersionSelected
     */
    private void toogleView(boolean isVersionSelected) {
        int index = gviewer.getComponentCount() - 1;
        gviewer.remove(index);
        gviewer.add(isVersionSelected ? gviewer.getSeqMap() : selectVersionPanel);
        gviewer.repaint();

    }

    /**
     * The versionName combo box changed. This changes the selected group (either to null, or to a valid group). It is
     * assumed that at this point, the species is valid.
     */
    private void versionCBChanged() {
        String versionName = (String) versionCB.getSelectedItem();

        if (curGroup != null) {
            gmodel.setSelectedGenomeVersion(null);
            gmodel.setSelectedSeq(null);
        }

        if (versionName.equals(SELECT_GENOME)) {
            // Select the null group (and the null seq), if it's not already selected.
            toogleView(false);
            return;
        }
        setSelectedGenomeVersion(versionName);
    }

    public void setSelectedGenomeVersion(String versionName) {
        GenomeVersion genomeVersion = gmodel.getSeqGroup(versionName);
        setSelectedGenomeVersion(genomeVersion);
    }

    public void setSelectedGenomeVersion(GenomeVersion genomeVersion) {
        toogleView(true);
        speciesCB.setEnabled(false);
        versionCB.setEnabled(false);
        InitVersionWorker worker = new InitVersionWorker(genomeVersion);
        CThreadHolder.getInstance().execute(genomeVersion.getName(), worker);
    }

    @Override
    public void valueChanged(ListSelectionEvent evt) {
        Object src = evt.getSource();
        if ((src == lsm) && (!evt.getValueIsAdjusting())) { // ignore extra messages
            int srow = seqTable.getSelectedRow();
            if (srow >= 0) {
                String seq_name = (String) seqTable.getValueAt(srow, 0);
                selected_seq = gmodel.getSelectedGenomeVersion().getSeq(seq_name);
                if (selected_seq != gmodel.getSelectedSeq().orElse(null)) {
                    gmodel.setSelectedSeq(selected_seq);
                }
            }
        }
    }

    @Override
    public void groupSelectionChanged(GenomeVersionSelectionEvent evt) {
        toogleView(true);
        GenomeVersion genomeVersion = gmodel.getSelectedGenomeVersion();
        previousGenomeVersion = genomeVersion;
        SeqGroupTableModel model = (SeqGroupTableModel) seqTable.getModel();
        model.setGenomeVersion(genomeVersion);

        refreshTable();
        versionNameChanged(evt);
    }

    /**
     * This gets called when the genome versionName is changed. This occurs via the combo boxes, or by an external event
     * like bookmarks, or LoadFileAction
     *
     * @param evt
     */
    public void versionNameChanged(GenomeVersionSelectionEvent evt) {
        GenomeVersion genomeVersion = evt.getSelectedGroup();

        if (genomeVersion == null) {
            if (versionCB.getSelectedItem() != SELECT_GENOME) {
                versionCB.removeItemListener(this);
                versionCB.setEnabled(false);
                versionCB.addItemListener(this);
            }
            curGroup = null;
            return;
        }
//        if (curGroup == genomeVersion) {
//            return;
//        }
        curGroup = genomeVersion;

        Set<DataContainer> dataContainers = genomeVersion.getAvailableDataContainers();
        if (dataContainers.isEmpty()) {
//            createUnknownVersion(genomeVersion);
            return;
        }
        final String versionName = GeneralUtils.getPreferredVersionName(dataContainers);
        if (versionName == null) {
            System.out.println("ERROR -- couldn't find version");
            return;
        }
        final String speciesName = GeneralLoadUtils.getVersionName2Species().get(versionName);
        if (speciesName == null) {
            // Couldn't find species matching this versionName -- we have problems.
            System.out.println("ERROR - Couldn't find species for version " + versionName);
            return;
        }

        ThreadUtils.runOnEventQueue(() -> {
            if (!speciesName.equals(speciesCB.getSelectedItem())) {
                // Set the selected species (the combo box is already populated)
                speciesCB.removeItemListener(SeqGroupView.this);
                speciesCB.setSelectedItem(speciesName);
                speciesCB.addItemListener(SeqGroupView.this);
            }
            if (!versionName.equals(versionCB.getSelectedItem())) {
                refreshVersionCB(speciesName);			// Populate the versionName CB
                versionCB.removeItemListener(SeqGroupView.this);
                versionCB.setSelectedItem(versionName);
                versionCB.addItemListener(SeqGroupView.this);
            }
        });

        GeneralLoadView.getLoadView().refreshTreeView();	// Replacing clearFeaturesTable with refreshTreeView.
        // refreshTreeView should only be called if feature table
        // needs to be cleared.
        GeneralLoadView.getLoadView().disableAllButtons();
        GeneralLoadView.loadGenomeLoadModeDataSets();
    }

    @Override
    public void seqSelectionChanged(SeqSelectionEvent evt) {
        if (SeqGroupView.DEBUG_EVENTS) {
            System.out.println("SeqGroupView received seqSelectionChanged() event: seq is " + evt.getSelectedSeq());
        }
        synchronized (seqTable) {  // or should synchronize on lsm?
            lsm.removeListSelectionListener(this);
            selected_seq = evt.getSelectedSeq();
            if (selected_seq == null) {
                seqTable.clearSelection();
            } else {

                int rowCount = seqTable.getRowCount();
                for (int i = 0; i < rowCount; i++) {
                    // should be able to use == here instead of equals(), because table's model really returns seq.getName()
                    if (selected_seq.getId() == seqTable.getValueAt(i, 0)) {
                        if (seqTable.getSelectedRow() != i) {
                            seqTable.setRowSelectionInterval(i, i);
                            scrollTableLater(seqTable, i);
                        }
                        break;
                    }
                }
            }
            lsm.addListSelectionListener(this);
        }

        chromosomeChanged(evt);
    }

    /**
     * Changed the selected chromosome.
     *
     * @param evt
     */
    public void chromosomeChanged(SeqSelectionEvent evt) {
        BioSeq aseq = evt.getSelectedSeq();

        if (aseq == null) {
            GeneralLoadView.getLoadView().refreshTreeView();	// Replacing clearFeaturesTable with refreshTreeView.
//			GeneralLoadView.getLoadView().refreshDataManagementView();
            // refreshTreeView should only be called if feature table
            // needs to be cleared.
            GeneralLoadView.getLoadView().disableAllButtons();
            return;
        }

        // validate that this sequence is in our group.
        GenomeVersion genomeVersion = aseq.getGenomeVersion();
        if (genomeVersion == null) {
            if (DEBUG_EVENTS) {
                System.out.println("sequence was null");
            }
            return;
        }
        Set<DataContainer> gVersions = genomeVersion.getAvailableDataContainers();
        if (gVersions.isEmpty()) {
//            createUnknownVersion(genomeVersion);
            return;
        }

        String speciesName = (String) this.speciesCB.getSelectedItem();
        String versionName = (String) this.versionCB.getSelectedItem();
        if (speciesName == null || versionName == null || speciesName.equals(SELECT_SPECIES) || versionName.equals(SELECT_GENOME)) {
            return;
        }

        if (!(GeneralUtils.getPreferredVersionName(gVersions).equals(versionName))) {
            return;
        }

        GeneralLoadView.getLoadView().refreshDataManagementView();
        //TODO Look loading data here...
        // GeneralLoadView.loadWholeRangeFeatures(DasServerType.getInstance());
    }

    @Subscribe
    public void dataProviderInit(DataProviderServiceChangeEvent event) {
        ((AbstractTableModel) seqTable.getModel()).fireTableDataChanged();
        GeneralLoadView.getLoadView().refreshTreeView();
        String speciesName = (String) this.speciesCB.getSelectedItem();
        synchronized (this) {
            refreshSpeciesCB();
        }
        if (speciesName != null && !speciesName.equals(SELECT_SPECIES)) {
            lookForPersistentGenome = false;
            String versionName = (String) this.versionCB.getSelectedItem();
            //refresh version names if a species is selected
            //refreshVersionCB(speciesName); //possibly not a permanent fix for bug which causes the genome version combo to be reset
            if (versionName != null && !versionName.equals(SELECT_GENOME)) {
                // refresh this version
                initVersion(versionName);

                // TODO: refresh feature tree view if a version is selected
                GeneralLoadView.getLoadView().refreshTreeView();
                if (AutoLoadFeatureAction.getActionCB().isSelected()) {
//                        GeneralLoadView.loadWholeRangeFeatures(null);
                }
                ((AbstractTableModel) seqTable.getModel()).fireTableDataChanged();
            }
        }
    }

//    /**
//     * group has been created independently of the discovery process (probably
//     * by loading a file). create new "unknown" species/versionName.
//     */
//    private void createUnknownVersion(GenomeVersion genomeVersion) {
//        gmodel.removeGroupSelectionListener(this);
//        gmodel.removeSeqSelectionListener(this);
//
//        speciesCB.removeItemListener(this);
//        versionCB.removeItemListener(this);
//        DataContainer dataContainer = GeneralLoadUtils.getUnknownDataContainer(genomeVersion);
//        String species = GeneralLoadUtils.getVersionName2Species().get(dataContainer.getGenomeVersion().getName());
//        refreshSpeciesCB();
//
//        if (!species.equals(speciesCB.getSelectedItem())) {
//            gmodel.removeGroupSelectionListener(this);
//            gmodel.removeSeqSelectionListener(this);
//
//            speciesCB.removeItemListener(this);
//            versionCB.removeItemListener(this);
//
//            // Set the selected species (the combo box is already populated)
//            speciesCB.setSelectedItem(species);
//            // populate the versionName combo box.
//            refreshVersionCB(species);
//        }
//
//        initVersion(dataContainer.getGenomeVersion().getName());
//
//        versionCB.setSelectedItem(dataContainer.getGenomeVersion().getName());
//        versionCB.setEnabled(true);
//        gviewer.getPartial_residuesButton().setEnabled(false);
//        gviewer.getRefreshDataAction().setEnabled(false);
//        addListeners();
//    }
    public void refreshSpeciesCB() {
        int speciesListLength = GeneralLoadUtils.getLoadedSpeciesNames().size();
        if (speciesListLength == speciesCB.getItemCount() - 1) {
            String speciesName = (String) speciesCB.getSelectedItem();
            // Check if new version has been added
            if (!speciesName.equals(SELECT_SPECIES)) {
                int versionListLength = getAllVersions(speciesName).size();
                if (versionListLength != versionCB.getItemCount() - 1) {
                    refreshVersionCB(speciesName);
                }
            }
            // No new species.  Don't bother refreshing.
            return;
        }

        ThreadUtils.runOnEventQueue(() -> {
            final List<String> speciesList = GeneralLoadUtils.getSpeciesList();

            speciesCB.removeItemListener(SeqGroupView.this);
            String oldSpecies = (String) speciesCB.getSelectedItem();

            speciesCB.removeAllItems();
            speciesCB.addItem(SELECT_SPECIES);
            for (String speciesName : speciesList) {
                speciesCBRenderer.setToolTipEntry(speciesName, SpeciesLookup.getCommonSpeciesName(speciesName));
                speciesCB.addItem(speciesName);
            }
            if (oldSpecies == null) {
                return;
            }

            if (speciesList.contains(oldSpecies)) {
                speciesCB.setSelectedItem(oldSpecies);
            } else {
                // species CB changed
                speciesCBChanged();
            }
            speciesCB.addItemListener(SeqGroupView.this);
        });
    }

    public List<String> getAllVersions(final String speciesName) {
        final Set<DataContainer> versionList = GeneralLoadUtils.getDataContainersForSpecies(speciesName);
        final List<String> versionNames = new ArrayList<>();
        if (versionList != null) {
            for (DataContainer dataContainer : versionList) {
                // the same versionName name may occur on multiple servers
                String versionName = dataContainer.getGenomeVersion().getName();
                if (!versionNames.contains(versionName)) {
                    versionNames.add(versionName);
                }
            }
            Collections.sort(versionNames, new StringVersionDateComparator());
        }
        return versionNames;
    }

    /**
     * Refresh the genome versions.
     *
     * @param speciesName
     */
    private void refreshVersionCB(final String speciesName) {
        final List<String> versionNames = getAllVersions(speciesName);
        for (String versionName : versionNames) {
            versionCBRenderer.setToolTipEntry(versionName, GeneralLoadUtils.listSynonyms(versionName));
        }

        // Sort the versions (by date)
        ThreadUtils.runOnEventQueue(() -> {
            versionCB.removeItemListener(SeqGroupView.this);
            String oldVersion = (String) versionCB.getSelectedItem();

            if (versionNames.isEmpty() || speciesName.equals(SELECT_SPECIES)) {
                versionCB.setSelectedIndex(0);
                versionCB.setEnabled(false);
                return;
            }

            // Add names to combo boxes.
            versionCB.removeAllItems();
            versionCB.addItem(SELECT_GENOME);
            versionNames.forEach(versionCB::addItem);
            versionCB.setEnabled(true);
            if (oldVersion != null && !oldVersion.equals(SELECT_GENOME) && GeneralLoadUtils.getVersionName2Species().containsKey(oldVersion)) {
                versionCB.setSelectedItem(oldVersion);
            } else {
                versionCB.setSelectedIndex(0);
            }
            if (versionCB.getItemCount() > 1) {
                versionCB.addItemListener(SeqGroupView.this);
            }
        });
    }

    public void initVersion(String versionName) {
        igbService.addNotLockedUpMsg(MessageFormat.format(BUNDLE.getString("loadingChr"), versionName));
        try {
            GeneralLoadUtils.initVersionAndSeq(versionName); // Make sure this genome versionName's feature names are initialized.
        } finally {
            igbService.removeNotLockedUpMsg(MessageFormat.format(BUNDLE.getString("loadingChr"), versionName));
        }
    }

    /**
     * Run initialization of version on thread, so we don't lock up the GUI. Merge with initVersion();
     */
    private class InitVersionWorker extends CThreadWorker<Void, Void> {

        private final GenomeVersion genomeVersion;

        InitVersionWorker(GenomeVersion genomeVersion) {
            super("Loading data for: " + genomeVersion.getName());
            this.genomeVersion = genomeVersion;
        }

        @Override
        public Void runInBackground() {
            igbService.addNotLockedUpMsg(MessageFormat.format(BUNDLE.getString("loadingChr"), genomeVersion.getName()));
            GeneralLoadUtils.initVersionAndSeq(genomeVersion.getName()); // Make sure this genome versionName's feature names are initialized.
            return null;
        }

        @Override
        protected void finished() {
            igbService.removeNotLockedUpMsg(MessageFormat.format(BUNDLE.getString("loadingChr"), genomeVersion.getName()));
            speciesCB.setEnabled(true);
            versionCB.setEnabled(true);
            if ((curGroup != null || genomeVersion != null) && curGroup != genomeVersion) {
                // avoid calling these a half-dozen times
                gmodel.setSelectedGenomeVersion(genomeVersion);
                gmodel.setSelectedSeq(genomeVersion.getSeq(0));
            }
        }
    }

    private void addListeners() {
        gmodel.addGroupSelectionListener(this);
        gmodel.addSeqSelectionListener(this);
        speciesCB.setEnabled(true);
        versionCB.setEnabled(true);
        speciesCB.addItemListener(this);
        versionCB.addItemListener(this);
        speciesCB.addItemListener(MainWorkspaceManager.getWorkspaceManager());
    }

    public JRPStyledTable getTable() {
        return seqTable;
    }

    public JComboBox getSpeciesCB() {
        return speciesCB;
    }

    public JComboBox getVersionCB() {
        return versionCB;
    }

    private static class BioSeqAlphanumComparator extends AlphanumComparator {

        @Override
        public int compare(Object o1, Object o2) {
            if (o1.toString().equals("genome")) {
                return 1;
            } else if (o2.toString().equals("genome")) {
                return -1;
            }
            return super.compare(o1, o2);
        }
    }
}
