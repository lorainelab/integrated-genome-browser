/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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
 *  A graph that holds a set of graphs and contains average, min, and max for the whole set.
 *  This assumes that all graphs in the multigraph have the same xcoords!
 *
 *  For building "composite" graphs from graph slices, see CompositeGraphSym
 *       and CompositeGraphGlyph instead
 */
public class MultiGraph extends SmartGraphGlyph {
  java.util.List graphs = new ArrayList();
  boolean all_graphs_added = false;
  SmartGraphGlyph avg_graph;
  SmartGraphGlyph min_graph;
  SmartGraphGlyph max_graph;
  java.util.List stat_graphs = new ArrayList();
  LinearTransform childtrans = new LinearTransform();
  Rectangle2D childbox = new Rectangle2D();

  public MultiGraph() {
    super(null, null, null);
    point_min_ycoord = Float.POSITIVE_INFINITY;
    point_max_ycoord = Float.NEGATIVE_INFINITY;
  }

  public void addGraph(GraphGlyph gl) {
    if (all_graphs_added) {
      throw new RuntimeException("Cannot add more graphs to MultiGraph after prepMultiGraph() is called!");
    }
    else {
      addChild(gl);
      gl.setShowHandle(false);
      graphs.add(gl);
      point_min_ycoord = Math.min(gl.getGraphMinY(), point_min_ycoord);
      point_max_ycoord = Math.max(gl.getGraphMaxY(), point_max_ycoord);
    }
  }

  /**
   *  Calculate average graph.
   */
  public void prepMultiGraph() {
    all_graphs_added = true;
    // calculate a new graph which is the average of all the graphs added via addGraph()
    //   this will get added as a child of the MultiGraph (so it gets rendered
    //   via standard draw(), etc.), but _not_ added to graphs list
    int shared_xcoords[] = ((GraphGlyph)graphs.get(0)).getXCoords();
    int num_points = shared_xcoords.length;
    int num_graphs = graphs.size();
    //    double[][] ycoords_array = new double[num_graphs];
    float avg_ycoords[] = new float[num_points];
    float min_ycoords[] = new float[num_points];
    float max_ycoords[] = new float[num_points];
    for (int i=0; i<num_points; i++) {
      float yavg = 0;
      float ymin = Float.POSITIVE_INFINITY;
      float ymax = Float.NEGATIVE_INFINITY;
      for (int k=0; k<num_graphs; k++) {
	float yval = (((GraphGlyph)graphs.get(k)).getYCoords())[i];
	yavg += yval;
	ymin = Math.min(ymin, yval);
	ymax = Math.max(ymax, yval);
      }
      yavg = yavg / (float)num_graphs;
      avg_ycoords[i] = yavg;
      min_ycoords[i] = ymin;
      max_ycoords[i] = ymax;
    }

    GraphState avg_gstate = GraphState.getTemporaryGraphState();
    GraphState min_gstate = GraphState.getTemporaryGraphState();
    GraphState max_gstate = GraphState.getTemporaryGraphState();
    avg_graph = new SmartGraphGlyph(shared_xcoords, avg_ycoords, avg_gstate);
    min_graph = new SmartGraphGlyph(shared_xcoords, min_ycoords, min_gstate);
    max_graph = new SmartGraphGlyph(shared_xcoords, max_ycoords, max_gstate);
    stat_graphs.add(avg_graph);
    stat_graphs.add(min_graph);
    stat_graphs.add(max_graph);

    for (int i=0; i<stat_graphs.size(); i++) {
      SmartGraphGlyph sgg = (SmartGraphGlyph)stat_graphs.get(i);
      // sgg.setFasterDraw(true);
      // sgg.setCalcCache(true);
      sgg.setSelectable(false);

      sgg.setGraphStyle(SmartGraphGlyph.LINE_GRAPH);
      sgg.setColor(Color.black);
      sgg.setShowHandle(false);
      sgg.setCoords(coordbox.x, coordbox.y, coordbox.width, coordbox.height);
    }

//    avg_graph.setPointCoords(shared_xcoords, avg_ycoords);
//  min_graph.setPointCoords(shared_xcoords, min_ycoords);
//    max_graph.setPointCoords(shared_xcoords, max_ycoords);

    avg_graph.setColor(Color.black);
    min_graph.setColor(Color.black);
    max_graph.setColor(Color.black);

    this.addChild(min_graph);
    this.addChild(max_graph);
    this.addChild(avg_graph);

  }


  public void removeGraph(GraphGlyph gl) {
    System.err.println("Currently cannot remove a child graph from a MultiGraph");
    System.exit(0);
  }

  public java.util.List getGraphs() { return graphs; }

  public void draw(ViewI view) {
    // do nothing -- let children handle drawing...
    drawHandle(view);
  }

