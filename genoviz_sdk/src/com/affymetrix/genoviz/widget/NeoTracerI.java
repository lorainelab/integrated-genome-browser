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

import com.affymetrix.genoviz.widget.neotracer.*;

/**
 * Implementers display a chromatogram from a sequencing machine.
 * The chromatogram is usualy visualized by four traces representing the four bases found in DNA.
 * Bases "called" from the chromatogram can be displayed along the chromatogram.
 * Initializing, selecting, highlighting, cropping, and interrogating are provided.
 *
 * <p> Example:
 * <pre>
 *   URL scfURL = new URL(this.getDocumentBase(), getParameter("scf_file")));
 *   Trace trace = new Trace(scfURL);
 *   NeoTracerI tracer = new NeoTracer();
 *   tracer.setTrace(trace);
 *   tracer.updateWidget(); // display
 *   tracer.centerAtBase(44);
 *   tracer.selectResidues(30,50);
 *   tracer.clearSelect();
 * </pre>
 */
public interface NeoTracerI extends NeoWidgetI  {
	/**
	 * Orientation (Direction) for a trace.
	 * If orientation is FORWARD,
	 * then the trace is shown as originally loaded.
	 *
	 * @see #setDirection
	 */
	public static final int FORWARD = 1;

	/**
	 * Orientation (Direction) for a trace.
	 * If the orientation is REVERSE_COMPLEMENT,
	 * logical reverse complement is performed on the trace.
	 * End coord becomes beg coord and vice versa,
	 * reverse complement of bases are used, and trace colors are also
	 * complemented.
	 *
	 * @see #setDirection
	 */
	public static final int REVERSE_COMPLEMENT = 2;

	/**
	 * component identifier constant for the trace chromogram display
	 * @see #getItems
	 */
	public static final int TRACES = 7000;

	/**
	 * component identifier constant for the base letter display
	 * @see #getItems
	 */
	public static final int BASES = TRACES + 1;

	/**
	 * component identifier constant for the panning axis scroller
	 * @see #getItems
	 */
	public static final int AXIS_SCROLLER = TRACES + 1;

	/**
	 * component identifier constant for the zooming adjustable
	 * @see #getItems
	 */
	public static final int AXIS_ZOOMER = TRACES + 2;

	/**
	 * component identifier constant for the vertical scaling adjustable
	 * @see #getItems
	 */
	public static final int OFFSET_ZOOMER = TRACES + 3;

	/**
	 * component identifier constant for other components not part
	 * of the interface description.
	 * @see #getItems
	 */
	public static final int UNKNOWN = TRACES + 4;

	/**
	 * Adjusts the bounds of the trace and sequence displayed.
	 * Allows clipping of the displayed sequence.
	 *
	 * @param start the integer indicating the starting sample position
	 * @param end  the integer indicating the final sample position.
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
	 * Sets the trace chromogram to display.
	 *
	 * @param trace the Trace to display
	 * @see Trace
	 */
	public void setTrace(TraceI trace);

	/**
	 * @return the Trace set via setTrace.
	 *
	 * @see #setTrace
	 */
	public TraceI getTrace();

	/**
	 * Adjusts the display
	 * so that the base at <code>baseNum</code> is centered.
	 *
	 * @param baseNum the integer index into the sequence of the base
	 *   to center the display.
	 * @return possibility of centering on base.
	 */
	public boolean centerAtBase(int baseNum);

	/**
	 * Highlights a region of the trace
	 * in the coordinates of the trace samples.
	 *
	 * @param start the integer starting sample position.
	 * @param end  the integer ending sample position.
	 */
	public void highlightTrace(int start, int end);

	/**
	 * Highlights a region of bases
	 * in the coordinate system of the trace samples.
	 *
	 * @param start the integer index of the starting base of the sequence.
	 * @param end  the integer index of the final base of the sequence.
	 */
	public void highlightBases(int start, int end);

	/**
	 * Selects a region of bases between the positions <code>base_start</code>
	 * and <code>base_end</code> of the sequence, inclusive.
	 *
	 * @param base_start the integer index of the starting base of the sequence
	 * @param base_end the integer index of the final base of the sequence
	 */
	public void selectResidues(int base_start, int base_end);

	/**
	 * Returns the <code>glyphs</code> beneath the coordinate (xcoord, ycoord)
	 * for the specified component.  The coordinate is in the coordinate
	 * space of the specified component, i.e., not necessarily pixel
	 * coordinates.
	 *
	 * <p> This method is used for event handling.  Mouse event callbacks include
	 * a reference to the component and the position of the event within
	 * that component.  <code>getItems</code> can be used to determine what
	 * items(s) are below the pointer.  The actual event handling mechanism
	 * is implementation specific.
	 *
	 * @param xcoord the double horizontal coordinate of the pointer
	 * @param ycoord the double vertical coordinate of the pointer
	 * @param component the constant component identifier
	 *
	 * @see #TRACES
	 * @see #BASES
	 * @see #AXIS_SCROLLER
	 * @see #AXIS_ZOOMER
	 * @see #OFFSET_ZOOMER
	 * @see #UNKNOWN
	 */
	public Vector<GlyphI> getItems(double xcoord, double ycoord, int component);

