/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

import com.affymetrix.genometryImpl.GraphSym;
import java.awt.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometryImpl.style.GraphStateI;
import com.affymetrix.genometryImpl.style.HeatMap;

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

  public static final int LINE_GRAPH = GraphStateI.LINE_GRAPH;
  public static final int BAR_GRAPH = GraphStateI.BAR_GRAPH;
  public static final int DOT_GRAPH = GraphStateI.DOT_GRAPH;
  public static final int STAIRSTEP_GRAPH = GraphStateI.STAIRSTEP_GRAPH;
  public static final int HEAT_MAP = GraphStateI.HEAT_MAP;

  int xpix_offset = 0;
  Point zero_point = new Point(0,0);
  Point2D coord = new Point2D(0,0);
  Point curr_point = new Point(0,0);
  Point prev_point = new Point(0,0);
  Point scratch_point = new Point(0,0);

  Rectangle2D label_coord_box = new Rectangle2D();
  Rectangle label_pix_box = new Rectangle();

  com.affymetrix.genoviz.util.Timer tim = new com.affymetrix.genoviz.util.Timer();

  boolean LARGE_HANDLE = true;
  boolean show_min_max = false;  // drawing lines for getVisibleMinY() and getVisibleMaxY() for debugging

  /**
   *  point_max_ycoord is the max ycoord (in graph coords) of all points in graph.
   *  This number is calculated in setPointCoords() directly fom ycoords, and cannot
   *     be modified (except for resetting the points by calling setPointCoords() again)
   */
  float point_max_ycoord = Float.NEGATIVE_INFINITY;
  float point_min_ycoord = Float.POSITIVE_INFINITY;

  // assumes sorted points, each x corresponding to y
  int xcoords[];
  int wcoords[];
  GraphSym graf;
