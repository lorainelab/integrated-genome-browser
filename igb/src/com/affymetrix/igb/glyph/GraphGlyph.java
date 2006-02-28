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
import java.text.NumberFormat;
import java.text.DecimalFormat;

import com.affymetrix.genoviz.bioviews.*;
import java.util.*;

/**
 *  An implementation of graphs for NeoMaps, capable of rendering graphs in a variety of styles
 *  Started with {@link com.affymetrix.genoviz.glyph.BasicGraphGlyph} and improved from there.
 *  ONLY MEANT FOR GRAPHS ON HORIZONTAL MAPS.
 */
public class GraphGlyph extends Glyph {
  public boolean TIME_DRAWING = false;

  // THICK_OUTLINE: should the selection outline be thick?
  static final boolean THICK_OUTLINE = true;

  // boolean auto_adjust_visible = true;

  static Font default_font = new Font("Courier", Font.PLAIN, 12);
  static Font axis_font = new Font("SansSerif", Font.PLAIN, 12);
  static NumberFormat nformat = new DecimalFormat();
  static double axis_bins = 10;
  static int default_heatmap_bins = 256;
  static Color[] default_heatmap_colors;  // default is black to white grayscale gradient

  public static final int LINE_GRAPH = 1;
  public static final int BAR_GRAPH = 2;
  public static final int DOT_GRAPH = 3;
  public static final int STAIRSTEP_GRAPH = 5;
  public static final int SPAN_GRAPH = 6;
  public static final int HEAT_MAP = 7;

  int xpix_offset = 0;
  Point zero_point = new Point(0,0);
  Point2D coord = new Point2D(0,0);
  Point curr_point = new Point(0,0);
  Point prev_point = new Point(0,0);
  Color[] heatmap_colors = null;
  int heatmap_bins = 256;

  Rectangle2D label_coord_box = new Rectangle2D();
  Rectangle label_pix_box = new Rectangle();

  com.affymetrix.genoviz.util.Timer tim = new com.affymetrix.genoviz.util.Timer();

  boolean show_zero_line = true;
  boolean LARGE_HANDLE = true;
  boolean hide_zero_points = true;
  String label = null;

  /**
   *  point_max_ycoord is the max ycoord (in graph coords) of all points in graph.
   *  This number is calculated in setPointCoords() directly fom ycoords, and cannot
   *     be modified (except for resetting the points by calling setPointCoords() again)
   */
  float point_max_ycoord = Float.NEGATIVE_INFINITY;
  float point_min_ycoord = Float.POSITIVE_INFINITY;

  // assumes sorted points, each x corresponding to y
  int xcoords[];
  float ycoords[];
  //  FillRectGlyph handle;
  int handle_width = 10;  // width of handle in pixels
  int handle_height = 5;  // height of handle in pixels

  Rectangle handle_pixbox = new Rectangle(); // caching rect for handle pixel bounds
  Rectangle pixel_hitbox = new Rectangle();  // caching rect for hit detection

  GraphState state;
  LinearTransform scratch_trans = new LinearTransform();

  static {
    default_heatmap_colors = new Color[default_heatmap_bins];
    for (int i=0; i<default_heatmap_bins; i++) {
      default_heatmap_colors[i] = new Color(i, i, i);
    }
  }

  public GraphGlyph(int[] xcoords, float[] ycoords)  {
    this(xcoords, ycoords, null);
  }

  public GraphGlyph(int[] xcoords, float[] ycoords, GraphState gstate) {
    super();
    state = gstate;
    if (state == null) { state = new GraphState(); }
    setCoords(coordbox.x, state.getGraphYPos(), coordbox.width, state.getGraphHeight());
    setColor(state.getColor());
    setGraphStyle(state.getGraphStyle());

    if (xcoords == null || ycoords == null || xcoords.length <=0 || ycoords.length <= 0) { return; }
    this.xcoords = xcoords;
    this.ycoords = ycoords;
    point_min_ycoord = Float.POSITIVE_INFINITY;
    point_max_ycoord = Float.NEGATIVE_INFINITY;
    for (int i=0; i<ycoords.length; i++) {
      if (ycoords[i] < point_min_ycoord) { point_min_ycoord = ycoords[i]; }
      if (ycoords[i] > point_max_ycoord) { point_max_ycoord = ycoords[i]; }
    }
    if (point_max_ycoord == point_min_ycoord) {
      point_min_ycoord = point_max_ycoord - 1;
    }
    //    System.out.println("min: " + min_ycoord + ", max: " + getVisibleMaxY());
    //    auto_adjust_visible = false;
    checkVisibleBoundsY();
  }

