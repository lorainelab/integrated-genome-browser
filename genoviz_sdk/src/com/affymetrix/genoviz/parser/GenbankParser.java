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
import com.affymetrix.genoviz.datamodel.NASeqFeature;
import com.affymetrix.genoviz.datamodel.SeqFeatureI;
import com.affymetrix.genoviz.datamodel.Range;
import com.affymetrix.genoviz.datamodel.SequenceI;
import com.affymetrix.genoviz.datamodel.Sequence;
import com.affymetrix.genoviz.util.Debug;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.lang.reflect.*;

/**
 * Parses <a href="http://www.ncbi.nlm.nih.gov" target="_top">Genbank</a>
 * "flat file" format data.
 * The Java Object used to model this data is an {@link AnnotatedSequence}.
 *
 * @author Eric Blossom
 * @author Cyrus Harmon
 * @author Joe Morris
 */
public abstract class GenbankParser implements ContentParser {

  /**
   * imports Genbank "flat file" format data from an internet.
   *
   * @param url the Uniform Resourse Locator pointing to the data.
   * @return an {@link AnnotatedSequence}.
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
   * imports Genbank "flat file" format data from a stream of tokens.
   *
   * @param toks from whence the data come.
   * @return a String containing all the residues.
   */
  private String parseResidues( StreamTokenizer toks )
    throws IOException
  {
    toks.resetSyntax();
    toks.eolIsSignificant(false);
    toks.whitespaceChars(10, 32);
    toks.whitespaceChars('0', ':');
    toks.wordChars('a', 'z');
    toks.wordChars('A', 'Z');
    int tok;
    StringBuffer sb = new StringBuffer();
loop:
    while (StreamTokenizer.TT_EOF != (tok = toks.nextToken())) {
      switch (tok) {
      case StreamTokenizer.TT_WORD:
        sb.append(toks.sval);
        break;
      default:
        Debug.warn("parsing residues, unexpected token: " + tok);
      case '/':
        Debug.inform("reached the end of input.");
        break loop;
      }
    }
    return sb.toString();
  }

  private String rightColumn(StreamTokenizer toks, int theMargin)
    throws IOException
  {
    String s = toks.sval;
    StringBuffer sb = new StringBuffer();
    String text = s.substring(theMargin);
    sb.append( text );
    int tok = StreamTokenizer.TT_WORD;
loop:
    while (StreamTokenizer.TT_EOF != (tok = toks.nextToken())) {
      switch (tok) {
      default:
      case StreamTokenizer.TT_EOL:
        sb.append('\n');
        break;
      case StreamTokenizer.TT_WORD:
        s = toks.sval;
        int margin_length = Math.min(theMargin, s.length() - 1);
        String label = s.substring(0, margin_length).trim();
        if (0 < label.length()) { // next section starting.
          toks.pushBack();
          break loop;
        }
        text = s.substring(theMargin);
        sb.append( " " + text );
        break;
      }
    }
    return sb.toString();
  }

  private void parseComment( StringBuffer theBuffer, StreamTokenizer toks ) throws IOException {
    String s = toks.sval;
    String text = s.substring(12);
    theBuffer.append("<p>");
    theBuffer.append( text );
    int tok = StreamTokenizer.TT_WORD;
loop:
    while (StreamTokenizer.TT_EOF != (tok = toks.nextToken())) {
      switch (tok) {
      default:
      case StreamTokenizer.TT_EOL:
        theBuffer.append('\n');
        break;
      case StreamTokenizer.TT_WORD:
        s = toks.sval;
        String label = s.substring(0, 12);
        if (!label.startsWith("  ")) { // next section starting.
          toks.pushBack();
          break loop;
        }
        text = s.substring(12);
        theBuffer.append( "\n" + text );
        break;
      }
    }
  }
  
  private static final boolean marking
=false;//    = !System.getProperty("java.version", "1.0").startsWith("1.0");

