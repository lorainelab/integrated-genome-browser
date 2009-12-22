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

package com.affymetrix.genometryImpl.util;

/**
 *  a List-like class for floats (since Lists don't deal with primitives).
 *  does not implement the Collection interfaces.
 *  really just for getting a stretchable array of floats.
 */
public final class FloatList {
	/**
	 *  the array of primitives (in the case of FloatList, floats)
	 */
	private float primData[];

	/**
	 * the size of the 'List' (the number of primitive values it contains
	 */
	private int size;

	public FloatList(int initialCapacity) {
		super();
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
		}
		this.primData = new float[initialCapacity];
	}

	public FloatList() {
		this(10);
	}

	public void trimToSize() {
		int oldCapacity = primData.length;
		if (size < oldCapacity) {
			float oldData[] = primData;
			primData = new float[size];
			System.arraycopy(oldData, 0, primData, 0, size);
		}
	}

	public void ensureCapacity(int minCapacity) {
		int oldCapacity = primData.length;
		if (minCapacity > oldCapacity) {
			float oldData[] = primData;
			int newCapacity = (oldCapacity * 3)/2 + 1;
			if (newCapacity < minCapacity) {
				newCapacity = minCapacity;
			}
			primData = new float[newCapacity];
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
				FloatList v = (FloatList)super.clone();
				v.primData = new float[size];
				System.arraycopy(primData, 0, v.primData, 0, size);
				return v;
			} catch (CloneNotSupportedException e) { 
				// this shouldn't happen, since we are Cloneable
				throw new InternalError();
			}
		}

	public float[] copyToArray() {
		float[] result = new float[size];
		System.arraycopy(primData, 0, result, 0, size);
		return result;
	}

	// WARNING -- this array may be bigger than 
	//    this.size(), and any entries beyond this.floatData[size()-1] are 
	//    not guaranteed to mean anything
	// if using this method, be sure to also retrieve this.size(), and 
	//    _don't_ use values beyond returned_array[size()-1]
	public float[] getInternalArray() {
		return primData;
	}

	public float get(int index) {
		return primData[index];
	}

	// replaces value at index
	// returns previous value at index
	/*public float set(int index, float val) {
		float oldValue = primData[index];
		primData[index] = val;
		return oldValue;
	}*/

	public boolean add(float i) {
		ensureCapacity(size + 1); 
		primData[size++] = i;
		return true;
	}

	public void add(int index, float val) {
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
	/*public float remove(int index) {
		float oldValue = primData[index];

		int numMoved = size - index - 1;
		if (numMoved > 0) {
			System.arraycopy(primData, index + 1, primData, index, numMoved);
		}
		size--;
		return oldValue;
	}*/

	/*public void clear() {
		//    for (int i = 0; i < size; i++)
		//      primData[i] = 0;
		size = 0;
	}*/

}