  protected void checkVisibleBoundsY() {
    if (getVisibleMinY() == Float.POSITIVE_INFINITY ||
	getVisibleMinY() == Float.NEGATIVE_INFINITY ||
	getVisibleMaxY() == Float.POSITIVE_INFINITY ||
	getVisibleMaxY() == Float.NEGATIVE_INFINITY) {
      setVisibleMaxY(point_max_ycoord);
      setVisibleMinY(point_min_ycoord);
    }    
  }

  /*
 public void setGraphState(GraphState gs) {
    state = gs;
    setCoords(coordbox.x, state.getGraphYPos(), coordbox.width, state.getGraphHeight());
    setColor(state.getColor());
    setGraphStyle(state.getGraphStyle());
  }
  */

  public GraphState getGraphState() { return state; }


  public void draw(ViewI view) {
    if (TIME_DRAWING) { tim.start(); }
    int graph_style = getGraphStyle();
    view.transformToPixels(coordbox, pixelbox);
    int pbox_yheight = pixelbox.y + pixelbox.height;
    Graphics g = view.getGraphics();

    getInternalLinearTransform(view, scratch_trans);
    double yscale = scratch_trans.getScaleY();
    double offset = scratch_trans.getOffsetY();

    // using same principles as in genometry span transformations:
    //   y = m(x)+b    // well, here x is actually ycoord of graph, and y is bin index
    //
    //   m = (ymax - ymin) / (xmax - xmin)
    //   b = ymin - m(xmin)   [ OR b = ymax - m(xmax) ]
    //   so  y = m(x) + ymin - m(xmin)
    //       y = m(x-xmin) + ymin;
    //   and for heatmap bins, ymin = 0,
    //     so y = m(x-xmin)

    // calculate slope (m)
    double heatmap_scaling = (double)(heatmap_bins-1) / (getVisibleMaxY() - getVisibleMinY());

    //    Rectangle view_pixbox = view.getPixelBox();
    Rectangle2D view_coordbox = view.getCoordBox();
    double xmin = view_coordbox.x;
    double xmax = view_coordbox.x + view_coordbox.width;

    if (getShowGraph() && xcoords != null && ycoords != null)  {
      int beg_index = 0;
      int end_index = xcoords.length-1;

      coord.y = offset - ((0 - getVisibleMinY()) * yscale);
      view.transformToPixels(coord, zero_point);
      if (zero_point.y < pixelbox.y)  {
	zero_point.y = pixelbox.y;
      }
      else if (zero_point.y > pbox_yheight) {
	zero_point.y = pbox_yheight;
      }
      else if (show_zero_line) {
	g.setColor(Color.gray);
 	g.drawLine( pixelbox.x, zero_point.y, ( pixelbox.x + pixelbox.width ), zero_point.y );
      }

      g.setColor(this.getColor());

      // set up prev_point before starting loop
      coord.x = xcoords[beg_index];
      coord.y = offset - ((ycoords[beg_index] - getVisibleMinY()) * yscale);
      view.transformToPixels(coord, prev_point);
      float prev_ytemp = ycoords[beg_index];

      int draw_beg_index = Arrays.binarySearch(xcoords, (int)xmin);
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

      if (draw_end_index >= xcoords.length) {
        // There may be a better way to included this check in earlier logic,
        // but this check is definitely needed at some point.
        draw_end_index = xcoords.length - 1;
      }

      float ytemp;
      for (int i = draw_beg_index; i <= draw_end_index; i++) {
	// flipping about yaxis... should probably make this optional
	// also offsetting to place within glyph bounds
	coord.x = xcoords[i];
	ytemp = ycoords[i];
	// flattening any points > getVisibleMaxY() or < getVisibleMinY()...
	if (ytemp > getVisibleMaxY()) { ytemp = getVisibleMaxY(); }
	else if (ytemp < getVisibleMinY()) { ytemp = getVisibleMinY(); }
	//	coord.y = offset - ((ycoords[i] - getVisibleMinY()) * yscale);
	coord.y = offset - ((ytemp - getVisibleMinY()) * yscale);
	view.transformToPixels(coord, curr_point);

        g.setColor(this.getColor());

	if (graph_style == LINE_GRAPH) {
	  g.drawLine(prev_point.x + xpix_offset, prev_point.y, curr_point.x + xpix_offset, curr_point.y);
	}
	else if (graph_style == BAR_GRAPH) {
	  g.drawLine(curr_point.x + xpix_offset, zero_point.y, curr_point.x + xpix_offset, curr_point.y);
	}
	else if (graph_style == DOT_GRAPH) {
	  g.fillRect(curr_point.x + xpix_offset, curr_point.y, 1, 1);
	}
	else if (graph_style == HEAT_MAP) {
	  //       y = m(x-xmin) + ymin;
	  //   and for heatmap bins, ymin = 0,
	  //     so y = m(x-xmin)
	  //	  int heatmap_index = (int)(heatmap_scaling * (ytemp - getVisibleMinY()));
	  //	  double heatmap_index = heatmap_scaling * (ytemp - getVisibleMinY());
	  double heatmap_index = heatmap_scaling * (prev_ytemp - getVisibleMinY());
	  if (heatmap_index < 0) { heatmap_index = 0; }
	  else if (heatmap_index > 255) { heatmap_index = 255; }
	  g.setColor(heatmap_colors[(int)heatmap_index]);
	  g.fillRect(prev_point.x, pixelbox.y,
		     curr_point.x - prev_point.x, pixelbox.height);
	}
	else if (graph_style == SPAN_GRAPH) {
	  // xstarts are even positions in xcoords array, xends are odd positions in xcoords array,
	  //   so only want to start drawing a rectangle on odd positions (and back-calculate xstart
	  if ((i % 2) != 0) {
	    int xpixend = curr_point.x;
	    coord.x = xcoords[i-1];
	    view.transformToPixels(coord, curr_point);
	    int xpixbeg = curr_point.x;
	    g.fillRect(xpixbeg, pixelbox.y+pixelbox.height/2,
		       Math.max((xpixend-xpixbeg), 1), pixelbox.height/2);
	  }
	}
	else if (graph_style == STAIRSTEP_GRAPH) {
	  if (i<=0 || (!(hide_zero_points && ycoords[i-1] == 0))) {
	    int stairwidth = curr_point.x - prev_point.x;
	    //	    if ((stairwidth < 0) || (stairwidth > (view.getPixelBox().width-5))) {
	    if ((stairwidth < 0) || (stairwidth > 10000)) {
	      // skip drawing if width > 10000?  testing fix for linux problem
	    }
	    else {
	      g.fillRect(prev_point.x, Math.min(zero_point.y, prev_point.y),
			 Math.max(1, (curr_point.x - prev_point.x)),
			 Math.max(1, (Math.abs(prev_point.y - zero_point.y))) );
	    }
	  }
	}
	prev_point.x = curr_point.x;
	prev_point.y = curr_point.y;
	prev_ytemp = ytemp;
      }

    }

    // drawing the "handle", which is the only part of the graph that recognizes hits
    // not a normal "child", so if it is hit then graph is considered to be hit...
    if (getShowHandle()) {
      drawHandle(view);
    }
    if (getShowAxis()) {
      drawAxisLabel(view);
    }

    // drawing outline around bounding box
    if (getShowBounds()) {
      g.setColor(Color.green);
      g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width-1, pixelbox.height-1);
    }