	/**
	 * Gets the color used for the background
	 * in the trimmed regions of the trace.
	 */
	public Color getTrimColor();

	/**
	 * Specify the background color for the trimmed portion of the trace.
	 *
	 * @param  col the Color to be used for the background in the trimmed
	 *  portion of the trace.
	 */
	public void setTrimColor(Color col);


	/**
	 * Highlights the left (5') end of the trace.
	 *
	 * @param  i the integer (in trace coordinates, not bases)
	 * specifying where to stop trimming the left end of the trace
	 * @see #setBasesTrimmedLeft
	 */
	public void setLeftTrim(int i);

	/**
	 * Highlights the portion of the trace
	 * corresponding to the first n bases called.
	 * This can be used to show that some base calls should be trimmed
	 * from the 5' end due to low quality.
	 *
	 * @param theBasesTrimmed how many
	 * @see #setLeftTrim
	 */
	public void setBasesTrimmedLeft(int theBasesTrimmed);
	public int getBasesTrimmedLeft();

	/**
	 * Highlights the right (3') end of the trace.
	 *
	 * @param  i the integer (in trace coordinates, not bases)
	 * specifying where to start trimming the right end of the trace
	 * @see #setBasesTrimmedRight
	 */
	public void setRightTrim(int i);

	/**
	 * Highlights the portion of the trace
	 * corresponding to the last n bases called.
	 * This can be used to show that some base calls should be trimmed
	 * from the 3' end due to low quality.
	 *
	 * @param theBasesTrimmed how many
	 * @see #setRightTrim
	 */
	public void setBasesTrimmedRight(int theBasesTrimmed);
	public int getBasesTrimmedRight();

	/**
	 * Sets whether or not the trace and bases for a given dye are visible.
	 *
	 * @param theDye  must be one of A, C, G, T, or N.
	 * @param isVisible  indicates visibility of <code>theDye</code>
	 */
	public void setVisibility(int theDye, boolean isVisible);

	/**
	 * @param theDye must be one of A, C, G, T, or N.
	 * @return the visibility of the specified dye.
	 *
	 * @see #setVisibility
	 */
	public boolean getVisibility(int theDye);


	/**
	 * Sets the trace's orientation.
	 * The property is named Direction rather than Orientation
	 * to avoid confusion with other orientation properties
	 * which distinguish between horizontal and vertical.
	 *
	 * @param orientation
	 * @see #FORWARD
	 * @see #REVERSE_COMPLEMENT
	 */
	public void setDirection(int orientation);

	/**
	 * @return whether or not the trace's orientation is forward.
	 * @see #setDirection
	 */
	public int getDirection();

	public void addRangeListener(NeoRangeListener l);
	public void removeRangeListener(NeoRangeListener l);

	/**
	 * Set the <em>external</em> {@link Adjustable} responsible for scrolling.
	 * The Adjustable should not be in the same container as the tracer.
	 * @param axisid Either NeoWidgetI.X or NeoWidgetI.Y
	 */
	public void setScroller(int axisid, Adjustable adj);

	/**
	 * Set the <em>external</em> {@link Adjustable} responsible for zooming.
	 * The Adjustable should not be in the same container as the tracer.
	 * Overriding NeoContainerWidget.setZoomer()
	 * to prevent zooming base_map in Y direction.
	 * @param axisid Either NeoWidgetI.X or NeoWidgetI.Y
	 */
	public void setZoomer(int axisid, Adjustable adj);

	/** Get the <em>internal</em> {@link Adjustable} responsible for scrolling.  */
	public Adjustable getScroller();

	/**
	 * Set the <em>internal</em> (the one in the same container as the tracer)
	 * {@link Adjustable} responsible for scrolling. If the given Adjustable
	 * isn't an instance of Component, the call will be ignored.
	 */
	public void setScroller (Adjustable scroller);

	/** Get the <em>internal</em> Adjustable responsible for horizontal zooming. */
	public Adjustable getHorizontalZoomer ();

	/**
	 * Set the <em>internal</em> (in the same container as the tracer)
	 * Adjustable responsible for horizontal zooming.  If the given
	 * Adjustable is not an instance of Component, the call will be
	 * ignored.
	 */
	public void setHorizontalZoomer (Adjustable zoomer);

	/** Get the <em>internal</em> Adjustable responsible for vertical zooming. */
	public Adjustable getVerticalZoomer ();

	/**
	 * Set the <em>internal</em> (in the same container as the tracer)
	 * Adjustable responsible for vertical zooming.  If the given
	 * Adjustable is not an instance of Component, the call will be
	 * ignored.
	 */
	public void setVerticalZoomer (Adjustable zoomer);

}
