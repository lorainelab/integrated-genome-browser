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

package com.affymetrix.genometryImpl.util;

/**
 *  A Vector-like class for chars (since Vectors don't deal with primitives).
 *  Does not implement the Collection interfaces.
 *  Really just for getting a stretchable array of chars.
 */
public class CharList {

  /**
   *  The array of primitives (in the case of CharList, chars).
   */
  private char primData[];

  /**
   * The size of the *List (the number of primitive values it contains).
   */
  private char size;

  public CharList(int initialCapacity) {
    super();
    if (initialCapacity < 0) {
      throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
    }
    this.primData = new char[initialCapacity];
  }

  public CharList() {
    this(10);
  }

  public void trimToSize() {
    int oldCapacity = primData.length;
    if (size < oldCapacity) {
      char oldData[] = primData;
      primData = new char[size];
      System.arraycopy(oldData, 0, primData, 0, size);
    }
  }

  public void ensureCapacity(int minCapacity) {
    int oldCapacity = primData.length;
    if (minCapacity > oldCapacity) {
      char oldData[] = primData;
      int newCapacity = (oldCapacity * 3)/2 + 1;
      if (newCapacity < minCapacity) {
        newCapacity = minCapacity;
      }
      primData = new char[newCapacity];
      System.arraycopy(oldData, 0, primData, 0, size);
    }
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public Object clone() {
    try { 
      CharList v = (CharList)super.clone();
      v.primData = new char[size];
      System.arraycopy(primData, 0, v.primData, 0, size);
      return v;
    } catch (CloneNotSupportedException e) { 
      // this shouldn't happen, since we are Cloneable
      throw new InternalError();
    }
  }

  public char[] copyToArray() {
    char[] result = new char[size];
    System.arraycopy(primData, 0, result, 0, size);
    return result;
  }
  
  /**
   * Get direct access to the internal data array.
   * WARNING -- this array may be bigger than 
   *   this.size(), and any entries beyond this.primData[size()-1] are 
   *  not guaranteed to mean anything.
   * If using this method, be sure to also retrieve this.size(), and 
   *  <b>do not</b> use values beyond returned_array[size()-1].
   */
  public char[] getInternalArray() {
    return primData;
  }

  public char get(int index) {
    return primData[index];
  }

  /**
   *  Replaces value at index.
   *  Returns previous value at index.
   */
  public char set(int index, char val) {
    char oldValue = primData[index];
    primData[index] = val;
    return oldValue;
  }

  public boolean add(char i) {
    ensureCapacity(size + 1); 
    primData[size++] = i;
    return true;
  }

  public void add(int index, char val) {
    if (index > size || index < 0) {
      throw new IndexOutOfBoundsException("Index: "+index+", Size: "+size);
    }

    ensureCapacity(size+1); 
    System.arraycopy(primData, index, primData, index + 1, size - index);
    primData[index] = val;
    size++;
  }
  
  // returns value that was removed
  public char remove(char index) {
    char oldValue = primData[index];

    int numMoved = size - index - 1;
    if (numMoved > 0) {
      System.arraycopy(primData, index+1, primData, index, numMoved);
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
