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

package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.style.GraphState;
import java.awt.*;
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.widget.NeoMap;

import com.affymetrix.genometryImpl.util.FloatList;
import com.affymetrix.genometryImpl.util.IntList;

public class GlyphSummarizer {
  public static boolean USE_TIMER = true;
  boolean filter_zeros = false;
  int normalizedMax = 100;  // CURRENTLY NOT USED

  // scaling factor -- scale to apply to hit counts to get height of summary at any point
  float scale_factor = 10.0f;
  Color glyph_color = Color.blue;

  public GlyphSummarizer() { }
  public GlyphSummarizer(int normMax) {
    normalizedMax = normMax;
  }

  public float getScaleFactor() { return scale_factor; }
  public void setScaleFactor(float scale) {
    this.scale_factor = scale;
  }

  public void setFilterZeros(boolean b) {
    filter_zeros = b;
  }
  public boolean getFilterZeros() { return filter_zeros; }

  public void setColor(Color col) {
    glyph_color = col;
  }
  public Color getColor() { return glyph_color; }

  /**
   *  Descends into parent's descendants, collecting all leaf glyphs and
   *    creating a summary over the leafs.
   */
  public GlyphI getSummaryGlyph(GlyphI parent, NeoMap map) {
    Vector vec = new Vector();
    collectLeafs(parent, vec);
    return getSummaryGlyph(vec, map);
  }

  public GlyphI getOldSummaryGlyph(GlyphI parent) {
    Vector vec = new Vector();
    collectLeafs(parent, vec);
    return getOldSummaryGlyph(vec);
  }

  public void collectLeafs(GlyphI gl, Vector vec) {
    if ((gl.getChildren() == null) ||
	(gl.getChildren().size() <= 0)) {
      vec.add(gl);
      return;
    }
    else {
      Vector children = gl.getChildren();
      int childCount = children.size();
      for (int i=0; i<childCount; i++) {
	GlyphI child = (GlyphI)children.elementAt(i);
	collectLeafs(child, vec);
      }
    }
  }

  public GlyphI getSummaryGlyph(java.util.List glyphsToSum, NeoMap map) {
    System.out.println("NewGlyphSummarizer: starting to summarize glyphs");

    com.affymetrix.genoviz.util.Timer tim = null;
    if (USE_TIMER)  { tim = new com.affymetrix.genoviz.util.Timer(); tim.start(); }
    int glyph_num = glyphsToSum.size();
    int[] starts = new int[glyph_num];
    int[] stops = new int[glyph_num];
    for (int i=0; i<glyph_num; i++) {
      GlyphI gl = (GlyphI)glyphsToSum.get(i);
      Rectangle2D cbox = gl.getCoordBox();
      starts[i] = (int)cbox.x;
      stops[i] = (int)(cbox.x + cbox.width);
    }
    Arrays.sort(starts);
    Arrays.sort(stops);
    if (USE_TIMER) {
      System.out.println("newGlyphSummarizer -- all sorted, time taken: " + tim.read()/1000f);
      tim.start();
    }
    int starts_index = 0;
    int stops_index = 0;
    int depth = 0;
    int max_depth = 0;
    // initializing capacity of sum_starts and sum_stops to max that could theoretically be
    //   needed, though likely won't fill it
    IntList transition_xpos = new IntList(glyph_num * 2);
    FloatList transition_ypos = new FloatList(glyph_num * 2);
    int prev_transition = 0;
    int transitions = 0;
    while ((starts_index < glyph_num) && (stops_index < glyph_num)) {
      // figure out whether next position is a start, stop, or both
      int next_start = starts[starts_index];
      int next_stop = stops[stops_index];
      int next_transition = Math.min(next_start, next_stop);
      // note that by design, if (next_start == next_stop), then both of the following
      //    conditionals will execute:
      if (next_start <= next_stop) {
	while (starts[starts_index] == next_start) {
	  depth++;
	  starts_index++;
	  if (starts_index >= glyph_num) { break; }
	}
	// while loop goes one over, so undo last one?
	//	depth--;
	//	starts_index--;
      }
      if (next_start >= next_stop) {
	while (stops[stops_index] == next_stop) {
	  depth--;
	  stops_index++;
	  if (stops_index >= glyph_num) { break; }
	}
	// while loop goes one over, so undo last one?
	//	depth++;
	//	stops_index--;
      }

      transition_xpos.add(next_transition);
      transition_ypos.add(depth);
      transitions++;
      max_depth = Math.max(depth, max_depth);
    }
    GraphSymFloatWithTemporaryState xxx = 
        new GraphSymFloatWithTemporaryState(transition_xpos.copyToArray(), transition_ypos.copyToArray(), null);
    SmartGraphGlyph sgg = new SmartGraphGlyph(xxx, xxx.getGraphState());
    Rectangle2D cbox = map.getCoordBounds();
//    sgg.setPointCoords(transition_xpos.copyToArray(), transition_ypos.copyToArray());
    sgg.setGraphStyle(GraphGlyph.STAIRSTEP_GRAPH);
    sgg.setColor(glyph_color);
    sgg.setCoords(cbox.x, 0, cbox.width, Math.max(max_depth*3, 50));
    if (USE_TIMER) {
      System.out.println("newGlyphSummarizer done -- time taken after sort: " + tim.read()/1000f);
    }
    System.out.println("total transitions: " + transition_xpos.size());
    System.out.println("total transitions another way: " + transitions);
    return sgg;
  }

