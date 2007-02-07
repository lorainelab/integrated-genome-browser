/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.Scored;
import com.affymetrix.igb.genometry.SingletonSymWithProps;
import com.affymetrix.igb.genometry.SmartAnnotBioSeq;
import com.affymetrix.igb.glyph.HeatMap;
import java.awt.Color;
import java.io.*;
import java.util.regex.Pattern;

/**
 *  A parser designed to parse data from cytoBand.txt files from UCSC.
 */
public class CytobandParser {

  static Pattern line_regex = Pattern.compile("\\t");

  static final HeatMap cyto_heat_map = HeatMap.makeLinearHeatmap("cytobands", Color.WHITE, Color.BLACK);
  static final Color cyto_acen_color = new Color(198,64,64); // dark red
  static final Color cyto_stalk_color = Color.GRAY;
      
  public static final String CYTOBAND_TIER_NAME = "Cytobands";
  public static final float GNEG_SCORE = 100.0f;
  public static final float GVAR_SCORE = 100.0f;
  public static final float ACEN_SCORE = 600.005f;
  public static final float STALK_SCORE = 500.005f;
  
  public CytobandParser() {
  }

  public void parse(InputStream dis, AnnotatedSeqGroup seq_group)
  throws IOException  {
    String line;
    
    Thread thread = Thread.currentThread();
    BufferedReader reader = new BufferedReader(new InputStreamReader(dis));
    while ((line = reader.readLine()) != null && (! thread.isInterrupted())) {
      if (line.startsWith("#") || "".equals(line)) {  // skip comment lines
        continue;
      } else {
        String[] fields = line_regex.split(line);
        int field_count = fields.length;
        String seq_name = null;
        String annot_name = null;
        String band = null;
        int min, max;
        float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
        boolean forward;
        
        if (fields == null || field_count != 5) {
          throw new IOException("Line has wrong number of data columns.");
        }
        
        seq_name = fields[0]; // seq id field
        MutableAnnotatedBioSeq seq = seq_group.getSeq(seq_name);
        if (seq == null) {
          System.out.println("seq not recognized, creating new seq: " + seq_name);
          seq = seq_group.addSeq(seq_name, 0);
        }
        
        int beg = Integer.parseInt(fields[1]);  // start field
        int end = Integer.parseInt(fields[2]);  // stop field
        annot_name = new String(fields[3]);
        band = new String(fields[4]);
        
        if (beg > seq.getLength()) {
          seq.setLength(beg);
        }
        if (end > seq.getLength()) {
          seq.setLength(end);
        }
        
        CytobandSym sym = new CytobandSym(beg, end, seq, annot_name, band);    
        ((SmartAnnotBioSeq) seq).addAnnotation(sym, CYTOBAND_TIER_NAME);
        
        if (annot_name != null) {
          seq_group.addToIndex(annot_name, sym);
        }
      }  // end of line.startsWith() else
    }   // end of line-reading loop
  }

  public static final String BAND_STALK = "stalk";
  public static final String BAND_ACEN = "acen";
  
  public static float parseScore(String s) {
    if ("gneg".equals(s)) {
      return GNEG_SCORE;
    } else if ("gvar".equals(s)) {
      return GVAR_SCORE;
    } else if (BAND_ACEN.equals(s)) {
      return ACEN_SCORE;
    } else if (BAND_STALK.equals(s)) {
      return STALK_SCORE;
    } else if (s.startsWith("gpos")) {
      // "gpos50" == 500
      // "gpos100" == 1000
      // etc.
      return 10.0f * Integer.parseInt(s.substring(4));
    }
    else return 0.0f;
  }
  
  public class CytobandSym extends SingletonSymWithProps implements Scored {
    String band;
    public CytobandSym(int start, int end, BioSeq seq, String name, String band) {
      super(start, end, seq);
      this.band = band;
      this.id = name;
    }

    public float getScore() {
      return parseScore(band);
    }
    
    public String getBand() {
      return band;
    }
    
    public Color getColor() {
      if ("acen".equals(band)) {
        return cyto_acen_color;
      } else if ("stalk".equals(band)) {
        return cyto_stalk_color;
      } else {
        float score = parseScore(band);
        return cyto_heat_map.getColors()[(int) (255 * 0.001 * score)];
      }
    }
    
    public boolean setProperty(String name, Object val) {
      if ("band".equals(name)) {
        band = name;
      } else {
        return super.setProperty(name, val);
      }
      return true;
    }
    
    public Object getProperty(String name) {
      if ("id".equals(name)) {
        return getID();
      }
      if ("method".equals(name)) {
        return CYTOBAND_TIER_NAME;
      }
      else if ("band".equals(name)) {
        return band;
      }
      else return super.getProperty(name);
    }
    
  }
}
