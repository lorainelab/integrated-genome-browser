/**
 *   Copyright (c) 2006-2007 Affymetrix, Inc.
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

package com.affymetrix.igb.prefs;

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.igb.genometry.SymWithProps;
import com.affymetrix.igb.tiers.AnnotStyle;
import com.affymetrix.swing.ListModelForLists;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;

public class WebLink {
  String regex = null;
  String url = null;
  String name = null;
  String id_field_name = null; // null implies use getId(); "xxx" means use getProperty("xxx");

  Pattern pattern = null;
  static DefaultListModel weblink_list = new DefaultListModel();
    
  public WebLink() {
  }
  
  public WebLink(String name, String regex, String url) throws PatternSyntaxException {
    this();
    setName(name);
    setRegex(regex);
    setUrl(url);
  }

  boolean equals(String s1, String s2) {
    return ( s1 == s2 || (s1 != null && s1.equals(s2)));
  }
  
  public boolean equals(Object o) {
    if (o instanceof WebLink) {
      WebLink w = (WebLink) o;
      return (equals(name, w.name) && equals(regex, w.regex) 
       && equals(url, w.url) && equals(id_field_name, w.id_field_name));
    }
    return false;
  }
  
  public int hashCode() {
    int hash = 1;
    if (name != null) { hash = 31*hash + name.hashCode(); }
    if (regex != null) { hash = 31*hash + regex.hashCode(); }
    if (url != null) { hash = 31*hash + url.hashCode(); }
    if (id_field_name != null) { hash = 31*hash + id_field_name.hashCode(); }
    return hash;
  }
    
  /**
   *  A a WebLink to the static list.  Multiple WebLink's with the same 
   *  regular expressions are allowed, as long as they have different URLs.
   */
  public static void addWebLink(WebLink wl) {
    if ( weblink_list.contains(wl) ) {
      System.out.println("Not adding duplicate web link for regex: '" + wl.regex + "'");
    } else {
      weblink_list.addElement(wl);
    }
  }

  /**
   *  Remove a WebLink from the static list.
   */
  public static void removeWebLink(WebLink wl) {
    weblink_list.removeElement(wl);
  }
  
  /** Get all web-link patterns for the given method name.
   *  These can come from regular-expression matching from the semi-obsolete
   *  XML-based preferences file, or from UCSC-style track lines in the
   *  input files.  It is entirely possible that some of the WebLinks in the
   *  array will have the same regular expression or point to the same URL.
   *  You may want to filter-out such duplicate results.
   */
  public static WebLink[] getWebLinks(String method) {
    ArrayList results = new ArrayList();
    
    // If the method name has already been used, then the annotStyle must have already been created
    AnnotStyle style = AnnotStyle.getInstance(method, false);
    String style_url = style.getUrl();
    if (style_url != null && ! style_url.equals("")) {
      WebLink link = new WebLink("Track Line URL", null, style_url);
      results.add(link);
    }

    if (method != null) {
      // This is not terribly fast, but it is not called in places where speed matters
      Enumeration en = weblink_list.elements();
      while (en.hasMoreElements()) {
        WebLink link = (WebLink) en.nextElement();
        if (link.matches(method) && link.url != null) {
          results.add(link);
        }
      }
    }

    return (WebLink[]) results.toArray(new WebLink[results.size()]);
  }
  
  /** Returns a ListModel backed by the list of WebLink items. */
  public static ListModel getWebLinkListModel() {
    return weblink_list;
  }
    
  public String getName() {
    return this.name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getRegex() {
    if (pattern == null) {
      return null;
    } else {
      return pattern.pattern();
    }
  }
  
  /** Sets the regular expression that must be matched.
   *  The special value <b>null</b> is also allowed, and matches every String.
   */
  public void setRegex(String regex) throws PatternSyntaxException {
    if (regex == null) {
      pattern = null;
    } else {
      pattern = Pattern.compile(regex);
    }
  }
  
  /** Return the compiled form of the regular expression. */
  public Pattern getPattern() {
    return pattern;
  }
  
  /** Returns the URL (or URL pattern) associated with this WebLink.
   *  If the URL pattern contains any "$$" characters, those should be
   *  replaced with URL-Encoded annotation IDs to get the final URL.
   *  Better to use {@link #getUrlForSym(SeqSymmetry)}.
   */
  public String getUrl() {
    return this.url;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }
  
  public boolean matches(String s) {
    if (pattern == null) {
      return true;
    } else {
      return pattern.matcher(s).matches();
    }
  }
  
  /** A pattern that matches the string "$$". */
  static final Pattern DOUBLE_DOLLAR_PATTERN = Pattern.compile("[$][$]");
  
  public static String replacePlaceholderWithId(String url, String id) {
    // Now replace all "$$" in the url pattern with the given id, URLEncoded
    if (url != null && id != null) {
      String encoded_id = URLEncoder.encode(id);
      url = DOUBLE_DOLLAR_PATTERN.matcher(url).replaceAll(encoded_id);
    }
    return url;
  }
  
  public String getURLForSym(SeqSymmetry sym) {
    // Currently this just replaces any "$$" with the ID, but it could
    // do something more sophisticated later, like replace "$$" with
    // some other sym property.
    if (id_field_name == null) {
      return replacePlaceholderWithId(getUrl(), sym.getID());
    }
    
    Object field_value = null;
    if (id_field_name != null && sym instanceof SymWithProps) {
      field_value = ((SymWithProps) sym).getProperty(id_field_name);
    }
    if (field_value == null) {
      System.out.println("WARNING: Selected item has no value for property '"+id_field_name+
          "' which is needed to construct the web link.");
      return replacePlaceholderWithId(getUrl(), "");
    }
    else {
      return replacePlaceholderWithId(getUrl(), field_value.toString());
    }
  }
  
  public String toString() {
    return "WebLink: name=" + name + 
        ", regex=" + regex + 
        ", url=" + url +
        ", id_field_name=" + id_field_name;
  }
}
