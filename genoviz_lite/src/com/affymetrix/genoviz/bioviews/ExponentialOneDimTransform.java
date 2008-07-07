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

package com.affymetrix.genoviz.bioviews;

/**
 * A transform used internally by some NeoWidgets to handle zooming, should
 *    not be used directly.
 */
public class ExponentialOneDimTransform implements OneDimTransform {
  private final double xmax, xmin, logMaxIn, logMinIn, ratio;
  private final double maxOut, minOut;

  /**
   * Creates an exponential transformation mapping the
   * range minIn to maxIn to the range minOut to maxOut.
   * 
   * @param minIn  bottom of the input range
   * @param maxIn  top of the input range
   * @param minOut  bottom of the output range
   * @param maxOut  top of the output range
   */
  public ExponentialOneDimTransform(double minIn, double maxIn, double minOut, double maxOut) {
    this.xmin = minIn;
    this.xmax = maxIn;
    this.minOut = minOut;
    this.maxOut = maxOut;
    logMaxIn = Math.log(maxIn);
    logMinIn = Math.log(minIn);
    ratio = (logMaxIn-logMinIn)/(maxOut-minOut);
  }

  @Override
  public double transform(double in) {
    double out = Math.exp(in*ratio + logMinIn);
    /*
     *  Fix for zooming -- for cases where y _should_ be 7, but ends up
     *  being 6.9999998 or thereabouts because of errors in Math.exp()
     */
    if ( Math.abs(out) > .1) {
      double outround = Math.round(out);
      if (Math.abs(out-outround) < 0.0001) {
        out = outround;
      }
    }
    return out;
  }

  /** @param dim ignored */
  @Override
  public double inverseTransform(double in) {
    return (Math.log(in)-logMinIn) / ratio;
  }

  /**
   * Creates a shallow copy.
   * Since the only instance variables are doubles,
   * it is also trivially a deep copy.
   */
  @Override
  public ExponentialOneDimTransform clone() throws CloneNotSupportedException {
    return (ExponentialOneDimTransform) super.clone();
  }
}
