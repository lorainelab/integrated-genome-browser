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

package com.affymetrix.genoviz.widget;

/**
 * for editing the selection event in NeoWidgets.
 */
public class SlctnEvntEditor extends IntSwitchEditor {

  public SlctnEvntEditor() {
    this.tags = new String[] { "none", "mouse down", "mouse up" };
    this.values = new int[] {
      NeoWidgetI.NO_SELECTION,
      NeoWidgetI.ON_MOUSE_DOWN,
      NeoWidgetI.ON_MOUSE_UP
    };
  }

}
