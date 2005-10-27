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

package com.affymetrix.igb.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.LocalUrlCacher;

/**
 *  A way of mapping synonyms to each other.
 */

public class SynonymLookup {
  static boolean DEBUG = false;
  static final Pattern line_regex = Pattern.compile("\t");
  static SynonymLookup default_lookup = new SynonymLookup();
  LinkedHashMap lookup_hash = new LinkedHashMap();

  public static void setDefaultLookup(SynonymLookup lookup) {
    default_lookup = lookup;
  }

  public void loadSynonyms(String synonym_loc) {
    System.out.println("url to load synonyms from: " + synonym_loc);
    try {
      InputStream syn_stream = LocalUrlCacher.getInputStream(synonym_loc);
      if (syn_stream == null) {
        System.out.println("WARNING: Unable to load synonym data from: " + synonym_loc);
      } else {
        loadSynonyms(syn_stream);
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void loadSynonyms(InputStream istream) {
    try {
      InputStreamReader ireader = new InputStreamReader(istream);
      BufferedReader br = new BufferedReader(ireader);
      String line;
      while ((line = br.readLine()) != null) {
	String[] fields = line_regex.split(line);
	if (fields.length > 0) {
	  addSynonyms(fields);
	}
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public static SynonymLookup getDefaultLookup() { return default_lookup; }

  public boolean isSynonym(String str1, String str2) {
    //    System.out.println("$$$$$ called isSynonym for " + str1 + ", " + str2 + " $$$$$");
    if (str1 == null || str2 == null) { return false; }
    if (str1.equals(str2)) { return true; }
    ArrayList al = (ArrayList)lookup_hash.get(str1);
    if (al != null) {
      int lcount = al.size();
      for (int i=0; i<lcount; i++) {
	String curstr = (String)al.get(i);
	//	System.out.println("trying to match " + str1 + " to " + curstr);
	if (str2.equals(curstr)) {
	  return true;
	}
      }
    }
    return false;
  }

  public void addSynonyms(String[] syns) {
    for (int i=0; i<syns.length; i++) {
      String syn1 = syns[i];
      if (DEBUG)  { System.out.println("adding:" + syn1 + ":"); }
      for (int k=i+1; k<syns.length; k++) {
	String syn2 = syns[k];
	addSynonym(syn1, syn2);
      }
    }
  }

  public void addSynonym(String str1, String str2) {
    ArrayList list1 = (ArrayList)lookup_hash.get(str1);
    if (list1 == null) {
      list1 = new ArrayList();
      lookup_hash.put(str1, list1);
      list1.add(str1);  // now including self as synonym -- GAH 10-28-2002
    }
    list1.add(str2);

    ArrayList list2 = (ArrayList)lookup_hash.get(str2);
    if (list2 == null) {
      list2 = new ArrayList();
      lookup_hash.put(str2, list2);
      list2.add(str2);  // now including self as synonym -- GAH 10-28-2002
    }
    list2.add(str1);

  }

  public ArrayList getSynonyms(String str) {
    return (ArrayList)lookup_hash.get(str);
  }
  
  /**
   *  Finds the first synonym in a list that matches the given string.
   *  @param choices a list of possible synonyms that might match the given test
   *  @param test  the id you want to find a synonym for
   *  @return either null or a String s, where isSynonym(test, s) is true.
   */
  public String findMatchingSynonym(List choices, String test) {
    String result = null;
    Iterator iter = choices.iterator();
    while (iter.hasNext()) {
      String id = (String) iter.next();
      if (this.isSynonym(test, id)) {
        result = id;
        break;
      }
    }
    return result;
  }

  public static void main(String[] args) {
    System.out.println("running SynonymLookup.main() to trigger static method...");
  }

}
