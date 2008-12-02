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

package com.affymetrix.igb.util;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.widget.tieredmap.ExpandedTierPacker;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.awt.AdjustableJSlider;

import com.affymetrix.genometry.*;
//import com.affymetrix.genometry.seq.*;
//import com.affymetrix.genometry.span.*;
//import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.util.SeqUtils;

public class GenometryViewer extends JPanel implements MouseListener {
  boolean PRINT_SELECTION = true;
  boolean SHOW_RESIDUES = true;
  BioSeq current_seq;
  NeoMap map;
  AdjustableJSlider xzoomer;
  AdjustableJSlider yzoomer;
  ExpandedTierPacker packer;

  int min_height = 10;
  int diff_height = 6;
  int yoffset = 20;
  int glyph_height = 10;
  int separator_height = 3;

  Color[] symcolors = { Color.blue, Color.green, Color.red, Color.white, Color.yellow};

  public GenometryViewer() {
    super();
    this.setLayout(new BorderLayout());
    map = new NeoMap();  // default constructor, so uses NeoMap's internal scrollbars
    map.setBackground(Color.black);
    this.add("Center", map);

    xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
    map.setZoomer(map.X, xzoomer);
    this.add("North", xzoomer);

    yzoomer = new AdjustableJSlider(Adjustable.VERTICAL);
    map.setZoomer(map.Y, yzoomer);
    this.add("East", yzoomer);

    packer = new ExpandedTierPacker();
    packer.setParentSpacer(diff_height/2);
    packer.setStretchHorizontal(false);

    map.setPacker(packer);
    map.addMouseListener(this);
    map.setSelectionAppearance(SceneI.SELECT_OUTLINE);
    map.setSelectionColor(Color.yellow);
  }

  public static GenometryViewer displaySymmetry(SeqSymmetry sym, BioSeq seq) {
    return displaySymmetry(sym, seq, true);
  }

  public static GenometryViewer displaySeq(BioSeq seq) {
    return displaySeq(seq, true);
  }

