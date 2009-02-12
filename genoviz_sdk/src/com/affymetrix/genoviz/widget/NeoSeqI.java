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

import com.affymetrix.genoviz.awt.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.datamodel.*;
import com.affymetrix.genoviz.util.*;

import com.affymetrix.genoviz.widget.neoseq.*;

/**
 * Implementers can display a sequence of residues as letter codes.
 * Initializing, selecting, highlighting, cropping, and
 * interrogating are provided.<p>
 *
 * Example:<p>
 *
 * <pre>
 * String seq = "AAACGTGGGGGGGGGGGGGGGAAAAAAAAAAAAATTTTTTTTTTTTTTTTTT";
 * NeoSeqI seqview = new NeoSeq();
 * seqview.setSequence(seq);
 * seqview.setStripeColors( { new Color(255,255,255),
 *                            new Color(200,255,200),
 *                            new Color(200,200,200) } );
 * seqview.setStripeOrientation(NeoSeqI.VERTICAL_STRIPES);
 * seqview.setFontName("Courier");
 * seqview.setFontSize(10);
 * seqview.addAnnotation(5,25,Color.magenta);
 * seqview.addAnnotation(40,45,Color.yellow);
 * seqview.highlightResidues(3,30);
 *
 * </pre>
 */
public interface NeoSeqI extends NeoWidgetI, Translatable {

	/**
	 * component identifier constant for the residue letter display.
	 * @see #getItems
	 */
	public static final int RESIDUES = 9000;

	/**
	 * component identifier constant for the numeric position display.
	 * @see #getItems
	 */
	public static final int NUMBERS = RESIDUES + 1;

	/**
	 * component identifier constant for the axis scroller.
	 * @see #getItems
	 */
	public static final int AXIS_SCROLLER = RESIDUES + 2;

	/**
	 * component identifier constant for other components not part
	 * of the interface description.
	 * @see #getItems
	 */
	public static final int UNKNOWN = RESIDUES + 3;


	/**
	 * constant to remove background striping of sequence display.
	 * @see #setStripeOrientation
	 */
	public static final int NO_STRIPES = WrapStripes.NONE;

	/**
	 * constant to add vertical striping of sequence display (default).
	 * @see #setStripeOrientation
	 */
	public static final int VERTICAL_STRIPES = WrapStripes.VERTICAL;

	/**
	 * constant to add horizontal striping of sequence display (default).
	 * @see #setStripeOrientation
	 */
	public static final int HORIZONTAL_STRIPES = WrapStripes.HORIZONTAL;

	/**
	 * scrolls so that a particular residue is visible.
	 * If it is already within range, then no scrolling occurs.
	 * The widget is scrolled just enough to make the residue visible,
	 * and no more.
	 *
	 * @param theResidueIndex points to the residue that must become visible.
	 */
	public void makeResidueVisible(int theResidueIndex);


	/**
	 * scrolls so that the given residue is on the first visible line.
	 *
	 * @param value points to a residue that must be in the top visible line.
	 */
	public void scrollSequence(int value);

	public void setFirstOrdinal (int first);
	public void setSequence(SequenceI seq, int first);

	/**
	 * sets the sequence residues for the display.
	 *
	 * @param seq_string a String containing the residues to be displayed
	 */
	public void setSequence(String seq_string);

	public GlyphI addTextColorAnnotation(int start, int end, Color color);

	/**
	 * add an annotation of a particular color on a specified sub-region.
	 *
	 * @param start  the integer starting coordinate of the annotation.
	 * @param end  the integer ending coordinate of the annotation.
	 * @param color the color of the annotation.
	 * @return       a tag to associate with the annotation for later reference
	 *
	 * @see NeoWidgetI#getColor
	 */
	public GlyphI addAnnotation(int start, int end, Color color);

	/**
	 *  add an outline as an annotation along the sequence.
	 */
	public GlyphI addOutlineAnnotation(int start, int end, Color color);


	/**
	 * remove an annotation from the sequence.
	 *
	 * @param item  the tag associated with the annotation
	 */
	public void removeAnnotation(GlyphI item);

	/**
	 * gets the start of an annotation.
	 *
	 * @param annotation an item added with the addAnnotation method.
	 * @return the start of the annotation.
	 */
	public int getAnnotationStart(GlyphI annotation);

	/**
	 * gets the end of an annotation.
	 *
	 * @param annotation an item added with the addAnnotation method.
	 * @return the end of the annotation.
	 */
	public int getAnnotationEnd(GlyphI annotation);

	/**
	 * gets the range of an annotation.
	 *
	 * @param annotation an item added with the addAnnotation method.
	 * @return the Range (start to end) of the annotation.
	 */
	public Range getAnnotationRange(GlyphI annotation);

	/**
	 *  Get annotation glyphs that overlap a sequence range.
	 *  Note that this method filters out the glyph used for highlighting
	 *  @param start   the start of the range to find overlaps
	 *  @param end     the end of the range to find overlaps
	 */
	public Vector getAnnotationItems(int start, int end);

