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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.lang.reflect.*;

/**
 * parses <a href="http://www.ebi.ac.uk:80/imgt/hla/docs/manual.html" target="_top">EMBL</a>
 * "flat file" format data.
 * The Java Object used to model this data is an {@link AnnotatedSequence}.
 *
 * @author Joe Morris
 */
public class EMBLSequenceParser implements ContentParser {

  /**
   * Imports data from a URL.
   */
  public Object importContent(URL url) throws java.io.IOException{
    Object results = null;

      InputStream istream = url.openStream();
      results = importContent(istream);
      istream.close();
      return results;
  }

  /**
   * Imports data from a stream of bytes.
   */
  public Object importContent ( InputStream istream ) throws IOException  {
    AnnotatedSequence as = new AnnotatedSequence();
    StringBuffer descr = new StringBuffer();
    SequenceI seq = new Sequence();
    InputStreamReader in = new InputStreamReader( istream );
    StreamTokenizer tokenizer = new StreamTokenizer( in );
    tokenizer.eolIsSignificant ( true );
    tokenizer.wordChars( ' ', '~' );
    descr.append ( parseHeader (as,  tokenizer ) );
    parseFeatureTable ( as, tokenizer );
    descr.append ( parseResHeader (tokenizer) );
    as.setDescription (descr.toString() );
    seq.setResidues ( parseResidues ( tokenizer ) );
    as.setSequence ( seq );
    return as;

  }

  private String parseResHeader ( StreamTokenizer tokenizer ) throws IOException {
    StringBuffer sb = new StringBuffer();
    String n = tokenizer.sval;
    while ( n.startsWith ("XX") || n.startsWith( "SQ" ) ) {
      if ( n.startsWith ("SQ") ) sb.append ( n );
      tokenizer.nextToken(); //EOL
      tokenizer.nextToken();
      n= tokenizer.sval;
    }
    return sb.toString();
  }

  private String parseResidues ( StreamTokenizer tokenizer ) throws IOException  {
     StringBuffer sb = new StringBuffer();
     String s = tokenizer.sval;
     s = s.trim();
     while ( true ) {
       for ( int i = 0; i < 60 ; i+=11 ) {
         sb.append ( s.substring ( i, i + 10 ) );
       }
       tokenizer.nextToken(); //EOL
       tokenizer.nextToken();
       s = tokenizer.sval;
       s = s.trim();
       if ( s.startsWith ( "//") ) break;
     }

     return ( sb.toString() );
  }

  private String parseHeader ( AnnotatedSequence as, StreamTokenizer tokenizer ) throws IOException {
    StringBuffer sb = new StringBuffer();
    int token;

    while ( StreamTokenizer.TT_EOF != ( token = tokenizer.nextToken() ) ) {

      switch (token) {
      default:
      case StreamTokenizer.TT_EOL:
        sb.append ( "\n" );
        break;
      case StreamTokenizer.TT_WORD:
        if (  tokenizer.sval.startsWith ( "AC")   || tokenizer.sval.startsWith ( "SV" ) ||
            tokenizer.sval.startsWith ( "NI" ) )  parseAC ( as, tokenizer.sval );
        if ( tokenizer.sval.startsWith ( "ID" ) ) parseID ( as, tokenizer.sval );
        if ( tokenizer.sval.startsWith ( "FH" ) ) {
          tokenizer.nextToken(); //EOL
          tokenizer.nextToken();
          while ( tokenizer.sval.startsWith ( "FH" ) || tokenizer.ttype == StreamTokenizer.TT_EOL ) {
            tokenizer.nextToken(); //EOL
            tokenizer.nextToken();
          }
          return sb.toString();
        }
        sb.append ( tokenizer.sval );
        break;
      }
    }

    return sb.toString();
  }

  private void parseID ( AnnotatedSequence as, String id ) {
    id = id.substring ( 2 );
    id = id.trim();
    id = id.substring ( 0, id.indexOf(" ") );
    as.addIdentifier ( id );
  }

