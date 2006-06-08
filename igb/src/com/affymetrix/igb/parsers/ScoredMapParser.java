/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.util.FloatList;
import com.affymetrix.igb.util.UnibrowPrefsUtil;

/**
 *  This class (and file format) have been replaced by ScoredIntervalParser (and sin file format)
 *  Kept now only to parse in older data files.
 */
public class ScoredMapParser {

  static Pattern line_regex  = Pattern.compile("\t");
  boolean attach_graphs = ScoredIntervalParser.default_attach_graphs;

  public void parse(InputStream istr, String stream_name, MutableAnnotatedBioSeq aseq) {
    attach_graphs = UnibrowPrefsUtil.getBooleanParam(ScoredIntervalParser.PREF_ATTACH_GRAPHS,
						     ScoredIntervalParser.default_attach_graphs);
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(istr));
      String line = null;

      ScoredContainerSym parent = new ScoredContainerSym();
      parent.addSpan(new SimpleSeqSpan(0, aseq.getLength(), aseq));
      parent.setProperty("method", stream_name);

      // assuming first line is header
      line = br.readLine();
      String[] headers = line_regex.split(line);
      java.util.List score_names = new ArrayList();
      java.util.List score_arrays = new ArrayList();
      System.out.println("headers: " + line);
      for (int i=2; i<headers.length; i++) {
	//	System.out.println("header " + i + ": " + headers[i]);
	score_names.add(headers[i]);
	score_arrays.add(new FloatList());
      }

      int line_count = 0;
      while ((line = br.readLine()) != null) {
	String[] fields = line_regex.split(line);
	int min = Integer.parseInt(fields[0]);
	int max = Integer.parseInt(fields[1]);
	SeqSymmetry child = new IndexedSingletonSym(min, max, aseq);
	parent.addChild(child);   // ScoredContainerSym.addChild() handles setting of child index and parent fields
	for (int field_index = 2; field_index < fields.length; field_index++) {
	  FloatList flist = (FloatList)score_arrays.get(field_index-2);
	  float score = Float.parseFloat(fields[field_index]);
	  flist.add(score);
	}
	line_count++;
      }
      System.out.println("data lines in file: " + line_count);
      // System.out.println("child syms: " + parent.getChildCount());
      int score_count = score_names.size();
      for (int i=0; i<score_count; i++) {
	String score_name = (String)score_names.get(i);
	FloatList flist = (FloatList)score_arrays.get(i);
	float[] scores = flist.copyToArray();
	// System.out.println("adding scores for " + score_name + ", score count = " + scores.length);
	// System.out.println("first 3 scores:\t" + scores[0] + "\t" + scores[1] + "\t" + scores[2]);
	parent.addScores(score_name, scores);
      }
      aseq.addAnnotation(parent);
      if (attach_graphs) {
	// make a GraphSym for each scores column, and add as an annotation to aseq
	for (int i=0; i<score_count; i++) {
	  String score_name = parent.getScoreName(i);
	  GraphSym gsym = parent.makeGraphSym(score_name, true);
	  aseq.addAnnotation(gsym);
	}
	// System.out.println("finished attaching graphs");
      }
    }
    catch (Exception ex) { ex.printStackTrace(); }
  }

  public static void main(String[] args) {
    String test_file = System.getProperty("user.dir") + "/testdata/fromAntonio/exp_files/maps_chr22_3-26-2004/tau0.2-pp0.map";
    String test_name = "tau0_test";
    System.out.println("testing ScoredMapParser, parsing file: " + test_file);
    ScoredMapParser tester = new ScoredMapParser();
    MutableAnnotatedBioSeq aseq = new SimpleAnnotatedBioSeq("test_seq", 50000000);
    try {
      FileInputStream fis = new FileInputStream(new File(test_file));
      tester.parse(fis, test_name, aseq);
    }
    catch (Exception ex) { ex.printStackTrace(); }
    System.out.println("done testing ScoredMapParser");
  }

}
