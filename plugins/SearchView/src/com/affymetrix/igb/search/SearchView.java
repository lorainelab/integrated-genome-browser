package com.affymetrix.igb.search;

import com.affymetrix.genometryImpl.thread.CThreadEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import java.awt.Dimension;
import java.util.ArrayList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableColumn;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.event.GenericServerInitEvent;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.event.SeqMapRefreshed;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.das2.SimpleDas2Feature;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.thread.CThreadListener;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.genometryImpl.util.SearchUtils;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.util.DNAUtils;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.swing.JComboBoxWithSingleListener;

public final class SearchView extends IGBTabPanel implements 
		ActionListener, GroupSelectionListener, SeqSelectionListener, SeqMapRefreshed, GenericServerInitListener {
	
	private static final long serialVersionUID = 0;
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("search");
	private static final int TAB_POSITION = 2;

	// A maximum number of hits that can be found in a search.
	// This helps protect against out-of-memory errors.
	private final static int MAX_HITS = 100000;
	private static GenometryModel gmodel = GenometryModel.getGenometryModel();
	private static AnnotatedSeqGroup group;
	private static int seqCount = 0;
	private static final int MAX_RESIDUE_LEN_SEARCH = 1000000;

	private static final String SEARCHLABELTEXT = "Search ";
	private static final String INLABELTEXT = "in ";
	private static final String FORLABELTEXT = "for ";
	private static final String REGEXID = BUNDLE.getString("searchRegexIDOrName");
	private static final String REGEXIDTF = BUNDLE.getString("searchRegexIDOrNameTF");
	private static final String REGEXRESIDUE = BUNDLE.getString("searchRegexResidue");
	private static final String REGEXRESIDUETF = BUNDLE.getString("searchRegexResidueTF");
	private static final String REGEXPROPS = BUNDLE.getString("searchRegexProps");
	private static final String REGEXPROPSTF = BUNDLE.getString("searchRegexPropsTF");
	private static final String CHOOSESEARCH = BUNDLE.getString("searchChooseSearch");
	private static final String FINDANNOTS = "Search for annotations or sequence in ";
	private static final String FINDANNOTSNULL = BUNDLE.getString("pleaseSelectGenome");
	private static final String SEQUENCETOSEARCH = BUNDLE.getString("searchSequenceToSearch");
	private static final String REMOTESERVERSEARCH1 = "also search remotely (";
	private static final String REMOTESERVERSEARCH2 = " server)";
	private static final String REMOTESERVERSEARCH2PLURAL = " servers)";
	private static final String REMOTESERVERSEARCH3 = " for IDs";

	private static final String SELECTINMAP_TEXT = BUNDLE.getString("searchSelectInMapText");
	private static final String SELECTINMAP_TIP = BUNDLE.getString("searchSelectInMapTip");

	private JTextField searchTF;
	private final JPanel pan1 = new JPanel();
	private final JComboBox sequenceCB = new JComboBoxWithSingleListener();
	private final JComboBox searchCB = new JComboBoxWithSingleListener();
	private final JCheckBox remoteSearchCheckBox = new JCheckBox("");
	private final JCheckBox selectInMapCheckBox = new JCheckBox(SELECTINMAP_TEXT);
	private final JButton searchButton = new JButton(MenuUtil.getIcon("toolbarButtonGraphics/general/Find16.gif"));
	private final JButton clearButton = new JButton(MenuUtil.getIcon("toolbarButtonGraphics/general/Delete16.gif"));
	private final CancelButton cancel = new CancelButton(igbService.getIcon("x_icon.gif"));;
	private final List<GlyphI> glyphs = new ArrayList<GlyphI>();
	private final static Color hitcolor = new Color(150, 150, 255);

	private  JTable table = new JTable();
	private  JLabel status_bar = new JLabel("0 results");
	private TableRowSorter<SearchResultsTableModel> sorter;
	private ListSelectionModel lsm;
	
	private CThreadWorker worker;
	private List<SeqSymmetry> remoteSymList;

	public SearchView(IGBService igbService) {
		super(igbService, BUNDLE.getString("searchTab"), BUNDLE.getString("searchTab"), false, TAB_POSITION);
		igbService.addSeqMapRefreshedListener(this);
		
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
		sequenceCB.setPreferredSize(new Dimension(searchCB.getPreferredSize().width, searchCB.getPreferredSize().height));
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
		//pan1.add(selectInMapCheckBox);

		
		if (group == null) {
			searchCB.setEnabled(false);
			searchTF.setEnabled(false);
			searchButton.setEnabled(false);
		}

		
		this.initSequenceCB();

		this.initTable();

		

		//Create a separate form for filterText
       /* JLabel l1 = new JLabel("Filter ID:", SwingConstants.TRAILING);
        pan1.add(l1);
       //Whenever filterText changes, invoke newFilter.
		filterText.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				newFilter();
			}

			public void insertUpdate(DocumentEvent e) {
				newFilter();
			}

			public void removeUpdate(DocumentEvent e) {
				newFilter();
			}
		});
        l1.setLabelFor(filterText);



        pan1.add(filterText);*/

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
		searchCB.addActionListener(this);
		searchTF.addActionListener(this);
		searchButton.addActionListener(this);
		clearButton.addActionListener(this);
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
		igbService.runOnEventQueue(new Runnable() {
			public void run() {
				// set up the sequence combo_box
				sequenceCB.removeAllItems();
				if (group != null) {
					if (!((String) searchCB.getSelectedItem()).equals(REGEXRESIDUE)) {
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
			}
		});
	}

	private void initSearchCB() {
		searchCB.removeAllItems();
		searchCB.addItem(REGEXID);
		searchCB.addItem(REGEXRESIDUE);
		searchCB.addItem(REGEXPROPS);
		searchCB.setToolTipText(CHOOSESEARCH);
	}

	private void initComponents() {
		searchTF = new JTextField(10);
		searchTF.setVisible(true);
		searchTF.setEnabled(true);
		searchTF.setMinimumSize(new Dimension(125,50));

		initRemoteServerCheckBox(null);

		selectInMapCheckBox.setToolTipText(SELECTINMAP_TIP);
		selectInMapCheckBox.setEnabled(true);
		
		searchButton.setToolTipText("Search");
		searchButton.setEnabled(true);
		
		clearButton.setToolTipText("Clear");
		cancel.setEnabled(false);
	}

	private void initTable() {
		
		lsm = table.getSelectionModel();
		lsm.addListSelectionListener(list_selection_listener);
		lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		setModel(new SymSearchResultsTableModel(Collections.<SeqSymmetry>emptyList()));
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
			
				if (table.getModel() instanceof SymSearchResultsTableModel) {
					SymSearchResultsTableModel model = (SymSearchResultsTableModel)table.getModel();
					SeqSymmetry sym = model.get(srow);

					if (sym != null) {
						if (remoteSymList != null && remoteSymList.contains(sym)) {
							if (group == null) {
								return;
							}
							zoomToCoord(sym);
							return;
						}

						if (igbService.getItem(sym) == null) {
							if (group == null) {
								return;
							}
							// Couldn't find sym in map view! Go ahead and zoom to it.
							zoomToCoord(sym);
							return;
						}

						// Set selected symmetry normally
						List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>(1);
						syms.add(sym);
						gmodel.setSelectedSymmetriesAndSeq(syms, this);
					}
				}else if(table.getModel() instanceof GlyphSearchResultsTableModel){
					GlyphSearchResultsTableModel model = (GlyphSearchResultsTableModel)table.getModel();
					GlyphI glyph = model.get(srow);
					for(GlyphI g : glyphs){
						igbService.deselect(g);
					}
					if(glyph != null){
						int start = (int)glyph.getCoordBox().x;
						int end = (int)(glyph.getCoordBox().x + glyph.getCoordBox().width);
						igbService.select(glyph);
						igbService.zoomToCoord(model.seq, start, end);
						igbService.centerAtHairline();
					}
				}
				
			}
		}

		private void zoomToCoord(SeqSymmetry sym) throws NumberFormatException {
			String seqID = sym.getSpanSeq(0).getID();
			BioSeq seq = group.getSeq(seqID);
			if (seq != null) {
				SeqSpan span = sym.getSpan(0);
				if (span != null) {
					// zoom to its coordinates
					igbService.zoomToCoord(seqID, span.getStart(), span.getEnd());
				}
			}
		}
	};

	// remove the previous search results from the map.
	private void clearResults() {
		if (!glyphs.isEmpty()) {
			glyphs.clear();
			igbService.setAnnotatedSeq(igbService.getAnnotatedSeq(), true, true, true);
		}
		
		clearTable();
	}

	private void clearTable() {
		((SearchResultsTableModel)table.getModel()).clear();
		((AbstractTableModel)table.getModel()).fireTableDataChanged();
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		if (src == this.searchCB) {
			clearResults();
			igbService.updateWidget();

			String searchMode = (String) this.searchCB.getSelectedItem();
			this.initSequenceCB();
			this.searchTF.setEnabled(true);

			boolean remoteEnabled = REGEXID.equals(searchMode);
			this.remoteSearchCheckBox.setEnabled(remoteEnabled);
			if (!remoteEnabled) {
				this.remoteSearchCheckBox.setSelected(false);
			}

			boolean isResidueSearch = REGEXRESIDUE.equals(searchMode);

			if(isResidueSearch){
				setModel(new GlyphSearchResultsTableModel(Collections.<GlyphI>emptyList(),""));
			}else{
				setModel(new SymSearchResultsTableModel(Collections.<SeqSymmetry>emptyList()));
			}

			boolean displaySelectedEnabled = !isResidueSearch;
			this.selectInMapCheckBox.setEnabled(displaySelectedEnabled);
			if (!displaySelectedEnabled) {
				this.selectInMapCheckBox.setSelected(true);	// we ALWAYS display in map if it's residues.
			}

			if (REGEXID.equals(searchMode)) {
				this.searchTF.setToolTipText(REGEXIDTF);
			}if (REGEXPROPS.equals(searchMode)) {
				this.searchTF.setToolTipText(REGEXPROPSTF);
			} else if (REGEXRESIDUE.equals(searchMode)) {
				this.searchTF.setToolTipText(REGEXRESIDUETF);
			} else {
				this.searchTF.setToolTipText("");
			}

			return;
		}
		if (src == this.searchTF || src == this.searchButton) {
			String searchMode = (String) this.searchCB.getSelectedItem();
			String chrStr = (String) this.sequenceCB.getSelectedItem();
			BioSeq chrfilter = igbService.getGenomeSeqId().equals(chrStr) ? null : group.getSeq(chrStr);
			if (REGEXID.equals(searchMode)) {
				displayRegexIDs(this.searchTF.getText().trim(), chrfilter, false);	// note we trim in case the user added spaces, which really shouldn't be on the outside of IDs or names
			} else if (REGEXPROPS.equals(searchMode)){
				displayRegexIDs(this.searchTF.getText().trim(), chrfilter, true);
			}else if (REGEXRESIDUE.equals(searchMode)) {
				displayRegexResidues(chrfilter);
			} 
		}
		if(src == this.clearButton){
			clearResults();
			searchTF.setText("");
		}
	}

	private void enableComp(boolean enabled){
		searchTF.setEnabled(enabled);
		sequenceCB.setEnabled(enabled);
		searchCB.setEnabled(enabled);
		searchButton.setEnabled(enabled);
		clearButton.setEnabled(enabled);
	}

	private void displayRegexIDs(final String search_text, final BioSeq chrFilter, final boolean search_props) {
		worker = new CThreadWorker(" "){

			@Override
			protected Object runInBackground() {
				
				String text = search_text;
				Pattern regex = null;
				try {
					String regexText = search_text;
					// Make sure this search is reasonable to do on a remote server.
					if (!(regexText.contains("*") || regexText.contains("^") || regexText.contains("$"))) {
						// Not much of a regular expression.  Assume the user wants to match at the start and end
						regexText = ".*" + regexText + ".*";
					}
					regex = Pattern.compile(regexText, Pattern.CASE_INSENSITIVE);
				} catch (PatternSyntaxException pse) {
					ErrorHandler.errorPanel("Regular expression syntax error...\n" + pse.getMessage());
					return null;
				} catch (Exception ex) {
					ErrorHandler.errorPanel("Problem with regular expression...", ex);
					return null;
				}
				enableComp(false);
				clearResults();

				String friendlySearchStr = friendlyString(text, sequenceCB.getSelectedItem().toString());
				setStatus(friendlySearchStr + ": Searching locally...");
				List<SeqSymmetry> localSymList = SearchUtils.findLocalSyms(group, chrFilter, regex, search_props);
				remoteSymList = null;

				// Make sure this search is reasonable to do on a remote server.
				if (!(text.contains("*") || text.contains("^") || text.contains("$"))) {
					// Not much of a regular expression.  Assume the user wants to match at the start and end
					text = "*" + text + "*";
				}
				friendlySearchStr = friendlyString(text, sequenceCB.getSelectedItem().toString());
				int actualChars = text.length();
				if (text.startsWith(".*")) {
					actualChars -= 2;
				} else if (text.startsWith("*")) {
					actualChars -= 1;
				}
				if (text.endsWith(".*")) {
					text = text.substring(0, text.length() - 2) + "*";	// hack for bug in DAS/2 server
					actualChars -= 2;
				} else if (text.endsWith("*")) {
					actualChars -= 1;
				}

				if (remoteSearchCheckBox.isSelected()) {
					if (actualChars < 3) {
						ErrorHandler.errorPanel(friendlySearchStr + ": Text is too short to allow remote search.");
						enableComp(true);
						return null;
					}

					setStatus(friendlySearchStr + ": Searching remotely...");
					remoteSymList = remoteSearchFeaturesByName(group, text, chrFilter);
				}

				if (localSymList.isEmpty() && (remoteSymList == null || remoteSymList.isEmpty())) {
					setStatus(friendlySearchStr + ": No matches");
					enableComp(true);
					return null;
				}

				String statusStr = friendlySearchStr + ": " + (localSymList == null ? 0 : localSymList.size()) + " local matches";
				if (remoteSearchCheckBox.isSelected() && actualChars >= 3) {
					statusStr += ", " + (remoteSymList == null ? 0 : remoteSymList.size()) + " remote matches";
				}
				setStatus(statusStr);
				if (selectInMapCheckBox.isSelected()) {
					gmodel.setSelectedSymmetriesAndSeq(localSymList, this);
				}
				if (remoteSymList != null) {
					localSymList.addAll(remoteSymList);
				}

				List<SeqSymmetry> tableRows = filterBySeq(localSymList, chrFilter);
				Collections.sort(tableRows, new Comparator<SeqSymmetry>() {
					public int compare(SeqSymmetry s1, SeqSymmetry s2) {
						return s1.getID().compareTo(s2.getID());
					}
				});
				setModel(new SymSearchResultsTableModel(tableRows));

				enableComp(true);
				
				return null;
			}

			@Override
			protected void finished() {	}
			
		};
		worker.addThreadListener(cancel);
		igbService.getPrimaryExecutor(this).execute(worker);
	}

	private static List<SeqSymmetry> filterBySeq(List<SeqSymmetry> results, BioSeq seq) {

		if (results == null || results.isEmpty()) {
			return new ArrayList<SeqSymmetry>();
		}

		int num_rows = results.size();

		List<SeqSymmetry> rows = new ArrayList<SeqSymmetry>(num_rows / 10);
		for (int j = 0; j < num_rows && rows.size() < MAX_HITS; j++) {
			SeqSymmetry result = results.get(j);

			SeqSpan span = null;
			if (seq != null) {
				span = result.getSpan(seq);
				if (span == null) {
					// Special case when chromosomes are not equal, but have same ID (i.e., really they're equal)
					SeqSpan tempSpan = result.getSpan(0);
					if (tempSpan != null && tempSpan.getBioSeq() != null && seq.getID().equals(tempSpan.getBioSeq().getID())) {
						span = tempSpan;
					}
				}
			} else {
				span = result.getSpan(0);
			}
			if (span == null) {
				continue;
			}

			rows.add(result);
		}

		return rows;
	}	
	

	/**
	 * Display (highlight on SeqMap) the residues matching the specified regex.
	 */
	private void displayRegexResidues(final BioSeq vseq) {
		if (vseq == null ) {
			ErrorHandler.errorPanel(
					"Residues for " + this.sequenceCB.getSelectedItem().toString() + " not available.  Please load residues before searching.");
			return;
		}

		if(vseq != igbService.getAnnotatedSeq()){
			boolean confirm = igbService.confirmPanel("Sequence " + vseq.getID() +
					" is not same as selected sequence " + igbService.getAnnotatedSeq().getID() +
					". \nPlease select the sequence before proceeding." +
					"\nDo you want to select sequence now ?");
			if(!confirm)
				return;
			SeqSpan viewspan = igbService.getVisibleSpan();
			int min = Math.max((viewspan.getMin() > vseq.getMax() ? -1 : viewspan.getMin()), vseq.getMin());
			int max = Math.min(viewspan.getMax(), vseq.getMax());
			SeqSpan newspan = new SimpleSeqSpan(min, max, vseq);
			gmodel.setSelectedSeq(vseq);
			igbService.zoomTo(newspan);
		}

		final boolean isComplete = vseq.isComplete();
		final boolean confirm = isComplete ? true : igbService.confirmPanel("Residues for " + sequenceCB.getSelectedItem().toString()
							+ " not loaded.  \nDo you want to load residues?");
		if (!confirm) {
			return;
		}
		
		worker = new CThreadWorker(" ") {

			@Override
			protected Object runInBackground() {
				if (!isComplete && confirm) {
					igbService.loadResidues(igbService.getVisibleSpan(), true);
				}
				regexTF(vseq);
				return null;
			}

			@Override
			protected void finished() {
				if (!isComplete && confirm) {
					igbService.setAnnotatedSeq(vseq, true, true, true);
				}
			}
		};
		worker.addThreadListener(cancel);
		igbService.getPrimaryExecutor(this).execute(worker);
	}

	private void regexTF(BioSeq vseq) {
		String searchStr = searchTF.getText().trim();
		String friendlySearchStr = friendlyString(searchStr, vseq.getID());
		if (searchStr.length() < 3) {
			ErrorHandler.errorPanel("Search must contain at least 3 characters");
			return;
		}
		Pattern regex = null;
		try {
			regex = Pattern.compile(searchStr, Pattern.CASE_INSENSITIVE);
		} catch (PatternSyntaxException pse) {
			ErrorHandler.errorPanel("Regular expression syntax error...\n" + pse.getMessage());
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem with regular expression...", ex);
			return;
		}

		clearResults();
		setStatus(friendlySearchStr + ": Working...");

		int residuesLength = vseq.getLength();
		int hit_count1 = 0;
		int hit_count2 = 0;
		int residue_offset1 = vseq.getMin();
		int residue_offset2 = vseq.getMax();
		Thread current_thread = Thread.currentThread();
		
		for(int i=0; i<residuesLength; i+=MAX_RESIDUE_LEN_SEARCH){
			if(current_thread.isInterrupted())
				break;
			
			int start = Math.max(i-searchStr.length(), 0);
			int end = Math.min(i+MAX_RESIDUE_LEN_SEARCH, residuesLength);
			
			String residues = vseq.getResidues(start, end);
			hit_count1 += igbService.searchForRegexInResidues(true, regex, residues, Math.max(residue_offset1,start), glyphs, hitcolor);

			// Search for reverse complement of query string
			// flip searchstring around, and redo nibseq search...
			String rev_searchstring = DNAUtils.reverseComplement(residues);
			hit_count2 += igbService.searchForRegexInResidues(false, regex, rev_searchstring, Math.min(residue_offset2,end), glyphs, hitcolor);
		}

		setStatus("Found " + ": " + hit_count1 + " forward and " + hit_count2 + " reverse strand hits. Click row to view hit.");
		igbService.updateWidget();

		Collections.sort(glyphs, new Comparator<GlyphI>() {
			public int compare(GlyphI g1, GlyphI g2) {
				return Integer.valueOf((int)g1.getCoordBox().x).compareTo((int)g2.getCoordBox().x);
			}
		});
		setModel(new GlyphSearchResultsTableModel(glyphs, vseq.getID()));
		enableComp(true);
	}

	private static String friendlyString(String text, String chr) {
		return "Search for " + text + " on " + chr;
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

	private static List<SeqSymmetry> remoteSearchFeaturesByName(AnnotatedSeqGroup group, String name, BioSeq chrFilter) {
		List<SeqSymmetry> features = new ArrayList<SeqSymmetry>();

		if (name == null || name.isEmpty()) {
			return features;
		}

		for (GenericVersion gVersion : group.getEnabledVersions()) {
			if (gVersion.gServer.serverType == ServerType.DAS2) {
				Das2VersionedSource version = (Das2VersionedSource) gVersion.versionSourceObj;
				if (version != null) {
					List<SeqSymmetry> newFeatures = version.getFeaturesByName(name, group, chrFilter);
					if (newFeatures != null) {
						newFeatures = filterBySeq(newFeatures, chrFilter);	// make sure we filter out other chromosomes
						features.addAll(newFeatures);
					}
				}
			}
		}

		return features;
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
	void setStatus(final String text) {
		igbService.runOnEventQueue(new Runnable() {

			public void run() {
				status_bar.setText(text);
			}
		});
	}

	public void mapRefresh() {
		igbService.mapRefresh(glyphs);
	}

	@SuppressWarnings("serial")
	private abstract class SearchResultsTableModel extends AbstractTableModel{
		public abstract Object get(int i);
		public abstract void clear();
		public abstract int[] getColumnWidth();
		public abstract int[] getColumnAlign();
	}

	private class GlyphSearchResultsTableModel extends SearchResultsTableModel {
		private static final long serialVersionUID = 1L;
		private final int[] colWidth = {10,10,5,10,65};
		private final int[] colAlign = {SwingConstants.RIGHT,SwingConstants.RIGHT,SwingConstants.CENTER,SwingConstants.CENTER,SwingConstants.LEFT};
		
		private final List<GlyphI> tableRows = new ArrayList<GlyphI>(0);
		protected final String seq;

		public GlyphSearchResultsTableModel(List<GlyphI> results, String seq) {
			tableRows.addAll(results);
			this.seq = seq;
		}

		private final String[] column_names = {
			BUNDLE.getString("searchTableStart"),
			BUNDLE.getString("searchTableEnd"),
			BUNDLE.getString("searchTableStrand"),
			BUNDLE.getString("searchTableChromosome"),
			BUNDLE.getString("searchTableMatch")
		};

		private static final int START_COLUMN = 0;
		private static final int END_COLUMN = 1;
		private static final int STRAND_COLUMN = 2;
		private static final int CHROM_COLUMN = 3;
		private static final int MATCH_COLUMN = 4;

		@Override
		public GlyphI get(int i) {
			return tableRows.get(i);
		}

		@Override
		public void clear() {
			tableRows.clear();
		}

		public int getRowCount() {
			return tableRows.size();
		}

		public int getColumnCount() {
			return column_names.length;
		}

		public Object getValueAt(int row, int col) {
			GlyphI glyph = tableRows.get(row);
			Map map = (Map) glyph.getInfo();

			switch (col) {
			
				case START_COLUMN:
					return (int)glyph.getCoordBox().x;

				case END_COLUMN:
					return (int)(glyph.getCoordBox().x  + glyph.getCoordBox().width);

				case STRAND_COLUMN:
					Object direction = map.get("direction");
					if (direction != null) {
						if (direction.toString().equalsIgnoreCase("forward")) {
							return "+";
						} else if (direction.toString().equalsIgnoreCase("reverse")) {
							return "-";
						}
					}
					return "";

				case CHROM_COLUMN:
					return seq;
					
				case MATCH_COLUMN:
					Object match = map.get("match");
					if (match != null) {
						return match.toString();
					}
				return "";
			}

			return "";
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		@Override
		public String getColumnName(int col) {
			return column_names[col];
		}
		
		@Override
		public Class<?> getColumnClass(int column) {
			if(column == START_COLUMN || column == END_COLUMN) {
				return Number.class;
			}
			return String.class;
		}

		@Override
		public int[] getColumnWidth() {
			return colWidth;
		}

		@Override
		public int[] getColumnAlign() {
			return colAlign;
		}

	}
	private class SymSearchResultsTableModel extends SearchResultsTableModel {
		private static final long serialVersionUID = 1L;
		private final int[] colWidth = {};
		private final int[] colAlign = {};
		
		private final List<SeqSymmetry> tableRows = new ArrayList<SeqSymmetry>(0);
		
		private final String[] column_names = {
			BUNDLE.getString("searchTableID"),
			BUNDLE.getString("searchTableTier"),
			BUNDLE.getString("searchTableGeneName"),
			BUNDLE.getString("searchTableStart"),
			BUNDLE.getString("searchTableEnd"),
			BUNDLE.getString("searchTableChromosome"),
			BUNDLE.getString("searchTableStrand")
		};
		private static  final int ID_COLUMN = 0;
		private static final int TIER_COLUMN = 1;
		private static  final int GENE_NAME_COLUMN = 2;
		private static final int START_COLUMN = 3;
		private static final int END_COLUMN = 4;
		private static final int CHROM_COLUMN = 5;
		private static final int STRAND_COLUMN = 6;

		public SymSearchResultsTableModel(List<SeqSymmetry> results) {
			tableRows.addAll(results);
		}

		public Object getValueAt(int row, int col) {
			SeqSymmetry sym = tableRows.get(row);
			SeqSpan span = sym.getSpan(0);
			switch (col) {
				case ID_COLUMN:
					return sym.getID();
				case TIER_COLUMN:
					return BioSeq.determineMethod(sym);
				case GENE_NAME_COLUMN:
					if (sym instanceof SimpleDas2Feature) {
						String geneName = ((SimpleDas2Feature)sym).getName();
						return geneName == null ? "" : geneName;
					}
					if (sym instanceof SymWithProps) {
						String geneName = (String)((SymWithProps)sym).getProperty("gene name");
						return geneName == null ? "" : geneName;
					}
					return "";
				case START_COLUMN:
					if (sym instanceof UcscPslSym) {
						return (((UcscPslSym) sym).getSameOrientation()) ? 
							((UcscPslSym) sym).getTargetMin() : ((UcscPslSym) sym).getTargetMax();
					}
					return (span == null ? "" : span.getStart());
				case END_COLUMN:
					if (sym instanceof UcscPslSym) {
						return (((UcscPslSym) sym).getSameOrientation()) ?
							((UcscPslSym) sym).getTargetMax() : ((UcscPslSym) sym).getTargetMin();
					}
					return (span == null ? "" : span.getEnd());
				case CHROM_COLUMN:
					if (sym instanceof UcscPslSym) {
						return ((UcscPslSym) sym).getTargetSeq().getID();
					}
					return (span == null ? "" : span.getBioSeq().getID());
				case STRAND_COLUMN:
					if (sym instanceof UcscPslSym) {
						return (
								(((UcscPslSym) sym).getSameOrientation())
								? "+" : "-");
					}
					if (span == null) {
						return "";
					}
					return (span.isForward() ? "+" : "-");
			}
			return "";
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
			
		public int getColumnCount() {
			return column_names.length;
		}

		@Override
		public String getColumnName(int col) {
			return column_names[col];
		}

		public int getRowCount() {
			return tableRows.size();
		}

		@Override
		public Class<?> getColumnClass(int column) {
			if(column == START_COLUMN || column == END_COLUMN) {
				return Number.class;
			}
			return String.class;
		}

		@Override
		public SeqSymmetry get(int i) {
			return tableRows.get(i);
		}

		@Override
		public void clear(){
			tableRows.clear();
		}

		@Override
		public int[] getColumnWidth() {
			return colWidth;
		}

		@Override
		public int[] getColumnAlign() {
			return colAlign;
		}
	}

	@Override
	public boolean isEmbedded() {
		return true;
	}
	
	@SuppressWarnings("serial")
	private class CancelButton extends JButton implements CThreadListener, ActionListener{
		
		public CancelButton(ImageIcon icon){
			super(icon);
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
