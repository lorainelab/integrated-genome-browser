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

package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.util.Comparable;
import com.affymetrix.genoviz.util.DNAUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.bioviews.Rectangle2D;
import com.affymetrix.genoviz.datamodel.Mapping;
import com.affymetrix.genoviz.datamodel.SequenceI;
import com.affymetrix.genoviz.datamodel.NASequenceI;
import com.affymetrix.genoviz.datamodel.Sequence;
import com.affymetrix.genoviz.datamodel.NASequence;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.Vector;
import java.util.Enumeration;

/**
 * Used in NeoAssembler to display gapped sequence alignments.
 * @author Gregg Helt
 */
public class AlignmentGlyph extends AbstractResiduesGlyph
implements Comparable {

  public boolean debugdraw = false;

  /**
   *  Experimenting with the option of building a gapped residue.
   *  String internally, and having only one AlignedResiduesGlyph child which
   *  uses the gapped residue String
   *  Use this option if uniChild == true
   */
  public boolean useUniChild = false;
  protected boolean showGapGlyph = false;
  String res_string = null;
  // efficient build of gapped residues string
  StringBuffer align_buffer;
  boolean align_modified = false;
  AlignedResiduesGlyph uniChild;
  boolean first_span_added = true;

    // is alignment being displayed at minimum zoom?
    // (currently widget must _explicitly_ tell glyph _before_ drawing that it
    //  is being drawn at widget's mimumum zoom
    //    boolean at_min_zoom = false;

    // toggle for whether to force switch from per-base representation to
    // arrows when shown at minimum zoom, regardless of size of
    // alignment and assembly
    //    boolean force_min_arrow = false;

    public static int setResiduesCount;

  /*
   * children of an AlignmentGlyph are
   *  AlignedUngappedResidue glyphs -- in other words, a
   *   aligned sequence with gaps is composed of smaller
   *   aligned ungapped sequences, interspersed with insertions
   *     (deletions are just unused residues in the sequence??? --
   *      well, right now there are no deletions
   * can also have other child glyphs, and includes support for
   *    delayed loading of residues for ResiduesGlyphI children
   */

    private int lastResidueEnd = -1;

  private SequenceI seq;
  private SequenceI reference;
  private Mapping seqToRefMap;
  private boolean setSequence = false;
  private boolean setReference = false;

  // These are now inherited from AbstractResiduesGlyph!  Leaving them in
  //   overshadows inherited fields, and causes problems with compare()
  //   (and probably other methods too)  GAH 1-9-98
  //  int seq_beg, seq_end;

  // in addition to holding all child glyphs in getChildren() vector,
  // keeping separate vectors for aligned span children and
  // unaligned span children (otherwise there's no real way to
  // distinguish them)
  protected Vector<GlyphI> unaligned_spans = new Vector<GlyphI>();
  protected Vector<GlyphI> aligned_spans = new Vector<GlyphI>();

  private ArrowGlyph arrow = new ArrowGlyph();

  /** should the arrow ever be drawn? */
  protected boolean drawArrow = true;

  /** should the arrow always be drawn? */
  protected boolean alwaysDrawArrow = false;

  /** should the unaligned spans (usually trimmed edges) always be drawn? */
  protected boolean alwaysDrawUnalignedSpans = false;

  protected boolean forward = true;

  public static final int UNKNOWN_RESIDUES = 0;
  public static final int NA_RESIDUES = 1;
  public static final int AA_RESIDUES = 2;

  /** color-coding based on residue identity */
  public static final int RESIDUE_BASED =
    AlignedResiduesGlyph.RESIDUE_BASED;

  /** color-coding based on residue comparison to a consensus residue */
  public static final int ALIGNMENT_BASED =
    AlignedResiduesGlyph.ALIGNMENT_BASED;

  /** no color-coding, color is fixed constant */
  public static final int FIXED_COLOR =
    AlignedResiduesGlyph.FIXED_COLOR;

  private int residueType = NA_RESIDUES;

  private boolean complementIfReversed = true;

  private Character match_char = null;

  /**
   * constructs a glyph for nucleic acid residues.
   */
  public AlignmentGlyph() {
    this(NA_RESIDUES);
  }

  public AlignmentGlyph(int residueType, int length, boolean useUniChild) {
    this(residueType);
    this.useUniChild = useUniChild;
    if (useUniChild) {
      align_buffer = new StringBuffer(length);
      for (int i=0; i<length; i++) {
        align_buffer.append(' ');
      }
    }
  }



  public AlignmentGlyph(int residueType)  {
    this.residueType = residueType;
    if (useUniChild) {
      if (residueType == AA_RESIDUES) {
        uniChild = new AlignedProteinGlyph();
      }
      else {
        uniChild = new AlignedDNAGlyph();
      }
      uniChild.setForegroundColor(this.getForegroundColor());
      uniChild.setColor(this.getBackgroundColor());
      uniChild.setResidueFont(this.getResidueFont());
    }
    setResidueFont(default_font);
    setDrawOrder(this.DRAW_CHILDREN_FIRST);
  }

  public void setCoords(double x, double y, double width, double height) {
    super.setCoords(x, y, width, height);
    arrow.setCoords(x, y, width, height);
    arrow.setForward(forward);

    Vector subGlyphs = arrow.getChildren();
    if (null != subGlyphs){
      for (int k = 0; k<subGlyphs.size(); k++){
        SolidGlyph sg = (SolidGlyph)(subGlyphs.elementAt(k));
        Rectangle2D rect = sg.getCoordBox();
        sg.setCoords(rect.x, y, rect.width, height);
      }
    }
    if (useUniChild) {
      int seqstart = (int)x;
      int seqend = (int)(x+width-1);
      uniChild.setCoords(x, y, width, height);
    }
  }

  public void showArrow(boolean b) {
    drawArrow = b;
  }

  /**
   * indicates whether to draw arrows.
   * even if higher resolution features are also being drawn.
   */
  public void setAlwaysDrawArrow(boolean bool) {
    alwaysDrawArrow = bool;
  }

  public boolean getAlwaysDrawArrow(boolean bool) {
    return alwaysDrawArrow;
  }

  /**
   *  toggle for whether to always draw unaligned spans.
   * (presumably as edges of alignment),
   * regardless of whether higher resolution features are also being drawn.
   */
  public void setAlwaysDrawUnalignedSpans(boolean bool) {
    alwaysDrawUnalignedSpans = bool;
  }

  public boolean getAlwaysDrawUnalignedSpans(boolean bool) {
    return alwaysDrawUnalignedSpans;
  }

  public void setResidues(String residues) {
    SequenceI newseq = new Sequence();
    newseq.setResidues(residues);
    setSequence(newseq);
  }

  public String getResidues() {
    if ( null == getSequence() ) return null;
    return getSequence().getResidues();
  }

  public void setSequence (SequenceI seq) {
    this.seq = seq;
    setSequence = true;
    if (useUniChild) {
      // need this cached String for efficiency because of new String
      // construction in Sequence.getResidues();
      res_string = seq.getResidues();
      uniChild.setResidues(align_buffer.toString());
      uniChild.setMatchChar ( match_char );
    }
    else {
      if (children == null) { return; }
      int max = children.size();
      AlignedResiduesGlyph child;
      int seqstart, seqend;
      for (int i=0; i<max; i++) {
        if (children.elementAt(i) instanceof AlignedResiduesGlyph) {
          child = (AlignedResiduesGlyph)children.elementAt(i);
          seqstart = child.getParentSeqStart();
          seqend = child.getParentSeqEnd();
          setChildResidues(child, seqstart, seqend);
          child.setMatchChar ( match_char );
        }
      }
      // expanding damage to ensure this glyph is redrawn if
      //    view is using damage optimizations
      scene.expandDamage(this);
    }
  }

  public void setReference(Sequence reference) {
    if (children == null) { return; }
    this.reference = reference;
    setReference = true;
    int max = children.size();
    AlignedResiduesGlyph child;
    for (int i=0; i<max; i++) {
      if (children.elementAt(i) instanceof AlignedResiduesGlyph) {
        child = (AlignedResiduesGlyph)children.elementAt(i);
        child.setReference(reference);
        child.setMatchChar ( match_char );
      }
    }
  }

  public void setMatchChar ( Character match_char ) {
    this.match_char = match_char;
  }

  public Character getMatchChar () {
    return this.match_char;
  }

  public SequenceI getSequence() {
    return seq;
  }

  /**
   * Note that this assumes adding based on sequence,
   * so that it will <em>include</em> the end.
   * Thus if start = 0, end = 1,
   * we are really creating a sequence annotation
   * that starts at 0 and is 2 map units long.
   */
  public GlyphI addUngappedAlignment(int seqstart, int seqend,
      int refstart, int refend) {
    return addAlignedSpan(seqstart, seqend, refstart, refend);
  }

  public AlignedResiduesGlyph addAlignedSpan(int seqstart, int seqend,
      int refstart, int refend) {
    if (useUniChild) {
      if (!setSequence) {
        throw new IllegalArgumentException("To use AlignmentGlyph uniChild " +
            "option, must set the sequence before adding any spans!");
      }
      if (first_span_added) {
        uniChild.setParentSeqStart(seqstart);
        uniChild.setParentSeqEnd(seqend);
        this.addChild(uniChild);
        first_span_added = false;
      }
      else {
      }
      int buf_position = refstart-(int)coordbox.x;
      for (int seq_position = seqstart; seq_position<=seqend; seq_position++) {
        align_buffer.setCharAt(buf_position,
            res_string.charAt(seq_position));
        buf_position++;
      }
      align_modified = true;
      return uniChild;
    }
    else  {
      AlignedResiduesGlyph glyph = null;

      if (residueType == NA_RESIDUES) {
        glyph = new AlignedDNAGlyph();
      } else if (residueType == AA_RESIDUES) {
        glyph = new AlignedProteinGlyph();
      } else {
        glyph = new AlignedResiduesGlyph();
      }
      aligned_spans.addElement(glyph);
      // This assumes that refstart <= refend always!!!

      addResidueGlyphChild(glyph, seqstart, seqend, refstart, refend);

      // adding glyph to show breaks in arrow glyphs that are large enough to view when zoomed out.
      if (lastResidueEnd == -1) {
        lastResidueEnd = refend +1;
      }
      else {
        if (refstart - lastResidueEnd > 0) {
          addResidueGapGlyph( lastResidueEnd, refstart-1);
        }
        lastResidueEnd = refend+1;
      }


      if (reference != null) {
        glyph.setReference(reference);
      }

      glyph.setForegroundColor(this.getForegroundColor());
      glyph.setBackgroundColor(this.getBackgroundColor());
      glyph.setMatchChar ( match_char );
      return glyph;
    }
  }

  public AlignedResiduesGlyph addUnalignedSpan(int seqstart, int seqend,
      int refstart, int refend) {
    AlignedResiduesGlyph glyph;
    if (residueType == NA_RESIDUES) {
      glyph = new AlignedDNAGlyph();
    } else if (residueType == AA_RESIDUES) {
      glyph = new AlignedProteinGlyph();
    } else {
      glyph = new AlignedResiduesGlyph();
    }
    unaligned_spans.addElement(glyph);
    glyph.setBackgroundColorStrategy(glyph.FIXED_COLOR);

    addResidueGlyphChild(glyph, seqstart, seqend, refstart, refend);

    if (lastResidueEnd == -1) {
      lastResidueEnd = refend +1;
    } else {
      if (refstart - lastResidueEnd > 0) {
        addResidueGapGlyph( lastResidueEnd, refstart -1);
      }
      lastResidueEnd = refend + 1;
    }

    glyph.setMatchChar ( match_char );
    return glyph;
  }

  public void setBackgroundColorStrategy(int strategy) {
    Vector glyphs = getAlignedSpans();
    AlignedResiduesGlyph arglyph;
    try {
      for (int i=0; i<glyphs.size(); i++) {
        arglyph = (AlignedResiduesGlyph)glyphs.elementAt(i);
        arglyph.setBackgroundColorStrategy(strategy);
      }
    }
    catch (Exception e) {
      // arglyph must not be an AlignedResiduesGlyph
    }
  }

  public void setBackgroundColorArray(Color[] col_array) {
    if (null != children) {
      Enumeration e = children.elements();
      while (e.hasMoreElements()) {
        Object o = e.nextElement();
        if (o instanceof AlignedResiduesGlyph) {
          ((AlignedResiduesGlyph) o).setBackgroundColorArray(col_array);
          ((AlignedResiduesGlyph)o).redoColors();
        }
      }
    }
  }

  public void setBackgroundColorMatrix(Color[][] col_matrix) {
    if (null != children) {
      Enumeration e = children.elements();
      while (e.hasMoreElements()) {
        Object o = e.nextElement();
        if (o instanceof AlignedResiduesGlyph) {
          ((AlignedResiduesGlyph) o).setBackgroundColorMatrix(col_matrix);
          ((AlignedResiduesGlyph)o).redoColors();
        }
      }
    }
  }

  public void setForegroundColorStrategy(int strategy) {
    Vector glyphs = getAlignedSpans();
    AlignedResiduesGlyph arglyph;
    try  {
      for (int i=0; i<glyphs.size(); i++) {
        arglyph = (AlignedResiduesGlyph)glyphs.elementAt(i);
        arglyph.setForegroundColorStrategy(strategy);
      }
    }
    catch (Exception e) {
      // arglyph must not be an AlignedResiduesGlyph
    }
  }


  public void setForegroundColorMatrix(Color[][] col_matrix) {
    if (null != children) {
      Enumeration e = children.elements();
      while (e.hasMoreElements()) {
        Object o = e.nextElement();
        if (o instanceof AlignedResiduesGlyph) {
          ((AlignedResiduesGlyph) o).setForegroundColorMatrix(col_matrix);
          ((AlignedResiduesGlyph)o).redoColors();
        }
      }
    }
  }


  public Vector getAlignedSpans() {
    return aligned_spans;
  }

  public Vector getUnalignedSpans() {
    return unaligned_spans;
  }

  public void removeChild(GlyphI glyph)  {
    aligned_spans.removeElement(glyph);
    unaligned_spans.removeElement(glyph);
    super.removeChild(glyph);
  }

  // used to add glyphs representing gaps between sequences that are rendered
  // on top of the arrow glyphs when zoomed out.  (PS 1.24.00)

  public void addResidueGapGlyph( int refstart, int refend){
    if(showGapGlyph){
      GlyphI child = new GapGlyph();
      arrow.addChild(child);
      //System.out.println("adding gap glyph");
      child.setCoords((double)(refstart), coordbox.y,
          (double)(refend-refstart), coordbox.height);
      child.setColor(new Color(180, 250, 250));
    }
  }

  public void setShowGapGlyph(boolean state){
    showGapGlyph = state;
  }

  // where most of the time in setting up gapped spans is spent
  public void addResidueGlyphChild(ResiduesGlyphI child,
      int seqstart, int seqend,
      int refstart, int refend) {
    child.setParentSeqStart(seqstart);
    child.setParentSeqEnd(seqend);
    child.setResidueFont(this.getResidueFont());
    addChild(child);

    // This assumes that refstart <= refend always!!!
    child.setCoords((double)refstart, coordbox.y,
                    (double)(refend-refstart+1), coordbox.height);
    // expand if this grows alignment
    expandIfNeeded(child);
    setChildResidues(child, seqstart, seqend);
  }

  protected void setChildResidues(ResiduesGlyphI child,
      int seqstart, int seqend) {

    if (seq == null) {  return; }
    if (this.isForward()) {
      if (seqstart < 0 || seqend >= seq.getLength()) {
        // would throw IllegalArgumentException here, but then could screw
        //     up adding of rest of spans -- returning silently for now
        // throw new IllegalArgumentException("attempt to reference " +
        //    "non-existent sequence position: " + seqstart + " to " +
        //    seqend + " in " + seq.getName());
        return;

      }
      setResiduesCount++;

      child.setResidues(seq.getResidues().substring(seqstart, seqend+1));
    }
    else {  // seqstart > seqend, use reverse complement
      if (seq instanceof NASequence) {
        seqstart = seq.getLength()-1-seqstart;
        seqend = seq.getLength()-1-seqend;
        if (seqstart < 0 || seqend >= seq.getLength()) {
          // would throw IllegalArgumentException here, but then could screw
          //     up adding of rest of spans -- returning silently for now
          // throw new IllegalArgumentException("attempt to reference " +
          //    "non-existent sequence position: " + seqstart + " to " +
          //    seqend + " in " + seq.getName());
          return;
        }
        if (complementIfReversed) {
          child.setResidues(
              ((NASequence)seq).getReverseComplement().substring(seqstart, seqend+1)
              );
    }
        else child.setResidues ( seq.getResidues ( seqstart, seqend + 1 ) );
      }
      else {
        if (seqend < 0 || seqstart >= seq.getLength()) {
          return;
        }
        if ( complementIfReversed ) child.setResidues(DNAUtils.reverseComplement(seq.getResidues().substring(seqend,seqstart+1)));
        else {
          child.setResidues ( seq.getResidues().substring(seqstart, seqend + 1 ) );
        }
      }
    }
  }

  public void setComplementIfReversed ( boolean complementIfReversed ) {
    this.complementIfReversed = complementIfReversed;
  }

  public boolean getComplementIfReversed ( boolean complementIfReversed ) {
    return complementIfReversed;
  }

  /**
   * overriding drawTraversal to provide for semantic zooming.
   * <em>Note</em>:
   * drawTraversal does <em>not</em> call AlignmentGlyph.draw().
   * Rather, it calls arrow.draw() and/or children.drawTraversal() as appropriate.
   * Therefore need to deal with drawing selection here rather than in draw().
   */
  public void drawTraversal(ViewI view)  {
    if (useUniChild && align_modified) {
      //      System.out.println("TRYING TO SET UNICHILD RESIDUES");
      setResidues(align_buffer.toString());
      align_modified = false;
    }
    if (coordbox.intersects(view.getCoordBox()) && isVisible) {
      if (debugdraw)  {
        System.out.println("now in AlignmentGlyph.drawTraversal(): " + this);
      }
      view.transformToPixels(coordbox, pixelbox);
      double pixels_per_base = pixelbox.width / coordbox.width;
      // if resolution is < 1 pixel/base, just draw as an arrow
      // or if it has no children
      //      if (pixels_per_base < 1 || children == null || children.size() <= 0) {
      // if (pixels_per_base < 1 || children == null || children.size() <= 0) {
      // GAH 4-28-99 modified to draw only arrows if pixels_per_base is not not integral
      if (pixels_per_base < 1 || children == null || children.size() <= 0 ||
          ((pixels_per_base-(int)pixels_per_base) != 0))  {
        if (drawArrow)  { arrow.drawTraversal(view); }
        // assuming unaligned spans are NOT transient
        // (should be valid assumption)
        if (alwaysDrawUnalignedSpans) {
          Vector spans = getUnalignedSpans();
          for (int i=0; i<spans.size(); i++) {
            ((GlyphI)spans.elementAt(i)).drawTraversal(view);
          }
        }
        // special-casing outline selection when at low resolution
        if (selected && view.getScene().getSelectionAppearance() == Scene.SELECT_OUTLINE) {
          drawSelectedOutline(view);
        }
        return;
      }
      else if (alwaysDrawArrow) {
        arrow.drawTraversal(view);
        if (selected && view.getScene().getSelectionAppearance() == Scene.SELECT_OUTLINE) {
          drawSelectedOutline(view);
        }
      }
      // otherwise draw the children (ungapped alignment glyphs)
      super.drawTraversal(view);
    }
  }

  /**
   * @param obj is another sequence to be compared with this one.
   * @return &lt; 0 if this sequence starts before the other,
   * or if they start at same position but, this is shorter than the other;
   * 0 if both are of the same size and location;
   * &gt; 0 otherwise.
   */
  public int compare(Object obj) {
    if (!(obj instanceof AlignmentGlyph)) {
      // should this throw an IllegalArgument exception???
      return 0;
    }
    AlignmentGlyph otherseq = (AlignmentGlyph)obj;
    if (seq_beg < otherseq.seq_beg) {
      return -1;
    }
    else if (seq_beg > otherseq.seq_beg) {
      return 1;
    }
    else {   // otherwise they start at the same location, go to size
      if (seq_end < otherseq.seq_end) {
        return -2;
      }
      else if (seq_end > otherseq.seq_end) {
        return 2;
      }
      else {  // same size and length, punt and return 0
        return 0;
      }
    }
  }

  public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
    calcPixels(view);
    return  isVisible?pixel_hitbox.intersects(pixelbox):false;
  }

  public boolean hit(Rectangle2D coord_hitbox, ViewI view)  {
    return isVisible?coord_hitbox.intersects(coordbox):false;
  }

  public void setSelected(boolean selected) {
    super.setSelected(selected);
    arrow.setSelected(selected);
    if (children != null) {
      int size = children.size();
      for (int i=0; i<size; i++) {
        ((GlyphI)children.elementAt(i)).setSelected(selected);
      }
    }
  }

  public void setForward(boolean forward)  {
    this.forward = forward;
    if (arrow != null) {
      arrow.setForward(forward);
    }
  }

  public boolean isForward() {
    return this.forward;
  }

  public void setBackgroundColor(Color c) {
    super.setBackgroundColor(c);
    arrow.setBackgroundColor(c);
    Vector vec = getAlignedSpans();
    GlyphI gl;
    for (int i=0; i<vec.size(); i++) {
      gl = (GlyphI)vec.elementAt(i);
      gl.setColor(c);
    }
  }

  public void setForegroundColor(Color c) {
    super.setForegroundColor(c);
    if (null != children) {
      Enumeration e = children.elements();
      while (e.hasMoreElements()) {
        Object o = e.nextElement();
        if (o instanceof AlignedResiduesGlyph) {
          ((AlignedResiduesGlyph) o).setForegroundColor(c);
        }
      }
    }
  }

  public void setMapping(Mapping m) {
    seqToRefMap = m;
  }

  public Mapping getMapping() {
    return seqToRefMap;
  }

  /**
   * Need to override setScene()
   * to make sure arrowglyph gets its scene set properly.
   */
  public void setScene(Scene s) {
    super.setScene(s);
    arrow.setScene(s);
  }

  /**
   * expands the AlignmentGlyph if child extends the alignment.
   */
  protected void expandIfNeeded(GlyphI child) {
    Rectangle2D childbox = child.getCoordBox();
    double oldend = coordbox.x + coordbox.width;
    double newend = childbox.x + childbox.width;
    if (childbox.x < coordbox.x || newend > oldend) {
      double newx = (double)Math.min(childbox.x, coordbox.x);
      double newwidth = (double)Math.max(oldend, newend) - newx;
      setCoords(newx, coordbox.y, newwidth, coordbox.height);
    }
  }

  /*
   * WARNING: We stub out these methods
   * just to satisfy ResiduesGlyphI interface.
   */

  /** @exception IllegalArgumentException. */
  public void setParentSeqStart(int beg) {
    throw new IllegalArgumentException("AlignmentGlyph.setParentSeqStart() "
        + "should not be called -- only exists to satisfy ResiduesGlyphI "
        + "interface");
  }
  /** @exception IllegalArgumentException. */
  public void setParentSeqEnd(int end) {
    throw new IllegalArgumentException("AlignmentGlyph.setParentSeqEnd() "
        + "should not be called -- only exists to satisfy ResiduesGlyphI "
        + "interface");
  }
  /** @exception IllegalArgumentException. */
  public int getParentSeqStart() {
    throw new IllegalArgumentException("AlignmentGlyph.getParentSeqStart() "
        + "should not be called -- only exists to satisfy ResiduesGlyphI "
        + "interface");
  }
  /** @exception IllegalArgumentException. */
  public int getParentSeqEnd() {
    throw new IllegalArgumentException("AlignmentGlyph.getParentSeqEnd() "
        + "should not be called -- only exists to satisfy ResiduesGlyphI "
        + "interface");
  }

}
