package com.affymetrix.igb.view;

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
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public final class SearchView extends JComponent implements ActionListener, GroupSelectionListener {

	// A maximum number of hits that can be found in a search.
	// This helps protect against out-of-memory errors.
	private final static int MAX_HITS = 100000;
	private static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
	private JLabel hitCountL;
	private JLabel optionalLabel;
	private JTextField searchTF;
	private JTextField optionalSearchTF2;
	private JComboBox sequence_CB = new JComboBox();
	private JComboBox searchCB;
	private JButton clear_button = new JButton("Clear");
	private SeqMapView gviewer;
	private Vector<GlyphI> glyphs = new Vector<GlyphI>();
	private Color hitcolor = new Color(150, 150, 255);
	private static final String ALLID = "All IDs";
	private static final String REGEXID = "ID matches string or Regular Expression";
	private static final String BETWEENID = "ID between x and y";
	private static final String REGEXRESIDUE = "Residues match string or Regular Expression";
	private static final String CHOOSESEARCH = "Choose search method";

	public SearchView() {
		super();
		gviewer = Application.getSingleton().getMapView();
		gmodel.addGroupSelectionListener(this);

		this.setLayout(new BorderLayout());

		initSearchCB();

		initSequenceCB();
		JPanel pan1 = new JPanel();


		JPanel pan2 = new JPanel();
		initSequenceCB();

		JLabel sequenceChooseLabel = new JLabel("Choose a sequence");
		pan2.add(sequenceChooseLabel);
		pan2.add(sequence_CB);

		JLabel searchMethodLabel = new JLabel(CHOOSESEARCH);
		pan2.add(searchMethodLabel);
		pan2.add(searchCB);

		hitCountL = new JLabel(" No hits");

		initComponents();

		pan2.add(searchTF);
		pan2.add(optionalLabel);
		pan2.add(optionalSearchTF2);


		pan2.add(hitCountL);

		//pan2.add(clear_button);

		this.add("North", pan2);
		this.add("Center", new JPanel());// a blank panel: improves appearance in JDK1.5

		gmodel.addGroupSelectionListener(this);
		searchCB.addActionListener(this);
		searchTF.addActionListener(this);
		clear_button.addActionListener(this);
	}

	private void initSequenceCB() {
		// set up the sequence combo_box
		sequence_CB.removeAllItems();
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		if (group != null) {
			sequence_CB.addItem(IGBConstants.GENOME_SEQ_ID);	// put this at top of list
			for (BioSeq seq : group.getSeqList()) {
				if (seq.getID().equals(IGBConstants.GENOME_SEQ_ID)) {
					continue;
				}
				sequence_CB.addItem(seq.getID());
			}
			sequence_CB.setToolTipText("Genome: " + group.getID());
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
		searchCB = new JComboBox();
		searchCB.removeAllItems();
		searchCB.addItem(ALLID);
		searchCB.addItem(REGEXID);
		searchCB.addItem(BETWEENID);
		searchCB.addItem(REGEXRESIDUE);
		searchCB.setToolTipText(CHOOSESEARCH);
	}

	private void initComponents() {
		searchTF = new JTextField(10);
		optionalSearchTF2 = new JTextField(10);
		optionalSearchTF2.setVisible(false);
		optionalLabel = new JLabel("and");
		optionalLabel.setVisible(false);
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
		System.out.println("Action: " + evt.toString());
		if (src == this.searchCB) {
			clearAll();
			String searchMode = (String) this.searchCB.getSelectedItem();
			boolean optionalField = BETWEENID.equals(searchMode);
			this.optionalLabel.setVisible(optionalField);
			this.optionalSearchTF2.setVisible(optionalField);
			return;
		}
		if (src == this.searchTF || src == this.optionalSearchTF2) {
			String searchMode = (String) this.searchCB.getSelectedItem();
			if (ALLID.equals(searchMode)) {
			} else if (REGEXID.equals(searchMode)) {
			} else if (BETWEENID.equals(searchMode)) {
			} else if (REGEXRESIDUE.equals(searchMode)) {
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
		}
		/*if (src == idsearchTF) {
		if (searchTF.getText().length() == 0) {
		hitCountL.setText(" No hits");
		return;
		}
		String id = searchTF.getText().intern();
		findSym(id);
		return;
		}*/
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

	private final void findSym(String id) {
		if (id == null) {
			return;
		}
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();

		List<SeqSymmetry> sym_list = group.findSyms(id);

		if (sym_list != null && !sym_list.isEmpty()) {
			hitCountL.setText(sym_list.size() + " matches found");
			gmodel.setSelectedSymmetriesAndSeq(sym_list, this);
		} else {
			hitCountL.setText(" no matches");
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

	/**
	 * This gets called when the genome versionName is changed.
	 * @param evt
	 */
	public void groupSelectionChanged(GroupSelectionEvent evt) {
		this.initSequenceCB();
	}
}
