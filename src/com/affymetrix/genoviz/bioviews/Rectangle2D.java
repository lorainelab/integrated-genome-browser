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

/**
 * This is similar to, but <em>not</em> the same as the {java.awt.geom.Rectangle2D.Double},
 * which was not available at the time this class was written.
 * Some methods may change
 * due to differences when dealing with (almost)real numbers instead of integers.
 * See, for example, intersects(Rectangle2D)
 */
public class Rectangle2D  {

  /**
   * The x coordinate of the rectangle.
   */
  public double x;

  /**
   * The y coordinate of the rectangle.
   */
  public double y;

  /**
   * The width of the rectangle.
   */
  public double width;

  /**
   * The height of the rectangle.
   */
  public double height;

  /**
   * Constructs a new rectangle.
   */
  public Rectangle2D() {
    super();
  }

  /**
   * Constructs and initializes a rectangle with the specified parameters.
   * @param x the x coordinate
   * @param y the y coordinate
   * @param width the width of the rectangle
   * @param height the height of the rectangle
   */
  public Rectangle2D(double x, double y, double width, double height) {
    /*
    if ( Double.isNaN( x ) ) {
      throw new IllegalArgumentException( "X must be a number." );
    }
    if ( Double.isNaN( y ) ) {
      throw new IllegalArgumentException( "Y must be a number." );
    }
    if ( Double.isNaN( width ) ) {
      throw new IllegalArgumentException( "Width must be a number." );
    }
    if ( Double.isNaN( height ) ) {
      throw new IllegalArgumentException( "Height must be a number." );
    }
    */
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  /**
   * Reshapes the rectangle.
   */
  public void reshape(double x, double y, double width, double height) {
    /*
    if ( Double.isNaN( x ) ) {
      throw new IllegalArgumentException( "X must be a number." );
    }
    if ( Double.isNaN( y ) ) {
      throw new IllegalArgumentException( "Y must be a number." );
    }
    if ( Double.isNaN( width ) ) {
      throw new IllegalArgumentException( "Width must be a number." );
    }
    if ( Double.isNaN( height ) ) {
      throw new IllegalArgumentException( "Height must be a number." );
    }
    */
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  public void reshape(Rectangle2D r) {
    x = r.x;
    y = r.y;
    width = r.width;
    height = r.height;
  }

  /**
   * Moves the rectangle.
   */
  public void move(double x, double y) {
    /*
    if ( Double.isNaN( x ) ) {
      throw new IllegalArgumentException( "X must be a number." );
    }
    if ( Double.isNaN( y ) ) {
      throw new IllegalArgumentException( "Y must be a number." );
    }
    */
    this.x = x;
    this.y = y;
  }

  /**
   * Translates the rectangle.
   */
  public void translate(double x, double y) {
    /*
    if ( Double.isNaN( x ) ) {
      throw new IllegalArgumentException( "X must be a number." );
    }
    if ( Double.isNaN( y ) ) {
      throw new IllegalArgumentException( "Y must be a number." );
    }
    */
    this.x += x;
    this.y += y;
  }

  /**
   * Resizes the rectangle.
   */
  public void resize(double width, double height) {
    /*
    if ( Double.isNaN( width ) ) {
      throw new IllegalArgumentException( "Width must be a number." );
    }
    if ( Double.isNaN( height ) ) {
      throw new IllegalArgumentException( "Height must be a number." );
    }
    */
    this.width = width;
    this.height = height;
  }

  /**
   * Checks if the specified point lies inside a rectangle.
   * @param x the x coordinate
   * @param y the y coordinate
   */
  public boolean inside(double x, double y) {
    return (x >= this.x) && ((x - this.x) < this.width) && (y >= this.y) && ((y-this.y) < this.height);
  }

  /**
   * Checks if two rectangles intersect.
   * Just like Rectangle, we consider Rectangle2Ds that only
   * share an edge as <em>not</em> intersecting.
   */
  public boolean intersects(Rectangle2D r) {

    // Just like Rectangle, sharing an edge is not considered intersection
    return !((r.x + r.width <= x) ||
        (r.y + r.height <= y) ||
        (r.x >= x + width) ||
        (r.y >= y + height));
  }

  /**
   * Computes the intersection of two rectangles.
   */
  public Rectangle2D intersection(Rectangle2D r) {
    double x1 = Math.max(x, r.x);
    double x2 = Math.min(x + width, r.x + r.width);
    double y1 = Math.max(y, r.y);
    double y2 = Math.min(y + height, r.y + r.height);
    return new Rectangle2D(x1, y1, x2 - x1, y2 - y1);
  }

  /**
   * Computes the union of two rectangles.
   */
  public Rectangle2D union(Rectangle2D r) {
    double x1 = Math.min(x, r.x);
    double x2 = Math.max(x + width, r.x + r.width);
    double y1 = Math.min(y, r.y);
    double y2 = Math.max(y + height, r.y + r.height);
    return new Rectangle2D(x1, y1, (x2 - x1), (y2 - y1));
  }

  /**
   * Adds a rectangle to a rectangle.
   * This results in the union of the two rectangles.
   */
  public void add(Rectangle2D r) {
    double x1 = Math.min(x, r.x);
    double x2 = Math.max(x + width, r.x + r.width);
    double y1 = Math.min(y, r.y);
    double y2 = Math.max(y + height, r.y + r.height);
    x = x1;
    y = y1;
    width = x2 - x1;
    height = y2 - y1;
  }

  /**
   * Adds a point to a rectangle.
   * This results in the smallest rectangle
   * that contains both the rectangle and the point.
   */
  public void add(double newx, double newy) {
    /*
    if ( Double.isNaN( newx ) ) {
      throw new IllegalArgumentException( "Newx must be a number." );
    }
    if ( Double.isNaN( newy ) ) {
      throw new IllegalArgumentException( "Newy must be a number." );
    }
    */
    double x1 = Math.min(x, newx);
    double x2 = Math.max(x + width, newx);
    double y1 = Math.min(y, newy);
    double y2 = Math.max(y + height, newy);
    x = x1;
    y = y1;
    width = x2 - x1;
    height = y2 - y1;
  }

  /**
   * Grows the rectangle horizontally and vertically.
   * e.g. <code>grow(1, 1)</code> causes growth by one in all four directions.
   * @param h amount to grow both to the left and to the right.
   * @param v amount to grow both up and down.
   */
  public void grow(double h, double v) {
    /*
    if ( Double.isNaN( h ) ) {
      throw new IllegalArgumentException( "H must be a number." );
    }
    if ( Double.isNaN( v ) ) {
      throw new IllegalArgumentException( "V must be a number." );
    }
    */
    x -= h;
    y -= v;
    width += h * 2;
    height += v * 2;
  }

  /**
   * Determines whether the rectangle is empty.
   */
  public boolean isEmpty() {
    return (width <= 0) || (height <= 0);
  }

  /**
   * Checks whether two rectangles are equal.
   */
  public boolean equals(Object obj) {
    if (obj instanceof Rectangle2D) {
      Rectangle2D r = (Rectangle2D)obj;
      return (x == r.x) && (y == r.y) && (width == r.width) && (height == r.height);
    }
    return false;
  }

  /**
   *  Reshape this rectangle to same coords as r
   */
  public void copyRect(Rectangle2D r) {
    x = r.x;
    y = r.y;
    width = r.width;
    height = r.height;
  }

  /**
   * Returns the String representation of this Rectangle2D's values.
   */
  public String toString() {
    return "GenoViz Rect2D: xmin = " + x + ", xmax = " + (x+width) +
      ", ymin = " + y + ", ymax = " + (y+height);
  }

}
