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
 *  A sym to efficiently store GFF 1.0 annotations. 
 *  See http://genome.ucsc.edu/goldenPath/help/customTrack.html#GTF
 */
public class UcscGffSym extends SingletonSymWithProps { 
  String source;
  String feature_type;
  float score;
  int frame;
  String group;

  public UcscGffSym(BioSeq seq, String source, String feature_type, int start, int end, 
		    float score, int frame, String group) {
    super(start, end, seq);
    this.source = source;
    this.feature_type = feature_type;
    this.score = score;
    this.frame = frame;
    this.group = group;
  }

  public String getSource()  { return source; }
  public String getFeatureType()  { return feature_type; }
  public float getScore()  { return score; }
  public int getFrame()  { return frame; }
  public String getGroup()  { return group; }
  

  public Object getProperty(String name) {
    if (name.equals("source") || name.equals("method")) { return source; }
    else if (name.equals("feature_type")) { return feature_type; }
    else if (name.equals("type")) { return feature_type; }
    else if (name.equals("score")) { return new Float(score); }
    else if (name.equals("frame")) { return new Integer(frame); }
    else if (name.equals("group")) { return group; }
    else if (name.equals("seqname")) { return seq.getID(); }
    else return super.getProperty(name);
  }

  public Map getProperties() {
    return cloneProperties();
  }

  public Map cloneProperties() {
    HashMap tprops = new HashMap();
    tprops.put("id", seq.getID());
    tprops.put("seqname", seq.getID());
    tprops.put("source", source);
    tprops.put("method", source);
    tprops.put("feature_type", feature_type);
    tprops.put("type", feature_type);
    tprops.put("score", new Float(getScore()));
    tprops.put("frame", new Integer(getFrame()));
    tprops.put("group", group);
    if (props != null) {
      tprops.putAll(props);
    }
    return tprops;
  }
    
}
