/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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
import com.affymetrix.genometry.span.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.tiers.AnnotStyle;
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
  // if attaching graphs to seq, then if separate by strand make a separate graph sym
  //     for + and - strand, otherwise put both strands in same graph
  static public final boolean separate_by_strand = true;

  /**
   *  Boolean preference for whether container glyphs should always be added.
   *  If false, will construct the container glyphs only if there is MORE than
   *  one score field in the file.
   */
  static public final String PREF_ALWAYS_ADD_CONTAINER_GLYPHS = "Always add container glyphs";
  static public final boolean default_always_add_container_glyphs = false;

  /**
   *  If attach_graphs, then in addition to ScoredContainerSym added as annotation to seq,
   *      each array of scores is converted to a GraphSym and also added as annotation to seq.
   */
  boolean attach_graphs = default_attach_graphs;


  public void parse(InputStream istr, String stream_name, AnnotatedSeqGroup seq_group)
  throws IOException {
    attach_graphs = UnibrowPrefsUtil.getBooleanParam(PREF_ATTACH_GRAPHS, default_attach_graphs);
    BufferedReader br= null;
    try {
      br = new BufferedReader(new InputStreamReader(istr));
      String line = null;

      //      Map seq2container = new LinkedHashMap();
      Map seq2sinentries = new LinkedHashMap();
      //      Map seq2arrays = new LinkedHashMap();
      //      Map arrays2container = new LinkedHashMap();
      Map index2id = new HashMap();
      List score_names = null;
      Map props = new HashMap();

      // parse header lines (which must begin with "#")
      while (((line = br.readLine())!= null) &&
	     (line.startsWith("#") ||
	      line.startsWith(" ") ||
	      line.startsWith("\t") )  ) {

	// skipping starting lines that begin with space or tab, since
        // files output from GCOS begin with a header line that starts with a tab.
	if (line.startsWith(" ")  || line.startsWith("\t")) {
	  System.out.println("skipping line starting with whitespace: " + line);
	  continue;
	}
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
      int mod_hit_count = 0;
      int total_mod_hit_count = 0;
      int miss_count = 0;

      Matcher strand_matcher = strand_regex.matcher("");
      boolean sin1 = false;
      boolean sin2 = false;
      boolean sin3 = false;
      boolean all_sin3 = true;
      java.util.List isyms = new ArrayList();

      // There should already be a non-header line in the 'line' variable.
      // Continue reading lines until there are no more lines.
      for ( ; line != null ; line = br.readLine()) {
	isyms.clear();
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

	// sin1 format if 4rth field is strand: [+-.]
	if ((fields.length > 3) && strand_matcher.reset(fields[3]).matches())  {
	  sin1 = true; sin2 = false; sin3 = false; all_sin3 = false;
	  score_offset = 4;
	  annot_id = null;
	  seqid = fields[0];
	  min = Integer.parseInt(fields[1]);
	  max = Integer.parseInt(fields[2]);
	  strand = fields[3];
	  MutableAnnotatedBioSeq aseq = seq_group.getSeq(seqid);
	  if (aseq == null) { aseq = makeNewSeq(seqid, seq_group); }
	  IndexedSingletonSym child;
	  if (strand.equals("-")) { child = new IndexedSingletonSym(max, min, aseq); }
	  else { child = new IndexedSingletonSym(min, max, aseq); }
	  isyms.add(child);
          if (max > aseq.getLength()) { aseq.setLength(max); }
	}
	// sin2 format if 5th field is strand: [+-.]
	else if ((fields.length > 4) && strand_matcher.reset(fields[4]).matches())  {
	  sin2 = true; sin1 = false; sin3 = false; all_sin3 = false;
	  score_offset = 5;
	  annot_id = fields[0];
	  seqid = fields[1];
	  min = Integer.parseInt(fields[2]);
	  max = Integer.parseInt(fields[3]);
	  strand = fields[4];

	  MutableAnnotatedBioSeq aseq = seq_group.getSeq(seqid);
	  if (aseq == null) { aseq = makeNewSeq(seqid, seq_group); }
          if (max > aseq.getLength()) { aseq.setLength(max); }
	  IndexedSingletonSym child;
	  if (strand.equals("-")) { child = new IndexedSingletonSym(max, min, aseq); }
	  else { child = new IndexedSingletonSym(min, max, aseq); }
	  child.setID(annot_id);
	  isyms.add(child);
	}
	else { // not sin1 or sin2, must be sin3
	  sin3 = true; sin1 = false; sin2 = false;
	  score_offset = 1;
	  annot_id = fields[0];
	  // need to match up to pre-existing annotation in seq_group
          SeqSymmetry original_sym = findSym(seq_group, annot_id);
          if (original_sym == null) {
	    // if no sym with exact id found, then try "extended id", because may be
	    //     a case where sym id had to be "extended" to uniquify it
	    //     for instance, when the same probeset maps to multiple locations
	    //     extended ids are just the original id with ".$" appended, where $ is
	    //     a number, and if id with $ exists, then there must also be ids with all
	    //     positive integers < $ as well.
            SeqSymmetry mod_sym = findSym(seq_group, annot_id + ".0");
            // if found matching sym based on extended id, then need to keep incrementing and
	    //    looking for more syms with extended ids
	    if (mod_sym == null) {
	      // no sym matching id found -- filtering out
	      miss_count++;
	      continue;
	    }
	    else {
	      mod_hit_count++;
	      int ext = 0;
	      while (mod_sym != null) {
		SeqSpan span = mod_sym.getSpan(0);
		IndexedSingletonSym child = new IndexedSingletonSym(span.getStart(), span.getEnd(), span.getBioSeq());
		child.setID(mod_sym.getID());
		isyms.add(child);
		total_mod_hit_count++;
		ext++;
                mod_sym = findSym(seq_group, annot_id + "." + ext);
	      }
	    }
	  }
	  else {
	    // making a big assumption here, that first SeqSpan in sym is seqid to use...
	    //    on the other hand, not sure how much it matters...
	    //    for now, since most syms to match up with will come from via parsing of GFF files,
	    //       probably ok
	    SeqSpan span = original_sym.getSpan(0);
	    IndexedSingletonSym child = new IndexedSingletonSym(span.getStart(), span.getEnd(), span.getBioSeq());
	    child.setID(original_sym.getID());
	    isyms.add(child);
	    hit_count++;
	  }
	}   // end sin3 conditional

	if (score_names == null) {
	  //	  score_count = fields.length - 4;
	  score_count = fields.length - score_offset;
	  score_names = initScoreNames(score_count, index2id, stream_name);
	}

	score_count = fields.length - score_offset;
	float[] entry_floats = new float[score_count];
	int findex = 0;
	for (int field_index = score_offset; field_index<fields.length; field_index++) {
	  float score = Float.parseFloat(fields[field_index]);
	  entry_floats[findex] = score;
	  findex++;
	}

	// usually there will be only one IndexedSingletonSym in isyms list,
	//    but in the case of sin3, can have multiple syms that match up to the same sin id via "extended ids"
	//    so cycle through all isyms
	int icount = isyms.size();
	for (int i=0; i<icount; i++) {
	  IndexedSingletonSym child = (IndexedSingletonSym)isyms.get(i);
	  MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)child.getSpan(0).getBioSeq();
	  java.util.List sin_entries = (java.util.List)seq2sinentries.get(aseq);
	  if (sin_entries == null) {
	    sin_entries = new ArrayList();
	    //	  seq2sinentries.put(seqid, sin_entries);
	    seq2sinentries.put(aseq, sin_entries);
	  }
	  SinEntry sentry = new SinEntry(child, entry_floats);
	  sin_entries.add(sentry);
	}

	line_count++;
      }  // end br.readLine() loop

      // now for each sequence seen, sort the SinEntry list by span min/max
      SinEntryComparator comp = new SinEntryComparator();
      Iterator ents  = seq2sinentries.entrySet().iterator();
      while (ents.hasNext()) {
	Map.Entry ent = (Map.Entry)ents.next();
	BioSeq aseq = (BioSeq)ent.getKey();
	//	java.util.List entry_list = (java.util.List)entrylists.next();
	java.util.List entry_list = (java.util.List)ent.getValue();

	//	System.out.println("hmm, seq = " + aseq.getID() + ", entry count = " + entry_list.size());
	Collections.sort(entry_list, comp);
      }

      System.out.println("number of scores per line: " + score_count);
      // now make the container syms
      Iterator seqs = seq2sinentries.keySet().iterator();
      while (seqs.hasNext()) {
	MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)seqs.next();
	ScoredContainerSym container = new ScoredContainerSym();
	container.addSpan(new SimpleSeqSpan(0, aseq.getLength(), aseq));
	Iterator iter = props.entrySet().iterator();
	while (iter.hasNext())  {
	  Map.Entry entry = (Map.Entry)iter.next();
	  container.setProperty((String)entry.getKey(), entry.getValue());
	}
	container.setProperty("method", stream_name);

        // Force the AnnotStyle for the container to have glyph depth of 1
        AnnotStyle style = AnnotStyle.getInstance(stream_name);
        style.setGlyphDepth(1);

	//	seq2container.put(seqid, container);
	java.util.List entry_list = (java.util.List)seq2sinentries.get(aseq);
	int entry_count = entry_list.size();
	System.out.println("entry list count: " + entry_count);
	for (int k=0; k<entry_count; k++) {
	  SinEntry sentry = (SinEntry)entry_list.get(k);
	  container.addChild(sentry.sym);
	}
	System.out.println("container child count: " + container.getChildCount());

	// Object[] scores = new Object[score_count];
	for (int i=0; i<score_count; i++) {
	  String score_name = (String)score_names.get(i);
	  float[] score_column = new float[entry_count];
	  for (int k=0; k<entry_count; k++) {
	    SinEntry sentry = (SinEntry)entry_list.get(k);
	    score_column[k] = sentry.scores[i];
	  }
	  container.addScores(score_name, score_column);
	}

        boolean always_add_container_glyphs = UnibrowPrefsUtil.getBooleanParam(PREF_ALWAYS_ADD_CONTAINER_GLYPHS, default_always_add_container_glyphs);

        // if not sin3, then add container as annotation to seq
	// if sin3, then already have corresponding annotations on seq, only need to attach graphs
	// NO, CAN"T DO THIS YET -- right now need to able to select to get indexed scores to show up in PivotView
        if (always_add_container_glyphs || score_count > 1) {
	//	if (! all_sin3) {
  	aseq.addAnnotation(container);
	//	}
        }
	System.out.println("seq = " + aseq.getID() + ", interval count = " + container.getChildCount());
	if (attach_graphs) {
	  attachGraphs(container);
	}
      }

      System.out.println("data lines in .sin file: " + line_count);
      if ((hit_count + miss_count) > 0)  {
	System.out.println("sin3 miss count: " + miss_count);
	System.out.println("sin3 exact id hit count: " + hit_count);
      }
      if (mod_hit_count > 0)  {System.out.println("sin3 extended id hit count: " + mod_hit_count); }
      if (total_mod_hit_count > 0)  { System.out.println("sin3 total extended id hit count: " + mod_hit_count); }

    }
    catch (Exception ex) {
      IOException ioe = new IOException("Error while reading '.sin' file from: '"+stream_name+"'");
      ioe.initCause(ex);
      throw ioe;
    }
    finally {
      if (br != null) try { br.close(); } catch (Exception e) {}
    }
  }

  /** Find the first matching symmetry in the seq_group, or null */
  private SeqSymmetry findSym(AnnotatedSeqGroup seq_group, String id) {
    //TODO: Make this parser deal with the fact that there can be multiple
    // syms with the same ID rather than insisting on taking only the first match.
    // This probably will make this parser simpler, since we may be able to drop
    // this stuff about adding ".0" and ".1" to non-unique ids.
    List sym_list = seq_group.findSyms(id);
    if (sym_list.isEmpty()) {
      return null;
    } else {
      return (SeqSymmetry) sym_list.get(0);
    }
  }

  protected MutableAnnotatedBioSeq makeNewSeq(String seqid, AnnotatedSeqGroup seq_group) {
    System.out.println("in ScoredIntervalParser, creating new seq: " + seqid);
    return seq_group.addSeq(seqid, 0); // hmm, should a default size be set?
  }


  /**
   *  Make a GraphSym for each scores column, and add as an annotation to aseq.
   */
  protected void attachGraphs(ScoredContainerSym container) {
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)container.getSpan(0).getBioSeq();
    int score_count = container.getScoreCount();
    for (int i=0; i<score_count; i++) {
      String score_name = container.getScoreName(i);
      if (separate_by_strand)  {
	GraphSym forward_gsym = container.makeGraphSym(score_name, true);
	GraphSym reverse_gsym = container.makeGraphSym(score_name, false);
	if (forward_gsym != null) {
	  aseq.addAnnotation(forward_gsym);
	}
	if (reverse_gsym != null) {
	  aseq.addAnnotation(reverse_gsym);
	}
      }
      else {
	GraphSym gsym = container.makeGraphSym(score_name);
	if (gsym != null) {
	  aseq.addAnnotation(gsym);
	}
      }
    }
  }


  protected List initScoreNames(int score_count, Map index2id, String stream_name) {
    List names = new ArrayList();
    for (int i=0; i<score_count; i++) {
      Integer index = new Integer(i);
      String id = (String)index2id.get(index);
      if (id == null) {
        if (stream_name == null) {  id = "score" + i;  }
        else {
          if (score_count > 1) {
            id = stream_name + ": score" + i;
          } else {
            id = stream_name;
          }
        }
      }
      names.add(id);
    }
    return names;
  }

  public static void main(String[] args) {
    String test_file = System.getProperty("user.dir") + "/testdata/sin/test1.sin";
    String test_name = "name_testing";
    System.out.println("testing ScoredIntervalParser, parsing file: " + test_file);
    ScoredIntervalParser tester = new ScoredIntervalParser();
    AnnotatedSeqGroup seq_group = SingletonGenometryModel.getGenometryModel().addSeqGroup("Test Seq Group");
    try {
      FileInputStream fis = new FileInputStream(new File(test_file));
      tester.parse(fis, test_name, seq_group);
    }
    catch (Exception ex) { ex.printStackTrace(); }
    System.out.println("done testing ScoredMapParser");
  }

  /** For sorting of sin lines. */
  class SinEntry {
    SeqSymmetry sym;
    float[] scores;
    public SinEntry(SeqSymmetry sym, float[] scores) {
      this.sym = sym;
      this.scores = scores;
    }
  }

  /** For sorting of sin lines. */
  class SinEntryComparator implements Comparator  {
    public int compare(Object objA, Object objB) {
      SeqSpan symA = ((SinEntry)objA).sym.getSpan(0);
      SeqSpan symB = ((SinEntry)objB).sym.getSpan(0);
      if (symA.getMin() < symB.getMin()) { return -1; }
      else if (symA.getMin() > symB.getMin()) { return 1; }
      else {  // mins are equal, try maxes
	if (symA.getMax() < symB.getMax()) { return -1; }
	else if (symA.getMax() > symB.getMax()) { return 1; }
	else { return 0; }  // mins are equal and maxes are equal, so consider them equal
      }
    }
  }

}
