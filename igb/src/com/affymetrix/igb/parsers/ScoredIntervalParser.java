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
import com.affymetrix.igb.util.UnibrowPrefsUtil;

/**
 *  Parses "sin" file format into genometry model of ScoredContainerSyms
 *     with IndexedSingletonSym children.
 *<pre>
 *  Description of ".sin" format:
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
 *  SIN format version 1
 *  tab-delimited lines with 4 required columns, any additional columns are scores:
 *  seqid    min_coord    max_coord    strand    [score]*
 *
 *  SIN format version 2
 *  tab-delimited lines with 5 required columns, any additional columns are scores:
 *  annot_id    seqid    min_coord    max_coord    strand    [score]*
 *
 *  SIN format version 3
 *  tab-delimited lines with 1 required column, any additional columns are scores:
 *  annot_id  [score]*
 *
 *  Parser _should_ be able to distinguish between these, based on combination of
 *     number of fields, and presence and position of strand field
 *
 *  For use in IGB, SIN version 3 is dependent on prior loading of annotations with ids, and whether those
 *     ids have actually been added to IGB's standard id-->annotation_sym mapping
 *
 *  seqid is word string [a-zA-Z_0-9]+
 *  min_coord is int
 *  max_coord is int
 *  strand can be '+', '-', or '.' for "unknown"
 *  score is float
 *  annot_id is word string [a-zA-Z_0-9]+
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
  static Pattern strand_regex = Pattern.compile("[\\+\\-\\.]");

  static public final String PREF_ATTACH_GRAPHS = "Make graphs from scored intervals";
  static public final boolean default_attach_graphs = true;

  /**
   *  If attach_graphs, then in addition to ScoredContainerSym added as annotation to seq,
   *      each array of scores is converted to a GraphSym and also added as annotation to seq.
   */
  boolean attach_graphs = default_attach_graphs;


  public void parse(InputStream istr, String stream_name, Map seqhash) {
    parse(istr, stream_name, seqhash, null);
  }

  public void parse(InputStream istr, String stream_name, Map seqhash, Map id2sym_hash) {
    attach_graphs = UnibrowPrefsUtil.getBooleanParam(PREF_ATTACH_GRAPHS, default_attach_graphs);
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(istr));
      String line = null;

      Map seq2container = new LinkedHashMap();
      Map seq2arrays = new LinkedHashMap();
      Map arrays2container = new LinkedHashMap();
      Map index2id = new HashMap();
      List score_names = null;
      Map props = new HashMap();

      // parse header lines (which must begin with "#")
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
      int hit_count = 0;
      int miss_count = 0;

      Matcher strand_matcher = strand_regex.matcher("");
      boolean sin1 = false;
      boolean sin2 = false;
      boolean sin3 = false;
      //      while (line != null) {
      while ((line = br.readLine()) != null) {
	// skip comment lines (any lines that start with "#")
	if (line.startsWith("#")) { continue; }

	String[] fields = line_regex.split(line);
	int fieldcount = fields.length;

	String annot_id = null;
	String seqid;
	int min;
	int max;
	String strand = null;
	int score_offset;
	SeqSymmetry original_sym = null;  // only used for sin3 format

	sin1 = (fields.length > 3) && strand_matcher.reset(fields[3]).matches();  // sin1 format if 4rth field is strand: [+-.]
	if (sin1) {
	  score_offset = 4;
	  annot_id = null;
	  seqid = fields[0];
	  min = Integer.parseInt(fields[1]);
	  max = Integer.parseInt(fields[2]);
	  strand = fields[3];
	}
	else {
	  sin2 = (fields.length > 4) && strand_matcher.reset(fields[4]).matches();   // sin2 format if 5th field is strand: [+-.]
	  if (sin2) {
	    score_offset = 5;
	    annot_id = fields[0];
	    seqid = fields[1];
	    min = Integer.parseInt(fields[2]);
	    max = Integer.parseInt(fields[3]);
	    strand = fields[4];
	  }
	  else {
	    sin3 = true;
	    score_offset = 1;
	    annot_id = fields[0];
	    // need to match up to pre-existing annotation in id2sym_hash
	    original_sym = (SeqSymmetry)id2sym_hash.get(annot_id);
	    if (original_sym == null) {
	      // no sym matching id found in id2sym_hash -- filter out
	      miss_count++;
	      continue;
	    }
	    else {
	      // making a big assumption here, that first SeqSpan in sym is seqid to use...
	      //    on the other hand, not sure how much it matters...
	      //    for now, since most syms to match up with will come from via parsing of GFF files,
	      //       probably ok
	      annot_id = original_sym.getID();
	      SeqSpan span = original_sym.getSpan(0);
	      seqid = span.getBioSeq().getID();
	      min = span.getMin();
	      max = span.getMax();
	      if (! span.isForward()) { strand = "-"; }
	      else { strand = "+"; }
	      hit_count++;
	    }
	  }
	}
	if (score_names == null) {
	  //	  score_count = fields.length - 4;
	  score_count = fields.length - score_offset;
	  score_names = initScoreNames(score_count, index2id);
	}

	ScoredContainerSym container = (ScoredContainerSym)seq2container.get(seqid);
	MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)seqhash.get(seqid);
	if (aseq == null) {
	  System.out.println("in ScoredIntervalParser, creating new seq: " + seqid);
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

	IndexedSym child;
	if (sin1 || sin2) {
	  if (strand.equals("-")) { child = new IndexedSingletonSym(max, min, aseq); }
	  else { child = new IndexedSingletonSym(min, max, aseq); }
	  if (sin2) { ((IndexedSingletonSym)child).setID(annot_id); }
	}
	else {  // sin3
	  // encountered visualization and selection problems using IndexedWrapperSym,
	  //   so for now making new sym, but using original_syms bounds and id
	  //	  child = new IndexedWrapperSym(original_sym);
	  if (strand.equals("-")) { child = new IndexedSingletonSym(max, min, aseq); }
	  else { child = new IndexedSingletonSym(min, max, aseq); }
	  ((IndexedSingletonSym)child).setID(annot_id);
	}
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
	for (int field_index = score_offset; field_index < fields.length; field_index++) {
	  FloatList flist = (FloatList)score_arrays.get(field_index - score_offset);
	  float score = Float.parseFloat(fields[field_index]);
	  flist.add(score);
	}
	line_count++;
      }

      System.out.println("data lines in .sin file: " + line_count);
      System.out.println("sin3 hit count: " + hit_count);
      System.out.println("sin3 miss count: " + miss_count);

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
	if (attach_graphs) {
	  // make a GraphSym for each scores column, and add as an annotation to aseq
	  for (int i=0; i<score_count; i++) {
	    String score_name = container.getScoreName(i);
	    GraphSym gsym = container.makeGraphSym(score_name);
	    aseq.addAnnotation(gsym);
	  }
	  // System.out.println("finished attaching graphs");
	}
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
