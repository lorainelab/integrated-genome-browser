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

package com.affymetrix.igb.genometry;

import java.util.*;
import com.affymetrix.genometry.*;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.util.IntList;
import com.affymetrix.igb.util.FloatList;
import com.affymetrix.igb.util.GraphSymUtils;

/**
 *  A SeqSymmetry that can only accept children that are instances of
 *  {@link com.affymetrix.igb.genometry.IndexedSym}.
 *  Assumes that ScoredContainerSym has only one SeqSpan
 */
public class ScoredContainerSym extends SimpleSymWithProps {
  Map name2scores = new HashMap();
  java.util.List scorevals = new ArrayList();
  java.util.List scorenames = new ArrayList();

  /**
   *  Adds scores.
   *  Assumes all child syms have already been added, and span has already been set.
   *  @param scores  a float array with the same length as the number of children.
   */
  public void addScores(String name, float[] scores) {
    name2scores.put(name, scores);
    scorevals.add(scores);
    scorenames.add(name);
  }

  public int getScoreCount() { return scorevals.size(); }

  public float[] getScores(String name) {
    return (float[])name2scores.get(name);
  }

  public float[] getScores(int index) {
    return (float[])scorevals.get(index);
  }

  public String getScoreName(int index)  {
    return (String)scorenames.get(index);
  }

  public float[] getChildScores(IndexedSym child, java.util.List scorelist) {
    float[] result = null;
    if (child.getParent() == this) {
      int score_index = child.getIndex();  // position in each score array for score for this child
      int scores_count = scorelist.size();
      result = new float[scores_count];
      for (int i=0; i<scores_count; i++) {
	float[] scores = (float[])scorelist.get(i);
	result[i] = scores[score_index];
      }
    }
    return result;
  }

  public float[] getChildScores(IndexedSym child) {
    return getChildScores(child, scorevals);
  }

  /**
   *  Can only accept children that are instances of IndexedSym.
   */
  public void addChild(SeqSymmetry sym) {
    if (sym instanceof IndexedSym) {
      IndexedSym isym = (IndexedSym)sym;
      int current_index = this.getChildCount();
      isym.setIndex(current_index);
      isym.setParent(this);
      super.addChild(isym);
    }
    else {
      System.err.println("ERROR: cannot add a child to ScoredContainerSym unless it is an IndexedSym");
    }
  }


 /**
  *  Creates a GraphSym.
  *  Assumes all child syms have already been added, and span has already been set.
  *<pre>
  *  Resultant graph sym has two data points for each child sym,
  *     first  with x = min of child's span, y = score at child's index in "name" float array
  *     second with x = max of child's span, y = 0
  *</pre>
  *
  *  The returned GraphSym will have this property set by default:
  *  <ol>
  *  <li>GraphSym.PROP_GRAPH_STRAND will be the correct strand Character.
  *  </ol>
  *
  * @return a GraphSym or null if there was an error condition
   */
  public GraphSym makeGraphSym(String name, boolean ensure_unique_id)  {
    float[] scores = getScores(name);
    SeqSpan pspan = this.getSpan(0);
    if (scores == null) {
      System.err.println("ScoreContainerSym.makeGraphSym() called, but no scores found for: " + name);
      return null;
    }
    if (pspan == null) {
      System.err.println("ScoreContainerSym.makeGraphSym() called, but has no span yet");
      return null;
    }
    BioSeq aseq = pspan.getBioSeq();
    int score_count = scores.length;
    int[] xcoords = new int[2 * score_count];
    float[] ycoords = new float[2 * score_count];
    for (int i=0; i<score_count; i++) {
      IndexedSym isym = (IndexedSym)this.getChild(i);
      if (isym.getIndex() != i) {
	System.err.println("problem in ScoredContainerSym.makeGraphSym(), " +
			   "child.getIndex() not same as child's index in parent child list: " +
			   isym.getIndex() + ", " + i);
      }
      SeqSpan cspan = isym.getSpan(aseq);
      xcoords[i*2] = cspan.getMin();
      ycoords[i*2] = scores[i];
      xcoords[(i*2)+1] = cspan.getMax();
      ycoords[(i*2)+1] = 0;
    }

    if (ensure_unique_id)  { name = GraphSymUtils.getUniqueGraphID(name, aseq); }
    GraphSym gsym = new GraphSym(xcoords, ycoords, name, aseq);
    gsym.getGraphState().setGraphStyle(GraphGlyph.STAIRSTEP_GRAPH);
    gsym.setProperty(GraphSym.PROP_GRAPH_STRAND, new Character('.'));
    return gsym;
  }

  /**
   *  Make a GraphSym, but only with scores for scored intervals in the specified orientation.
   *  @param orientation  true for forward strand intervals.
   *  @see #makeGraphSym(String,boolean)
   */
  public GraphSym makeGraphSym(String name, boolean ensure_unique_id, boolean orientation)  {
    float[] scores = getScores(name);
    SeqSpan pspan = this.getSpan(0);
    if (scores == null) {
      System.err.println("ScoreContainerSym.makeGraphSym() called, but no scores found for: " + name);
      return null;
    }
    if (pspan == null) {
      System.err.println("ScoreContainerSym.makeGraphSym() called, but has no span yet");
      return null;
    }
    BioSeq aseq = pspan.getBioSeq();
    int score_count = scores.length;

    IntList xlist = new IntList(score_count);
    FloatList ylist = new FloatList(score_count);
    int correct_strand_count = 0;
    for (int i=0; i<score_count; i++) {
      IndexedSym isym = (IndexedSym)this.getChild(i);
      if (isym.getIndex() != i) {
	System.err.println("problem in ScoredContainerSym.makeGraphSym(), " +
			   "child.getIndex() not same as child's index in parent child list: " +
			   isym.getIndex() + ", " + i);
      }
      SeqSpan cspan = isym.getSpan(aseq);
      if (cspan.isForward() == orientation) {
	xlist.add(cspan.getMin());
	xlist.add(cspan.getMax());
	ylist.add(scores[i]);
	ylist.add(0);
	correct_strand_count++;
      }
    }
    int[] xcoords = xlist.copyToArray();
    float[] ycoords = ylist.copyToArray();
    if (xcoords.length <= 0) {
      return null;
    }
    else {
      String name_with_strand = name + (orientation ? " (+)" : " (-)" );
      if (ensure_unique_id)  {  name_with_strand = GraphSymUtils.getUniqueGraphID(name_with_strand, aseq); }
      GraphSym gsym = new GraphSym(xcoords, ycoords, name_with_strand, aseq);
      gsym.getGraphState().setGraphStyle(GraphGlyph.STAIRSTEP_GRAPH);
      gsym.setProperty(GraphSym.PROP_GRAPH_STRAND, new Character(orientation ? '+' : '-'));
      return gsym;
    }
  }

}