	/**
	 * highlight the sequence residues
	 * between start and end and creates a selection.
	 *
	 * @param start  the integer starting coordinate of the highlight
	 * @param end  the integer ending coordinate of the highlight
	 *
	 * @see #getSelectedResidues
	 */
	public void highlightResidues(int start, int end);


	/**
	 * sets the sequence residues for the display.
	 *
	 * @param seq  a Sequence containing the residues to be displayed.
	 *
	 * @see Sequence
	 */
	public void setSequence(SequenceI seq);

	/**
	 * returns the index of the residue in the sequence
	 * at the position <code>(xcoord, ycoord)</code> in the display.
	 *
	 * @param xcoord the horizontal offset (column) of the residue
	 * @param ycoord the vertical offset (row) of the residue
	 * @return  the integer index of the residue in the sequence
	 */
	public int getCoordResidue(double xcoord, double ycoord);

	/**
	 * returns the <code>Glyphs</code> beneath the coordinate (xcoord, ycoord)
	 * for the specified component.  The coordinate is in the coordinate
	 * space of the specified component, i.e., not necessarily pixel
	 * coordinates.<p>
	 *
	 * This method is used for event handling.  Mouse event callbacks
	 * include a reference to the component and the position of the event
	 * within that component.  <code>getItems</code> can be used to determine
	 * what item(s) are below the pointer.  The actual event handling
	 * mechanism is implementation specific.
	 *
	 * @param xcoord the double horizontal coordinate of the pointer
	 * @param ycoord the double vertical coordinate of the pointer
	 * @param component the constant component identifier
	 *
	 * @return a Vector of one or more glyphs below the point.
	 *
	 * @see #RESIDUES
	 * @see #NUMBERS
	 * @see #AXIS_SCROLLER
	 * @see #UNKNOWN
	 */
	public Vector<GlyphI> getItems(double xcoord, double ycoord, int component);

	/**
	 * get the Java AWT font of the sequence residues.
	 *
	 * @return  the java.awt.font used to display sequence residues
	 */
	public Font getFont();

	/**
	 * set the display font of the sequence residues.  Proportionate or
	 * variable width fonts can both be specified.
	 *
	 * @param font the Java AWT font to be used to display
	 * sequence residues
	 */
	public void setFont(Font font);

	/**
	 * get the name of the display font of the sequence residues.
	 *
	 * @return the String name of the font to use
	 */
	public String getFontName();

	/**
	 * set the display font of the sequence residues.  Proportionate or
	 * variable width fonts can both be specified.
	 *
	 * @param font the String name of the font to use
	 */
	public void setFontName(String font);

	/**
	 * get the size (in points) of the font used to display sequence residues.
	 *
	 * @return the integer point size of the font
	 */
	public int getFontSize();

	/**
	 * set the font size of the characters used to display the sequence residues.
	 *
	 * @param size the integer point size of the font
	 */
	public void setFontSize(int size);

	/**
	 * indicate the minimum grouping size with respect to line breaking.
	 * Lines will be wrapped such that the number of residues on a line
	 * is a multiple of <code>groupWidth</code>.
	 *
	 * @param groupWidth the integer minimum grouping size
	 */
	public void setResidueMultipleConstraint(int groupWidth);

	/**
	 * retrieves the minimum grouping size with respect to line breaking.
	 *
	 * @see #setResidueMultipleConstraint
	 * @return the integer grouping size
	 */
	public int getResidueMultipleConstraint();

	/**
	 * set the width of the striping.
	 *
	 * @param width the integer width in pixels of the stripe
	 */
	public void setStripeWidth(int width);

	/**
	 * returns the current width of striping.
	 *
	 * @return the integer width of striping.
	 */
	public int getStripeWidth();

	/**
	 * set the orientation of the striping in the display of the sequence residues.
	 * Striping is used as a background coloring of the residues to make viewing
	 * the sequence easier.
	 *
	 * @param orientation  the constant orientation identifier,
	 * either NO_STRIPES, VERTICAL_STRIPES, or
	 * HORIZONTAL_STRIPES.
	 *
	 * @see #NO_STRIPES
	 * @see #VERTICAL_STRIPES
	 * @see #HORIZONTAL_STRIPES
	 */
	public void setStripeOrientation(int orientation);

	/**
	 * returns the current orientation of the striping.
	 *
	 * @return the constant orientation identifier,
	 * either NO_STRIPES, VERTICAL_STRIPES, or HORIZONTAL_STRIPES.
	 */
	public int getStripeOrientation();

	/**
	 * sets the number of pixels between each letter displayed.
	 *
	 * @param size the integer number of pixels between residues
	 */
	public void setSpacing(int size);

	/**
	 * sets the colors to use for striping.
	 * Two or more colors can be specified to alternate among.
	 *
	 * @param colors an array of Colors.  default is
	 *  { Color.white, Color.lightGray }
	 */
	public void setStripeColors(Color[] colors);

