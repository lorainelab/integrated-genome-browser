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
import com.affymetrix.genoviz.datamodel.AnnotatedSequence;
import com.affymetrix.genoviz.datamodel.SeqFeatureI;
import com.affymetrix.genoviz.datamodel.NASeqFeature;
import com.affymetrix.genoviz.datamodel.Range;
import com.affymetrix.genoviz.util.Debug;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Parses
 * <a href="http://www.sanger.ac.uk/Software/formats/GFF/">GFF</a>
 * formatted sequence feature data.
 * GFF stands for General Feature Format.
 *
 * <p> This parses and emits GFF version 1 and 2.
 * Support for version 2 is preliminary (6/29/00).
 *
 * <p> As yet,
 * this ignores the meta-data tags
 * date, source-version, and sequence-region
 *
 * @author Eric Blossom
 * @author Steve Chervitz
 */
public class GFFParser implements ContentParser {

  /**
   * imports GFF format data from an internet.
   *
   * @param url the Uniform Resourse Locator pointing to the data.
   * @return a representation of the annotated sequence or a Vector of them.
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
   * However, it actually returns an {@link AnnotatedSequence}
   * or a Vector of them.
   * To use it as such
   * you will need to cast the returned Object back to an AnnotatedSequence.
   * If it is a Vector,
   * you can iterate through it casting each object to an AnnotatedSequence.
   *
   * @param theInput from whence the data come.
   * @return a representation of the annotated sequence or a Vector of them.
   */
  public Object importContent( InputStream theInput ) throws IOException {

    Hashtable annoSeqs = new Hashtable();

    InputStreamReader in = new InputStreamReader( theInput );
    StreamTokenizer tokens = new StreamTokenizer( in );

    tokens.wordChars(' '+1, '"'); // (!")
    tokens.wordChars('$', '-'); // ($%&'()*+,-)
    tokens.wordChars('/', '/');
    tokens.ordinaryChars( '0', '9' ); // undo parseNumbers().
    tokens.wordChars( '0', '9' ); // allow digits at start of a word.
    tokens.wordChars(':', '~');
    tokens.eolIsSignificant(true);
    int token;

    token = tokens.nextToken();
    while (StreamTokenizer.TT_EOF != token) {
      switch (token) {
      case StreamTokenizer.TT_WORD:
        tokens.wordChars(' ', ' ');
        parseLine(token, tokens, annoSeqs);
        tokens.ordinaryChar(' ');
        token = tokens.nextToken();
        break;
      case '#':
        token = tokens.nextToken();
        switch (token) {
        case '#': // meta info line
          parseMetaLine(tokens, annoSeqs);
          token = tokens.nextToken();
          break;
        default: // actual comment
          token = consumeLine(token, tokens);
        }
        break;
      default:
        token = tokens.nextToken();
      }
    }

    if ( annoSeqs.size() < 1 ) {
      return null;
    }
    else if ( 1 == annoSeqs.size() ) {
      Enumeration e = annoSeqs.elements();
      Object o = e.nextElement();
      return o;
    }
    else {
      Vector v = new Vector( annoSeqs.size() );
      Enumeration e = annoSeqs.elements();
      while ( e.hasMoreElements() ) {
        Object o = e.nextElement();
        v.addElement( o );
      }
      return v;
    }

  }

  private AnnotatedSequence getAnnotSeq( String theName, Hashtable theSeqs ) {
    AnnotatedSequence as;
    Object o = theSeqs.get( theName );
    if ( null == o ) {
      as = new AnnotatedSequence();
      as.addIdentifier( theName );
      theSeqs.put( theName, as );
    }
    else {
      as = (AnnotatedSequence) o;
    }
    return as;
  }

  private static final int BEGIN = 0;
  private static final int IN_SEQ = BEGIN+1;
  private StringBuffer residuesBuffer;
  private int residuesBuffer_capacity = 4096;
  private String seqName;
  int metaState = BEGIN;

