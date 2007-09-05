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
import com.affymetrix.genoviz.parser.GFFParser;

import java.io.*;
import java.util.*;

/**
 * parses GFF data.
 * We use a regular (slow) {@link GFFParser} to parse all but the residues.
 * When there are many residues the regular GFFParser is too slow.
 * The residues are parsed here.
 *
 * @author Eric Blossom
 */
public class ManyResiduesGFFParser implements ContentParser {

  static class Parser extends Thread {

    InputStream reader;
    GFFParser gffParser = new GFFParser();
    Object product = null;

    public Parser( InputStream r ) {
      this.reader = r;
    }

    public void run() {
      try {
        this.product = gffParser.importContent( this.reader );
        if ( null == this.product ) {
          //System.err.println( "null?" );
        }
      }
      catch ( IOException e ) {
        System.err.println( e.getMessage() );
      }
    }

    public Object getProduct() {
      return this.product;
    }

  }

  public Object importContent( InputStream theInput ) throws IOException {
    Object o = null;
    InputStreamReader in = new InputStreamReader( theInput );
    BufferedReader r = new BufferedReader( in );
    PipedInputStream is = new PipedInputStream();
    OutputStream os = new PipedOutputStream( is );
    Parser p = new Parser( is );
    p.start();
    Hashtable<String,SequenceI> seqs = readByChar( os, r );
    try {
      p.join();
      o = p.getProduct();
      o = addSeqs( o, seqs );
      Vector v = ( Vector ) o;
      if ( 1 == v.size() ) {
        o = v.elementAt( 0 );
      }
    }
    catch ( InterruptedException e ) {
      System.err.println( e );
    }
    return o;
  }

  public void exportContent( OutputStream s, Object o ) throws IOException {
    ContentParser p = new GFFParser();
    p.exportContent( s, o );
  }

  /**
   * reads through the input, keeping the residues
   * and passing everything else on to the output stream.
   */
  private Hashtable<String,SequenceI> readByChar( OutputStream os, BufferedReader is ) throws IOException {

    Hashtable<String,SequenceI> seqs = new Hashtable<String,SequenceI>();
    SequenceI seq = null;

    String s = is.readLine();
    byte[] b;
    while ( null != s ) {
      if ( s.startsWith( "##DNA" ) ) { // Residues start after "##DNA" line.
        seq = parseResidues( s, is );
        String n = seq.getID();
        if ( null == n ) {
          n = "";
        }
        seqs.put( n, seq );
      }
      else { // Pass the rest on to the tokenizing parser.
        b = s.getBytes();
        os.write( b );
        os.write( '\n' );
      }
      s = is.readLine();
    }
    os.close();
    return seqs;

  }

  // Get the residues.
  private SequenceI parseResidues( String header, BufferedReader is ) throws IOException {

    StringBuffer sb = new StringBuffer();
    int c = is.read();
    while ( -1 != c && 'e' != c ) { // Residues end before "##end-DNA" line.
      switch ( c ) {
        case '\n':
        case '#':
          break;
        default:
          sb.append( ( char ) c );
          break;
        }
      c = is.read();
    }
    is.readLine(); // consume the rest of the ##end-DNA line.
    SequenceI s = new NASequence();
    s.setResidues( sb.toString() );
    StringTokenizer st = new StringTokenizer( header );
    if ( 1 < st.countTokens() ) {
      st.nextToken(); // get past ##DNA
      String seqname = st.nextToken();
      s.setID( seqname );
    }
    return s;

  }

  @SuppressWarnings("unchecked")
  private Vector<AnnotatedSequence> addSeqs( Object o, Hashtable<String,SequenceI> theSeqs ) {
    Vector<AnnotatedSequence> v;
    if ( null == o ) {
      v = new Vector<AnnotatedSequence>();
    }
    else if ( o instanceof Vector ) {
      v = ( Vector<AnnotatedSequence> ) o;
    }
    else if ( o instanceof AnnotatedSequence ) {
      v = new Vector<AnnotatedSequence>();
      v.addElement( (AnnotatedSequence) o );
    }
    else {
      throw new IllegalArgumentException(
        "Need an AnnotatedSequence or a Vector of them." );
    }
    return addSeqs( v, theSeqs );
  }

  private Vector<AnnotatedSequence> addSeqs( Vector<AnnotatedSequence> v, Hashtable<String,SequenceI> theSeqs ) {
    if ( null == theSeqs || theSeqs.isEmpty() ) {
      return v;
    }
    // Go through the vector adding residues.
    Vector<String> done = new Vector<String>();
    Enumeration enu = v.elements();
    while ( enu.hasMoreElements() ) {
      Object o = enu.nextElement();
      // Set the matching SequenceI.
      AnnotatedSequence aseq = ( AnnotatedSequence ) o;
      Enumeration e2 = aseq.identifiers();
      while ( e2.hasMoreElements() ) {
        Object o2 = e2.nextElement();
        String sname = o2.toString();
        o = theSeqs.get( sname );
        if ( null != o ) {
          SequenceI s = ( SequenceI ) o;
          aseq.setSequence( s );
          done.addElement( sname );
          break;
        }
      }
    }
    // Remove the SequenceI's used above from theSeqs.
    // Note that with Java 1.2 collections,
    // we will be able to use an iterator
    // and do the removing in the above loop.
    enu = done.elements();
    while ( enu.hasMoreElements() ) {
      String sname = ( String ) enu.nextElement();
      theSeqs.remove( sname );
    }
    // Create new AnnotatedSequences for those still left.
    enu = theSeqs.elements();
    while ( enu.hasMoreElements() ) {
      Object o = enu.nextElement();
      SequenceI s = ( SequenceI ) o;
      AnnotatedSequence aseq = new AnnotatedSequence();
      aseq.setSequence( s );
      v.addElement( aseq );
    }
    return v;
  }

}
