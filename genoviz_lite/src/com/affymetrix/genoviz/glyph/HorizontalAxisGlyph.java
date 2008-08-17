/**
 *   Copyright (c) 1998-2008 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ArrayList;


//TODO: Fix: minor ticks do not line-up precisely with major ticks
/**
 *  A glyph to display a horizontal numbered axis.
 */
public class HorizontalAxisGlyph extends Glyph {

  public static final int FULL = 0;
  public static final int ABBREV = FULL + 1;
  public static final int COMMA = ABBREV + 1;
  public static final int NO_LABELS = COMMA + 1;
  protected int labelFormat = FULL;

  /*
   * We use the term "thickness" for the dimension orthogonal to orientation.
   * We use the term "length" for the dimension along the orientation.
   */
  protected List selectedRegions;  // default to true for backward compatability
  private final Stroke theStroke = new BasicStroke(0);

  protected boolean hitable = true;
  protected Font labelFont;

  private static final int MAJORTICKHEIGHT = 2;  // gap between centerLine and edge of labels.
  protected static final int labelGap = MAJORTICKHEIGHT + 2;
  protected NeoConstants.Placement labelPlacement = NeoConstants.Placement.ABOVE;
  protected int labelShift = 5;
  protected int labelThickness;

  private double centerLine;
  private final static int centerLineThickness = 0;
  private Rectangle2D.Double lastCoordBox = null;

  /**
   * Sets the font in which labels will be rendered.
   *
   * @param f the font to use
   */
  protected final void internalSetFont(Font f) {
    labelFont = f;
    FontMetrics fm = GeneralUtils.getFontMetrics(f); //TODO: avoid this?
    labelThickness = fm.getAscent();
    setLabelPlacement(getLabelPlacement()); // to recalculate offsets.
  }

  /**
   * Sets the font in which labels will be rendered.
   *
   * @param fnt a new matching font will be created
   * and used internally.
   */
  @Override
  public void setFont(Font fnt) {
    if (!fnt.equals(this.labelFont)) {
      internalSetFont(fnt);
    }
  }

  @Override
  public Font getFont() {
    return labelFont;
  }

  /**
   * Sets the color in which the axis is rendered.
   *
   * <p><em>Note that the super class, Glyph,
   * sets the background color in setColor.
   * We override it here to set the foreground color
   * since that is the color used in our draw method.
   * </em></p>
   *
   * @param color the color the axis should be.
   * @deprecated use setForegroundColor().
   */
  @Deprecated
  @Override
  public void setColor(Color color) {
    this.setForegroundColor(color);
  }

  /**
   * @deprecated use getForegroundColor().
   * @see #setColor
   */
  @Deprecated
  @Override
  public Color getColor() {
    return this.getForegroundColor();
  }  //TODO: make an enum

  /**
   * Sets the label format.
   * Format {@link #ABBREV} will replace trailing "000" with a "k"
   * and trailing "000000" with an "M", etc. for "G", "T".
   * Format {@link #COMMA} will put commas in the numbers,
   * like "1,234,250".
   *
   * @param theFormat {@link #FULL} or {@link #ABBREV} or {@link #COMMA}.
   */
  public void setLabelFormat(int theFormat) {
    if (theFormat != FULL && theFormat != ABBREV && theFormat != COMMA && theFormat != NO_LABELS) {
      throw new IllegalArgumentException(
        "Label format must be FULL, ABBREV, COMMA, or NO_LABELS.");
    }
    this.labelFormat = theFormat;
  }

  //TODO: use a Range object for the range
  @SuppressWarnings("unchecked")
  public void selectRange(int[] range) {
    if (range.length != 2) {
      System.err.println("AxisGlyph.selectRange got a int[] that was not of length 2.  Not selecting range.");
      return;
    }
    if (selectedRegions == null) {
      selectedRegions = new ArrayList();
    }
    selectedRegions.add(range);
  }

  public void deselectAll() {
    selectedRegions.clear();
  }

  public int getLabelFormat() {
    return this.labelFormat;
  }  //private double tickOffset = .5f;
  protected NeoConstants.Placement tickPlacement = NeoConstants.Placement.ABOVE;

