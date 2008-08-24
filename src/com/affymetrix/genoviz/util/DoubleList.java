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

package com.affymetrix.genoviz.util;

/**
 *  a List-like class for doubles (since Lists don't deal with primitives).
 *  does not implement the Collection interfaces.
 *  really just for getting a stretchable array of doubles.
 */
//TODO: Move this somewhere else
public class DoubleList implements Cloneable {
  /**
   *  The array of primitives (in the case of DoubleList, doubles)
   */
  private double primData[];

  /**
   * The size of the 'List' (the number of primitive values it contains)
   */
  private int size;

  public DoubleList(int initialCapacity) {
    super();
    if (initialCapacity < 0) {
      throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
    }
    this.primData = new double[initialCapacity];
  }

  public DoubleList() {
    this(10);
  }

  public void trimToSize() {
    int oldCapacity = primData.length;
    if (size < oldCapacity) {
      double oldData[] = primData;
      primData = new double[size];
      System.arraycopy(oldData, 0, primData, 0, size);
    }
  }

  public void ensureCapacity(int minCapacity) {
    int oldCapacity = primData.length;
    if (minCapacity > oldCapacity) {
      double oldData[] = primData;
      int newCapacity = (oldCapacity * 3)/2 + 1;
      if (newCapacity < minCapacity) {
        newCapacity = minCapacity;
      }
      primData = new double[newCapacity];
      System.arraycopy(oldData, 0, primData, 0, size);
    }
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public Object clone() {
    try { 
      DoubleList v = (DoubleList)super.clone();
      v.primData = new double[size];
      System.arraycopy(primData, 0, v.primData, 0, size);
      return v;
    } catch (CloneNotSupportedException e) { 
      // this shouldn't happen, since we are Cloneable
      throw new InternalError();
    }
  }

  public double[] copyToArray() {
    double[] result = new double[size];
    System.arraycopy(primData, 0, result, 0, size);
    return result;
  }
  
  // WARNING -- this array may be bigger than 
  //    this.size(), and any entries beyond this.doubleData[size()-1] are 
  //    not guaranteed to mean anything
  // if using this method, be sure to also retrieve this.size(), and 
  //    _don't_ use values beyond returned_array[size()-1]
  public double[] getInternalArray() {
    return primData;
  }

  public double get(int index) {
    return primData[index];
  }

  // replaces value at index
  // returns previous value at index
  public double set(int index, double val) {
    double oldValue = primData[index];
    primData[index] = val;
    return oldValue;
  }

  public boolean add(double i) {
    ensureCapacity(size + 1); 
    primData[size++] = i;
    return true;
  }

  public void add(int index, double val) {
    if (index > size || index < 0) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

    ensureCapacity(size+1); 
    System.arraycopy(primData, index, primData, index + 1, size - index);
    primData[index] = val;
    size++;
  }
  
  // returns value that was removed
  public double remove(int index) {
    double oldValue = primData[index];

    int numMoved = size - index - 1;
    if (numMoved > 0) {
      System.arraycopy(primData, index + 1, primData, index, numMoved);
    }
    size--;
    return oldValue;
  }

  public void clear() {
    //    for (int i = 0; i < size; i++)
    //      primData[i] = 0;
    size = 0;
  }

}
