/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genometry.*;
import com.affymetrix.igb.tiers.*;

import com.affymetrix.igb.IGB;
import com.affymetrix.igb.genometry.NibbleBioSeq;
import com.affymetrix.igb.glyph.FlyPointLinkerGlyph;
import com.affymetrix.igb.util.IntList;
import com.affymetrix.genometry.seq.CompositeNegSeq;

/**
 *  Variation on orf analysis panel.  OrfAnalyzer2 differs from OrfAnalyzer in that
 *  OrfAnalyzer2 is simpler.  It is meant to be used as a component integrated into the
 *  AltSpliceView, and apply to the virtual sequence being viewed in AltSpliceView.  It does
 *  not incorporate (at least not yet) content stat graphs (GC, dicodon, etc.).
 */
public class OrfAnalyzer2 extends JComponent
    implements ChangeListener, ActionListener  {

  // GAH 8-23-2004
  // As IGB is currently configured, smv should be set to the internal SeqMapView of the AltSpliceView...
  SeqMapView smv;
  //  AltSpliceView sliceview;
  JSlider orf_thresh_slider;
  JCheckBox showCB;
  //  JTextField orf_threshTF;
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

  java.util.List orf_holders = new ArrayList();
  String[] stop_codons = { "TAA", "TAG", "TGA", "TTA", "CTA", "TCA" };
  Color[] stop_colors = { Color.red, Color.orange, Color.yellow };
  boolean vertical_layout = false;

  public OrfAnalyzer2(SeqMapView view, boolean vlayout)  {
    super();
    vertical_layout = vlayout;
    smv = view;
    init();
  }

  public void init() {

    orfL = new JLabel("Min ORF Length:", SwingConstants.CENTER);
    orf_threshL = new JLabel(Integer.toString(current_orf_thresh), SwingConstants.CENTER);;
    showCB = new JCheckBox("Analyze ORFs");
    orf_thresh_slider = new JSlider(JSlider.HORIZONTAL, orf_thresh_min, orf_thresh_max, current_orf_thresh);
    //    Dimension dim = orf_thresh_slider.getPreferredSize();
    //    orf_thresh_slider.setPreferredSize(new Dimension(50, dim.height));
    JPanel pan1 = new JPanel();
    pan1.setLayout(new FlowLayout());
    pan1.add(orfL);
    pan1.add(orf_threshL);

    if (vertical_layout) {
      this.setLayout(new GridLayout(3,1));
      this.add(showCB);
      this.add(pan1);
      this.add(orf_thresh_slider);
    }
    else {
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
      //      System.out.println("value: " + current_orf_thresh);
      for (int i=0; i<orf_holders.size(); i++) {
	FlyPointLinkerGlyph fw = (FlyPointLinkerGlyph)orf_holders.get(i);
	fw.setMinThreshold(current_orf_thresh);
      }
      AffyTieredMap map = smv.getSeqMap();
      map.updateWidget();
      //      orf_threshTF.setText(current_orf_thresh);
      orf_threshL.setText(Integer.toString(current_orf_thresh));
    }
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == showCB) {
      show_orfs = ((JCheckBox)src).isSelected();
      //      System.out.println("got action, show results = " + show_orfs);
      if (show_orfs)  {
        redoOrfs();
      }
      else {
	AffyTieredMap map = smv.getSeqMap();
	removeTiersFromMap();
	adjustMap();
      }
    }
  }


  //   void sequenceChanged(AnnotatedBioSeq newseq) {
  public void redoOrfs()  {
    if (smv == null) { return; }
    BioSeq vseq = smv.getViewSeq();
    current_seq = vseq;
    if (current_seq == null)  { return; }
    removeTiersFromMap();

    if (! show_orfs) { return; }
    //    System.out.println("running OrfAnalyzer on sequence in SeqMapView: " + smv);
    //    System.out.println("seq: " + current_seq.getID() + ", length = " + current_seq.getLength());
    //    SeqSpan span = new SimpleSeqSpan(15000000, 17000000, current_seq);
    SeqSpan vspan = smv.getVisibleSpan();
    int span_start = vspan.getMin();
    int span_end = vspan.getMax();
    int span_mid = (span_start + span_end)/2;

    span_start = span_mid - (max_analysis_span/2);
    span_start = (int)(span_start / 3) * 3;
    span_end = span_mid + (max_analysis_span/2);
    span_end = (int)(span_end / 3) * 3;

    // rounding down to closest divisible by three (for consistency of forward strand frames)
    //    span_start = (int)(span_start / 3) * 3;
    //    int span_end = span_start + 2000000;
    //    span_end = Math.min(span_end, current_seq.getLength()-55);
    // rounding down to closest divisible by three (for consistency of reverse strand frames)
    //    span_end = (int)(span_end / 3) * 3;
    int span_length = span_end - span_start;

    //    int reside_offset = span_start;
    int residue_offset = 0;
    //    AnnotatedBioSeq aseq = smv.getSeq();
    if (vseq instanceof CompositeNegSeq) {
      residue_offset = ((CompositeNegSeq)vseq).getMin();
    }

    AffyTieredMap map = smv.getSeqMap();
    Map method2color = smv.getColorHash();
    orf_holders = new Vector();
    if (vseq==null || ! (vseq.isComplete())) {
      IGB.errorPanel("Cannot perform ORF analysis: must first load all residues for sequence");
      return;
    }

    String residues = null;
    NibbleBioSeq nibseq = null;
    boolean use_nibseq = (vseq instanceof NibbleBioSeq);
    if (use_nibseq)  {
      nibseq = (NibbleBioSeq)vseq;  // vseq is a NibbleBioSeq, therefore also a CharacterIterator
    }
    else {
      residues = vseq.getResidues();
      //      System.out.println("got residues: " + residues.length());
    }

    fortier = new TransformTierGlyph();
    fortier.setLabel("Stop Codons (+)");
    fortier.setFixedPixelHeight(true);
    fortier.setFixedPixHeight(25);
    fortier.setFillColor(Color.darkGray);
    map.addTier(fortier, true);  // put forward tier above axis

    revtier = new TransformTierGlyph();
    revtier.setLabel("Stop Codons (-)");
    revtier.setFixedPixelHeight(true);
    revtier.setFixedPixHeight(25);
    revtier.setFillColor(Color.darkGray);
    map.addTier(revtier, false);  // put reverse tier below axis

    Color pointcol = (Color)method2color.get("stop_codon");
    Color linkcol = (Color)method2color.get("dynamic_orf");
    IntList[] frame_lists = new IntList[6];
    for (int i=0; i<6; i++) {
      // estimating number of stop codons, then padding by 20%
      //      frame_lists[i] = new IntList((int)((vseq.getLength()/64)/1.2));
      frame_lists[i] = new IntList((int)((span_length/64)/1.2));
    }


    //    System.out.println("start of span: " + span_start);
    //    System.out.println("end of span: " + span_end);
    //    System.out.println("length of span: " + span_length);
    for (int i = 0; i<stop_codons.length; i++) {
      int count=0;
      boolean forward_codon = (i <= 2);
      String codon = stop_codons[i];

      int seq_index = span_start;
      int res_index;
      //      if (use_nibseq)  { res_index = nibseq.indexOf(codon, 0); }
      //      else { res_index = residues.indexOf(codon, 0); }
      res_index = span_start - residue_offset;
      if (use_nibseq)  { res_index = nibseq.indexOf(codon, res_index); }
      else { res_index = residues.indexOf(codon, res_index); }
      // need to factor in possible offset of residues string from start of
      //    sequence (for example, when sequence is a CompNegSeq)
      //      while (res_index >=0) {
      while (res_index >= 0  && (seq_index < span_end)) {
	int frame;
	// need to factor in possible offset of residues string from start of
	//    sequence (for example, when sequence is a CompNegSeq)
	seq_index = res_index + residue_offset;

	if (forward_codon) { frame = res_index % 3; } // forward frames = (0, 1, 2)
	else { frame = 3 + (res_index % 3); } // reverse frames = (3, 4, 5)
	//	System.out.println("frame: " + frame);
	frame_lists[frame].add(seq_index);
	if (use_nibseq) { res_index = nibseq.indexOf(codon, res_index+1); }
	else { res_index = residues.indexOf(codon, res_index+1); }
	count++;
      }
      //      System.out.println("resindex: " + res_index);
      //      System.out.println("seqindex: " + seq_index);
      //      System.out.println("count: " + count);
    }

    for (int frame=0; frame<6; frame++) {
      //    for (int frame=0; frame<1; frame++) {
      boolean forward = frame <= 2;
      IntList xpos_vec = frame_lists[frame];
      //      IntList xpos_vec = frame_lists[0];
      int[] xpos = xpos_vec.copyToArray();
      //      System.out.println("xpos length: " + xpos.length);
      // must sort positions!  because positions were added to IntList for each type of
      //   stop codon before other types, positions in IntList will _not_ be in
      //   ascending order (though for a particular type, e.g. "TAA", they will be)
      Arrays.sort(xpos);

      //      System.out.println("for codons in frame " + frame + ", count = " + xpos.length);
      GlyphI point_template = new FillRectGlyph();
      //      GlyphI point_template = new OutlineRectGlyph();
      point_template.setColor(pointcol);
      point_template.setCoords(residue_offset, 0, vseq.getLength(), 10);
      GlyphI link_template = new FillRectGlyph();
      link_template.setColor(linkcol);
      link_template.setCoords(0, 0, 0, 4);  // only height is retained
      //      FlyweightPointGlyph fw = new FlyweightPointGlyph(point_template, xpos, 3);
      //      FlyPointLinkerGlyph fw = new FlyPointLinkerGlyph(point_template, link_template, xpos, 3);
      FlyPointLinkerGlyph fw = new FlyPointLinkerGlyph(point_template, link_template, xpos, 3,
						       span_start, span_end);
      fw.setMinThreshold(current_orf_thresh);
      orf_holders.add(fw);
      if (forward) {
	fortier.addChild(fw);
      }
      else {
	revtier.addChild(fw);
      }
    }
    adjustMap();
    //    map.repack();
    //    map.updateWidget();
  }


  public void removeTiersFromMap() {
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

  public void adjustMap() {
    AffyTieredMap tiermap = smv.getSeqMap();
    tiermap.repack();
    tiermap.stretchToFit(false, true);
    tiermap.updateWidget();
  }







}
