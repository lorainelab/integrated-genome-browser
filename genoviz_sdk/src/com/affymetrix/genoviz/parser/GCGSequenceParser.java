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

import com.affymetrix.genoviz.datamodel.SequenceI;
import com.affymetrix.genoviz.datamodel.Sequence;

import java.io.*;

/**
 * Handles GCG format sequence data.
 * Strips numbers and white space.
 * It does not handle multiple sequence format (MSF) files.
 *
 * @author Eric Blossom
 * @see MSFParser
 */
public class GCGSequenceParser implements ContentParser {

  /**
   * Imports the data from a stream of bytes.
   * This is defined as returning an Object
   * to conform to the ContentParser interface.
   * However, it actually returns a {@link Sequence}.
   * To use it as such
   * you will need to cast the returned Object back to a Sequence.
   *
   * @param theInput from whence the data come.
   * @return a {@link Sequence}.
   */
  public Object importContent( InputStream theInput ) throws IOException {
    StringBuffer out = new StringBuffer();
    BufferedReader in;
    String line;
    SequenceI seq = new Sequence();

    in = new BufferedReader( new InputStreamReader( theInput ) );
    try {
      line = in.readLine();
      // Find header.
      while ( null != ( line = in.readLine() )
        && !line.endsWith( ".." ) ) {
      }
      seq.setDescription(line);
      while ( null != ( line = in.readLine() ) ) {
        appendLine( out, line );
      }
    } finally {
      in.close();
    }
    seq.setResidues(out.toString());
    return seq;
  }

  /**
   * Not yet implemented.
   */
  public void exportContent( OutputStream out, Object o )
    throws IOException {
  }

  protected void appendLine( StringBuffer out, String theLine ) {
    char[] charsIn = theLine.toCharArray();
    for ( int i = 0; i < charsIn.length; i++ ) {
      char c = charsIn[i];
      switch ( c ) {
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
      case '\t':
      case ' ':
        break;
      default:
        out.append( c );
      }
    }
  }

}
