/**
*   Copyright (c) 2007 Affymetrix, Inc.
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
package com.affymetrix.genometryImpl.style;


/**
 *  Encapsulates information needed to restore the visual appearance of
 *    a graph stored at a URL.
 */
public interface GraphStateI {

  //TODO: Make this an enum
  public static int THRESHOLD_DIRECTION_GREATER = 1;
  public static int THRESHOLD_DIRECTION_BETWEEN = 0;
  public static int THRESHOLD_DIRECTION_LESS = -1;
  
  //TODO: Make this an enum
  public static final int LINE_GRAPH = 1;
  public static final int BAR_GRAPH = 2;
  public static final int DOT_GRAPH = 3;
  public static final int MINMAXAVG = 4;
  public static final int STAIRSTEP_GRAPH = 5;
  
  /** @deprecate Use EXT_HEAT_MAP for most cases. */
  public static final int HEAT_MAP = 7;

  public static final int BIG_DOT_GRAPH = 8;

  /** A heatmap that draws the color for the Minimum value at each pixel. */
  public static final int MIN_HEAT_MAP = 9;
  /** A heatmap that draws the color for the Maximum value at each pixel. */
  public static final int MAX_HEAT_MAP = 10;
  /** A heatmap that draws the color for the Average value at each pixel. */
  public static final int AVG_HEAT_MAP = 11;

  /** A heatmap that draws the color for the most extreme value at each pixel; 
   *  i.e. the value the most different from the midpoint of the range.
   */
  public static final int EXT_HEAT_MAP = 12;
  
  
  /** Copy all the properties, except ID and label, of the given state into this state. */
  public void copyProperties(GraphStateI ostate);

  public String getUrl();
  public int getGraphStyle();
  public HeatMap getHeatMap();

  public float getVisibleMinY();
  public float getVisibleMaxY();

  public boolean getFloatGraph();
  public boolean getShowThreshold();
  public boolean getShowAxis();

  public boolean getShowHandle();
  public boolean getShowGraph();
  public boolean getShowBounds();
  public boolean getShowLabel();
  public boolean getShowLabelOnRight();
  public boolean getShowZeroLine();

  public float getMinScoreThreshold();
  public float getMaxScoreThreshold();

  public int getMaxGapThreshold();
  public int getMinRunThreshold();
  public double getThreshStartShift();
  public double getThreshEndShift();
  
  /**
   *  Returns either {@link #THRESHOLD_DIRECTION_GREATER} or
   *  {@link #THRESHOLD_DIRECTION_LESS} or {@link #THRESHOLD_DIRECTION_BETWEEN}.
   */
  public int getThresholdDirection();

  public void setUrl(String url);
  public void setFloatGraph(boolean b);
  public void setGraphStyle(int style);
  public void setHeatMap(HeatMap hmap);
  public void setVisibleMinY(float vminy);
  public void setVisibleMaxY(float vmaxy);
  public void setShowThreshold(boolean b);
  public void setShowAxis(boolean b);
  public void setShowHandle(boolean b);
  public void setShowGraph(boolean b);
  public void setShowBounds(boolean b);
  public void setShowLabel(boolean b);
  public void setShowLabelOnRight(boolean b);
  public void setShowZeroLine(boolean b);
  public void setMinScoreThreshold(float thresh);
  public void setMaxScoreThreshold(float thresh);
  public void setMaxGapThreshold(int thresh);
  public void setMinRunThreshold(int thresh);
  public void setThreshStartShift(double d);
  public void setThreshEndShift(double d);
  
  /**
   *  Set to either {@link #THRESHOLD_DIRECTION_GREATER} or
   *  {@link #THRESHOLD_DIRECTION_LESS} or {@link #THRESHOLD_DIRECTION_BETWEEN}.
   */
  public void setThresholdDirection(int d);

  public IAnnotStyle getTierStyle();
  
  public IAnnotStyle getComboStyle();

  public void setComboStyle(IAnnotStyle s);
}
