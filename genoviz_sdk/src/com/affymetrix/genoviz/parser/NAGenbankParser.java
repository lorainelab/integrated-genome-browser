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

import com.affymetrix.genoviz.datamodel.NASeqFeature;
import com.affymetrix.genoviz.datamodel.Range;
import com.affymetrix.genoviz.datamodel.SeqFeatureI;
import com.affymetrix.genoviz.util.Debug;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

/**
 * parses <a href="http://www.ncbi.nlm.nih.gov" target="_top">Genbank</a>
 * "flat file" format nucleic acid data.
 *
 * @author Eric Blossom
 */
public class NAGenbankParser extends GenbankParser {

  protected SeqFeatureI parseLocation(StreamTokenizer toks, String theFeatureType)
    throws IOException
  {
//Debug.inform("parsing location...");
    NASeqFeature f = null;
    int tok = toks.nextToken();
    if (StreamTokenizer.TT_WORD == tok && toks.sval.equals("complement")) {
      tok = toks.nextToken();
      if (tok != '(')
        throw new RuntimeException("invalid location: expected \"(\"");
      f = (NASeqFeature)parseLocation(toks, theFeatureType);
      f.setStrand(NASeqFeature.REVERSE);
      tok = toks.nextToken();
      if (tok != ')')
        throw new RuntimeException("invalid location: expected \")\"");
    }
//    else if (StreamTokenizer.TT_WORD == tok && toks.sval.equals("join")) {
    else if (StreamTokenizer.TT_WORD == tok && 'a' <= toks.sval.charAt(0)) {
      tok = toks.nextToken();
      if (tok != '(')
        throw new RuntimeException("invalid location: expected \"(\"");
      f = (NASeqFeature)parseLocation(toks, theFeatureType);
      tok = toks.nextToken();
      if (tok != ')')
        throw new RuntimeException("invalid location: expected \")\"");
    }
    else {
      toks.pushBack();
      Vector v = parseSpans(toks);
      if (null == f) f = new NASeqFeature(theFeatureType);
      f.setStrand(NASeqFeature.FORWARD);
      Enumeration it = v.elements();
      while (it.hasMoreElements()) {
        Range r = (Range)it.nextElement();
        f.addPiece(r);
      }
    }
    return f;
  }

  /**
   * for testing.
   * /
  public static void main(String argv[]) {
    Debug.setLevel(Debug.INFORM);
    GenbankParser p = new NAGenbankParser();
    try {
      String s = "http://roma/~eric/genbankExample.txt";
      if (0 < argv.length) {
        s = argv[0];
      }
      URL ourURL = new URL(s);
      Object o = p.importContent(ourURL);
      p.exportContent(System.out, o);
    } catch (Exception e) {
      Debug.warn(e.toString());
    }
  }
  /* */

}
