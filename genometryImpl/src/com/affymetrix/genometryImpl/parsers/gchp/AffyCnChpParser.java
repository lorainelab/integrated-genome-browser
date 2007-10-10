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
  public List parse(File file, InputStream istr, String stream_name, AnnotatedSeqGroup seq_group,
      boolean ensure_unique_id) throws IOException {
    SingletonGenometryModel.logInfo("Parsing with " + this.getClass().getName() + ": " + stream_name);
    
    ArrayList results = new ArrayList();
    
    try {
      
      AffyGenericChpFile chpFile = AffyGenericChpFile.parse(file, istr, false);
      
      AffyDataGroup group = chpFile.groups.get(0);
      AffyDataSet dataSet = group.getDataSets().get(0);
      
      
      for (String seq_name : dataSet.getChromosomeNames()) {
        // Make sure that all the seq's mentioned in the header are
        // present in the SeqGroup.  Adds them if necessary.
        getSeq(seq_group, seq_name);
      }
            
      //TODO: ids
      String gid = stream_name;
      if (ensure_unique_id)  {
        // Making sure the ID is unique on the whole genome, not just this seq
        // will make sure the GraphState is also unique on the whole genome.
        gid = AnnotatedSeqGroup.getUniqueGraphID(gid, seq_group);
      }
      
      for (AffySingleChromData data : dataSet.getSingleChromData()) {
        MutableAnnotatedBioSeq seq = getSeq(seq_group, data.displayName);
        data.makeGraphs(seq);
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
    
  private MutableAnnotatedBioSeq getSeq(AnnotatedSeqGroup seq_group, String seqid) {
    MutableAnnotatedBioSeq aseq = seq_group.getSeq(seqid);
    if (aseq == null) {
      aseq = seq_group.addSeq(seqid, 1);
    }
    return aseq;
  }
  
  public void testFullRead(String test_file) {
    FileInputStream fis = null;
    BufferedInputStream bis = null;
    AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");

    try {
      File fil = new File(test_file);      
      fis = new FileInputStream(fil);
      bis = new BufferedInputStream(fis);
      System.out.println("START Parse");
      List results = parse(fil, bis, test_file, seq_group, false);
      System.out.println("END Parse");
      System.out.println("");
      
      //this.dump(System.out);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try {fis.close();} catch (Exception e) { e.printStackTrace(); }
      try {bis.close();} catch (Exception e) { e.printStackTrace(); }
    }
  }

  /** Tests the parsing of the given filename. */
  public static void main(String[] args) {
    AffyCnChpParser parser = new AffyCnChpParser();
    String fileName = "C:\\Documents and Settings\\eerwin\\My Documents\\data\\copy_number\\NA06985_GW6_C.cnchp";
    if (args.length > 0) {
      fileName = args[0];
    }
    System.out.println("Testing reading of file: " + fileName);
    parser.testFullRead(fileName);
  }
}