  /**
   * Places the axis ticks relative to the center line.
   *
   * @param thePlacement ABOVE or BELOW for HORIZONTAL axes,
   *                     RIGHT or LEFT for VERTICAL axes.
   */
  public void setTickPlacement(NeoConstants.Placement thePlacement) {
    switch (thePlacement) {
      case ABOVE:
        subtick_size = 1;
        break;
      case BELOW:
        subtick_size = -2;
        break;
      case CENTER:
        subtick_size = -1;
        break;
      case NONE:
        subtick_size = 0;
        break;
      default:
        throw new IllegalArgumentException("Tick placement must be ABOVE, BELOW, CENTER or NONE.");
    }
    this.tickPlacement = thePlacement;
  }

  public NeoConstants.Placement getTickPlacement() {
    return this.tickPlacement;
  }

  /**
   * Places the axis labels relative to the center line and ticks.
   *
   * <p> Bug: When placed to the LEFT labels are still left justified.
   * This leaves an unsightly gap with small numbers.
   *
   * @param thePlacement ABOVE or BELOW for HORIZONTAL axes,
   *                     RIGHT or LEFT for VERTICAL axes.
   */
  public void setLabelPlacement(NeoConstants.Placement thePlacement) {
    switch (thePlacement) {
      case ABOVE:
        labelShift = labelGap;
        break;
      case BELOW:
        labelShift = -centerLineThickness - labelGap - labelThickness;
        break;
      default:
        throw new IllegalArgumentException("Label placement must be ABOVE or BELOW.");
    }
    this.labelPlacement = thePlacement;
  }

  public NeoConstants.Placement getLabelPlacement() {
    return this.labelPlacement;
  }

  /**
   * Creates an axis.
   */
  public HorizontalAxisGlyph() {
    internalSetFont(new Font("Helvetica", Font.BOLD, 12));
    setSelectable(false);
  }

  @Override
  public void setCoords(double x, double y, double width, double height) {
    super.setCoords(x, y, width, height);
    setCenter();
  }

  @Override
  public void setCoordBox(Rectangle2D.Double coordbox) {
    super.setCoordBox(coordbox);
    setCenter();
  }

  /**
   * Centers the centerLine within this axis' coordbox.
   */
  protected void setCenter() {
    centerLine = coordbox.y + coordbox.height / 2;
    this.lastCoordBox = null;
  }

  /**
   * Places the centerLine inside the coordbox.
   * This should only be called when the coordbox has moved.
   */
  private void placeCenter(ViewI theView) {

    if (null == lastCoordBox) { // then this is the first time we've done this.

      // Mark the original placement of our coord box.
      lastCoordBox = new Rectangle2D.Double(this.coordbox.x,
        this.coordbox.y,
        this.coordbox.width,
        this.coordbox.height);

      // Center the centerLine in the original coord box.
      Rectangle2D.Double centralLine = new Rectangle2D.Double(coordbox.x, coordbox.y, 0f, 0f);
      centerLine = coordbox.y + coordbox.height / 2;
      centralLine.y = centerLine;
      centralLine.width = coordbox.width;
      theView.transformToPixels(coordbox, pixelbox);
      Rectangle centralBox = new Rectangle();
      theView.transformToPixels(centralLine, centralBox);


      // Adjust the pixel box to shrink wrap the axis.
      centralBox.y -= MAJORTICKHEIGHT;
      centralBox.height = centerLineThickness + (2 * MAJORTICKHEIGHT);
      if (NeoConstants.Placement.ABOVE == this.labelPlacement) {
        centralBox.y -= labelThickness;
        centralBox.height += labelThickness;
      } else if (NeoConstants.Placement.BELOW == this.labelPlacement) {
        centralBox.height += labelThickness;
      }

      Rectangle2D.Double temp_rect = new Rectangle2D.Double(coordbox.x, coordbox.y,
        coordbox.width, coordbox.height);
      // Readjust the coord box to match the new pixel box.
      theView.transformToCoords(centralBox, temp_rect);

      coordbox.y = temp_rect.y;
      coordbox.height = temp_rect.height;
    // leave coordbox.x and coordbox.width alone
    // (temp_rect.width will be pretty close to coordbox.width, but round-off errors in
    // the transformations can result in problems that manifest as the right
    // edge of the axis not being drawn when the zoom level is very high)
    } else {
      double r = (lastCoordBox.y - centerLine) / lastCoordBox.height;
      centerLine = this.coordbox.y - (r * this.coordbox.height);
    }
    lastCoordBox.x = coordbox.x;
    lastCoordBox.y = coordbox.y;
    lastCoordBox.width = coordbox.width;
    lastCoordBox.height = coordbox.height;

  }

