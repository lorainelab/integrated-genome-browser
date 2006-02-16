/**
*   Copyright (c) 2006 Affymetrix, Inc.
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
package com.affymetrix.igb.parsers;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.util.GraphSymUtils;
import com.affymetrix.igb.util.IntList;
import com.affymetrix.igb.util.FloatList;

public class WiggleParser extends TrackLineParser {

  /**
   *  wiggle subformats 
   *    BED4
   *    VARSTEP
   *    FIXEDSTEP
   */
  static int UNKNOWN = 0;
  static int BED4 = 1;
  static int VARSTEP = 2;
  static int FIXEDSTEP = 3;

  static Pattern field_regex = Pattern.compile("\\s+");  // one or more whitespace

  /**
   * NOR YET IMPLEMENTED.
   *  Currently only reading one particular wiggle format:
   *     variableStep two-column data; started by a declaration line and followed with
   *       chromosome positions and data values:
   *
   *   variableStep  chrom=chrN  [span=windowSize}
   *   chromStartA  dataValueA
   *   chromStartB  dataValueB
   */
  public List parse(InputStream istr, AnnotatedSeqGroup seq_group, boolean annotate_seq, 
    String stream_name) throws IOException {
    
    List grafs = new ArrayList();
    int current_format = UNKNOWN;
    IntList xlist = null;
    FloatList ylist = null;
    String graph_name = null;
    String seqid = null;
    Map graph_props = null;
      BufferedReader br = new BufferedReader(new InputStreamReader(istr));
      String line;
      while ((line = br.readLine()) != null) {
	if (line.startsWith("#")) { continue; }
	else if (line.startsWith("%")) { continue; }
	else if (line.startsWith("track")) { 
	  setTrackProperties(line);
	  continue;
	}
	else if (line.startsWith("variableStep")) {
	  if (xlist != null && ylist != null) {
	    grafs.add(createGraph(seq_group, graph_name, graph_props, seqid, xlist, ylist));
	  }
	  String[] fields = field_regex.split(line);
	  for (int i=1; i<fields.length; i++) {
	    if (fields[i].startsWith("chrom=")) {
	      seqid = fields[i].substring(6);
	      System.out.println("current seqid = " + seqid);
	    }
	  }
	  current_format = VARSTEP;
	  xlist = new IntList();
	  ylist = new FloatList();
	}
	else {
	  String[] fields = field_regex.split(line);
	}
      }
    
    if (annotate_seq) {
      Iterator giter = grafs.iterator();
      while (giter.hasNext()) {
	GraphSym graf = (GraphSym)giter.next();
	MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq)graf.getGraphSeq();
	seq.addAnnotation(graf);
      }
    }
    return grafs;
  }


  protected static GraphSym createGraph(AnnotatedSeqGroup seq_group, String gname, Map gprops, String seqid, 
					IntList xlist, FloatList ylist) {
    BioSeq seq = seq_group.getSeq(seqid);
    GraphSym gsym = new GraphSym(xlist.copyToArray(), ylist.copyToArray(), gname, seq);
    // add props ??? NOT YET IMPLEMENTED
    return gsym;
  }

  /**
   *  Currently only writing out one particular wiggle format. Specifically:
   *     variableStep two-column data; started by a declaration line and followed with
   *       chromosome positions and data values:
   *<pre>
   *   variableStep  chrom=chrN  [span=windowSize]
   *   chromStartA  dataValueA
   *   chromStartB  dataValueB
   *</pre>
   */
  public static boolean writeGraphs(java.util.Collection graphs, OutputStream outstream) {
    try {
      OutputStreamWriter osw = new OutputStreamWriter(outstream);
      BufferedWriter bw = new BufferedWriter(osw);
      Iterator iter = graphs.iterator();
      while (iter.hasNext()) {
	GraphSym graf = (GraphSym)iter.next();
	String seqid = graf.getGraphSeq().getID();
	String gname = graf.getGraphName();
	bw.write("track type=wiggle_0 name=\"" + gname + "\"\n");
	bw.write("variableStep\tchrom=" + seqid + "\n");
	int pcount = graf.getPointCount();
	int[] xcoords = graf.getGraphXCoords();
	float[] ycoords = graf.getGraphYCoords();
	for (int i=0; i<pcount; i++) {
	  bw.write(Integer.toString(xcoords[i]));
	  bw.write("\t");
	  bw.write(Float.toString(ycoords[i]));
	  bw.write("\n");
	}
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    return true;
  }

  public static void main(String[] args) {
    if (args.length < 2) { 
      System.err.println("Usage: WiggleParser in_file out_file [seqid]"); 
      System.err.println("     (seqid is required if and only if input format is .gr)");
      System.exit(1);
    }
    String in_file = args[0];
    String out_file = args[1];
    // read in_file using GraphSymUtils.readGraphs() ?  need to modify to handle gr (like readGraph())
    AnnotatedSeqGroup seq_group = SingletonGenometryModel.getGenometryModel().addSeqGroup("Test Seq Group");
    
    try {
      InputStream istr = new FileInputStream(new File(in_file));
      List gsyms = new ArrayList();
      if (in_file.endsWith(".gr")) {
	// NOT YET IMPLEMENTED
	System.err.println("     Conversion of .gr files not yet implemented");
      }
      else {
	gsyms = GraphSymUtils.readGraphs(istr, in_file, seq_group);
      }
      // write out_file using WiggleParser.writeGraphs();
      System.out.println("writing out graphs in wiggle format: " + out_file);
      OutputStream ostr = new FileOutputStream(new File(out_file));
      WiggleParser.writeGraphs(gsyms, ostr);
      ostr.close();
      System.out.println("done writing out graphs");
    }
    catch (Exception ex) { ex.printStackTrace(); }
  }

}
