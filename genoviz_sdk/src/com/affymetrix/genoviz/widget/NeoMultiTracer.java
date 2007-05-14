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
import java.awt.event.*;
import java.util.*;

import com.affymetrix.genoviz.awt.*;
import com.affymetrix.genoviz.event.NeoBaseSelectListener;
import com.affymetrix.genoviz.event.NeoBaseSelectEvent;

import com.affymetrix.genoviz.datamodel.*;

/**
 * NeoMultiTracer acts as a container for multiple NeoTracers.
 * It takes a consensus sequence and alignes the traces
 * based on how they fit into the consensus.
 */
public class NeoMultiTracer extends Container
  implements NeoBaseSelectListener, AdjustmentListener
{
  private Vector<NeoTracer> tracers; // vector of NeoTracers
  private Sequence cons_bases; // consensus to be added to NeoTracers
  private Mapping cons_aligner;
  private Panel panel;
  private NeoScrollbar hscroll;
  private NeoScrollbar vzoom;

  private NeoScrollbar slider;

  private int current_cons_index = 0;
  private Range visible_cons_range = new Range( 0, 0 );

  public NeoMultiTracer() {
    this.setLayout(new BorderLayout() );

    tracers = new Vector<NeoTracer>();

    panel = new NeoPanel();
    this.add( panel, BorderLayout.CENTER );

    vzoom = new NeoScrollbar(NeoScrollbar.VERTICAL);
    this.add( vzoom, BorderLayout.WEST );

    slider = new NeoScrollbar(NeoScrollbar.HORIZONTAL);
    this.add (slider, BorderLayout.SOUTH );
    slider.addAdjustmentListener (this);
  }

  /**
   * Add Consensus sequence.
   * This <strong>must</strong> be done before addTracer.
   */
  public void setConsensus( Sequence consensus, Mapping aligner ) {
    this.cons_bases = consensus;
    this.cons_aligner = aligner;
  }


  /**
   * Add a NeoTracer to the container
   */
  public void addTracer( NeoTracer tracer, Mapping trace_aligner ) {

    tracers.addElement( tracer );
    tracer.setRangeZoomer( vzoom );
    tracer.addBaseSelectListener( this );
    tracer.setConsensus( cons_bases, cons_aligner, trace_aligner, null );
    tracer.padCoordBox();

    panel.setLayout( new GridLayout( tracers.size(), 1 ) );
    panel.add( (Component)tracer );

    centerAllOnConsensusBase( 0 );
  }

  public void removeTracers() {
    panel.removeAll();
    tracers.removeAllElements();
  }

  public void setSize( int w, int h ) {
    super.setSize( w, h );
  }


  /** Implements NeoBaseSelectListener. */
  public void baseSelected( NeoBaseSelectEvent event ) {
    centerAllOnBase( event.getSelectedIndex(), (NeoTracer)(event.getSource()) );
  }

  /**
   * Align all the NeoTracers to a particular base.
   * @param base_index is the position of the base on tracer.
   * @param tracer is the context for base_index.
   */
  public void centerAllOnBase ( int base_index, NeoTracer tracer ) {
    int axis_index = tracer.mapToAxisPos( base_index );
    centerAllOnConsensusBase( axis_index );
  }

  /** Need to get the consensus base indices at the edges of the screen. */
  public void centerAllOnConsensusBase( int axis_index ) {
    Vector<NeoTracer> centered_left  = new Vector<NeoTracer>(); // centered, entire left covered
    Vector<NeoTracer> centered_right  = new Vector<NeoTracer>();  // centered, entire right covered
    Vector<NeoTracer> non_centered_left = new Vector<NeoTracer>();  // centering failed, some trace on left
    Vector<NeoTracer> non_centered_right = new Vector<NeoTracer>(); // '', some trace on right
    Vector coverage = new Vector();

    int max_end = Integer.MIN_VALUE; // used to order the centered_rights
    int min_beg = Integer.MAX_VALUE; // used to order the centered_lefts

    current_cons_index = axis_index;
    // in case there is no coverage, approximate new visible range from width of last range.
    int visible_cons_width = visible_cons_range.end - visible_cons_range.beg;
    visible_cons_range.beg = axis_index - (int)( visible_cons_width/2 );
    visible_cons_range.end = axis_index + visible_cons_width - (int)( visible_cons_width/2 );

    for (NeoTracer t : tracers ) {
      Range r = t.getVisibleBaseRange(); // do I need to shift coord box by hand before calling this???
      int base_index = t.mapFromAxisPos( axis_index );
      if( t.centerAtBase( base_index ) ) { // returns true if centering successful
        if( r.beg != -1 ) {
          if( r.beg < min_beg ) { // trace that is farthest to the left gets first spot
            min_beg = r.beg;
            centered_left.insertElementAt( t, 0 );
          }
          else {
            centered_left.addElement(t);
          }
          visible_cons_range.beg = t.mapToAxisPos( r.beg );
        }
        if( r.end != -1 ) {
          if( r.end > max_end ) { // trace that is farthest to the right gets first spot
            max_end = r.end;
            centered_right.insertElementAt( t, 0 );
          }
          else {
            centered_right.addElement(t);
          }
          visible_cons_range.end = t.mapToAxisPos( r.end );
        }
        // trace has been centered
        t.updateWidget();
      }
      else { // could not center at axis_index. put in proper bin for aligning later
        if( r.beg == -1 && r.end == -1 ) { // both edges previously off screen
          int trace_beg = t.mapToAxisPos( 0 );
          if( trace_beg > axis_index ) { // trace is to the right of screen
            non_centered_right.addElement( t );
          }
          else { // trace is to left of screen
            non_centered_left.addElement( t );
          }
        }
        else if ( r.beg == -1 ) {
          non_centered_right.addElement( t );
        }
        else if( r.end == -1 ) {
          non_centered_left.addElement( t );
        }
      }
    } // done looping to place or center tracers


    // position tracers only on the right
    Enumeration<NeoTracer> e = non_centered_right.elements();
    while( e.hasMoreElements() ) { // line left edge up with covering tracer
      NeoTracer t = ( NeoTracer )(e.nextElement());

      if( centered_right.size() >= 1 ) {
        // full coverage is guaranteed, unless there is NO overlap at all
        NeoTracer cover = (NeoTracer)centered_right.elementAt( 0 );
        int mappped_base_index = cover.mapFromAxisPos( t.mapToAxisPos(0) );
        int view_point;
        try {
          view_point = cover.getBaseViewPoint( mappped_base_index );
          t.positionBase( 0, view_point );
        }
        catch( ArrayIndexOutOfBoundsException excpt ) { // there is no overlap
          alignTracerToRightSide( t );
        }
        t.updateWidget();
      }
      else { // no guarantied coverage, check for overlap
        Enumeration e2 = centered_left.elements();
        NeoTracer overlapping_tracer = null;
        int mapped_base_index = -1;
        while( e2.hasMoreElements() ) {
          NeoTracer cover = (NeoTracer)( e2.nextElement() );
          mapped_base_index = cover.mapFromAxisPos( t.mapToAxisPos(0) );
          if( cover.getActiveBaseCalls().getBaseCount() > mapped_base_index ) {
            // found overlap
            overlapping_tracer = cover;
            break;
          }
        }
        if( overlapping_tracer != null && mapped_base_index != -1 ) { // sync on overlapping tracer
          int view_point = overlapping_tracer.getBaseViewPoint( mapped_base_index );

          t.positionBase( 0, view_point );
          t.updateWidget();
        }
        else { // there is no overlapping tracer, line up on the edge
          alignTracerToRightSide( t );
          t.updateWidget();
        }
      }
    } // done with non_centered_right tracers

    // do it all over again for the non_centered_left tracers
    e = non_centered_left.elements();
    while( e.hasMoreElements() ) { // line right edge up with covering tracer
      NeoTracer t = ( NeoTracer )(e.nextElement());
      int trace_end_index = t.getActiveBaseCalls().getBaseCount() - 1;
      if( centered_left.size() >= 1 ) {
        // full coverage is guaranteed, unless there is NO overlap at all
        NeoTracer cover = (NeoTracer)centered_left.elementAt( 0 ); // first element is guaranteed to have the most coverage
        int mappped_base_index = cover.mapFromAxisPos( t.mapToAxisPos( trace_end_index ) );
        int view_point;
        try {
          view_point = cover.getBaseViewPoint( mappped_base_index );
          t.positionBase( trace_end_index, view_point );
        }
        catch( ArrayIndexOutOfBoundsException excpt ) { // there is no overlap
          alignTracerToLeftSide( t );
        }
        t.updateWidget();
      }
      else { // no easy coverage, check for overlap
        Enumeration e2 = centered_right.elements();
        NeoTracer overlapping_tracer = null;
        int mapped_base_index = -1;
        while( e2.hasMoreElements() ) {
          NeoTracer cover = (NeoTracer)( e2.nextElement() );
          mapped_base_index = cover.mapFromAxisPos( t.mapToAxisPos( trace_end_index ) );
          if( mapped_base_index >= 0 ) {
            // found overlap
            overlapping_tracer = cover;
            break;
          }
        }
        if( overlapping_tracer != null && mapped_base_index != -1 ) { // sync on overlapping tracer
          int view_point = overlapping_tracer.getBaseViewPoint( mapped_base_index );

          t.positionBase( trace_end_index, view_point );
          t.updateWidget();
        }
        else { // there is no overlapping tracer, line up on the edge
          alignTracerToLeftSide( t );
          t.updateWidget();
        }
      }
    } // done with non_centered_left tracers

  }


  void alignTracerToRightSide( NeoTracer t ) {
    int mapped_base_index = t.mapFromAxisPos( visible_cons_range.end );
    // -1 special code for right side
    if( t.positionBase( mapped_base_index, -1 ) == false ) {
      // if alignment on mapped_base_index is imposible, move trace off screen
      t.positionBase( 0, 1000 ); // TODO change 1000 to something like, visible_width
    }

  }

  void alignTracerToLeftSide( NeoTracer t ) {
    int mapped_base_index = t.mapFromAxisPos( visible_cons_range.beg );
    if( t.positionBase( mapped_base_index, 0 ) == false ) {
      // if alignment on mapped_base_index is imposible, move trace off screen
      t.positionBase( 0, 1000 ); // TODO change 1000 to something like, visible_width
    }
  }

  /**
   * Implements Adjustment Listener.
   * Handling own horizontal scroll events,
   * to allow consensus bases scrolling.
   */
  public void adjustmentValueChanged( AdjustmentEvent event ) {

    // See if the event either didn't come from a NeoScrollbar or
    // didn't come from the slider,

    try {
      if (((NeoScrollbar) event.getAdjustable()) != slider)
        return;
    } catch (ClassCastException e) {
      return;
    }

    if( slider.getValue() != current_cons_index ) {
        centerAllOnConsensusBase( slider.getValue() );
        current_cons_index = slider.getValue();
    }
  }

}
