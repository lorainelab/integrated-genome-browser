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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.*;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableSeqSymmetry;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.symmetry.SingletonSeqSymmetry;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.Rectangle2D;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.util.FloatList;
import com.affymetrix.igb.util.IntList;

/**
 *  A smarter graph glyph.
 *  It:
 * <ol>
 * <li> improves efficiency of drawing zoomed-out large graphs (via max once-per-pixel drawing)
 * <li> improves efficiency of drawing zoomed-in large graphs (via binary search for draw end-points)
 * <li> allows for summarizing min, max, and average separately for zoomed-out graphs
 * <li> allows for on-the-fly thresholding and "glyphification" of regions that pass threshold
 * </ol>
 *
 *  Also, now trying to further improve efficiency for zoomed-out large graphs via data structure
 *      enhancements.
 *  And, working on drawing y-axis....
 *
 *  ONLY MEANT FOR GRAPHS ON HORIZONTAL MAPS
 */
public class SmartGraphGlyph extends GraphGlyph {
  public boolean NEWDEBUG = false;
  public boolean THRESH_DEBUG = false;

  /*
   *  The x scale at which to transition from MinMaxAvg drawing to Bar drawing
   *   (if graph_style is set to MINMAXAVG)
   *   specified in coords_per_pixel
   *   (note that view's transform scale is expressed differently, in pixels_per_coord...)
   */
  double transition_scale = 100;  // specified in coords_per_pixel
  boolean TRANSITION_TO_BARS = true;
  boolean SHOW_CACHE_INDICATOR = false;
  boolean USE_GRAPH_CACHE = true;
  boolean CALC_GRAPH_CACHE = true;
  boolean CACHE_DIRECT_DRAW = false;

  public static final int MINMAXAVG = 4;
  public static final int MINMAXAVG_BARS = 101;  // NOT YET IMPLEMENTED
  public static final int MINMAXAVG_STAIRSTEP = 102;  // NOT YET IMPLEMENTED


  boolean AVGLINE = true;
  boolean MINMAXBAR = true;
  int thresh_contig_height = 10;  // in pixels, for calculating where to draw thresholded regions
  int thresh_contig_yoffset = 2;  // in pixels, for calculating where to draw thresholded regions
  Rectangle2D thresh_coord_box = new Rectangle2D();  // for calculating where to draw thresholded regions
  Rectangle thresh_pix_box = new Rectangle();  // for calculating where to draw thresholded regions

  Color lighter;
  Color darker;
  Color thresh_color;

  // still need to make thresh_glyph draw as a fixed-pixel (1 or 2) line instead of as a variable-pixel fillRect...
  ThreshGlyph thresh_glyph = new ThreshGlyph();

  // eventually want hierarchy of graph compression caches at multiple compression levels...
  //  GraphCache[] caches;
  //  GraphCache graph_cache;
  //  GraphCache2 graph_cache;
  ArrayList caches = new ArrayList();  // the hiearchy of graph caches (well really just one for now...)
  //  int compression_level = 100;  // average # of points per entry in flat graph compression cache
  int compression_level = 20;  // average # of points per entry in flat graph compression cache

  /*
   *  Want to eventually support many drawing options:
   *
   *    The simple ones (just like GraphGlyph)
   *      SIMPLE_LINE_GRAPH
   *      SIMPLE_BAR_GRAPH
   *      SIMPLE_DOT_GRAPH
   *
   *    AVG_LINE_GRAPH
   *    AVG_BAR_GRAPH
   *    AVG_DOT_GRAPH
   *    AVG_HIDDEN
   *
   *    MIN_MAX_BAR
   *    MIN_MAX_HIDDEN
   *
   */

  /*
   *  may need to try a new approach to minimize switching graphics color
   *  (testing is showing that graph draws ~5x _slower_ if have to alternate colors with
   *    every graphics draw call...)
   *  Therefore want to separate out for example the minmax bar drawing from the average line
   *     drawing since they are different colors.  Could do this with two passes, but then
   *     redoing a lot of summing, etc.
   *    As an alternative, may try caching the average position for each pixel
   *       (as doubles in graph coords, or maybe in pixel positions)
   *
   */
  int pixel_cache[];

  public SmartGraphGlyph(int[] xcoords, float[] ycoords, GraphState gstate) {
    this(xcoords, null, ycoords, gstate);
  }
  
  public SmartGraphGlyph(int[] xcoords, int[] wcoords, float[] ycoords, GraphState gstate) {
    super(xcoords, wcoords, ycoords, gstate);
    setDrawOrder(Glyph.DRAW_SELF_FIRST);

    //    thresh_glyph = new ThreshGlyph();
    thresh_glyph.setVisibility(getShowThreshold());
    thresh_glyph.setSelectable(false);
    if (thresh_color != null)  { thresh_glyph.setColor(thresh_color); }
    this.addChild(thresh_glyph);

    if (xcoords == null || ycoords == null || xcoords.length <=0 || ycoords.length <= 0) { return; }
    caches.clear();
    if (CALC_GRAPH_CACHE) {
      double graph_coord_length = xcoords[xcoords.length-1] - xcoords[0];
      double avg_bases_per_point = graph_coord_length / ((double)xcoords.length);
      int bases_per_bin = (int)Math.ceil(avg_bases_per_point * compression_level);

      GraphCache2 graph_cache = new GraphCache2(bases_per_bin, xcoords, ycoords);
      caches.add(graph_cache);
    }
    //    if (getMinScoreThreshold() == Float.NEGATIVE_INFINITY ||
    //	getMinScoreThreshold() == Float.POSITIVE_INFINITY) {
    //      setMinScoreThreshold(getVisibleMinY() + ((getVisibleMaxY() - getVisibleMinY())/2));
    //    }
    if (Float.isInfinite(getMinScoreThreshold()) && Float.isInfinite(getMaxScoreThreshold())) {
      setMinScoreThreshold(getVisibleMinY() + ((getVisibleMaxY() - getVisibleMinY())/2));
    }
    resetThreshLabel();
  }

