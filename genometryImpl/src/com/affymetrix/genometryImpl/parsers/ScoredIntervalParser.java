/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.ScoredContainerSym;
import com.affymetrix.genometryImpl.IndexedSingletonSym;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import java.util.regex.Matcher;

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
chr22        14433291        14433388        +        140.642        175.816
chr22        14433586        14433682        +        52.3838        58.1253
chr22        14434054        14434140        +        36.2883        40.7145

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
  //static public final String PREF_ALWAYS_ADD_CONTAINER_GLYPHS = "Always add container glyphs";
  //static public final boolean default_always_add_container_glyphs = false;

  /**
   *  If attach_graphs, then in addition to ScoredContainerSym added as annotation to seq,
   *      each array of scores is converted to a GraphSym and also added as annotation to seq.
   */
//  boolean attach_graphs = default_attach_graphs;


  public void parse(InputStream istr, String stream_name, AnnotatedSeqGroup seq_group)
  throws IOException {
    //boolean attach_graphs = UnibrowPrefsUtil.getBooleanParam(PREF_ATTACH_GRAPHS, default_attach_graphs);
    String unique_container_name = AnnotatedSeqGroup.getUniqueGraphID(stream_name, seq_group);

    BufferedReader br= null;
    int line_count = 0;
    int score_count = 0;
    int hit_count = 0;
    int mod_hit_count = 0;
    int total_mod_hit_count = 0;
    int miss_count = 0;
    boolean sin1 = false;
    boolean sin2 = false;
    boolean sin3 = false;
    boolean all_sin3 = true;

    try {
      br = new BufferedReader(new InputStreamReader(istr));
      String line = null;

      //      Map seq2container = new LinkedHashMap();
      Map<MutableAnnotatedBioSeq,List<SinEntry>> seq2sinentries = new LinkedHashMap<MutableAnnotatedBioSeq,List<SinEntry>>();
      //      Map seq2arrays = new LinkedHashMap();
      //      Map arrays2container = new LinkedHashMap();
      Map<Integer,String> index2id = new HashMap<Integer,String>();
      List<String> score_names = null;
      Map<String,Object> props = new HashMap<String,Object>();

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
            try {
                int score_index = Integer.parseInt(tag.substring(tag.indexOf("score") + 5));
              index2id.put(new Integer(score_index), val);
            } catch (NumberFormatException nfe) {
              throw new IOException("Tag '"+tag+"' is not in the format score# where # = 0,1,2,....");
            }
          }
          else {
            props.put(tag, val);
          }
        }
      }

      Matcher strand_matcher = strand_regex.matcher("");
      List<IndexedSingletonSym> isyms = new ArrayList<IndexedSingletonSym>();

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
          seq_group.addToIndex(annot_id, child);
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
          //          score_count = fields.length - 4;
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
          IndexedSingletonSym child = isyms.get(i);
          MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)child.getSpan(0).getBioSeq();
          List<SinEntry> sin_entries = seq2sinentries.get(aseq);
          if (sin_entries == null) {
            sin_entries = new ArrayList<SinEntry>();
            //          seq2sinentries.put(seqid, sin_entries);
            seq2sinentries.put(aseq, sin_entries);
          }
          SinEntry sentry = new SinEntry(child, entry_floats);
          sin_entries.add(sentry);
        }

        line_count++;
      }  // end br.readLine() loop

      // now for each sequence seen, sort the SinEntry list by span min/max
      SinEntryComparator comp = new SinEntryComparator();
      for (List<SinEntry> entry_list : seq2sinentries.values()) {
        Collections.sort(entry_list, comp);
      }

      System.out.println("number of scores per line: " + score_count);
      // now make the container syms
      for (MutableAnnotatedBioSeq aseq : seq2sinentries.keySet()) {
        ScoredContainerSym container = new ScoredContainerSym();
        container.addSpan(new SimpleSeqSpan(0, aseq.getLength(), aseq));
        for (Map.Entry<String,Object> entry : props.entrySet())  {
          container.setProperty(entry.getKey(), entry.getValue());
        }

        container.setProperty("method", unique_container_name);
        container.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);

        // Force the AnnotStyle for the container to have glyph depth of 1
        IAnnotStyleExtended style = seq_group.getStateProvider().getAnnotStyle(unique_container_name);
        style.setGlyphDepth(1);

        //        seq2container.put(seqid, container);
        List<SinEntry> entry_list = seq2sinentries.get(aseq);
        int entry_count = entry_list.size();
        System.out.println("entry list count: " + entry_count);
        for (int k=0; k<entry_count; k++) {
          container.addChild((SeqSymmetry) entry_list.get(k));
        }
        System.out.println("container child count: " + container.getChildCount());

        // Object[] scores = new Object[score_count];
        for (int i=0; i<score_count; i++) {
          String score_name = score_names.get(i);
          float[] score_column = new float[entry_count];
          for (int k=0; k<entry_count; k++) {
            SinEntry sentry = entry_list.get(k);
            score_column[k] = sentry.scores[i];
          }
          container.addScores(score_name, score_column);
        }

        //boolean always_add_container_glyphs = UnibrowPrefsUtil.getBooleanParam(PREF_ALWAYS_ADD_CONTAINER_GLYPHS, default_always_add_container_glyphs);
        //boolean always_add_container_glyphs = default_always_add_container_glyphs;

        // always add the container as an annotation, and
        // do not attach any graphs
        // ScoredContainerGlyph factory will then draw container syms, or graphs, or both

        container.setID(unique_container_name);

        //if (always_add_container_glyphs || score_count > 1) {
          aseq.addAnnotation(container);
        //}
        //System.out.println("seq = " + aseq.getID() + ", interval count = " + container.getChildCount());
        //if (attach_graphs) {
        //  attachGraphs(container, aseq);
        //}
      }

      System.out.println("data lines in .sin file: " + line_count);
      if ((hit_count + miss_count) > 0)  {
        System.out.println("sin3 miss count: " + miss_count);
        System.out.println("sin3 exact id hit count: " + hit_count);
      }
      if (mod_hit_count > 0)  {System.out.println("sin3 extended id hit count: " + mod_hit_count); }
      if (total_mod_hit_count > 0)  { System.out.println("sin3 total extended id hit count: " + total_mod_hit_count); }

    }
    catch (Exception ex) {
      IOException ioe = new IOException("Error while reading '.egr' or '.sin' file from: '"+stream_name+"'");
      ioe.initCause(ex);
      throw ioe;
    }
    finally {
      if (br != null) try { br.close(); } catch (Exception e) {}
    }

    if (all_sin3 && hit_count == 0 && mod_hit_count == 0 && miss_count > 0) {
      throw new IOException("No data loaded. The ID's in the file did not match any ID's from data that has already been loaded.");
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


//  /**
//   *  Make a GraphSym for each scores column, and add as an annotation to aseq.
//   */
//  static protected void attachGraphs(ScoredContainerSym container, MutableAnnotatedBioSeq aseq) {
//    GraphIntervalSym[] graphs = makeGraphs(container);
//    for (int i=0; i<graphs.length; i++) {
//      aseq.addAnnotation(graphs[i]);
//    }
//  }
//
//  static public GraphIntervalSym[] makeGraphs(ScoredContainerSym container) {
//    //TODO: deal with uniquifying the styles?
//    // May not be needed, because the styles are created here and never
//    // applied to anything else anyway
//
//    IAnnotStyle combo_f = new DefaultIAnnotStyle("Scores (+)", true);
//    combo_f.setExpandable(true);
//    combo_f.setCollapsed(false);
//    IAnnotStyle combo_r = new DefaultIAnnotStyle("Scores (-)", true);
//    combo_r.setExpandable(true);
//    combo_r.setCollapsed(false);
//    IAnnotStyle combo = new DefaultIAnnotStyle("Scores", true);
//    combo.setExpandable(true);
//    combo.setCollapsed(false);
//
//    int score_count = container.getScoreCount();
//    ArrayList results = new ArrayList(score_count * 2);
//
//    for (int i=0; i<score_count; i++) {
//      String score_name = container.getScoreName(i);
//      if (separate_by_strand)  {
//        GraphIntervalSym forward_gsym = container.makeGraphSym(score_name, true, true);
//        GraphIntervalSym reverse_gsym = container.makeGraphSym(score_name, true, false);
//        if (forward_gsym != null) {
//          forward_gsym.getGraphState().setFloatGraph(false);
//          forward_gsym.getGraphState().setComboStyle(combo_f);
//          forward_gsym.getGraphState().getTierStyle().setHumanName(score_name);
//          results.add(forward_gsym);
//        }
//        if (reverse_gsym != null) {
//          reverse_gsym.getGraphState().setFloatGraph(false);
//          reverse_gsym.getGraphState().setComboStyle(combo_r);
//          reverse_gsym.getGraphState().getTierStyle().setHumanName(score_name);
//          results.add(reverse_gsym);
//        }
//      }
//      else {
//        GraphSym gsym = container.makeGraphSym(score_name, true);
//        if (gsym != null) {
//          gsym.getGraphState().setFloatGraph(false);
//          gsym.getGraphState().setComboStyle(combo);
//          gsym.getGraphState().getTierStyle().setHumanName(score_name);
//          results.add(gsym);
//        }
//      }
//    }
//    return (GraphIntervalSym[]) results.toArray(new GraphIntervalSym[results.size()]);
//  }


  protected List<String> initScoreNames(int score_count, Map<Integer,String> index2id, String stream_name) {
    List<String> names = new ArrayList<String>();
    for (int i=0; i<score_count; i++) {
      Integer index = new Integer(i);
      String id = index2id.get(index);
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

  /** Writes the given GraphIntervalSym in egr format (version 1).
   *  Also writes a header.
   */
  public static boolean writeEgrFormat(GraphIntervalSym graf, String genome_version, OutputStream ostr) throws IOException {
    int xpos[] = graf.getGraphXCoords();
    int widths[] = graf.getGraphWidthCoords();
    //float ypos[] = (float[]) graf.getGraphYCoords();
    BufferedOutputStream bos = null;
    DataOutputStream dos = null;

    try {
      bos = new BufferedOutputStream(ostr);
      dos = new DataOutputStream(bos);

      BioSeq seq = graf.getGraphSeq();
      String seq_id = (seq == null ? "." : seq.getID());

      String human_name = graf.getGraphState().getTierStyle().getHumanName();

      if (genome_version != null) {
        dos.writeBytes("# genome_version = " + genome_version + '\n');
      }
      dos.writeBytes("# score0 = " + human_name + '\n');

      Object strand_property = graf.getProperty(GraphSym.PROP_GRAPH_STRAND);
      char strand_char = '.';
      if (GraphSym.GRAPH_STRAND_PLUS.equals(strand_property)) {
        strand_char = '+';
      }
      else if (GraphSym.GRAPH_STRAND_MINUS.equals(strand_property)) {
        strand_char = '-';
      }
      else if (GraphSym.GRAPH_STRAND_BOTH.equals(strand_property)) {
        // the GraphIntervalSym does NOT keep track of the strand of each
        // individual region inside it.
        strand_char = '.';
      }

      for (int i=0; i<xpos.length; i++) {
        int x2 = xpos[i] + widths[i];
        dos.writeBytes(seq_id + '\t' + xpos[i] + '\t' +  x2  + '\t' + strand_char + '\t' + graf.getGraphYCoordString(i) + '\n');
      }
      dos.flush();
    } finally {
      dos.close();
    }
    return true;
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
  public class SinEntry {
    SeqSymmetry sym;
    float[] scores;
    public SinEntry(SeqSymmetry sym, float[] scores) {
      this.sym = sym;
      this.scores = scores;
    }
  }

  /** For sorting of sin lines. */
  public class SinEntryComparator implements Comparator<SinEntry>  {
    public int compare(SinEntry objA, SinEntry objB) {
      SeqSpan symA = objA.sym.getSpan(0);
      SeqSpan symB = objB.sym.getSpan(0);
      final int minA = symA.getMin();
      final int minB = symB.getMin();
      if (minA < minB) { return -1; }
      else if (minA > minB) { return 1; }
      else {  // mins are equal, try maxes
        final int maxA = symA.getMax();
        final int maxB = symB.getMax();
        if (maxA < maxB) { return -1; }
        else if (maxA > maxB) { return 1; }
        else { return 0; }  // mins are equal and maxes are equal, so consider them equal
      }
    }
  }

}