  public void setPointCoords(int xcoords[], float ycoords[]) {
    System.err.println("Cannot call setPointCoords() on a MultiGraph!!!");
    System.exit(0);
  }

  public void setVisibleMaxY(float ymax) {
    super.setVisibleMaxY(ymax);
    for (int i=0; i<graphs.size(); i++) {
      GraphGlyph gr = (GraphGlyph)graphs.get(i);
      gr.setVisibleMaxY(ymax);
    }
    for (int i=0; i<stat_graphs.size(); i++) {
      GraphGlyph gr = (GraphGlyph)stat_graphs.get(i);
      gr.setVisibleMaxY(ymax);
    }
  }

  public void setVisibleMinY(float ymin) {
    super.setVisibleMinY(ymin);
    for (int i=0; i<graphs.size(); i++) {
      GraphGlyph gr = (GraphGlyph)graphs.get(i);
      gr.setVisibleMinY(ymin);
    }
    for (int i=0; i<stat_graphs.size(); i++) {
      GraphGlyph gr = (GraphGlyph)stat_graphs.get(i);
      gr.setVisibleMinY(ymin);
    }
  }

  public void setGraphStyle(int type) {
    super.setGraphStyle(type);
    for (int i=0; i<graphs.size(); i++) {
      GraphGlyph gr = (GraphGlyph)graphs.get(i);
      gr.setGraphStyle(type);
      if (type == MINMAXAVG) { gr.setVisibility(false); }
      else { gr.setVisibility(true); }
    }

  }


  /**
   *  Subclassing to avoid movement of non-graph children.
   *    (which at the moment are query boxes (for QueryableMultiGraphs))
   **/
  public void moveRelative(double diffx, double diffy) {
    //    System.out.println("MultiGraph.moveRelative() called");
    coordbox.x += diffx;
    coordbox.y += diffy;
    //    super.moveRelative(diffx, diffy);
    //    super.moveRelative(xdelta, ydelta);
    if (children != null) {
      int numchildren = children.size();
      for (int i=0; i<numchildren; i++) {
	//        ((GlyphI)children.elementAt(i)).moveRelative(diffx, diffy);
	GlyphI child = (GlyphI)children.elementAt(i);
	if (child instanceof GraphGlyph) {
	  child.moveRelative(diffx, diffy);
	}
      }
    }
    state.getTierStyle().setHeight(coordbox.height);
    state.getTierStyle().setY(coordbox.y);
    if (xcoords != null && mutable_xcoords && diffx != 0.0f) {
      int maxi = xcoords.length;
      for (int i=0; i<maxi; i++) {
	xcoords[i] += diffx;
      }
    }
  }


  public void moveAbsolute(double x, double y) {
    //    System.out.println("MultiGraph.moveAbsolute() called");
    super.moveAbsolute(x, y);
  }

  public void setCoords(double x, double y, double width, double height) {
    //    System.out.println("MultiGraph.setCoords() called");
    super.setCoords(x, y, width, height);
    for (int i=0; i<graphs.size(); i++) {
      GlyphI gl = (GlyphI)graphs.get(i);
      gl.setCoords(x, y, width, height);
    }
    for (int i=0; i<stat_graphs.size(); i++) {
      GlyphI gl = (GlyphI)stat_graphs.get(i);
      gl.setCoords(x, y, width, height);
    }
  }

  /**
   *  Temporarily changing view's transform for drawing children.
   *  if child is itself a graph, then it can handle it's own transformations
   *  if child is not a graph, then use modified transform.
   */
  public void drawChildren(ViewI view) {
    LinearTransform vtrans = (LinearTransform)view.getTransform();
    Rectangle2D vbox = view.getCoordBox();
    childtrans.copyTransform(vtrans);
    getChildTransform(view, childtrans);
    childbox.reshape(coordbox.x, getVisibleMinY(), coordbox.width, getVisibleMaxY() - getVisibleMinY());
    if (children != null)  {
      GlyphI child;
      int numChildren = children.size();
      for ( int i = 0; i < numChildren; i++ ) {
	child = (GlyphI) children.elementAt( i );
	if (!(child instanceof TransientGlyph) || drawTransients()) {
	  if (child instanceof GraphGlyph) {
	    child.drawTraversal(view);
	  }
	  else {
	    view.setTransform(childtrans);
	    view.setCoordBox(childbox);
	    //	    ((Glyph)child).DEBUG_DRAW = true;
	    //	    if (child.isVisible()) {
	    //	      System.out.println("child within view: " + child.withinView(view) + ": " + child);
	    //	    }
	    child.drawTraversal(view);
	    //	    ((Glyph)child).DEBUG_DRAW = false;
	    view.setTransform(vtrans);
	    view.setCoordBox(vbox);
	  }
	}
      }
    }
  }

}
