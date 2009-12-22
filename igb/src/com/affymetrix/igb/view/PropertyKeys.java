/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.igb.view;
import java.util.*;

/**
 * Expresses allowed property names for SeqFeatures.  Offers
 * methods for accessing name-value pairs using Properties
 * Objects retrieved from SeqFeatures.
 * <p>
 * This is a modified version of com.affymetrix.genoviz.bio.datamodel.feature.PropertyKeys
 */
final class PropertyKeys {
  String[] keys = new String[0];

  /**
   * Orders for keys.  Properties name-value pairs are
   *    listed in the order defined here.
   * If properties are present with names _not_ defined here, they are
   *    listed last.
   */
  public void setKeyOrder(String[] keys) {
    this.keys = keys;
  }

  /**
   * Fills up a Vector with arrays containing names and values
   * for each of the given Properties.
   * e.g., {name,value0,value1,value2,...,valueN} for
   * N different Properties Objects.
   * @param props  the list of Properties derived from SeqFeatures.
   * @param noData  the String value to use to represent cases where
   *   there is no value of the property for a given key
   */
  public Vector<String[]> getNameValues(Map[] props, String noData) {
		Vector<String[]> result = new Vector<String[]>();
		// collect all possible names from the given Properties
		int num_props = props.length;
		Hashtable<String, String[]> rows_thus_far = new Hashtable<String, String[]>();
		for (int i = 0; i < props.length; i++) {
			//      System.out.println(i);
			//      System.out.println(props[i]);
			if (props[i] == null) {
				continue;
			}
			Iterator names_iter = props[i].keySet().iterator();
			while (names_iter.hasNext()) {
				Object obj = names_iter.next();
				String name = null;
				String name_value[] = null;
				if (obj != null) {
					name = obj.toString();
					name_value = rows_thus_far.get(name);
				}
				if (name_value != null) {
					continue;
				} else {
					name_value = new String[num_props + 1];
					name_value[0] = name;
					for (int j = 0; j < props.length; j++) {
						Object val = null;
						if (props[j] != null) {
							val = props[j].get(name);
						}
						val = (val == null ? noData : val);
						// if val is a List for multivalued property, rely on toString() to convert to [item1, item2, etc.]
						//   string representation
						name_value[j + 1] = val.toString();
					}
					rows_thus_far.put(name, name_value);
				}
			}
		}
		// now sort
		for (int i = 0; i < keys.length; i++) {
			String[] row = rows_thus_far.get(keys[i]);
			if (row != null) {
				result.addElement(row);
			}
			rows_thus_far.remove(keys[i]);
		}
		Enumeration<String[]> rows = rows_thus_far.elements();
		while (rows.hasMoreElements()) {
			result.addElement(rows.nextElement());
		}
		return result;
	}

  public static String getName(Vector<String[]> name_values,
                                 int index) {
    // name_values is a list of arrays - the first item of
    // each array is the name-value
    return (name_values.get(index))[0];
  }
}