  /**
   * imports Genbank "flat file" format data from a stream of bytes.
   * This is defined as returning an Object
   * to conform to the ContentParser interface.
   * However, it actually returns an AnnotatedSequence.
   * To use it as such
   * you will need to cast the returned Object back to an AnnotatedSequence.
   *
   * @param theInput from whence the data come.
   * @return a representation of the annotated sequence.
   * @see com.affymetrix.genoviz.datamodel.AnnotatedSequence
   * @see ContentParser
   */
  public Object importContent( InputStream theInput ) throws IOException {
    StringBuffer descr = new StringBuffer();
    AnnotatedSequence sequence = new AnnotatedSequence();
    SequenceI seq = new Sequence();
    InputStreamReader in = new InputStreamReader( theInput );
    StreamTokenizer toks = new StreamTokenizer( in );
    toks.eolIsSignificant(true);
    toks.ordinaryChars(' ', ' ');
    toks.wordChars( ' ', '~' );
    String name;
    while (!(name = parseSectionName(toks)).equals("//")) {
      if (name.equals("FEATURES")) {
        parseFeatureTable(sequence, toks);
      }
      else if (name.equals("COMMENT")) {
        if (marking) {
          parseComment(descr, toks);
        }
        else {
          if (0 < descr.length())
            descr.append('\n');
          descr.append(toks.sval);
          appendSection(descr, toks);
        }
      }
      else if (name.equals("ORIGIN")) {
        Debug.inform("parsing sequence...");
        seq.setResidues(parseResidues(toks));
        sequence.setSequence(seq);
      }
      else {
        Debug.warn("marking up section " + name);
        if (marking) {
          if (0 < descr.length())
            descr.append("<p>");
          descr.append("<strong>");
          descr.append(name);
          descr.append(":</strong> ");
          descr.append(toks.sval.substring(12));
          markupSection(descr, toks);
        }
        else {
          if (0 < descr.length())
            descr.append('\n');
          descr.append(toks.sval);
          appendSection(descr, toks);
        }
      }
    }
    sequence.setDescription(descr.toString());
    return sequence;
  }

  /**
   * Only implemented for instances of AnnotatedSequence.
   */
  public void exportContent( OutputStream theOutput, Object o )
    throws IOException
  {
    if (o instanceof AnnotatedSequence) {
      exportContent(theOutput, (AnnotatedSequence)o);
      return;
    }
    throw new IllegalArgumentException(
      "Can only export an AnnotatedSequence.");
  }

  private void exportContent(OutputStream theOutput, AnnotatedSequence theSeq)
    throws IOException
  {
    PrintWriter pw = new PrintWriter(theOutput, true);
    // Prints the description up to but not including the base count    
    pw.print ( theSeq.getDescription().substring( 0, theSeq.getDescription().indexOf ( "BASE COUNT") ) );
    pw.println ( "FEATURES              Location/Qualifiers");
     exportFeatures( pw, theSeq);
    // Prints the base count
    pw.println ( theSeq.getDescription().substring(theSeq.getDescription().indexOf ( "BASE COUNT") ) );    
    exportBases( pw, theSeq.getSequence().getResidues() );
  }

  private void exportBases ( PrintWriter pw, String theBases )
  {
    int place = 0;
    System.err.println( "begin exportBases" );
    //formats the actual nucleotide sequence as Genbank defines it.
    int numlines =  ( int )( Math.floor ( theBases.length() / 60 ) );
    for ( int i = 0; i < numlines; i++ ) {
      int indent = (int)(8 - Math.floor ( Math.log ( i * 60 + 1 ) / Math.log ( 10 ) ) );
      for ( int j = 1; j < indent; j++ ) {
        pw.print ( " " );
      }
      pw.print ( (i * 60 + 1) + " ");
      for ( int k = 0; k < 6; k++ ) {
        pw.print ( theBases.substring( place , ( place + 10 ) )  + " ");
        place += 10;
      }
      pw.println();
    }
    pw.println("//");
  }

