/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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
import java.util.*;
import java.util.regex.*;

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
    InputStream syn_stream = null;
    try {
      syn_stream = LocalUrlCacher.getInputStream(synonym_loc);
    } catch (IOException ioe) {
      syn_stream = null;
    } finally {
      if (syn_stream != null) try {syn_stream.close();} catch(Exception e) {}
    }

    if (syn_stream == null) {
      System.out.println("WARNING: Unable to load synonym data from: " + synonym_loc);
      return;
    }

    try {
      loadSynonyms(syn_stream);
    }
    catch (Exception ex) {
      System.out.println("WARNING: Error while loading synonym data from: " + synonym_loc);
      ex.printStackTrace();
    } finally {
      if (syn_stream != null) try {syn_stream.close();} catch(Exception e) {}
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

  boolean caseSensitive = true;
  
  /** Set whether tests should be case sensitive or not.
   *  You can turn this on or off safely before or after loading the synonyms.
   *  Default is true: case does matter by default.
   */
  public void setCaseSensitive(boolean case_sensitive) {
    this.caseSensitive = case_sensitive;
  }
  
  public boolean isCaseSensitive() { return caseSensitive; }
  
  
  /**
   * Set this flag to true to automatically take care of synonyms of "_random"
   * sequence names.  If true, then "XXX_random" is a synonym of "YYY_random"
   * if "XXX" is a synonym of "YYY" (or if that particular set of synonyms
   * was explicitly specified.)
   */
  public boolean stripRandom = false;
  
  public boolean isSynonym(String str1, String str2) {
    //    System.out.println("$$$$$ called isSynonym for " + str1 + ", " + str2 + " $$$$$");
    if (str1 == null || str2 == null) { return false; }
    if (str1.equals(str2)) { return true; }
    ArrayList al = getSynonyms(str1);
    if (al != null) {
      int lcount = al.size();
      for (int i=0; i<lcount; i++) {
        String curstr = (String)al.get(i);
        //        System.out.println("trying to match " + str2 + " to " + curstr);
        if (isCaseSensitive()) {
          if (str2.equals(curstr)) {
            return true;
          }
        } else {
          if (str2.equalsIgnoreCase(curstr)) {
            return true;
          }
        }
      }
    }
    if (stripRandom) {
      if (str1.toLowerCase().endsWith("_random") && str2.toLowerCase().endsWith("_random")) {
        return isSynonym(stripRandom(str1), stripRandom(str2));
      }
    }
    return false;
  }
  
  // Strip the word "_random" from the item name
  // Will not change the case of the input, but if isCaseSensitive is false, will
  // also strip "_RanDom", etc.
  String stripRandom(String s) {
    if (s == null) return null;
    String s2 = s;
    if (isCaseSensitive()) {
      if (s.endsWith("_random")) {
        s2 = s.substring(0, s.length() - 7);
      }
    }
    else {
      if (s.toLowerCase().endsWith("_random")) {
        s2 = s.substring(0, s.length() - 7);
      }
    }
    return s2;
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
    if (! list1.contains(str2)) {
      list1.add(str2);
    }

    ArrayList list2 = (ArrayList)lookup_hash.get(str2);
    if (list2 == null) {
      list2 = new ArrayList();
      lookup_hash.put(str2, list2);
      list2.add(str2);  // now including self as synonym -- GAH 10-28-2002
    }
    if (! list2.contains(str1)) {
      list2.add(str1);
    }

  }

  /** Returns all known synonyms for a given string.
   *  Even if isCaseSensitive() is false, the items in the returned list will still 
   *  have the same cases as were given in the input, but the lookup to find the
   *  list of synonyms will be done in a case-insensitive way.
   */
  public ArrayList getSynonyms(String str) {
    if (isCaseSensitive()) {
      return (ArrayList)lookup_hash.get(str);
    } else {
      // If not case-sensitive
      
      Object o = lookup_hash.get(str);
      if (o != null) return (ArrayList) o;
      
      Iterator iter = lookup_hash.keySet().iterator();
      while (iter.hasNext()) {
        String key = (String) iter.next();
        if (key.equalsIgnoreCase(str)) { return (ArrayList) lookup_hash.get(key); }
      }
    }
    return null;
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
}
