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
Affymetrix' version of the java.beans.PropertyChangedListener interface.
For use with Java 1.0x.
*/
public interface PropertyChangeListener {

  public abstract void propertyChange(java.awt.Event theEvent);

}