  private void exportFeatures( PrintWriter pw, AnnotatedSequence theSeq)
  {
    StringBuffer toBeFormatted = new StringBuffer("");
    Enumeration e = theSeq.features();
    NASeqFeature aFeature = null;
    Object theObject = new Object();
    String featureName = null;
    Enumeration rangeOrAttrib;
    int numberOfElements = 0;
    Range theRange = null;
    while ( e.hasMoreElements() ) {
      //  aFeature is a feature from the Annoatated Sequence's enumeration
      toBeFormatted.setLength( 0 );
      aFeature = ( NASeqFeature )e.nextElement();
      toBeFormatted.append("     ");
      toBeFormatted.append(aFeature.getType());
      // Prints spaces to the 22nd place, where the feature attributes are supposed to begin.
      for ( int count = 5 +  (( String )aFeature.getType()).length()  ; count < 22; count++)
        toBeFormatted.append(" ");
      rangeOrAttrib = aFeature.pieces();
      // Find out how many members the Enumeration has (how many ranges) for formatting,  
      // then reset it to print.
      while ( rangeOrAttrib.hasMoreElements() ) {
        numberOfElements++;
        theRange = (Range)rangeOrAttrib.nextElement();  
      }
      rangeOrAttrib = aFeature.pieces();
      if (numberOfElements > 1) {
        if ( aFeature.getStrand() == aFeature.REVERSE ) toBeFormatted.append( "complement(" );
        toBeFormatted.append("join(");
        theRange = (Range)rangeOrAttrib.nextElement();
        toBeFormatted.append ( theRange.beg ).append( ".." ).append ( theRange.end );
        while ( rangeOrAttrib.hasMoreElements()) {
          theRange = (Range)rangeOrAttrib.nextElement();
          toBeFormatted.append( "," ).append( theRange.beg ).append( ".." ).append( theRange.end );
        }
        toBeFormatted.append( ")" );
        if ( aFeature.getStrand() == aFeature.REVERSE ) toBeFormatted.append ( ")" );
      }
      else  toBeFormatted.append( theRange.beg ).append ( ".." ).append( theRange.end );
      formatFeature( toBeFormatted.toString(), pw );


      // rangeOrAttrib becomes an Enumeration of attributes, instead of pieces/ranges.
      rangeOrAttrib = aFeature.attributes();
      while ( rangeOrAttrib.hasMoreElements() ) {
        featureName = (String)rangeOrAttrib.nextElement();
        theObject = aFeature.getAttribute ( featureName );

        if (theObject instanceof Vector ) {
          Vector theVect = (Vector) theObject;
          for (int i = 0; i < theVect.size(); i++) {
            toBeFormatted.setLength( 0 );
            toBeFormatted.append("/").append( featureName ).append( "=" );
            toBeFormatted.append("\"").append( theVect.elementAt( i ).toString() ).append("\"");
            formatFeatureAttribute( toBeFormatted.toString(), pw );
          }
        }

        else {

          try  {
            /* this makes all attributes which are numbers int's, such as
               '  /codon_start=1 ' as opposed to ' /codon_start=1.0 ',
               which is what a mere string rendition yeilds.
               Works well for codon_start, which is what is in all of my examples,
               but if any of the attribs are doubles that should be printed as doubles,
               it will need changing.
             */
            toBeFormatted.setLength( 0 );
            toBeFormatted.append("/").append( featureName ).append( "=" );
            Float temp = Float.valueOf ( aFeature.getAttribute( featureName ) + "" );
            toBeFormatted.append( temp.intValue() ) ;
            formatFeatureAttribute( toBeFormatted.toString(), pw );
          }

          catch (NumberFormatException except) {
            toBeFormatted.setLength( 0 );
            toBeFormatted.append("/").append( featureName ).append( "=" );
            toBeFormatted.append( "\"" ).append( aFeature.getAttribute( featureName) ).append ("\"");
            formatFeatureAttribute( toBeFormatted.toString(), pw );
          }
        }
      }
    }
  }

private void formatFeatureAttribute ( String toBeFormatted, PrintWriter pw ) {

    StringTokenizer parser = new StringTokenizer ( toBeFormatted, " ", true);
    String theToken = "";
    StringBuffer theLine = new StringBuffer ("");
    theLine.append ( "                      " );

    while ( parser.hasMoreTokens() ) {
      theToken = parser.nextToken();

      if (theToken.length() > 58) {    //in case there are no spaces - i.e., a sequence translation
        int numLines = (int)Math.floor ( theToken.length() / 58 );
        for (int i = 0; i < numLines; i++) {
          pw.print (  "                      " );
          pw.println ( theToken.substring ( i * 58, ( i + 1) * 58 ) );
        }
        if ( theToken.length() > numLines * 58 ) {
          pw.print (  "                      " );
          pw.println ( theToken.substring ( numLines * 58 ) );
        }
        return;
      }

      if ( ( theToken.length() + theLine.length() ) < 81 ) {
        theLine.append ( theToken );
        if ( parser.hasMoreTokens() ) theLine.append( parser.nextToken() );  // the separator comma
      }
      else {
        pw.println ( theLine.toString() );
        theLine.setLength( 0 );
        theLine.append(  "                      " ).append(  theToken );
        if ( parser.hasMoreTokens() ) theLine.append( parser.nextToken() );
      }
    }
    pw.println ( theLine );
  }
  
