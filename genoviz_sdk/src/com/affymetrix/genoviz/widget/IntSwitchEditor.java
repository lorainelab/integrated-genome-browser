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

import java.beans.PropertyEditorSupport;

/**
 * A base for various bean box property editors.
 * Classes can extend this
 * and initialize the <code>tags</code> and <code>values</code arrays
 * in the constructor.
 * The result will be a property editor
 * that associates a set of text strings
 * with their respective integer values.
 */
public abstract class IntSwitchEditor extends PropertyEditorSupport {

  protected String[] tags;
  protected int[] values;

  public String[] getTags() {
    return this.tags;
  }

  public String getAsText() {
    int v = ((Integer)getValue()).intValue();
    for (int i = 0; i < tags.length; i++) {
      if (values[i] == v) return tags[i];
    }
    return null;
  }

  public void setAsText(String theValue) {
    for (int i = 0; i < tags.length; i++) {
      if (tags[i].equals(theValue)) {
        setValue(new Integer(values[i]));
        return;
      }
    }
    throw new IllegalArgumentException
      ("No such tag as \"" + theValue + "\"");
  }

}
