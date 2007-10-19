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

package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.igb.Application;
import java.io.*;
import java.util.zip.*;

public abstract class Streamer {
  
  /** A list of all the compression-type file endings that this
   *  object knows how to decompress.
   *  This list is all lower-case, but should be treated as case-insensitive.
   */
  public static final String[] compression_endings = 
    {".z", ".gzip", ".gz", ".zip"};


  /** Returns the file name with all {@link #compression_endings} stripped-off. */
  public static String stripEndings(String name) {
    for (int i=0; i<compression_endings.length; i++) {
      String ending = compression_endings[i].toLowerCase();
      if (name.toLowerCase().endsWith(ending)) {
        String stripped_name = name.substring(0, name.lastIndexOf('.'));
        return stripEndings(stripped_name);
      }
    }
    return name;
  }
    
  /** Returns a BufferedInputStream, possibly wrapped by a
   *  GZIPInputStream, or ZipInputStream,
   *  as appropriate based on the name of the given file.
   *  @param f a file
   *  @param sb a StringBuffer used to pass back the name of the file
   *            with the compression endings (like ".zip") removed,
   *            and converted to lower case.
   */
  public static InputStream getInputStream(File f, StringBuffer sb) throws
    FileNotFoundException, IOException {

    String infile_name = f.getName();
    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
    InputStream isr = unzipStream(bis, infile_name, sb);
    return isr;
  }

  /**
   *  Takes a named input stream and returns another one which is
   *  an instance of GZIPInputStream or ZipInputStream if the given name
   *  ends with one of the {@link #compression_endings} (case insensitive).
   *  (If the stream name does not have one of those endings, the original
   *  InputStream is simply returned unchanged.)  
   *  The name with the compression ending stripped off (and converted to lower case) is
   *  returned in the value of stripped_name.
   */
  public static InputStream unzipStream(InputStream istr, String stream_name,
    StringBuffer stripped_name)
  throws IOException  {
    String lc_stream_name = stream_name.toLowerCase();
    if (lc_stream_name.endsWith(".gz") || lc_stream_name.endsWith(".gzip") ||
	lc_stream_name.endsWith(".z"))  {
      //System.out.println("uncompressing via gzip stream");
      GZIPInputStream gzstr = new GZIPInputStream(istr);
      //      String new_name = stream_name.substring(0, stream_name.lastIndexOf('.'));
      String new_name = stream_name.substring(0, stream_name.lastIndexOf('.'));
      //System.out.println("stripped off gzip suffix, name = " + new_name);
      return unzipStream(gzstr, new_name, stripped_name);
    }
    else if (stream_name.endsWith(".zip")) {
      //System.out.println("uncompressing via zip stream");
      ZipInputStream zstr = new ZipInputStream(istr);
      zstr.getNextEntry();
      String new_name = stream_name.substring(0, stream_name.lastIndexOf('.'));
      //System.out.println("stripped off zip suffix, name = " + new_name);
      return unzipStream(zstr, new_name, stripped_name);
    }
    stripped_name.append(stream_name);
    return istr;
  }
  
  public static void close(Closeable str) {
    try {
      str.close();
    } catch (Exception e) {
      SingletonGenometryModel.logDebug("Failed to close an input stream");
    }
  }
}