  private void formatFeature ( String toBeFormatted, PrintWriter pw )
  {  
    StringTokenizer parser = new StringTokenizer ( toBeFormatted, ",", true);
    String theToken = "";
    StringBuffer theLine = new StringBuffer ();
    //Formats the first line of a feature, with the feature type and range
    while ( parser.hasMoreTokens() ) {
      theToken = parser.nextToken();
      if ( ( theToken.length() + theLine.length() + 1 ) < 78 ) {
        theLine.append ( theToken );
        if ( parser.hasMoreTokens() ) theLine.append( parser.nextToken() );  // the separator comma
      }
      else {
        pw.println ( theLine.toString() );
        theLine.setLength( 0 );
        theLine.append(  "                      " ).append(  theToken );
        if ( parser.hasMoreTokens() ) theLine.append( parser.nextToken() );
      }
    }
    pw.println ( theLine );
  }
  private String parseSectionName(StreamTokenizer toks) 
    throws IOException
  {
    int tok = toks.nextToken();
    switch (tok) {
    default:
    case StreamTokenizer.TT_EOF:
      return "//";
    case StreamTokenizer.TT_WORD:
      String line = toks.sval;
      if (!line.startsWith("  ")) {
        if (13 > line.length()) return line.trim();
        return line.substring(0, 12).trim();
      }
    }
    throw new RuntimeException("expected a section");
  }

  /**
   * appends an arbitrary section.
   *
   * @param theBuffer is where the text goes.
   * @param toks is where the text comes from.
   */
  private void appendSection(StringBuffer theBuffer, StreamTokenizer toks) 
    throws IOException
  {
    int tok;
    while (StreamTokenizer.TT_WORD == (tok = toks.nextToken())) {
      switch (tok) {
      default:
      case StreamTokenizer.TT_EOF:
        return; // Perhaps we should throw an exception here.
      case StreamTokenizer.TT_WORD:
        String line = toks.sval;
        if (!line.startsWith("  ")) { // It is the start of the next section.
          toks.pushBack();
          return;
        }
        else {
          theBuffer.append('\n');
          theBuffer.append(line);
        }
      }
    }
  }

  /**
   * marks up text in an arbitrary section.
   *
   * @param theBuffer is where the marked up text goes.
   * @param toks is where the text comes from.
   */
  private void markupSection(StringBuffer theBuffer, StreamTokenizer toks) 
    throws IOException
  {
    int tok;
    while (StreamTokenizer.TT_WORD == (tok = toks.nextToken())) {
      switch (tok) {
      default:
      case StreamTokenizer.TT_EOF:
        return; // Perhaps we should throw an exception here.
      case StreamTokenizer.TT_WORD:
        String line = toks.sval;
        if (!line.startsWith("  ")) { // It is the start of the next section.
          toks.pushBack();
          return;
        }
        if (!line.startsWith("       ")) { // It is the start of a sub section.
          theBuffer.append("<br><em>");
          if (line.length() < 13) {
            theBuffer.append(line.trim());
            theBuffer.append("</em> ");
          }
          else {
            theBuffer.append(line.substring(0, 12).trim());
            theBuffer.append("</em> ");
            theBuffer.append(line.substring(12));
          }
        }
        else {
          theBuffer.append(line);
        }
      }
    }
  }

