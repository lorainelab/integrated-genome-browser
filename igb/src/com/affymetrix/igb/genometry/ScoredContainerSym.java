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

package com.affymetrix.igb.genometry;

import java.util.*;
import com.affymetrix.genometry.*;

/**
 *  A SeqSymmetry that can only accept children that are instances of
 *  {@link IndexedSingletonSym}.
 */
public class ScoredContainerSym extends SimpleSymWithProps {
  Map name2scores = new HashMap();
  java.util.List scorevals = new ArrayList();
  java.util.List scorenames = new ArrayList();

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

  public float[] getChildScores(IndexedSingletonSym child, java.util.List scorelist) {
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

  public float[] getChildScores(IndexedSingletonSym child) {
    return getChildScores(child, scorevals);
  }

  /**
   *  Can only accept children that are instances of IndexedSingletonSym.
   */
  public void addChild(SeqSymmetry sym) {
    if (sym instanceof IndexedSingletonSym) {
      IndexedSingletonSym isym = (IndexedSingletonSym)sym;
      int current_index = this.getChildCount();
      isym.setIndex(current_index);
      isym.setParent(this);
      super.addChild(isym);
    }
    else {
      System.err.println("ERROR: cannot add a child to ScoredContainerSym unless it is an IndexedSingletonSym");
    }
  }

}
