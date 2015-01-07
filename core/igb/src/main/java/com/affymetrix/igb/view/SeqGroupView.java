package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.comparator.AlphanumComparator;
import com.affymetrix.genometryImpl.comparator.SeqSymIdComparator;
import com.affymetrix.genometryImpl.comparator.StringVersionDateComparator;
import com.affymetrix.genometryImpl.event.GenericServerInitEvent;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.DisplayUtils;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.SpeciesLookup;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.action.AutoLoadFeatureAction;
import com.affymetrix.igb.general.Persistence;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.JRPStyledTable;
import com.affymetrix.igb.swing.JRPComboBox;
import com.affymetrix.igb.swing.JRPComboBoxWithSingleListener;
import com.affymetrix.igb.swing.ScriptManager;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.util.JComboBoxToolTipRenderer;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.igb.view.welcome.MainWorkspaceManager;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author lorainelab
 */
public class SeqGroupView implements ItemListener, ListSelectionListener,
		GroupSelectionListener, SeqSelectionListener, GenericServerInitListener {

	private static final NumberFormat nformat = NumberFormat.getIntegerInstance(Locale.ENGLISH);
	private static final boolean DEBUG_EVENTS = false;
	public static final String SELECT_SPECIES = IGBConstants.BUNDLE.getString("speciesCap");
	private static final String SELECT_GENOME = IGBConstants.BUNDLE.getString("genomeVersionCap");
	private static final GenometryModel gmodel = GenometryModel.getInstance();
	protected String[] columnToolTips = {null, BUNDLE.getString("sequenceHeaderLengthToolTip")};
	private final JRPStyledTable seqtable;
	private final ListSelectionModel lsm;
	private BioSeq selected_seq = null;
	private AnnotatedSeqGroup previousGroup = null;
	private int previousSeqCount = 0;
	private TableRowSorter<SeqGroupTableModel> sorter;
	private String most_recent_seq_id = null;
	private static SeqGroupView singleton;
	private JComboBoxToolTipRenderer versionCBRenderer;
	private JComboBoxToolTipRenderer speciesCBRenderer;
	private AnnotatedSeqGroup curGroup = null;
	private volatile boolean lookForPersistentGenome = true;
	private static SeqMapView gviewer;
	private JRPComboBox speciesCB;
	private JRPComboBox versionCB;
	private final IGBService igbService;
        private SelectVersionPanel selectVersionPanel;
	SeqGroupView(IGBService _igbService) {
		igbService = _igbService;
		gviewer = Application.getSingleton().getMapView();
                selectVersionPanel = new SelectVersionPanel();
		seqtable = new JRPStyledTable("SeqGroupView_seqtable") {

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
		seqtable.setToolTipText(BUNDLE.getString("chooseSeq"));
		seqtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		seqtable.setCellSelectionEnabled(false);
		seqtable.setRowSelectionAllowed(true);
		
		SeqGroupTableModel model = new SeqGroupTableModel(null);
		seqtable.setModel(model);	// Force immediate visibility of column headers (although there's no data).

		lsm = seqtable.getSelectionModel();
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

	static void init(IGBService _igbService) {
		singleton = new SeqGroupView(_igbService);
		singleton.addListeners();
		singleton.populateSpeciesData();
	}

	public static SeqGroupView getInstance() {
		return singleton;
	}

	/**
	 * Refresh seqtable if more chromosomes are added, for example.
	 */
	public void refreshTable() {
		final AbstractTableModel model = ((AbstractTableModel) seqtable.getModel());
		model.fireTableDataChanged();
		ThreadUtils.runOnEventQueue(new Runnable(){
			@Override
			public void run() {
				if(seqtable.getTableHeader().getColumnModel().getColumnCount() > 0){
					seqtable.getTableHeader().getColumnModel().getColumn(0).setHeaderValue(model.getColumnName(0));
					seqtable.getTableHeader().repaint();
				}
			}
		});
	}

	private void warnAboutNewlyAddedChromosomes(int previousSeqCount, AnnotatedSeqGroup group) {
		if (previousSeqCount > group.getSeqCount()) {
			System.out.println("WARNING: chromosomes have been added");
			if (previousSeqCount < group.getSeqCount()) {
				System.out.print("New chromosomes:");
				for (int i = previousSeqCount; i < group.getSeqCount(); i++) {
					System.out.print(" " + group.getSeq(i).getID());
				}
				System.out.println();
			}
		}
	}

	// Scroll the table such that the selected row is visible
	void scrollTableLater(final JTable table, final int i) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				// Check the row count first since this is multi-threaded
				if (table.getRowCount() >= i) {
					DisplayUtils.scrollToVisible(table, i, 0);
				}
			}
		});
	}

	private final class SeqLengthComparator implements Comparator<String> {

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
	 * The species combo box changed. If the species changes to SELECT, the
	 * SelectedSeqGroup is set to null. If the species changes to a specific
	 * organism and there's only one choice for the genome versionName, the
	 * SelectedSeqGroup is set to that versionName. Otherwise, the
	 * SelectedSetGroup is set to null.
	 */
	private void speciesCBChanged() {
		String speciesName = (String) speciesCB.getSelectedItem();

		// Populate the versionName CB
		refreshVersionCB(speciesName);
                
		// Select the null group (and the null seq), if it's not already selected.
		if (curGroup != null) {
			gmodel.setSelectedSeqGroup(null); // This method is being called on purpose to fire group selection event.
			gmodel.setSelectedSeq(null);	  // which in turns calls refreshTreeView method.
		}
	}
        
        /**
         * This method is used to toggle view in the gViewer when the user has only selected the 
         * species and yet to choose the version.
         * The method considers that the gviewer has the seqmap as the last component added.
         * @param isVersionSelected 
         */
        private void toogleView(boolean isVersionSelected) {
            int index = gviewer.getComponentCount()-1;
            gviewer.remove(index);
            gviewer.add(isVersionSelected ? gviewer.getSeqMap() : selectVersionPanel);
            gviewer.repaint();

        }
	/**
	 * The versionName combo box changed. This changes the selected group
	 * (either to null, or to a valid group). It is assumed that at this point,
	 * the species is valid.
	 */
	private void versionCBChanged() {
		String versionName = (String) versionCB.getSelectedItem();
		if (DEBUG_EVENTS) {
			System.out.println("Selected version: " + versionName);
		}

		if (curGroup != null) {
			gmodel.setSelectedSeqGroup(null);
			gmodel.setSelectedSeq(null);
		}

		if (versionName.equals(SELECT_GENOME)) {
			// Select the null group (and the null seq), if it's not already selected.
                        toogleView(false);
			return;
		}

		setSelectedGroup(versionName);
	}

	public void setSelectedGroup(String versionName) {
                toogleView(true);
		AnnotatedSeqGroup group = gmodel.getSeqGroup(versionName);
		if (group == null) {
			System.out.println("Group was null -- trying species instead");
			group = gmodel.getSeqGroup(GeneralLoadUtils.getVersionName2Species().get(versionName));
			if (group == null) {
				return;
			}
		}

		speciesCB.setEnabled(false);
		versionCB.setEnabled(false);

		InitVersionWorker worker = new InitVersionWorker(versionName, group);
		CThreadHolder.getInstance().execute(versionName, worker);
	}

	public void valueChanged(ListSelectionEvent evt) {
		Object src = evt.getSource();
		if ((src == lsm) && (!evt.getValueIsAdjusting())) { // ignore extra messages
			if (SeqGroupView.DEBUG_EVENTS) {
				System.out.println("SeqGroupView received valueChanged() ListSelectionEvent");
			}
			int srow = seqtable.getSelectedRow();
			if (srow >= 0) {
				String seq_name = (String) seqtable.getValueAt(srow, 0);
				selected_seq = gmodel.getSelectedSeqGroup().getSeq(seq_name);
				if (selected_seq != gmodel.getSelectedSeq()) {
					gmodel.setSelectedSeq(selected_seq);
				}
			}
		}
	}

	public void groupSelectionChanged(GroupSelectionEvent evt) {
                toogleView(true);
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		if (SeqGroupView.DEBUG_EVENTS) {
			System.out.println("SeqGroupView received groupSelectionChanged() event");
			if (group == null) {
				System.out.println("  group is null");
			} else {
				System.out.println("  group: " + group.getID());
				System.out.println("  seq count: " + group.getSeqCount());
			}
		}
		if (previousGroup == group) {
			if (group == null) {
				return;
			}
			warnAboutNewlyAddedChromosomes(previousSeqCount, group);
		}

		previousGroup = group;
		previousSeqCount = group == null ? 0 : group.getSeqCount();


		SeqGroupTableModel mod = new SeqGroupTableModel(group);

		sorter = new TableRowSorter<SeqGroupTableModel>(mod) {

			@Override
			public Comparator<?> getComparator(int column) {
				if (column == 0) {
					return new BioSeqAlphanumComparator();
				}
				return new SeqLengthComparator();
			}
		};

		selected_seq = null;
		seqtable.setModel(mod);
		//Disabled for now
		//seqtable.setRowSorter(sorter);
		sorter.setSortKeys(Arrays.asList(new SortKey(0, SortOrder.ASCENDING)));
		
		TableColumn c = seqtable.getColumnModel().getColumn(1);
		c.setCellRenderer(new ColumnRenderer());

		refreshTable();

		if (group != null && most_recent_seq_id != null) {
			// When changing genomes, try to keep the same chromosome selected when possible
			BioSeq aseq = group.getSeq(most_recent_seq_id);
			if (aseq != null) {
				gmodel.setSelectedSeq(aseq);
			}
		}

		versionNameChanged(evt);
	}

	/**
	 * This gets called when the genome versionName is changed. This occurs via
	 * the combo boxes, or by an external event like bookmarks, or
	 * LoadFileAction
	 *
	 * @param evt
	 */
	public void versionNameChanged(GroupSelectionEvent evt) {
		AnnotatedSeqGroup group = evt.getSelectedGroup();

		if (DEBUG_EVENTS) {
			System.out.println("GeneralLoadView.groupSelectionChanged() called, group: " + (group == null ? null : group.getID()));
		}
		if (group == null) {
			if (versionCB.getSelectedItem() != SELECT_GENOME) {
				versionCB.removeItemListener(this);
				versionCB.setEnabled(false);
				versionCB.addItemListener(this);
			}
			curGroup = null;
			return;
		}
		if (curGroup == group) {
			if (DEBUG_EVENTS) {
				System.out.println("GeneralLoadView.groupSelectionChanged(): group was same as previous.");
			}
			return;
		}
		curGroup = group;

		Set<GenericVersion> gVersions = group.getEnabledVersions();
		if (gVersions.isEmpty()) {
			createUnknownVersion(group);
			return;
		}
		final String versionName = GeneralUtils.getPreferredVersionName(gVersions);
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

		if (!speciesName.equals(speciesCB.getSelectedItem())) {
			// Set the selected species (the combo box is already populated)
			ThreadUtils.runOnEventQueue(new Runnable() {

				public void run() {
					speciesCB.removeItemListener(SeqGroupView.this);
					speciesCB.setSelectedItem(speciesName);
					speciesCB.addItemListener(SeqGroupView.this);
				}
			});
		}
		if (!versionName.equals(versionCB.getSelectedItem())) {
			refreshVersionCB(speciesName);			// Populate the versionName CB
			ThreadUtils.runOnEventQueue(new Runnable() {

				public void run() {
					versionCB.removeItemListener(SeqGroupView.this);
					versionCB.setSelectedItem(versionName);
					versionCB.addItemListener(SeqGroupView.this);
				}
			});
		}

		GeneralLoadView.getLoadView().refreshTreeView();	// Replacing clearFeaturesTable with refreshTreeView.
		// refreshTreeView should only be called if feature table
		// needs to be cleared.
		GeneralLoadView.getLoadView().disableAllButtons();
		GeneralLoadView.AutoloadQuickloadFeature();
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		if (SeqGroupView.DEBUG_EVENTS) {
			System.out.println("SeqGroupView received seqSelectionChanged() event: seq is " + evt.getSelectedSeq());
		}
		synchronized (seqtable) {  // or should synchronize on lsm?
			lsm.removeListSelectionListener(this);
			selected_seq = evt.getSelectedSeq();
			if (selected_seq == null) {
				seqtable.clearSelection();
			} else {
				most_recent_seq_id = selected_seq.getID();

				int rowCount = seqtable.getRowCount();
				for (int i = 0; i < rowCount; i++) {
					// should be able to use == here instead of equals(), because table's model really returns seq.getID()
					if (most_recent_seq_id == seqtable.getValueAt(i, 0)) {
						if (seqtable.getSelectedRow() != i) {
							seqtable.setRowSelectionInterval(i, i);
							scrollTableLater(seqtable, i);
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

		if (DEBUG_EVENTS) {
			System.out.println("GeneralLoadView.seqSelectionChanged() called, aseq: " + (aseq == null ? null : aseq.getID()));
		}

		if (aseq == null) {
			GeneralLoadView.getLoadView().refreshTreeView();	// Replacing clearFeaturesTable with refreshTreeView.
//			GeneralLoadView.getLoadView().refreshDataManagementView();
			// refreshTreeView should only be called if feature table
			// needs to be cleared.
			GeneralLoadView.getLoadView().disableAllButtons();
			return;
		}

		// validate that this sequence is in our group.
		AnnotatedSeqGroup group = aseq.getSeqGroup();
		if (group == null) {
			if (DEBUG_EVENTS) {
				System.out.println("sequence was null");
			}
			return;
		}
		Set<GenericVersion> gVersions = group.getEnabledVersions();
		if (gVersions.isEmpty()) {
			createUnknownVersion(group);
			return;
		}

		String speciesName = (String) this.speciesCB.getSelectedItem();
		String versionName = (String) this.versionCB.getSelectedItem();
		if (speciesName == null || versionName == null || speciesName.equals(SELECT_SPECIES) || versionName.equals(SELECT_GENOME)) {
			return;
		}

		if (!(GeneralUtils.getPreferredVersionName(gVersions).equals(versionName))) {
			/*
			 * System.out.println("ERROR - versions don't match: " + versionName
			 * + "," + GeneralLoadUtils.getPreferredVersionName(gVersions));
			 */
			return;
		}

		GeneralLoadView.getLoadView().refreshDataManagementView();
		GeneralLoadView.loadWholeRangeFeatures(ServerTypeI.DAS2);
		GeneralLoadView.loadWholeRangeFeatures(ServerTypeI.DAS);
	}

	public void genericServerInit(GenericServerInitEvent evt) {
		GenericServer gServer = (GenericServer) evt.getSource();
		
		if (gServer.getServerStatus() == ServerStatus.NotResponding) {
			((AbstractTableModel)seqtable.getModel()).fireTableDataChanged();
			GeneralLoadView.getLoadView().refreshTreeView();
			refreshSpeciesCB();
			return;
		}

		if (gServer.getServerStatus() != ServerStatus.Initialized) {
			return;	// ignore uninitialized servers
		}

		if (gServer.serverType != ServerTypeI.LocalFiles) {
			if (gServer.serverType != null) {
				igbService.removeNotLockedUpMsg("Loading server " + gServer + " (" + gServer.serverType.getName() + ")");
			}
		}

		String speciesName = (String) this.speciesCB.getSelectedItem();
		synchronized(this){
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
					GeneralLoadView.loadWholeRangeFeatures(null);
				}
				((AbstractTableModel)seqtable.getModel()).fireTableDataChanged();
			}
		}

	}

	private void populateSpeciesData() {
//		final Set<GenericServer> servers = new HashSet<GenericServer>();
//		servers.addAll(ServerList.getServerInstance().getEnabledServers());
		for (final GenericServer gServer : ServerList.getServerInstance().getEnabledServers()) {
			CThreadWorker<Void, Void> worker = new CThreadWorker<Void, Void>("loading server " + gServer.serverName) {

				protected Void runInBackground() {
					GeneralLoadUtils.discoverServer(gServer);
					return null;
				}

				@Override
				public void finished() {
//					synchronized (servers) {
//						servers.remove(gServer);
//
//						if (servers.isEmpty()) {
//							runBatchOrRestore();
//						}
//					}
				}
			};

			CThreadHolder.getInstance().execute(gServer, worker);
		}
	}

	/**
	 * group has been created independently of the discovery process (probably
	 * by loading a file). create new "unknown" species/versionName.
	 */
	private void createUnknownVersion(AnnotatedSeqGroup group) {
		gmodel.removeGroupSelectionListener(this);
		gmodel.removeSeqSelectionListener(this);

		speciesCB.removeItemListener(this);
		versionCB.removeItemListener(this);
		GenericVersion gVersion = GeneralLoadUtils.getUnknownVersion(group);
		String species = GeneralLoadUtils.getVersionName2Species().get(gVersion.versionName);
		refreshSpeciesCB();
		if (DEBUG_EVENTS) {
			System.out.println("Species is " + species + ", version is " + gVersion.versionName);
		}

		if (!species.equals(speciesCB.getSelectedItem())) {
			gmodel.removeGroupSelectionListener(this);
			gmodel.removeSeqSelectionListener(this);

			speciesCB.removeItemListener(this);
			versionCB.removeItemListener(this);

			// Set the selected species (the combo box is already populated)
			speciesCB.setSelectedItem(species);
			// populate the versionName combo box.
			refreshVersionCB(species);
		}

		initVersion(gVersion.versionName);

		versionCB.setSelectedItem(gVersion.versionName);
		versionCB.setEnabled(true);
		gviewer.getPartial_residuesButton().setEnabled(false);
		gviewer.getRefreshDataAction().setEnabled(false);
		addListeners();
	}

	private void refreshSpeciesCB() {
		int speciesListLength = GeneralLoadUtils.getSpecies2Generic().keySet().size();
		if (speciesListLength == speciesCB.getItemCount() - 1) {
			String speciesName = (String) speciesCB.getSelectedItem();
			// Check if new version has been added
			if(!speciesName.equals(SELECT_SPECIES)){
				int versionListLength = getAllVersions(speciesName).size();
				if(versionListLength != versionCB.getItemCount() - 1){
					refreshVersionCB(speciesName);
				}
			}
			// No new species.  Don't bother refreshing.
			return;
		}

		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
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
			}
		});
	}

	public List<String> getAllVersions(final String speciesName) {
		final Set<GenericVersion> versionList = GeneralLoadUtils.getSpecies2Generic().get(speciesName);
		final List<String> versionNames = new ArrayList<String>();
		if (versionList != null) {
			for (GenericVersion gVersion : versionList) {
				// the same versionName name may occur on multiple servers
				String versionName = gVersion.versionName;
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

		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
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
				for (String versionName : versionNames) {
					versionCB.addItem(versionName);
				}
				versionCB.setEnabled(true);
				if (oldVersion != null && !oldVersion.equals(SELECT_GENOME) && GeneralLoadUtils.getVersionName2Species().containsKey(oldVersion)) {
					versionCB.setSelectedItem(oldVersion);
				} else {
					versionCB.setSelectedIndex(0);
				}
				if (versionCB.getItemCount() > 1) {
					versionCB.addItemListener(SeqGroupView.this);
				}
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

	private void runBatchOrRestore() {
		try {
			// Only run batch script or restore persistent genome once all the server responses have come back.
			String batchFile = IGB.commandLineBatchFileStr;
			if (batchFile != null) {
				IGB.commandLineBatchFileStr = null;	// we're not using this again!
				lookForPersistentGenome = false;
				Thread.sleep(1000);	// hack so event queue finishes
				ScriptManager.getInstance().runScript(batchFile);
			} else {
				if (lookForPersistentGenome) {
					lookForPersistentGenome = false;
					//Thread.sleep(1000);	// hack so event queue finishes
					RestorePersistentGenome();
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(GeneralLoadView.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * bootstrap bookmark from Preferences for last species/versionName/genome /
	 * sequence / region
	 */
	private void RestorePersistentGenome() {
		// Get group and seq info from persistent preferences.
		// (Recovering as much data as possible before activating listeners.)
		final AnnotatedSeqGroup group = Persistence.restoreGroupSelection();
		if (group == null) {
			return;
		}

		Set<GenericVersion> gVersions = group.getEnabledVersions();
		if (gVersions == null || gVersions.isEmpty()) {
			return;
		}
		final String versionName = GeneralUtils.getPreferredVersionName(gVersions);
		if (versionName == null || GeneralLoadUtils.getVersionName2Species().get(versionName) == null || gmodel.getSeqGroup(versionName) != group) {
			return;
		}

		if (gmodel.getSelectedSeqGroup() != null || gmodel.getSelectedSeq() != null) {
			return;
		}

		final CThreadWorker<BioSeq, Void> worker = new CThreadWorker<BioSeq, Void>("Loading previous genome " + versionName + " ...") {

			@Override
			protected BioSeq runInBackground() {

				initVersion(versionName);

				if (Thread.currentThread().isInterrupted()) {
					return null;
				}

				gmodel.setSelectedSeqGroup(group);

				List<GenericFeature> features = GeneralLoadUtils.getSelectedVersionFeatures();
				if (features == null || features.isEmpty()) {
					return null;
				}

				BioSeq seq = Persistence.restoreSeqSelection(group);
				if (seq == null) {
					seq = group.getSeq(0);
					if (seq == null) {
						return null;
					}
				}


				// Try/catch may not be needed.
				try {
					Persistence.restoreSeqVisibleSpan(gviewer);
				} catch (Exception e) {
					e.printStackTrace();
				}

				return seq;
			}

			@Override
			protected void finished() {
				try {
					if (Thread.currentThread().isInterrupted() || isCancelled()) {
						return;
					}

					BioSeq seq = get();
					if (seq != null) {
						gmodel.setSelectedSeq(seq);
					}

				} catch (Exception ex) {
					Logger.getLogger(GeneralLoadView.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		};

		GroupSelectionListener listener = new GroupSelectionListener() {

			public void groupSelectionChanged(GroupSelectionEvent evt) {
				if (evt.getSelectedGroup() != group) {
					worker.cancel(true);
				}
				gmodel.removeGroupSelectionListener(this);
			}
		};

		gmodel.addGroupSelectionListener(listener);
		
		if(IGB.getSingleton().getFrame().isVisible()){
			CThreadHolder.getInstance().execute(versionName, worker);
		}else{
			final ComponentListener componentListener = new ComponentAdapter() {
				
				@Override
				public void componentShown(ComponentEvent e) {
					IGB.getSingleton().getFrame().removeComponentListener(this);
					CThreadHolder.getInstance().execute(versionName, worker);
				}
			};
			IGB.getSingleton().getFrame().addComponentListener(componentListener);
		}
	}

	/**
	 * Run initialization of version on thread, so we don't lock up the GUI.
	 * Merge with initVersion();
	 */
	private class InitVersionWorker extends CThreadWorker<Void, Void> {

		private final String versionName;
		private final AnnotatedSeqGroup group;

		InitVersionWorker(String versionName, AnnotatedSeqGroup group) {
			super("init " + versionName);
			this.versionName = versionName;
			this.group = group;
		}

		@Override
		public Void runInBackground() {
			igbService.addNotLockedUpMsg(MessageFormat.format(BUNDLE.getString("loadingChr"), versionName));
			GeneralLoadUtils.initVersionAndSeq(versionName); // Make sure this genome versionName's feature names are initialized.
			return null;
		}

		@Override
		protected void finished() {
			igbService.removeNotLockedUpMsg(MessageFormat.format(BUNDLE.getString("loadingChr"), versionName));
			speciesCB.setEnabled(true);
			versionCB.setEnabled(true);
			if ((curGroup != null || group != null) && curGroup != group) {
				// avoid calling these a half-dozen times
				gmodel.setSelectedSeqGroup(group);
				// TODO: Need to be certain that the group is selected at this point!
				gmodel.setSelectedSeq(group.getSeq(0));
			}
		}
	}

	private void addListeners() {
		ServerList.getServerInstance().addServerInitListener(this);
		gmodel.addGroupSelectionListener(this);
		gmodel.addSeqSelectionListener(this);

		speciesCB.setEnabled(true);
		versionCB.setEnabled(true);
		speciesCB.addItemListener(this);
		versionCB.addItemListener(this);
		speciesCB.addItemListener(MainWorkspaceManager.getWorkspaceManager());
	}

	public JRPStyledTable getTable() {
		return seqtable;
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
			if(o1.toString().equals("genome")) {
				return 1;
			} else if (o2.toString().equals("genome")) {
				return -1;
			}
			return super.compare(o1, o2);
		}
	}
}