  public static GenometryViewer displaySeq(BioSeq seq, boolean showres) {
    GenometryViewer view = new GenometryViewer();
    view.setShowResidues(showres);
    view.setSeq(seq);
    //    view.setSymmetry(sym, sym.getSpan(0).getBioSeq());
    JFrame frm = new JFrame();
    Container pan = frm.getContentPane();
    pan.setLayout(new BorderLayout());
    pan.add("Center", view);
    frm.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent evt) { System.exit(0); } } );
    frm.setSize(800, 200);
    frm.show();
    return view;
  }

  public static GenometryViewer displaySymmetry(SeqSymmetry sym, BioSeq seq, boolean showres) {
    GenometryViewer view = displaySeq(seq, showres);
    view.addSymmetry(sym, seq);
    return view;
  }
  
  public void setShowResidues(boolean b) {
    SHOW_RESIDUES = b;
  }
  public void setPrintSelection(boolean b) {
    PRINT_SELECTION = b;
  }
  public boolean getShowResidues() { return SHOW_RESIDUES; }
  public boolean getPrintSlection() { return PRINT_SELECTION; }

  public void addSeparator() {
    GlyphI gl = new FillRectGlyph();
    gl.setColor(Color.gray);
    gl.setCoords(0, 0, current_seq.getLength(), separator_height);
    map.addItem(gl);
    map.repack();
    map.stretchToFit();
    map.updateWidget();
  }

  public void addSymmetry(SeqSymmetry sym, BioSeq seq, boolean repack) {
    if (sym == null) { return; }
    if (seq != current_seq) {
      System.out.println("when adding symmetries, seq must be same as previously added symmetries!");
    }
    int symDepth = SeqUtils.getDepth(sym) -1;
    GlyphI symglyph = glyphifySymmetry(sym, seq, null, 0, glyph_height);
    if (repack) {
      map.setPacker(packer);
      map.repack();
      map.stretchToFit();
      map.updateWidget();
    }
  }

  public NeoMap getSeqMap() {
    return map;
  }

  public void addSymmetry(SeqSymmetry sym, BioSeq seq) {
    addSymmetry(sym, seq, true);
  }

  public BioSeq getSeq() {
    return current_seq;
  }

  public void setAnnotatedSeq(AnnotatedBioSeq seq) {
    this.setSeq(seq);
    int annotCount = seq.getAnnotationCount();
    System.out.println("annotation count: " + annotCount);
    for (int i=0; i<annotCount; i++) {
      SeqSymmetry annot = seq.getAnnotation(i);
      this.addSymmetry(annot, seq, false);
    }
    map.setPacker(packer);
    map.repack();
    map.stretchToFit();
    map.updateWidget();
  }


  public void setSeq(BioSeq seq) {
    map.clearWidget();
    current_seq = seq;
    int length = seq.getLength();
    map.setMapOffset(0, 200);
    map.setMapRange(0, length);
    GlyphI agl = map.addAxis(50);
    agl.setColor(Color.gray);
    // just trying to give axis a little more space
    agl.getCoordBox().height = 40;

    if (SHOW_RESIDUES) {
      System.out.println("getting residues for " + seq.getID());
      String residues = seq.getResidues();
      if (residues != null) {
	System.out.println("residues: " + residues.length());
	SequenceGlyph sg = new SequenceGlyph();
	sg.setResidues(residues);
	sg.setCoords(0, 0, seq.getLength(), 10);
	sg.setForegroundColor(Color.yellow);
	map.addItem(sg);
      }
      else {
	System.out.println("residues not available for " + seq.getID());
      }
    }

    map.updateWidget();
  }

  public void setSymmetry(SeqSymmetry sym, BioSeq seq) {
    setSeq(seq);
    addSymmetry(sym, seq);
  }

  public GlyphI glyphifySymmetry(SeqSymmetry sym, BioSeq seq, GlyphI parent_glyph, 
			       int depth, int glyph_height) {
    if (sym == null) { return null; }
    SeqSpan span = sym.getSpan(seq);
    if (span == null) { return null; }
    boolean forward = span.isForward();

    Color col = symcolors[depth % symcolors.length];   // cycle through colors
    //    GlyphI gl = new FillRectGlyph();
    GlyphI gl = new OutlineRectGlyph();
    gl.setColor(col);

    // hack for 0-sized parent, because for some reason 
    //  0-width glyph is not being displayed (nor are its children...)
    if (span.getMin() <= 0 && span.getMax() <= 0) {
      gl.setCoords(0, 0, seq.getLength(), glyph_height);
    }
    else {
      gl.setCoords(span.getMin(), 0, span.getLength(), glyph_height);
    }
    if (parent_glyph == null) {
      map.addItem(gl);        // if no parent glyph, add directly to map (or tier...)
    }
    else {
      parent_glyph.addChild(gl);
    }

    int childCount = sym.getChildCount();
    if (childCount > 0) {
      gl.setPacker(packer);
      // now recursively call glyphifySymmetry on children
      for (int i=0; i<childCount; i++) {
	SeqSymmetry childsym = sym.getChild(i);
	glyphifySymmetry(childsym, seq, gl, depth+1, glyph_height);
      }
      gl.pack(map.getView());
    }
    gl.setInfo(sym);
    return gl;
  }

  /** MouseListener interface implementation */
  public void mouseClicked(MouseEvent e) { }
  public void mouseEntered(MouseEvent e) { }
  public void mouseExited(MouseEvent e) { }
  public void mouseReleased(MouseEvent e) { }
  public void mousePressed(MouseEvent e) {
    Object source = e.getSource();
    if (!(e instanceof NeoMouseEvent)) { return; }
    NeoMouseEvent nme = (NeoMouseEvent)e;
    Object coord_source = nme.getSource();
    if (coord_source == map)  {
      map.setZoomBehavior(map.X, map.CONSTRAIN_COORD, nme.getCoordX());
    }
    Vector hitGlyphs = map.getItems(nme.getCoordX(), nme.getCoordY());
    map.clearSelected();
    if (hitGlyphs != null && hitGlyphs.size() > 0) {
      GlyphI sel_glyph = (GlyphI)hitGlyphs.elementAt(hitGlyphs.size()-1);
      map.select(sel_glyph);
      map.updateWidget();
      SeqSymmetry sym = (SeqSymmetry)sel_glyph.getInfo();
      if (sym != null && PRINT_SELECTION) { 
	SeqUtils.printSymmetry(sym); 
      }
    }
  }

  /*public static void main(String[] args) {
    SeqSymmetry testsym1 = makeTestSymmetry1();
    BioSeq seq = testsym1.getSpan(0).getBioSeq();
    GenometryViewer view = displaySymmetry(testsym1, seq);
  }

  public static SeqSymmetry makeTestSymmetry1() {
    MutableSeqSymmetry sym = new SimpleMutableSeqSymmetry();
    BioSeq seq1 = new SimpleBioSeq("seq1", 10000);
    sym.addSpan(new SimpleSeqSpan(2000, 8000, seq1));
    MutableSeqSymmetry sym2 = new SimpleMutableSeqSymmetry();
    sym2.addSpan(new SimpleSeqSpan(2000, 2500, seq1));
    MutableSeqSymmetry sym3 = new SimpleMutableSeqSymmetry();
    sym3.addSpan(new SimpleSeqSpan(7000, 8000, seq1));
    MutableSeqSymmetry sym4 = new SimpleMutableSeqSymmetry();
    sym4.addSpan(new SimpleSeqSpan(7000, 7200, seq1));
    MutableSeqSymmetry sym5 = new SimpleMutableSeqSymmetry();
    sym5.addSpan(new SimpleSeqSpan(7700, 8000, seq1));
    MutableSeqSymmetry sym6 = new SimpleMutableSeqSymmetry();
    sym6.addSpan(new SimpleSeqSpan(7000, 7090, seq1));
    MutableSeqSymmetry sym7 = new SimpleMutableSeqSymmetry();
    sym7.addSpan(new SimpleSeqSpan(7110, 7200, seq1));
    sym.addChild(sym2);
    sym.addChild(sym3);
    sym3.addChild(sym4);
    sym3.addChild(sym5);
    sym4.addChild(sym6);
    sym4.addChild(sym7);
    return sym;
  }*/

}

