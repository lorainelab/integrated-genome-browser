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

package com.affymetrix.genoviz.widget.neomap;

import com.affymetrix.genoviz.widget.*;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.MapGlyphFactory;
import com.affymetrix.genoviz.datamodel.AnnotatedSequence;
import com.affymetrix.genoviz.datamodel.NASeqFeature;
import com.affymetrix.genoviz.datamodel.AASeqFeature;
import com.affymetrix.genoviz.datamodel.AASequence;
import com.affymetrix.genoviz.datamodel.SeqFeatureI;
import com.affymetrix.genoviz.datamodel.Range;
import com.affymetrix.genoviz.datamodel.SequenceI;
import com.affymetrix.genoviz.glyph.SequenceGlyph;
import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.genoviz.util.Debug;

import java.util.Enumeration;
import java.util.Hashtable;

import java.awt.Frame; // Just for testing.
import java.awt.Frame; // Just for testing.
import java.awt.Color;

/**
 * adapts a {@link NeoMap} to a {@link SequenceI} or an {@link AnnotatedSequence}.
 */
public class NeoMapAnnotatedSeqDA implements DataAdapter {

  AnnotatedSequence annotatedSequence;
  SequenceI sequence;
  NeoMapI map;

  /**
   * throws an exception.
   * This satisfies the DataAdapter interface.
   * However, the real methods are polymorphisms of this one.
   * There is one real setModel method for each valid type of Object.
   */
  public void setModel( Object dataModel ) {
    Debug.inform("DataAdapter loading model");
    if (dataModel instanceof AnnotatedSequence) {
      setModel((AnnotatedSequence) dataModel);
    }
    else
    if (dataModel instanceof SequenceI) {
      setModel((SequenceI) dataModel);
    }
    else
      throw new IllegalArgumentException(
        "Got a " + dataModel.getClass().getName()
        + ". Need a Sequence, or an AnnotatedSequence.");
  }

  /**
   * sets the data model.
   *
   * @param theSequence the annotated sequence to show.
   */
  public void setModel(AnnotatedSequence theSequence) {
    Debug.inform("DataAdapter loading AnnotatedSequence");
    Debug.test(null!=theSequence, "DataAdapter: Null data model.");
    this.annotatedSequence = theSequence;
//    setModel(theSequence.getSequence());
    if (null != this.map) {
      connect(this.map, this.annotatedSequence);
    }
  }

  /**
   * sets the data model.
   *
   * @param theSequence the sequence to show.
   */
  public void setModel(SequenceI theSequence) {
    Debug.inform("DataAdapter loading Sequence");
    Debug.test(null!=theSequence, "DataAdapter: Null data model.");
    this.sequence = theSequence;
    if (null != this.map) {
      connect(this.map, this.sequence);
    }
  }

  String[] glyphColors = { "blue", "darkGray", "magenta", "gray" };
  Color axisColor = Color.black;

  public void setGlyphColors(String[] cols) {
    glyphColors = cols;
  }

  public void setAxisColor(Color c) {
    axisColor = c;
  }

