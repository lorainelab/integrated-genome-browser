package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.AbstractResiduesGlyph;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.util.DNAUtils;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.tiers.TransformTierGlyph;

import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.SymMapChangeEvent;
import com.affymetrix.genometryImpl.event.SymMapChangeListener;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.igb.das2.Das2VersionedSource;
import com.affymetrix.swing.IntegerTableCellRenderer;
import java.awt.Dimension;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

public final class SearchView extends JComponent implements ActionListener, GroupSelectionListener, SeqSelectionListener, SymMapChangeListener {
	private static final long serialVersionUID = 0;
	// A maximum number of hits that can be found in a search.
	// This helps protect against out-of-memory errors.
	private final static int MAX_HITS = 100000;
	private static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
	private static AnnotatedSeqGroup group;
	private static int seqCount = 0;

	private static final String SEARCHLABELTEXT = "Search ";
	private static final String INLABELTEXT = "in ";
	private static final String FORLABELTEXT = "for ";
	private static final String REGEXID = "Matching IDs";
	private static final String REGEXIDTF = "IDs match string or Regular Expression";
	private static final String REGEXRESIDUE = "Matching residues";
	private static final String REGEXRESIDUETF = "Residues match string or Regular Expression";
	private static final String CHOOSESEARCH = "Choose search method";
	private static final String FINDANNOTS = "Find Annotations For ";
	private static final String FINDANNOTSNULL = "Please select genome before continuing";
	private static final String SEQUENCETOSEARCH = "Sequence to search";
	private static final String REMOTESERVERSEARCH1 = "also search remotely (";
	private static final String REMOTESERVERSEARCH2 = " server)";
	private static final String REMOTESERVERSEARCH2PLURAL = " servers)";
	private static final String REMOTESERVERSEARCH3 = " for IDs";

	private static final String SELECTINMAP_TEXT = "select in map";
	private static final String SELECTINMAP_TIP = "highlight matches in sequence map";

	private JTextField searchTF;
	private JPanel pan1 = new JPanel();
	private JComboBox sequence_CB = new JComboBox();
	private JComboBox searchCB = new JComboBox();
	private JCheckBox remoteSearchCheckBox = new JCheckBox("");
	private JCheckBox selectInMapCheckBox = new JCheckBox(SELECTINMAP_TEXT);
	private JButton searchButton = new JButton("Search");
	private JButton clear_button = new JButton("Clear");
	private SeqMapView gviewer;
	private Vector<GlyphI> glyphs = new Vector<GlyphI>();
	private Color hitcolor = new Color(150, 150, 255);

	private final JTable table = new JTable();
	private final JTextField filterText = new JTextField();
	private final JLabel status_bar = new JLabel("0 results");
	private SearchResultsTableModel model;
	private TableRowSorter<SearchResultsTableModel> sorter;
	private ListSelectionModel lsm;

	private List<SeqSymmetry> tableRows = new ArrayList<SeqSymmetry>(0);

	private List<SeqSymmetry> localSymList;
	private List<SeqSymmetry> remoteSymList;

	private static final boolean DEBUG = true;

