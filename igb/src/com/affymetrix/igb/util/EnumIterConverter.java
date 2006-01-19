package com.affymetrix.igb.util;

import java.util.Enumeration;
import java.util.Iterator;

public class EnumIterConverter {

  public static Iterator getIterator(final Enumeration numer) {
    return new Iterator() {
	public boolean hasNext() { return numer.hasMoreElements(); }
	public Object next() { return numer.nextElement(); }
	public void remove() { throw new UnsupportedOperationException(); }
      };
  }

  public static Enumeration getEnumeration(final Iterator iter) {
    return new Enumeration() {
	public boolean hasMoreElements() { return iter.hasNext(); }
	public Object nextElement() { return iter.next(); }
      };
  }

}