  /**
   * configures the map to show the data model.
   */
  private void connect(NeoMapI theMap, AnnotatedSequence theSequence) {

    int highEnd = Integer.MIN_VALUE;
    int lowEnd = Integer.MAX_VALUE;

    Hashtable featureTypes = new Hashtable();
    Hashtable pieceGlyphs = new Hashtable();

    int length = 0;
    this.sequence = theSequence.getSequence();
    if (null != this.sequence) {
      String residues = this.sequence.getResidues();
      length = Math.max(this.sequence.getLength(), residues.length());
      lowEnd = 1;
      highEnd = length;
      this.map.setMapRange((int)(-length*0.05), (int)(length*1.05));
    }

    AxisGlyph ag = (AxisGlyph)(this.map.addAxis(0));
    ag.setForegroundColor(axisColor);

    // switching to set behavior to NeoMapI.FIT_WIDGET to ensure initial
    // fit of map range (but then will refit with every reshape())
    // -- GAH 5-13-98
    //    this.map.setReshapeBehavior(NeoMapI.X, NeoMapI.NONE);
   this.map.setReshapeBehavior(NeoMapI.X, NeoMapI.FITWIDGET);

    // Add the annotations.

    int featureOffset = 3;
    int FEATURE_HEIGHT = 7;

    Enumeration enum = this.annotatedSequence.features();
    while (enum.hasMoreElements()) {
      Object o = enum.nextElement();
      SeqFeatureI f = (SeqFeatureI)o;
      String type = f.getType();
      if (!type.equals("source")) {
        if (null == featureTypes.get(type)) {
          String s = "-offset -"
            + (featureOffset * FEATURE_HEIGHT)
            + " -color " + glyphColors[featureOffset % glyphColors.length];

          if (type.equalsIgnoreCase("helix")) {
            featureTypes.put(type, this.map.addFactory(
              "-glyphtype com.affymetrix.genoviz.glyph.Glyph " + s));
            pieceGlyphs.put(type, this.map.addFactory(
              "-glyphtype com.affymetrix.genoviz.glyph.SquiggleGlyph " + s));
          }
          else if (type.equalsIgnoreCase("disulfid")) {
            featureTypes.put(type, this.map.addFactory(
              "-glyphtype com.affymetrix.genoviz.glyph.BridgeGlyph " + s));
            pieceGlyphs.put(type, this.map.addFactory(
              "-glyphtype com.affymetrix.genoviz.glyph.BridgeGlyph " + s));
          }
          else {
            featureTypes.put(type, this.map.addFactory(
                  "-glyphtype com.affymetrix.genoviz.glyph.LineContainerGlyph " + s));
            pieceGlyphs.put(type, this.map.addFactory(
                  "-glyphtype com.affymetrix.genoviz.glyph.FillRectGlyph " + s));
          }
          featureOffset++;
        }
        MapGlyphFactory factory = (MapGlyphFactory)featureTypes.get(type);
        int strand = NASeqFeature.FORWARD;
        if (f instanceof NASeqFeature) {
          strand = ((NASeqFeature)f).getStrand();
        }
        int beg = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;
        int p = 0;
        Enumeration pieces = f.pieces();
        while (pieces.hasMoreElements()) {
          p++;
          Object oo = pieces.nextElement();
          Range r = (Range)oo;
          if (r.beg < beg) beg = r.beg;
          if (end < r.end) end = r.end;
        }
        lowEnd = Math.min(lowEnd, beg);
        highEnd = Math.max(highEnd, end);

        if (p < 2) { // There is only one piece.
          factory = (MapGlyphFactory)pieceGlyphs.get(type);
        }
        GlyphI lc;
        if (NASeqFeature.REVERSE == strand)
          lc = (GlyphI)this.map.addItem(factory, end-1, beg-1);
        else
          lc = (GlyphI)this.map.addItem(factory, beg-1, end-1);
        this.map.setDataModel(lc, f);
        if (1 < p) { // There is more than one piece.
          factory = (MapGlyphFactory)pieceGlyphs.get(type);
          pieces = f.pieces();
          while (pieces.hasMoreElements()) {
            Object oo = pieces.nextElement();
            Range r = (Range)oo;
            if (NASeqFeature.REVERSE == strand)
              this.map.addItem(lc, this.map.addItem(factory, r.end-1, r.beg-1));
            else
              this.map.addItem(lc, this.map.addItem(factory, r.beg-1, r.end-1));
          }
        }

      }
    }

    int offset = (featureOffset+1)*FEATURE_HEIGHT;
    if (this.sequence instanceof AASequence) {
      // This magic number of 20 should be changed
      // so that the residues are on the map axis
      // or actually just below it
      // and they are visible.
      this.map.setMapOffset(-offset, 20);
    }
    else {
      this.map.setMapOffset(-offset, offset);
    }

    if (null != this.sequence) {
      //  Create a sequence item, and add visible annotations to it.
      this.map.configure("-offset 1 "
        + "-glyphtype com.affymetrix.genoviz.glyph.SequenceGlyph "
        + "-width 6");
      SequenceGlyph sg =
        (SequenceGlyph)this.map.addItem(0, length-1);
      sg.setResidues(this.sequence.getResidues());
      sg.setBackgroundColor(this.map.getMapColor());
      sg.setSelectable(false);
    }

    length = highEnd - lowEnd + 1;
    this.map.setMapRange(lowEnd - (int)(length*0.05),
      highEnd + (int)(length*0.05));

    this.map.updateWidget();
  }


  /**
   * configures the map to show the sequence.
   */
  private void connect(NeoMapI theMap, SequenceI theSequence) {

    String residues = theSequence.getResidues();
    int length = Math.max(theSequence.getLength(), residues.length());
    this.map.setMapRange((int)(-length*0.05), (int)(length*1.05));

    //  Create a sequence glyph.
    this.map.configure("-offset 1 "
      + "-glyphtype com.affymetrix.genoviz.glyph.SequenceGlyph "
      + "-width 6");
    SequenceGlyph sg =
      (SequenceGlyph)this.map.addItem(0, length-1);
    sg.setResidues(this.sequence.getResidues());
    sg.setBackgroundColor(this.map.getMapColor());
    sg.setSelectable(false);

    this.map.updateWidget();

  }


  /**
   * throws an exception.
   * This satisfies the DataAdapter interface.
   * However, the real methods are polymorphisms of this one.
   * There is one real setWidget method for each valid type of NeoWidgetI.
   */
  public void setWidget(NeoWidgetI theWidget) {
    // Why is this if statement needed?
    // When run from main below it is not needed.
    // When called from AnnoSeq, it is.
    if (theWidget instanceof NeoMapI) {
      setWidget((NeoMapI) theWidget);
    }
    else
      throw new IllegalArgumentException("Need a NeoMapI. "
        + "Got a " + theWidget.getClass().getName() + ".");
  }

  /**
   * sets the widget.
   *
   * @param theMap the map on which to show the sequence.
   */
  public void setWidget(NeoMapI theMap) {
    Debug.test(null!=theMap, "DataAdapter: Null widget.");
    this.map = theMap;
    if (null != this.annotatedSequence) {
      connect(this.map, this.annotatedSequence);
    }
    else if (null != this.sequence) {
      connect(this.map, this.sequence);
    }
  }


}