  /**
   * parse a line of metadata.
   * Such lines start with two hash characters "##".
   *
   * @param theTokens from whence the data come.
   * @param theSeqs contains the annotated sequences
   * where our sequences is stored.
   */
  protected void parseMetaLine(StreamTokenizer theTokens, Hashtable theSeqs)
    throws IOException {
    int token = theTokens.nextToken();
    switch (token) {
    case StreamTokenizer.TT_WORD:
      String keyWord = theTokens.sval;
      if (keyWord.equalsIgnoreCase("DNA")) {
        metaState = IN_SEQ;
        residuesBuffer = new StringBuffer(residuesBuffer_capacity);
        token = theTokens.nextToken();
        if ( StreamTokenizer.TT_WORD == token ) {
          this.seqName = theTokens.sval;
        }
        consumeLine(token, theTokens);
      }
      else if (keyWord.equalsIgnoreCase("end-DNA")) {
        metaState = BEGIN;
        AnnotatedSequence as = getAnnotSeq( this.seqName, theSeqs );
        SequenceI seq = new Sequence();
        seq.setResidues(residuesBuffer.toString());
        residuesBuffer = null;
        System.gc(); // Desperate attempt to avoid running out of memory.
        seq.setID(this.seqName);
        as.setSequence(seq);
        consumeLine(token, theTokens);
      }
      // Now supports version 1 and 2. Version 2 support still early. SAC 6/29/00
      else if (keyWord.equalsIgnoreCase("gff-version")) {
        consumeLine(token, theTokens);
      }
      else if (keyWord.equalsIgnoreCase("source-version")) {
        consumeLine(token, theTokens);
      }
      else if (keyWord.equalsIgnoreCase("date")) {
        consumeLine(token, theTokens);
      }
      else if (keyWord.equalsIgnoreCase("sequence-region")) {
        consumeLine(token, theTokens);
      }
      else { // Residue Codes or Error
        switch (metaState) {
        case BEGIN: // actually, an error.
          System.err.println("unrecognized meta info line. " + keyWord);
          break;
        case IN_SEQ:
          if ( 1048576 < residuesBuffer_capacity &&
               residuesBuffer_capacity <
               residuesBuffer.length() + theTokens.sval.length() ) {
            residuesBuffer.ensureCapacity( residuesBuffer.capacity() + 1000000 );
          }
          residuesBuffer.append(theTokens.sval);
          if ( residuesBuffer_capacity < residuesBuffer.capacity() ) {
            System.gc();
            residuesBuffer_capacity = residuesBuffer.capacity();
          }
          break;
        }
        consumeLine(token, theTokens);
      }
      break;
    }
  }

  /**
   * throws out the rest of the line comming in.
   * Tokens are read and discarded
   * until either an EOL is encountered
   * or the input runs out.
   */
  protected int consumeLine(int token, StreamTokenizer theTokens)
    throws IOException {
    while (StreamTokenizer.TT_EOL != token &&
           StreamTokenizer.TT_EOF != token) {
      token = theTokens.nextToken();
    }
    return token;
  }

  /**
   * Sets the tokenizer for "normal" lines.
   * The character '#' starts a comment.
   * "Words" are any string of non-numeric characters
   * ( including spaces, excluding '#', '.', and '/' ).
   *
   * NOTE: Nothing is calling this method. SAC 6/30/00
   */
  protected void setLineSyntax(StreamTokenizer theTokens) {
    throw new RuntimeException( "Someone called setLineSyntax!");
  }