  /*
  public void setGraphState(GraphState state) {
    super.setGraphState(state);
    //   setMinScoreThreshold(state.getMinScoreThreshold());  // to trigger setting of threshold glyph's label
    resetThreshLabel();
  }
  */

  public void draw(ViewI view) {
    if (NEWDEBUG) {
      System.out.println("called SmartGraphGlyph.draw(), coords = " + coordbox);
    }
    int graph_style = getGraphStyle();
    //    System.out.println("thresh glyph visibility: " + thresh_glyph.isVisible() + ", show threshold = " + show_threshold);

    // GAH 9-13-2002
    // hack to get thresholding to work -- thresh line child glyph keeps getting removed
    //   as a child of graph... (must be something in SeqMapView.setAnnotatedSeq()...
    if (this.getChildCount() == 0) {
      if (thresh_glyph == null) {
	thresh_glyph = new ThreshGlyph();
	//	thresh_glyph.setVisibility(getShowThreshold());
	thresh_glyph.setSelectable(false);
	thresh_glyph.setColor(thresh_color);
      }
      this.addChild(thresh_glyph);
      /*
      double thresh_ycoord;
      if (getMinScoreThreshold() != Float.NEGATIVE_INFINITY) {
	thresh_ycoord = getCoordValue(view, getMinScoreThreshold());
      }
      else if (getMaxScoreThreshold() != Float.POSITIVE_INFINITY) {
	thresh_ycoord = getCoordValue(view, getMaxScoreThreshold());
      }
      else {
	System.out.println("in SmartGraphGlyph.draw(), problem with setting up threshold line!");
	thresh_ycoord = 0;
      }
      thresh_glyph.setCoords(coordbox.x, thresh_ycoord, coordbox.width, 1);
      */
    }

    if (graph_style == LINE_GRAPH ||
	graph_style == BAR_GRAPH ||
	graph_style == DOT_GRAPH ||
	graph_style == STAIRSTEP_GRAPH ||
	graph_style == HEAT_MAP) {
        super.draw(view);
    }
    else if (graph_style == MINMAXAVG) {
      double xpixels_per_coord = ((LinearTransform)view.getTransform()).getScaleX();
      double xcoords_per_pixel = 1 / xpixels_per_coord;
      if (TRANSITION_TO_BARS && (xcoords_per_pixel < transition_scale)) {
	// if at resolution where bars should be displayed, then temporarily set
	//   graph_style to BAR_GRAPH and invoke bar drawing by calling super.draw(),
	//   then reset to MINMAXAVG after drawing has occurred
	state.setGraphStyle(BAR_GRAPH);
	super.draw(view);
	state.setGraphStyle(MINMAXAVG);
      }
      else {
	drawMinMaxAvg(view);
      }
    }
    if (getShowThreshold()) {
      drawThresholdedRegions(view);
      //      thresh_glyph.draw(view);
    }
    else {
      thresh_glyph.setVisibility(false);
    }
  }

