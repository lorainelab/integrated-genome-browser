/**
 *   Copyright (c) 2006 Affymetrix, Inc.
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

import com.affymetrix.igb.tiers.AnnotStyle;
import java.util.*;
import java.util.regex.*;

public class WebLink {  
  String regex = null;
  String url = null;
  String name = null;
  Pattern pattern = null;
  
  public WebLink() {
  }
  
  public WebLink(String name, String regex, String url) throws PatternSyntaxException {
    this();
    setName(name);
    setRegex(regex);
    setUrl(url);
  }  

  static Map regex2weblink = new HashMap();
  
  public static void addWebLink(WebLink wl) {
    // use a map rather than a list so that duplicate regex's can't occur
    WebLink old_one = (WebLink) regex2weblink.get(wl.getRegex());
    regex2weblink.put(wl.getRegex(), wl);
  }

  /** Get all web-link patterns for the given method name.
   *  These can come from regular-expression matching from the semi-obsolete
   *  XML-based preferences file, or from UCSC-style track lines in the
   *  input files.
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
      Iterator iter = regex2weblink.values().iterator();
      while (iter.hasNext()) {
        WebLink link = (WebLink) iter.next();
        if (link.matches(method) && link.url != null) {
          results.add(link);
        }
      }
    }

    return (WebLink[]) results.toArray(new WebLink[results.size()]);
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
  
  public void setRegex(String regex) throws PatternSyntaxException {
    if (regex == null) {
      pattern = null;
    } else {
      pattern = Pattern.compile(regex);
    }
  }
  
  public Pattern getPattern() {
    return pattern;
  }
  
  /** Returns the URL (or URL pattern) associated with this WebLink.
   *  If the URL pattern contains any "$$" characters, those should be
   *  replaced with URL-Encoded annotation IDs to get the final URL.
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
  
  public String toString() {
    return "WebLink: name='"+name+"' pattern='"+pattern+"'  url='"+url+"'";
  }
}
