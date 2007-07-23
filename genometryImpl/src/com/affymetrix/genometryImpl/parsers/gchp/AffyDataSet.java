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

import com.affymetrix.genometryImpl.util.*;
import java.io.*;
import java.util.*;

public class AffyDataSet {
  
  long pos_first_data_element;
  long pos_next_data_element;
  String name;
  int param_count;
  List<AffyChpParameter> params;
  long num_columns;
  List<AffyChpColumnType> columns;
  long num_rows;
  
  
  /** Creates a new instance of AffyDataSet */
  protected AffyDataSet() {
  }
    
  public static AffyDataSet parse(DataInputStream dis) throws IOException {
    AffyDataSet a = new AffyDataSet();
    
    a.pos_first_data_element = dis.readInt();
    a.pos_next_data_element = dis.readInt();
    a.name = AffyGenericChpFile.parseWString(dis);
    a.param_count = dis.readInt();
    a.params = new ArrayList<AffyChpParameter>(a.param_count);
    for (int i=0; i<a.param_count; i++) {
      a.params.add(AffyGenericChpFile.parseParameter(dis));
    }
    
    a.num_columns = dis.readInt();
    a.columns = new ArrayList<AffyChpColumnType>((int) a.num_columns); // TODO:  what if really "long", not "int"?
    for (int i=0; i<a.num_columns; i++) {
      AffyChpColumnType col = new AffyChpColumnType(
        AffyGenericChpFile.parseWString(dis),
        dis.readByte(),
        dis.readInt());
      a.columns.add(col);
    }
        
    a.num_rows = dis.readInt();
    for (int row=0; row < a.num_rows; row++) {
      for (AffyChpColumnType col : a.columns) {
        col.addData(dis);
      }
    }
    
    return a;
  }
  
  public String toString() {
    return "AffyDataSet: first_element: " + pos_first_data_element +
        " next_element: " + pos_next_data_element +
        " name: " + name +
        "\n params: " + params.size() +
        "\n num_columns: " + num_columns +
        "\n num_rows: " + num_rows;
  }

  public void dump(PrintStream str) {
    str.println(this.getClass().getName());
    str.println("  first_element: " + pos_first_data_element);
    str.println("  next_element: " + pos_next_data_element);
    str.println("  name: " + name);
    str.println("  params: " + params.size());
    str.println("  num_columns: " + num_columns);
    str.println("  num_rows: " + num_rows);
    str.println("  Parameters:  ");
    
    for (AffyChpParameter param : params) {
      param.dump(str);
    }
    
    str.println("  Columns:  ");
    for (AffyChpColumnType column : columns) {
      column.dump(str);
    }
   
  }    
}