	public SearchView() {
		super();
		gviewer = Application.getSingleton().getMapView();
		gmodel.addGroupSelectionListener(this);
		group = gmodel.getSelectedSeqGroup();

		this.setLayout(new BorderLayout());

		initSearchCB();

		initComponents();
		String annotsStr = (group == null) ? FINDANNOTSNULL : (FINDANNOTS + group.getID());
		pan1.setBorder(BorderFactory.createTitledBorder(annotsStr));
		pan1.setLayout(new BoxLayout(pan1, BoxLayout.X_AXIS));

		pan1.add(new JLabel(SearchView.SEARCHLABELTEXT));
		pan1.add(searchCB);

		pan1.add(Box.createRigidArea(new Dimension(50, 0)));
		pan1.add(new JLabel(SearchView.INLABELTEXT));
		sequence_CB.setToolTipText(SEQUENCETOSEARCH);
		pan1.add(sequence_CB);

		pan1.add(Box.createRigidArea(new Dimension(50, 0)));
		pan1.add(new JLabel(SearchView.FORLABELTEXT));
		pan1.add(searchTF);
		
		pan1.add(Box.createRigidArea(new Dimension(50, 0)));

		pan1.add(searchButton);
		
		pan1.add(Box.createRigidArea(new Dimension(30, 0)));

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

		bottom_row.add(status_bar, BorderLayout.CENTER);
		validate();

		AnnotatedSeqGroup.addSymMapChangeListener(this);
		gmodel.addGroupSelectionListener(this);
		gmodel.addSeqSelectionListener(this);
		searchCB.addActionListener(this);
		searchTF.addActionListener(this);
		searchButton.addActionListener(this);
		clear_button.addActionListener(this);
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
		// set up the sequence combo_box
		sequence_CB.removeAllItems();
		if (group != null) {
			if (!((String)this.searchCB.getSelectedItem()).equals(REGEXRESIDUE)) {
				sequence_CB.addItem(IGBConstants.GENOME_SEQ_ID); // put this at top of list
			}
			for (BioSeq seq : group.getSeqList()) {
				if (seq.getID().equals(IGBConstants.GENOME_SEQ_ID)) {
					continue;
				}
				sequence_CB.addItem(seq.getID());
			}
			sequence_CB.setToolTipText(SEQUENCETOSEARCH);
			sequence_CB.setEnabled(true);
		} else {
			sequence_CB.setToolTipText("Genome has not been selected");
			sequence_CB.setEnabled(false);
		}

		sequence_CB.setSelectedItem(IGBConstants.GENOME_SEQ_ID);
	}

	private void initSearchCB() {
		searchCB.removeAllItems();
		searchCB.addItem(REGEXID);
		searchCB.addItem(REGEXRESIDUE);
		searchCB.setToolTipText(CHOOSESEARCH);
	}

	private void initComponents() {
		searchTF = new JTextField(10);
		searchTF.setVisible(true);
		searchTF.setEnabled(true);
		
		initRemoteServerCheckBox(null);

		selectInMapCheckBox.setToolTipText(SELECTINMAP_TIP);
		selectInMapCheckBox.setEnabled(true);
		searchButton.setEnabled(true);
	}

	private void initTable() {
		model = new SearchResultsTableModel(tableRows);
		
		lsm = table.getSelectionModel();
		lsm.addListSelectionListener(list_selection_listener);
		lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		sorter = new TableRowSorter<SearchResultsTableModel>(model);

		table.setModel(model);
		table.setRowSelectionAllowed(true);
		table.setRowSorter(sorter);
		table.setEnabled(true);
		table.setDefaultRenderer(Integer.class, new IntegerTableCellRenderer());
		//table.setDefaultRenderer(SeqSymmetry.class, new SeqSymmetryTableCellRenderer());
	}

	/** This is called when the user selects a row of the table. */
	ListSelectionListener list_selection_listener = new ListSelectionListener() {

		public void valueChanged(ListSelectionEvent evt) {
			if (evt.getSource() == lsm && !evt.getValueIsAdjusting() && model.getRowCount() > 0) {
				int srow = table.getSelectedRow();
				if (srow < 0) {
					return;
				}
				SeqSymmetry sym = tableRows.get(srow);
				if (sym != null) {
					if (remoteSymList != null && remoteSymList.contains(sym)) {
						if (group == null) {
							return;
						}
						// remote symmetry.  We must zoom to its coordinate and select its seq.
						String seqID = sym.getSpanSeq(0).getID();
						BioSeq seq = group.getSeq(seqID);
						if (seq != null) {
							SeqSpan span = sym.getSpan(0);
							if (span != null) {
								// zoom to its coordinates
								MapRangeBox.zoomToSeqAndSpan(seqID, span.getStart(), span.getEnd());
							}
						}

						return;
					}

					// Set selected symmetry normally
					List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>(1);
					syms.add(sym);
					gmodel.setSelectedSymmetriesAndSeq(syms, this);
				}
			}
		}
	};

