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

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSymByte;
import com.affymetrix.genometryImpl.GraphSymFloat;
import com.affymetrix.genometryImpl.GraphSymInt;
import com.affymetrix.genometryImpl.GraphSymShort;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SingletonSymWithProps;
import com.affymetrix.genometryImpl.util.ByteList;
import com.affymetrix.genometryImpl.util.FloatList;
import com.affymetrix.genometryImpl.util.IntList;
import com.affymetrix.genometryImpl.util.ShortList;
import java.io.*;
import java.util.*;

public class AffyCnChpParser {
  
  /** Creates a new instance of AffyCnChpParser */
  public AffyCnChpParser() {
  }
  
  /** Returns a List of GraphSym objects. */
  public List parse(InputStream istr, String stream_name, AnnotatedSeqGroup seq_group,
      boolean ensure_unique_id) throws IOException {
    SingletonGenometryModel.logInfo("Parsing with " + this.getClass().getName() + ": " + stream_name);
    
    ArrayList results = new ArrayList();
    
    try {
      
      AffyGenericChpFile chpFile = AffyGenericChpFile.parse(istr, false);
      
      AffyDataGroup group = chpFile.groups.get(0);
      AffyDataSet dataSet = group.getDataSets().get(0);
      
      List<AffySingleChromData> chromDataList = new ArrayList<AffySingleChromData>(dataSet.byte2chromData.values());
      
      for (AffySingleChromData data : chromDataList) {
        // Make sure that all the seq's mentioned in the header are
        // present in the SeqGroup.  Adds them if necessary.
        getSeq(seq_group, data.displayName);
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
              SingletonSymWithProps sym = new SingletonSymWithProps(data.probeSetNames.get(i), start, start+1, seq);
              sym.setProperty("method", theMethod);
//          tca.addChild(sym);
              seq.addAnnotation(sym);
            }
          }          

          for (AffyChpColumnData colData : data.columns) {
            String graphId = colData.name;
            if (colData.getData() instanceof FloatList) {              
              List<Object> trimmedXandY = trimNaN(positions, (FloatList) colData.getData());
              IntList xlist = (IntList) trimmedXandY.get(0);
              FloatList flist = (FloatList) trimmedXandY.get(1);
              
              xlist.trimToSize();
              flist.trimToSize();
              GraphSymFloat gsym = new GraphSymFloat(xlist.getInternalArray(), flist.getInternalArray(), graphId, seq);
              seq.addAnnotation(gsym);
            } else if (colData.getData() instanceof IntList) {
              IntList ilist = (IntList) colData.getData();
              ilist.trimToSize();
              GraphSymInt gsym = new GraphSymInt(positions.getInternalArray(), ilist.getInternalArray(), graphId, seq);
              seq.addAnnotation(gsym);
            } else if (colData.getData() instanceof ShortList) {
              ShortList ilist = (ShortList) colData.getData();
              ilist.trimToSize();
              GraphSymShort gsym = new GraphSymShort(positions.getInternalArray(), ilist.getInternalArray(), graphId, seq);
              seq.addAnnotation(gsym);
            } else if (colData.getData() instanceof ByteList) {
              ByteList ilist = (ByteList) colData.getData();
              ilist.trimToSize();
              GraphSymByte gsym = new GraphSymByte(positions.getInternalArray(), ilist.getInternalArray(), graphId, seq);
              seq.addAnnotation(gsym);
            } else {
              SingletonGenometryModel.logError("Don't know how to make a graph for data of type: " + colData.type);
            }
          }
        }
        
        
      }
      
    } catch (Exception e) {
      if (! (e instanceof IOException)) {
        IOException ioe = new IOException("IOException for file: " + stream_name);
        e.printStackTrace();
        ioe.initCause(e);
        throw ioe;
      }
    }
    
    return results;
  }
  
  
  /** Removes x,y pairs where the y-value is invalid (NaN or Infinite).
   *  Returns a List containing one IntList and one FloatList. 
   *  If there were no invalid values of y, the output IntList and FloatList
   *  will be the same objects as the input, otherwise they will both be
   *  new objects.  If the given FloatList contains ONLY invalid values,
   *  then the returned IntList and FloatList will both be empty.
   */
  List<Object> trimNaN(IntList x, FloatList y) {
    if (x.size() != y.size()) {
      throw new IllegalArgumentException("Lists must be the same size");
    }

    boolean had_bad_values = false;
    IntList x_out = new IntList(x.size());
    FloatList y_out = new FloatList(y.size());
        
    for (int i=0; i<x.size(); i++) {
      float f = y.get(i);
      if (Float.isNaN(f) || Float.isInfinite(f)) {
        had_bad_values = true;
      } else {
        x_out.add(x.get(i));
        y_out.add(f);
      }
    }
    
    if (had_bad_values) {
      return Arrays.<Object>asList(x_out, y_out);
    } else {
      return Arrays.<Object>asList(x, y);
    }
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
      
      List results = parse(fis, test_file, seq_group, true);
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
