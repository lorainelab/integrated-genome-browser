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

package com.affymetrix.genoviz.beans;

/**
 * Stub for java.beans.PropertyChangeSupport
 * so that NeoWidgetICustomizer will compile in 1.0.2
 */

public class PropertyChangeSupport{

  public PropertyChangeSupport(Object theBean) {
  }
  public void addPropertyChangeListener(PropertyChangeListener listener) {
  }
  public void removePropertyChangeListener(PropertyChangeListener listener) {
  }
  public void firePropertyChange(String propertyName,
    Object oldValue, Object newValue) {
  }
}
