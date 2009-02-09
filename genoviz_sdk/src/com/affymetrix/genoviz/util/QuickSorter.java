/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.util;

import java.util.Vector;

/**
 * Implements the quicksort algorithm.
 * Sorts Vectors of objects that implement the Comparable interface.
 * The sort is based on Comparable.compare(obj) method.
 * @see Comparable
 */
public class QuickSorter {

  /**
   * performs the sort.
   *
   * @param vect the Comparable Objects to be sorted.
   */
  public static void sort(Vector vect) {
    quickSort(vect, 0, vect.size()-1);
    insertionSort(vect);
  }

  static void swap(Vector vect, int i, int j) {
    Comparable x = (Comparable)vect.elementAt(i);
    vect.setElementAt(vect.elementAt(j), i);
    vect.setElementAt(x, j);
  }

  static void quickSort(Vector vect, int el, int yu) {
    if (yu-el < 16) {
      return;
    }
    int m, pivot, other;
    m = (yu+el)/2;
    if (((Comparable)vect.elementAt(el)).compare(vect.elementAt(yu)) < 0) {
      pivot = el;
      other = yu;
    }
    else {
      pivot = yu;
      other = el;
    }
    if (((Comparable)vect.elementAt(pivot)).compare(vect.elementAt(m)) < 0) {
      if (((Comparable)vect.elementAt(m)).compare(vect.elementAt(other)) < 0) {
        pivot = m;
      }
      else {
        pivot = other;
      }
    }

    swap(vect, el, pivot);

    int i, j;
    i = el + 1;
    j = yu - 1;
    while (true) {
      while (((Comparable)vect.elementAt(el)).compare(vect.elementAt(i)) < 0) {
        i++;
      }
      while (((Comparable)vect.elementAt(el)).compare(vect.elementAt(j)) > 0) {
        j--;
      }
      if (i >= j) {
        break;
      }
      swap(vect, i, j);
      i++;
      j--;
    }
    swap(vect, el, j);
    if (j - el < yu -i) {
      quickSort(vect, el, j-1);
      quickSort(vect, i, yu);
    }
    else {
      quickSort(vect, i, yu);
      quickSort(vect, el, j-1);
    }
  }

  static void insertionSort(Vector vect) {
    int i, j;
    for (i = 1; i < vect.size(); i++) {
      Comparable x = (Comparable)vect.elementAt(i);
      for (j = i;
          (j>0) && ((Comparable)vect.elementAt(j-1)).compare(x) > 0;
          j--) {
        vect.setElementAt(vect.elementAt(j-1), j);
      }
      vect.setElementAt(x, j);
    }
  }

}
