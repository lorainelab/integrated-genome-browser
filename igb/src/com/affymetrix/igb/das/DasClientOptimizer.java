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

package com.affymetrix.igb.das;

import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.SeqSpanComparator;
import java.util.*;
import java.util.List;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.SeqSymSummarizer;
import com.affymetrix.genometryImpl.SimpleSymWithProps;

import com.affymetrix.igb.menuitem.DasFeaturesAction2;
import com.affymetrix.igb.util.UnibrowPrefsUtil;

public class DasClientOptimizer {
  //static boolean SHOW_DAS_QUERY_GENOMETRY = true;
  boolean server_supports_within = true;

  /**
   *  The main IGB data model root, a hash of genome ids to
   *     genome hash (of seqid to annotated seqs).
   *
   */
  Map name2genome = null;

  public DasClientOptimizer(Map genomes_model) {
    name2genome = genomes_model;
  }

  /**
   *  Loads annotations from a DAS source.
   *  @param das_source_root  should be of the form "http://server.location:port/das_root"
   *    (or possibly "http://server.location:port/das_root/genome_version/" ?)
   *  @param overlap_span  The span containing the range from which you want annotations.
   *     The BioSeq of this SeqSpan must NOT be an instance of {@link SmartAnnotBioSeq}.
   *  @param types  a list of String's containg the types of annotations you want
   */
  public boolean loadAnnotations(String das_source_root, String das_source_label,
                                 SeqSpan overlap_span, List types) {
    boolean SHOW_DAS_QUERY_GENOMETRY = UnibrowPrefsUtil.getTopNode().getBoolean(
      DasFeaturesAction2.PREF_SHOW_DAS_QUERY_GENOMETRY, DasFeaturesAction2.default_show_das_query_genometry);
    BioSeq seq = overlap_span.getBioSeq();
    if (! (seq instanceof SmartAnnotBioSeq)) {
      System.out.println("Can't use DasClientOptimizer, seq is NOT an SmartAnnotBioSeq!");
      return false;
    }
    SeqSymmetry new_query = new SingletonSeqSymmetry(overlap_span);
    SmartAnnotBioSeq iseq = (SmartAnnotBioSeq)seq;
    String genome_version = iseq.getVersion();
    int omin = overlap_span.getMin();
    int omax = overlap_span.getMax();
    int typecount = types.size();

    for (int i=0; i<typecount; i++) {
      String short_type = (String)types.get(i);
      String long_type = das_source_label + ":" + short_type;
      SimpleSymWithProps query_sym = new SimpleSymWithProps();
      query_sym.setProperty("method", ("das_raw_query:" + long_type));
      query_sym.addSpan(overlap_span);
      if (SHOW_DAS_QUERY_GENOMETRY) {
        iseq.addAnnotation(query_sym);
      }

      MutableSeqSymmetry cont_sym = (MutableSeqSymmetry)iseq.getAnnotation(long_type);
      if (cont_sym == null) {
        cont_sym = (MutableSeqSymmetry)iseq.addAnnotation(long_type);
      }
      int prevcount = cont_sym.getChildCount();
      SeqSymmetry prev_union = null;
      SeqSymmetry split_query = null;
      if (prevcount > 0) {
        ArrayList contlist = new ArrayList();
        contlist.add(cont_sym);
        prev_union = SeqSymSummarizer.getUnion(contlist, iseq);

        ArrayList<SeqSymmetry> qnewlist = new ArrayList<SeqSymmetry>();
        qnewlist.add(new_query);
        ArrayList<SeqSymmetry> qoldlist = new ArrayList<SeqSymmetry>();
        qoldlist.add(prev_union);
        split_query = SeqSymSummarizer.getExclusive(qnewlist, qoldlist, iseq);
      }
      else {
        prev_union = new SimpleSymWithProps();  // since no previous, union of previous is empty sym
        split_query = query_sym;
      }
      SeqSpan split_query_span = split_query.getSpan(iseq);
      SimpleSymWithProps split_swp = new SimpleSymWithProps();
      SeqUtils.copyToMutable(split_query, split_swp);
      split_swp.setProperty("method", ("das_optimized_query:" + long_type));
      if (SHOW_DAS_QUERY_GENOMETRY) { iseq.addAnnotation(split_swp); }

      if (server_supports_within) {
        int first_within_min;
        int last_within_max;
        if (prevcount > 0) {
          List union_spans = SeqUtils.getLeafSpans(prev_union, iseq);
          SeqSpanComparator spancomp = new SeqSpanComparator();
          // since prev_union was created via SeqSymSummarizer, spans should come out already
          //   sorted by ascending min (and with no overlaps)
          //          Collections.sort(union_spans, spancomp);
          int insert = Collections.binarySearch(union_spans, split_query_span, spancomp);
          if (insert < 0) { insert = -insert -1; }
          if (insert == 0) { first_within_min = 0; }
          else { first_within_min = ((SeqSpan)union_spans.get(insert-1)).getMax(); }
          // since sorted by min, need to make sure that we are at the insert index
          //   at which get(index).min >= exclusive_span.max,
          //   so increment till this (or end) is reached
          while ((insert < union_spans.size()) &&
                 (((SeqSpan)union_spans.get(insert)).getMin() < split_query_span.getMax()))  {
            insert++;
          }
          if (insert == union_spans.size()) { last_within_max = iseq.getLength(); }
          else { last_within_max = ((SeqSpan)union_spans.get(insert)).getMin(); }
          // done determining first_within_min and last_within_max
        }
        else { // prevcount == 0
          first_within_min = 0;
          last_within_max = iseq.getLength();
        }
        int split_count = split_query.getChildCount();
        if (split_count == 0 || split_count == 1) {
          SimpleSymWithProps within_swp = new SimpleSymWithProps();
          within_swp.addSpan(new SimpleSeqSpan(first_within_min, last_within_max, iseq));
          within_swp.addChild(new SingletonSeqSymmetry(first_within_min,
                                                       first_within_min, iseq));
          if (split_count == 0) { within_swp.addChild(split_query); }
          else { within_swp.addChild(split_query.getChild(0)); }
          within_swp.addChild(new SingletonSeqSymmetry(last_within_max,
                                                       last_within_max, iseq));
          within_swp.setProperty("method", ("das_within_query:" + long_type));
          if (SHOW_DAS_QUERY_GENOMETRY) { iseq.addAnnotation(within_swp); }
        }
        else {
          int cur_within_min;
          int cur_within_max;
          for (int k=0; k<split_count; k++) {
            SeqSymmetry csym = split_query.getChild(k);
            SeqSpan cspan = csym.getSpan(iseq);
            if (k == 0) { cur_within_min = first_within_min; }
            else { cur_within_min = cspan.getMin(); }
            if (k == (split_count-1)) { cur_within_max = last_within_max; }
            else { cur_within_max = cspan.getMax(); }
            SimpleSymWithProps within_swp = new SimpleSymWithProps();
            within_swp.addSpan(new SimpleSeqSpan(cur_within_min, cur_within_max, iseq));
            within_swp.addChild(new SingletonSeqSymmetry(cur_within_min,
                                                         cur_within_min, iseq));
            within_swp.addChild(csym);
            within_swp.addChild(new SingletonSeqSymmetry(cur_within_max,
                                                         cur_within_max, iseq));
            within_swp.setProperty("method", ("das_within_query:" + long_type));
            if (SHOW_DAS_QUERY_GENOMETRY) { iseq.addAnnotation(within_swp); }
          }
        }
      }
      else {  // ! server_supports_within, so optimize by fragmenting but not by "within" constraints

      }

      // adding original query sym to type container sym
      cont_sym.addChild(query_sym);

      // now need to request annotations for each span of split query (including
      //    within constraints if server has support for them)
      // when loading these annotations, need to suppress attaching them
      //    directly as annotations on the sequence
      // instead, get back list of annotations returned by the server and
      //    then create a DasFeatureRequestSym to hold them
      // add the DasFeatureRequestSym to the seq as an annotation via
      //    SmartAnnotBioSeq.addAnnotation(sym, type), which will actually
      //    add it as a _child_ of a container annotation sym for the given type,
      //    therefore the container annotation sym is the only sym directly
      //    attached as an annotation of the SmartAnnotBioSeq (and the container sym is
      //    created by SmartAnnotBioSeq.addAnnotation(sym, type) if there is not already a
      //    container sym of the right type attached to the SmartAnnotBioSeq)
      //
     //  loadOptimizedDas(server, overlap_span, within_span, type);
    }
    return true;
  }


}