  private void skipSection(StreamTokenizer toks) 
    throws IOException
  {
    int tok;
    while (StreamTokenizer.TT_WORD == (tok = toks.nextToken())) {
      switch (tok) {
      default:
      case StreamTokenizer.TT_EOF:
        return; // Perhaps we should throw an exception here.
      case StreamTokenizer.TT_WORD:
        String line = toks.sval;
        if (!line.startsWith("  ")) {
          toks.pushBack();
          return;
        }
      }
    }
  }

  private void parseFeatureTable(AnnotatedSequence theSeq, StreamTokenizer toks)
    throws IOException
  {
    // What about 2 optional lines?
    SeqFeatureI feature;
    while (null != (feature = parseFeature(toks))) {
      theSeq.addFeature(feature);
    }
  }

  private long features = 0;
  private SeqFeatureI parseFeature(StreamTokenizer toks)
    throws IOException
  {
    SeqFeatureI f = null;
    int tok = toks.nextToken();

    switch (tok) {
    default:
    case StreamTokenizer.TT_EOF:
      return null;
    case StreamTokenizer.TT_WORD:
      String line = toks.sval;
      String lineType = line.substring(0, 2);
      if (lineType.equals("  ") || lineType.equals("FT")) {
        if (20 > line.length()) return null; // line.trim();
        String feature = rightColumn(toks, 21);
        String featureType = line.substring(2, 21).trim();
        StreamTokenizer stoks = new StreamTokenizer(
          new StringReader(feature));
        stoks.ordinaryChar('.');
        stoks.ordinaryChar('/');
        stoks.wordChars('_', '_');
        f = parseLocation(stoks, featureType);
        while ('/' == (tok = stoks.nextToken())) {
          tok = parseAttribute(f, stoks);
        }
        stoks.pushBack();
        while (StreamTokenizer.TT_EOF != (tok = stoks.nextToken())) {
          switch (tok) {
          case StreamTokenizer.TT_WORD:
            Debug.inform("string token >" + stoks.sval + "<");
            break;
          case StreamTokenizer.TT_NUMBER:
            Debug.inform("number token >" + stoks.nval + "<");
            break;
          case '\"':
            Debug.inform("quoted string >" + stoks.sval + "<");
          default:
            Debug.inform("other token >" + tok + "<");
          }
        }
        features++;
        if (0 == features % 100) {
          System.gc();
          try {
            Thread.sleep(100);
          } catch (InterruptedException ie) {
          }
        Debug.inform("Parsed " + features + " features.");
        Debug.inform(" at line " + line);
        }
        return f; // line.substring(5, 20).trim();
      }
      toks.pushBack();
      return null;
    }
  }

  private int parseAttribute(SeqFeatureI f, StreamTokenizer stoks) 
    throws IOException
  {
    String attName = "", attValue = "";
    int tok = stoks.nextToken();
    switch (tok) {
    case StreamTokenizer.TT_WORD:
      attName = stoks.sval;
      break;
    case StreamTokenizer.TT_NUMBER:
      throw new RuntimeException(
        "expected an attrubute name. got a number " + stoks.nval);
    default:
      throw new RuntimeException("expected an attrubute name. got "+ tok);
    }
    tok = stoks.nextToken();
    if ('/' == tok) {
      return tok;
    }
    if (StreamTokenizer.TT_EOL == tok || StreamTokenizer.TT_EOF == tok) {
      return tok; // It was an attribute (a.k.a. qualifier) with no value.
    }
    if ('=' != tok) {
      throw new RuntimeException("expected \"=\". got "+ tok);
    }
    tok = stoks.nextToken();
    switch (tok) {
    case StreamTokenizer.TT_WORD:
    case '"':
      attValue = stoks.sval;
      break;
    case StreamTokenizer.TT_NUMBER:
      attValue = "" + stoks.nval;
      break;
    case StreamTokenizer.TT_EOL:
      break;
    case '(':
      attValue = parseParen(stoks);
      break;
    case '[':
      attValue = "" + parseRef(stoks);
      break;
    default:
      throw new RuntimeException("expected a value. got "+ tok);
    }
    if (attName.equals("translation")) {
      attValue = spaceless(attValue);
    }
    f.addAttribute(attName, attValue);

    return tok;
  }

