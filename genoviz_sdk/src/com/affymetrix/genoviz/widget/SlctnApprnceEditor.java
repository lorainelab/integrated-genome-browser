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

import com.affymetrix.genoviz.bioviews.SceneI;

/**
 * for editing selection appearance in NeoWidgets.
 */
public class SlctnApprnceEditor extends IntSwitchEditor {

  public SlctnApprnceEditor() {
    this.tags = new String[] { "none", "outline", "filled" };
    this.values = new int[] {
      SceneI.SELECT_NONE,
      SceneI.SELECT_OUTLINE,
      SceneI.SELECT_FILL
    };
  }

}
