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

package com.affymetrix.genoviz.widget;

import java.awt.*;
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.event.*;

/**
 * This interface adds functionality for displaying an assembly
 * of multiple sequences.  Initialization, cropping, and alignment
 * modification are provided.<p>
 *
 * Example:<p> <pre>
 * NeoAssemblerI map = new NeoAssembler();
 * map.setResidueColor(Color.white);
 * map.setMatchColor(new Color(80, 80, 80));
 * map.setMisMatchColor(new Color(150, 150, 150));
 * map.setSelectionMethod(NeoMap.ON_MOUSE_DOWN);
 *
 * // Suppose we have an alignment like so:
 * //
 * //               1
 * //     0123456789012345
 * //       ACTG ACTGACT     &lt;&lt; Consensus
 * // Q:  AAACTTGACTGACT     &lt;&lt; a sequence with extra bases
 * // R:      TG ACT         &lt;&lt; a smaller sequence with gaps
 *
 * map.setRange(0, 15);
 *
 * // Indicate the complete consensus sequence
 * // and its start and end in the alignment coordinate system.
 * //  The first "A" aligns at position 2.
 * //  The last "T" aligns at position 13.
 * //  There may be gaps.  These are specified using addAlignedSpan().
 * Object cons_tag = map.setConsensus(2, 13, "ACTGACTGACT");
 *
 * // The sequence from 0..3 in the consensus sequence maps
 * // to positions 2..5 in the alignment coordinate system.
 * map.addAlignedSpan(cons_tag, 0, 3, 2, 5);
 *
 * // Similarly add the second span.
 * map.addAlignedSpan(cons_tag, 4, 10, 7, 13);
 *
 * // Now add the sequences that align to the consensus.
 * // The first sequence extends from 0 to 13.
 * // Gaps and residues can be delayed.
 * Object seq_tag = map.addSequence(0, 13);
 * map.setLabel(seq_tag, "Q");
 * map.setResidues(seq_tag, "AAACTTGACTGACT");
 * map.addAlignedSpan(seq_tag, 0, 13, 0, 13);
 *
 * // Finally, add the smaller sequence with two spans:
 * seq_tag = map.addSequence(4, 9);
 * map.setLabel(seq_tag, "R");
 * map.setResidues(seq_tag, "TGACT");
 * map.addAlignedSpan(seq_tag, 0, 1, 4, 5);
 * map.addAlignedSpan(seq_tag, 2, 4, 7, 9);
 *
 * map.updateWidget();
 *
 * </pre>
 *
 * @version $Id$
 */
public interface NeoAssemblerI extends NeoWidgetI {
	public static final int UNKNOWN_ASSEMBLY = 0;
	public static final int NA_ASSEMBLY = 1;
	public static final int AA_ASSEMBLY = 2;

	public static final int SELECT_ALIGNMENTS = NO_SELECTION + 1;
	public static final int SELECT_RESIDUES = NO_SELECTION + 2;

	/**
	 * component identifier constant for the labels of the individual
	 * sequences of the assembly.
	 * @see #getItems
	 */
	public static final int LABELS = 6000;

	/**
	 * component identifier constant for the consensus sequence.
	 * @see #getItems
	 */
	public static final int CONSENSUS = LABELS + 1;

	/**
	 * component identifier constant for the set of alignment sequences.
	 * @see #getItems
	 */
	public static final int ALIGNMENTS = LABELS + 2;

	/**
	 * component identifier constant for the horizontal panning axis scroller
	 * @see #getItems
	 */
	public static final int AXIS_SCROLLER = LABELS + 3;

	/**
	 * component identifier constant for the vertical panning axis scroller
	 * @see #getItems
	 */
	public static final int OFFSET_SCROLLER = LABELS + 4;

	/**
	 * component identifier constant for other components not part
	 * of the interface description.
	 * @see #getItems
	 */
	public static final int UNKNOWN = LABELS + 5;

	/**
	 * component identifier constant for the consensus label.
	 * @see #getItems
	 */
	public static final int CONSENSUS_LABEL = LABELS + 6;

	/**
	 * adjusts the bounds of the assembly displayed.  in practice
	 * this should be set to range from 0 to one past the length
	 * of the assembly.
	 *
	 * @param start the integer indicating the starting residue position
	 * @param end   the integer indicating the final residue position.
	 */
	public void setRange(int start, int end);