  private void parseAC ( AnnotatedSequence as, String id ) {
    String temp;
    id = id.substring ( 2 );
    id = id.trim();
    StringTokenizer s = new StringTokenizer ( id, ";", false );
    while ( s.hasMoreTokens() ) {
      temp = s.nextToken ();
      temp = temp.trim();
    }
  }

  private void parseFeatureTable (AnnotatedSequence as,  StreamTokenizer tokenizer ) throws IOException {
    int i = 0;
    System.err.println ("Parsing FeatureTable: ");
    while ( tokenizer.sval.startsWith ( "FT" ) || tokenizer.ttype == StreamTokenizer.TT_EOL ) {
      NASeqFeature f = parseFeature ( tokenizer );
      as.addFeature ( f );
      i++;
    }

    Enumeration e, p;
    Range r;
    NASeqFeature sf;
  }

  private NASeqFeature parseFeature ( StreamTokenizer tokenizer ) throws IOException  {

    String line, type, attribute;
    StringBuffer sb = new StringBuffer();
    NASeqFeature feature;
    int ellipsis_index;
    Integer i = new Integer(0);
    boolean more = true;

    line = tokenizer.sval;
    type = line.substring (5, 21);
    type = type.trim();
    if (type == "") System.err.println ( "parseFeature: no feature type." );
    feature = new NASeqFeature ( type );
    line = line.substring ( 21 );
    line = line.trim();

    //parse RANGES

    while ( ! line.startsWith ( "/" ) ) {
      sb.append ( line );
      tokenizer.nextToken();  //EOL
      tokenizer.nextToken();
      line = tokenizer.sval;
      line = line.substring ( 3 );
      line = line.trim();
    }
    parseRanges ( sb.toString(), feature );



    sb.setLength ( 0 );

    //parse ATTRIBUTES

    parseAttribSet ( tokenizer, feature );

    return feature;
  }


  private void parseAttribSet ( StreamTokenizer tokenizer, NASeqFeature feature ) throws IOException {
    StringBuffer sb = new StringBuffer ( );
    String line, name;
    while ( true ) {
      line = tokenizer.sval;
      if ( ! line.startsWith ( "FT" ) ) break;
      line = line.substring ( 21 );
      line = line.trim();
      if ( ! line.startsWith ( "/" )  ) break;
      sb.append ( line );
      tokenizer.nextToken(); //EOL
      tokenizer.nextToken();

      // inner while for  multi-line attribs
      while ( true ) {
        line = tokenizer.sval;
        if ( ! line.startsWith ( "FT" ) ) break;
        if ( line.charAt ( 5 ) != 32 ) break;     // 32 is a space: makes sure there is no Feature type tag
        line = line.substring ( 4 );
        int i = 0;
        while (true) {
          if (line.charAt ( i ) == 32) i++;
          else break;
        }
        line = line.substring ( i );                    //trims left-side whitespace
        if ( line.indexOf ("=") != -1 ) {
          String s = line.substring ( 0, line.indexOf ( "=" ) );
          if ( ! (s.equals("/translation") ) ) line = (line +   " ");
          // Trying to get spaces on lines that end in the middle of notes and stuff,
          // so words don't get smooshed together.  Doesn't work yet.
        }
        if ( line.startsWith ( "/" ) ) break;
        sb.append ( line );
        tokenizer.nextToken(); //EOL
        tokenizer.nextToken();
      }
      parseAttrib ( sb.toString(), feature );
      sb.setLength( 0 );
    }
  }

  private void parseAttrib ( String line, NASeqFeature feature ) {
    String name, value;
    int equalspos  = line.indexOf ( "=" );
    line = line.substring ( 1 ) ;
    name = line.substring ( 0, equalspos - 1 );
    value = line.substring ( equalspos  );
    if (value.startsWith ( "\"" ) ) {
      value = value.substring ( 1 );
      value = value.substring ( 0, value.length() - 1 );
    }
    feature.addAttribute ( name, value );

  }

