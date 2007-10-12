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
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.parsers.Streamer;
import java.io.*;
import java.util.*;

public class AffyCnChpParser {
  
  /** Creates a new instance of AffyCnChpParser */
  public AffyCnChpParser() {
  }
  
  public void parse(File file, ChromLoadPolicy loadPolicy, InputStream istr, String stream_name, AnnotatedSeqGroup seq_group) throws IOException {
    SingletonGenometryModel.logInfo("Parsing with " + this.getClass().getName() + ": " + stream_name);
    
    //ArrayList results = new ArrayList();
    
    try {
      
      AffyGenericChpFile chpFile = AffyGenericChpFile.parse(file, loadPolicy, istr, false);
      
      AffyDataGroup group = chpFile.groups.get(0);
      AffyDataSet dataSet = group.getDataSets().get(0);
      
      
      for (String seq_name : dataSet.getChromosomeNames()) {
        // Make sure that all the seq's mentioned in the header are
        // present in the SeqGroup.  Adds them if necessary.
        getSeq(seq_group, seq_name);
      }            
      
      for (AffySingleChromData data : dataSet.getSingleChromData()) {
        MutableAnnotatedBioSeq seq = getSeq(seq_group, data.displayName);
        List<SeqSymmetry> syms = data.makeGraphs(seq);
        for (SeqSymmetry sym : syms) {
          seq.addAnnotation(sym); 
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
    
    //return results;
  }

  /** Parses the file, subject to the loadPolicy, and returns a 
   *  list of AffySingleChromData objects. 
   *  Does not create graphs or add any annotations to any BioSeq.
   *  If you do not do keep a reference the returned data, it may be garbage collected.
   */
  public List<AffySingleChromData> parse2(File file, ChromLoadPolicy loadPolicy) throws IOException {
    SingletonGenometryModel.logInfo("Parsing file with " + this.getClass().getName() + ": " + file.getName());
    
    ArrayList<AffySingleChromData> results = new ArrayList<AffySingleChromData>();
    InputStream istr = new BufferedInputStream(new FileInputStream(file));
    
    try {
      
      AffyGenericChpFile chpFile = AffyGenericChpFile.parse(file, loadPolicy, istr, false);
      
      AffyDataGroup group = chpFile.groups.get(0);
      AffyDataSet dataSet = group.getDataSets().get(0);
      
                  
      for (AffySingleChromData data : dataSet.getSingleChromData()) {
        if (loadPolicy.shouldLoadChrom(data.displayName)) {
          results.add(data);
        }
      }
      
    } catch (Exception e) {
      if (! (e instanceof IOException)) {
        IOException ioe = new IOException("IOException for file: " + file);
        ioe.initCause(e);
        throw ioe;
      }
    } finally {
      istr.close(); // if there is an exception, we probably do want to know about it.
      // because it might prevent us from opening the file again later.
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
      ChromLoadPolicy loadPolicy = ChromLoadPolicy.getLoadAllPolicy();
      fis = new FileInputStream(fil);
      bis = new BufferedInputStream(fis);
      System.out.println("START Parse");
      parse(fil, loadPolicy, bis, test_file, seq_group);
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
