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

import com.affymetrix.genometryImpl.parsers.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AffyDataGroup {
  
  /** File position of the NEXT data group. */
  long file_pos;

  /** File position of the first data set within the group. */
  long file_first_dataset_pos;

  /** Number of data sets within the data group. */
  int num_datasets;

  /** Name of the data group. */
  String name;

  List<AffyDataSet> dataSets = new ArrayList<AffyDataSet>();
  
  /** Creates a new instance of AffyDataGroup */
  protected AffyDataGroup() {
  }
  
  public AffyDataGroup(int pos, int data_pos, int sets, String name) {
    this.file_first_dataset_pos = data_pos;
    this.file_pos = pos;
    this.num_datasets = sets;
    this.name = name;
  }
  
  public static AffyDataGroup parse(DataInputStream dis) throws IOException {
    AffyDataGroup group = new AffyDataGroup();
    
    group.file_pos = dis.readInt(); // TODO: UINT32
    group.file_first_dataset_pos = dis.readInt(); // TODO: UINT32
    group.num_datasets = dis.readInt(); // INT32
    group.name = AffyGenericChpFile.parseWString(dis);
    
    for (int i=0; i<group.num_datasets; i++) {
      AffyDataSet data = AffyDataSet.parse(dis);
      group.dataSets.add(data);
    }
    
    return group;
  }
  
  public List<AffyDataSet> getDataSets() {
    return dataSets;
  }
  
  public String toString() {
    return "AffyDataGroup: pos: " + file_pos + ", first_dataset_pos: " + file_first_dataset_pos 
        + ", datasets: " + num_datasets + ", name: " + name;
  }
  
}