	/**
	 * add a consensus sequence letters starting at position
	 * <code>start</code> and ending at <code>end</code> in the alignment
	 * coordinate system.
	 *
	 * @param start the starting position in the alignment coordinate system.
	 * @param end  the final position in the alignment coordinate system.
	 *
	 * @return a <code>Glyph</code> of the consensus sequence.
	 * Use <code>addAlignedSpan</code> to indicate the alignment of
	 * the consensus sequence to the alignment coordinate system.
	 */
	public GlyphI setConsensus(int start, int end, String residues);

	/**
	 *
	 * add the sequence letters to be associated with <code>seq_tag</code>.
	 * <code>setResidues</code> allows application controlled delayed loading
	 * of sequence data.
	 *
	 * @param seq_tag an Object (GlyphI) to associate a string of letters.
	 * Instances of seq_tag are returned from <code>addSequence</code>.
	 *
	 * @param residues a String of letters
	 *
	 * @return the (possibly new) <code>Glyph</code> for the sequence.
	 *
	 * @see #addSequence
	 */
	public GlyphI setResidues(GlyphI seq_tag, String residues);

	/**
	 * returns the <code>Glyphs</code> beneath the coordinate
	 * (xcoord, ycoord) for the specified component.  The coordinate
	 * is in the coordinate space of the specified component, i.e., not
	 * necessarily pixel coordinates.<p>
	 *
	 *
	 * This method is used for event handling.  Mouse event callbacks include
	 * a reference to the component and the position of the event within
	 * that component.  <code>getItems</code> can be used to determine what
	 * items(s) are below the pointer.  The actual event handling mechanism
	 * is implementation specific.
	 *
	 * @param xcoord the double horizontal coordinate of the pointer
	 * @param ycoord the double vertical coordinate of the pointer
	 * @param component the constant component identifier
	 *
	 * @see #LABELS
	 * @see #CONSENSUS
	 * @see #ALIGNMENTS
	 * @see #AXIS_SCROLLER
	 * @see #OFFSET_SCROLLER
	 * @see #UNKNOWN
	 */
	public Vector<GlyphI> getItems(double xcoord, double ycoord, int component);

	// should probably be in a NeoResidueI interface?
	// since it is also needed in NeoSeq, NeoMap, NeoQualler, NeoTracer
	// at least for NeoAssembler, should throw error if set to anything
	// other than Courier
	/**
	 * sets the font to be used for displaying residue letters.
	 *
	 * @param fnt  the Font to use for residue letters
	 * @see #getResidueFont
	 */
	public void setResidueFont(Font fnt);

	/**
	 * returns the currently set font used for display residue letters.
	 *
	 * @return the <code>Font</code> currently set.
	 * @see #setResidueFont
	 */
	public Font getResidueFont();

	/**
	 * sets the color to be used for displaying residues matching
	 * the consensus sequence.
	 *
	 * @param col  the <code>Color</code> for matching residues
	 * @see #getMatchColor
	 */
	public void setMatchColor(Color col);

	/**
	 * returns the currently set color for displaying residues matching
	 * the consensus sequence.
	 *
	 * @return the <code>Color</code> of matching residues
	 * @see #setMatchColor
	 */
	public Color getMatchColor();


	/**
	 * sets the color to be used for displaying residues <b>not</b> matching
	 * the consensus sequence.
	 *
	 * @param col  the <code>Color</code> for mismatching residues
	 * @see #getMisMatchColor
	 */
	public void setMisMatchColor(Color col);

	/**
	 * returns the currently set color for displaying residues
	 * <b>not</b> matching the consensus sequence.
	 *
	 * @return the <code>Color</code> of mismatching residues
	 * @see #setMisMatchColor
	 */
	public Color getMisMatchColor();

	/**
	 * sets the default color for displaying residue letters.
	 *
	 * @param col  the <code>Color</code> to display residues
	 * @see #getResidueColor
	 */
	public void setResidueColor(Color col);

	/**
	 * returns the currently set default color for displaying residue letters.
	 *
	 * @return the <code>Color</code> for displaying residues
	 * @see #setResidueColor
	 */
	public Color getResidueColor();

	/**
	 * sets the color for displaying the label for each aligned sequence.
	 *
	 * @param col  the label <code>Color</code>
	 * @see #getLabelColor
	 */
	public void setLabelColor(Color col);

	/**
	 * returns the currently set color for displaying the label for each
	 * aligned sequence.
	 *
	 * @return the current label <code>Color</code>
	 * @see #setLabelColor
	 */
	public Color getLabelColor();