//  float ycoords[];

  int handle_width = 10;  // width of handle in pixels
  int handle_height = 5;  // height of handle in pixels

  Rectangle handle_pixbox = new Rectangle(); // caching rect for handle pixel bounds
  Rectangle pixel_hitbox = new Rectangle();  // caching rect for hit detection

  GraphStateI state;
  LinearTransform scratch_trans = new LinearTransform();

  public float getYCoord(int i) {
    return graf.getGraphYCoord(i);
  }
  
  public GraphGlyph(int[] xcoords, GraphSym graf, GraphStateI gstate) {
    this(xcoords, null, graf, gstate);
  }

  public GraphGlyph(int[] xcoords, int[] wcoords, GraphSym graf, GraphStateI gstate) {
    super();
    state = gstate;
    if (state == null) {
      throw new NullPointerException();
    }

    setCoords(coordbox.x, state.getTierStyle().getY(), coordbox.width, state.getTierStyle().getHeight());
    setColor(state.getTierStyle().getColor());
    setGraphStyle(state.getGraphStyle());

    if (xcoords == null || xcoords.length <=0 || graf.getPointCount() <= 0) { return; }
    if (wcoords != null) {
      if (wcoords.length != xcoords.length || wcoords.length != graf.getPointCount()) { return; }
    }
    this.xcoords = xcoords;
    this.wcoords = wcoords;
    //this.ycoords = ycoords;
    this.graf = graf;
    point_min_ycoord = Float.POSITIVE_INFINITY;
    point_max_ycoord = Float.NEGATIVE_INFINITY;
    float f;
    for (int i=0; i<graf.getPointCount(); i++) {
      f = graf.getGraphYCoord(i);
      if (f < point_min_ycoord) { point_min_ycoord = f; }
      if (f > point_max_ycoord) { point_max_ycoord = f; }
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

  public String getID() {
    Object mod = this.getInfo();
    String ident = null;
    if (mod instanceof SeqSymmetry) {
      ident = ((SeqSymmetry)mod).getID();
    }
    if (ident == null) {
      ident = state.getTierStyle().getUniqueName();
    }
    return ident;
  }

  public GraphStateI getGraphState() { return state; }

  // temporary variables used in draw()
  Point2D x_plus_width2D = new Point2D(0,0);
  Point curr_x_plus_width = new Point(0,0);

  public void draw(ViewI view) {
    if (TIME_DRAWING) { tim.start(); }
    int graph_style = getGraphStyle();
    view.transformToPixels(coordbox, pixelbox);
    int pbox_yheight = pixelbox.y + pixelbox.height;
    Graphics g = view.getGraphics();
    getInternalLinearTransform(view, scratch_trans);
    double yscale = scratch_trans.getScaleY();
    double offset = scratch_trans.getOffsetY();
    //    System.out.println("offset: " + offset);

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
    Color[] heatmap_colors = null;
    double heatmap_scaling = 1;
    if (state.getHeatMap() != null) {
      heatmap_colors = state.getHeatMap().getColors();
      heatmap_scaling = (double)(heatmap_colors.length - 1) / (getVisibleMaxY() - getVisibleMinY());
    }

    //    Rectangle view_pixbox = view.getPixelBox();
    Rectangle2D view_coordbox = view.getCoordBox();
    double xmin = view_coordbox.x;
    double xmax = view_coordbox.x + view_coordbox.width;

    if (getShowGraph() && graf != null && xcoords != null  && graf.getPointCount() == xcoords.length)  {
      int beg_index = 0;
      //int end_index = xcoords.length-1;

      if (show_min_max) {
	//	Point scratch_point = new Point();
	coord.y = offset;  // visible min, since = offset - ((getVisibleMinY() - getVisibleMinY()) * yscale);
	view.transformToPixels(coord, scratch_point);
	g.setColor(Color.yellow);
 	g.drawLine( pixelbox.x, scratch_point.y, ( pixelbox.x + pixelbox.width ), scratch_point.y );
	coord.y = offset - ((getVisibleMaxY() - getVisibleMinY()) * yscale);
	view.transformToPixels(coord, scratch_point);
	g.setColor(Color.blue);
 	g.drawLine( pixelbox.x, scratch_point.y, ( pixelbox.x + pixelbox.width ), scratch_point.y );
      }
      
      float yzero = 0;
      if (getVisibleMinY() > yzero) { yzero = getVisibleMinY(); }
      else if (getVisibleMaxY() < yzero) { yzero = getVisibleMaxY(); }
      //      coord.y = offset - ((0 - getVisibleMinY()) * yscale);
      coord.y = offset - ((yzero - getVisibleMinY()) * yscale);
      view.transformToPixels(coord, zero_point);
      //      if (zero_point.y < pixelbox.y)  { zero_point.y = pixelbox.y; }
      //      else if (zero_point.y > pbox_yheight) { zero_point.y = pbox_yheight; }
      if (getGraphState().getShowZeroLine() && 
	  graph_style != HEAT_MAP && 
	  yzero == 0)  {// zero_point within min/max, so draw
	g.setColor(Color.gray);
 	g.drawLine( pixelbox.x, zero_point.y, ( pixelbox.x + pixelbox.width ), zero_point.y );
      }

      g.setColor(this.getColor());

      // set up prev_point before starting loop
      coord.x = xcoords[beg_index];
      coord.y = offset - ((graf.getGraphYCoord(beg_index) - getVisibleMinY()) * yscale);
      view.transformToPixels(coord, prev_point);
      float prev_ytemp = graf.getGraphYCoord(beg_index);

      Point max_x_plus_width = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);

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

      // figure out what is the last x index value for the loop
      if (draw_end_index >= xcoords.length) {
        if (graph_style == HEAT_MAP || graph_style == DOT_GRAPH) {
          draw_end_index = xcoords.length - 1;
        } else {
          draw_end_index = xcoords.length - 2;
        }
      }

      float ytemp;
      int ymin_pixel, yheight_pixel;
      int curr_max_index = 0; // used for heatmaps

      g.translate(xpix_offset, 0);

      RenderingHints original_render_hints = null;
      if (g instanceof Graphics2D) {
        Graphics2D g2 = (Graphics2D) g;
        original_render_hints = g2.getRenderingHints();
        Map my_render_hints = new HashMap();
        my_render_hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g2.addRenderingHints(my_render_hints);
      }
      
      // START OF BIG LOOP:
      for (int i = draw_beg_index; i <= draw_end_index; i++) {
        // flipping about yaxis... should probably make this optional
        // also offsetting to place within glyph bounds
        coord.x = xcoords[i];
        ytemp = graf.getGraphYCoord(i);
        // flattening any points > getVisibleMaxY() or < getVisibleMinY()...
	if (ytemp > getVisibleMaxY()) { ytemp = getVisibleMaxY(); } 
	else if (ytemp < getVisibleMinY()) { ytemp = getVisibleMinY(); }
        coord.y = offset - ((ytemp - getVisibleMinY()) * yscale);
        view.transformToPixels(coord, curr_point);
	//	if (curr_point.y > pbox_yheight) { curr_point.y = pbox_yheight; }
	//	else if (curr_point.y < pixelbox.y) { curr_point.y = pixelbox.y; }

        if (wcoords != null) {
          x_plus_width2D.x = xcoords[i] + wcoords[i];
          x_plus_width2D.y = coord.y;
          view.transformToPixels(x_plus_width2D, curr_x_plus_width);
	  //	  if (curr_x_plus_width.y > pbox_yheight) { curr_x_plus_width.y = pbox_yheight; }
	  //	  else if (curr_x_plus_width.y < pixelbox.y) { curr_x_plus_width.y = pixelbox.y; }
        }

        // g.setColor(this.getColor()); Don't keep resetting color!

        if (graph_style == LINE_GRAPH) {
          if (wcoords == null) {
            g.drawLine(prev_point.x, prev_point.y,
                curr_point.x, curr_point.y);
          } else {
            // Draw a line representing the width: (x,y) to (x + width,y)
            g.drawLine(curr_point.x, curr_point.y,
                curr_x_plus_width.x, curr_x_plus_width.y);

            // Usually draw a line from (xA + widthA,yA) to next (xB,yB), but when there
            // are overlapping spans, only do this from the largest previous (x+width) value
            // to an xA that is larger than that.
            if (curr_point.x >= max_x_plus_width.x && max_x_plus_width.x != Integer.MIN_VALUE) {
              g.drawLine(max_x_plus_width.x, max_x_plus_width.y,
                  curr_point.x, curr_point.y);
            }
            if (curr_x_plus_width.x >= max_x_plus_width.x) {
              max_x_plus_width.x = curr_x_plus_width.x; // xB + widthB
              max_x_plus_width.y = curr_x_plus_width.y; // yB
            }
          }
        } 
	else if (graph_style == BAR_GRAPH) {
          if (curr_point.y > zero_point.y) {
            ymin_pixel = zero_point.y;
            yheight_pixel = curr_point.y - zero_point.y;
          } else {
            ymin_pixel = curr_point.y;
            yheight_pixel = zero_point.y - curr_point.y;
          }
          if (yheight_pixel < 1) { yheight_pixel = 1; }

          if (wcoords == null) {
            g.fillRect(curr_point.x, ymin_pixel, 1, yheight_pixel + 1);
          } else {
            final int width = Math.max(1, curr_x_plus_width.x - curr_point.x);
            g.drawRect(curr_point.x, ymin_pixel, width, yheight_pixel);
          }
        }
	else if (graph_style == DOT_GRAPH) {
          if (wcoords == null) {
            g.fillRect(curr_point.x, curr_point.y, 1, 1);
          }
          else {
            g.drawLine(curr_point.x, curr_point.y, curr_x_plus_width.x, curr_point.y);
          }
        } else if (graph_style == HEAT_MAP) {


          if (wcoords == null) { // there are no wcoords, so bars go from previous x to current x (like stairstep graphs)
            // When multiple coords map to one pixel, use the color corresponding to the max value.
            float the_y = prev_ytemp;
            int heatmap_index = (int) (heatmap_scaling * (the_y - getVisibleMinY()));
                        
            if (heatmap_index < 0) { heatmap_index = 0; } else if (heatmap_index > 255) { heatmap_index = 255; }
            
            if (heatmap_index > curr_max_index) { curr_max_index = heatmap_index; }

            if (curr_point.x == prev_point.x) {
              g.setColor(heatmap_colors[curr_max_index]);
              g.fillRect(prev_point.x, pixelbox.y,
                1, pixelbox.height+1);
            }
            else {
              g.setColor(heatmap_colors[heatmap_index]);
              // the x+1 start point prevents this from over-writing the last rectangle
              g.fillRect(prev_point.x + 1, pixelbox.y,
                  curr_point.x - prev_point.x, pixelbox.height+1);
              curr_max_index = 0;
            }
          }
          else { // the wcoords are not null, so the bars have width
            float the_y = ytemp;
            int heatmap_index = (int) (heatmap_scaling * (the_y - getVisibleMinY()));
            if (heatmap_index < 0) { heatmap_index = 0; } else if (heatmap_index > 255) { heatmap_index = 255; }
            int pixel_width = curr_x_plus_width.x - curr_point.x;
            if (pixel_width < 1) { pixel_width = 1; }
            g.setColor(heatmap_colors[heatmap_index]);
            g.fillRect(curr_point.x, pixelbox.y,
                pixel_width, pixelbox.height+1);
          }
        }
//	else if (graph_style == SPAN_GRAPH) {
//	  // xstarts are even positions in xcoords array, xends are odd positions in xcoords array,
//	  //   so only want to start drawing a rectangle on odd positions (and back-calculate xstart
//	  if ((i % 2) != 0) {
//	    int xpixend = curr_point.x;
//	    coord.x = xcoords[i-1];
//	    view.transformToPixels(coord, curr_point);
//	    int xpixbeg = curr_point.x;
//	    g.fillRect(xpixbeg, pixelbox.y+pixelbox.height/2,
//		       Math.max((xpixend-xpixbeg), 1), pixelbox.height/2);
//	  }
//	}
        else if (graph_style == STAIRSTEP_GRAPH) {
          if (i<=0 || (graf.getGraphYCoord(i-1) != 0)) {
            int stairwidth = curr_point.x - prev_point.x;
            if ((stairwidth < 0) || (stairwidth > 10000)) {
              // skip drawing if width > 10000?  testing fix for linux problem
            } else {
              // draw the same regardless of whether wcoords == null
              g.fillRect(prev_point.x, Math.min(zero_point.y, prev_point.y),
                  Math.max(1, stairwidth),
                  Math.max(1, (Math.abs(prev_point.y - zero_point.y))) );
            }
          }
          // If this is the very last point, special rules apply
          if (i == draw_end_index) {
            int stairwidth = (wcoords == null) ? 1 : curr_x_plus_width.x - curr_point.x;
            g.fillRect(curr_point.x, Math.min(zero_point.y, curr_point.y),
                Math.max(1, stairwidth),
                Math.max(1, (Math.abs(curr_point.y - zero_point.y))) );
          }
        }
        prev_point.x = curr_point.x;
        prev_point.y = curr_point.y;
        prev_ytemp = ytemp;
      }
      // END: big loop      
      
      g.translate(-xpix_offset, 0);
      if (g instanceof Graphics2D) {
        Graphics2D g2 = (Graphics2D) g;
        if (original_render_hints != null) {
          g2.setRenderingHints(original_render_hints);
        }
      }

      //      System.out.println("draw count: " + draw_count);

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
    if (state.getShowLabelOnRight()) {
      drawLabelRight(view);
    } else {
      drawLabelLeft(view);
    }
  }
  
  public void drawLabelRight(ViewI view) {
    if (getLabel() == null) { return; }

    // if full view differs from current view, and current view doesn't right align with full view,
    //   don't draw handle (only want handle at right side of full view)
    if (view.getFullView().getCoordBox().x + view.getFullView().getCoordBox().width 
        != view.getCoordBox().x + view.getCoordBox().width)  {
      return;
    }

    view.transformToPixels(coordbox, pixelbox);
    Rectangle view_pixbox = view.getPixelBox();
    
    if (view_pixbox != null) {
      Graphics g = view.getGraphics();
      g.setFont(default_font);
      FontMetrics fm = g.getFontMetrics();
      java.awt.geom.Rectangle2D sb = fm.getStringBounds(getLabel(), g);
      int stringWidth = (int) sb.getWidth() + 1;
      
//      int stringHeight = fm.getMaxAscent() - 1;
//      int x = view_pixbox.x + view_pixbox.width - stringWidth;
//      int width = view_pixbox.x + view_pixbox.width - x;
//      
//      Color c = new Color(this.getColor().getRed(), this.getColor().getGreen(), this.getColor().getBlue(), 64);
//      Color c = new Color(0, 0, 0, 64);
//      g.setColor(c);
//      g.fillRect(x, pixelbox.y, width, stringHeight);
//      g.setColor(getColor());
//      g.drawString(getLabel(), x, pixelbox.y + stringHeight);
      
      g.setColor(getColor());
      g.drawString(getLabel(), (view_pixbox.x + view_pixbox.width - stringWidth), (pixelbox.y + fm.getMaxAscent() - 1));
    }
  }

  public void drawLabelLeft(ViewI view) {
    if (getLabel() == null) { return; }
    Rectangle hpix = calcHandlePix(view);
    if (hpix != null) {
      Graphics g = view.getGraphics();
      g.setColor(this.getColor());
      g.setFont(default_font);
      FontMetrics fm = g.getFontMetrics();
      g.drawString(getLabel(), (hpix.x + hpix.width + 1), (hpix.y + fm.getMaxAscent() - 1));
    }
  }

  //Color handleColor = new Color(128, 128, 128, 64);
  
  public void drawHandle(ViewI view) {
    Rectangle hpix = calcHandlePix(view);
    if (hpix != null) {
      Graphics g = view.getGraphics();
      Color c = new Color(this.getColor().getRed(), this.getColor().getGreen(), this.getColor().getBlue(), 64);
      g.setColor(c);
      g.fillRect(hpix.x, hpix.y, hpix.width, hpix.height);
//      g.setColor(Color.gray);
      g.drawRect(hpix.x, hpix.y, hpix.width, hpix.height);
    }
  }

  public void drawAxisLabel(ViewI view) {
    Graphics g = view.getGraphics();
    Rectangle hpix = calcHandlePix(view);

    if (hpix != null) {
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
      g.setColor(this.getColor());
      for (int i=0; i<=axis_bins; i++) {
        if (i==0 || i == axis_bins) {
          g.fillRect(hpix.x + hpix.width, (int)(mark_ypix) - 1, 12, 2);
        } else {
          g.fillRect(hpix.x + hpix.width, (int)(mark_ypix), 8, 1);
        }
	mark_ypix += spacing;
      }
      g.setColor(this.getColor());
      g.setFont(axis_font);
      FontMetrics fm = g.getFontMetrics();
      g.drawString(nformat.format(getVisibleMinY()), hpix.x + 25, (int)max_ypix - fm.getDescent());
      g.drawString(nformat.format(getVisibleMaxY()), hpix.x + 25, (int)min_ypix + fm.getAscent() + 1);
    }
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
//    g.drawRect(view_pixbox.x, pixelbox.y,
//               view_pixbox.width-1, pixelbox.height-1);
//    if (THICK_OUTLINE) {
//      g.drawRect(view_pixbox.x+1, pixelbox.y+1,
//               view_pixbox.width-3, pixelbox.height-3);
//    }
    
    // only outline the handle, not the whole graph
    g.drawRect(view_pixbox.x, pixelbox.y,
               handle_width-1, pixelbox.height-1);
    if (THICK_OUTLINE) {
      g.drawRect(view_pixbox.x+1, pixelbox.y+1,
               handle_width-3, pixelbox.height-3);
    }
    
    // also draw a little pointing triangle to make the selection stand-out more
    int[] xs = {view_pixbox.x + handle_width, view_pixbox.x + 2*handle_width, view_pixbox.x + handle_width};
    int[] ys = {pixelbox.y,  pixelbox.y + (int) (0.5*(pixelbox.height-1)), pixelbox.y + pixelbox.height-1};
    g.fillPolygon(xs, ys, 3);
  }


  boolean mutable_xcoords = true;
  public void moveRelative(double xdelta, double ydelta) {
    super.moveRelative(xdelta, ydelta);
    state.getTierStyle().setHeight(coordbox.height);
    state.getTierStyle().setY(coordbox.y);
    if (xcoords != null && mutable_xcoords && xdelta != 0.0f) {
      int maxi = xcoords.length;
      for (int i=0; i<maxi; i++) {
	xcoords[i] += xdelta;
      }
    }
  }

  public void setCoords(double newx, double newy, double newwidth, double newheight) {
    super.setCoords(newx, newy, newwidth, newheight);
    state.getTierStyle().setHeight(newheight);
    state.getTierStyle().setY(newy);
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
      if (hpix != null && (hpix.intersects(pixel_hitbox))) { return true; }
    }
    return false;
  }

  protected Rectangle calcHandlePix(ViewI view) {
    // could cache pixelbox of handle, but then will have problems if try to
    //    have multiple views on same scene / glyph hierarchy
    // therefore reconstructing handle pixel bounds here... (although reusing same object to
    //    cut down on object creation)
    //    System.out.println("comparing full view cbox.x: " + view.getFullView().getCoordBox().x +
    //		       ", view cbox.x: " + view.getCoordBox().x);

    // if full view differs from current view, and current view doesn't left align with full view,
    //   don't draw handle (only want handle at left side of full view)
    if (view.getFullView().getCoordBox().x != view.getCoordBox().x)  {
      return null;
    }
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
    state.getTierStyle().setColor(c);
  }

  public String getLabel() {
    String lab = state.getTierStyle().getHumanName();
    // If it has a combo style and that is collapsed, then only use the label
    // from the combo style.  Otherwise use the individual tier style.
    if (state.getComboStyle() != null && state.getComboStyle().getCollapsed()) {
      lab = state.getComboStyle().getHumanName();
    }
    if (lab == null) {
      // if no label was set, try using ID
      lab = getID();
    }
    return lab;
  }

  public boolean getShowGraph() { return state.getShowGraph(); }
  public boolean getShowBounds() { return state.getShowBounds();  }
  public boolean getShowHandle() { return state.getShowHandle(); }
  public boolean getShowLabel() { return state.getShowLabel(); }
  public boolean getShowAxis() { return state.getShowAxis(); }
  public int getXPixelOffset() { return xpix_offset; }

//  public void setLabel(String str) { state.setLabel(str); }
  public void setShowGraph(boolean show) { state.setShowGraph(show); }
  public void setShowHandle(boolean show) { state.setShowHandle(show); }
  public void setShowBounds(boolean show) { state.setShowBounds(show); }
  public void setShowLabel(boolean show) { state.setShowLabel(show); }
  public void setShowAxis(boolean b) { state.setShowAxis(b); }
  public void setXPixelOffset(int offset) { xpix_offset = offset; }

  public void setGraphStyle(int type) {
    state.setGraphStyle(type);
    if (type == HEAT_MAP) {
      setHeatMap(state.getHeatMap());
    }
  }
  public int getGraphStyle() { return state.getGraphStyle(); }

  public int[] getXCoords() { return xcoords; };
  //public float[] getYCoords() { return ycoords; }
  
  /** Returns the width coordinate array or null. */
  public int[] getWCoords() { return wcoords; }
  
  public int getPointCount() {
    if (xcoords == null) { return 0; }
    else { return xcoords.length; }
  }

  public void setHeatMap(HeatMap hmap) {
    state.setHeatMap(hmap);
  }

  public HeatMap getHeatMap() {
    return state.getHeatMap();
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
    return top_ycoord_inset;
  }

  protected double getLowerYCoordInset(ViewI view) {
    //    return 0;
    return 5;  // GAH 3-21-2005
  }

  protected void getInternalLinearTransform(ViewI view, LinearTransform lt) {
    double top_ycoord_inset = getUpperYCoordInset(view);
    double bottom_ycoord_inset = getLowerYCoordInset(view);

    double num = getVisibleMaxY() - getVisibleMinY();
    if (num <= 0) { num = 0.1; } // if scale is 0 or negative, set to a small default instead

    double yscale = (coordbox.height - top_ycoord_inset - bottom_ycoord_inset) / num;
    double yoffset = coordbox.y + coordbox.height - bottom_ycoord_inset;
    lt.setScaleY(yscale);
    lt.setOffsetY(yoffset);
  }


}


