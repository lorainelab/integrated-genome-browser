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

package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

/**
 *  A JFileChooser that specializes in saving instances of GraphSym as WIG files.
 *  This can be used to load graphs also.
 */
public class GraphWigFileChooser extends UniFileChooser {

  static public final UniFileFilter wig_filter = new UniFileFilter(new String[] {"wig"}, "Wiggle Graph");
  
  public GraphWigFileChooser() {
    super();
    reinitialize();
  }
  
  public void reinitialize() {
    FileFilter[] filters = getChoosableFileFilters();
    for (int i=0; i<filters.length; i++) {
      removeChoosableFileFilter(filters[i]);
    }

    addChoosableFileFilter(wig_filter);
    
    setFileFilter(getChoosableFileFilters()[0]);
    setMultiSelectionEnabled(false);
    setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    rescanCurrentDirectory();
    setSelectedFile(null);  
  }
}
