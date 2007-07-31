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
package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.GraphSym;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSymFloat;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphStateI;
import java.awt.Color;

/**
 *  A parser for graph data in the UCSC browser Wiggle format.
 *  See http://genome.ucsc.edu/google/goldenPath/help/wiggle.html
 *  There are three sub-formats: BED4, VARSTEP, and FIXEDSTEP.
 *  This parser reads the "Track" lines and applies some properties from
 *  them, but ignores properties that don't easily apply to IGB.
 */
public class WiggleParser {
    
  public static final int BED4 = 1;
  public static final int VARSTEP = 2;
  public static final int FIXEDSTEP = 3;
  
  static Pattern field_regex = Pattern.compile("\\s+");  // one or more whitespace
  
  TrackLineParser track_line_parser;
  boolean ensure_unique_id = true;
  
  public WiggleParser() {
    track_line_parser = new TrackLineParser();
  }
  
  /**
   *  Reads a Wiggle-formatted file using any combination of the three formats
   *  {@link #BED4}, {@link #VARSTEP}, {@link #FIXEDSTEP}.
   *  The format must be specified on the first line following a track line,
   *  otherwise BED4 is assumed.
   */
  public List parse(InputStream istr, AnnotatedSeqGroup seq_group, boolean annotate_seq,
      String stream_name) throws IOException {
    
    int current_format = BED4;
    List grafs = new ArrayList();
    WiggleData current_data = null;
    Map current_datamap = null; // Map: seq_id -> WiggleData
    boolean previous_line_was_track_line = false;
    
    BufferedReader br = new BufferedReader(new InputStreamReader(istr));
    String line;
    Map graph_props_map = new LinkedHashMap();
    
    while ((line = br.readLine()) != null && ! Thread.currentThread().isInterrupted()) {
      // Generally should be "track" line, followed by optional "format" line
      // (If there is no format line, BED4 format is assumed.)
      
      if (line.length() == 0) { continue; }
//TODO:
//      else if (line.startsWith(Bookmark.IGB_GRAPHS_PRAGMA)) {
//        try {
//          Bookmark.parseIGBGraphsPragma(graph_props_map, line, false);
//        } catch (Exception e) {
//          throw new IOException("Couldn't parse IGB-graphs pragma");
//        }
//      }
      else if (line.startsWith("#")) { continue; } 
      else if (line.startsWith("%")) { continue; } 
      else if (line.startsWith("browser")) { continue; } 
      else if (line.startsWith("track")) {
        grafs.addAll(finishLine(seq_group, current_datamap, stream_name));
        // finish previous graph(s) using previous track properties
        
        track_line_parser.parseTrackLine(line);
                
        current_format = BED4; // unless there is a format line next, assume BED4
        current_data = null;
        current_datamap = new HashMap(); // Map: seq_id -> WiggleData
        previous_line_was_track_line = true;
        continue;
      } else if (line.startsWith("variableStep")) {
        if (! previous_line_was_track_line) {
          throw new IOException("Wiggle format error: 'variableStep' line is not preceded by a 'track' line");
        }
        current_format = VARSTEP;
        current_data = new VariableStepWiggleData(track_line_parser.getCurrentTrackHash(), line, seq_group);
        current_datamap.put("", current_data);
      } else if (line.startsWith("fixedStep")) {
        if (! previous_line_was_track_line) {
          throw new IOException("Wiggle format error: 'fixedStep' line is not preceded by a 'track' line");
        }
        current_format = FIXEDSTEP;
        current_data = new FixedStepWiggleData(track_line_parser.getCurrentTrackHash(), line, seq_group);
        current_datamap.put("", current_data);
      }
      
      // Else, it is a data line
      else {
        String[] fields = field_regex.split(line.trim()); // trim() because lines are allowed to start with whitespace
        
        
        if (current_format == VARSTEP) {
          
          current_data.xlist.add(Integer.parseInt(fields[0]));
          current_data.ylist.add(Float.parseFloat(fields[1]));
          
        } else if (current_format == FIXEDSTEP) {
          
          current_data.ylist.add(Float.parseFloat(fields[0]));
          
        } else if (current_format == BED4) {
          // chrom  start end value
          String seq_id = fields[0];
          
          current_data = (WiggleData) current_datamap.get(seq_id);
          if (current_data == null) {
            current_data = new BedWiggleData(track_line_parser.getCurrentTrackHash(), seq_group, seq_id);
            current_datamap.put(seq_id, current_data);
          }
          
          int x1 = Integer.parseInt(fields[1]);
          int x2 = Integer.parseInt(fields[2]);
          int start = Math.min(x1, x2);
          int width = Math.max(x1, x2) - start;
          
          current_data.xlist.add(x1);
          current_data.wlist.add(width);
          current_data.ylist.add(Float.parseFloat(fields[3]));
          
        } else {  // cannot happen
          throw new RuntimeException("Format undefined");
        }
      }
      previous_line_was_track_line = false;
    }
    
    grafs.addAll(finishLine(seq_group, current_datamap, stream_name));
    
    if (annotate_seq) {
      Iterator giter = grafs.iterator();
      while (giter.hasNext()) {
        GraphSym graf = (GraphSym)giter.next();
        MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq)graf.getGraphSeq();
        seq.addAnnotation(graf);
      }
    }

//    BookmarkPropertyParser.applyGraphProperties(grafs, graph_props_map);
    
