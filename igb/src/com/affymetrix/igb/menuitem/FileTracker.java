package com.affymetrix.igb.menuitem;

import java.io.File;
import com.affymetrix.igb.util.UnibrowPrefsUtil;

/**
 *  Used to cache info on current directory.
 */
public final class FileTracker {
  private final String name;

  /** The singleton FileTracker used to remember the user's most recent data directory. */
  public final static FileTracker DATA_DIR_TRACKER 
    = new FileTracker(UnibrowPrefsUtil.DATA_DIRECTORY);

  /** The singleton FileTracker used to remember the user's most recent output directory. */
  public final static FileTracker OUTPUT_DIR_TRACKER
    = new FileTracker(UnibrowPrefsUtil.OUTPUT_DIRECTORY);
  
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
