/**
 *   Copyright (c) 2001-2005 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.SeqSpan;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.igb.tiers.*;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.glyph.FlyPointLinkerGlyph;
import com.affymetrix.genometryImpl.util.IntList;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.style.DefaultIAnnotStyle;
import com.affymetrix.igb.util.UnibrowPrefsUtil;

/**
 *  OrfAnalyzer2 is used on the virtual sequence being viewed in AltSpliceView.  It does
 *  not incorporate content stat graphs (GC, dicodon, etc.).
 */
public final class OrfAnalyzer extends JComponent
		implements ChangeListener, ActionListener {

	public static final String PREF_STOP_CODON_COLOR = "stop codon";
	public static final String PREF_DYNAMIC_ORF_COLOR = "dynamic orf";
	public static final Color default_stop_codon_color = new Color(200, 150, 150);
	public static final Color default_dynamic_orf_color = new Color(100, 200, 100);
	// GAH 8-23-2004
	// As IGB is currently configured, smv should be set to the internal SeqMapView of the AltSpliceView...
	SeqMapView smv;
	JSlider orf_thresh_slider;
	JCheckBox showCB;
	JLabel orf_threshL;
	JLabel orfL;
	TransformTierGlyph fortier;
	TransformTierGlyph revtier;
	int current_orf_thresh = 300;
	int orf_thresh_min = 10;
	int orf_thresh_max = 500;
	int max_analysis_span = 1000000;
	boolean show_orfs;
	BioSeq current_seq;
	private List<FlyPointLinkerGlyph> orf_holders = new ArrayList<FlyPointLinkerGlyph>();
	String[] stop_codons = {"TAA", "TAG", "TGA", "TTA", "CTA", "TCA"};
	Color[] stop_colors = {Color.red, Color.orange, Color.yellow};
	boolean vertical_layout = false;

	public OrfAnalyzer(SeqMapView view, boolean vlayout) {
		super();
		vertical_layout = vlayout;
		smv = view;
		init();
	}

	public void init() {

		orfL = new JLabel("Min ORF Length:", SwingConstants.CENTER);
		orf_threshL = new JLabel(Integer.toString(current_orf_thresh), SwingConstants.CENTER);
		;
		showCB = new JCheckBox("Analyze ORFs");
		orf_thresh_slider = new JSlider(JSlider.HORIZONTAL, orf_thresh_min, orf_thresh_max, current_orf_thresh);
		JPanel pan1 = new JPanel();
		pan1.setLayout(new FlowLayout());
		pan1.add(orfL);
		pan1.add(orf_threshL);

		if (vertical_layout) {
			this.setLayout(new GridLayout(3, 1));
			this.add(showCB);
			this.add(pan1);
			this.add(orf_thresh_slider);
		} else {
			this.setLayout(new FlowLayout());
			this.add(showCB);
			this.add(pan1);
			this.add(orf_thresh_slider);
		}

		showCB.addActionListener(this);
		orf_thresh_slider.addChangeListener(this);
	}

	public void stateChanged(ChangeEvent evt) {
		Object src = evt.getSource();
		if (src == orf_thresh_slider) {
			current_orf_thresh = orf_thresh_slider.getValue();
			for (FlyPointLinkerGlyph fw : orf_holders) {
				fw.setMinThreshold(current_orf_thresh);
			}
			AffyTieredMap map = smv.getSeqMap();
			map.updateWidget();
			orf_threshL.setText(Integer.toString(current_orf_thresh));
		}
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		if (src == showCB) {
			show_orfs = ((JCheckBox) src).isSelected();
			if (show_orfs) {
				redoOrfs();
			} else {
				removeTiersFromMap();
				adjustMap();
			}
		}
	}

	public void redoOrfs() {
		if (smv == null) {
			return;
		}
		BioSeq vseq = smv.getViewSeq();
		current_seq = vseq;
		if (current_seq == null) {
			return;
		}
		removeTiersFromMap();

		if (!show_orfs) {
			return;
		}
		SeqSpan vspan = smv.getVisibleSpan();
		int span_start = vspan.getMin();
		int span_end = vspan.getMax();
		int span_mid = (int) (0.5f * span_start + 0.5f * span_end);

		span_start = span_mid - (max_analysis_span / 2);
		span_start -= span_start % 3;
		span_end = span_mid + (max_analysis_span / 2);
		span_end -= span_end % 3;

		int span_length = span_end - span_start;

		int residue_offset = 0;


		AffyTieredMap map = smv.getSeqMap();
		orf_holders = new ArrayList<FlyPointLinkerGlyph>();
		if (vseq == null || !(vseq.isComplete())) {
			Application.errorPanel("Cannot perform ORF analysis: must first load all residues for sequence");
			show_orfs = false;
			showCB.setSelected(false);
			return;
		}

		residue_offset = vseq.getMin();


		fortier = new TransformTierGlyph(new DefaultIAnnotStyle());
		fortier.setLabel("Stop Codons");
		fortier.setFixedPixelHeight(true);
		fortier.setFixedPixHeight(25);
		fortier.setFillColor(Color.darkGray);
		fortier.setDirection(TierGlyph.Direction.FORWARD);
		map.addTier(fortier, true);  // put forward tier above axis

		revtier = new TransformTierGlyph(new DefaultIAnnotStyle());
		revtier.setLabel("Stop Codons");
		revtier.setFixedPixelHeight(true);
		revtier.setFixedPixHeight(25);
		revtier.setFillColor(Color.darkGray);
		revtier.setDirection(TierGlyph.Direction.REVERSE);
		map.addTier(revtier, false);  // put reverse tier below axis

		Color pointcol = UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_STOP_CODON_COLOR, default_stop_codon_color);
		Color linkcol = UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_DYNAMIC_ORF_COLOR, default_dynamic_orf_color);

		IntList[] frame_lists = new IntList[6];
		for (int i = 0; i < 6; i++) {
			// estimating number of stop codons, then padding by 20%
			frame_lists[i] = new IntList((int) ((span_length / 64) / 1.2));
		}


		for (int i = 0; i < stop_codons.length; i++) {
			int count = 0;
			boolean forward_codon = (i <= 2);
			String codon = stop_codons[i];

			int seq_index = span_start;
			int res_index = span_start - residue_offset;
			res_index = vseq.indexOf(codon, res_index);

			// need to factor in possible offset of residues string from start of
			//    sequence (for example, when sequence is a CompNegSeq)
			while (res_index >= 0 && (seq_index < span_end)) {
				int frame;
				// need to factor in possible offset of residues string from start of
				//    sequence (for example, when sequence is a CompNegSeq)
				seq_index = res_index + residue_offset;

				if (forward_codon) {
					frame = res_index % 3;
				} // forward frames = (0, 1, 2)
				else {
					frame = 3 + (res_index % 3);
				} // reverse frames = (3, 4, 5)
				frame_lists[frame].add(seq_index);
				res_index = vseq.indexOf(codon, res_index + 1);

				count++;
			}
		}

		for (int frame = 0; frame < 6; frame++) {
			boolean forward = frame <= 2;
			IntList xpos_vec = frame_lists[frame];
			int[] xpos = xpos_vec.copyToArray();
			// must sort positions!  because positions were added to IntList for each type of
			//   stop codon before other types, positions in IntList will _not_ be in
			//   ascending order (though for a particular type, e.g. "TAA", they will be)
			Arrays.sort(xpos);

			GlyphI point_template = new FillRectGlyph();
			point_template.setColor(pointcol);
			point_template.setCoords(residue_offset, 0, vseq.getLength(), 10);

			GlyphI link_template = new FillRectGlyph();
			link_template.setColor(linkcol);
			link_template.setCoords(0, 0, 0, 4);  // only height is retained

			TierGlyph tier = forward ? fortier : revtier;
			GlyphI orf_glyph = null;
			if (xpos.length > 0) {
				FlyPointLinkerGlyph fw = new FlyPointLinkerGlyph(point_template, link_template, xpos, 3,
						span_start, span_end);
				fw.setHitable(false);
				orf_glyph = fw;
				fw.setMinThreshold(current_orf_thresh);
				orf_holders.add(fw);
			} else {
				orf_glyph = new FillRectGlyph() {

					@Override
					public boolean isHitable() {
						return false;
					}
				};
				orf_glyph.setColor(tier.getFillColor());
			}
			// Make orf_glyph as long as vseq; otherwise, two or more could pack into one line.
			// The underlying symmetry may be shorter, so "zoom to selected" won't work.
			// But the glyph not hittable (for other reasons), so this isn't an issue.
			orf_glyph.setCoords(residue_offset, 0, vseq.getLength(), point_template.getCoordBox().height);
			tier.addChild(orf_glyph);
		}
		adjustMap();
	}

	private void removeTiersFromMap() {
		AffyTieredMap map = smv.getSeqMap();
		if (fortier != null) {
			map.removeTier(fortier);
			fortier = null;
		}
		if (revtier != null) {
			map.removeTier(revtier);
			revtier = null;
		}
	}

	private void adjustMap() {
		AffyTieredMap tiermap = smv.getSeqMap();
		tiermap.repack();
		tiermap.stretchToFit(false, true);
		tiermap.updateWidget();
	}
}
