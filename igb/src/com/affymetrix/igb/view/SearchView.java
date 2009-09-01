package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.AbstractResiduesGlyph;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.util.Timer;
import com.affymetrix.genoviz.util.DNAUtils;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.tiers.TransformTierGlyph;

import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.SymMapChangeEvent;
import com.affymetrix.genometryImpl.event.SymMapChangeListener;
import com.affymetrix.igb.util.TableSorter2;
import com.affymetrix.igb.view.AnnotBrowserView.SeqSymmetryMethodComparator;
import com.affymetrix.igb.view.AnnotBrowserView.SeqSymmetryTableCellRenderer;
import com.affymetrix.swing.IntegerTableCellRenderer;
import java.awt.Dimension;
import javax.swing.table.DefaultTableModel;

public final class SearchView extends JComponent implements ActionListener, GroupSelectionListener, SeqSelectionListener, SymMapChangeListener {
	private static final long serialVersionUID = 0;
	// A maximum number of hits that can be found in a search.
	// This helps protect against out-of-memory errors.
	private final static int MAX_HITS = 100000;
	private static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
	private static AnnotatedSeqGroup group;
	private JLabel hitCountL;
	private JLabel andLabel;
	private JTextField searchTF;
	private JTextField searchTF2;
	private JPanel pan1 = new JPanel();
	private JComboBox sequence_CB = new JComboBox();
	private JComboBox searchCB = new JComboBox();
	private JCheckBox remoteSearchCheckBox = new JCheckBox("search DAS/2 servers");
	private JButton clear_button = new JButton("Clear");
	private SeqMapView gviewer;
	private Vector<GlyphI> glyphs = new Vector<GlyphI>();
	private Color hitcolor = new Color(150, 150, 255);


	private final JTable table = new JTable();
	private final JLabel status_bar = new JLabel("0 results");
	// The second column in the table contains an object of type SeqSymmetry
	// but we use a special TableCellRenderer so that what is actually displayed
	// is a String representing the Tier
	private final static String[] col_headings = {"ID", "Tier", "Start", "End", "Sequence"};
	private final static Class<?>[] col_classes = {String.class, SeqSymmetry.class, Integer.class, Integer.class, String.class};
	private final static Vector<String> col_headings_vector = new Vector<String>(Arrays.asList(col_headings));
	private DefaultTableModel model;
	private ListSelectionModel lsm;

	private static final String ALLID = "All loaded IDs";
	private static final String REGEXID = "Matching ID";
	private static final String REGEXIDTF = "ID matches string or Regular Expression";
	private static final String BETWEENID = "ID between...";
	private static final String REGEXRESIDUE = "Matching residues";
	private static final String REGEXRESIDUETF = "Residues match string or Regular Expression";
	private static final String CHOOSESEARCH = "Choose search method";
	private static final String FINDANNOTS = "Find Annotations For ";
	private static final String FINDANNOTSBY = " By...";
	private static final String FINDANNOTSNULL = "Please select genome before continuing";
	private static final String SEQUENCETOSEARCH = "Sequence to search";

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

		pan1.add(Box.createRigidArea(new Dimension(200, 0)));


		pan1.add(searchCB);
		pan1.add(searchTF);
		pan1.add(andLabel);
		pan1.add(searchTF2);
		pan1.add(remoteSearchCheckBox);
		if (group == null) {
			searchCB.setEnabled(false);
			searchTF.setEnabled(false);
		}

		
		this.initSequenceCB();

		this.initTable();
		

		this.add("North", pan1);

		JPanel pan2 = new JPanel();
		hitCountL = new JLabel(" No hits");

		pan2.add(hitCountL);
		pan2.add(this.table);
		this.add("Center", pan2);// a blank panel: improves appearance in JDK1.5


		JScrollPane scroll_pane = new JScrollPane(table);
		this.add(scroll_pane, BorderLayout.CENTER);

		Box bottom_row = Box.createHorizontalBox();
		this.add(bottom_row, BorderLayout.SOUTH);

