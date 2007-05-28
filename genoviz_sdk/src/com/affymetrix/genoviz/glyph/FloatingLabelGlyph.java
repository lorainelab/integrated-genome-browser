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

package com.affymetrix.genoviz.glyph;

import java.awt.*;
import java.util.*;
import com.affymetrix.genoviz.bioviews.*;

/**
 * A label that doubles to remain entirely in view as long as any part of its
 * bounding box is in view.
 *
 * <p> FloatingLabelGlyph is still experimental.
 */
public class FloatingLabelGlyph extends Glyph
{
  // these should all be variables in base glyphs
    String label;
    public boolean ABOVE, BELOW;
    public boolean LEFT, RIGHT;

    public FloatingLabelGlyph (String label) {
        this.label = label;
        ABOVE = false;
        BELOW = false;
        LEFT = false;
        RIGHT = false;
    }

    public FloatingLabelGlyph () {
    }

    public void setLabel (String label) {
        this.label = label;
    }

    public String getLabel () {
        return label;
    }

  public void draw(ViewI view) {
    Graphics g = view.getGraphics();
    FontMetrics font = g.getFontMetrics();
    int text_width = font.stringWidth(label);
    int text_height = font.getHeight();
    int blank_width = font.charWidth ('z');
    double start, end;
    double xpixel_center;
    boolean in_view = false;

    Rectangle2D view_box = view.getCoordBox();
    Rectangle2D parent_box = parent.getCoordBox();
    start = ((parent_box.x < view_box.x) ? view_box.x : parent_box.x);
    end = (((parent_box.x+parent_box.width) > (view_box.x+view_box.width)) ? (view_box.x+view_box.width) : (parent_box.x+parent_box.width));
    Rectangle2D avail_box = new Rectangle2D (start, parent_box.y,
                                             (end - start),
                                             parent_box.height);
    view.transformToPixels(avail_box, pixelbox);
    if (text_width <= pixelbox.width) {
        Rectangle parent_pix = parent.getPixelBox(view);

        if (LEFT) {
            if ((parent_pix.x - text_width - blank_width) >= pixelbox.x) {
                in_view = true;
                pixelbox.x = (parent_pix.x - text_width - blank_width);
            }
        }
        else if (RIGHT) {
            if ((parent_pix.x + parent_pix.width + text_width + blank_width)
                    <= pixelbox.x + pixelbox.width) {
                in_view = true;
                pixelbox.x = (parent_pix.x + parent_pix.width + blank_width);
            }
        }
        else {
            if (text_width <= pixelbox.width) {
                in_view = true;
                xpixel_center = pixelbox.x + (pixelbox.width / 2);
                pixelbox.x = (int) (xpixel_center - (text_width / 2));
            }
        }

        if (ABOVE) {
            in_view &= true;
            pixelbox.y = pixelbox.y;
        }
        else if (BELOW) {
            in_view &= true;
            pixelbox.y += 2 * pixelbox.height;
        }
        else {
            in_view &= true;
            pixelbox.y += pixelbox.height;
        }
        pixelbox.width = text_width;
        pixelbox.height = text_height;
        g.setColor(getForegroundColor());
        g.drawString(label, pixelbox.x, pixelbox.y);
    }
    super.draw(view);
  }

  public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
    calcPixels(view);
    return  pixel_hitbox.intersects(pixelbox);
  }

  public boolean hit(Rectangle2D coord_hitbox, ViewI view)  {
    return coord_hitbox.intersects(coordbox);
  }

  /**
   * @deprecated use {@link #setForegroundColor}.
   */
  @Deprecated
  public void setColor( Color c ) {
    setForegroundColor( c );
  }

  /**
   * @deprecated use {@link #getForegroundColor}.
   */
  @Deprecated
  public Color getColor() {
    return getForegroundColor();
  }

}
