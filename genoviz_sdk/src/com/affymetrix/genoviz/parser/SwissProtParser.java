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

import com.affymetrix.genoviz.datamodel.*;
import com.affymetrix.genoviz.util.Debug;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * parses <a href="http://www.expasy.ch/sprot/" target="_top">SWISS-PROT</a> format data.
 * This was coded from
 * <cite>The SWISS-PROT Protein Sequence Data Bank User Manual</cite>,
 * Release 34, October 1996
 * by Amos Bairoch
 *
 * @author Eric Blossom
 * @author Cyrus Harmon
 */
public class SwissProtParser implements ContentParser {

  //private static final boolean marking = !System.getProperty("java.version", "1.0").startsWith("1.0");
  private static final boolean marking=false;

  /**
   * imports Swiss-prot data from an internet.
   * Many record types are ignored.
   * We parse the following types of records:
   * <ul>
   * <li>ID - Identification.
   * <li>AC - Accession number(s).
   * <li>DE - Description.
   * <li>CC - Comments or notes.
   * <li>FT - Feature table data.
   * <li>SQ - Sequence header (and the following lines of residues).
   * </ul>
   *
   * @param url the Uniform Resource Locator pointing to the Swiss-prot data.
   * @return a representation of the sequence.
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
   * However, it actually returns a AnnotatedSequence.
   * To use it as such
   * you will need to cast the returned Object back to a AnnotatedSequence.
   *
   * @param theInput from whence the data come.
   * @return a representation of the annotated sequence.
   * @see com.affymetrix.genoviz.datamodel.AnnotatedSequence
   * @see ContentParser
   */
  public Object importContent( InputStream theInput ) throws IOException {

    BufferedReader in;
    in = new BufferedReader( new InputStreamReader( theInput ) );

    StringBuffer sequenceBuffer = new StringBuffer();

    AnnotatedSequence sequence = new AnnotatedSequence();
    Sequence seq = new AASequence();
    SeqFeatureI f = null;
    SeqFeatureI prev_feature = null;
    StringBuffer comment = null;
    StringBuffer descr = null;
    StringBuffer seqDescription = null;

    String line = in.readLine();
    while (null != line && !line.equals("//")) {
      String lineType = null;
      String lineText = null;
      try {
        lineType = line.substring(0, 2);
        lineText = line.substring(5);
      } catch (StringIndexOutOfBoundsException e) {
      }
      if (null != lineType && null != lineText) {

        if (lineType.equals("FT")) { // Feature Table Entry
          String type = "";
          int from = 0;
          int to = 0;
          String description = "";
          if (13 <= line.length()) {
            type = line.substring(5, 13).trim();
            if (type.equals("")) {
              if (35 < line.length()) {
                if (null != prev_feature) {
                  f = prev_feature;
                  description = f.getAttribute("note") + line.substring(34);
                  if (0 < description.length()) {
                    f.replaceAttribute("note", description);
                  }
                }
              }
            } else {
              f = new AASeqFeature(type);
              if (20 <= line.length()) {
                from = Integer.parseInt(line.substring(14, 20).trim());
                if (27 <= line.length()) {
                  to = Integer.parseInt(line.substring(21, 27).trim());
                  f.addPiece(new Range(from, to));
                  if (35 < line.length()) {
                    description = line.substring(34);
                    if (0 < description.length()) {
                      f.addAttribute("note", description);
                    }
                  }
                }
              }
              sequence.addFeature(f);
            }
          }
          prev_feature = f;
        } else if (lineType.equals("  ")) { // Sequence Residues
          StringTokenizer st = new StringTokenizer(line);
          while (st.hasMoreTokens()) {
            sequenceBuffer.append(st.nextToken());
          }
        }
        else if (lineType.equals("CC")) {
          if (null == comment) {
            comment = new StringBuffer(lineText);
          }
          else {
            comment.append('\n');
            comment.append(lineText);
          }
        }
        else if (lineType.equals("DE")) {
          if (null == seqDescription) {
            seqDescription = new StringBuffer(lineText);
          }
          else {
            seqDescription.append('\n');
            seqDescription.append(lineText);
          }
        } else if (lineType.equals("ID")) { // Identification line
          StringTokenizer st = new StringTokenizer(lineText);
          if (st.hasMoreTokens()) {
            String id = st.nextToken();
            sequence.addIdentifier(new AdHocIdentifier("swissprot.name", id));
          }
        } else if (lineType.equals("AC")) { // Accession Number
          StringTokenizer st = new StringTokenizer(lineText);
          while (st.hasMoreTokens()) {
            String id = st.nextToken();
            int i = id.indexOf(';');
            if (0 <= i) {
              sequence.addIdentifier( new AdHocIdentifier( "swissprot.accession", id.substring(0, i) ) );
            } else {
              sequence.addIdentifier( new AdHocIdentifier( "swissprot.accession", id ) );
            }
          }
        } else if (lineType.equals("SQ")) { // Sequence Header
        } else {
          //System.err.println("unrecognized line type " + lineType);
        }
      }
      line = in.readLine();
    }
    seq.setResidues(sequenceBuffer.toString());
    sequence.setSequence(seq);
    if (null != seqDescription) {
      descr = new StringBuffer(seqDescription.toString());
      descr.append('\n');
    }
    if (null != comment) {
      if (null == descr) {
        descr = new StringBuffer(comment.toString());
      }
      else {
        descr.append('\n');
        descr.append(comment);
      }
      descr.append('\n');
    }
    if (null != descr) {
      if (marking) {
        sequence.setDescription("<pre>" + descr.toString() + "</pre>");
      }
      else {
        sequence.setDescription(descr.toString());
      }
    }

    return sequence;
  }


  /**
   * is not yet implemented.
   * In it's current form it just puts out the results of toString().
   * It can only be used for debugging.
   */
  public void exportContent( OutputStream theOutput, Object o )
    throws IOException
  {
    if (o instanceof AnnotatedSequence) {
      exportContent(theOutput, (AnnotatedSequence)o);
      return;
    }
    System.err.println( o.toString() );
    throw new IllegalArgumentException(
      "Can only export an AnnotatedSequence.");
  }

  private void exportContent(OutputStream theOutput, AnnotatedSequence theSeq)
    throws IOException
  {
    System.out.println(theSeq.toString());
  }


}
