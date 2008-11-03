/**
*   Copyright (c) 2008 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/
package com.affymetrix.genoviz.bioviews;

/** Identifies the two axes of a SceneI. */
//TODO: find and verify all uses, especially uses of ordinal()
public enum WidgetAxis {
  /** The primary axis.
   *  When a map is {@link com.affymetrix.genoviz.util.NeoConstants.Orientation#Horizontal},
   *  this corresponds to the x axis.
   */
  Range,

  /** The secondary axis. */
  Offset;
}

