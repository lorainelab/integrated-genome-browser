/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

import java.io.PrintWriter;
import java.util.*;
import javax.swing.table.TableModel;
import javax.swing.filechooser.FileFilter;

/**
 * Subclasses can filter table data for output to files.
 * This is also suitable for filtering file names for a JFileChooser.
 * @author  Eric Blossom
 */
public abstract class TableFilter extends FileFilter {

  protected Set validSuffixes = new HashSet();
  protected String description = "";

  /**
   * Creates an empty filter.
   * Subclasses will need to add valid suffixes
   * and a description.
   * This can be done in a static initializer
   * as there are no setter methods here.
   */
  public TableFilter() {
  }

  /**
   * Creates a new instance of TableFilter.
   * @param theSuffixes must all be strings with no dots.
   *        BUG: This is not enforced.
   */
  public TableFilter( Collection theSuffixes, String theDescription ) {
    this.validSuffixes.addAll( theSuffixes );
    if ( null != theDescription ) {
      this.description = theDescription;
    }
  }

  public boolean accept( java.io.File f ) {
    if ( null == f ) return false;
    if ( f.isDirectory() ) return true;
    String lowerName = f.getName().toLowerCase();
    int lastDotAt = lowerName.lastIndexOf( "." );
    String suffix = "";
    if ( -1 < lastDotAt ) {
      suffix = lowerName.substring( lastDotAt+1 );
    }
    return this.validSuffixes.contains( suffix );
  }

  public String getDescription() {
    return this.description;
  }

  /**
   * Convert the table model to a stream of characters and write it.
   * @param theTable to convert
   * @param theDestination to whence it goes.
   */
  public abstract void write( TableModel theTable, PrintWriter theDestination );

}
