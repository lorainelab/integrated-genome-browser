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

package com.affymetrix.genoviz.widget.neoseq;
import com.affymetrix.genoviz.glyph.*;

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.util.*;

import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.datamodel.Range;

public class WrapStripes extends WrapGlyph {

  protected Color[] stripe_colors  = { Color.white, Color.lightGray };

  protected Point backPixelPoint1 = new Point(0,0);
  protected Point backPixelPoint2 = new Point(0,0);

  protected Point2D backCoordPoint1 = new Point2D(0,0);
  protected Point2D backCoordPoint2 = new Point2D(0,0);


  public static final int NONE = 0;
  public static final int VERTICAL = 1;
  public static final int HORIZONTAL = 2;

  protected int orientation = VERTICAL;

  protected int y_offset_fudge = 3;

  public void draw(ViewI view) {

    Graphics g = view.getGraphics();
    Rectangle2D visible_box = ((View)view).calcCoordBox();

    if (residues_per_stripe > 0) {
      if (orientation == VERTICAL) {
        if (residues_per_stripe > 0) {
          int total_stripes = residues_per_line/residues_per_stripe;

          int stripe_num = 0;

          while (stripe_num < total_stripes)  {
            backCoordPoint1.x = (visible_box.x + stripe_num)*residues_per_stripe;
            backCoordPoint1.y = visible_box.y;
            backCoordPoint2.x = backCoordPoint1.x + residues_per_stripe;
            backCoordPoint2.y = visible_box.y + visible_box.height;

            backPixelPoint1 =
              view.transformToPixels(backCoordPoint1, backPixelPoint1);

            backPixelPoint2 =
              view.transformToPixels(backCoordPoint2, backPixelPoint2);

            int stripe_color_index = stripe_num % stripe_colors.length;
            if (stripe_colors[stripe_color_index] != null) {
              g.setColor(stripe_colors[stripe_color_index]);
              g.fillRect(backPixelPoint1.x,
                  backPixelPoint1.y,
                  backPixelPoint2.x - backPixelPoint1.x,
                  backPixelPoint2.y - backPixelPoint1.y);
            }
            stripe_num++;
          }
        }
      }
      else if ( orientation == HORIZONTAL ) {

        int stripe_start;
        int stripe_end;

        if ( residues_per_line < 1 ) {
          return; // Avoid infinite loop below.
        }
        for ( stripe_start = (int)visible_box.y;
            stripe_start < (int)(visible_box.y + visible_box.height);
            stripe_start += residues_per_line )
        {
          stripe_end = stripe_start +
            (residues_per_line * residues_per_stripe);

          int yoffset =
            stripe_start % (stripe_colors.length * residues_per_stripe);
          int stripe_num = stripe_start/(residues_per_line * residues_per_stripe);

          backCoordPoint1.x = 0;
          backCoordPoint1.y = stripe_start;
          backCoordPoint2.x = visible_box.width;
          backCoordPoint2.y = stripe_end;

          backPixelPoint1 =
            view.transformToPixels(backCoordPoint1, backPixelPoint1);

          backPixelPoint2 =
            view.transformToPixels(backCoordPoint2, backPixelPoint2);

          int stripe_color_index = stripe_num % stripe_colors.length;
          if (stripe_colors[stripe_color_index] != null) {
            g.setColor(stripe_colors[stripe_color_index]);
            g.fillRect(backPixelPoint1.x,
                backPixelPoint1.y + y_offset_fudge,
                backPixelPoint2.x - backPixelPoint1.x,
                backPixelPoint2.y - backPixelPoint1.y);

            stripe_num++;
          }
        }
      }
      else if (orientation == NONE) {
      }
    }
  }

  public void setStripeWidth(int i) {
    if (i >= 0) {
      residues_per_stripe = i;
    }
  }

  public int getStripeWidth() {
    return residues_per_stripe;
  }

  public void setStripeOrientation(int i) {
    orientation = i;
  }

  public int getStripeOrientation() {
    return orientation;
  }

  public void setStripeColors(Color[] colors) {
    this.stripe_colors = colors;
  }

  public Color[] getStripeColors() {
    return this.stripe_colors;
  }

}