  //  public GlyphI getSummaryGlyph(Collection glyphsToSummarize, int depthToCheck) {
  public GlyphI getOldSummaryGlyph(Collection glyphsToSummarize) {
    System.out.println("GlyphSummarizer: starting to summarize glyphs");
    com.affymetrix.genoviz.util.Timer tim = null;
    if (USE_TIMER)  { tim = new com.affymetrix.genoviz.util.Timer(); tim.start(); }
    GlyphI summaryGlyph = new StretchContainerGlyph();

    /**
     * 1) Construct a list of all the transition points (every edge of every
     *    glyph at the proper depth, excluding redundant edges)
     *    [by convention, first element of array is first edge _into_ a glyph]
     */

    // The list to keep transition points in --
    //   using an array instead of Collection so don't have to objectify the ints,
    //   but also checking array bounds to stretch if needed...
    int[] transitions = new int[glyphsToSummarize.size() * 2];
    Iterator iter = glyphsToSummarize.iterator();
    int index = 0;
    while (iter.hasNext()) {
      GlyphI gl = (GlyphI)iter.next();

      // stretching array if exceed bounds
      /*
      if (index >= transitions.length) {
	int[] newarray = new int[transitions.length + 100];
	System.arraycopy(transitions, 0, newarray, 0, transitions.length);
	transiions = newarray;
      }
      */
      Rectangle2D cbox = gl.getCoordBox();
      transitions[index] = (int)cbox.x;
      index++;
      transitions[index] = (int)(cbox.x + cbox.width);
      index++;
    }

    // sort array
    Arrays.sort(transitions);
    // construct new array based on transitions but with no redundancies
    int[] temp_trans = new int[transitions.length];
    int previous = transitions[0];
    temp_trans[0] = previous;
    int uindex = 1;
    for (int i=1; i<transitions.length; i++) {
      int current = transitions[i];
      if (current != previous) {
	temp_trans[uindex] = current;
	previous = current;
	uindex++;
      }
    }
    int[] unique_transitions = new int[uindex];
    System.arraycopy(temp_trans, 0, unique_transitions, 0, uindex);
    int segCount = uindex - 1;

    if (USE_TIMER) {
      System.out.println("GlyphSummarizer -- all sorted, time taken: " + tim.read()/1000f);
      tim.start();
    }

    for (int i=0; i<segCount; i++) {
      /**
       * 2) Construct a glyph summarySegment for every sequential pair of transition points,
       *    with x = array[i] and width = array[i+1] - array[i],
       *    and y = FLOAT_MIN/2 and height = FLOAT_MAX (???)
       */
      int segStart = unique_transitions[i];
      int segEnd = unique_transitions[i+1];

      GlyphI newgl = new FillRectGlyph();
      newgl.setColor(glyph_color);

      /**
       * 3) Query every glyph in the glyphsToSummarize vector (or their children at the proper
       *    depth if depthToCheck > 0) for intersection with summarySegment glyph,
       *    an tally up hits
       */
      int hitCount = 0;
      iter = glyphsToSummarize.iterator();
      while (iter.hasNext()) {
	GlyphI gl = (GlyphI)iter.next();
	Rectangle2D cbox = gl.getCoordBox();
	int glStart = (int)cbox.x;
	int glEnd = (int)(cbox.x + cbox.width);

	// if !(segEnd <= glStart || segStart >= glEnd) then there is intersection
	if (! (segEnd <= glStart || segStart >= glEnd)) {
	  hitCount++;
	}
      }

      /**
       * 4) reset y = 0 and height = # of hits
       */
      // if want to filter out regions with no hits, uncomment out conditional
      if ((!filter_zeros) || (hitCount > 0))  {
	newgl.setCoords(segStart, -hitCount*scale_factor, segEnd - segStart, hitCount*scale_factor);
	summaryGlyph.addChild(newgl);
      }
    }

    /**
     * 5) normalize height somehow...
     *  NOT YET IMPLEMENTED
     */
    Vector ch = summaryGlyph.getChildren();
    System.out.println("summary glyph children: " + ch.size());

    if (USE_TIMER) {
      System.out.println("GlyphSummarizer done -- time taken after sort: " + tim.read()/1000f);
    }
    return summaryGlyph;
  }
}