	/**
	 * Returns the visible residues.
	 * Returns an empty string ("") if there are no residues.
	 */
	public String getVisibleResidues();

	/**
	 * Returns the selected residues.
	 * Returns an empty string ("") if there is no selection.
	 */
	public String getSelectedResidues();

	/**
	 *  returns true if a range of residues is currently selected.
	 */
	public boolean residuesSelected();

	/**
	 *  determines display options for reverse complement, sequence translation.
	 *  @param type     possible types are
	 *      NUCLEOTIDES, COMPLEMENT, FRAME_ONE, FRAME_TWO, FRAME_THREE,
	 *      FRAME_NEG_ONE, FRAME_NEG_TWO, FRAME_NEG_THREE
	 *  @param show     whether to show sequence as this type or not
	 */
	public void setShow(int type, boolean show);

	/** Set whether or not the numbering should be displayed descending. */
	public void setRevNumbering (boolean revNums);

	/** Get whether or not the numbering will be displayed descending. */
	public boolean getRevNumbering ();

	/**
	 * Method for determining if a Glyph is fully visible and is unobscured.
	 * That is, there are no glyphs on top of this one.
	 *
	 *  @param gl  The glyph that is either unobscured or not.
	 *  @return  whether or not the glyph is unobscured.
	 */
	public boolean isUnObscured(GlyphI gl);

	/*
	 * for NeoSeq, we define isOnTop() to depend on what type of glyph is
	 *   passed as the argument
	 * WrapFontColors only need to be on top of other WrapFontColors
	 * WrapColors only need to be on top of other WrapColors
	 * any other type of glyph currently returns false
	 */
	public boolean isOnTop(GlyphI gl);

	/**
	 * Method for determining if a Glyph is
	 * fully visible.
	 *
	 * @param gl  The glyph that is either fully visible or not.
	 * @return  whether or not the glyph is fully visible.
	 */
	public boolean isFullyWithinView(GlyphI gl);

	/**
	 * Method for determining if a Glyph is at least partially visible.
	 *
	 * @param gl  The glyph that is either partially visible or not.
	 * @return  whether or not the glyph is at least partially visible.
	 */
	public boolean isPartiallyWithinView(GlyphI gl);

	/**
	 * Return the NeoAdjustable responsible for scrolling in the NeoSeqI.
	 * For the current NeoSeq implementation, this can be cast to a
	 * NeoScrollbar or Component to adjust foregrounds and background colors, etc.
	 */
	public Adjustable getScroller();

	/**
	 * Set the adjustable responsible for scrolling in the NeoSeqI.
	 * If the given Adjustable isn't an instance of Component,
	 * the call will be ignored.
	 */
	public void setScroller (Adjustable scroller);

	/**
	 * Sets the Color of the numbers and residues in the NeoSeq.
	 * @param theColor the Color to be used for the residues
	 *                 and the numbers.
	 */
	public void setResidueColor(Color theColor);

	/**
	 * Gets the color of the residues and the numbers.
	 * @return   the Color of the residues and the numbers.
	 */
	public Color getResidueColor();

	/**
	 * Sets the resiudes of the NeoSeq.
	 * @param theResidues  a String of residues
	 */
	public GlyphI setResidues(String theResidues);

	/**
	 * @return a string of Nucleotide or Amino Acid codes.
	 */
	public String getResidues();

	/**
	 * Adds residues to the current sequence.
	 * @param theResidues  a String of residues that are to be
	 *      added to the sequence.
	 */
	public void appendResidues(String theResidues);

	/**
	 * Returns the number of the first selected base.
	 * Returns -1 if there is no selection.
	 *
	 */
	public int getSelectedStart();

	/**
	 * Returns the number of the last selected base.
	 * Returns -1 if there is no selection.
	 *
	 */
	public int getSelectedEnd();

	/**
	 * Removes highlighting of selected range of sequence.
	 */
	public void deselect();

	public Color getResidueFontColor();
	public void setResidueFontColor(Color col);
	public Color getNumberFontColor();
	public void setNumberFontColor(Color col);

	public void addRangeListener(NeoRangeListener l);
	public void removeRangeListener(NeoRangeListener l);

	/**
	 * Add a given ORFSpecs object whose ORF is to be associated with the NeoSeq.
	 */
	public void addORFSpecs (ORFSpecs orf);


	/**
	 * Remove a given ORFSpecs object
	 * whose ORF is to no longer be associated with the NeoSeq.
	 */
	public void removeORFSpecs (ORFSpecs orf);

	/**
	 * Set whether or not translation should only be displayed inside ORFs.
	 * @see #addORFSpecs (ORFSpecs)
	 */
	public void setTranslateOnlyORFs (boolean b);

	/**
	 * Get whether or not translation is only being displayed inside ORFs.
	 * @see #addORFSpecs (ORFSpecs)
	 */
	public boolean isTranslateOnlyORFs ();

}
