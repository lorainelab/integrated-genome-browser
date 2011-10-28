package com.affymetrix.igb.search;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.thread.CThreadEvent;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import java.awt.Dimension;
import java.util.ArrayList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableColumn;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.GenericServerInitEvent;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.SeqMapRefreshed;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genometryImpl.thread.CThreadListener;
import com.affymetrix.genometryImpl.thread.CThreadWorker;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.JRPComboBoxWithSingleListener;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.genoviz.swing.recordplayback.JRPCheckBox;
import com.affymetrix.genoviz.swing.recordplayback.JRPTable;
import com.affymetrix.genoviz.swing.recordplayback.JRPTextField;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.shared.ISearchMode;
import com.affymetrix.igb.shared.IStatus;
import com.affymetrix.igb.shared.SearchResultsTableModel;

public final class SearchView extends IGBTabPanel implements 
		GroupSelectionListener, SeqSelectionListener, SeqMapRefreshed, GenericServerInitListener, IStatus {
	
	private static final long serialVersionUID = 0;
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("search");
	private static final int TAB_POSITION = 2;

	public class SearchModeAction extends GenericAction {
		private static final long serialVersionUID = 1L;
		@Override
		public String getText() { return null; }

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			String searchMode = (String) SearchView.this.searchCB.getSelectedItem();
			selectedSearchMode = searchModeMap.get(searchMode);
			if (selectedSearchMode == null) {
				return;
			}
			clearResults();
			igbService.getSeqMap().updateWidget();

			SearchView.this.initSequenceCB();
			SearchView.this.searchTF.setEnabled(true);

			boolean remoteEnabled = selectedSearchMode.useRemote();
			SearchView.this.remoteSearchCheckBox.setEnabled(remoteEnabled);
			if (!remoteEnabled) {
				SearchView.this.remoteSearchCheckBox.setSelected(false);
			}

			setModel(selectedSearchMode.getEmptyTableModel());

			SearchView.this.searchTF.setToolTipText(selectedSearchMode.getTooltip());

			return;
		}
	}
	private SearchModeAction searchModeAction = new SearchModeAction();

	public class SearchAction extends GenericAction {
		private static final long serialVersionUID = 1L;
		@Override
		public String getText() { return null; }

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			String searchMode = (String) SearchView.this.searchCB.getSelectedItem();
			selectedSearchMode = searchModeMap.get(searchMode);
			String chrStr = (String) SearchView.this.sequenceCB.getSelectedItem();
			final BioSeq chrfilter = igbService.getGenomeSeqId().equals(chrStr) ? null : group.getSeq(chrStr);
			if (selectedSearchMode.checkInput(SearchView.this.searchTF.getText().trim(), chrfilter, SearchView.this.sequenceCB.getSelectedItem().toString())) {
				enableComp(false);
				clearResults();
				CThreadWorker<Object, Void> worker = new CThreadWorker<Object, Void>(" ") {
					@Override
					protected Object runInBackground() {
						return selectedSearchMode.run(SearchView.this.searchTF.getText().trim(), chrfilter, SearchView.this.sequenceCB.getSelectedItem().toString(), remoteSearchCheckBox.isSelected(), SearchView.this, glyphs);
					}
					@Override
					protected void finished() {
						selectedSearchMode.finished(chrfilter);
						enableComp(true);
						try {
							SearchResultsTableModel model = (SearchResultsTableModel)get();
							if (model != null) {
								setModel(model);
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
					}
				};
				worker.addThreadListener(cancel);
				ThreadUtils.getPrimaryExecutor(this).execute(worker);
			}
		}
	}
	private SearchAction searchAction = new SearchAction();
	public class ClearAction extends GenericAction {
		private static final long serialVersionUID = 1L;
		@Override
		public String getText() { return null; }

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			clearResults();
			searchTF.setText("");
		}
	}
	private ClearAction clearAction = new ClearAction();

	// A maximum number of hits that can be found in a search.
	// This helps protect against out-of-memory errors.
	private static GenometryModel gmodel = GenometryModel.getGenometryModel();

	private static final String SEARCHLABELTEXT = "Search ";
	private static final String INLABELTEXT = "in ";
	private static final String FORLABELTEXT = "for ";
	private static final String CHOOSESEARCH = BUNDLE.getString("searchChooseSearch");
	private static final String FINDANNOTS = "Search for annotations or sequence in ";
	private static final String FINDANNOTSNULL = BUNDLE.getString("pleaseSelectGenome");
	private static final String SEQUENCETOSEARCH = BUNDLE.getString("searchSequenceToSearch");
	private static final String REMOTESERVERSEARCH1 = "also search remotely (";
	private static final String REMOTESERVERSEARCH2 = " server)";
	private static final String REMOTESERVERSEARCH2PLURAL = " servers)";
	private static final String REMOTESERVERSEARCH3 = " for IDs";
	
	private final JRPTextField searchTF = new JRPTextField("SearchView_searchTF", 10);
	private final JPanel pan1 = new JPanel();
	private final JRPComboBoxWithSingleListener sequenceCB = new JRPComboBoxWithSingleListener("SearchView_sequenceCB");
	private final JRPComboBoxWithSingleListener searchCB = new JRPComboBoxWithSingleListener("SearchView_searchCB");
	private final JRPCheckBox remoteSearchCheckBox = new JRPCheckBox("SearchView_remoteSearchCheckBox", "");
	private final JRPButton searchButton = new JRPButton("SearchView_searchButton", MenuUtil.getIcon("toolbarButtonGraphics/general/Find16.gif"));
	private final JRPButton clearButton = new JRPButton("SearchView_clearButton", MenuUtil.getIcon("toolbarButtonGraphics/general/Delete16.gif"));
	private final CancelButton cancel = new CancelButton("SearchView_CancelButton", igbService.getIcon("x_icon.gif"));
	private final List<GlyphI> glyphs = new ArrayList<GlyphI>();

	private JRPTable table = new JRPTable("SearchView_table");
	private JLabel status_bar = new JLabel("0 results");
	private TableRowSorter<SearchResultsTableModel> sorter;
	private ListSelectionModel lsm;
	private AnnotatedSeqGroup group;
	private int seqCount = 0;
	private CThreadWorker<Object, Void> worker;
	private Map<String, ISearchMode> searchModeMap;
	private ISearchMode selectedSearchMode;

	public SearchView(IGBService igbService) {
		super(igbService, BUNDLE.getString("searchTab"), BUNDLE.getString("searchTab"), false, TAB_POSITION);
		igbService.getSeqMapView().addToRefreshList(this);
		
		group = gmodel.getSelectedSeqGroup();

		this.setLayout(new BorderLayout());

		initSearchCB();

		initComponents();
		String annotsStr = (group == null) ? FINDANNOTSNULL : (FINDANNOTS + group.getID());
		pan1.setBorder(BorderFactory.createTitledBorder(annotsStr));
		pan1.setLayout(new BoxLayout(pan1, BoxLayout.X_AXIS));

		pan1.add(new JLabel(SearchView.SEARCHLABELTEXT));
		pan1.add(searchCB);

		pan1.add(Box.createRigidArea(new Dimension(4, 0)));
		pan1.add(new JLabel(SearchView.INLABELTEXT));
		sequenceCB.setMinimumSize(new Dimension(4, 0));
		sequenceCB.setToolTipText(SEQUENCETOSEARCH);
		pan1.add(sequenceCB);

		pan1.add(Box.createRigidArea(new Dimension(4, 0)));
		pan1.add(new JLabel(SearchView.FORLABELTEXT));
		pan1.add(searchTF);
		
		pan1.add(Box.createRigidArea(new Dimension(4, 0)));

		pan1.add(searchButton);
		pan1.add(clearButton);
		
		pan1.add(Box.createRigidArea(new Dimension(2, 0)));

		pan1.add(remoteSearchCheckBox);
		
		if (group == null) {
			searchCB.setEnabled(false);
			searchTF.setEnabled(false);
			searchButton.setEnabled(false);
		}

		this.initSequenceCB();

		this.initTable();

		this.add("North", pan1);

		
		JScrollPane scroll_pane = new JScrollPane(table);
		this.add(scroll_pane, BorderLayout.CENTER);

		Box bottom_row = Box.createHorizontalBox();
		this.add(bottom_row, BorderLayout.SOUTH);

		bottom_row.add(cancel);
		bottom_row.add(status_bar);
		validate();

		gmodel.addGroupSelectionListener(this);
		gmodel.addSeqSelectionListener(this);
		searchCB.addActionListener(searchModeAction);
		searchTF.addActionListener(searchAction);
		searchButton.addActionListener(searchAction);
		clearButton.addActionListener(clearAction);
		igbService.addServerInitListener(this);
	}

	private void initRemoteServerCheckBox(AnnotatedSeqGroup group) {
		int remoteServerCount = getRemoteServerCount(group);
		String remoteServerPluralText = remoteServerCount == 1 ? REMOTESERVERSEARCH2 : REMOTESERVERSEARCH2PLURAL;
		remoteSearchCheckBox.setText(REMOTESERVERSEARCH1 + remoteServerCount + remoteServerPluralText);
		remoteSearchCheckBox.setToolTipText(REMOTESERVERSEARCH1 + remoteServerCount + remoteServerPluralText + REMOTESERVERSEARCH3);
		remoteSearchCheckBox.setEnabled(remoteServerCount > 0);
		remoteSearchCheckBox.setSelected(remoteServerCount > 0);
	}

	private void initSequenceCB() {
		ThreadUtils.runOnEventQueue(new Runnable() {
			public void run() {
				// set up the sequence combo_box
				sequenceCB.removeAllItems();
				if (group != null) {
					if (selectedSearchMode != null && selectedSearchMode.useGenomeInSeqList()) {
						sequenceCB.addItem(igbService.getGenomeSeqId()); // put this at top of list
					}
					for (BioSeq seq : group.getSeqList()) {
						if (seq.getID().equals(igbService.getGenomeSeqId())) {
							continue;
						}
						sequenceCB.addItem(seq.getID());
					}
					sequenceCB.setToolTipText(SEQUENCETOSEARCH);
					sequenceCB.setEnabled(true);
				} else {
					sequenceCB.setToolTipText("Genome has not been selected");
					sequenceCB.setEnabled(false);
				}

				sequenceCB.setSelectedItem(igbService.getGenomeSeqId());
				sequenceCB.setPreferredSize(new Dimension(searchCB.getPreferredSize().width, searchCB.getPreferredSize().height));
			}
		});
	}

	public void initSearchCB() {
		Object saveSearchMode = searchCB.getSelectedItem();
		searchCB.removeAllItems();
		searchModeMap = new HashMap<String, ISearchMode>();
		boolean saveFound = false;
		List<ISearchMode> searchModes = ExtensionPointHandler.getExtensionPoint(ISearchMode.class).getExtensionPointImpls();

		for (ISearchMode searchMode : searchModes) {
			searchCB.addItem(searchMode.getName());
			searchModeMap.put(searchMode.getName(), searchMode);
			if (searchMode == saveSearchMode) {
				saveFound = true;
			}
		}
		searchCB.setToolTipText(CHOOSESEARCH);
		if (saveSearchMode == null || !saveFound) {
			if (searchCB.getItemCount() > 0) {
				searchCB.setSelectedIndex(0);
				saveSearchMode = searchCB.getSelectedItem();
			}
		}
		else {
			searchCB.setSelectedItem(saveSearchMode);
		}
		initSequenceCB();
	}

	private void initComponents() {
		searchTF.setVisible(true);
		searchTF.setEnabled(true);
		searchTF.setMinimumSize(new Dimension(125,50));

		initRemoteServerCheckBox(null);

		searchButton.setToolTipText("Search");
		searchButton.setEnabled(true);
		
		clearButton.setToolTipText("Clear");
		cancel.setEnabled(false);
	}

	private void initTable() {
		
		lsm = table.getSelectionModel();
		lsm.addListSelectionListener(list_selection_listener);
		lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

//		setModel(SearchModeHolder.getInstance().getSearchModes().get(0).getEmptyTableModel());
		table.setRowSelectionAllowed(true);
		table.setEnabled(true);
	}

	private void setModel(SearchResultsTableModel model){
		sorter = new TableRowSorter<SearchResultsTableModel>(model);
		table.setModel(model);
		table.setRowSorter(sorter);

		for(int i=0; i<model.getColumnWidth().length; i++){
			int colPer = model.getColumnWidth()[i];
			int colWidth = table.getWidth() * colPer/100;
			TableColumn column = table.getColumnModel().getColumn(i);
			column.setPreferredWidth(colWidth);

			int colAlign = model.getColumnAlign()[i];
			DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();
			dtcr.setHorizontalAlignment(colAlign);
			column.setCellRenderer(dtcr);
		}
	}

	/** This is called when the user selects a row of the table. */
	private final ListSelectionListener list_selection_listener = new ListSelectionListener() {

		public void valueChanged(ListSelectionEvent evt) {
			if (evt.getSource() == lsm && !evt.getValueIsAdjusting() && table.getModel().getRowCount() > 0) {
				int srow = table.getSelectedRow();
				srow = table.convertRowIndexToModel(srow);
				if (srow < 0) {
					return;
				}

				selectedSearchMode.valueChanged((SearchResultsTableModel)table.getModel(), srow, glyphs);
			}
		}
	};

	// remove the previous search results from the map.
	private void clearResults() {
		if (!glyphs.isEmpty()) {
			glyphs.clear();
			igbService.getSeqMapView().setAnnotatedSeq(igbService.getSeqMapView().getAnnotatedSeq(), true, true, true);
		}
		
		clearTable();
	}

	private void clearTable() {
		if (table.getModel() instanceof SearchResultsTableModel) {
			((SearchResultsTableModel)table.getModel()).clear();
		}
		((AbstractTableModel)table.getModel()).fireTableDataChanged();
	}

	public void enableComp(boolean enabled){
		searchTF.setEnabled(enabled);
		sequenceCB.setEnabled(enabled);
		searchCB.setEnabled(enabled);
		searchButton.setEnabled(enabled);
		clearButton.setEnabled(enabled);
	}

	private static int getRemoteServerCount(AnnotatedSeqGroup group) {
		if (group == null) {
			return 0;
		}
		int count = 0;
		for (GenericVersion gVersion : group.getEnabledVersions()) {
			if (gVersion.gServer.serverType == ServerType.DAS2) {
				count++;
			}
		}
		return count;
	}

	public void genericServerInit(GenericServerInitEvent evt) {
		initRemoteServerCheckBox(group);
	}
	
	public void groupSelectionChanged(GroupSelectionEvent evt) {
		groupOrSeqChange();
		clearResults();
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		groupOrSeqChange();
	}

	private void groupOrSeqChange() {
		AnnotatedSeqGroup newGroup = gmodel.getSelectedSeqGroup();
		int newSeqCount = (group == null) ? 0 : group.getSeqCount();
		String annotsStr = (newGroup == null) ? FINDANNOTSNULL : (FINDANNOTS + newGroup.getID());
		pan1.setBorder(BorderFactory.createTitledBorder(annotsStr));
		this.searchCB.setEnabled(newGroup != null);
		this.searchButton.setEnabled(newGroup != null);
		this.searchTF.setEnabled(newGroup != null);
		setStatus("");
		
		// only re-initialize the combobox if the group or seqs have changed
		if (newGroup != group || seqCount != newSeqCount) {
			group = newGroup;
			seqCount = newSeqCount;
			this.initSequenceCB();
			initRemoteServerCheckBox(group);

		}
	}

	/** Set the text in the status bar in a thread-safe way. */
	public void setStatus(final String text) {
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				status_bar.setText(text);
			}
		});
	}

	public void mapRefresh() {
		igbService.mapRefresh(glyphs);
	}

	@Override
	public boolean isEmbedded() {
		return true;
	}
	
	@SuppressWarnings("serial")
	private class CancelButton extends JRPButton implements CThreadListener, ActionListener{
		
		public CancelButton(String id, ImageIcon icon){
			super(id, icon);
			setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));
			addActionListener(this);
		}
		
		public void heardThreadEvent(CThreadEvent cte) {
			if(cte.getState() == CThreadEvent.STARTED){
				setEnabled(true);
			}else{
				setEnabled(false);
			}
		}

		public void actionPerformed(ActionEvent ae) {
			if(worker != null && !worker.isCancelled() && !worker.isDone()){
				worker.cancel(true);
			}
		}
		
	}
}