  /**
   * sets the coords
   * to make sure axis spans the whole map
   * in the direction it is oriented.
   *
   * <p> A NeoMap keeps a list of its axes
   * added via the NeoMap's addAxis method.
   * Every time the NeoMap's range changes this method is called
   * for each of it's axes.
   */
  public void rangeChanged() {
    coordbox.x = parent.getCoordBox().x;
    coordbox.width = parent.getCoordBox().width;
  }  
  
  // A couple constants used only in the draw method.
  protected int subtick_size = 1;
  protected static final Rectangle2D.Double unitrect = new Rectangle2D.Double(0, 0, 1, 1);
  // A couple of temporary rectangles used in the draw method
  private final Rectangle2D.Double select_coord = new Rectangle2D.Double();
  private final Rectangle select_pix = new Rectangle();
  private final Rectangle2D.Double scratchcoords = new Rectangle2D.Double();
  private final Rectangle scratchpixels = new Rectangle();

  @Override
  public void draw(ViewI view) {
    String label = null;
    int axis_loc;
    TwoDimTransform cumulative;
    int axis_length;

    // Packers do not seem to be calling setCoord method.
    // So we need to do this in case a packer has moved the axis.
    if (null == lastCoordBox || !this.coordbox.equals(lastCoordBox)) {
      placeCenter(view);
    }

    // We don't need to do this if the axis is never moved
    // as it was when it was invisible to packers
    // by dint of having no intersects or hit methods.

    view.transformToPixels(coordbox, pixelbox);

    Rectangle2D.Double scenebox = view.getScene().getCoordBox();
    double scene_start, scene_end;
    scene_start = scenebox.x;
    scene_end = scenebox.x + scenebox.width;
    scratchcoords.y = centerLine;
    scratchcoords.height = 0;
    scratchcoords.x = coordbox.x;
    scratchcoords.width = coordbox.width;
    view.transformToPixels(scratchcoords, scratchpixels);
    cumulative = view.getTransform();

    Rectangle clipbox = view.getPixelBox();
    Graphics2D g = view.getGraphics();
    Font savefont = g.getFont();
    Stroke savestroke = g.getStroke();
    if (savefont != labelFont) {
      g.setFont(labelFont);
    }

    cumulative.transform(unitrect, scratchcoords);
    double pixels_per_unit = scratchcoords.width;

    // if make it this far but scale is weird, return without drawing
    if (pixels_per_unit == 0 || Double.isNaN(pixels_per_unit) ||
      Double.isInfinite(pixels_per_unit)) {
      return;
    }

    int axis_start;   // start to draw axis at (in canvas coordinates)
    int axis_end;     // end to draw axis to (in canvas coordinates)

    double units_per_pixel = 1 / pixels_per_unit;

    int clip_start, clip_end;
    axis_loc = scratchpixels.y;
    axis_start = pixelbox.x;
    axis_end = pixelbox.x + pixelbox.width;
    clip_start = clipbox.x;
    clip_end = clipbox.x + clipbox.width;

    if (axis_start > clip_start) {
      axis_start = clip_start;
    }

    if (axis_end < clip_end) {
      axis_end = clip_end;
    }

    axis_length = axis_end - axis_start + 1;

    g.setColor(getForegroundColor());
    g.setStroke(theStroke);

    // Draw the base line.

    int center_line_start = axis_loc - centerLineThickness / 2;

    g.drawLine(axis_start, center_line_start, axis_start + axis_length, center_line_start);
//      g.fillRect(axis_start, center_line_start, axis_length, centerLineThickness);
    // Drawing selected major axis ticks and labels in red if selected
    if (selectedRegions != null) {
      g.setColor(getBackgroundColor());
      for (int i = 0; i < selectedRegions.size(); i++) {
        int[] select_range = (int[]) selectedRegions.get(i);
        select_coord.x = select_range[0];
        select_coord.width = select_range[1] - select_range[0];
        view.transformToPixels(select_coord, select_pix);
        g.drawLine(select_pix.x, center_line_start, select_pix.width, center_line_start);
      //g.fillRect ( select_pix.x, center_line_start, select_pix.width, centerLineThickness);
      }
      g.setColor(getForegroundColor());
    }

    // space between tickmarks (in map coordinates)
    double tick_increment = tickIncrement(units_per_pixel, pixels_per_unit);

    // Calculate map_loc and max_map.

    double map_loc;
    double max_map;    // max tickmark to draw (in map coordinates)

    if (pixelbox.x < clipbox.x) {
      map_loc = (((int) (view.transformToCoords(clipbox, scratchcoords).x /
        tick_increment)) * tick_increment);
    } else {
      map_loc = view.transformToCoords(pixelbox, scratchcoords).x;
    }

    if (pixelbox.x + pixelbox.width > clipbox.x + clipbox.width) {
      view.transformToCoords(clipbox, scratchcoords);
      max_map = scratchcoords.x + scratchcoords.width;
    } else {
      view.transformToCoords(pixelbox, scratchcoords);
      max_map = scratchcoords.x + scratchcoords.width;
    }


    double subtick_increment = tick_increment / 10;
    double subtick_loc;
    // need to do tick_loc for those maps that don't start
    // at convenient tick_increments
    double tick_loc = tick_increment * Math.ceil(map_loc / tick_increment);


    // making sure first tick_loc is offscreen to ensure that all visible
    // subticks between it and first visible tick_loc get drawn
    // fixes missing subtick problem -- GAH 12/14/97

    tick_loc -= tick_increment;
    subtick_loc = tick_loc;
    double tick_scaled_loc;
    double tick_scaled_increment;
    scratchcoords.x = tick_loc;
    scratchcoords.width = tick_increment;
    cumulative.transform(scratchcoords, scratchcoords);
    tick_scaled_loc = scratchcoords.x;
    tick_scaled_increment = scratchcoords.width;

    // Draw the major tick marks including labels.

    int canvas_loc;   // location in canvas coordinates

    int string_draw_count = 0;
    //int init_tick_loc = (int)tick_loc;

    for (; tick_loc <= max_map; tick_loc += tick_increment, tick_scaled_loc += tick_scaled_increment) {
      canvas_loc = (int) tick_scaled_loc;

      // Don't draw things which are off the screen
      //        if (canvas_loc < clipbox.x || canvas_loc > clipbox.x+clipbox.width) continue;
        /*
      if (canvas_loc < clipbox.x || canvas_loc > clipbox.x+clipbox.width) {
      if (canvas_loc < clipbox.x) { less_count++; }
      else { greater_count++; }
      continue;
      }
       */

      if (selectedRegions != null) {
        g.setColor(getForegroundColor());
        for (int j = 0; j < selectedRegions.size(); j++) {
          int[] select_range = (int[]) selectedRegions.get(j);
          select_coord.x = select_range[0];
          select_coord.width = select_range[1] - select_range[0];
          view.transformToPixels(select_coord, select_pix);
          if (canvas_loc > select_pix.x && canvas_loc < (select_pix.x + select_pix.width)) {
            g.setColor(getBackgroundColor());
          }
        }
      }
      if (labelFormat != NO_LABELS) {
        label = stringRepresentation(tick_loc, tick_increment);
      }
      // putting in check to make sure don't extend past scene bounds when
      // view is "bigger" than scene
      if (tick_loc >= scene_start && tick_loc <= scene_end) {
        // DRAW the MAJOR tick marks
        if (labelFormat != NO_LABELS) {
          g.drawString(label, canvas_loc, center_line_start - labelShift);
          string_draw_count++;
        }
        g.drawLine(canvas_loc, center_line_start, canvas_loc, center_line_start + 4);
//            g.fillRect(canvas_loc, center_line_start-2,
//                       2, centerLineThickness+4);
      }
    }
    //      System.out.println("initial loc: " + init_tick_loc + ", less_count: "+ less_count + ", greater_count: " + greater_count +
    //                         "string_draw_count: " + string_draw_count);

    //Draw the minor tick marks.

    double subtick_scaled_loc, subtick_scaled_increment;
    scratchcoords.x = subtick_loc;
    scratchcoords.width = subtick_increment;
    // what is this doing??? hopefully just vestigial...
    // should try getting rid of it soon -- GAH 12-6-97
    cumulative.transform(scratchcoords, scratchcoords);
    subtick_scaled_loc = scratchcoords.x;
    subtick_scaled_increment = scratchcoords.width;

    for (; subtick_loc <= max_map; subtick_loc += subtick_increment) {
      //  canvas_loc = (int)subtick_scaled_loc;
      canvas_loc = (int) (subtick_scaled_loc + 0.5f);

      if (selectedRegions != null) {
        g.setColor(getForegroundColor());
        for (int j = 0; j < selectedRegions.size(); j++) {
          int[] select_range = (int[]) selectedRegions.get(j);
          select_coord.x = select_range[0];
          select_coord.width = select_range[1] - select_range[0];
          view.transformToPixels(select_coord, select_pix);
          if (canvas_loc > select_pix.x && canvas_loc < (select_pix.x + select_pix.width)) {
            g.setColor(getBackgroundColor());
          }
        }
      }
      // putting in check to make sure don't extend past scene bounds when
      //   view is "bigger" than scene
      if (subtick_loc >= scene_start && subtick_loc <= scene_end) {
        // this should put a tick subtick_size pixels tall tick above
        // the line, nothing below it
        // Draw the minor tickmarks
        g.drawLine(canvas_loc, center_line_start - subtick_size,
          canvas_loc, center_line_start);
      }
      subtick_scaled_loc += subtick_scaled_increment;
    }

    if (savefont != labelFont) {
      g.setFont(savefont);
    }
    g.setStroke(savestroke);

    super.draw(view);
  } // end of Draw method.

