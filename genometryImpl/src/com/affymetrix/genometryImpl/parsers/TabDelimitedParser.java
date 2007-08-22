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
import java.util.regex.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.util.*;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonSymWithProps;
import com.affymetrix.genometryImpl.SimpleSymWithProps;

/**
 *  Attempts to parse "generic" tab delimited data.
 *  For example excel spreadsheets (in tab format).
 *  Which columns to use for start, end, etc. are specified in the constructor.
 */
public class TabDelimitedParser {
  int chromosome_col;
  int start_col;
  int end_col;     // should need only end_col or length_col, not both
  int length_col;  // should need only end_col or length_col, not both
  int strand_col;  // column to use for determining strand
  int group_col;  // column to use for grouping features...
  int type_col;   // column to use for setting feature type
  
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
  boolean use_group = false;
  boolean use_type = false;
  boolean use_strand = false;
  boolean has_header = false;
  
  String default_type = "unknown_type";
  static final Pattern line_splitter = Pattern.compile("\t");
  
  public void setSeqColumn(int col) { seq_col = col; }
  public void setStartColumn(int col) { start_col = col; }
  public void setEndColumn(int col) { end_col = col; }
  public void setStrandColumn(int col) { strand_col = col; }
  public void setLengthColumn(int col) { length_col = col; }
  
  public void setSeqColumn2(int col) { seq_col2 = col; }
  public void setStartColumn2(int col) {  start_col2 = col; }
  public void setEndColumn2(int col) { end_col2 = col; }
  
  public void setGroupColumn(int col) { group_col = col; }
  public void setTypeColumn(int col) { type_col = col; }
  public void setPropertyColumn(int col, String prop_name) {
    
  }
  
  /**
   *  Constructor.
   *  Each argument tells which column to find a particular item in.
   *  -1 for any arg indicates that it is not present in the table.
   */
  public TabDelimitedParser(int type, int chromosome, int start, int end, int length,
      int strand, int group, boolean props, boolean header, boolean addToIndex) {
    
    if (chromosome < 0) {
      throw new IllegalArgumentException("Chromosome column number must be 0 or greater.");
    }
    
    chromosome_col = chromosome;
    start_col = start;
    end_col = end;
    length_col = length;
    group_col = group;
    type_col = type;
    strand_col = strand;
    this.addToIndex = addToIndex;
    
    has_header = header;
    use_length = (length >= 0);
    use_group = (group >= 0);
    use_type = (type >=0);
    use_strand = (strand >= 0);
    
    this.make_props = props;
  }
  
  public TabDelimitedParser(String type, int chromosome, int start, int end, int length,
      int strand, int group, boolean props, boolean header, boolean addToIndex) {
    this(-1, chromosome, start, end, length, strand, group, props, header, addToIndex);
    default_type = type;
  }
  
  public void parse(InputStream istr, AnnotatedSeqGroup seq_group) {
    
    HashMap group_hash = new HashMap();
    MutableSeqSpan union_span = new SimpleMutableSeqSpan();
    ArrayList col_names = null;
    //    System.out.println("use_type: " + use_type);
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
        String type = default_type;
        if (use_type) {
          type = cols[type_col];
          //	  System.out.println("type = " + type);
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
        
        SingletonSymWithProps child = new SingletonSymWithProps(start, end, seq);
        child.setProperty("method", type);
        String id = type + " " + seq.getID() + ":" + start + "-" + end;
        child.setProperty("id", id);
        if (make_props) {
          for (int i=0; i<cols.length && i<col_names.size(); i++) {
            String name = (String)col_names.get(i);
            String val = cols[i];
            child.setProperty(name, val);
          }
        }
        
        if (use_group) {
          String group = cols[group_col];
          //	  SingletonSymWithProps parent = (SingletonSymWithProps)group_hash.get(group);
          SimpleSymWithProps parent = (SimpleSymWithProps)group_hash.get(group);
          if (parent == null) {
            //	    parent = new SingletonSymWithProps(start, end, aseq);
            //	    parent = new SimpleSymWithProps(start, end, aseq);
            parent = new SimpleSymWithProps();
            SimpleMutableSeqSpan span = new SimpleMutableSeqSpan(start, end, seq);
            parent.addSpan(span);
            //	    System.out.println("parent type = " + type);
            parent.setProperty("method", type);
            id = type + " " + span.getBioSeq().getID() + ":" + span.getStart() + "-" + span.getEnd();
            
            parent.setProperty("id", id);
            group_hash.put(group, parent);
            // or maybe should add all parents to a grandparent, and add _grandparent_ to aseq???
            seq.addAnnotation(parent);
            seq_group.addToIndex(parent.getID(), parent);
          } else {
            MutableSeqSpan pspan = (MutableSeqSpan)parent.getSpan(seq);
            //	    SeqUtils.encompass((SeqSpan)parent, (SeqSpan)child, union_span);
            SeqUtils.encompass(pspan, (SeqSpan)child, union_span);
            //	    System.out.println("expanding parent");
            //	    parent.set(union_span.getStart(), union_span.getEnd(), aseq);
            pspan.set(union_span.getStart(), union_span.getEnd(), seq);
          }
          parent.addChild(child);
        } else {
//          SingletonSymWithProps sym = new SingletonSymWithProps(start, end, seq);
//          sym.setProperty("method", type);
          seq.addAnnotation(child);
          seq_group.addToIndex(child.getID(), child);
        }
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
    
    String fil = System.getProperty("user.dir") + "/data/copy_number/DUKE_US_DukeCNV_NSP_T24_1_1.rpt";
    // type, start, end, length, strand, group, boolean props, boolean has_header
    TabDelimitedParser tester = new TabDelimitedParser(0, 1, 9, 10, -1, -1, -1, true, true, true);
    try {
      FileInputStream fis = new FileInputStream(new File(fil));
      AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");
      
      tester.parse(fis, seq_group);
      
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
