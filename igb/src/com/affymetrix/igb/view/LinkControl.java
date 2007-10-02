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

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometryImpl.SymWithProps;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.util.*;
import com.affymetrix.igb.menuitem.MenuUtil;
import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.igb.util.WebBrowserControl;

public class LinkControl implements ContextualPopupListener {

  public LinkControl() { }

  public void popupNotify(JPopupMenu popup, List selected_syms, SeqSymmetry primary_sym) {
    if (primary_sym == null) System.out.println("if primary_sym is null!");
    if (selected_syms.size() == 1 && primary_sym != null) {
      
      Map menu_items = new LinkedHashMap(); // map of menu url->name, or url -> url if there is no name
      
      // DAS files can contain links for each individual feature.
      // These are stored in the "link" property
      Object links = null;
      if (primary_sym instanceof SymWithProps) {
        links = ((SymWithProps) primary_sym).getProperty("link");
      }
      Object link_names = null;
      if (primary_sym instanceof SymWithProps) {
        link_names = ((SymWithProps) primary_sym).getProperty("link_name");
      }
      if (links != null) {
        if (links instanceof String) {
          String url = (String) links;
          url = WebLink.replacePlaceholderWithId(url, primary_sym.getID());
          if (link_names instanceof String) {
            menu_items.put(url, (String) link_names);
          } else {
            menu_items.put(url, url);            
          }
        } else if (links instanceof List) {
          List urls = (List)links;
          for (int i=0; i<urls.size(); i++) {
            String url = (String) urls.get(i);
            url = WebLink.replacePlaceholderWithId(url, primary_sym.getID());
            menu_items.put(url, url);
          }
        } else if (links instanceof Map) {
          Map name2url = (Map) links;
          Iterator iter = name2url.entrySet().iterator();
          while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            String name = (String)entry.getKey();
            String url = (String)entry.getValue();
            url = WebLink.replacePlaceholderWithId(url, primary_sym.getID());
            menu_items.put(url, name);
          }
        }
      }
      
      // Most links come from matching the tier name (i.e. method)
      // to a regular expression.
      String method = SeqMapView.determineMethod(primary_sym);
      WebLink[] web_links = WebLink.getWebLinks(method);
      // by using a Map to hold the urls, any duplicated urls will be filtered-out.

      for (int i=0; i<web_links.length; i++) {
        // Generally, just let any link replace an existing link that has the same URL.
        // But, if the new one has no name, and the old one does, then keep the old one.
        String new_name = web_links[i].getName();
        String url = web_links[i].getURLForSym(primary_sym);
        String old_name = (String) menu_items.get(url);
        if (old_name == null || "".equals(old_name)) {
          menu_items.put(url, new_name);
        }
      }
      
      //String id = (String) proper.getProperty("id");
      //String id = (String) primary_sym.getID();
      makeMenuItemsFromMap(popup, menu_items);
    }
  }
  
  void makeMenuItemsFromMap(JPopupMenu popup, Map urls) {    
    if (urls.isEmpty()) {
      return;
    }
    
    Iterator iter = urls.entrySet().iterator();

    if (urls.size() == 1) {
      Map.Entry entry = (Map.Entry) iter.next();
      String url = (String) entry.getKey();
      String name = (String) entry.getValue();
      if (name == null || name.equals(url)) {
        name = "Get more info";
      }
      JMenuItem mi = makeMenuItem(name, url);
      mi.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Search16.gif"));
      popup.add(mi);
    }
    else {
      JMenu linkMenu = new JMenu("Get more info");
      linkMenu.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Search16.gif"));
      popup.add(linkMenu);
      
      while (iter.hasNext()) {
        Map.Entry entry = (Map.Entry) iter.next();
        String url = (String) entry.getKey();        
        String name = (String) entry.getValue();
        if (name == null || name.equals(url)) {
          name = "Unnamed link to web";
        }
        JMenuItem mi = makeMenuItem(name, url);
        linkMenu.add(mi);
      }
    }  
  }
  
  JMenuItem makeMenuItem(String name, final String url) {
    JMenuItem linkMI = new JMenuItem(name);
    if (url != null) {
      linkMI.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          WebBrowserControl.displayURLEventually(url);
        }
      });
    }
    return linkMI;
  }
}