	/**
     * Update the row filter regular expression from the expression in
     * the text box.
     */
    private void newFilter() {
        RowFilter<SearchResultsTableModel, Object> rf = null;
        //If current expression doesn't parse, don't update.
        try {
            rf = RowFilter.regexFilter(filterText.getText(), 0);
        } catch (java.util.regex.PatternSyntaxException e) {
            return;
        }
        sorter.setRowFilter(rf);
    }


	private void displayInTable(List<SeqSymmetry> rows) {
		model.fireTableDataChanged();
	}

	private void clearTable() {
		tableRows.clear();
		model.fireTableDataChanged();
	}

	// remove the previous search results from the map.
	private void clearResults() {
		if (!glyphs.isEmpty()) {
			gviewer.setAnnotatedSeq(gviewer.getAnnotatedSeq(), true, true);
		}
		glyphs.clear();

		clearTable();
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		if (src == this.searchCB) {
			clearResults();
			NeoMap map = gviewer.getSeqMap();
			map.updateWidget();

			String searchMode = (String) this.searchCB.getSelectedItem();
			this.initSequenceCB();
			this.searchTF.setEnabled(true);

			boolean remoteEnabled = REGEXID.equals(searchMode);
			this.remoteSearchCheckBox.setEnabled(remoteEnabled);
			if (!remoteEnabled) {
				this.remoteSearchCheckBox.setSelected(false);
			}

			boolean displaySelectedEnabled = !REGEXRESIDUE.equals(searchMode);
			this.selectInMapCheckBox.setEnabled(displaySelectedEnabled);
			if (!displaySelectedEnabled) {
				this.selectInMapCheckBox.setSelected(true);	// we ALWAYS display in map if it's residues.
			}

			if (REGEXID.equals(searchMode)) {
				this.searchTF.setToolTipText(REGEXIDTF);
			} else if (REGEXRESIDUE.equals(searchMode)) {
				this.searchTF.setToolTipText(REGEXRESIDUETF);
			} else {
				this.searchTF.setToolTipText("");
			}

			return;
		}
		if (src == this.searchTF || src == this.searchButton) {
			String searchMode = (String) this.searchCB.getSelectedItem();
			String chrStr = (String) this.sequence_CB.getSelectedItem();
			BioSeq chrfilter = IGBConstants.GENOME_SEQ_ID.equals(chrStr) ? null : group.getSeq(chrStr);
			if (REGEXID.equals(searchMode)) {
				displayRegexIDs(this.searchTF.getText(), chrfilter);
			} else if (REGEXRESIDUE.equals(searchMode)) {
				displayRegexResidues();
			}
		}
	}

	private void displayRegexIDs(String text, BioSeq chrFilter) {
		if (!(text.contains("*") || text.contains("^") || text.contains("$"))) {
			// Not much of a regular expression.  Assume the user wants to match at the start and end
			text = ".*" + text + ".*";
		}
		Pattern regex = null;
		try {
			regex = Pattern.compile(text, Pattern.CASE_INSENSITIVE);
		} catch (PatternSyntaxException pse) {
			Application.errorPanel("Regular expression syntax error...\n" + pse.getMessage());
		} catch (Exception ex) {
			Application.errorPanel("Problem with regular expression...", ex);
			return;
		}

		clearResults();

		String friendlySearchStr = friendlyString(text, this.sequence_CB.getSelectedItem().toString());
		status_bar.setText(friendlySearchStr + ": Searching locally...");

		// Local symmetries
		localSymList = group.findSyms(regex);
		remoteSymList = null;

		// Make sure this search is reasonable to do on a remote server.
		int actualChars = text.length();
		if (text.startsWith(".*")) {
			actualChars -= 2;
		} else if (text.startsWith("*")) {
			actualChars -= 1;
		}
		if (text.endsWith(".*")) {
			actualChars -= 2;
		} else if (text.endsWith("*")) {
			actualChars -= 1;
		}

		if (this.remoteSearchCheckBox.isSelected()) {
			if (actualChars < 3) {
				Application.errorPanel(friendlySearchStr + ": Text is too short to allow remote search.");
				return;
			}

			status_bar.setText(friendlySearchStr + ": Searching remotely...");
			remoteSymList = remoteSearchFeaturesByName(group, text, chrFilter);
		}

		if (localSymList == null && remoteSymList == null) {
			setStatus(friendlySearchStr + ": No matches");
			return;
		}

		String statusStr = friendlySearchStr + ": " + (localSymList == null ? 0 : localSymList.size()) + " local matches";
		if (this.remoteSearchCheckBox.isSelected() && actualChars >= 3) {
				statusStr += ", " + (remoteSymList == null ? 0 : remoteSymList.size()) + " remote matches";
		}
		setStatus(statusStr);
		if (this.selectInMapCheckBox.isSelected()) {
			gmodel.setSelectedSymmetriesAndSeq(localSymList, this);
		}
		if (remoteSymList != null) {
			localSymList.addAll(remoteSymList);
		}

		tableRows = filterRows(localSymList, chrFilter);
		displayInTable(tableRows);

	}


