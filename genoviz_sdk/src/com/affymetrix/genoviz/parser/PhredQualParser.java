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

import com.affymetrix.genoviz.datamodel.ReadConfidence;
import com.affymetrix.genoviz.datamodel.BaseConfidence;

import com.affymetrix.genoviz.util.Debug;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Parses Phred "qual" format.
 * <p> The quality scores are expected to be in a file
 * whose format looks like this:
 * <pre>
 * &gt;LD34.15    861      0    861  ABI
 * 4 4 4 4 4 6 6 4 4 6 18 4 4 4 6 6 4 6 6 7 7 13 10 7
 * 7 9 9 17 21 31 33 36 33 33 21 20 10 15 16 11 8 8 9
 * 21 16 26 24 26 16 16 6 6 6 6 6 14 17 21 19 13 10 18
 * 16 21 21 31 22 27 27 32 45 34 34 34 36 34 37 40 37
 * </pre>
 *
 * @author Cyrus Harmon
 * @author Eric Blossom
 */
public class PhredQualParser implements ContentParser {

  /**
   * imports the data from a stream of bytes.
   * This is defined as returning an Object
   * to conform to the ContentParser interface.
   * However, it actually returns a {@link ReadConfidence}.
   * To use it as such
   * you will need to cast the returned Object back to a ReadConfidence.
   *
   * @param theInput from whence the data come.
   * @return a {@link ReadConfidence}
   */
  public Object importContent( InputStream theInput ) throws IOException {
    StringBuffer out = new StringBuffer();
    BufferedReader in;
    String line;

    in = new BufferedReader( new InputStreamReader( theInput ) );

    ReadConfidence rc = null;
    try {
      while ( null != ( line = in.readLine() )
          && !line.startsWith( ">" ) ) {
        // Skip header.
      }
      rc = new ReadConfidence();
      StreamTokenizer tokens = new StreamTokenizer(in);
      int tok;
      while ( StreamTokenizer.TT_NUMBER == ( tok = tokens.nextToken() ) ) {
        rc.addBaseConfidence(new BaseConfidence('N', new Double(tokens.nval).intValue()));
      }
    } finally {
      in.close();
    }
    return rc;
  }

  /**
   * not implemented.
   */
  public void exportContent( OutputStream theOutput, Object o )
    throws IOException {
  }

}
