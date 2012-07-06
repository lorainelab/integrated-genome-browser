package com.affymetrix.igb.search;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import java.awt.Dimension;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableColumn;


import com.affymetrix.common.ExtensionPointHandler;

import com.affymetrix.genometryImpl.thread.CThreadEvent;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.GenericServerInitEvent;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genometryImpl.thread.CThreadListener;
import com.affymetrix.genometryImpl.thread.CThreadWorker;

import com.affymetrix.genoviz.swing.ColorTableCellRenderer;
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
import javax.swing.table.TableCellRenderer;

public final class SearchView extends IGBTabPanel implements
		GroupSelectionListener, SeqSelectionListener, GenericServerInitListener, IStatus {

	private static final long serialVersionUID = 0;
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("search");
	private static final String DEFAULT_SEARCH_MODE_CLASS = "SearchModeID";
	private static final int TAB_POSITION = 2;

	public class SearchModeAction extends GenericAction {

		private static final long serialVersionUID = 1L;

		@Override
		public String getText() {
			return null;
		}

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			String searchMode = (String) SearchView.this.searchCB.getSelectedItem();
			selectedSearchMode = searchModeMap.get(searchMode);
			if (selectedSearchMode == null) {
				return;
			}
			clearTable();
			igbService.getSeqMap().updateWidget();

			SearchView.this.initSequenceCB();
//			SearchView.this.searchTF.setEnabled(true);

			initOptionCheckBox();

			setModel(selectedSearchMode.getEmptyTableModel());

			SearchView.this.searchTF.setToolTipText(selectedSearchMode.getTooltip());

			return;
		}
	}
	private SearchModeAction searchModeAction = new SearchModeAction();

	public class SearchAction extends GenericAction {

		private static final long serialVersionUID = 1L;

		@Override
		public String getText() {
			return null;
		}

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			String searchMode = (String) SearchView.this.searchCB.getSelectedItem();
			selectedSearchMode = searchModeMap.get(searchMode);
			String chrStr = (String) SearchView.this.sequenceCB.getSelectedItem();
			final BioSeq chrfilter = Constants.GENOME_SEQ_ID.equals(chrStr) ? null : group.getSeq(chrStr);
			String errorMessage = selectedSearchMode.checkInput(SearchView.this.searchTF.getText().trim(), chrfilter, SearchView.this.sequenceCB.getSelectedItem().toString());
			if (errorMessage == null) {
				enableComp(false);
				clearTable();
				CThreadWorker<Object, Void> worker = new CThreadWorker<Object, Void>(" ") {

					@Override
					protected Object runInBackground() {
						return selectedSearchMode.run(SearchView.this.searchTF.getText().trim(), chrfilter, SearchView.this.sequenceCB.getSelectedItem().toString(), optionCheckBox.isSelected(), SearchView.this);
					}

					@Override
					protected void finished() {
						selectedSearchMode.finished(chrfilter);
						enableComp(true);
						initOptionCheckBox();
						try {
							SearchResultsTableModel model = (SearchResultsTableModel) get();
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
			else {
				ErrorHandler.errorPanel(errorMessage);
			}
		}
	}
	private SearchAction searchAction = new SearchAction();

	public class ClearAction extends GenericAction {

		private static final long serialVersionUID = 1L;

		@Override
		public String getText() {
			return null;
		}

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			clearResults();
			searchTF.setText("");
		}
	}
	
	ItemListener itemListener = new ItemListener(){

		public void itemStateChanged(ItemEvent e) {
			if(selectedSearchMode != null && selectedSearchMode.useOption()){
				JCheckBox checkbox = (JCheckBox)e.getSource();
				selectedSearchMode.setOptionState(checkbox.isSelected());
			}
		}
		
	};
		
	private ClearAction clearAction = new ClearAction();
	// A maximum number of hits that can be found in a search.
	// This helps protect against out-of-memory errors.
	private static GenometryModel gmodel = GenometryModel.getGenometryModel();
	private static final String CHOOSESEARCH = BUNDLE.getString("searchChooseSearch");
	private static final String FINDANNOTS = BUNDLE.getString("findAnnots");
	private static final String FINDANNOTSNULL = BUNDLE.getString("pleaseSelectGenome");
	private static final String SEQUENCETOSEARCH = BUNDLE.getString("searchSequenceToSearch");
	private final JRPTextField searchTF = new JRPTextField("SearchView_searchTF", 10);
	private final JPanel pan1 = new JPanel();
	private final JRPComboBoxWithSingleListener sequenceCB = new JRPComboBoxWithSingleListener("SearchView_sequenceCB");
	private final JRPComboBoxWithSingleListener searchCB = new JRPComboBoxWithSingleListener("SearchView_searchCB");
	private final JRPCheckBox optionCheckBox = new JRPCheckBox("SearchView_optionCheckBox", "");
	private final JRPButton searchButton = new JRPButton("SearchView_searchButton", MenuUtil.getIcon("images/search.png"));
	private final JRPButton clearButton = new JRPButton("SearchView_clearButton", MenuUtil.getIcon("images/delete.png"));
	private final CancelButton cancel = new CancelButton("SearchView_CancelButton",MenuUtil.getIcon("images/stop.png"));
	private JRPTable table = new JRPTable("SearchView_table");
	private JLabel status_bar = new JLabel(BUNDLE.getString("noResults"));
	private TableRowSorter<SearchResultsTableModel> sorter;
	private ListSelectionModel lsm;
	private AnnotatedSeqGroup group;
	private int seqCount = 0;
	private CThreadWorker<Object, Void> worker;
	private Map<String, ISearchMode> searchModeMap;
	private ISearchMode selectedSearchMode;

	public SearchView(IGBService igbService) {
		super(igbService, BUNDLE.getString("searchTab"), BUNDLE.getString("searchTab"), false, TAB_POSITION);
		
		group = gmodel.getSelectedSeqGroup();

		this.setLayout(new BorderLayout());

		initSearchCB();

		initComponents();
		String annotsStr = (group == null) ? FINDANNOTSNULL : MessageFormat.format(FINDANNOTS, group.getID());
		pan1.setBorder(BorderFactory.createTitledBorder(annotsStr));
		pan1.setLayout(new BoxLayout(pan1, BoxLayout.X_AXIS));

		pan1.add(new JLabel(BUNDLE.getString("searchLabelText")));
		pan1.add(searchCB);

		pan1.add(Box.createRigidArea(new Dimension(4, 0)));
		pan1.add(new JLabel(BUNDLE.getString("inLabelText")));
		sequenceCB.setMinimumSize(new Dimension(4, 0));
		sequenceCB.setToolTipText(SEQUENCETOSEARCH);
		pan1.add(sequenceCB);

		pan1.add(Box.createRigidArea(new Dimension(4, 0)));
		pan1.add(new JLabel(BUNDLE.getString("forLabelText")));
		pan1.add(searchTF);

		pan1.add(Box.createRigidArea(new Dimension(4, 0)));

		pan1.add(searchButton);
		pan1.add(clearButton);

		pan1.add(Box.createRigidArea(new Dimension(2, 0)));

		pan1.add(optionCheckBox);

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
		optionCheckBox.addItemListener(itemListener);
		igbService.addServerInitListener(this);
	}

	private void initOptionCheckBox() {
		String searchMode = (String) searchCB.getSelectedItem();
		selectedSearchMode = searchModeMap.get(searchMode);
		
		if(selectedSearchMode == null)
			return;
		
		if(selectedSearchMode.useOption()){
			int remoteServerCount = getRemoteServerCount(group);
			optionCheckBox.setText(selectedSearchMode.getOptionName(remoteServerCount));
			optionCheckBox.setToolTipText(selectedSearchMode.getOptionTooltip(remoteServerCount));
			boolean enabled = selectedSearchMode.getOptionEnable(remoteServerCount);
			optionCheckBox.setEnabled(enabled);
			if(!enabled){
				optionCheckBox.setSelected(false);
			}else{
				optionCheckBox.setSelected(selectedSearchMode.getOptionState());
			}
		}else{
			optionCheckBox.setEnabled(false);
			optionCheckBox.setSelected(false);
		}
		
	}

	private void initSequenceCB() {
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				// set up the sequence combo_box
				sequenceCB.removeAllItems();
				if (group != null) {
					if (selectedSearchMode != null && selectedSearchMode.useGenomeInSeqList()) {
						sequenceCB.addItem(Constants.GENOME_SEQ_ID); // put this at top of list
					}
					for (BioSeq seq : group.getSeqList()) {
						if (seq.getID().equals(Constants.GENOME_SEQ_ID)) {
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

				sequenceCB.setSelectedItem(Constants.GENOME_SEQ_ID);
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
		// consistent order for search modes
		Collections.sort(searchModes,
			new Comparator<ISearchMode>() {
				@Override
				public int compare(ISearchMode o1, ISearchMode o2) {
					return o1.getClass().getName().compareTo(o2.getClass().getName());
				}
			
			}
		);

		ISearchMode defaultSearchMode = null;
		for (ISearchMode searchMode : searchModes) {
			searchCB.addItem(searchMode.getName());
			searchModeMap.put(searchMode.getName(), searchMode);
			if (searchMode == saveSearchMode) {
				saveFound = true;
			}
			if (DEFAULT_SEARCH_MODE_CLASS.equals(searchMode.getClass().getSimpleName())) {
				defaultSearchMode = searchMode;
			}
		}
		searchCB.setToolTipText(CHOOSESEARCH);
		if (saveSearchMode == null || !saveFound) {
			if (defaultSearchMode != null) {
				searchCB.setSelectedItem(defaultSearchMode);
				saveSearchMode = defaultSearchMode;
			}
			else if (searchCB.getItemCount() > 0) {
				searchCB.setSelectedIndex(0);
				saveSearchMode = searchCB.getSelectedItem();
			}
		} else {
			searchCB.setSelectedItem(saveSearchMode);
		}
		initSequenceCB();
	}

	private void initComponents() {
		searchTF.setEnabled(true);
		searchTF.setMinimumSize(new Dimension(125, 50));

		searchButton.setToolTipText("Search");
		searchButton.setEnabled(true);

		clearButton.setToolTipText("Clear");
		cancel.setEnabled(false);
	}

	private void initTable() {

		lsm = table.getSelectionModel();
		lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

//		setModel(SearchModeHolder.getInstance().getSearchModes().get(0).getEmptyTableModel());
		table.setRowSelectionAllowed(true);
		table.setEnabled(true);

		table.addMouseListener(list_selection_listener);
	}

	private void setModel(SearchResultsTableModel model) {
		sorter = new TableRowSorter<SearchResultsTableModel>(model);
		table.setModel(model);
		table.setRowSorter(sorter);
		
		for (int i = 0; i < model.getColumnWidth().length; i++) {
			int colPer = model.getColumnWidth()[i];
			int colWidth = table.getWidth() * colPer / 100;
			TableColumn column = table.getColumnModel().getColumn(i);
			column.setPreferredWidth(colWidth);

			int colAlign = model.getColumnAlign()[i];
			DefaultTableCellRenderer dtcr = model.getColumnRenderer(i);
			dtcr.setHorizontalAlignment(colAlign);
			column.setCellRenderer(dtcr);
		}
	}
	
	/** This is called when the user double click a row of the table. */
	private final MouseListener list_selection_listener = new MouseListener() {

		public void mouseClicked(MouseEvent e) {
				if (e.getComponent().isEnabled()
						&& e.getButton() == MouseEvent.BUTTON1
						&& e.getClickCount() == 2) {
					int srow = table.getSelectedRow();
					srow = table.convertRowIndexToModel(srow);
					if (srow < 0) {
						return;
					}
					selectedSearchMode.valueChanged(
							(SearchResultsTableModel) table.getModel(), srow);
				}
			}

		public void mousePressed(MouseEvent me) {}

		public void mouseReleased(MouseEvent me) {}

		public void mouseEntered(MouseEvent me) {}

		public void mouseExited(MouseEvent me) {}
	};

	// remove the previous search results from the map.
	private void clearResults() {
		String searchMode = (String) SearchView.this.searchCB.getSelectedItem();
		selectedSearchMode = searchModeMap.get(searchMode);
		if (selectedSearchMode != null) {
			selectedSearchMode.clear();
		}
		clearTable();
	}

	private void clearTable() {
		if (table.getModel() instanceof SearchResultsTableModel) {
			((SearchResultsTableModel) table.getModel()).clear();
		}
		((AbstractTableModel) table.getModel()).fireTableDataChanged();
	}

	public void enableComp(boolean enabled) {
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
		initOptionCheckBox();
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
		String annotsStr = (newGroup == null) ? FINDANNOTSNULL : MessageFormat.format(FINDANNOTS, newGroup.getID());
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
			initOptionCheckBox();
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


	@Override
	public boolean isEmbedded() {
		return true;
	}

	@SuppressWarnings("serial")
	private class CancelButton extends JRPButton implements CThreadListener, ActionListener {

		public CancelButton(String id, ImageIcon icon) {
			super(id, icon);
			setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));
			addActionListener(this);
		}

		public void heardThreadEvent(CThreadEvent cte) {
			if (cte.getState() == CThreadEvent.STARTED) {
				setEnabled(true);
			} else {
				setEnabled(false);
			}
		}

		public void actionPerformed(ActionEvent ae) {
			if (worker != null && !worker.isCancelled() && !worker.isDone()) {
				worker.cancel(true);
			}
		}
	}
}
