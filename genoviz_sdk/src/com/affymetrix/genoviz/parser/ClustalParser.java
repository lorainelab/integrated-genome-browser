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

import com.affymetrix.genoviz.datamodel.AnnotatedSequence;
import com.affymetrix.genoviz.datamodel.MultiSeqAlign;
import com.affymetrix.genoviz.datamodel.SeqFeatureI;
import com.affymetrix.genoviz.datamodel.Range;
import com.affymetrix.genoviz.datamodel.SequenceI;
import com.affymetrix.genoviz.util.Debug;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Parses <a href="http://www2.ebi.ac.uk/clustalw/" target="_top">Clustal W</a>
 * format multiple sequence alignment data.
 * It handles output from clustalw version 1.7
 *
 * @author Eric Blossom
 * @author Cyrus Harmon
 */
public class ClustalParser implements ContentParser {

  /**
   * the data model.
   */
  MultiSeqAlign alignment = new MultiSeqAlign();

  /**
   * imports ClustalW format data from an internet.
   *
   * @param url the Uniform Resourse Locator pointing to the data.
   * @return a representation of the annotated sequence.
   */
  public Object importContent(URL url) {
    Object results = null;
    try {
      InputStream istream = url.openStream();
      results = importContent(istream);
      istream.close();
    }
    catch(Exception ex) {
      System.err.println(ex.getMessage());
    }
    return results;
  }

  /**
   * imports the data from a stream of bytes.
   * This is defined as returning an Object
   * to conform to the ContentParser interface.
   * However, it actually returns a {@link MultiSeqAlign}.
   * To use it as such
   * you will need to cast the returned Object back to a MultiSeqAlign.
   *
   * @param theInput from whence the data come.
   * @return a representation of the multiple-sequence alignment.
   * @see com.affymetrix.genoviz.datamodel.MultiSeqAlign
   * @see ContentParser
   */
  public Object importContent( InputStream theInput ) throws IOException {

    BufferedReader in;
    in = new BufferedReader( new InputStreamReader( theInput ) );

    StringBuffer sequenceBuffer = new StringBuffer();
    String line = in.readLine();
    while (null != line && line.length() < 8
      && !line.substring(0, 8).equals("CLUSTAL "))
    {
      line = in.readLine();
    }
    while (null != (line = in.readLine())) {

      int first = line.indexOf(' ');
      if (2 < first) {         // if line begins with a name (skip blank lines)
        int last = line.lastIndexOf(' ');
        String name = line.substring(0, first).trim();
        if (0 < name.length()) {
          Object
          o = this.alignment.getSequence(name);
          if (null != o) {
            SequenceI seq = (SequenceI) o;
            seq.appendResidues(line.substring(last).trim().replace('-', MultiSeqAlign.GAP_CHAR));
          }
          else {
            String seq = new String(line.substring(last).trim().replace('-', MultiSeqAlign.GAP_CHAR));
            this.alignment.addSequence(name, seq);
          }
        }
      }
    }

    return this.alignment;
  }


  /**
   * If o is a MultiSeqAlign, then exports it.
   * Otherwise throws an IllegalArgumentException.
   */
  public void exportContent( OutputStream theOutput, Object o )
    throws IOException
  {
    if (o instanceof MultiSeqAlign) {
      exportContent(theOutput, (MultiSeqAlign)o);
      return;
    }
    System.err.println( o.toString() );
    throw new IllegalArgumentException(
      "Can only export an MultiSeqAlign.");
  }

  private void exportContent(OutputStream theOutput, MultiSeqAlign theSeq)
    throws IOException
  {
    Enumeration keys = theSeq.sequenceNames();
    while(keys.hasMoreElements()) {
      Object o = keys.nextElement();
      String key = (String) o;
      System.out.println(key + ": " + theSeq.getSequence(key));
    }
  }


}