  /**
   * parses a "normal" line.
   * i.e. a line that does not start with "##".
   *
   * <p>Fields are: <pre>
   * seqname source feature start end score strand frame [group/attributes] [comments]
   * </pre>
   *
   * @param token the current token just received.
   * @param tokens whence more data will come.
   * @param theSeqs contains the annotated sequences
   * where our sequences is stored.
   */
  protected void parseLine(int token,
                           StreamTokenizer tokens,
                           Hashtable theSeqs)
    throws IOException {
    SeqFeatureI f = null;
    String source = null;
    String feature = null;
    int start = 0;
    int end = 0;
    String score = null;
    String frame = null;
    StringBuffer group = null;
    if (StreamTokenizer.TT_WORD == token) {
      this.seqName = tokens.sval;
      AnnotatedSequence sequence = getAnnotSeq( this.seqName, theSeqs );
      if (StreamTokenizer.TT_WORD == (token = tokens.nextToken())) {
        source = tokens.sval;
        if (StreamTokenizer.TT_WORD == (token = tokens.nextToken())) {
          feature = tokens.sval;
          f = new NASeqFeature(source + " " + feature);
          f.addAttribute("source", source);
          // NOTE: Scientific notation numbers (e.g., 7.543e+34) get botched
          //       with parseNumbers() Therefore, we're processing everything
          //       a word token.  SAC 6/29/00
          // We're expecting some numbers on the rest of the line.
          if (StreamTokenizer.TT_WORD == (token = tokens.nextToken())) {
            start = Integer.parseInt(tokens.sval);
            if (StreamTokenizer.TT_WORD == (token = tokens.nextToken())) {
              end = Integer.parseInt(tokens.sval);
              f.addPiece(new Range(start, end));

              if (setScore( tokens, f)) {

                token = tokens.nextToken(); // Trying for strand.
                if ('-' == token) {
                  tokens.sval = "-";
                  token = StreamTokenizer.TT_WORD;
                }
                else if ('+' == token) {
                  tokens.sval = "+";
                  token = StreamTokenizer.TT_WORD;
                }
                else if ('.' == token) {
                  tokens.sval = ".";
                  token = StreamTokenizer.TT_WORD;
                }
                // Note that the next condition is met when there is a '.' in this column.
                // However, it is also met by invalid input of an actual zero like '0.00'.
                // A '.' is valid input meaning that there is no strand information.
                // A number is not valid.
                // This hack allows us to read a valid file.
                // Someday, someone should figure out how to do this the right way. - elb
                else if ( StreamTokenizer.TT_NUMBER == token && 0.0 == tokens.nval ) {
                  // Mark the strand as unknown.
                  tokens.sval = ".";
                  token = StreamTokenizer.TT_WORD;
                }
                if (StreamTokenizer.TT_WORD == token) {
                  char strand = tokens.sval.charAt(0);
                  switch (strand) {
                  case '+':
                    ((NASeqFeature)f).setStrand(NASeqFeature.FORWARD);
                    break;
                  case '-':
                    ((NASeqFeature)f).setStrand(NASeqFeature.REVERSE);
                    break;
                  }

                  if (StreamTokenizer.TT_EOL != (token = tokens.nextToken())) {
                    if (StreamTokenizer.TT_WORD == token) {
                      frame = tokens.sval;
                      f.addAttribute("frame", frame);
                    }
                  }

                  // tab is only non-word char
                  tokens.quoteChar('"'); // protects tabs and other junk in quotes
                  tokens.wordChars('#', '#');
                  token = tokens.nextToken();
                  // check for no end-of-line and no comment
                  if ( StreamTokenizer.TT_EOL != token ) {
                    group = new StringBuffer();
                    while (token != StreamTokenizer.TT_EOL) {

                      if ( token == StreamTokenizer.TT_WORD && tokens.sval.trim().startsWith( "#" ) ) {
                        break; // found start of comment
                      }
                      else if ( token == '"' ) {
                        group.append(" \"" + tokens.sval + "\"" );
                      }
                      else if ( token == StreamTokenizer.TT_WORD ) {
                        group.append(" " + tokens.sval);
                      }
                      else if ( token == StreamTokenizer.TT_NUMBER ) {
                        group.append(" " + String.valueOf ( tokens.nval));
                      }

                      token = tokens.nextToken();
                    }
                    if ( ( null != group ) && ( group.length() > 0 ) ) {
                      addAttributes( f, group.toString() );
                      group.setLength(0);
                    }
                  }
                  tokens.ordinaryChar((int)'"');;
                  tokens.wordChars('"', '"');
                  tokens.ordinaryChar((int)'#');
                  sequence.addFeature(f);
                }
              }
              else {
                System.err.println( "WARNING: Can't set score: sval = " +
                    tokens.sval +", nval = " + tokens.nval);
              }
            }
          }
        }
      }
    }

    while (StreamTokenizer.TT_EOF != token && StreamTokenizer.TT_EOL != token) {
      token = tokens.nextToken();
    }
  }

