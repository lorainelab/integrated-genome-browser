package com.affymetrix.genometryImpl.comparator;

import java.util.Comparator;

/**
 *
 * Ref: http://stackoverflow.com/questions/7469643/how-to-sort-alphabetically-while-ignoring-case-sensitive
 */
public class SortIgnoreCase implements Comparator<Object> {

	@Override
	public int compare(Object o1, Object o2) {
		String s1 = (String) o1;
		String s2 = (String) o2;
		return s1.toLowerCase().compareTo(s2.toLowerCase());
	}
}
