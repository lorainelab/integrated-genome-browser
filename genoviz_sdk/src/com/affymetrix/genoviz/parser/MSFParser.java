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

package com.affymetrix.genoviz.parser;

import com.affymetrix.genoviz.datamodel.MultiSeqAlign;
import com.affymetrix.genoviz.datamodel.SequenceI;
import com.affymetrix.genoviz.datamodel.Sequence;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * handles GCG Multiple Sequence Format (MSF) data.
 *
 * @author Eric Blossom
 */
public class MSFParser implements ContentParser {

  /**
   * the data model.
   */
  MultiSeqAlign alignment = new MultiSeqAlign();

  /**
   * imports the data from a stream of bytes.
   * This is defined as returning an Object
   * to conform to the ContentParser interface.
   * However, it actually returns a {@link MultiSeqAlign}.
   * To use it as such
   * you will need to cast the returned Object back to a MultiSeqAlign.
   *
   * @param theInput from whence the data come.
   * @return a {@link MultiSeqAlign}.
   */
  public Object importContent( InputStream theInput ) throws IOException {
    StringBuffer out = new StringBuffer();
    BufferedReader in;
    String line;

    in = new BufferedReader( new InputStreamReader( theInput ) );
    try {
      // Find GCG information line.
      while ( null != ( line = in.readLine() )
        && !line.endsWith( ".." ) ) {
      }
      // Skip sequence name lines.
      while ( null != ( line = in.readLine() )
        && !line.startsWith( "//" ) ) {
      }
      while ( null != ( line = in.readLine() ) ) {
        appendLine( out, line );
      }
    } finally {
      in.close();
    }
    return this.alignment;
  }

  /**
   * not yet implemented.
   */
  public void exportContent( OutputStream out, Object o )
    throws IOException {
  }

  protected void appendLine( StringBuffer out, String theLine ) {

    String name = "";

    if (theLine.length() < 1) return;
    if (' ' == theLine.charAt(0)) return;
    if ('\t' == theLine.charAt(0)) return;

    StringBuffer sb = new StringBuffer();
    StringTokenizer st = new StringTokenizer(theLine);

    if (st.hasMoreTokens()) {
      name = st.nextToken();
    }
    while (st.hasMoreTokens()) {
      sb.append(st.nextToken());
    }

    if (0 < name.length()) {
      SequenceI s = this.alignment.getSequence(name);
      StringBuffer seq;
      if (null != s) {
        seq = new StringBuffer(s.getResidues());
      }
      else {
        seq = new StringBuffer();
      }
      seq.append(sb.toString().replace('.', MultiSeqAlign.GAP_CHAR));
      this.alignment.addSequence(name, seq.toString());
    }
  }

}
