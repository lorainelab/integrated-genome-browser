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
 *  Efficient implementation of a basic SeqSymmetry that is
 *  of breadth 1 (1 SeqSpan per symmetry) and depth 2 (has one
 *  level of SeqSymmetry children.
 *
 *  Similar to UcscGeneSym and UcscPslSym, but with most of the
 *    extras stripped away
 */
public class Efficient2LevelSym {
  String id;
  String type;  // ???
  BioSeq seq;
  int min;
  int max;
  boolean forward;
  int[] cmins;
  int[] cmaxs;

  public Efficient2LevelSym(String type, String id, BioSeq seq,
			    boolean forward, int min, int max, int[] cmins, int[] cmaxs) {
    this.type = type;
    this.id = id;
    this.seq = seq;
    this.forward = forward;
    this.min = min;
    this.max = max;
    this.cmins = cmins;
    this.cmaxs = cmaxs;
  }
}
