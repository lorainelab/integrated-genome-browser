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
import java.util.regex.Matcher;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.util.IntList;
import com.affymetrix.igb.util.FloatList;

/**
 *  parses ".sin" file format into genometry model of ScoredContainerSyms
 *     with IndexedSingletonSym children.
 *<pre>
 *  Description of .sin format:
 *  HEADER SECTION
 *  .sin files have an optional header section at the beginning,
 *     which is just a list of tag-value pairs, one per line, in the form:
 *       # tag = value
 *  Currently the only tags used by the parser are of the form "score$i"
 *     For each score column in the data section at index $i, if there is a
 *       header with tag of "score$i", then the id of that set of scores will be set
 *       to the corresponding value.  If no score tag exists for a given column i, then 
 *       by default it is assigned an id of "score$i"
 *  Also, it is recommended that a tagval pair with tag = "genome_version" be included
 *     to indicate which genome assembly the sequence coordinates are based on
 *     Although currently this is not used, this will likely be used in subsequent
 *     releases to ensure that the .sin file is being compared to other annotations
 *     from the same assembly
 *
 *  DATA SECTION
 *  tab-delimited lines with 4 required columns, any additional columns are scores:
 *  seqid    min_coord    max_coord    strand    [score]*
 *
 *  seqid is word string [a-zA-Z_0-9]+
 *  min_coord is int
 *  max_coord is int
 *  strand can be '+', '-', or '.' for "unknown"
 *  score is float
 *
 *  all lines must have same number of columns
 *
 *  EXAMPLE:

# genome_version = H_sapiens_Apr_2003
# score0 = A375
# score1 = FHS
chr22	14433291	14433388	+	140.642	175.816
chr22	14433586	14433682	+	52.3838	58.1253
chr22	14434054	14434140	+	36.2883	40.7145

 <pre>
 */
public class ScoredIntervalParser {

  static Pattern line_regex  = Pattern.compile("\t");
  static Pattern tagval_regex = Pattern.compile("#\\s*([\\w]+)\\s*=\\s*(.*)$");

  public void parse(InputStream istr, String stream_name, Map seqhash) {
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(istr));
      String line = null;

      Map seq2container = new LinkedHashMap();
      Map seq2arrays = new LinkedHashMap();
      Map arrays2container = new LinkedHashMap();
      Map index2id = new HashMap();
      List score_names = null;
      Map props = new HashMap();

      // assuming first line is header
      while (((line = br.readLine())!= null) && (line.startsWith("#"))) {
	Matcher match = tagval_regex.matcher(line);
	if (match.matches()) {
	  String tag = match.group(1);
	  String val = match.group(2);
	  if (tag.startsWith("score")) {
	    int score_index = Integer.parseInt(tag.substring(tag.indexOf("score") + 5));
	    index2id.put(new Integer(score_index), val);
	  }
	  else {
	    props.put(tag, val);
	  }
	}
      }

      int line_count = 0;
      int score_count = 0;
      while (line != null) {
	// skip comment lines (any lines that start with "#")
	if (line.startsWith("#")) { line = br.readLine(); continue; }

	String[] fields = line_regex.split(line);
	String seqid = fields[0];
	int min = Integer.parseInt(fields[1]);
	int max = Integer.parseInt(fields[2]);
	String strand = fields[3];
	if (score_names == null) {
	  score_count = fields.length - 4;
	  score_names = initScoreNames(score_count, index2id);
	}
        ScoredContainerSym container = (ScoredContainerSym)seq2container.get(seqid);
        MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)seqhash.get(seqid);
        if (aseq == null) {
          aseq = new SimpleAnnotatedBioSeq(seqid, 0); // hmm, should a default size be set?
          seqhash.put(seqid, aseq);
        }
        if (container == null) {
          container = new ScoredContainerSym();
          container.addSpan(new SimpleSeqSpan(0, aseq.getLength(), aseq));
          Iterator iter = props.entrySet().iterator();
          while (iter.hasNext())  {
            Map.Entry entry = (Map.Entry)iter.next();
            container.setProperty((String)entry.getKey(), entry.getValue());
          }
          container.setProperty("method", stream_name);
	  seq2container.put(seqid, container);
	}

	SeqSymmetry child;
	if (strand.equals("-")) { child = new IndexedSingletonSym(max, min, aseq); }
	else { child = new IndexedSingletonSym(min, max, aseq); }
	// ScoredContainerSym.addChild() handles setting of child index and parent fields
	container.addChild(child);

	List score_arrays = (List)seq2arrays.get(seqid);
	if (score_arrays == null) {
	  score_arrays = new ArrayList();
	  for (int i=0; i<score_names.size(); i++) {
	    score_arrays.add(new FloatList());  // adding empty FloatLists for each column
	  }
	  seq2arrays.put(seqid, score_arrays);
	  arrays2container.put(score_arrays, container);
	}
	for (int field_index = 4; field_index < fields.length; field_index++) {
	  FloatList flist = (FloatList)score_arrays.get(field_index-4);
	  float score = Float.parseFloat(fields[field_index]);
	  flist.add(score);
	}
	line_count++;
	line = br.readLine();
      }

      System.out.println("data lines in .sin file: " + line_count);
      Iterator iter = arrays2container.entrySet().iterator();
      while (iter.hasNext()) {
	Map.Entry entry = (Map.Entry)iter.next();
	ArrayList score_arrays = (ArrayList)entry.getKey();
	ScoredContainerSym container = (ScoredContainerSym)entry.getValue();
        MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)container.getSpan(0).getBioSeq();
	for (int i=0; i<score_count; i++) {
	  String score_name = (String)score_names.get(i);
	  FloatList flist = (FloatList)score_arrays.get(i);
	  float[] scores = flist.copyToArray();
	  container.addScores(score_name, scores);
	}
	aseq.addAnnotation(container);
	System.out.println("seq = " + aseq.getID() + ", interval count = " + container.getChildCount());
      }

    }
    catch (Exception ex) { ex.printStackTrace(); }
  }

  protected List initScoreNames(int score_count, Map index2id) {
    List names = new ArrayList();;
    for (int i=0; i<score_count; i++) {
      Integer index = new Integer(i);
      String id = (String)index2id.get(index);
      if (id == null) {  id = "score" + i; }
      names.add(id);
    }
    return names;
  }

  public static void main(String[] args) {
    String test_file = System.getProperty("user.dir") + "/testdata/sin/test1.sin";
    String test_name = "name_testing";
    System.out.println("testing ScoredIntervalParser, parsing file: " + test_file);
    ScoredIntervalParser tester = new ScoredIntervalParser();
    Map seqhash = new HashMap();
    try {
      FileInputStream fis = new FileInputStream(new File(test_file));
      tester.parse(fis, test_name, seqhash);
    }
    catch (Exception ex) { ex.printStackTrace(); }
    System.out.println("done testing ScoredMapParser");
  }

}
