/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl;

public class SingletonGenometryModel extends GenometryModel {

  static SingletonGenometryModel smodel = new SingletonGenometryModel();

  /** Constructor is protected to allow for subclassing. */
  protected SingletonGenometryModel() {
    super();
  }
  
  public static SingletonGenometryModel getGenometryModel() {
    return smodel;
  }

}
