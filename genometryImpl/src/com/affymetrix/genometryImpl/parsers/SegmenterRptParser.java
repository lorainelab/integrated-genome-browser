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

package com.affymetrix.genometryImpl.parsers;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.util.*;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonSymWithProps;
import com.affymetrix.genometryImpl.SimpleSymWithProps;

/**
 *  Parses tab-delimited output data from the Copy Number "Segmenter" program.
 *  The file extension is usually ".rpt".
 *
 *  (This was based on the TabDelimitedParser, but then specialized.)
 */
public class SegmenterRptParser {
  public static final String DEL = "del";
  public static final String DUP = "dup";

  int chromosome_col;
  int start_col;
  int end_col;     // should need only end_col or length_col, not both
  int length_col;  // should need only end_col or length_col, not both
  int strand_col;  // column to use for determining strand
  int cn_change_col;  // column to use for determining cn change type ("dup", "del", etc.)
  int sample_col;  // column to use for sample name
  
  boolean addToIndex; // whether to add annotation id's to the index on the seq group
  
  int seq_col;
  int seq_col2;
  int start_col2;
  int end_col2;     // should need only end_col or length_col, not both
  int strand_col2;  // column to use for determining strand
  
  // if makeProps, then each column (other than start, end, length, group) will become a
  //    property in the SymWithProps that is generated
  boolean make_props = true;
  
  boolean use_length = false;
  boolean use_strand = false;
  boolean has_header = false;
  
  static final Pattern line_splitter = Pattern.compile("\t");
    
  public SegmenterRptParser() {
    this(true, true);
  }
  
  public SegmenterRptParser(boolean props, boolean addToIndex) {
    
    this.chromosome_col = 1;
    this.start_col = 9;
    this.end_col = 10;
    this.length_col = -1;
    this.sample_col = 0;
    this.strand_col = 0;
    this.cn_change_col = 4;
    this.addToIndex = addToIndex;
    
    this.has_header = true;
        
    this.use_length = (this.length_col >= 0);
    this.use_strand = (this.strand_col >= 0);
    
    this.make_props = props;
  }
  
  public void parse(InputStream istr, String default_type, AnnotatedSeqGroup seq_group) { 
    
    HashMap group_hash = new HashMap();
    MutableSeqSpan union_span = new SimpleMutableSeqSpan();
    ArrayList col_names = null;

    try {
      InputStreamReader asr = new InputStreamReader(istr);
      BufferedReader br = new BufferedReader(asr);
      String line;
      if (has_header) {
        line = br.readLine();
        String[] cols = line_splitter.split(line);
        col_names = new ArrayList(cols.length);
        for (int i=0; i<cols.length; i++) {
          col_names.add(cols[i]);
        }
      }
      while ((line = br.readLine()) != null) {
        
        String[] cols = line_splitter.split(line);
        if (cols.length <= 0) { continue; }
        
        int start = Integer.parseInt(cols[start_col]);
        int end;
        if (use_length) {
          int length = Integer.parseInt(cols[length_col]);
          if (use_strand) {
            String strand = cols[strand_col];
            //	    boolean revstrand = strand.equals("-");
            if (strand.equals("-")) { 
              end = start - length; 
            } 
            else {
              end = start + length; 
            }
          } else {
            end = start + length;
          }
        } else {
          end = Integer.parseInt(cols[end_col]);
        }
        
        String chromName = cols[chromosome_col];
        MutableAnnotatedBioSeq seq = seq_group.getSeq(chromName);
        if (seq == null) {
          seq = seq_group.addSeq(chromName, 0);
        }
        
        if (seq.getLength() < end) {
          seq.setLength(end);
        }
        if (seq.getLength() < start) {
          seq.setLength(start);
        }
        
        String sample = cols[sample_col];
        if (sample == null || sample.trim().length() == 0) {
          sample = default_type;
        }
        
        String change_type = cols[cn_change_col];
        
        SingletonSymWithProps sym = new SingletonSymWithProps(start, end, seq);
        sym.setProperty("method", change_type);
        String id = sample + " " + seq.getID() + ":" + start + "-" + end;
        sym.setProperty("id", id);

        if (make_props) {
          for (int i=0; i<cols.length && i<col_names.size(); i++) {
            String name = (String)col_names.get(i);
            String val = cols[i];
            sym.setProperty(name, val);
          }
        }
        
        seq.addAnnotation(sym);
        seq_group.addToIndex(id, sym);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return;
  }
  
  public static void main(String[] args) {
// 0 Sample
// 1 Chr
// 2 Cytoband_Start_Pos
// 3 Cytoband_End_Pos
// 4 CN_ChangeType
// 5 Size(kb)
// 6 CN_State
// 7 Start_ProbeSet
// 8 End_ProbeSet
// 9 Start_Physical_Pos
// 10 End_Physical_Position
// 11 #ProbeSet
// 12 %ProbeSets_withCNV
// 13 CNV_Annotation
    
    String filname = System.getProperty("user.dir") + "/data/copy_number/DUKE_US_DukeCNV_NSP_T24_1_1.rpt";
    File file = new File(filname);
    // type, start, end, length, strand, group, boolean props, boolean has_header
    SegmenterRptParser tester = new SegmenterRptParser();
    try {
      FileInputStream fis = new FileInputStream(file);
      AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");
      
      tester.parse(fis, file.getName(), seq_group);
      
      for (int s=0; s<seq_group.getSeqCount(); s++) {
        MutableAnnotatedBioSeq aseq = seq_group.getSeq(s);
        for (int i=0; i<aseq.getAnnotationCount(); i++) {
          SeqSymmetry annot = aseq.getAnnotation(i);
          SeqUtils.printSymmetry(annot, "  ", true);
        }
      }
      
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
  
  
}
