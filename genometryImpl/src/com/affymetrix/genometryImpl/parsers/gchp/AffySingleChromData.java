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

package com.affymetrix.genometryImpl.parsers.gchp;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
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
import com.affymetrix.igb.Application;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class AffySingleChromData {
  
  int start;
  int rowCount;
  int chromNum; // in the chp file, a chromosome is indicated by an arbitrary number, actually a byte, not an int
  String displayName; // AND by a display name.
  ArrayList<CharSequence> probeSetNames = new ArrayList<CharSequence>();
  IntList positions = new IntList();
  
  List<AffyChpColumnData> columns = new ArrayList<AffyChpColumnData>();
  AffyGenericChpFile chpFile;
  AffyDataSet dataSet;
  
  /** Creates a new instance of SingleChromosomeData */
  public AffySingleChromData(AffyGenericChpFile chpFile, AffyDataSet dataSet, 
    int chromNum, String chromDisplayName, int start, int count, 
    List<AffyChpColumnData> columns) {
    this.chpFile = chpFile;
    this.dataSet = dataSet;
    this.chromNum = chromNum;
    this.displayName = chromDisplayName;
    this.start = start;
    this.rowCount = count;
    this.columns = columns;
  }

  @Override
  public String toString() {
    return this.getClass().getName() + " [displayName=" + displayName 
      + ", start=" + start + ", count="
      + rowCount + ", columns=" + columns.size()+"]";
  }
  
  void parse(DataInputStream dis) throws IOException {
    Application.logDebug("Parsing chromData: " + this.displayName + ", " + this.rowCount);
    for (int row=0; row < rowCount; row++) {
      CharSequence probeSetName = AffyGenericChpFile.parseString(dis);
      byte readChromNum = dis.readByte(); //treat as unsigned, but doesn't matter here
        // chromNum is redundant information.  We already know the chromosomeDisplayName
      if (readChromNum != chromNum) {
        throw new IOException("Chromosome number doesn't match expected value");
      }
      
      
      int position = dis.readInt(); //to be interpreted as unsigned, but store for now as int
      
      positions.add(position);
      probeSetNames.add(probeSetName);

      for (AffyChpColumnData col : columns) {
        col.addData(dis);
      }
    }
    positions.trimToSize();
    probeSetNames.trimToSize();
  }

  /** Creates a GraphSym and adds it as an annotation to the BioSeq. */
  void makeGraphs(MutableAnnotatedBioSeq seq) throws IOException {
    positions.trimToSize();

    if (positions.size() > 0) {
      boolean addSingletonSyms = false;
      if (addSingletonSyms) {
        String theMethod = "posistions";
//        TypeContainerAnnot tca = new TypeContainerAnnot(theMethod);
//        tca.setProperty("method", theMethod);
//        tca.setID(theMethod);
//        seq.addAnnotation(tca);
        for (int i = 0; i < positions.size(); i++) {
          //TODO: insn't there a class that accepts an IntList as the set of positions?
          final int start_pos = positions.get(i);
          SingletonSymWithProps sym = new SingletonSymWithProps(this.probeSetNames.get(i), start_pos, start_pos + 1, seq);
          sym.setProperty("method", theMethod);
//          tca.addChild(sym);
          seq.addAnnotation(sym);
        }
      }

      for (AffyChpColumnData colData : this.columns) {
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
  
  /** Removes x,y pairs where the y-value is invalid (NaN or Infinite).
   *  Returns a List containing one IntList and one FloatList. 
   *  If there were no invalid values of y, the output IntList and FloatList
   *  will be the same objects as the input, otherwise they will both be
   *  new objects.  If the given FloatList contains ONLY invalid values,
   *  then the returned IntList and FloatList will both be empty.
   */
  List<Object> trimNaN(IntList x, FloatList y) {
    if (x.size() != y.size()) {
      throw new IllegalArgumentException("Lists must be the same size " + x.size() + " != " + y.size());
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
}