	/**
	 * adds a new sequence to the alignment
	 * that extends from <code>start</code> to <code>end</code> in the
	 * alignment coordinate system.  There may be one or more gaps between
	 * these bounds as set by <code>addAlignedSpan</code>.
	 *
	 * @param start the starting position in the alignment coordinate
	 *   system of the first residue of this sequence.
	 * @param end  the final position in the alignment coordinate
	 *   system of the last residue of this sequence.
	 *
	 * @return the <code>Glyph</code> for the sequence.
	 *
	 * @see #addAlignedSpan
	 * @see #setResidues
	 * @see #setLabel
	 */
	public GlyphI addSequence(int start, int end);

	/**
	 * associates a string label with an aligned sequence.  The label is
	 * displayed adjacent to the sequence alignment display.
	 *
	 * @param gl  the <code>GlyphI</code> representing the
	 *   aligned sequence returned from
	 *   <code>addSequence</code>
	 * @param name the <code>String</code> label to display for
	 *   <code>gl</code>
	 * @see #addSequence
	 */
	public GlyphI setLabel(GlyphI gl, String name);

	/**
	 * gets the label of an aligned sequence.
	 *
	 * @param gl the alignment labeled.
	 * @return a String containing the label.
	 */
	public String getLabel(GlyphI gl);

	/*
	 * To Be Done.
	 */
	public GlyphI addUnalignedSpan(GlyphI seq_tag, int seqstart, int seqend,
			int refstart, int refend);

	/**
	 * adds a span to a sequence object.  Each aligned sequence has one
	 * or more ungapped spans.  <code>addAlignedSpan</code> maps the
	 * subsequence from <code>seqstart</code> to <code>seqend</code> to
	 * the alignment coordinate system from <code>alignstart</code> to
	 * <code>alignend</code>.
	 *
	 * @param seq_tag   an Object (GlyphI) to which a new span is added.
	 *                  instances of seq_tag are returned from
	 *                  <code>addSequence</code>.
	 * @param seqstart  the starting residue position in the sequence of this
	 *                  span.
	 * @param seqend    the final residue position in the sequence of this span.
	 * @param refstart  the starting position in the alignment coordinate
	 *                  system of the first residue of this span.
	 * @param refend    the final position in the alignment coordinate
	 *                  system of the last residue of this span.
	 *
	 * @return the <code>Glyph</code> of the span.
	 *
	 * @see #addSequence
	 */
	public GlyphI addAlignedSpan(GlyphI seq_tag, int seqstart, int seqend,
			int refstart, int refend);

	/**
	 * Sets whether or not alignments are to be sorted automatically.
	 *
	 * @param sort <code>true</code> indicates that alignments
	 *   should be automatically sorted.
	 */
	public void setAutoSort(boolean sort);

	/**
	 * returns a <code>boolean</code> indicating whether or not alignments
	 * are automatically sorted.
	 *
	 * @return <code>true</code> if alignments are automatically sorted.
	 */
	public boolean getAutoSort();

	/**
	 *  Pack (or repack) the aligned sequences
	 */
	public void pack();

	/**
	 * allows another object to listen for NeoRangeEvents
	 * generated by implementers.
	 *
	 * @param l the listener.
	 */
	public void addRangeListener(NeoRangeListener l);

	/**
	 * allows another object to stop listening for NeoRangeEvents
	 * generated by implementers.
	 *
	 * @param l the listener.
	 */
	public void removeRangeListener(NeoRangeListener l);

	/** Get the Adjustable responsible for horizontal scrolling */

	public Adjustable getHorizontalScroller ();

	/**
	 * Set the scroller responsible for horizontal scrolling.  If the
	 * given Adjustable is not an instance of Component, the call will
	 * be ignored.
	 */
	public void setHorizontalScroller (Adjustable scroller);

	/** Get the Adjustable responsible for vertical scrolling */

	public Adjustable getVerticalScroller ();

	/**
	 * Set the scroller responsible for vertical scrolling.  If the
	 * given Adjustable is not an instance of Component, the call will
	 * be ignored.
	 */
	public void setVerticalScroller (Adjustable scroller);

	/**
	 * Set an INTERNAL horizontal zoomer.  If the given Adjustable is
	 * not an instance of Component, the call will be ignored.
	 */
	public void setInternalZoomer (Adjustable adjustable);

	/** Get the INTERNAL horizontal zoomer. */
	public Adjustable getInternalZoomer ();

}
