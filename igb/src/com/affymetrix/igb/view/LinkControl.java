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

package com.affymetrix.igb.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Pattern;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.menuitem.MenuUtil;
import com.affymetrix.igb.parsers.XmlPrefsParser;
import com.affymetrix.igb.util.WebBrowserControl;

public class LinkControl implements ActionListener, ContextualPopupListener {
  Map menu2url = new HashMap();

  /** A pattern that matches the string "$$". */
  Pattern DOUBLE_DOLLAR_PATTERN = Pattern.compile("[$][$]");

  public LinkControl() { }

  public void popupNotify(JPopupMenu popup, List selected_syms, SymWithProps primary_sym) {
    menu2url.clear();
    if (selected_syms.size() == 1) {
      SymWithProps proper;
      if (primary_sym == null) {
        SeqSymmetry sym = (SeqSymmetry)selected_syms.get(0);

        proper = null;
        if (sym instanceof SymWithProps) {
          proper = (SymWithProps) sym;
        }
        else if (sym instanceof DerivedSeqSymmetry) {
          SeqSymmetry original = ((DerivedSeqSymmetry) sym).getOriginalSymmetry();
          if (original instanceof SymWithProps) {
            proper = (SymWithProps) original;
          }
        }
      } else {
        proper = primary_sym;
      }

      if (proper != null) {
	String id = (String)proper.getProperty("id");
	Object links = proper.getProperty("link");
	String weburl = null;
	if (links != null) {
	  if (links instanceof String) {
	    weburl = (String)links;
	  }
	  else if (links instanceof List) {
	    List urls = (List)links;
	    if (urls.size() == 1) {
	      weburl = (String)urls.get(0);
	    }
	    else {
	      JMenu linkMenu = new JMenu("links to web");
	      popup.add(linkMenu);
	      for (int i=0; i<urls.size(); i++) {
		String url = (String)urls.get(i);
		JMenuItem linkMI = new JMenuItem(url);
		linkMenu.add(linkMI);
		linkMI.addActionListener(this);
		menu2url.put(linkMI, url);
	      }
	    }

	  }
	  else if (links instanceof Map) {
	    Map name2url = (Map)links;
	    if (name2url.size() == 1) {
	      Map.Entry entry = (Map.Entry)name2url.entrySet().iterator().next();
	      weburl = (String)entry.getValue();
	    }
	    else {
	      JMenu linkMenu = new JMenu("links to web");
	      popup.add(linkMenu);
	      Iterator iter = name2url.entrySet().iterator();
	      while (iter.hasNext()) {
		Map.Entry entry = (Map.Entry)iter.next();
		String name = (String)entry.getKey();
		String url = (String)entry.getValue();
		JMenuItem linkMI = new JMenuItem(name);
		linkMenu.add(linkMI);
		linkMI.addActionListener(this);
		menu2url.put(linkMI, url);
	      }
	    }

	  }
	}
	else if (id != null) {
          String url_pattern = (String) proper.getProperty("url");

          if (url_pattern == null) {
            String meth = SeqMapView.determineMethod(proper);
            url_pattern = XmlPrefsParser.getLinkURL(IGB.getIGBPrefs(), meth);
          }

          // Now replace all "$$" in the url pattern with the given id, URLEncoded
          if (url_pattern != null) {
            String encoded_id = URLEncoder.encode(id);
            weburl = DOUBLE_DOLLAR_PATTERN.matcher(url_pattern).replaceAll(encoded_id);
          }

	}
	if (weburl != null) {
	  JMenuItem browserMI = new JMenuItem("Get more info");
          browserMI.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Search16.gif"));
	  browserMI.addActionListener(this);
	  menu2url.put(browserMI, weburl);
	  popup.add(browserMI);
	}
      }
    }
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    String weburl = (String)menu2url.get(src);
    if (weburl != null) {
      WebBrowserControl.displayURLEventually(weburl);
    }
  }
}