    if (getShowLabel()) {
      drawLabel(view);
    }
    if (TIME_DRAWING) { System.out.println("graph draw time: " + tim.read()); }
  }

  public void drawLabel(ViewI view) {
    if (label == null) { return; }
    Rectangle hpix = calcHandlePix(view);
    Graphics g = view.getGraphics();
    g.setColor(Color.lightGray);
    g.setFont(default_font);
    FontMetrics fm = g.getFontMetrics();
    g.drawString(label, (hpix.x + hpix.width + 1), (hpix.y + fm.getMaxAscent() - 1));
  }

  public void drawHandle(ViewI view) {
    Rectangle hpix = calcHandlePix(view);
    Graphics g = view.getGraphics();
    g.setColor(this.getColor());
    g.fillRect(hpix.x, hpix.y, hpix.width, hpix.height);
    g.setColor(Color.gray);
    g.drawRect(hpix.x, hpix.y, hpix.width, hpix.height);
  }

  public void drawAxisLabel(ViewI view) {
    Graphics g = view.getGraphics();
    Rectangle hpix = calcHandlePix(view);

    getInternalLinearTransform(view, scratch_trans);
    double yscale = scratch_trans.getScaleY();
    double yoffset = scratch_trans.getOffsetY();

    coord.y = yoffset;
    view.transformToPixels(coord, curr_point);
    double max_ypix = curr_point.y;
    coord.y = yoffset - ((getVisibleMaxY() - getVisibleMinY()) * yscale);
    view.transformToPixels(coord, curr_point);
    double min_ypix = curr_point.y;
    double pix_height = max_ypix - min_ypix;
    double spacing = pix_height / axis_bins;
    double mark_ypix = min_ypix;
    g.setColor(Color.gray);
    for (int i=0; i<=axis_bins; i++) {
      g.fillRect(hpix.x + 10, (int)(mark_ypix), 10, 1);
      mark_ypix += spacing;
    }
    g.setColor(Color.gray);
    g.setFont(axis_font);
    g.drawString(nformat.format(getVisibleMinY()), hpix.x + 20, (int)max_ypix - 2);
    g.drawString(nformat.format(getVisibleMaxY()), hpix.x + 20, (int)min_ypix + 12);
  }

  /** Draws the outline in a way that looks good for tiers.  With other glyphs,
   *  the outline is usually drawn a pixel or two larger than the glyph.
   *  With TierGlyphs, it is better to draw the outline inside of or contiguous
   *  with the glyphs borders.
   *  This method assumes the tiers are horizontal.
   *  The left and right border are taken from the view's pixel box,
   *  the top and bottom border are from the coord box.
   **/
  protected void drawSelectedOutline(ViewI view) {
    draw(view);

    Rectangle view_pixbox = view.getPixelBox();
    Graphics g = view.getGraphics();
    g.setColor(view.getScene().getSelectionColor());
    view.transformToPixels(getPositiveCoordBox(), pixelbox);
    g.drawRect(view_pixbox.x, pixelbox.y,
               view_pixbox.width-1, pixelbox.height-1);
    if (THICK_OUTLINE) {
      g.drawRect(view_pixbox.x+1, pixelbox.y+1,
               view_pixbox.width-3, pixelbox.height-3);
    }
  }


  boolean mutable_xcoords = true;
  public void moveRelative(double xdelta, double ydelta) {
    super.moveRelative(xdelta, ydelta);
    state.setGraphHeight(coordbox.height);
    state.setGraphYPos(coordbox.y);
    if (xcoords != null && mutable_xcoords && xdelta != 0.0f) {
      int maxi = xcoords.length;
      for (int i=0; i<maxi; i++) {
	xcoords[i] += xdelta;
      }
    }
  }

  public void setCoords(double newx, double newy, double newwidth, double newheight) {
    super.setCoords(newx, newy, newwidth, newheight);
    state.setGraphHeight(newheight);
    state.setGraphYPos(newy);
  }

  /**
   *  Designed to work in combination with pickTraversal().
   *  If called outside of pickTraversal(), may get the wrong answer
   *      since won't currently take account of nested transforms, etc.
   */
  public boolean hit(Rectangle2D coord_hitbox, ViewI view) {
    // within bounds of graph ?
    if (getShowHandle() && isVisible() && coord_hitbox.intersects(coordbox)) {
      // overlapping handle ?  (need to do this one in pixel space?)
      view.transformToPixels(coord_hitbox, pixel_hitbox);
      Rectangle hpix = calcHandlePix(view);
      if (hpix.intersects(pixel_hitbox)) { return true; }
    }
    return false;
  }

  protected Rectangle calcHandlePix(ViewI view) {
    // could cache pixelbox of handle, but then will have problems if try to
    //    have multiple views on same scene / glyph hierarchy
    // therefore reconstructing handle pixel bounds here... (although reusing same object to
    //    cut down on object creation)
      view.transformToPixels(coordbox, pixelbox);
      Rectangle view_pixbox = view.getPixelBox();
      int xbeg = Math.max(view_pixbox.x, pixelbox.x);
      if (LARGE_HANDLE) {
	handle_pixbox.setBounds(xbeg, pixelbox.y, handle_width, pixelbox.height);
      }
      else {
	handle_pixbox.setBounds(xbeg, pixelbox.y + (pixelbox.height/2) - handle_width/2,
			      handle_width, handle_height);
      }
      return handle_pixbox;
  }

  /**
   *  This will replace any previous setting of maxy and miny!
   *
   */
  /*
  public void setPointCoords(int xcoords[], float ycoords[]) {
    this.xcoords = xcoords;
    this.ycoords = ycoords;
    point_min_ycoord = Float.POSITIVE_INFINITY;
    point_max_ycoord = Float.NEGATIVE_INFINITY;
    for (int i=0; i<ycoords.length; i++) {
      if (ycoords[i] < point_min_ycoord) { point_min_ycoord = ycoords[i]; }
      if (ycoords[i] > point_max_ycoord) { point_max_ycoord = ycoords[i]; }
    }
    if (point_max_ycoord == point_min_ycoord) {
      point_min_ycoord = point_max_ycoord - 1;
    }
    //    System.out.println("min: " + min_ycoord + ", max: " + getVisibleMaxY());
    //    auto_adjust_visible = false;
    if (getVisibleMinY() == Float.POSITIVE_INFINITY ||
	getVisibleMinY() == Float.NEGATIVE_INFINITY ||
	getVisibleMaxY() == Float.POSITIVE_INFINITY ||
	getVisibleMaxY() == Float.NEGATIVE_INFINITY) {
      setVisibleMaxY(point_max_ycoord);
      setVisibleMinY(point_min_ycoord);
    }
  }
  */

  /**
   *  getGraphMaxY() returns max ycoord (in graph coords) of all points in graph.
   *  This number is calculated in setPointCoords() directly fom ycoords, and cannot
   *     be modified (except for resetting the points by calling setPointCoords() again)
   */
  public float getGraphMinY() { return point_min_ycoord; }
  public float getGraphMaxY() { return point_max_ycoord; }

  /**
   *  getVisibleMaxY() returns max ycoord (in graph coords) that is visible (rendered).
   *  This number can be modified via calls to setVisibleMaxY, and the visual effect is
   *     to threhsold the graph drawing so that any points above max_ycoord render as max_ycoord
   */
  public float getVisibleMaxY() { return state.getVisibleMaxY(); }
  public float getVisibleMinY() { return state.getVisibleMinY(); }

  /**
   *  If want to override default setting of y min and max (based on setPointCoords()),
   *    then must call setMaxY() and setMinY() _after_ call to setPointCoords() --
   *    any subsequent call to setPointCoords will again reset y min and max.
   */
  public void setVisibleMaxY(float ymax) {
    state.setVisibleMaxY(ymax);
  }

  /**
   *  If want to override default setting of y min and max (based on setPointCoords()),
   *    then must call setMaxY() and setMinY() _after_ call to setPointCoords() --
   *    any subsequent call to setPointCoords will again reset y min and max.
   */
  public void setVisibleMinY(float ymin) {
    state.setVisibleMinY(ymin);
  }

  public void setColor( Color c ) {
    setBackgroundColor( c );
    setForegroundColor( c );
    state.setColor(c);
  }

  public void setLabel(String str) {
    this.label = str;
  }

  public String getLabel() {
    return label;
  }

  public boolean getShowGraph() { return state.getShowGraph(); }
  public boolean getShowBounds() { return state.getShowBounds();  }
  public boolean getShowHandle() { return state.getShowHandle(); }
  public boolean getShowLabel() { return state.getShowLabel(); }
  public boolean getShowAxis() { return state.getShowAxis(); }
  public int getXPixelOffset() { return xpix_offset; }

  public void setShowGraph(boolean show) { state.setShowGraph(show); }
  public void setShowHandle(boolean show) { state.setShowHandle(show); }
  public void setShowBounds(boolean show) { state.setShowBounds(show); }
  public void setShowLabel(boolean show) { state.setShowLabel(show); }
  public void setShowAxis(boolean b) { state.setShowAxis(b); }
  public void setXPixelOffset(int offset) { xpix_offset = offset; }

  public void setGraphStyle(int type) {
    state.setGraphStyle(type);
    if (type == HEAT_MAP && heatmap_colors == null) {
      initHeatMap(default_heatmap_colors);
    }
  }
  public int getGraphStyle() { return state.getGraphStyle(); }

  public int[] getXCoords() { return xcoords; };
  public float[] getYCoords() { return ycoords; }

  public int getPointCount() {
    if (xcoords == null) { return 0; }
    else { return xcoords.length; }
  }

  /*  public void initHeatMap() {
    heatmap_colors = new Color[heatmap_bins];
    for (int i=0; i<heatmap_bins; i++) {
      heatmap_colors[i] = new Color(i, 0, i);
    }
  }
  */

  public void initHeatMap(Color[] colors) {
    if (colors == null) {
      System.out.println("GraphGlyph.initHeatMap called with a null color gradient, ignoring");
    }
    else {
      heatmap_colors = colors;
      heatmap_bins = heatmap_colors.length;
    }
  }

  public void getChildTransform(ViewI view, LinearTransform trans) {
    double external_yscale = trans.getScaleY();
    double external_offset = trans.getOffsetY();
    double internal_yscale = coordbox.height / (getVisibleMaxY() - getVisibleMinY());
    double internal_offset = coordbox.y + coordbox.height;

    // double new_yscale = internal_yscale * external_yscale;
    //    double new_yoffset = (external_yscale * internal_offset) + external_offset;
    double new_yscale = internal_yscale * external_yscale * -1;
    double new_yoffset =
      (external_yscale * internal_offset) +
      (external_yscale * internal_yscale * getVisibleMinY()) +
      external_offset;
    trans.setScaleY(new_yscale);
    trans.setOffsetY(new_yoffset);
  }

  protected double getUpperYCoordInset(ViewI view) {
    double top_ycoord_inset = 0;
    if (getShowLabel()) {
      Graphics g = view.getGraphics();
      g.setFont(default_font);
      FontMetrics fm = g.getFontMetrics();
      label_pix_box.height = fm.getAscent() + fm.getDescent();
      view.transformToCoords(label_pix_box, label_coord_box);
      top_ycoord_inset = label_coord_box.height;
    }
    /*
    else {  // GAH 3-21-2005
      label_pix_box.height = 4;
      view.transformToCoords(label_pix_box, label_coord_box);
      top_ycoord_inset = label_coord_box.height;
    }
    */
    return top_ycoord_inset;
  }

  protected double getLowerYCoordInset(ViewI view) {
    //    return 0;
    return 5;  // GAH 3-21-2005
  }

  //  public double getInternalYScale(ViewI view) {
  protected void getInternalLinearTransform(ViewI view, LinearTransform lt) {
    Graphics g = view.getGraphics();
    double top_ycoord_inset = getUpperYCoordInset(view);
    double bottom_ycoord_inset = getLowerYCoordInset(view);
    double yscale = (coordbox.height - top_ycoord_inset - bottom_ycoord_inset) / (getVisibleMaxY() - Math.min(0, getVisibleMinY()));
    double yoffset = coordbox.y + coordbox.height - bottom_ycoord_inset;
    lt.setScaleY(yscale);
    lt.setOffsetY(yoffset);
  }


}


