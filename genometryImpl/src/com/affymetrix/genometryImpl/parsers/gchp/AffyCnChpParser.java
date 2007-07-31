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

/** A parser for the Affymetrix Generic CHP files containing copy number data. */
package com.affymetrix.genometryImpl.parsers.gchp;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSymFloat;
import com.affymetrix.genometryImpl.GraphSymInt;
import com.affymetrix.genometryImpl.SingletonSymWithProps;
import com.affymetrix.genometryImpl.util.FloatList;
import com.affymetrix.genometryImpl.util.IntList;
import java.io.*;
import java.util.*;

public class AffyCnChpParser {
  
  /** Creates a new instance of AffyCnChpParser */
  public AffyCnChpParser() {
  }
  
  /** Returns a List of GraphSym objects. */
  public List parse(InputStream istr, String stream_name, AnnotatedSeqGroup seq_group,
      boolean annotate_seq, boolean ensure_unique_id)
      throws IOException {
    System.out.println("Parsing with " + this.getClass().getName() + ": " + stream_name);
    
    ArrayList results = new ArrayList();
    
    try {
      
      AffyGenericChpFile chpFile = AffyGenericChpFile.parse(istr, false);
      
      AffyDataGroup group = chpFile.groups.get(0);
      AffyDataSet dataSet = group.getDataSets().get(0);
      
      List<AffySingleChromData> chromDataList = new ArrayList<AffySingleChromData>(dataSet.byte2chromData.values());
      
      for (AffySingleChromData data : chromDataList) {
        //System.out.println("Chromosome " + data.displayName);
        //System.out.println("Start: " + data.start);
        //System.out.println("Offset: " + data.count);
        MutableAnnotatedBioSeq seq = getSeq(seq_group, data.displayName);
      }
            
      //TODO: ids
      String gid = stream_name;
      if (ensure_unique_id)  {
        // Making sure the ID is unique on the whole genome, not just this seq
        // will make sure the GraphState is also unique on the whole genome.
        gid = AnnotatedSeqGroup.getUniqueGraphID(gid, seq_group);
      }
      
      boolean addSingletonSyms = false;
      
      for (AffySingleChromData data : chromDataList) {
        MutableAnnotatedBioSeq seq = getSeq(seq_group, data.displayName);
        
        data.columns.get(0);
        
        IntList positions = data.positions;
        positions.trimToSize();
        
        if (positions.size() > 0) {
          if (addSingletonSyms) {
            String theMethod = "posistions:"+stream_name;
//        TypeContainerAnnot tca = new TypeContainerAnnot(theMethod);
//        tca.setProperty("method", theMethod);
//        tca.setID(theMethod);
//        seq.addAnnotation(tca);
            for (int i=0; i<positions.size(); i++) {
              //TODO: insn't there a class that accepts an IntList as the set of positions?
              final int start = positions.get(i);
              SingletonSymWithProps sym = new SingletonSymWithProps(data.probeSetNames.get(i), (int) start, (int) start+1, seq);
              sym.setProperty("method", theMethod);
//          tca.addChild(sym);
              seq.addAnnotation(sym);
            }
          }          

          for (AffyChpColumnData colData : data.columns) {
            String graphId = colData.name;
            if (colData.getData() instanceof FloatList) {
              FloatList flist = (FloatList) colData.getData();
              flist.trimToSize();
              GraphSymFloat gsym = new GraphSymFloat(positions.getInternalArray(), flist.getInternalArray(), graphId, seq);
              seq.addAnnotation(gsym);
            } else if (colData.getData() instanceof IntList) {
              IntList ilist = (IntList) colData.getData();
              ilist.trimToSize();
              GraphSymInt gsym = new GraphSymInt(positions.getInternalArray(), ilist.getInternalArray(), graphId, seq);
              seq.addAnnotation(gsym);
            } else {
              System.out.println("Don't know how to make a graph for data of type: " + colData.type);
            }
          }
        }
        
        
      }
      
    } catch (Exception e) {
      if (! (e instanceof IOException)) {
        IOException ioe = new IOException("IOException for file: " + stream_name);
        ioe.initCause(e);
        throw ioe;
      }
    }
    
    return results;
  }
  
  private MutableAnnotatedBioSeq getSeq(AnnotatedSeqGroup seq_group, String seqid) {
    MutableAnnotatedBioSeq aseq = seq_group.getSeq(seqid);
    if (aseq == null) {
      aseq = seq_group.addSeq(seqid, 1);
    }
    return aseq;
  }
  
  public void testFullRead(String test_file) {
    FileInputStream fis = null;
    AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");

    try {
      File fil = new File(test_file);      
      fis = new FileInputStream(fil);
      System.out.println("START Parse");
      
      List results = parse(fis, test_file, seq_group, true, true);
      System.out.println("END Parse");
      System.out.println("");
      
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try {fis.close();} catch (Exception e) { e.printStackTrace(); }
    }
  }

  /** Tests the parsing of the given filename. */
  public static void main(String[] args) {
    AffyCnChpParser parser = new AffyCnChpParser();
    String fileName = "C:\\Documents and Settings\\eerwin\\My Documents\\NA06985_GW6_C.cnchp";
    if (args.length > 0) {
      fileName = args[0];
    }
    System.out.println("Testing reading of file: " + fileName);
    parser.testFullRead(fileName);
  }
}