  private void parseRanges ( String rangestr, NASeqFeature feature ) {
    StringTokenizer st = new StringTokenizer ( rangestr, "(,", false );
    String tok, temp;
    Range range;
    boolean forward = true;
    int ellipsis;
    if ( st.countTokens() == 1 ) {
      tok = st.nextToken();
      ellipsis = tok.indexOf ( ".");
      temp = tok.substring ( 0, ellipsis );
      range = new Range ( 0,0 );
      range.beg = Integer.parseInt ( temp );
      temp = tok.substring ( ellipsis + 2 );
      range.end = Integer.parseInt ( temp );
      feature.addPiece ( range );
      return;
    }
    while ( st.hasMoreElements() ) {
      tok = st.nextToken();
      if ( tok.equalsIgnoreCase ("join") ) {
        tok = st.nextToken ();
      }
      if ( tok.equalsIgnoreCase( "complement") ) {
        forward = false;
        tok = st.nextToken();
      }
      //Takes care of accession numbers in ranges, which *are* thrown out in this version;
      //they will not be re-exported.
      while ( ( tok.charAt ( 0 ) > 0x3A ) && st.hasMoreElements() ) {
        System.err.println ( "Threw out bad range: " + tok );
        tok = st.nextToken();
      }
      if ( tok.charAt ( 0 ) > 0x3A && ( ! st.hasMoreElements() ) ) {
        System.err.println ( "Threw out bad range: " + tok );
        break;
      }

      ellipsis = tok.indexOf ( ".");
      temp = tok.substring ( 0, ellipsis );
      range = new Range ( 0,0 );
      range.beg = Integer.parseInt ( temp );
      temp = tok.substring ( ellipsis + 2 );
      while ( temp.indexOf (")") != -1 ) temp = temp.substring ( 0, temp.length() - 1 );
      range.end = Integer.parseInt ( temp );
      feature.addPiece ( range );
      if ( ! st.hasMoreElements() ) break;
    }
    if (forward) feature.setStrand ( NASeqFeature.FORWARD );
    else feature.setStrand ( NASeqFeature.REVERSE );

    Enumeration p;
    Range r;

    p = feature.pieces();
    while ( p.hasMoreElements() ) {
      r = (Range) p.nextElement() ;
    }


  }


  private void dump(AnnotatedSequence as) {
    Enumeration e, p;
    NASeqFeature sf;
    Range r;
    System.out.println ( "Beginning dump of AnnotatedSequence data:");
    e = as.features();
    while (e.hasMoreElements() ) {
      sf = (NASeqFeature) e.nextElement();
      p = sf.pieces();
      while ( p.hasMoreElements() ) {
        r = (Range) p.nextElement() ;
        System.out.println ( " RANGE: " + r.beg + ".." + r.end );
      }
    }
    System.out.println ( "XX");
    System.out.println ( as.getSequence() );

  }