		bottom_row.add(status_bar);
		validate();

		AnnotatedSeqGroup.addSymMapChangeListener(this);
		gmodel.addGroupSelectionListener(this);
		gmodel.addSeqSelectionListener(this);
		searchCB.addActionListener(this);
		searchTF.addActionListener(this);
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
			//sequence_CB.setToolTipText("Genome: " + group.getID());
			sequence_CB.setToolTipText(SEQUENCETOSEARCH);
			sequence_CB.setEnabled(true);
		} else {
			sequence_CB.setToolTipText("Genome has not been selected");
			sequence_CB.setEnabled(false);
		}

		MutableAnnotatedBioSeq selected_seq = gmodel.getSelectedSeq();
		if (selected_seq != null) {
			sequence_CB.setSelectedItem(selected_seq.getID());
		}

	}

	private void initSearchCB() {
		searchCB.removeAllItems();
		searchCB.addItem(ALLID);
		searchCB.addItem(REGEXID);
		searchCB.addItem(BETWEENID);
		searchCB.addItem(REGEXRESIDUE);
		searchCB.setToolTipText(CHOOSESEARCH);
	}

	private void initComponents() {
		searchTF = new JTextField(10);
		searchTF.setVisible(false);
		searchTF2 = new JTextField(10);
		searchTF2.setVisible(false);
		andLabel = new JLabel("and");
		andLabel.setVisible(false);
		remoteSearchCheckBox.setEnabled(false);
		remoteSearchCheckBox.setToolTipText("search remote servers for IDs");
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

		TableSorter2 sort_model = new TableSorter2(model);
		//sort_model.addMouseListenerToHeaderInTable(table); // for TableSorter version 1
		sort_model.setTableHeader(table.getTableHeader()); // for TableSorter2
		sort_model.setColumnComparator(SeqSymmetry.class, new SeqSymmetryMethodComparator());

		table.setModel(sort_model);
		table.setRowSelectionAllowed(true);
		table.setEnabled(true);
		table.setDefaultRenderer(Integer.class, new IntegerTableCellRenderer());
		table.setDefaultRenderer(SeqSymmetry.class, new SeqSymmetryTableCellRenderer());

	}

	private void clearAll() {
		searchTF.setText("");
		hitCountL.setText(" No hits");
		clearResults();
		NeoMap map = gviewer.getSeqMap();
		map.updateWidget();
	}

	// remove the previous search results from the map.
	private void clearResults() {
		if (!glyphs.isEmpty()) {
			gviewer.setAnnotatedSeq(gviewer.getAnnotatedSeq(), true, true);
		}
		glyphs.clear();
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		if (src == this.searchCB) {
			clearAll();
			String searchMode = (String) this.searchCB.getSelectedItem();
			this.searchTF.setVisible(!ALLID.equals(searchMode));

			boolean optionalField = BETWEENID.equals(searchMode);
			this.andLabel.setVisible(optionalField);
			this.searchTF2.setVisible(optionalField);

			boolean remoteEnabled = REGEXID.equals(searchMode);
			this.remoteSearchCheckBox.setEnabled(remoteEnabled);

			if (REGEXID.equals(searchMode)) {
				this.searchTF.setToolTipText(REGEXIDTF);
			} else if (REGEXRESIDUE.equals(searchMode)) {
				this.searchTF.setToolTipText(REGEXRESIDUETF);
			} else {
				this.searchTF.setToolTipText("");
			}

			return;
		}
		if (src == this.searchTF || src == this.searchTF2) {
			String searchMode = (String) this.searchCB.getSelectedItem();
			if (ALLID.equals(searchMode)) {
				displayAllIDs();
			} else if (REGEXID.equals(searchMode)) {
				displayRegexIDs(this.searchTF.getText());
			} else if (BETWEENID.equals(searchMode)) {
				displayBetweenIDs(this.searchTF.getText(), this.searchTF2.getText());
			} else if (REGEXRESIDUE.equals(searchMode)) {
				displayRegexResidues();
			}
		}
	}

	private static final void displayAllIDs() {
		Set<String> results = group.getSymmetryIDs();
	}

	private void displayRegexIDs(String text) {
		// Local symmetries
		List<SeqSymmetry> sym_list = group.findSyms(text);

		//List<SeqSymmetry> remoteSymList =

		// Display them instead
		if (sym_list != null && !sym_list.isEmpty()) {
			hitCountL.setText(sym_list.size() + " matches found");
			gmodel.setSelectedSymmetriesAndSeq(sym_list, this);
		} else {
			hitCountL.setText(" no matches");
		}
	}

	private static void displayBetweenIDs(String text, String text2) {
		Set<String> results = group.getSymmetryIDs(text, text2);
	}


	private void displayRegexResidues() {
		clearResults();
		Timer tim = new Timer();
		NeoMap map = gviewer.getSeqMap();
		MutableAnnotatedBioSeq vseq = gviewer.getViewSeq();
		if (vseq == null || !vseq.isComplete()) {
			hitCountL.setText(" No hits");
			Application.errorPanel("Residues for seq not available, search aborted");
			return;
		}
		int residue_offset = ((BioSeq) vseq).getMin();
		TransformTierGlyph axis_tier = gviewer.getAxisTier();
		GlyphI seq_glyph = findSeqGlyph(axis_tier);
		regexTF(tim, (BioSeq) vseq, residue_offset, seq_glyph, axis_tier, map);
	}


	private static final GlyphI findSeqGlyph(TransformTierGlyph axis_tier) {
		// find the sequence glyph on axis tier.
		for (GlyphI seq_glyph : axis_tier.getChildren()) {
			if (seq_glyph instanceof AbstractResiduesGlyph) {
				return seq_glyph;
			}
		}
		return null;
	}

	private final void regexTF(Timer tim, BioSeq vseq, int residue_offset, GlyphI seq_glyph, TransformTierGlyph axis_tier, NeoMap map) {
		if (searchTF.getText().length() == 0) {
			hitCountL.setText(" No hits");
			return;
		}
		hitCountL.setText(" Working...");
		tim.start();
		String residues = vseq.getResidues();
		try {

			String str = searchTF.getText();
			if (str.length() < 3) {
				Application.errorPanel("Regular expression must contain at least 3 characters");
				return;
			}
			// It is possible to add the flag Pattern.CASE_INSENSITIVE, but that
			// makes the search slower.  Better to let the user decide whether
			// to add the "(?i)" flag for themselves
			Pattern regex = Pattern.compile(str);
			int hit_count1 = searchForRegexInResidues(true, regex, residues, residue_offset, seq_glyph, axis_tier, 0);

			// Search for reverse complement of query string
			//   flip searchstring around, and redo nibseq search...
			String rev_searchstring = DNAUtils.reverseComplement(residues);
			int hit_count2 = searchForRegexInResidues(false, regex, rev_searchstring, residue_offset, seq_glyph, axis_tier, 0);

			float match_time = tim.read() / 1000f;
			System.out.println("time to run search: " + match_time);
			hitCountL.setText(" " + hit_count1 + " forward strand hits and " + hit_count2 + " reverse strand hits");
			map.updateWidget();
		} catch (PatternSyntaxException pse) {
			Application.errorPanel("Regular expression syntax error...\n" + pse.getMessage());
		} catch (Exception ex) {
			Application.errorPanel("Problem with regular expression...", ex);
		}
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

	public void groupSelectionChanged(GroupSelectionEvent evt) {
		groupOrSeqChange();
	}
	public void seqSelectionChanged(SeqSelectionEvent evt) {
		groupOrSeqChange();
	}

	private void groupOrSeqChange() {
		group = gmodel.getSelectedSeqGroup();
		String annotsStr = (group == null) ? FINDANNOTSNULL : (FINDANNOTS + group.getID());
		pan1.setBorder(BorderFactory.createTitledBorder(annotsStr));
		this.searchCB.setEnabled(group != null);
		this.searchTF.setEnabled(group != null);
		this.initSequenceCB();
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
}
