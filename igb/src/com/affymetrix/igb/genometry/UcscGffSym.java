/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
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
 *  A sym to efficiently store GFF 1.0 annotations. 
 *  See http://genome.ucsc.edu/goldenPath/help/customTrack.html#GTF
 */
public class UcscGffSym extends SingletonSymWithProps { 
  final static boolean GFF_BASE1 = true;

  public static final float UNKNOWN_SCORE = Float.NEGATIVE_INFINITY;
  public static final char UNKNOWN_FRAME = '.';
  
  String source;
  String feature_type;
  float score;
  char frame;
  String group;

  /**
   * Constructor.
   * The coordinates should be given exactly as they appear in a GFF file.
   * In principle, the first coordinate is supposed to be less than the second one,
   * but in practice this isn't always followed, so this constructor will correct
   * those errors and will also convert from interbase-0 to base-1 coordinates.
   * @param coord_A  The coordinate in column 4 of the GFF file.
   * @param coord_B  The coordinate in column 5 of the GFF file.
   */
  public UcscGffSym(BioSeq seq, String source, int coord_A, int coord_B, 
                    float score, char strand, char frame, String group_field) {
    super(0, 0, seq);

    // GFF spec says coord_A <= coord_B, but this is not always obeyed
    int max = Math.max(coord_A, coord_B);
    int min = Math.min(coord_A, coord_B);
    if (GFF_BASE1) { // convert from base-1 numbering to interbase-0 numbering
      min--;
    }
    
    if (strand == '-') {
      setCoords(max, min);
    } else {
      setCoords(min, max);
    }
    
    this.source = source;
    this.feature_type = feature_type;
    this.score = score;
    this.frame = frame;
    this.group = group_field;
  }

  public String getSource()  { return source; }
  public String getFeatureType()  { return feature_type; }
  public float getScore()  { return score; }
  public char getFrame()  { return frame; }
  public String getGroup()  { return group; }
  

  public Object getProperty(String name) {
    if (name.equals("source") || name.equals("method")) { return source; }
    else if (name.equals("feature_type")) { return feature_type; }
    else if (name.equals("type")) { return feature_type; }
    else if (name.equals("score") && score != UNKNOWN_SCORE) { return new Float(score); }
    else if (name.equals("frame") && frame != UNKNOWN_FRAME) { return new Character(frame); }
    else if (name.equals("group")) { return group; }
    else if (name.equals("seqname")) { return getID(); }
    else return super.getProperty(name);
  }

  public Map getProperties() {
    return cloneProperties();
  }

  public Map cloneProperties() {
    Map tprops = super.cloneProperties();
    if (tprops == null) {
      tprops = new HashMap();
    }
    tprops.put("id", getID());
    tprops.put("seqname", getID());
    if (source != null) { tprops.put("source", source); }
    if (source != null) { tprops.put("method", source); }
    if (feature_type != null) { tprops.put("feature_type", feature_type); }
    if (feature_type != null) { tprops.put("type", feature_type); }
    if (score != UNKNOWN_SCORE) {
      tprops.put("score", new Float(getScore()));
    }
    if (frame != UNKNOWN_FRAME) {
      tprops.put("frame", new Character(frame));
    }
    tprops.put("group", group);
    return tprops;
  }
}