  /**
   * Exports content to an EMBL flat file.
   * No support for hypertext linking at this time.
   * @param theOutput where the data is going.
   * @param o which should be an AnnotatedSequence
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
    String s;
    PrintWriter pw = new PrintWriter(theOutput, true);
    s = theSeq.getDescription();
    s= s.substring ( 0, s.indexOf ("SQ " ) -1 );
    pw.println ( s );
    pw.println ( "FH   Key             Location/Qualifiers \nFH" );
    exportFeatureTable ( pw, theSeq );
  }

  private void exportFeatureTable ( PrintWriter pw, AnnotatedSequence theSeq ) {

    Enumeration e = theSeq.features();
    Enumeration ranges;
    NASeqFeature feature;
    String type;
    StringBuffer sb = new StringBuffer();
    Range range = new Range(0,0);
    int numberOfElements = 0;
    while ( e.hasMoreElements() ) {
      feature = (NASeqFeature) e.nextElement();
      sb.append ( "FT   " );
      type = feature.getType();
      sb.append ( type );
      for ( int i = 0; i < ( 16 - type.length() ); i++ ) sb.append ( " " );
      ranges = feature.pieces();

      while ( ranges.hasMoreElements() ) {
        numberOfElements++;
        ranges.nextElement();
      }
      ranges = feature.pieces();

      if (numberOfElements > 1) {
        sb.append("join(");
        while ( ranges.hasMoreElements() ) {
          range = (Range) ranges.nextElement();
          if ( feature.getStrand() == feature.REVERSE ) sb.append( "complement(" );
          sb.append( range.beg ).append( ".." ).append( range.end );
          if ( feature.getStrand() == feature.REVERSE ) sb.append ( ")" );
          if ( ranges.hasMoreElements() ) sb.append (",");
        }
        sb.append( ")" );
      }
      else {
        range = (Range)ranges.nextElement();
        sb.append( range.beg ).append ( ".." ).append( range.end );
      }

      formatFTLine ( pw, sb.toString() );
      sb.setLength( 0 );
      Enumeration attribs = feature.attributes();
      String attrib_name;
      String attrib_val;
      Object o;
      Vector vect;
      while ( attribs.hasMoreElements() ) {
        attrib_name = (String) attribs.nextElement() ;
        if ( attrib_name.equalsIgnoreCase ( "translation" ) ) {
          sb.append( "/" ).append(attrib_name).append("=\"");
          sb.append( feature.getAttribute ( attrib_name ) ).append ("\"");
          formatTrans ( pw, sb.toString() );
          sb.setLength ( 0 );
        }
        else {
          o = feature.getAttribute ( attrib_name );
          if ( o instanceof String ) {
            formatAttrib ( pw, attrib_name, (String) feature.getAttribute ( attrib_name ));
          }
          else if ( o instanceof Vector ) {
            vect=  (Vector) o;
            for ( int i = 0; i < vect.size(); i++ ) {
              formatAttrib ( pw, attrib_name, (String) vect.elementAt ( i ) );
            }
          }
        }
      }
    }
  }

  private void formatAttrib ( PrintWriter pw, String name, String value ) {
    StringBuffer sb = new StringBuffer ();
    sb.append( "FT                   " ).append("/").append( name);
    sb.append( "=\"");
    sb.append( value ).append ( "\"");
    formatFTLine ( pw, sb.toString() );
  }

  private void formatTrans ( PrintWriter pw, String line ) {

    line = ( "FT                   " + line );
    if ( line.length() < 80 ) {
      pw.println ( line ) ;
      return;
    }
    else {
      while ( line.length() > 80 ) {
        pw.println ( line.substring ( 0, 80 ) );
        line =  ( "FT                   " + line.substring ( 80 ) );
      }
      pw.println ( line );
    }
  }

  private void formatFTLine ( PrintWriter pw, String rangestr ) {
    StringTokenizer st  = new StringTokenizer ( rangestr, ", ", true );
    StringBuffer sb = new StringBuffer ();
    String token;

    if ( st.countTokens() == 1 )  {
     pw.println ( st.nextToken() );
     return;
    }

    while ( st.hasMoreTokens() ) {
      token = st.nextToken() ;
      if ( ( sb.length() + token.length() ) < 80  ) {
        sb.append ( token );
        if ( st.hasMoreTokens() ) sb.append ( st.nextToken() ); //delimiter
      }

      else {
        pw.println ( sb.toString() );
        sb.setLength ( 0 );
        sb.append ( "FT                   " );
        sb.append ( token );
        if ( st.hasMoreTokens() ) sb.append ( st.nextToken() ); //delimiter
      }

    }
    if ( sb.length() > 22 ) pw.println ( sb.toString() );
  }


}
