/**
 *   Copyright (c) 2006-2007 Affymetrix, Inc.
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

package com.affymetrix.igb.thresholding;

import java.util.ResourceBundle;

import com.affymetrix.genometryImpl.symmetry.GraphIntervalSym;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

/**
 *  A JFileChooser that specializes in saving instances of GraphSym.
 *  This can be used to load graphs also, but it is only set-up to recognize
 *  the few graph file types that can also be written out.
 */
public final class GraphSaverFileChooser extends UniFileChooser {
  private static final long serialVersionUID = 1L;
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("thresholding");

  static public final UniFileFilter wig_filter = new UniFileFilter(new String[] {"wig"}, BUNDLE.getString("wig"));
  static public final UniFileFilter egr_filter = new UniFileFilter(new String[] {"egr"}, BUNDLE.getString("egr"));
  static public final UniFileFilter gr_filter = new UniFileFilter(new String[] {"gr"}, BUNDLE.getString("gr"));
  static public final UniFileFilter sgr_filter = new UniFileFilter(new String[] {"sgr"}, BUNDLE.getString("sgr"));
  static public final UniFileFilter bgr_filter = new UniFileFilter(new String[] {"bgr"}, BUNDLE.getString("bgr"));
  
  public GraphSaverFileChooser(GraphSym sym) {
    super();
    reinitialize(sym);
  }
  
  public void reinitialize(GraphSym sym) {
    FileFilter[] filters = getChoosableFileFilters();
    for (int i=0; i<filters.length; i++) {
      removeChoosableFileFilter(filters[i]);
    }
    
   
    if (sym instanceof GraphIntervalSym) {
      addChoosableFileFilter(wig_filter);
      addChoosableFileFilter(egr_filter);
    }
    else {
      addChoosableFileFilter(bgr_filter);      
      addChoosableFileFilter(sgr_filter);
      addChoosableFileFilter(gr_filter);
    }

    setFileFilter(getChoosableFileFilters()[0]);
    setMultiSelectionEnabled(false);
    setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    rescanCurrentDirectory();
    setSelectedFile(null);  
  }
}
