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

package com.affymetrix.genoviz.bioviews;

import java.awt.*;
import java.lang.*;

/**
 *  A transform used internally by NeoSeq, should not be used directly.
 */
public class ConstrainLinearTrnsfm extends LinearTransform {

  public static final int FLOOR = 0;
  public static final int ROUND = 1;
  public static final int CEILING = 2;

  protected double constrain_value;
  protected int constrain_behavior;

  public ConstrainLinearTrnsfm() {
    constrain_value = 1;
    constrain_behavior = FLOOR;
    // Why FLOOR?   GAH
    //    constrain_behavior = ROUND;
  }

  public void setConstrainValue(double cv) {
    constrain_value = cv;
  }

  public double getConstrainValue() {
    return constrain_value;
  }

  public void setConstrainBehavior(int b) {
    constrain_behavior = b;
  }

  public int getConstrainBehavior() {
    return constrain_behavior;
  }

  public double transform(int orientation, double in) {
    double out = 0;
    if (orientation == X) {
      out = in * xscale;
    } else if (orientation == Y) {
      out = in * yscale;
    }

    if (constrain_behavior == FLOOR) {
      out = out - (out % constrain_value);
    } else if (constrain_behavior == ROUND) {
      double out2 = out - (out % constrain_value);
      if ( out - out2 > constrain_value / 2 ) {
        out = out2 + constrain_value;
      } else {
        out = out2;
      }
    } else if (constrain_behavior == CEILING) {
      double out2 = out - (out % constrain_value);
      if ( out - out2 > 0 ) {
        out = out2 + constrain_value;
      } else {
        out = out2;
      }
    }

    if (orientation == X) {
      out += xoffset;
    } else if (orientation == Y) {
      out += yoffset;
    }

    return out;
  }

  public double inverseTransform(int orientation, double in) {
    double out = 0;
    if (orientation == X) {
      out = (in - xoffset) / xscale;
    } else if (orientation == Y) {
      out = (in - yoffset) / yscale;
    }
    return out;
  }

  public void setScale(double x, double y) {
    xscale = x;
    yscale = y;
  }

  public void setTranslation(double x, double y) {
    xoffset = x;
    yoffset = y;
  }

  public Rectangle2D transform(Rectangle2D src, Rectangle2D dst) {
    dst.x = src.x * xscale + xoffset;
    dst.y = src.y * yscale + yoffset;
    dst.width = src.width * xscale;
    dst.height = src.height * yscale;
    return dst;
  }

  public Rectangle2D inverseTransform(Rectangle2D src, Rectangle2D dst) {
    dst.x = (src.x - xoffset) / xscale;
    dst.y = (src.y - yoffset) / yscale;
    dst.width = src.width / xscale;
    dst.height = src.height / yscale;
    return dst;
  }

  public Point2D transform(Point2D src, Point2D dst) {
    dst.x = src.x * xscale + xoffset;
    dst.y = src.y * yscale + yoffset;
    return dst;
  }

  public Point2D inverseTransform(Point2D src, Point2D dst) {
    dst.x = (src.x - xoffset) / xscale;
    dst.y = (src.y - yoffset) / yscale;
    return dst;
  }

  public boolean equals(TransformI Tx) {
    if (Tx instanceof ConstrainLinearTrnsfm) {
      ConstrainLinearTrnsfm clint = (ConstrainLinearTrnsfm)Tx;
      return (xscale == clint.getScaleX() &&
              yscale == clint.getScaleY() &&
              xoffset == clint.getOffsetX() &&
              yoffset == clint.getOffsetY() &&
              constrain_value == clint.getConstrainValue() &&
              constrain_behavior == clint.getConstrainBehavior() );
    }
    else {
      return false;
    }
  }

}
