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

package com.affymetrix.igb.menuitem;

import java.io.File;
import com.affymetrix.igb.util.UnibrowPrefsUtil;

/**
 *  Used to cache info on current directory.
 */
public final class FileTracker {
  final String name;

  /** The singleton FileTracker used to remember the user's most recent data directory. */
  public final static FileTracker DATA_DIR_TRACKER 
    = new FileTracker(UnibrowPrefsUtil.DATA_DIRECTORY);

  /** The singleton FileTracker used to remember the user's most recent output directory. */
  public final static FileTracker OUTPUT_DIR_TRACKER
    = new FileTracker(UnibrowPrefsUtil.OUTPUT_DIRECTORY);
  
  /** A FileTracker used by the QueryFrame for loading/saving control graph
   *  tab separated files.
   */
  public final static FileTracker CONTROL_GRAPH_DIR_TRACKER 
    = new FileTracker(UnibrowPrefsUtil.CONTROL_GRAPH_DIRECTORY);
    
  private FileTracker(String name) {
    this.name = name;
  }

  public void setFile(File fl) {
    UnibrowPrefsUtil.saveFilename(name, fl);
  }

  public File getFile() {
    return UnibrowPrefsUtil.getFilename(name);
  }

}
