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
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.tiers.TransformTierGlyph;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public final class SearchView extends JComponent implements ActionListener {

	// A maximum number of hits that can be found in a search.
	// This helps protect against out-of-memory errors.
	private final static int MAX_HITS = 100000;
	private static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
	private JLabel idsearchL;
	private JTextField idsearchTF;
	private JLabel idHitCountL;
	private JTextField regexTF;
	private JLabel regexL;
	private JLabel regexHitCountL;
	private JRadioButton ID_all_RB;
  private JLabel ID_and_Label;
  private JTextField ID_between_1_TF;
  private JTextField ID_between_2_TF;
  private JRadioButton ID_between_RB;
  private JRadioButton ID_exact_RB;
  private JTextField ID_exact_TF;
  private JRadioButton ID_regex_RB;
  private JTextField ID_regex_TF;
  private JRadioButton ID_starts_with_RB;
  private JTextField ID_starts_with_TF;
  private JRadioButton SEQ_all_RB;
  private JRadioButton SEQ_name_RB;

	private ButtonGroup buttonGroup1;
  private ButtonGroup buttonGroup2;
  private ButtonGroup buttonGroup3;
  private JPanel jPanel1;
  private JPanel jPanel2;
	
  private JButton reset_button;
  private JComboBox sequence_CB;

	private JButton clear_button = new JButton("Clear");
	private SeqMapView gviewer;
	private Vector<GlyphI> glyphs = new Vector<GlyphI>();
	private Color hitcolor = new Color(150, 150, 255);

	public SearchView() {
		super();
		gviewer = Application.getSingleton().getMapView();

		this.setLayout(new BorderLayout());
		JPanel pan1 = new JPanel();
		pan1.setLayout(new GridLayout(3, 2));

		idsearchL = new JLabel("ID of annotation to find: ");
		idsearchTF = new JTextField("", 20);
		idHitCountL = new JLabel(" No hits");

		regexL = new JLabel("String or regex to find in sequence: ");
		regexTF = new JTextField("", 20);
		regexHitCountL = new JLabel(" No hits");


		//initComponents();
		
		/*pan1.add(ID_exact_RB);
		pan1.add(ID_starts_with_RB);
		pan1.add(ID_regex_RB);
		pan1.add(ID_all_RB);
		pan1.add(ID_between_RB);
		pan1.add(ID_between_1_TF);
		pan1.add(ID_between_2_TF);*/

		
		pan1.add(idsearchL);
		pan1.add(idsearchTF);
		pan1.add(idHitCountL);
		
		pan1.add(regexL);
		pan1.add(regexTF);
		pan1.add(regexHitCountL);

		pan1.add(clear_button);
	
		this.add("North", pan1);
		this.add("Center", new JPanel());// a blank panel: improves appearance in JDK1.5

		idsearchTF.addActionListener(this);
		regexTF.addActionListener(this);
		clear_button.addActionListener(this);
	}


  private void initComponents() {
    buttonGroup1 = new ButtonGroup();
    buttonGroup2 = new ButtonGroup();
    //buttonGroup3 = new ButtonGroup();
    jPanel1 = new JPanel();
    ID_exact_RB = new JRadioButton();
    ID_starts_with_RB = new JRadioButton();
    ID_regex_RB = new JRadioButton();
    ID_exact_TF = new JTextField();
    ID_starts_with_TF = new JTextField();
    ID_regex_TF = new JTextField();
    ID_all_RB = new JRadioButton();
    ID_between_1_TF = new JTextField();
    ID_between_2_TF = new JTextField();
    ID_and_Label = new JLabel();
    ID_between_RB = new JRadioButton();
    jPanel2 = new JPanel();
    SEQ_all_RB = new JRadioButton();
    SEQ_name_RB = new JRadioButton();
    sequence_CB = new JComboBox();
    reset_button = new JButton();

   // jPanel1.setBorder(BorderFactory.createTitledBorder("Find Annotations By..."));
    buttonGroup1.add(ID_exact_RB);
    ID_exact_RB.setMnemonic('E');
    ID_exact_RB.setText("Exact ID");
    ID_exact_RB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    ID_exact_RB.setMargin(new java.awt.Insets(0, 0, 0, 0));
    ID_exact_RB.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent evt) {
        ID_exact_RBStateChanged(evt);
      }
    });

    buttonGroup1.add(ID_starts_with_RB);
    ID_starts_with_RB.setMnemonic('D');
    ID_starts_with_RB.setText("ID starts with");
    ID_starts_with_RB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    ID_starts_with_RB.setMargin(new java.awt.Insets(0, 0, 0, 0));
    ID_starts_with_RB.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent evt) {
        ID_starts_with_RBStateChanged(evt);
      }
    });

    buttonGroup1.add(ID_regex_RB);
    ID_regex_RB.setMnemonic('x');
    ID_regex_RB.setText("Regular Expression");
    ID_regex_RB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    ID_regex_RB.setMargin(new java.awt.Insets(0, 0, 0, 0));
    ID_regex_RB.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent evt) {
        ID_regex_RBStateChanged(evt);
      }
    });

    ID_exact_TF.setEnabled(false);

    ID_starts_with_TF.setEnabled(false);

    ID_regex_TF.setEnabled(false);

    buttonGroup1.add(ID_all_RB);
    ID_all_RB.setMnemonic('I');
    ID_all_RB.setText("All IDs");
    ID_all_RB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    ID_all_RB.setMargin(new java.awt.Insets(0, 0, 0, 0));

    ID_between_1_TF.setEnabled(false);

    ID_between_2_TF.setEnabled(false);

    ID_and_Label.setText("and");

    buttonGroup1.add(ID_between_RB);
    ID_between_RB.setMnemonic('b');
    ID_between_RB.setText("ID between");
    ID_between_RB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    ID_between_RB.setMargin(new java.awt.Insets(0, 0, 0, 0));
    ID_between_RB.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent evt) {
        ID_between_RBStateChanged(evt);
      }
    });

    sequence_CB.setEnabled(false);

  }

	private void reset_buttonActionPerformed(ActionEvent evt) {
    ID_exact_TF.setText("");
    ID_between_1_TF.setText("");
    ID_between_2_TF.setText("");
    ID_regex_TF.setText("");
    ID_starts_with_TF.setText("");
    ID_all_RB.setSelected(true);
    SEQ_all_RB.setSelected(true);
  }

  private void SEQ_name_RBStateChanged(ChangeEvent evt) {
    sequence_CB.setEnabled(SEQ_name_RB.isSelected());
  }

  private void ID_regex_RBStateChanged(ChangeEvent evt) {
    ID_regex_TF.setEnabled(ID_regex_RB.isSelected());
  }

  private void ID_between_RBStateChanged(ChangeEvent evt) {
    ID_between_1_TF.setEnabled(ID_between_RB.isSelected());
    ID_between_2_TF.setEnabled(ID_between_RB.isSelected());
  }

  private void ID_starts_with_RBStateChanged(ChangeEvent evt) {
    ID_starts_with_TF.setEnabled(ID_starts_with_RB.isSelected());
  }

  private void ID_exact_RBStateChanged(ChangeEvent evt) {
    ID_exact_TF.setEnabled(ID_exact_RB.isSelected());
  }


	private void clearAll() {		
		idsearchTF.setText("");
		regexTF.setText("");
		idHitCountL.setText(" No hits");
		regexHitCountL.setText(" No hits");
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

		if (src == clear_button) {
			clearAll();
			return;
		}
		if (src == idsearchTF) {
			if (idsearchTF.getText().length() == 0) {
				idHitCountL.setText(" No hits");
				return;
			}
			String id = idsearchTF.getText().intern();
			findSym(id);
			return;
		}
		if (src == regexTF) {
			clearResults();

			Timer tim = new Timer();

			NeoMap map = gviewer.getSeqMap();
			BioSeq vseq = gviewer.getViewSeq();
			if (vseq == null || !vseq.isComplete()) {
				if (src == regexTF) {
					regexHitCountL.setText(" No hits");
				}
				Application.errorPanel("Residues for seq not available, search aborted");
				return;
			}
			int residue_offset = ((SmartAnnotBioSeq) vseq).getMin();

			TransformTierGlyph axis_tier = gviewer.getAxisTier();
			GlyphI seq_glyph = findSeqGlyph(axis_tier);
			if (src == regexTF) {
				regexTF(tim, vseq, residue_offset, seq_glyph, axis_tier, map);
			}
		}
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
		if (regexTF.getText().length() == 0) {
			regexHitCountL.setText(" No hits");
			return;
		}
		regexHitCountL.setText(" Working...");
		tim.start();
		String residues = vseq.getResidues();
		try {
			
			String str = regexTF.getText();
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
			regexHitCountL.setText(" " + hit_count1 + " forward strand hits and " + hit_count2 + " reverse strand hits");
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
			idHitCountL.setText(sym_list.size() + " matches found");
			gmodel.setSelectedSymmetriesAndSeq(sym_list, this);
		} else {
			idHitCountL.setText(" no matches");
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

}