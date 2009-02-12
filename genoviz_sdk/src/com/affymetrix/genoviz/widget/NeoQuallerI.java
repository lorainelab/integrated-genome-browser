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

import com.affymetrix.genoviz.widget.neoqualler.*;

/**
 * For displaying a sequence of called bases
 * along with the quality scores for each call.
 */
public interface NeoQuallerI extends NeoWidgetI {

	/**
	 * component identifier constant for the quality score histogram display.
	 * @see #getItems
	 */
	public static final int BARS = 8000;

	/**
	 * component identifier constant for the sequence base (letter) display.
	 * @see #getItems
	 */
	public static final int BASES = BARS + 1;

	/**
	 * component identifier constant for the panning axis scroller.
	 * @see #getItems
	 */
	public static final int AXIS_SCROLLER = BARS + 1;

	/**
	 * component identifier constant for the zooming adjustable.
	 * @see #getItems
	 */
	public static final int AXIS_ZOOMER = BARS + 2;

	/**
	 * component identifier constant for the vertical scaling adjustable.
	 * @see #getItems
	 */
	public static final int OFFSET_ZOOMER = BARS + 3;

	/**
	 * component identifier constant for other components not part
	 * of the interface description.
	 * @see #getItems
	 */
	public static final int UNKNOWN = BARS + 4;


	/**
	 * adjusts the bounds of the quality and sequence displayed.
	 * allows clipping of the displayed sequence.
	 * range parameters are in the coordinate system of the sequence,
	 * i.e. (10,20) is the range between position 10 and 20 in the sequence,
	 * inclusive.
	 *
	 * @param start the integer indicating the starting base position
	 * @param end the integer indicating the final base position.
	 */
	public void setRange(int start, int end);

	/**
	 * @return the start of the range set by setRange.
	 */
	public int getRangeStart();

	/**
	 * @return the end of the range set by setRange.
	 */
	public int getRangeEnd();

	/**
	 * Associates a set of confidence scores with this widget.
	 * ReadConfidence contains the sequence bases and a quality score per base.
	 *
	 * @param read_conf the ReadConfidence containing the sequence
	 *                  and quality scores.
	 *
	 * @see ReadConfidence
	 */
	public void setReadConfidence(ReadConfidence read_conf);

	/**
	 * highlights the first n bases called.
	 * This can be used to show that some base calls should be trimmed
	 * from the 5' end due to low quality.
	 *
	 * @param theBasesTrimmed how many
	 */
	public void setBasesTrimmedLeft(int theBasesTrimmed);
	public int getBasesTrimmedLeft();

	/**
	 * highlights last n bases called.
	 * This can be used to show that some base calls should be trimmed
	 * from the 3' end due to low quality.
	 *
	 * @param theBasesTrimmed how many
	 */
	public void setBasesTrimmedRight(int theBasesTrimmed);
	public int getBasesTrimmedRight();

	/**
	 * adjusts the display
	 * so that the base at <code>baseNum</code> is centered.
	 *
	 * @param baseNum the integer index into the sequence of the base
	 *                to center the display.
	 */
	public void centerAtBase(int baseNum);

	/**
	 * highlights a region of the quality display.
	 *
	 * @param start the integer starting base position
	 * @param end   the integer ending base position
	 *
	 */
	public void highlightBars(int start, int end);

	/**
	 * highlights a region of bases.
	 *
	 * @param start the integer index of the starting base of the sequence
	 * @param end   the integer index of the final base of the sequence
	 */
	public void highlightBases(int start, int end);

	/**
	 * selects a region of bases.
	 *
	 * @param start the integer index of the starting base of the sequence
	 * @param end   the integer index of the final base of the sequence
	 */
	public void selectBases(int start, int end);

	/**
	 * Clears the bases selected in calls to selectBases.
	 *
	 * @see #selectBases
	 */
	public void clearSelection();

	/**
	 * returns the <code>Glyphs</code> beneath the coordinate
	 * (xcoord, ycoord) for the specified component.  The coordinate
	 * is in the coordinate space of the specified component, i.e., not
	 * necessarily pixel coordinates.
	 *
	 * <p> This method is used for event handling.
	 * Mouse event callbacks include a reference to the
	 * component and the position of the event within that component.
	 * <code>getItems</code> can be used to determine what item(s) are
	 * below the pointer.  The actual event handling mechanism is
	 * implementation specific.
	 *
	 * @param xcoord the double horizontal coordinate of the pointer
	 * @param ycoord the double vertical coordinate of the pointer
	 * @param component the constant component identifier
	 *
	 * @see #BARS
	 * @see #BASES
	 * @see #AXIS_SCROLLER
	 * @see #AXIS_ZOOMER
	 * @see #OFFSET_ZOOMER
	 * @see #UNKNOWN
	 *
	 * @return a Vector of one or more glyphs below the point.
	 */
	public Vector<GlyphI> getItems(double xcoord, double ycoord, int component);


	public void zoomRange(double scale);
	public void scrollRange(double offset);

	public void addRangeListener(NeoRangeListener l);
	public void removeRangeListener(NeoRangeListener l);

}
