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
 *  A Vector-like class for shorts (since Vectors don't deal with primitives).
 *  Does not implement the Collection interfaces.
 *  Really just for getting a stretchable array of shorts.
 */
public class ShortList {

	/**
	 *  The array of primitives (in the case of shortList, shorts).
	 */
	private short primData[];

	/**
	 * The size of the *List (the number of primitive values it contains).
	 */
	private int size;

	public ShortList(int initialCapacity) {
		super();
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
		}
		this.primData = new short[initialCapacity];
	}

	public ShortList() {
		this(10);
	}

	public void trimToSize() {
		int oldCapacity = primData.length;
		if (size < oldCapacity) {
			short oldData[] = primData;
			primData = new short[size];
			System.arraycopy(oldData, 0, primData, 0, size);
		}
	}

	public void ensureCapacity(int minCapacity) {
		int oldCapacity = primData.length;
		if (minCapacity > oldCapacity) {
			short oldData[] = primData;
			int newCapacity = (oldCapacity * 3)/2 + 1;
			if (newCapacity < minCapacity) {
				newCapacity = minCapacity;
			}
			primData = new short[newCapacity];
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
				ShortList v = (ShortList)super.clone();
				v.primData = new short[size];
				System.arraycopy(primData, 0, v.primData, 0, size);
				return v;
			} catch (CloneNotSupportedException e) { 
				// this shouldn't happen, since we are Cloneable
				throw new InternalError();
			}
		}

	public short[] copyToArray() {
		short[] result = new short[size];
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
	public short[] getInternalArray() {
		return primData;
	}

	public short get(int index) {
		return primData[index];
	}

	/**
	 *  Replaces value at index.
	 *  Returns previous value at index.
	 */
	public short set(int index, short val) {
		short oldValue = primData[index];
		primData[index] = val;
		return oldValue;
	}

	public boolean add(short i) {
		ensureCapacity(size + 1); 
		primData[size++] = i;
		return true;
	}

	public void add(int index, short val) {
		if (index > size || index < 0) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
		}

		ensureCapacity(size+1); 
		System.arraycopy(primData, index, primData, index + 1,
				size - index);
		primData[index] = val;
		size++;
	}

	// returns value that was removed
	public short remove(short index) {
		short oldValue = primData[index];

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