  /**
   * Parsing of scientific-notation numbers requires special care.
   * <ul>
   * <li> Positive sci notation values are recognized as words (e.g., 8.9717e+160)
   *    - These numbers are fully contained in sval.
   * <li> Negative sci notation values are recognized as numbers (e.g., -1.9964e+256)
   *    - The tokenizer splits it into two parts, -1.9964 (number) and e+256 (word).
   *    - The exponential part of must be obtained by another call to nextToken().
   *    - If that next token does starts with e+ or e-, then it gets added
   *      to the previous token to construct the full sci notation value.
   *      Otherwise, the original number was not sci notation and pushBack()
   *      is called to undo the second call to nextToken().
   * </ul>
   */
  private boolean setScore( StreamTokenizer tokens, SeqFeatureI f )
    throws IOException
  {

    boolean result = false;

    int token = tokens.nextToken();

    if( StreamTokenizer.TT_WORD == token) {
      if( tokens.sval.equalsIgnoreCase( "nan" ) ) {
        System.err.println( "WARNING: NaN score:  Feature: " + f + "\n---\n" );
        double dscore = Double.NaN;
        result = true;
        f.addAttribute("score", "" + dscore);
      }
      else {
        String sscore = tokens.sval;
        f.addAttribute("score", "" + sscore);
        result = true;
      }
    }

    else if (StreamTokenizer.TT_NUMBER == token ) {
      double dscore = tokens.nval;
      result = true;
      token = tokens.nextToken();

      if( null != tokens.sval &&
          ( tokens.sval.toLowerCase().startsWith("e+") ||
            tokens.sval.toLowerCase().startsWith("e-")))
      {
        f.addAttribute("score", "" + dscore + tokens.sval);
      }
      else {
        f.addAttribute("score", "" + dscore);
        tokens.pushBack();
      }
    }

    else {
      // Scores such as "-inf" cause the tokens.nval to contain the score
      //from the preceeding line.

      if ('-' == token) {
        token = tokens.nextToken();
        if( tokens.sval.toLowerCase().startsWith( "inf" ) ) {
          System.err.println( "WARNING: Negative infinity score:  Feature: " +
              f + "\n---\n" );
          double dscore = Double.NEGATIVE_INFINITY;
          result = true;
          f.addAttribute("score", "" + dscore);
        }
      }

      else if ('+' == token) {
        token = tokens.nextToken();
        if( tokens.sval.toLowerCase().startsWith( "inf" ) ) {
          System.err.println( "WARNING: Positive infinity score:  Feature = " +
              f + "\n---\n" );
          double dscore = Double.POSITIVE_INFINITY;
          result = true;
          f.addAttribute("score", "" + dscore);
        }
      }

      else {
        throw new IOException( "Unexpected GFF score token: ->" + token + "<-,  sval ="
                                 +  tokens.sval +", nval = "
                                 + tokens.nval
                                 + "\n  FEATURE: " + f + "\n---\n");
      }
    }

    return result;
  }


  /**
   * To support GFF version 2, we need to examine group for the
   * key-value structure and create a set of features for each key
   * value pair. The value can actually be a multi-valued list. For
   * simplicity, we will treat it as a single value. SAC 6/28/00
   *
   * <p>Example group data:<pre>
   *   gene_id "ctg15556-000000.0"; transcript_id "ctg15556-000000.0.2"; exon_number 0
   * </pre>
   *
   * <p> Strategy:<ul>
   *  <li> Instead of adding the whole group as a single
   * note attribute to the feature, attempt to parse it into key-value
   * pairs and add each as separate attributes.
   *   <li> If no attributes are defined, add the whole string as a
   * single "note" attribute.
   *  <li> Issue: There is no way to distinguish the attributes added from
   * the group column versus attributes added from the other columns.
   * </ul>
   */
  private void addAttributes ( SeqFeatureI feat, String attribs ) {

    StringTokenizer strtok = new StringTokenizer( attribs, ";" );

    if( strtok.countTokens() == 1 ) {
      feat.addAttribute( "note", attribs );
    }
    else {
      String key = null;
      StringBuffer valbuf = new StringBuffer();
      for(int i = 0; strtok.hasMoreElements(); i++ ) {
        addKeyValuePair(feat, strtok.nextToken());
      }
    }
  }

  protected void addKeyValuePair( SeqFeatureI feat, String attrib ) {

    StringTokenizer strtok = new StringTokenizer( attrib );

    if( strtok.countTokens() == 1 ) {
      feat.addAttribute( "note", attrib );
    }
    else {
      String key = null;
      StringBuffer valbuf = new StringBuffer();
      for(int i = 0; strtok.hasMoreElements(); i++ ) {
        if( 0 == i ) {
          key = strtok.nextToken();
        }
        else {
          valbuf.append( strtok.nextToken() + " ");
        }
      }

      if( valbuf.length() > 0 ) {
        String value = valbuf.toString().trim();
        // Strip off double quotes, if any.
        if( value.indexOf('"') == 0 && value.lastIndexOf('"') == value.length() - 1) {
          feat.addAttribute( key, value.substring(1, value.length()-1) );
        }
        else {
          feat.addAttribute( key, value );
        }
      }
      else if( key != null ) {
        feat.addAttribute( "note", key );
      }
    }
  }


  public void exportContent( OutputStream theOutput, Object o )
    throws IOException
  {
    PrintWriter pw = new PrintWriter(theOutput, true);

    if (o instanceof AnnotatedSequence) {
      fileHeader(pw);
      exportContent(pw, (AnnotatedSequence)o);
      return;
    }
    else if ( o instanceof Vector ) {
      Enumeration e = ((Vector)o).elements();
      fileHeader(pw);
      while ( e.hasMoreElements() ) {
        exportContent( pw, (AnnotatedSequence) e.nextElement() );
        pw.println();
      }
      return;
    }
    pw.close();

    System.err.println( o.toString() );
    throw new IllegalArgumentException ("Can only export a Sequence.");
  }