  public void drawMinMaxAvg(ViewI view) {
    // could size cache to just the view's pixelbox, but then may end up creating a
    //   new int array every time the pixelbox changes (which with view damage or
    //   scrolling optimizations turned on could be often)
    int comp_ysize = ((View)view).getComponentSize().width;
    // could check for exact match with comp_ysize, but allowing larger comp size here
    //    may be good for multiple maps that share the same scene, so that new int array
    //    isn't created every time paint switches from mapA to mapB -- the array will
    //    be reused and be the length of the component with greatest width...
    if ((pixel_cache == null) || (pixel_cache.length < comp_ysize)) {
      //      System.out.println("in SmartGraphGlyph, creating new pixel cache");
      pixel_cache = new int[comp_ysize];
    }
    for (int i=0; i<comp_ysize; i++) {
      pixel_cache[i] = Integer.MIN_VALUE;
    }
    if (TIME_DRAWING) { tim.start(); }
    view.transformToPixels(coordbox, pixelbox);
    if (getShowGraph())  {
      drawGraph(view);
    }
    if (getShowHandle()) {
      drawHandle(view);
    }
    if (getShowAxis()) {
      drawAxisLabel(view);
    }
    // drawing outline around bounding box
    if (getShowBounds()) {
      Graphics g = view.getGraphics();
      g.setColor(Color.green);
      g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width-1, pixelbox.height-1);
    }
    if (getShowLabel()) {
      drawLabel(view);
    }
    if (TIME_DRAWING) {
      long drawtime = tim.read();
      System.out.println("smart graph draw time: " + drawtime);
    }
  }


  public void drawGraph(ViewI view) {
    int graph_style = getGraphStyle();
    view.transformToPixels(coordbox, pixelbox);
    int pbox_yheight = pixelbox.y + pixelbox.height;
    Graphics g = view.getGraphics();
    double coords_per_pixel = 1.0f / ((LinearTransform)view.getTransform()).getScaleX();
    //    System.out.println("Coords per pixel: " + coords_per_pixel);
    // calculating scaling factor to "normalize" graph point yvalues to
    //    coord height of graph
    //    double yscale = coordbox.height / (getVisibleMaxY() - getVisibleMinY());
    //    double offset = coordbox.y + coordbox.height;

    getInternalLinearTransform(view, scratch_trans);
    double yscale = scratch_trans.getScaleY();
    double offset = scratch_trans.getOffsetY();

    //    Rectangle view_pixbox = view.getPixelBox();
    Rectangle2D view_coordbox = view.getCoordBox();
    double xmin = view_coordbox.x;
    double xmax = view_coordbox.x + view_coordbox.width;

    coord.y = offset - ((0 - getVisibleMinY()) * yscale);
    view.transformToPixels(coord, zero_point);

    int beg_index = 0;
    int end_index = xcoords.length-1;

    if (zero_point.y < pixelbox.y)  { zero_point.y = pixelbox.y; }
    else if (zero_point.y > pbox_yheight) { zero_point.y = pbox_yheight; }
    else if (show_zero_line) {
      g.setColor(Color.gray);
      g.drawLine(pixelbox.x, zero_point.y, pixelbox.width, zero_point.y);
    }
    g.setColor(darker);

    /*
     * GAH 8-7-2002
     * calculate the avg_points_per_pixel to determine whether to use
     *   GraphCache(s), and if so, what compression level
     *   (for first attempt, should choose greatest compression level such that compression (number of points per
     *     cache entry at that level) is still less than avg_points_per_pixel)
     */
    double graph_coord_length = xcoords[end_index] - xcoords[beg_index];
    //    double avg_coords_between_points = graph_coord_length / ((double)xcoords.length);
    //    double avg_points_per_coord = 1.0f / avg_coords_between_points;
    double avg_coords_between_points = graph_coord_length / xcoords.length;
    double avg_points_per_coord = 1.0 / avg_coords_between_points;
    // want points_per_coord * coords_per_pixel   ( = points_per_pixel)
    //    double avg_points_per_pixel = avg_coords_per_point / coords_per_pixel;
    double avg_points_per_pixel = avg_points_per_coord * coords_per_pixel;
    /*
     *  first implementation of graph compression caching, just using a flat caching
     *      (one level of compression / summation)
     */
    if ((avg_points_per_pixel > compression_level) && USE_GRAPH_CACHE) {
      int draw_beg_index;
      int draw_end_index;

      // first figure out which graph cache / compression level to use
      // defaulting to first cache for now...
      GraphCache2 graph_cache = (GraphCache2)caches.get(0);

      draw_beg_index = Arrays.binarySearch(graph_cache.xmin, (int)xmin);
      draw_end_index = Arrays.binarySearch(graph_cache.xmax, (int)xmax);
      if (draw_beg_index < 0) {
	// want draw_beg_index to be index of max xcoord <= view_start
	//  (insertion point - 1)  [as defined in Arrays.binarySearch() docs]
	draw_beg_index = (-draw_beg_index -1) - 1;
	if (draw_beg_index < 0) { draw_beg_index = 0; }
      }
      if (draw_end_index < 0) {
	// want draw_end_index to be index of min xcoord >= view_end
	//   (insertion point)  [as defined in Arrays.binarySearch() docs]
	draw_end_index = -draw_end_index -1;
	if (draw_end_index < 0) { draw_end_index = 0; }
	else if (draw_end_index >= graph_cache.xmin.length) { draw_end_index = graph_cache.xmin.length - 1; }
	if (draw_end_index < (graph_cache.xmin.length-1)) { draw_end_index++; }
      }
      coord.x = graph_cache.xmin[draw_beg_index];
      //      coord.y = offset - (graph_cache.yavg[draw_beg_index] * yscale);
      coord.y = offset - ((graph_cache.ymin[draw_beg_index] - getVisibleMinY()) * yscale);
      view.transformToPixels(coord, prev_point);

      int ymin_pixel = prev_point.y;;
      int ymax_pixel = prev_point.y;
      int temp_ymin_pixel = prev_point.y;
      int temp_ymax_pixel = prev_point.y;

      int yavg_pixel;
      int ysum = prev_point.y;
      int temp_ysum = prev_point.y;

      int points_in_pixel = 1;
      int draw_count = 0;
      int last_xavg = 0;
      int last_yavg = 0;
      int last_ymin = 0;
      int last_ymax = 0;
      // trying first with just min/max drawing...
      if (CACHE_DIRECT_DRAW) {
	g.setColor(darker);
	for (int i = draw_beg_index; i <= draw_end_index; i++) {
	  coord.x = (graph_cache.xmin[i] + graph_cache.xmax[i]) / 2.0f;
	  coord.y = offset - ((graph_cache.ymin[i] - getVisibleMinY()) * yscale);
	  view.transformToPixels(coord, curr_point);
	  // flipping -- hmm...
	  //	ymin_pixel = curr_point.y;
	  ymax_pixel = curr_point.y;

	  coord.y = offset - ((graph_cache.ymax[i] - getVisibleMinY()) * yscale);
	  view.transformToPixels(coord, curr_point);
	  // flipping -- hmm...
	  //	ymax_pixel = curr_point.y;
	  ymin_pixel = curr_point.y;

	  g.fillRect(curr_point.x, ymin_pixel, 1, (ymax_pixel - ymin_pixel + 1));
	  draw_count++;
	}
      }
      else {  // using cache, but still collecting values per pixel...
	g.setColor(darker);
	for (int i = draw_beg_index; i <= draw_end_index; i++) {
	  coord.x = ((double)graph_cache.xmin[i] + (double)graph_cache.xmax[i]) / 2.0;
	  coord.y = offset - ((graph_cache.ymin[i] - getVisibleMinY()) * yscale);
	  view.transformToPixels(coord, curr_point);
	  // flipping -- hmm...
	  //	ymin_pixel = curr_point.y;
	  temp_ymax_pixel = curr_point.y;

	  coord.y = offset - ((graph_cache.ymax[i] - getVisibleMinY()) * yscale);
	  view.transformToPixels(coord, curr_point);
	  // flipping -- hmm...
	  //	ymax_pixel = curr_point.y;
	  temp_ymin_pixel = curr_point.y;

	  coord.y = offset - ((graph_cache.yavg[i] - getVisibleMinY()) * yscale);
	  view.transformToPixels(coord, curr_point);
	  temp_ysum = curr_point.y;

	  if (prev_point.x == curr_point.x) {
	    // collect ymin, ymax, y_average for all coord points that transform to
	    //    the same x pixel
	    ymin_pixel = Math.min(ymin_pixel, temp_ymin_pixel);
	    ymax_pixel = Math.max(ymax_pixel, temp_ymax_pixel);
	    ysum += temp_ysum;
	    points_in_pixel++;
	  }

	  else {  // draw previous pixel position
	    if ((graph_style == MINMAXAVG) && MINMAXBAR) {
	      int ystart = Math.max(Math.min(ymin_pixel, pbox_yheight), pixelbox.y);
	      int yheight = Math.min(Math.max(ymax_pixel, pixelbox.y), pbox_yheight) - ystart;
	      g.fillRect(prev_point.x, ystart, 1, yheight);
	      //	      g.drawLine(prev_point.x, Math.max(Math.min(ymin_pixel, pbox_yheight), pixelbox.y),
	      //			 prev_point.x, Math.min(Math.max(ymax_pixel, pixelbox.y), pbox_yheight));
	      draw_count++;
	    }
	    yavg_pixel = ysum / points_in_pixel;
	    if (AVGLINE) {
	      // cache for drawing later
	      if (prev_point.x > 0 && prev_point.x < pixel_cache.length) {
		pixel_cache[prev_point.x] =
		  Math.min(Math.max(yavg_pixel, pixelbox.y), pbox_yheight);
	      }
	    }
	    last_xavg = prev_point.x;
	    last_yavg = yavg_pixel;
	    ymin_pixel = temp_ymin_pixel;
	    ymax_pixel = temp_ymax_pixel;
	    ysum = temp_ysum;

	    points_in_pixel = 1;
	    prev_point.x = curr_point.x;
	    prev_point.y = curr_point.y;
	  }
	}
      }
      if (SHOW_CACHE_INDICATOR) {
	g.setColor(Color.white);
	g.fillRect(20, ymin_pixel-5, 25, 10);
      }
    }
    // not using graph "cache", because zoomed too far in (but not far enough to switch to bars)
    else  { // not using graph cache...
      // using binary search to find end points --
      //    assumes xcoords array is ordered by increasing value
      int draw_beg_index = Arrays.binarySearch(xcoords, (int)xmin);

      // The +1 on draw_end_index might be an error, but it gets corrected below
      int draw_end_index = Arrays.binarySearch(xcoords, (int)xmax) + 1;

      if (draw_beg_index < 0) {
	// want draw_beg_index to be index of max xcoord <= view_start
	//  (insertion point - 1)  [as defined in Arrays.binarySearch() docs]
	draw_beg_index = (-draw_beg_index -1) - 1;
	if (draw_beg_index < 0) { draw_beg_index = 0; }
      }
      if (draw_end_index < 0) {
	// want draw_end_index to be index of min xcoord >= view_end
	//   (insertion point)  [as defined in Arrays.binarySearch() docs]
	draw_end_index = -draw_end_index -1;
	if (draw_end_index < 0) { draw_end_index = 0; }
	else if (draw_end_index >= xcoords.length) { draw_end_index = xcoords.length - 1; }
	if (draw_end_index < (xcoords.length-1)) { draw_end_index++; }
      }

      // draw_end_index is sometimes too large (by 1)
      if (draw_end_index >= xcoords.length) {
        draw_end_index = xcoords.length - 1;
      }

      coord.x = xcoords[draw_beg_index];
      //      coord.y = offset - (ycoords[draw_beg_index] * yscale);
      coord.y = offset - ((ycoords[draw_beg_index] - getVisibleMinY()) * yscale);
      view.transformToPixels(coord, prev_point);

      int ymin_pixel = prev_point.y;;
      int ymax_pixel = prev_point.y;
      int yavg_pixel;
      int ysum = prev_point.y;
      int points_in_pixel = 1;
      int draw_count = 0;

      g.setColor(darker);
      for (int i = draw_beg_index; i <= draw_end_index; i++) {
	coord.x = xcoords[i];
	coord.y = offset - ((ycoords[i] - getVisibleMinY()) * yscale);
	view.transformToPixels(coord, curr_point);
	if (prev_point.x == curr_point.x) {
	  // collect ymin, ymax, y_average for all coord points that transform to
	  //    the same x pixel
	  ymin_pixel = Math.min(ymin_pixel, curr_point.y);
	  ymax_pixel = Math.max(ymax_pixel, curr_point.y);
	  ysum += curr_point.y;
	  points_in_pixel++;
	}
	else {  // draw previous pixel position
	  if ((graph_style == MINMAXAVG) && MINMAXBAR)  {
	    int ystart = Math.max(Math.min(ymin_pixel, pbox_yheight), pixelbox.y);
	    int yend = Math.min(Math.max(ymax_pixel, pixelbox.y), pbox_yheight);
	    int yheight = yend - ystart;
	    g.fillRect(prev_point.x, ystart, 1, yheight);
	    //	    g.fillRect(prev_point.x, ystart, 1, 1);
	    //	    g.fillRect(prev_point.x, yend, 1, 1);
	    //	    g.drawLine(prev_point.x, Math.max(Math.min(ymin_pixel, pbox_yheight), pixelbox.y),
	    //		       prev_point.x, Math.min(Math.max(ymax_pixel, pixelbox.y), pbox_yheight));
	    draw_count++;
	  }
	  yavg_pixel = ysum / points_in_pixel;
	  if (AVGLINE) {
	    // cache for drawing later
	    if (prev_point.x > 0 && prev_point.x < pixel_cache.length) {
	      pixel_cache[prev_point.x] =
		  Math.min(Math.max(yavg_pixel, pixelbox.y), pbox_yheight);
	    }
	  }
	  ymin_pixel = curr_point.y;
	  ymax_pixel = curr_point.y;
	  ysum = curr_point.y;
	  points_in_pixel = 1;
	  prev_point.x = curr_point.x;
	  prev_point.y = curr_point.y;
	}
      }
      // can only show threshold if xy coords are also being shown (show_graph = true)
    }

    if (AVGLINE) {
      g.setColor(lighter);
      int prev_index = 0;
      // find the first pixel position that has a real value in pixel_cache
      while ((prev_index < pixel_cache.length) && (pixel_cache[prev_index] == Integer.MIN_VALUE)) {
	prev_index++;
      }
      if (prev_index != pixel_cache.length) { // successfully found a real value in pixel cache
	int yval;
	for (int i=prev_index+1; i<pixel_cache.length; i++) {
	  yval = pixel_cache[i];
	  if (yval != Integer.MIN_VALUE) {
	    if (pixel_cache[i-1] == Integer.MIN_VALUE &&
		coords_per_pixel > 30) {
	      // last pixel had no datapoints, so just draw a point at current pixel
	      g.drawLine(i, yval, i, yval);
	    }
	    else {
	      // last pixel had at least one datapoint, so connect with line
	      g.drawLine(prev_index, pixel_cache[prev_index], i, yval);
	    }
	    prev_index = i;
	  }
	}
      }
    }
  }

  /**
   *  Retrieve the graph yvalue corresponding to a given ycoord.
   */
  public float getGraphValue(ViewI view, double coord_value) {
    getInternalLinearTransform(view, scratch_trans);
    double yscale = scratch_trans.getScaleY();
    double offset = scratch_trans.getOffsetY();

    float graph_value = (float)((offset - coord_value) / yscale) + getVisibleMinY();
    return graph_value;
  }

  /**
   *  Retrieve the map y coord corresponding to a given graph yvalue.
   */
  public double getCoordValue(ViewI view, float graph_value) {
    getInternalLinearTransform(view, scratch_trans);
    double yscale = scratch_trans.getScaleY();
    double offset = scratch_trans.getOffsetY();

    float coord_value = (float)(offset - ((graph_value - getVisibleMinY()) * yscale));
    return coord_value;
  }

  public void drawThresholdedRegions(ViewI view) {
    drawThresholdedRegions(view, null, null);
  }

  /**
   *  Draws thresholded regions.
   *  Current set up so that if regions_parent != null, then instead of drawing to view,
   *   populate regions_parent with child SeqSymmetries for each region that passes threshold,
   *
   */
  public void drawThresholdedRegions(ViewI view, MutableSeqSymmetry region_holder, BioSeq aseq) {
   /*
    *  Should really eventually move the SeqSymmetry stuff out of this class, maybe have
    *     drawThresholdedRegions() populate regions that pass threshold as two IntLists or
    *     something...
    */
    double max_gap_threshold = getMaxGapThreshold();
    double min_run_threshold = getMinRunThreshold();
    double span_start_shift = getThreshStartShift();
    double span_end_shift = getThreshEndShift();

    int thresh_direction = getThresholdDirection();
    float min_score_threshold = Float.NEGATIVE_INFINITY;
    float max_score_threshold = Float.POSITIVE_INFINITY;
    if (thresh_direction == GraphState.THRESHOLD_DIRECTION_GREATER) {
      min_score_threshold = getMinScoreThreshold();
      max_score_threshold = Float.POSITIVE_INFINITY;
    }
    else if (thresh_direction == GraphState.THRESHOLD_DIRECTION_LESS) {
      min_score_threshold = Float.NEGATIVE_INFINITY;
      max_score_threshold = getMaxScoreThreshold();
    }
    else if (thresh_direction == GraphState.THRESHOLD_DIRECTION_BETWEEN) {
      min_score_threshold = getMinScoreThreshold();
      max_score_threshold = getMaxScoreThreshold();
    }

    // if neither min or max score thresholds have been set, assume that only using
    //     min score threshold and set so it is in the middle of visible score range
    if (Float.isInfinite(min_score_threshold) && Float.isInfinite(max_score_threshold)) {
      setMinScoreThreshold(getVisibleMinY() + ((getVisibleMaxY() - getVisibleMinY())/2));
      min_score_threshold = getMinScoreThreshold();
      max_score_threshold = Float.POSITIVE_INFINITY;
    }

    Rectangle2D view_coordbox = view.getCoordBox();
    double xmin = view_coordbox.x;
    double xmax = view_coordbox.x + view_coordbox.width;
    int draw_beg_index;
    int draw_end_index;
    boolean make_syms = ((region_holder != null) && (aseq != null));
    //    boolean make_syms = (aseq != null);
    int sym_count = 0;
    int draw_count = 0;
    if (make_syms) {
      // if writing to region_holder, then want to do _whole_ graph, not just
      //   what's in current view
      draw_beg_index = 0;
      draw_end_index = xcoords.length - 1;
    }
    else {
      draw_beg_index = Arrays.binarySearch(xcoords, (int)xmin);
      draw_end_index = Arrays.binarySearch(xcoords, (int)xmax) + 1;
    }

    if (draw_beg_index < 0) {
      // want draw_beg_index to be index of max xcoord <= view_start
      //  (insertion point - 1)  [as defined in Arrays.binarySearch() docs]
      draw_beg_index = (-draw_beg_index -1) - 1;
     if (draw_beg_index < 0) { draw_beg_index = 0; }
    }
    if (draw_end_index < 0) {
      // want draw_end_index to be index of min xcoord >= view_end
      //   (insertion point)  [as defined in Arrays.binarySearch() docs]
      draw_end_index = -draw_end_index -1;
      if (draw_end_index < 0) { draw_end_index = 0; }
      else if (draw_end_index >= xcoords.length) { draw_end_index = xcoords.length - 1; }
      if (draw_end_index < (xcoords.length-1)) { draw_end_index++; }
    }


    // dynamicly confining threshold to within getVisibleMinY() and getVisibleMaxY(), since
    //   I'm having trouble dealing with this in GlyphDragger (where I'm trying to
    //   optionally confine drag of glyph so that it can't go outside it's parent's borders...
    else {
      // GAH 4-12-2005 commenting out restriction of score threshold to visible range, because
      //   otherwise it could conflict with reported threshold settings in GraphAdjusterView
      //   instead, later in method setting thresh glyph visibility to false if outside visible score range
      //      if (min_score_threshold < getVisibleMinY()) { setScoreThreshold(getVisibleMinY()); }
      //      else if (min_score_threshold > getVisibleMaxY()) { setScoreThreshold(getVisibleMaxY()); }
    }

    double thresh_ycoord;
    double thresh_score;
    if ( ! Float.isInfinite(min_score_threshold)) {
      thresh_score = min_score_threshold;
    }
    else if (! Float.isInfinite(max_score_threshold)) {
      thresh_score = max_score_threshold;
    }
    else {
      System.out.println("in SmartGraphGlyph.drawThresholdedRegions(), problem with setting up threshold line!");
      thresh_score = (getVisibleMinY() + (getVisibleMaxY()/2));
    }
    if (thresh_score < getVisibleMinY()  ||
	thresh_score > getVisibleMaxY() ) {
      thresh_glyph.setVisibility(false);
    }
    else {
      thresh_glyph.setVisibility(true);
    }

    thresh_ycoord = getCoordValue(view, (float)thresh_score);
    thresh_glyph.setCoords(coordbox.x, thresh_ycoord, coordbox.width, 1);

    Graphics g = view.getGraphics();
    g.setColor(lighter);
    double x, y;
    double pass_thresh_start = 0;
    double pass_thresh_end = 0;
    int pass_thresh_count = 0;
    boolean pass_threshold_mode = false;
    int min_index = 0;
    int max_index = xcoords.length-1;

    // need to widen range searched to include previous and next points out of view that
    //   pass threshold (unless distance to view is > max_gap_threshold
    int new_beg = draw_beg_index;

    // GAH 2006-02-16 changed to <= max_gap instead of <, to better mirror Affy tiling array pipeline
    while ((new_beg > min_index) &&
    	   // ((xcoords[draw_beg_index] - xcoords[new_beg]) <= max_gap_threshold)) {
    	   ((xcoords[draw_beg_index] - xcoords[new_beg]) <= max_gap_threshold)) {
      new_beg--;
    }
    draw_beg_index = new_beg;

    int new_end = draw_end_index;
    boolean pass_score_thresh;
    boolean passes_max_gap;
    //    boolean passes_min_run;
    boolean draw_previous = false;
    //    while ((new_end < max_index) &&   // end_index is really the maximum allowed draw_end_index
    //	   ((xcoords[new_end] - xcoords[draw_end_index]) < max_gap_threshold) &&
    //	   (ycoords[new_end] < min_score_threshold)) {

    // GAH 2006-02-16 changed to <= max_gap instead of <, to better mirror Affy tiling array pipeline
    while ((new_end < max_index) &&   // end_index is really the maximum allowed draw_end_index
	   // 	   ((xcoords[new_end] - xcoords[draw_end_index]) < max_gap_threshold)) {
	   ((xcoords[new_end] - xcoords[draw_end_index]) <= max_gap_threshold)) {
      new_end++;
    }
    draw_end_index = new_end;

    if (draw_end_index >= xcoords.length) { draw_end_index = xcoords.length - 1; }

    if (THRESH_DEBUG) {
      int check_xmin = xcoords[draw_beg_index];
      int check_xmax = xcoords[draw_end_index];
      int delta_xmin = (int)xmin - check_xmin;  // should be positive
      int delta_xmax = check_xmax - (int)xmax; // should be positive
      System.out.println("");
      System.out.println("view   bounds: min = " + xmin + ", max = " + xmax);
      System.out.println("check bounds: min = " + check_xmin + ", max = " + check_xmax);
      System.out.println("delta: min = " + delta_xmin + ", max = " + delta_xmax);
      if (delta_xmin < 0) { System.out.println("$$$$$$$$$$$$ xmin < check_xmin $$$$$$$$$$$"); }
      if (delta_xmax < 0) { System.out.println("$$$$$$$$$$$$ xmax > check_xmax $$$$$$$$$$$"); }
    }

    // eight possible states:
    //
    //     pass_threshold_mode    [y >= min_score_threshold]   [x-pass_thresh_end <= max_dis_thresh]
    //
    //  prune previous region and draw when:
    //      true, false, false
    //      true, true, false
    for (int i = draw_beg_index; i <= draw_end_index; i++) {
      x = xcoords[i];
      y = ycoords[i];

      //      pass_score_thresh = ((y >= min_score_threshold) &&
      // GAH 2006-02-16 changed to > min_score instead of >= min_score, to better mirror Affy tiling array pipeline
      pass_score_thresh = ((y > min_score_threshold) &&
      			   (y <= max_score_threshold) );
      passes_max_gap = ((x - pass_thresh_end) <= max_gap_threshold);
      if (pass_threshold_mode) {  // if currently keeping track of potential passed-threshold region
	// true, ?, ?
	if (pass_score_thresh) { // this point passes threshold test
	  // true, true, ?
	  if (passes_max_gap) { // AND its within max distance
	    // true, true, true
	    // passes threshold test, within max distance, keep extending region
	    pass_thresh_end = x;
	  }
	  else {
	    // true, true, false
	    // passes threshold test, but NOT within max distance
	    // therefore end (and draw) previous region, and start this as a potential new region
	    draw_previous = true;
	  }
	}
	else {  // this point does not pass threshold test
	  // true, false, ?
	  if (passes_max_gap) {
	    // true, false, true
	  }
	  else {
	    // true, false, false
	    // attempting to extend region, but NOT passing threshold, and NOT within max distance
	    // therefore end (and draw) previous region
	    draw_previous = true;
	  }
	}
      }
      else {
	// false, ?, ?
	if (pass_score_thresh) {
	  // false, true, ?
	  // switch into pass_threshold_mode
	  // don't need to worry about distance thresh here
	  pass_thresh_start = x;
	  pass_thresh_end = x;
	  pass_threshold_mode = true;
	}
	else {
	  // false, false, ?
	  // not extending a region (since not in pass_threshold_mode), so do nothing?
	  // don't need to worry about distance thresh here
	}
      }

      if (draw_previous) {
	double draw_min = pass_thresh_start + span_start_shift;
	double draw_max = pass_thresh_end + span_end_shift;
	//	boolean passes_min_run = ((draw_max - draw_min) >= min_run_threshold);
	// make sure that length of region is > min_run_threshold
	// GAH 2006-02-16 changed to > min_run instead of >=, to better mirror Affy tiling array pipeline
	boolean passes_min_run = ((draw_max - draw_min) > min_run_threshold);
	if (passes_min_run) {  // make sure aren't drawing single points
	  coord.x = draw_min;
	  view.transformToPixels(coord, prev_point);
	  coord.x = draw_max;
	  view.transformToPixels(coord, curr_point);
	  if (make_syms) {
	    SeqSymmetry sym =
	      new SingletonSeqSymmetry((int)draw_min, (int)draw_max, aseq);
	    region_holder.addChild(sym);
	    sym_count++;
	  }
	  else {
	    draw_count++;
	    g.fillRect(prev_point.x,
		       pixelbox.y + pixelbox.height - thresh_contig_height,
		       curr_point.x - prev_point.x + 1,
		       thresh_contig_height);
	  }
	}
	draw_previous = false;
	if (pass_score_thresh) {  // current point passes threshold test, start new region scan
	  pass_thresh_start = x;
	  pass_thresh_end = x;
	  pass_threshold_mode = true;
	}
	else {
	  pass_threshold_mode = false;
	}
      }
    }

    // clean up by doing a draw if exited loop while still in pass_threshold_mode
    if (pass_threshold_mode && (pass_thresh_end != pass_thresh_start)) {
      //	  System.out.println("clean up at " + pass_thresh_start);
      double draw_min = pass_thresh_start + span_start_shift;
      double draw_max = pass_thresh_end + span_end_shift;
      //      boolean passes_min_run = ((draw_max - draw_min) >= min_run_threshold);
      // GAH 2006-02-16 changed to > min_run instead of >=, to better mirror Affy tiling array pipeline
      boolean passes_min_run = ((draw_max - draw_min) > min_run_threshold);
      if (passes_min_run) {
	coord.x = draw_min;
	view.transformToPixels(coord, prev_point);
	coord.x = draw_max;
	view.transformToPixels(coord, curr_point);
	if (make_syms) {
	  SeqSymmetry sym =
	    new SingletonSeqSymmetry((int)pass_thresh_start, (int)pass_thresh_end, aseq);
	  region_holder.addChild(sym);
	}
	else {
	  g.fillRect(prev_point.x,
		     pixelbox.y + pixelbox.height - thresh_contig_height,
		     curr_point.x - prev_point.x + 1,
		     thresh_contig_height);
	}
      }

    }
  }


  public void setBackgroundColor(Color col) {
    super.setBackgroundColor(col);
    lighter = col.brighter();
    darker = col.darker();
    thresh_color = darker.darker();
    if (thresh_glyph != null)  { thresh_glyph.setColor(thresh_color); }
  }

  public void setShowThreshold(boolean show) {
    state.setShowThreshold(show);
    thresh_glyph.setVisibility(show);
  }

  public boolean getShowThreshold() { return state.getShowThreshold(); }

  public void resetThreshLabel() {
    float min_thresh = getMinScoreThreshold();
    float max_thresh = getMaxScoreThreshold();
    int direction = state.getThresholdDirection();
    if (direction == GraphState.THRESHOLD_DIRECTION_BETWEEN) {
      thresh_glyph.setLabel(nformat.format(min_thresh) + " -- " + nformat.format(max_thresh));
    }
    else if (direction == GraphState.THRESHOLD_DIRECTION_GREATER) {
      thresh_glyph.setLabel(">= " + nformat.format(min_thresh));
    }
    else if (direction == GraphState.THRESHOLD_DIRECTION_LESS) {
      thresh_glyph.setLabel("<= " + nformat.format(max_thresh));
    }
  }

  public void setThresholdDirection(int d) {
    state.setThresholdDirection(d);
    resetThreshLabel();
  }

  public void setMinScoreThreshold(float thresh) {
    state.setMinScoreThreshold(thresh);
    resetThreshLabel();
  }

  public void setMaxScoreThreshold(float thresh) {
    state.setMaxScoreThreshold(thresh);
    resetThreshLabel();
  }

  public void setMaxGapThreshold(int thresh) { state.setMaxGapThreshold(thresh); }
  public void setMinRunThreshold(int thresh) { state.setMinRunThreshold(thresh); }
  public void setThreshStartShift(double d) { state.setThreshStartShift(d); }
  public void setThreshEndShift(double d) { state.setThreshEndShift(d); }

  public int getThresholdDirection() { return state.getThresholdDirection(); }
  public float getMinScoreThreshold() { return state.getMinScoreThreshold(); }
  public float getMaxScoreThreshold()  { return state.getMaxScoreThreshold(); }
  public double getMaxGapThreshold() { return state.getMaxGapThreshold(); }
  public double getMinRunThreshold() { return state.getMinRunThreshold(); }
  public double getThreshStartShift() { return state.getThreshStartShift(); }
  public double getThreshEndShift() { return state.getThreshEndShift(); }


  /*
  public void setPointCoords(int xcoords[], float ycoords[]) {
    if (xcoords.length <= 0 || ycoords.length <= 0) {
      return;
    }
    super.setPointCoords(xcoords, ycoords);
    caches.clear();
    if (CALC_GRAPH_CACHE) {
      double graph_coord_length = xcoords[xcoords.length-1] - xcoords[0];
      double avg_bases_per_point = graph_coord_length / ((double)xcoords.length);
      int bases_per_bin = (int)Math.ceil(avg_bases_per_point * compression_level);

      GraphCache2 graph_cache = new GraphCache2(bases_per_bin, xcoords, ycoords);
      caches.add(graph_cache);
    }
    //    if (getMinScoreThreshold() == Float.NEGATIVE_INFINITY ||
    //	getMinScoreThreshold() == Float.POSITIVE_INFINITY) {
    //      setMinScoreThreshold(getVisibleMinY() + ((getVisibleMaxY() - getVisibleMinY())/2));
    //    }
    if ((getMinScoreThreshold() == Float.NEGATIVE_INFINITY) && (getMaxScoreThreshold() == Float.POSITIVE_INFINITY))  {
      setMinScoreThreshold(getVisibleMinY() + ((getVisibleMaxY() - getVisibleMinY())/2));
    }
  }
  */

 //  public void setFasterDraw(boolean b) { USE_GRAPH_CACHE = b; }
  // public boolean getFasterDraw() { return USE_GRAPH_CACHE; }
  // public void setCalcCache(boolean b) { CALC_GRAPH_CACHE = b; }
  // public boolean getCalcCache() { return CALC_GRAPH_CACHE; }

  /**
   *  Same as GraphGlyph.getInternalLinearTransform(), except
   *  also caclulates a bottom y offset for showing thresholded
   *  regions, if showThresholdedRegions() == true.
   */
  protected double getLowerYCoordInset(ViewI view) {
    double bottom_ycoord_inset = super.getLowerYCoordInset(view);
    if (getShowThreshold())  {
      thresh_pix_box.height = thresh_contig_height + thresh_contig_yoffset;
      view.transformToCoords(thresh_pix_box, thresh_coord_box);
      bottom_ycoord_inset += thresh_coord_box.height;
    }
    return bottom_ycoord_inset;
  }

}

