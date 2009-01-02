/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.igb.bookmarks;

import java.util.*;
import javax.swing.table.AbstractTableModel;

/**
 * A simple instance of AbstractTableModel used by BookmarkTableComponent.
 * @author  ed
 */
public class BookmarkTableModel extends AbstractTableModel {
  
  /** A silly little helper class that holds two strings. 
   *  A String[2] array would work just as well.
   */
  static class Duple {
    public String a;
    public String b;
    public Duple(String a, String b) {
      this.a = a;
      this.b = b;
    }
  }
  
  List duples = Collections.EMPTY_LIST;
  String[] names = {"Parameter", "Value"};
  
  /** The number of extra rows to display to give users room to
   *  enter extra data into the table.
   */
  public final static int EXTRA_ROWS = 5;
  
  public BookmarkTableModel() {
  }

  /** Fills the table model with data from the Map.
   *  Some extra empty rows may also be appended to the table to 
   *  allow room for extra data.
   */
  public void setValuesFromMap(Map<String,String[]> map) {
    if (map == null) {
      throw new IllegalArgumentException("Map was null");
    }
    duples = new ArrayList();
    Iterator keys = map.keySet().iterator();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      String[] value = (String[]) map.get(key);
      if (value.length == 0) {
       //System.out.println("******************* length 0 for: "+key);
       duples.add(new Duple(key, ""));
      } else {
        for (int i=0; i<value.length; i++) {
          Duple duple = new Duple(key, value[i]);
          duples.add(duple);
        }
      }
    }
    for (int i=EXTRA_ROWS; i>0; i--) {
      duples.add(new Duple("",""));
    }
    fireTableDataChanged();
  }
  
  /** Returns the current contents of the table model as a Map.
   *  The returned Map will be a new map, not the same as the one passed in to
   *  {@link #setValuesFromMap(Map)}.
   *  Any item with an empty key or value will not be included in the Map.
   */
  public Map<String,String[]> getValuesAsMap() {
    LinkedHashMap m = new LinkedHashMap();
    for (int i=0; i<getRowCount(); i++) {
      String key = (String) getValueAt(i, 0);
      String value = (String) getValueAt(i, 1);
      Bookmark.addToMap(m, key, value);
    }
    return m;
  }
  
  public int getColumnCount() { return 2; }
  public int getRowCount() { return duples.size();}

  public Object getValueAt(int row, int col) {
    if (row < duples.size()) {
      Duple duple = (Duple) duples.get(row);
      if (col==0) {
        return duple.a;
      } else if (col==1) {
        return duple.b;
      }
    }
    return "";
  }

  public void setValueAt(Object aValue, int row, int col) {
    String s = (aValue == null ? "" : aValue.toString());
    Duple duple = (Duple) duples.get(row);
    if (col==0) {
      duple.a = s;
    } else if (col==1) {
      duple.b = s;
    }
    //System.out.println("Set value: "+row+","+col+": "+aValue);
    fireTableCellUpdated(row, col);
  }

  public String getColumnName(int column) {return names[column];}
  public Class getColumnClass(int col) {return String.class;}
  public boolean isCellEditable(int row, int col) {return true;}
}