    return grafs;
  }
  
  static String parseFormatLine(String name, String format_line, String default_val) {
    String val = default_val;
    String[] fields = field_regex.split(format_line);
    for (int i=1; i<fields.length; i++) {
      if (fields[i].startsWith(name+"=")) {
        val = fields[i].substring(name.length()+1);
      }
    }
    return val;
  }


  /** 
   * Finishes the current data section and creates a list of GraphSym objects.
   * (For the BED4 format, there can be multiple graphs in the list, for the 
   *  other formats there will only be one.)
   */
  List finishLine(AnnotatedSeqGroup seq_group, Map m, String stream_name) {
    if (m == null) {
      return Collections.EMPTY_LIST;
    }

    List grafs = new ArrayList(m.size());
    
    Map track_hash = track_line_parser.getCurrentTrackHash();
    String graph_id = (String) track_hash.get(TrackLineParser.NAME);
    if (graph_id == null) {
      graph_id = stream_name;
    }
    if (ensure_unique_id) {
      graph_id = AnnotatedSeqGroup.getUniqueGraphID(graph_id, seq_group);
    }
    track_hash.put(TrackLineParser.NAME, graph_id);
    
    GraphStateI gstate = seq_group.getStateProvider().getGraphState(graph_id);
    track_line_parser.applyTrackProperties(track_hash, gstate);
    
    Iterator iter = m.keySet().iterator();
    while (iter.hasNext()) {
      String seq_id = (String) iter.next();
      WiggleData wig = (WiggleData) m.get(seq_id);
      GraphSymFloat gsym = wig.createGraph(graph_id);
    
      if (gsym != null) {
        grafs.add(gsym);
      }
    }

    return grafs;
  }
    
  
  /**
   *  Writes out one particular wiggle format. Specifically:
   *     variableStep two-column data; started by a declaration line and followed with
   *       chromosome positions and data values:
   *<pre>
   *   variableStep  chrom=chrN  [span=windowSize]
   *   chromStartA  dataValueA
   *   chromStartB  dataValueB
   *</pre>
   *  @param graphs  a Collection of GraphSym objects.  (They do not have to be GraphSymFloat objects.)
   */
  public static boolean writeVariableStep(java.util.Collection graphs, OutputStream outstream) {
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
        //float[] ycoords = (float[]) graf.getGraphYCoords();
        for (int i=0; i<pcount; i++) {
          bw.write(Integer.toString(xcoords[i]));
          bw.write("\t");
          bw.write(graf.getGraphYCoordString(i));
          bw.write("\n");
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return true;
  }

  /** Writes the given GraphIntervalSym in wiggle-BED format.  
   *  Also writes a track line as a header. 
   */
  public static boolean writeBedFormat(GraphIntervalSym graf, String genome_version, OutputStream outstream) throws IOException {    
    int xpos[] = graf.getGraphXCoords();
    int widths[] = graf.getGraphWidthCoords();
    //float ypos[] = (float[]) graf.getGraphYCoords();

    OutputStreamWriter osw = null;
    BufferedWriter bw = null;

    try {
      osw = new OutputStreamWriter(outstream);
      bw = new BufferedWriter(osw);

      BioSeq seq = graf.getGraphSeq();
      String seq_id = (seq == null ? "." : seq.getID());
      String human_name = graf.getGraphState().getTierStyle().getHumanName();
      String gname = graf.getGraphName();
      GraphStateI state = graf.getGraphState();
      Color color = state.getTierStyle().getColor();

      if (genome_version != null) {
        bw.write("# genome_version = " + genome_version + '\n');
      }
      bw.write("track type=wiggle_0 name=\"" + gname + "\"");
      bw.write(" description=\""+human_name+"\"");
      bw.write(" visibility=full");
      bw.write(" color=" + color.getRed() + ","+color.getGreen()+","+color.getBlue());
      bw.write(" viewLimits="+Float.toString(state.getVisibleMinY())+":"+Float.toString(state.getVisibleMaxY()));
      bw.write("");
      bw.write('\n');
      
      for (int i=0; i<xpos.length; i++) {
        int x2 = xpos[i] + widths[i];
        bw.write(seq_id + ' ' + xpos[i] + ' ' +  x2  + ' ' + graf.getGraphYCoord(i) + '\n');
      }
      bw.flush();
    } finally {
      bw.close();
      osw.close();
    }
    return true;
  }

}