  // This DecimalFormat is used with the COMMA format.
  // It simply instructs java to insert commas between every three characters.
  DecimalFormat comma_format = new DecimalFormat("#,###.###");

  /**
   * Represents a number as a string.
   * Output depends on the format set in {@link #setLabelFormat(int)}.
   * <p>
   * Gregg added this to deal with an annoying tendency
   * of the <code>String.valueOf()</code> method in some JVM's
   * to add extraneous precision.
   * For example,
   * <code>String.valueOf(1f)</code> returns "1.0".
   * Not the desired "1".
   *
   * @param theNumber to convert
   * @return a String representing the number.
   */
  private String stringRepresentation(double theNumber, double theIncrement) {
    double double_label = theNumber;
    int int_label;
    // This fix should be faster than checking the string
    if (theIncrement < 2) {
      // temp fix for Java doubleing-point to string conversion problems,
      // needs to be made more general at some point
      int_label = (int) (double_label * 1000 + 0.5);
      double_label = ((double) int_label) / 1000;
      return String.valueOf(double_label);
    } else {
      int_label = (int) Math.round(double_label);
      if (ABBREV == this.labelFormat) {
        if (0 == int_label % 1000 && 0 != int_label) {
          int_label /= 1000;
          if (0 == int_label % 1000) {
            int_label /= 1000;
            if (0 == int_label % 1000) {
              int_label /= 1000;
              if (0 == int_label % 1000) {
                return comma_format.format(int_label) + "T";
              }
              return comma_format.format(int_label) + "G";
            }
            return comma_format.format(int_label) + "M";
          }
          return comma_format.format(int_label) + "k";
        }
        return comma_format.format(int_label);
      } else if (COMMA == this.labelFormat) {
        return comma_format.format(int_label);
      } else if (this.labelFormat == FULL) {
        String str = Integer.toString(int_label);
        if (str.endsWith("000")) {
          str = str.substring(0, str.length() - 3) + "kb";
        }
        return str;
      }
      return String.valueOf(int_label);
    }
  }