	private static List<SeqSymmetry> filterRows(List<SeqSymmetry> results, BioSeq seq) {

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
	private void displayRegexResidues() {
		BioSeq vseq = gviewer.getViewSeq();
		if (vseq == null || !vseq.isComplete()) {
			Application.errorPanel(
					"Residues for " + this.sequence_CB.getSelectedItem().toString() + " not available.  Please load residues before searching.");
			return;
		}
		regexTF((BioSeq) vseq);
	}

	private static GlyphI findSeqGlyph(TransformTierGlyph axis_tier) {
		// find the sequence glyph on axis tier.
		for (GlyphI seq_glyph : axis_tier.getChildren()) {
			if (seq_glyph instanceof AbstractResiduesGlyph) {
				return seq_glyph;
			}
		}
		return null;
	}

	private void regexTF(BioSeq vseq) {
		String searchStr = searchTF.getText();
		String friendlySearchStr = friendlyString(searchStr, vseq.getID());
		if (searchStr.length() < 3) {
			Application.errorPanel("Search must contain at least 3 characters");
			return;
		}
		Pattern regex = null;
		try {
			regex = Pattern.compile(searchStr, Pattern.CASE_INSENSITIVE);
		} catch (PatternSyntaxException pse) {
			Application.errorPanel("Regular expression syntax error...\n" + pse.getMessage());
		} catch (Exception ex) {
			Application.errorPanel("Problem with regular expression...", ex);
			return;
		}

		clearResults();
		status_bar.setText(friendlySearchStr + ": Working...");

		String residues = vseq.getResidues();
		TransformTierGlyph axis_tier = gviewer.getAxisTier();
		GlyphI seq_glyph = findSeqGlyph(axis_tier);
		int residue_offset = vseq.getMin();
		int hit_count1 = searchForRegexInResidues(true, regex, residues, residue_offset, seq_glyph, axis_tier);

		// Search for reverse complement of query string
		//   flip searchstring around, and redo nibseq search...
		String rev_searchstring = DNAUtils.reverseComplement(residues);
		residue_offset = vseq.getMax();
		int hit_count2 = searchForRegexInResidues(false, regex, rev_searchstring, residue_offset, seq_glyph, axis_tier);

		setStatus(friendlySearchStr + ": " + hit_count1 + " forward strand hits and " + hit_count2 + " reverse strand hits");
		NeoMap map = gviewer.getSeqMap();
		map.updateWidget();
	}

	private int searchForRegexInResidues(boolean forward, Pattern regex, String residues, int residue_offset, GlyphI seq_glyph, TransformTierGlyph axis_tier) {
		int hit_count = 0;
		Matcher matcher = regex.matcher(residues);
		while (matcher.find()) {
			int start = residue_offset + (forward ? matcher.start(0) : -matcher.end(0));
			int end = residue_offset + (forward ? matcher.end(0) : -matcher.start(0));
			//int end = matcher.end(0) + residue_offset;
			GlyphI gl = new FillRectGlyph();
			gl.setColor(hitcolor);
			if (seq_glyph != null) {
				double offset = forward ? 0 : seq_glyph.getCoordBox().height / 2;

				gl.setCoords(start, seq_glyph.getCoordBox().y, end - start, seq_glyph.getCoordBox().height);
				seq_glyph.addChild(gl);

				// when adding as a child of the CharSeqGlyph, it automatically gets re-positioned, so we move it back where we want it
				gl.setCoords(start, seq_glyph.getCoordBox().y + offset, end - start, seq_glyph.getCoordBox().height / 2);
			} else {
				double pos = forward ? 10 : 15;
				gl.setCoords(start, pos, end - start, 10);
				axis_tier.addChild(gl);
			}
			glyphs.add(gl);
			hit_count++;
		}
		return hit_count;
	}

	private static String friendlyString(String text, String chr) {
		return "Search for " + text + " on " + chr;
	}

	private static int getRemoteServerCount(AnnotatedSeqGroup group) {
		if (group == null) {
			return 0;
		}
		int count = 0;
		for (GenericVersion gVersion : group.getVersions()) {
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

		for (GenericVersion gVersion : group.getVersions()) {
			if (gVersion.gServer.serverType == ServerType.DAS2) {
				Das2VersionedSource version = (Das2VersionedSource) gVersion.versionSourceObj;
				if (version != null) {
					List<SeqSymmetry> newFeatures = version.getFeaturesByName(name, group, chrFilter);
					if (newFeatures != null) {
						features.addAll(newFeatures);
					}
				}
			}
		}

		if (DEBUG) {
			System.out.println("features found: " + features.size());
		}

		return features;
	}

	public void groupSelectionChanged(GroupSelectionEvent evt) {
		groupOrSeqChange();
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

		// only re-initialize the combobox if the group or seqs have changed
		if (newGroup != group || seqCount != newSeqCount) {
			group = newGroup;
			seqCount = newSeqCount;
			this.initSequenceCB();
			initRemoteServerCheckBox(group);

		}
	}

	/** Causes a call to {@link #setStatus(String)}.
	 * }
	 *  Normally, this occurs as a result of a call to
	 *  {@link AnnotatedSeqGroup#symHashChanged(Object)}.
	 */
	public void symMapModified(SymMapChangeEvent evt) {
		//showSymHash(evt.getSeqGroup());
		setStatus("Data modified, search again");
	}

	/** Set the text in the status bar in a thread-safe way. */
	void setStatus(final String text) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				status_bar.setText(text);
			}
		});
	}

	/** A renderer that displays the value of {@link SeqMapView#determineMethod(SeqSymmetry)}. */
	private static class SeqSymmetryTableCellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 0;
		public SeqSymmetryTableCellRenderer() {
			super();
		}

		@Override
		protected void setValue(Object value) {
			SeqSymmetry sym = (SeqSymmetry) value;
			super.setValue(BioSeq.determineMethod(sym));
		}
	}

	/** A Comparator that compares based on {@link BioSeq#determineMethod(SeqSymmetry)}. */
	private static class SeqSymmetryMethodComparator implements Comparator<SeqSymmetry> {

		public int compare(SeqSymmetry s1, SeqSymmetry s2) {
			return BioSeq.determineMethod(s1).compareTo(BioSeq.determineMethod(s2));
		}
	}

	private class SearchResultsTableModel extends AbstractTableModel {

		private final String[] column_names = {"ID", "Tier", "Start", "End", "Chromosome", "Strand"};
		private final int ID_COLUMN = 0;
		private final int TIER_COLUMN = 1;
		private final int START_COLUMN = 2;
		private final int END_COLUMN = 3;
		private final int CHROM_COLUMN = 4;
		private final int STRAND_COLUMN = 5;
		private List<SeqSymmetry> search_results;

		public SearchResultsTableModel(List<SeqSymmetry> results) {
			search_results = results;
		}

		public Object getValueAt(int row, int col) {
			SeqSymmetry sym = tableRows.get(row);
			SeqSpan span = sym.getSpan(0);
			switch (col) {
				case ID_COLUMN:
					return sym.getID();
				case TIER_COLUMN:
					return BioSeq.determineMethod(sym);
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


	}
}
