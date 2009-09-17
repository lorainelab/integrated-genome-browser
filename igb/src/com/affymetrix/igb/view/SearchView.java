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
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public final class SearchView extends JComponent implements ActionListener, GroupSelectionListener, SeqSelectionListener, SymMapChangeListener {
	private static final long serialVersionUID = 0;
	// A maximum number of hits that can be found in a search.
	// This helps protect against out-of-memory errors.
	private final static int MAX_HITS = 100000;
	private static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
	private static AnnotatedSeqGroup group;
	private static int seqCount = 0;

	private JTextField searchTF;
	private JPanel pan1 = new JPanel();
	private JComboBox sequence_CB = new JComboBox();
	private JComboBox searchCB = new JComboBox();
	private JCheckBox remoteSearchCheckBox = new JCheckBox("search remotely");
	private JCheckBox selectInMapCheckBox = new JCheckBox("select in map");
	private JButton searchButton = new JButton("Search");
	private JButton clear_button = new JButton("Clear");
	private SeqMapView gviewer;
	private Vector<GlyphI> glyphs = new Vector<GlyphI>();
	private Color hitcolor = new Color(150, 150, 255);


	private final JTable table = new JTable();
	private final JTextField filterText = new JTextField();
	private final JLabel status_bar = new JLabel("0 results");
	// The second column in the table contains an object of type SeqSymmetry
	// but we use a special TableCellRenderer so that what is actually displayed
	// is a String representing the Tier
	private final static String[] col_headings = {"ID", "Tier", "Start", "End", "Chromosome", "Strand"};
	private final static Class<?>[] col_classes = {String.class, String.class, Integer.class, Integer.class, String.class, String.class};
	private final static Vector<String> col_headings_vector = new Vector<String>(Arrays.asList(col_headings));
	private final static int NUM_COLUMNS = col_headings_vector.size();
	private DefaultTableModel model;
	private TableRowSorter<DefaultTableModel> sorter;
	private ListSelectionModel lsm;

	private static final String ALLID = "All loaded IDs";
	private static final String REGEXID = "Matching IDs";
	private static final String REGEXIDTF = "IDs match string or Regular Expression";
	private static final String REGEXRESIDUE = "Matching residues";
	private static final String REGEXRESIDUETF = "Residues match string or Regular Expression";
	private static final String CHOOSESEARCH = "Choose search method";
	private static final String FINDANNOTS = "Find Annotations For ";
	private static final String FINDANNOTSNULL = "Please select genome before continuing";
	private static final String SEQUENCETOSEARCH = "Sequence to search";

	private static final boolean DEBUG = true;

	private class SearchRow {
		String serverID;
		String id;
		int start;
		int end;
		String chr;
		String orientation;
	}

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


		JLabel sequenceChooseLabel = new JLabel("Sequence");
		pan1.add(sequenceChooseLabel);
		sequence_CB.setToolTipText(SEQUENCETOSEARCH);
		pan1.add(sequence_CB);

		pan1.add(Box.createRigidArea(new Dimension(100, 0)));


		pan1.add(searchCB);
		pan1.add(searchTF);
		pan1.add(remoteSearchCheckBox);
		pan1.add(selectInMapCheckBox);

		pan1.add(Box.createRigidArea(new Dimension(30, 0)));

		pan1.add(searchButton);
		
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

	private void initSequenceCB() {
		// set up the sequence combo_box
		sequence_CB.removeAllItems();
		if (group != null) {
			sequence_CB.addItem(IGBConstants.GENOME_SEQ_ID);	// put this at top of list
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
		searchCB.addItem(ALLID);
		searchCB.addItem(REGEXID);
		searchCB.addItem(REGEXRESIDUE);
		searchCB.setToolTipText(CHOOSESEARCH);
	}

	private void initComponents() {
		searchTF = new JTextField(10);
		searchTF.setVisible(true);
		searchTF.setEnabled(false);
		remoteSearchCheckBox.setEnabled(false);
		remoteSearchCheckBox.setToolTipText("search remote servers for IDs");
		selectInMapCheckBox.setToolTipText("highlight matches in sequence map");
		searchButton.setEnabled(true);
	}

	private void initTable() {
		model = new DefaultTableModel() {
			private static final long serialVersionUID = 0;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}

			@Override
			public Class getColumnClass(int column) {
				return col_classes[column];
			}

			@Override
			public void fireTableStructureChanged() {
				// The columns never change, so suppress tableStructureChanged events
				// converting to normal table-rows-changed-type events.
				// This allows the column-based sorting settings to be preserved when
				// the data changes.
				fireTableChanged(new javax.swing.event.TableModelEvent(this));
			}
		};
	
		model.setDataVector(new Vector(0), col_headings_vector);

		lsm = table.getSelectionModel();
		//lsm.addListSelectionListener(list_selection_listener);
		lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		sorter = new TableRowSorter<DefaultTableModel>(model);

		table.setModel(model);
		table.setRowSelectionAllowed(true);
		table.setRowSorter(sorter);
		table.setEnabled(true);
		table.setDefaultRenderer(Integer.class, new IntegerTableCellRenderer());
		table.setDefaultRenderer(SeqSymmetry.class, new SeqSymmetryTableCellRenderer());
	}

	/**
     * Update the row filter regular expression from the expression in
     * the text box.
     */
    private void newFilter() {
        RowFilter<DefaultTableModel, Object> rf = null;
        //If current expression doesn't parse, don't update.
        try {
            rf = RowFilter.regexFilter(filterText.getText(), 0);
        } catch (java.util.regex.PatternSyntaxException e) {
            return;
        }
        sorter.setRowFilter(rf);
    }


	private void clearAll() {
		searchTF.setText("");
		clearResults();
		NeoMap map = gviewer.getSeqMap();
		map.updateWidget();
	}

	private static Vector<Vector<Object>> buildRows(List<SeqSymmetry> results, BioSeq seq) {

		if (results == null || results.isEmpty()) {
			return new Vector<Vector<Object>>(0);
		}

		int num_rows = results.size();

		Vector<Vector<Object>> rows = new Vector<Vector<Object>>(num_rows, num_rows / 10);
		for (int j = 0; j < num_rows && rows.size() < MAX_HITS; j++) {
			Vector<Object> a_row = new Vector<Object>(NUM_COLUMNS);
			SeqSymmetry result = results.get(j);
			if (!convertSymmetryToRow(result, j, seq, a_row)) {
				continue ;
			}
			rows.add(a_row);
		}

		return rows;
	}


	private static boolean convertSymmetryToRow(SeqSymmetry result, int j, BioSeq seq, Vector<Object> a_row) {
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
			return false;
		}
		// TODO: use SearchRow class
		a_row.add(result.getID());	// ID
		a_row.add(BioSeq.determineMethod(result));	// tier
		if (result instanceof UcscPslSym) {
			a_row.add(((UcscPslSym) result).getTargetMin());
			a_row.add(((UcscPslSym) result).getTargetMax());
			a_row.add(((UcscPslSym) result).getTargetSeq().getID());
		} else {
			a_row.add(new Integer(span.getStart()));
			a_row.add(new Integer(span.getEnd()));
			if (seq != null) {
				a_row.add(seq.getID());
			} else {
				a_row.add(span.getBioSeq().getID());
			}
		}
		
		a_row.add(span.isForward() ? "+" : "-");
		return true;
	}

	private void displayInTable(final Vector<Vector<Object>> rows) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				model.setDataVector(rows, col_headings_vector);
			}
		});
	}

	// Clear the table (using invokeLater)
	private void clearTable() {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				model.setDataVector(new Vector(0), col_headings_vector);
			}
		});
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
			clearAll();
			String searchMode = (String) this.searchCB.getSelectedItem();
			this.searchTF.setEnabled(!ALLID.equals(searchMode));

			boolean remoteEnabled = REGEXID.equals(searchMode);
			this.remoteSearchCheckBox.setEnabled(remoteEnabled);
			if (!remoteEnabled) {
				this.remoteSearchCheckBox.setSelected(false);
			}

			boolean displaySelectedEnabled = !REGEXRESIDUE.equals(searchMode);
			this.selectInMapCheckBox.setEnabled(displaySelectedEnabled);
			if (!displaySelectedEnabled) {
				this.selectInMapCheckBox.setSelected(false);
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
		if (src == this.searchTF) {
			return;
		}
		if (src == this.searchButton) {
			String searchMode = (String) this.searchCB.getSelectedItem();
			String chrStr = (String) this.sequence_CB.getSelectedItem();
			BioSeq chrfilter = IGBConstants.GENOME_SEQ_ID.equals(chrStr) ? null : group.getSeq(chrStr);
			if (ALLID.equals(searchMode)) {
				displayRegexIDs(".*", chrfilter);
			} else if (REGEXID.equals(searchMode)) {
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
		List<SeqSymmetry> sym_list = group.findSyms(regex);
		List<SeqSymmetry> remoteSymList = null;

		if (this.remoteSearchCheckBox.isSelected()) {
			if (text.length() < 3) {
				status_bar.setText(friendlySearchStr + ": Text is too short for remote search...");
			} else {
				status_bar.setText(friendlySearchStr + ": Searching remotely...");
				remoteSymList = remoteSearchFeaturesByName(group, text, chrFilter);
			}
		}

		if (sym_list == null && remoteSymList == null) {
			setStatus(friendlySearchStr + ": No matches");
			return;
		}

		String statusStr = friendlySearchStr + ": " + (sym_list == null ? 0 : sym_list.size()) + " local matches";
		if (this.remoteSearchCheckBox.isSelected() && text.length() >= 3) {
				statusStr += ", " + (remoteSymList == null ? 0 : remoteSymList.size()) + " remote matches";
		}
		setStatus(statusStr);
		if (this.selectInMapCheckBox.isSelected()) {
			gmodel.setSelectedSymmetriesAndSeq(sym_list, this);
		}
		if (remoteSymList != null) {
			sym_list.addAll(remoteSymList);
		}

		final Vector<Vector<Object>> rows = buildRows(sym_list, chrFilter);
		displayInTable(rows);

	}

	/**
	 * Display (highlight on SeqMap) the residues matching the specified regex.
	 */
	private void displayRegexResidues() {
		MutableAnnotatedBioSeq vseq = gviewer.getViewSeq();
		if (vseq == null || !vseq.isComplete()) {
			Application.errorPanel("Residues for seq not available, search aborted");
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
		int hit_count1 = searchForRegexInResidues(true, regex, residues, residue_offset, seq_glyph, axis_tier, 0);

		// Search for reverse complement of query string
		//   flip searchstring around, and redo nibseq search...
		String rev_searchstring = DNAUtils.reverseComplement(residues);
		int hit_count2 = searchForRegexInResidues(false, regex, rev_searchstring, residue_offset, seq_glyph, axis_tier, 0);

		setStatus(friendlySearchStr + ": " + hit_count1 + " forward strand hits and " + hit_count2 + " reverse strand hits");
		NeoMap map = gviewer.getSeqMap();
		map.updateWidget();
	}

	private int searchForRegexInResidues(boolean forward, Pattern regex, String residues, int residue_offset, GlyphI seq_glyph, TransformTierGlyph axis_tier, int hit_count) {
		Matcher matcher = regex.matcher(residues);
		while (matcher.find()) {
			int start = matcher.start(0) + residue_offset;
			int end = matcher.end(0) + residue_offset;
			GlyphI gl = new FillRectGlyph();
			gl.setColor(hitcolor);
			if (seq_glyph != null) {
				double pos = seq_glyph.getCoordBox().y + (forward ? 0 : 5);
				gl.setCoords(start, pos, end - start, seq_glyph.getCoordBox().height);
				seq_glyph.addChild(gl);
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
		String searchMode = (String) this.searchCB.getSelectedItem();
		this.searchTF.setEnabled(newGroup != null && !ALLID.equals(searchMode));

		// only re-initialize the combobox if the group or seqs have changed
		if (newGroup != group || seqCount != newSeqCount) {
			group = newGroup;
			seqCount = newSeqCount;
			this.initSequenceCB();
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
}
