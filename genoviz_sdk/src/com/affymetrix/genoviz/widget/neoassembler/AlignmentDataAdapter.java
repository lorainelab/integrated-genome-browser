/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.widget.neoassembler;

import com.affymetrix.genoviz.glyph.AlignedResiduesGlyph;
import com.affymetrix.genoviz.widget.DataAdapter;
import com.affymetrix.genoviz.widget.NeoAssemblerI;
import com.affymetrix.genoviz.widget.NeoMapI;
import com.affymetrix.genoviz.widget.NeoWidgetI;
import com.affymetrix.genoviz.bioviews.GlyphI;

import com.affymetrix.genoviz.datamodel.MultiSeqAlign;
import com.affymetrix.genoviz.util.Debug;

import java.awt.Color;
import java.awt.Component;
import java.util.StringTokenizer;
import java.util.Enumeration;

public class AlignmentDataAdapter implements DataAdapter {

  MultiSeqAlign alignment;
  NeoAssemblerI assem;

  public void setModel( Object dataModel ) {
    Debug.test(null!=dataModel, "DataAdapter: Null data model.");
    if (dataModel instanceof MultiSeqAlign) {
      setModel((MultiSeqAlign)dataModel);
    }
    else {
      throw new IllegalArgumentException("Need a MultiSeqAlign");
    }
  }
  protected void setModel(MultiSeqAlign theMultiSeqAlign) {
    this.alignment = theMultiSeqAlign;
    if (null != this.assem) {
      doIt();
    }
  }

  public void setWidget( NeoWidgetI widget ) {
    Debug.test(null!=widget, "DataAdapter: Null widget.");
    if (widget instanceof NeoAssemblerI) {
      setWidget((NeoAssemblerI)widget);
    }
    else {
      throw new IllegalArgumentException("Need a NeoAssemblerI");
    }
  }
  protected void setWidget(NeoAssemblerI widget) {
    this.assem = widget;
    if (null != this.alignment) {
      doIt();
    }
  }

  private int assemStart = 0;
  private int assemEnd = 0;

  private void doIt() {

    Enumeration enum = this.alignment.sequenceNames();
    while (enum.hasMoreElements()) {
      Object o = enum.nextElement();
      String key = (String)o;
      String seq = this.alignment.getResidues(key);
      addSequence(key, seq);
    }

//    this.setConsensus(assem, alignment.buildConsensus());

    assem.setRange(assemStart-1, assemEnd+1);
    assem.setBackground(NeoAssemblerI.ALIGNMENTS, Color.gray);
    assem.setBackground(NeoAssemblerI.CONSENSUS, Color.gray);
  }

  public void setConsensus(
    String        theResidues)
  {
    GlyphI theSeq;
    StringTokenizer st = new StringTokenizer(theResidues);
    int spanStart = 0, spanEnd = 1;

    // Figure out where the first and last residues are.
    char[] residue = theResidues.toCharArray();
    for (spanStart = 0; ' ' == residue[spanStart]; spanStart++);
    for (spanEnd = residue.length-1; ' ' == residue[spanEnd]; spanEnd--);
    if (assemEnd < spanEnd) assemEnd = spanEnd;

    theSeq = assem.setConsensus(spanStart, spanEnd, theResidues);

    // Add the residues without any gaps.
    // The gaps will be deduced when we add spans later.
    StringBuffer sb = new StringBuffer();
    while (st.hasMoreTokens()) {
      sb.append(st.nextToken());
    }

    assem.setResidues(theSeq, sb.toString());
    addSpans(residue , theSeq);
  }

  private void addSequence(
    String        theName,
    String        theResidues)
  {
    GlyphI theSeq;
    StringTokenizer st = new StringTokenizer(theResidues);
    int spanStart = 0, spanEnd = 1;

    // Figure out where the first and last residues are.
    char[] residue = theResidues.toCharArray();
    for (spanStart = 0;
        spanStart < residue.length && ' ' == residue[spanStart];
        spanStart++);
    for (spanEnd = residue.length-1;
        spanEnd > 0 && ' ' == residue[spanEnd];
        spanEnd--);
    if (spanEnd < spanStart) {
      spanEnd = spanStart;
    }
    if (assemEnd < spanEnd) assemEnd = spanEnd;
    theSeq = assem.addSequence(spanStart, spanEnd);
    assem.setLabel( theSeq, theName );

    // Add the residues without any gaps.
    // The gaps will be deduced when we add spans later.
    StringBuffer sb = new StringBuffer();
    while (st.hasMoreTokens()) {
      sb.append(st.nextToken());
    }
    assem.setResidues(theSeq, sb.toString());
    addSpans(residue , theSeq);
  }

  static final int GAP = 0;
  static final int SPAN = 1;

  private void addSpans(
      char[]        residue ,
      GlyphI        theSeq )
  {
    StringBuffer sb = new StringBuffer();
    int spanStart = 0; // spanStart is at the first residue.
    int spanEnd = -1; // spanEnd is at the last residue.
    int mapStart = 0, mapEnd = 1;
    int state = GAP;

    int i;
    for (i = 0; i < residue.length; i++) {
      switch (state) {
      case GAP:
        switch (residue[i]) {
        case ' ':
          break;
        default:
          sb.append(residue[i]);
          mapStart = i;
          spanEnd++;
          spanStart = spanEnd;
          state = SPAN;
        }
        break;
      case SPAN:
        switch (residue[i]) {
        case ' ':
          mapEnd = i - 1;
          AlignedResiduesGlyph spanGlyph;
          spanGlyph = (AlignedResiduesGlyph) assem.addAlignedSpan(theSeq,
              spanStart,
              spanEnd,
              mapStart,
              mapEnd);
          spanGlyph.setBackgroundColorStrategy(spanGlyph.RESIDUE_BASED);
          spanGlyph.setForegroundColor(Color.black);
          state = GAP;
          break;
        default:
          sb.append(residue[i]);
          spanEnd++;
        }
      }
    }

    // Don't neglect the last span if the sequence did not end in a gap.
    if (SPAN == state) {
      mapEnd = i - 1;
      AlignedResiduesGlyph spanGlyph;
      spanGlyph = (AlignedResiduesGlyph) assem.addAlignedSpan(
          theSeq,
          spanStart,
          spanEnd,
          mapStart,
          mapEnd);
      spanGlyph.setBackgroundColorStrategy(spanGlyph.RESIDUE_BASED);
      spanGlyph.setForegroundColor(Color.black);
      state = GAP;
    }

  }

}
