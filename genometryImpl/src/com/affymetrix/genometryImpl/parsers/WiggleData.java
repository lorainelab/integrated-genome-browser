/**
 *   Copyright (c) 2006-2007 Affymetrix, Inc.
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

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.GraphSymFloat;
import com.affymetrix.genometryImpl.util.FloatList;
import com.affymetrix.genometryImpl.util.IntList;
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
  Map<String,String> track_line_map; // = track_line_parser.getCurrentTrackHash();
  AnnotatedSeqGroup seq_group;

  /**
   *  Creates a GraphSym from the stored data, or returns null if no data
   *  has been stored yet.
   */
  public abstract GraphSymFloat createGraph(String graph_id);

  void sortData(int graph_length, int[] xcoords, int[] wcoords, float ycoords[]) {

    List<Point3D> points = new ArrayList<Point3D>(graph_length);
    if (wcoords != null) {
      for (int i=0; i<graph_length; i++) {
        Point3D pnt = new Point3D(xcoords[i], wcoords[i], ycoords[i]);
        points.add(pnt);
      }
    } else {
      for (int i=0; i<graph_length; i++) {
        Point3D pnt = new Point3D(xcoords[i], 1, ycoords[i]);
        points.add(pnt);
      }
    }
    PointComp pointcomp = new PointComp();
    Collections.sort(points, pointcomp);

    for (int i=0; i<graph_length; i++) {
      Point3D pnt = points.get(i);
      xcoords[i] = pnt.x;
      ycoords[i] = pnt.y;
      if (wlist != null) {
        wcoords[i] = pnt.w;
      }
    }
  }

  class Point3D {
    float y;
    int x, w;
    public Point3D(int x, int w, float y) {
      this.x = x; this.y =y; this.w = w;
    }
  }

  class PointComp implements Comparator<Point3D> {
    public int compare(Point3D p1, Point3D p2) {
      if (p1.x < p2.x) {
        return -1;
      }
      else if (p1.x == p2.x) {
        return 0;
      }
      else {
        return +1;
      }
    }
  }
}

class FixedStepWiggleData extends WiggleData {
  String format_line;
  public FixedStepWiggleData(Map<String,String> m, String format_line, AnnotatedSeqGroup group) {
    super();
    track_line_map = new HashMap<String,String>(m);
    this.format_line = format_line;
    this.seq_group = group;
    xlist = null;
    wlist = null;
    ylist = new FloatList();
  }

  public GraphSymFloat createGraph(String graph_id) {
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
    GraphSymFloat gsym = null;
    // fixed-step data never needs sorting
    if (x_span == 0 || x_span == 1) {
      gsym = new GraphSymFloat(x_vals, ylist.copyToArray(), graph_id, seq);
    } else {
      gsym = new GraphIntervalSym(x_vals, w_vals, ylist.copyToArray(), graph_id, seq);
    }
    return gsym;
  }

}

class VariableStepWiggleData extends WiggleData {
  String format_line;
  public VariableStepWiggleData(Map<String,String> m, String format_line, AnnotatedSeqGroup group) {
    super();
    track_line_map = new HashMap<String,String>(m);
    this.format_line = format_line;
    this.seq_group = group;
    xlist = new IntList();
    wlist = null;
    ylist = new FloatList();
  }

  public GraphSymFloat createGraph(String graph_id) {
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
    GraphSymFloat gsym = null;
    sortData(xlist.size(), xlist.getInternalArray(), widths, ylist.getInternalArray());
    if (widths == null) {
      gsym = new GraphSymFloat(xlist.copyToArray(), ylist.copyToArray(), graph_id, seq);
    } else {
      gsym = new GraphIntervalSym(xlist.copyToArray(), widths, ylist.copyToArray(), graph_id, seq);
    }

    return gsym;
  }

}

class BedWiggleData extends WiggleData {
  String seq_id;

  public BedWiggleData(Map<String,String> m, AnnotatedSeqGroup group, String seq_id) {
    super();
    track_line_map = new HashMap<String,String>(m);
    this.seq_id = seq_id;
    this.seq_group = group;
    xlist = new IntList();
    wlist = new IntList();
    ylist = new FloatList();
  }

  public GraphSymFloat createGraph(String graph_id) {
    if (xlist.size() == 0) {
      return null;
    }

    int largest_x = xlist.get(xlist.size()-1) + wlist.get(wlist.size()-1);

    BioSeq seq = seq_group.addSeq(seq_id, largest_x);

    sortData(xlist.size(), xlist.getInternalArray(), wlist.getInternalArray(), ylist.getInternalArray());
    GraphSymFloat gsym = new GraphIntervalSym(xlist.copyToArray(), wlist.copyToArray(), ylist.copyToArray(), graph_id, seq);

    return gsym;
  }
}