  /**
   * puts the header meta lines on the print writer.
   */
  private void fileHeader(PrintWriter pw) throws IOException {
    pw.println("##gff-version 1");
    pw.println("##source-version com.affymetrix.genoviz.parser.GFFParser");
    Date now = new Date();
    pw.println("##date " + (1900+now.getYear()) + "-" + now.getMonth() +
        "-" + now.getDate());
  }

  private final static int LINE_LENGTH = 60;

  /**
   *  prints the residues in a meta line.
   *  Called by the version of exportContent(PrintWriter, AnnotatedSequence).
   *
   *  @param pw The output for the sequence to go to
   *  @param theSequence The sequence being printed
   */
  protected void exportContent(PrintWriter pw, SequenceI theSequence)
    throws IOException
  {
    if ( null == theSequence ) return; // Nothing to do.
    pw.print("##DNA");
    String s = theSequence.getID();
    if ( null != s ) {
      pw.print(" " + s);
    }
    pw.println();
    String r = (theSequence.getResidues());
    int limit = r.length() - LINE_LENGTH;
    int i;
    for (i=0; i< limit; i+= LINE_LENGTH) {
      pw.println("##"+r.substring(i, i+LINE_LENGTH));
    }
    if ( i < r.length() ) {
      pw.println("##"+r.substring(i));
    }
    pw.println("##end-DNA");
  }


  /**
   * puts a GFF version of the annotated sequence on a print writer.
   * Called by the public exportContent method.
   * It uses exportContent(PrintWriter, Sequence)
   * to print the sequence, and then prints the features itself.
   * Since files imported from GFF in the GenoViz are currently storing
   * both the ID and the source in the field returned by AnnotatedSequence.getID(),
   * this exporter checks to see if the source String is in the ID.
   * If it isn't, it prints the source separately.
   * @param pw The place to send output
   * @param theAnnoSequence The sequence being printed
   */
  protected void exportContent(PrintWriter pw, AnnotatedSequence theAnnoSequence)
    throws IOException
  {
    exportContent(pw, theAnnoSequence.getSequence());

    if (theAnnoSequence.featureCount() != 0) {

      Enumeration seqFeatures = theAnnoSequence.features();
      while ( seqFeatures.hasMoreElements() ) {
        NASeqFeature feature = ((NASeqFeature)seqFeatures.nextElement());
        String theSource = feature.getAttribute("source") + "";
        String theType = feature.getType();
        String ourID = null;
        SequenceI si = theAnnoSequence.getSequence();
        if ( null != si ) {
          ourID = si.getID();
        }
        if ( null == ourID || ourID.length() < 1 ) {
          Enumeration ids = theAnnoSequence.identifiers();
          if ( ids.hasMoreElements() ) {
            Object o = ids.nextElement();
            if ( null != o ) {
              ourID = o.toString();
            }
          }
        }
        if ( null == ourID || ourID.length() < 1 ) {
          ourID = "???";
        }
        pw.print(ourID);
        pw.print("\u0009");
        pw.print(theSource);
        pw.print("\u0009");
        if (theType.indexOf(theSource + " ") == 0) {
          pw.print(theType.substring(theSource.length()));
        }
        else {
          pw.print(theType);
        }
        pw.print("\u0009");
        Enumeration pieceEnum = feature.pieces();
        while (pieceEnum.hasMoreElements() ) {
          Range theRange = ((Range)pieceEnum.nextElement());
          pw.print(theRange.beg + "\u0009");
          pw.print(theRange.end + "\u0009");
        }
        String scoreTemp = (feature.getAttribute("score") + "");
        if (scoreTemp.compareTo("0.0") == 0) {
          pw.print("0");
        }
        else {
          pw.print(scoreTemp);
        }
        pw.print("\u0009");
        if (feature.getStrand() == NASeqFeature.FORWARD) {
          pw.print("+\u0009");
        }
        else if (feature.getStrand() == NASeqFeature.REVERSE) {
          pw.print("-\u0009");
        }
        else {
          pw.print(".\u0009");
        }
        pw.print((feature.getAttribute("frame")));
        pw.print("\u0009");
        if (feature.getAttribute("note") != null) {
          pw.print(feature.getAttribute("note") + "\u0009");
        }
        pw.println();

      }

    }

  }


}
