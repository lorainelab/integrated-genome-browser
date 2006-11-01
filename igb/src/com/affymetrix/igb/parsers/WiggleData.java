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

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.GraphIntervalSym;
import com.affymetrix.igb.genometry.GraphSym;
import com.affymetrix.igb.util.FloatList;
import com.affymetrix.igb.util.IntList;
import java.util.*;

/**
 *  A class used to temporarily hold data during processing of Wiggle-format files.
 */
public abstract class WiggleData {
  public IntList xlist;
  public IntList wlist;
  public FloatList ylist;
  ArrayList triplets;
  String track_line;
  Map track_line_map; // = track_line_parser.getCurrentTrackHash();
  AnnotatedSeqGroup seq_group;
  
  /**
   *  Creates a GraphSym from the stored data, or returns null if no data
   *  has been stored yet.
   */
  public abstract GraphSym createGraph(String graph_id);
}

class FixedStepWiggleData extends WiggleData {
  String format_line;
  public FixedStepWiggleData(Map m, String format_line, AnnotatedSeqGroup group) {
    super();
    track_line_map = new HashMap(m);
    this.format_line = format_line;
    this.seq_group = group;
    xlist = null;
    wlist = null;
    ylist = new FloatList();
  }
  
  public GraphSym createGraph(String graph_id) {
    if (ylist.size() == 0) {
      return null;
    }
    String seq_id = WiggleParser.parseFormatLine("chrom", format_line, "unknown");
    int x_start = Integer.parseInt(WiggleParser.parseFormatLine("start", format_line, "1"));
    int x_step = Integer.parseInt(WiggleParser.parseFormatLine("step", format_line, "1"));
    int x_span = Integer.parseInt(WiggleParser.parseFormatLine("span", format_line, "0"));
    
    int[] x_vals = new int[ylist.size()];
    int[] w_vals = new int[ylist.size()];
    
    // fill in x values
    int x = x_start;
    for (int i=0; i<x_vals.length; i++) {
      x_vals[i] = x;
      w_vals[i] = x_span;
      x += x_step;
    }
    
    BioSeq seq = seq_group.addSeq(seq_id, x_vals[x_vals.length-1] + x_span);
    GraphSym gsym = null;
    if (x_span == 0) {
      gsym = new GraphSym(x_vals, ylist.copyToArray(), graph_id, seq);
    } else {
      gsym = new GraphIntervalSym(x_vals, w_vals, ylist.copyToArray(), graph_id, seq);
    }
    return gsym;
  }
  
}

class VariableStepWiggleData extends WiggleData {
  String format_line;
  public VariableStepWiggleData(Map m, String format_line, AnnotatedSeqGroup group) {
    super();
    track_line_map = new HashMap(m);
    this.format_line = format_line;
    this.seq_group = group;
    xlist = new IntList();
    wlist = null;
    ylist = new FloatList();
  }
  
  public GraphSym createGraph(String graph_id) {
    if (xlist.size() == 0) {
      return null;
    }
    int[] widths = null;
    
    String seq_id = WiggleParser.parseFormatLine("chrom", format_line, "unknown");
    String span_str = WiggleParser.parseFormatLine("span", format_line, "0");
    
    int span = 0;
    if (! "0".equals(span_str) && ! "1".equals(span_str)) {
      span = Integer.parseInt(span_str);
      widths = new int[xlist.size()];
      Arrays.fill(widths, span);
    }
    
    int largest_x = xlist.get(xlist.size()-1) + span;
    BioSeq seq = seq_group.addSeq(seq_id, largest_x);
    GraphSym gsym = null;
    if (widths == null) {
      gsym = new GraphSym(xlist.copyToArray(), ylist.copyToArray(), graph_id, seq);
    } else {
      gsym = new GraphIntervalSym(xlist.copyToArray(), widths, ylist.copyToArray(), graph_id, seq);
    }
    
    return gsym;
  }
  
}

class BedWiggleData extends WiggleData {
  String seq_id;
  
  public BedWiggleData(Map m, AnnotatedSeqGroup group, String seq_id) {
    super();
    track_line_map = new HashMap(m);
    this.seq_id = seq_id;
    this.seq_group = group;
    xlist = new IntList();
    wlist = new IntList();
    ylist = new FloatList();
  }
  
  public GraphSym createGraph(String graph_id) {
    if (xlist.size() == 0) {
      return null;
    }
    
    int largest_x = xlist.get(xlist.size()-1) + wlist.get(wlist.size()-1);
    
    BioSeq seq = seq_group.addSeq(seq_id, largest_x);
    GraphSym gsym = new GraphIntervalSym(xlist.copyToArray(), wlist.copyToArray(), ylist.copyToArray(), graph_id, seq);
    
    return gsym;
  }
}