  private String parseParen(StreamTokenizer toks)
    throws IOException
  {
    StringBuffer sb = new StringBuffer("(");
    int depth = -1;
    int tok;
    do {
      tok = toks.nextToken();
      switch (tok) {
      case StreamTokenizer.TT_WORD:
      case '"':
        sb.append(toks.sval);
        break;
      case StreamTokenizer.TT_NUMBER:
        sb.append("" + toks.nval);
        break;
      case StreamTokenizer.TT_EOF:
        break;
      case '(':
        depth--;
        sb.append((char)tok);
        break;
      case ')':
        depth++;
        sb.append((char)tok);
        break;
      default:
        sb.append((char)tok);
        break;
      }
    } while (depth < 0 && StreamTokenizer.TT_EOL != tok);
    return sb.toString();
  }

  private int parseRef(StreamTokenizer toks)
    throws IOException
  {
    int n = -1;
    int token = toks.nextToken();
    switch (token) {
    case StreamTokenizer.TT_NUMBER:
      n = (int) toks.nval;
      token = toks.nextToken(); // parse the closing "]".
      break;
    default:
      toks.pushBack();
    }
    return n;
  }


  private String spaceless(String theSource) {
    StringTokenizer st = new StringTokenizer(theSource);
    StringBuffer sb = new StringBuffer();
    while (st.hasMoreTokens()) {
      sb.append(st.nextToken());
    }
    return sb.toString();
  }

  protected abstract SeqFeatureI parseLocation(
    StreamTokenizer toks, String theFeatureType)
    throws IOException;

  protected Vector parseSpans(StreamTokenizer toks)
    throws IOException
  {
    int tok;
    Vector v = new Vector();
    do {
      Range r = parseSpan(toks);
      if (null != r) {
        v.addElement(r);
      }
      tok = toks.nextToken();
    }
    while (',' == tok);
    toks.pushBack();
    return v;
  }

  private Range parseSpan(StreamTokenizer toks)
    throws IOException
  {
    int tok = toks.nextToken();
    if ('<' == tok || '>' == tok) 
      tok = toks.nextToken();
    if (StreamTokenizer.TT_WORD == tok ) { // it might be an external reference
      tok = toks.nextToken();
      if (':' == tok) {
        parseSpan(toks);
        return null;
      } else {
        throw new RuntimeException("invalid span: expected \":\"");
      }
    }
    if (StreamTokenizer.TT_NUMBER != tok)
      throw new RuntimeException("invalid span: expected number");
    int x = (int)toks.nval;
    tok = toks.nextToken();
    switch (tok) {
    case '.':
    case '^':
      break;
    default:
      toks.pushBack();
      return new Range(x, x);
    }
    tok = toks.nextToken();
    if ('<' == tok || '>' == tok) 
      tok = toks.nextToken();
    if (StreamTokenizer.TT_NUMBER != tok)
      throw new RuntimeException("invalid span: expected number");
    int y = (int)toks.nval;
    return new Range(x, y);
  }

  private void skipFeature(StreamTokenizer toks) 
    throws IOException
  {
    int tok;
    while (StreamTokenizer.TT_WORD == (tok = toks.nextToken())) {
      switch (tok) {
      default:
      case StreamTokenizer.TT_EOF:
        return; // Perhaps we should throw an exception here.
      case StreamTokenizer.TT_WORD:
        String line = toks.sval;
        if (!line.startsWith("             ")) {
          toks.pushBack();
          return;
        }
      }
    }
  }

}
