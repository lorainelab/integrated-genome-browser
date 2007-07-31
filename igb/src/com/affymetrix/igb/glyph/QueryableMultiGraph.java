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

import java.awt.*;
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;

/**
 *  A subclass of MultiGraph that allows for
 *  "box" queries to select/highlight graphs that meet
 *   query parameters.
 */
public class QueryableMultiGraph extends MultiGraph  {

  java.util.List query_boxes = new ArrayList();
  Color selection_color = Color.red;
  Color querybox_color = Color.blue;
  Color unselected_color = Color.gray;

  public void addQueryBox(Rectangle2D box) {
    GlyphI boxglyph = new OutlineRectGlyph();
    //    GlyphI boxglyph = new FillRectGlyph();
    boxglyph.setCoordBox(box);
    boxglyph.setColor(querybox_color);
    query_boxes.add(boxglyph);
    addChild(boxglyph);
    selectGraphsByQueryBox();
  }

  public void clearQueryBoxes() {
    for (int i=0; i<query_boxes.size(); i++) {
      GlyphI boxgl = (GlyphI)query_boxes.get(i);
      removeChild(boxgl);
    }
    query_boxes.clear();
  }

  /**
   *  Highlight graphs that meet criteria specified in query boxes.
   *<pre>
   *  Query boxes are combined in a logical AND
   *  For each query box:
   *     For each graph satisfying all query boxes so far:
   *        Binary search followed by scan to find xcoord index bounds within the graph points
   *        For each point within the the xcoords bounds of the query box:
   *            test whether point ycoord is within query box ybounds
   *</pre>
   */
  public void selectGraphsByQueryBox() {
    int graph_num = graphs.size();
    int qbox_num = query_boxes.size();
    GRAPH_LOOP:
    for (int i=0; i<graph_num; i++) {
      GraphGlyph graph = (GraphGlyph)graphs.get(i);
      boolean graph_satisfies_query = true;
      QUERYBOX_LOOP:
      for (int k=0; k<qbox_num; k++) {
	GlyphI boxgl = (GlyphI)query_boxes.get(k);
	Rectangle2D qbox = boxgl.getCoordBox();
	int xmin = (int)qbox.x;
	int xmax = (int)(qbox.x + qbox.width);
	float ymin = (float)qbox.y;
	float ymax = (float)(qbox.y + qbox.height);
	// binary search to find index of first point that overlaps xbounds of query box
	int[] gxcoords = graph.getXCoords();
	//float[] gycoords = graph.getYCoords();
	int point_beg_index = Arrays.binarySearch(gxcoords, xmin);
	if (point_beg_index < 0) {
	  // want point_beg_index to be index of min xcoord >= xmin
	  //  (insertion point - 1)  [as defined in Arrays.binarySearch() docs]
	  point_beg_index = (-point_beg_index -1);
	}
	int pindex = point_beg_index;
	POINT_LOOP:
	while ((pindex < gxcoords.length-1) && (gxcoords[pindex] < xmax)) {
	  float yval = (float) graph.graf.getGraphYCoord(pindex);
	  if ((yval < ymin) || (yval > ymax)) {
	    graph_satisfies_query = false;
	    break QUERYBOX_LOOP;
	  }
	  pindex++;
	}
      }
      if (graph_satisfies_query) {
	graph.setColor(Color.red);
	graph.getScene().toFrontOfSiblings(graph);
      }
      else {
	graph.setColor(Color.gray);
      }
    }

    for (int i=0; i<stat_graphs.size(); i++) {
      GlyphI gl = (GlyphI)stat_graphs.get(i);
      gl.getScene().toFrontOfSiblings(gl);
    }
    for (int i=0; i<qbox_num; i++) {
      GlyphI gl = (GlyphI)query_boxes.get(i);
      gl.getScene().toFrontOfSiblings(gl);
    }
  }

}
