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

package com.affymetrix.genoviz.util;

import java.util.*;

/** 
 * Utility methods for some simple statistics.
 */
public class Stat {

  /**
   * @param values a vector of Doubles to average.
   * @return the mean
   */
  public static double mean(Vector values) {
    double total = 0;

    for (int i = 0; i < values.size(); i++) {
      total += ((Double) values.elementAt (i)).doubleValue();
    }
    return (total / values.size());
  }

  /**
   * @param values a vector of Doubles to average.
   * @return the mean deviation
   */
  public static double mean_deviation (Vector values) {
    double value;
    double average = mean (values);
    double total = 0;

    for (int i = 0; i < values.size(); i++) {
      value = ((Double) values.elementAt (i)).doubleValue();
      total += Math.abs (value - average);
    }
    return (total / values.size());
  }

  /**
   * @param values a vector of Doubles to average.
   * @return the variance
   */
  public static double variance (Vector values) {
    double deviation;
    double value;
    double average = mean (values);
    double total = 0;

    for (int i = 0; i < values.size(); i++) {
      value = ((Double) values.elementAt (i)).doubleValue();
      deviation = value - average;
      total += deviation * deviation;
    }
    return (total / values.size());
  }

  /**
   * @param values a vector of Doubles to average.
   * @return the standard deviation
   */
  public static double standard_deviation (Vector values) {
    double var = variance (values);
    return (Math.sqrt (var));
  }

}
