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
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.menuitem.MenuUtil;
import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.igb.util.WebBrowserControl;

public class LinkControl implements ActionListener, ContextualPopupListener {
  Map menu2url = new HashMap();

  /** A pattern that matches the string "$$". */
  Pattern DOUBLE_DOLLAR_PATTERN = Pattern.compile("[$][$]");

  public LinkControl() { }

  public void popupNotify(JPopupMenu popup, List selected_syms, SymWithProps primary_sym) {
    menu2url.clear();
    
    if (selected_syms.size() == 1 && primary_sym != null) {
      
      Map menu_items = new LinkedHashMap(); // map of menu url->name, or url -> url if there is no name
      
      // DAS files can contain links for each individual feature.
      // These are stored in the "link" property
      Object links = primary_sym.getProperty("link");
      Object link_names = primary_sym.getProperty("link-name");
      if (links != null) {
        if (links instanceof String) {
          String url = (String) links;
          if (link_names instanceof String) {
            menu_items.put(url, (String) link_names);
          } else {
            menu_items.put(url, url);            
          }
        } else if (links instanceof List) {
          List urls = (List)links;
          for (int i=0; i<urls.size(); i++) {
            String url = (String) urls.get(i);
            menu_items.put(url, url);
          }
        } else if (links instanceof Map) {
          Map name2url = (Map) links;
          Iterator iter = name2url.entrySet().iterator();
          while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            String name = (String)entry.getKey();
            String url = (String)entry.getValue();
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
        menu_items.put(web_links[i].getUrl(), web_links[i].getName());
      }
      
      //String id = (String) proper.getProperty("id");
      String id = (String) primary_sym.getID();
      makeMenuItemsFromMap(popup, menu_items, id);
    }
  }
  
  void makeMenuItemsFromMap(JPopupMenu popup, Map urls, String id) {    
    if (urls.isEmpty()) {
      return;
    }
    
    Iterator iter = urls.entrySet().iterator();

    if (urls.size() == 1) {
      Map.Entry entry = (Map.Entry) iter.next();
      String url = (String) entry.getKey();
      String name = (String) entry.getValue();
      if (name.equals(url)) {
        name = "Get more info";
      }
      JMenuItem mi = makeMenuItem(name, url, id);
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
        if (name.equals(url)) {
          name = "Unnamed link to web";
        }
        JMenuItem mi = makeMenuItem(name, url, id);
        linkMenu.add(mi);
      }
    }  
  }

  JMenuItem makeMenuItem(String name, String url, String id) {
    JMenuItem linkMI = new JMenuItem(name);
    linkMI.addActionListener(this);
    url = convertUrl(url, id);
    menu2url.put(linkMI, url);
    return linkMI;
  }
  
  String convertUrl(String url, String id) {
    // Now replace all "$$" in the url pattern with the given id, URLEncoded
    if (url != null && id != null) {
      String encoded_id = URLEncoder.encode(id);
      url = DOUBLE_DOLLAR_PATTERN.matcher(url).replaceAll(encoded_id);
    }
    return url;
  }
  
  
  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    String weburl = (String)menu2url.get(src);
    if (weburl != null) {
      WebBrowserControl.displayURLEventually(weburl);
    }
  }
}