  private final double tickIncrement(double theUnitsPerPixel, double thePixelsPerUnit) {
    double result = 1;
    double increment = 1;
    double remainder;
    if (theUnitsPerPixel < 1) {
      remainder = thePixelsPerUnit;
      while (remainder >= 10) {
        remainder /= 10;
        increment *= 10;
      }
      if (remainder >= 2) {
        remainder /= 2;
        increment *= 2;
        if (remainder >= 2.5) {
          remainder /= 2.5;
          increment *= 2.5;
        }
      }
      result = (100 / increment);
    } else {
      remainder = theUnitsPerPixel;

      // The COMMA format requires 25% more space to accomodate "," characters
      // The ABBREV format is hard to predict, so give it extra space as well
      if (labelFormat != FULL) {
        remainder *= 1.25;
      }

      while (remainder >= 10) {
        remainder /= 10;
        increment *= 10;
      }
      if (remainder >= 2.5) {
        remainder /= 2.5;
        increment *= 2.5;
        if (remainder >= 2) {
          remainder /= 2;
          increment *= 2;
        }
      }
      result = (increment * 200);
    }

    return result;
  }

  /** If false, then {@link #hit(Rectangle, ViewI)} and
   *  {@link #hit(Rectangle2D.Double, ViewI)} will always return false.
   */
  public void setHitable(boolean h) {
    this.hitable = h;
  }

  @Override
  public boolean isHitable() {
    return hitable;
  }

  @Override
  public boolean hit(Rectangle pixel_hitbox, ViewI view) {
    return (isHitable() && pixel_hitbox.intersects(pixelbox));
  }

  @Override
  public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view) {
    return (isHitable() && coord_hitbox.intersects(coordbox));
  }
}