class GraphCache2 {
  public int bases_per_entry;
  public int[] xmin;
  public int[] xmax;
  public float[] ymin;
  public float[] ymax;
  public float[] yavg;
  // currently num_points is not used during rendering, so could probably leave it out
  //   could potentially incorporate it later for more accurate rendering...
  public int[] num_points;

  public GraphCache2(int bases_per_entry, int[] xcoords, float[] ycoords) {
    int count_guess = 1;
    if (bases_per_entry != 0) {
      count_guess = (int)(Math.abs((xcoords[xcoords.length - 1] - xcoords[0])) / bases_per_entry);
    }
    if (count_guess < 1)  { count_guess = 1; }
    IntList xmin_list = new IntList(count_guess);
    IntList xmax_list = new IntList(count_guess);
    FloatList ymin_list = new FloatList(count_guess);
    FloatList ymax_list = new FloatList(count_guess);
    FloatList yavg_list = new FloatList(count_guess);
    IntList num_points_list = new IntList(count_guess);

    int cur_index = 0;

    while (cur_index < xcoords.length) {
      float valmin = Float.POSITIVE_INFINITY;
      float valmax = Float.NEGATIVE_INFINITY;
      float valavg = 0;
      int entry_xmin = xcoords[cur_index];
      int entry_xlimit = entry_xmin + bases_per_entry;
      int entry_points = 0;
      // really don't need entry_xmax assignment here, always gets assigned in loop
      //   (even if only one point in this cache entry), but setting for clarification...
      int entry_xmax = entry_xmin;
      if ( (cur_index < xcoords.length) &&
	      (xcoords[cur_index] <= entry_xlimit) ) {
	while ( (cur_index < xcoords.length) &&
		(xcoords[cur_index] <= entry_xlimit) ) {
	  float yval = ycoords[cur_index];
	  entry_xmax = xcoords[cur_index];
	  valmin = Math.min(valmin, yval);
	  valmax = Math.max(valmax, yval);
	  valavg += yval;
	  entry_points++;
	  cur_index++;
	}
	valavg = valavg / (float)entry_points;

	xmin_list.add(entry_xmin);
	xmax_list.add(entry_xmax);
	num_points_list.add(entry_points);
	ymin_list.add(valmin);
	ymax_list.add(valmax);
	yavg_list.add(valavg);
      }
      else {
	// shouldn't hit this branch unless somethings gone wrong.
	// for now just increment cur_index to avoid infinite loop...
	cur_index++;
      }
    }

    xmin = xmin_list.copyToArray();
    xmax = xmax_list.copyToArray();
    ymin = ymin_list.copyToArray();
    ymax = ymax_list.copyToArray();
    yavg = yavg_list.copyToArray();
    num_points = num_points_list.copyToArray();
  }
}

