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

import java.io.*;
import java.net.*;
import java.util.*;

import com.affymetrix.genoviz.datamodel.SubstitutionMatrix;

/**
 * Parses substitution matrix files that come with BLAST programs.
 *
 * <p> Matrix file format:
 * Comment lines start with a hash character (#).
 * The first non-comment line contains matrix column headers,
 * each a single letter amino acid code.
 * The rest of the lines start with a row header
 * (single-letter amino acid code), 
 * followed by integers for each matrix position.
 *  
 * <p> Example:<pre>
 *    #  Matrix made by matblas from blosum62.iij
 *       A  R  N  D  C  Q  E  G  H  I  L  K  M  F  P  S  T  W  Y  V  B  Z  X  *
 *    A  4 -1 -2 -2  0 -1 -1  0 -2 -1 -1 -1 -1 -2 -1  1  0 -3 -2  0 -2 -1  0 -4
 *    R -1  5  0 -2 -3  1  0 -2  0 -3 -2  2 -1 -3 -2 -1 -1 -3 -2 -3 -1  0 -1 -4
 *    etc...
 * </pre>
 *
 * @author Cyrus Harmon
 * @author Eric Blossom
 * @see com.affymetrix.genoviz.datamodel.SubstitutionMatrix
 */
public class BlastMatrixParser {

  /**
   * @return a {@link SubstitutionMatrix}
   */
  public Object importContent(InputStream theInput) throws IOException {
    BufferedReader dis;
    SubstitutionMatrix matrix = new SubstitutionMatrix();
    boolean buffered_arg = false;

    dis = new BufferedReader( new InputStreamReader( theInput ) );
    try {
      String line;
      String field, row_header;
      char column_headers[] = new char[24];
      char row_char, column_char;
      StringTokenizer tokens;
      int row = 0, column = 0;
      boolean no_header_yet = true;
      double val;

      while (null != (line = dis.readLine())) {
        column = 0;

        // comments
        if (line.startsWith("#")) { continue; }

        tokens = new StringTokenizer(line);

        if (no_header_yet && tokens.hasMoreElements()) {
          int index = 0;
          while (tokens.hasMoreElements()) {
            field = tokens.nextToken();
            column_headers[index++] = field.charAt(0);
          }
          no_header_yet = false;
          continue;
        }

        // skip first field, the row id
        if (tokens.hasMoreElements()) {
          row_header = tokens.nextToken();
        }
        else { continue; }  // skip empty lines

        row_char = row_header.charAt(0);

        while (tokens.hasMoreElements()) {
          field = tokens.nextToken();
          val = Float.valueOf(field).doubleValue();
          column_char = column_headers[column];
          matrix.put(row_char, column_char, val);
          column++;
        }
      }
    }  finally {
      dis.close();
    }
    return matrix;
  }

  /**
   * Not yet implemented
   */
  public void exportContent(OutputStream theOutput, Object o)
    throws IOException {
  };

 
}
