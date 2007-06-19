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
import com.affymetrix.igb.util.IntList;
import com.affymetrix.igb.util.FloatList;
import com.affymetrix.igb.util.PointIntFloat;

public class SgrParser {
  static boolean DEBUG = false;
  static Comparator pointcomp = PointIntFloat.getComparator(true, true);
  static Pattern line_regex = Pattern.compile("\\s+");  // replaced single tab with one or more whitespace

  public List parse(InputStream istr, String stream_name, AnnotatedSeqGroup seq_group,
                    boolean annotate_seq)
        throws IOException {
      return parse(istr, stream_name, seq_group, annotate_seq, true);
    }

  public List parse(InputStream istr, String stream_name, AnnotatedSeqGroup seq_group,
                    boolean annotate_seq, boolean ensure_unique_id)
      throws IOException {
    System.out.println("Parsing with SgrParser: " + stream_name);
    
    ArrayList results = new ArrayList();
    
    try {
    InputStreamReader isr = new InputStreamReader(istr);
    BufferedReader br = new BufferedReader(isr);

    String line;
    Map xhash = new HashMap();
    Map yhash = new HashMap();

    String gid = stream_name;
    if (ensure_unique_id)  {
      // Making sure the ID is unique on the whole genome, not just this seq
      // will make sure the GraphState is also unique on the whole genome.
      gid = AnnotatedSeqGroup.getUniqueGraphID(gid, seq_group);
    }
    
    while ((line = br.readLine()) != null) {
      if (line.startsWith("#")) { continue; }
      if (line.startsWith("%")) { continue; }
      String[] fields = line_regex.split(line);
      String seqid = fields[0];
      IntList xlist = (IntList)xhash.get(seqid);
      if (xlist == null) {
	xlist = new IntList();
	xhash.put(seqid, xlist);
      }
      FloatList ylist = (FloatList)yhash.get(seqid);
      if (ylist == null) {
	ylist = new FloatList();
	yhash.put(seqid, ylist);
      }
      int x = Integer.parseInt(fields[1]);
      float y = Float.parseFloat(fields[2]);

      if (DEBUG)  { System.out.println("seq = " + seqid + ", x = " + x + ", y = " + y); }

      xlist.add(x);
      ylist.add(y);
    }

    // after populating all xlists, now make sure sorted
    sortAll(xhash, yhash);

    Iterator iter = xhash.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry keyval = (Map.Entry)iter.next();
      String seqid = (String)keyval.getKey();
      BioSeq aseq = seq_group.getSeq(seqid);
      IntList xlist = (IntList)keyval.getValue();
      FloatList ylist = (FloatList)yhash.get(seqid);

      if (aseq == null) {
        aseq = seq_group.addSeq(seqid, xlist.get(xlist.size()-1));
      }

      int[] xcoords = xlist.copyToArray();
      xlist = null;
      float[] ycoords = ylist.copyToArray();
      ylist = null;

      GraphSym graf = new GraphSym(xcoords, ycoords, gid, aseq);
      results.add(graf);
    }

    } catch (Exception e) {
      if (! (e instanceof IOException)) {
        IOException ioe = new IOException("Trouble reading SGR file: " + stream_name);
        ioe.initCause(e);
        throw ioe;
      }
    }
    
    return results;
  }


  public static void sortAll(Map xhash, Map yhash) {
    // after populating all xlists, now make sure sorted
    Iterator iter = xhash.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry keyval = (Map.Entry)iter.next();
      String seqid = (String)keyval.getKey();
      IntList xlist = (IntList)keyval.getValue();
      if (DEBUG)  { System.out.println("key = " + seqid); }
      int xcount = xlist.size();
      boolean sorted = true;
      int prevx = Integer.MIN_VALUE;
      for (int i=0; i<xcount; i++) {
	int x = xlist.get(i);
	if (x < prevx) {
	  sorted = false;
	  break;
	}
	prevx = x;
      }
      if (! sorted) {
	pointSort(seqid, xhash, yhash);
      }
    }
  }


  protected static void pointSort(String seqid, Map xhash, Map yhash) {
    // System.out.println("points aren't sorted for seq = " + seqid + ", sorting now");
    IntList xlist = (IntList)xhash.get(seqid);
    FloatList ylist = (FloatList)yhash.get(seqid);
    int graph_length = xlist.size();
    List points = new ArrayList(graph_length);
    for (int i=0; i<graph_length; i++) {
      int x = xlist.get(i);
      float y = ylist.get(i);
      PointIntFloat pnt = new PointIntFloat(x, y);
      points.add(pnt);
    }
    Collections.sort(points, pointcomp);
    IntList new_xlist = new IntList(graph_length);
    FloatList new_ylist = new FloatList(graph_length);
    for (int i=0; i<graph_length; i++) {
      PointIntFloat pnt = (PointIntFloat) points.get(i);
      new_xlist.add(pnt.x);
      new_ylist.add(pnt.y);
    }
    xhash.put(seqid, new_xlist);
    yhash.put(seqid, new_ylist);
  }

  public static boolean writeSgrFormat(GraphSym graf, OutputStream ostr) throws IOException {
    BioSeq seq = graf.getGraphSeq();
    if (seq == null) {
      throw new IOException("You cannot use the '.sgr' format when the sequence is unknown. Use '.gr' instead.");
    }
    String seq_id = seq.getID();
    
    int xpos[] = graf.getGraphXCoords();
    float ypos[] = graf.getGraphYCoords();
      
    BufferedOutputStream bos = null;
    DataOutputStream dos = null;
    try {
      bos = new BufferedOutputStream(ostr);
      dos = new DataOutputStream(bos);
      
      for (int i=0; i<xpos.length; i++) {
        dos.writeBytes(seq_id + "\t" + xpos[i] + "\t" + ypos[i] + "\n");
      }
      dos.flush();
    } finally {
      dos.close();
    }
    return true;
  }

  public static void main(String[] args) {
    String test_file = System.getProperty("user.dir") + "/testdata/graph/test1.sgr";
    SgrParser test = new SgrParser();

    try {
      FileInputStream fis = new FileInputStream(new File(test_file));
      AnnotatedSeqGroup seq_group = SingletonGenometryModel.getGenometryModel().addSeqGroup("New Group");
      test.parse(fis, test_file, seq_group, true);
    }
    catch (Exception ex) { ex.printStackTrace(); }
  }

}
