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
package com.affymetrix.igb.view;

import com.affymetrix.igb.prefs.*;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

public class DataLoadPrefsView extends JPanel implements IPrefEditorComponent {
  
  static Map cache_usage_options;
  static Map usage2str;

  JButton clear_cacheB;
  JCheckBox cache_annotsCB;
  JCheckBox cache_residuesCB;
  JComboBox cache_usage_selector;
  JButton reset_das_dna_serverB = new JButton("Reset");

  static {
    String norm = "Normal Usage";
    String ignore = "Ignore Cache";
    String only = "Use Only Cache";
    Integer normal = new Integer(LocalUrlCacher.NORMAL_CACHE);
    Integer ignore_cache = new Integer(LocalUrlCacher.IGNORE_CACHE);
    Integer cache_only = new Integer(LocalUrlCacher.ONLY_CACHE);
    cache_usage_options = new LinkedHashMap();
    cache_usage_options.put(norm, normal);
    cache_usage_options.put(ignore, ignore_cache);
    cache_usage_options.put(only, cache_only);
    usage2str = new LinkedHashMap();
    usage2str.put(normal, norm);
    usage2str.put(ignore_cache, ignore);
    usage2str.put(cache_only, only);
  }
    
  public DataLoadPrefsView() {
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.setBorder(BorderFactory.createEtchedBorder());
        
    Box url_box = new Box(BoxLayout.Y_AXIS);
    url_box.setBorder(new javax.swing.border.TitledBorder("Personal QuickLoad URL"));
    JTextField quickload_url_TF = UnibrowPrefsUtil.createTextField(
        UnibrowPrefsUtil.getLocationsNode(), QuickLoadView2.PREF_USER_DEFINED_QUICKLOAD_URL, "");
    quickload_url_TF.setMaximumSize(new Dimension(quickload_url_TF.getMaximumSize().width,
        quickload_url_TF.getPreferredSize().height));
    url_box.add(quickload_url_TF);

    url_box.setAlignmentX(0.0f);
    this.add(url_box);
    this.add(Box.createRigidArea(new Dimension(0, 5)));
    
    Box server_box = new Box(BoxLayout.X_AXIS);
    server_box.setBorder(new javax.swing.border.TitledBorder("Das DNA Server URL"));
    JTextField das_dna_server_TF = 
        UnibrowPrefsUtil.createTextField(UnibrowPrefsUtil.getLocationsNode(), 
        QuickLoadView2.PREF_DAS_DNA_SERVER_URL, QuickLoadView2.DEFAULT_DAS_DNA_SERVER);
    das_dna_server_TF.setMaximumSize(new Dimension(das_dna_server_TF.getMaximumSize().width,
        das_dna_server_TF.getPreferredSize().height));
    server_box.add(das_dna_server_TF);
    JButton reset_das_dna_serverB = new JButton("Reset");
    server_box.add(reset_das_dna_serverB);
    reset_das_dna_serverB.addActionListener(reset_das_dna_server_al);
    server_box.setAlignmentX(0.0f);
    this.add(server_box);
    this.add(Box.createRigidArea(new Dimension(0, 5)));
    
    cache_annotsCB = UnibrowPrefsUtil.createCheckBox("Cache Annotations",
        UnibrowPrefsUtil.getTopNode(),
        QuickLoadServerModel.PREF_QUICKLOAD_CACHE_ANNOTS, 
        QuickLoadServerModel.CACHE_ANNOTS_DEFAULT);
    cache_residuesCB = UnibrowPrefsUtil.createCheckBox("Cache DNA Residues",
        UnibrowPrefsUtil.getTopNode(),
        QuickLoadServerModel.PREF_QUICKLOAD_CACHE_RESIDUES, 
        QuickLoadServerModel.CACHE_RESIDUES_DEFAULT);
    
    clear_cacheB = new JButton("Clear Cache");
    cache_usage_selector = new JComboBox();
    Iterator iter = cache_usage_options.keySet().iterator();
    while (iter.hasNext()) {
      String str = (String)iter.next();
      cache_usage_selector.addItem(str);
    }
    cache_usage_selector.setSelectedItem(usage2str.get(new Integer(LocalUrlCacher.getPreferredCacheUsage())));
    Dimension d = new Dimension(cache_usage_selector.getPreferredSize());
    cache_usage_selector.setMaximumSize(d);
    
    JPanel cache_options_box = new JPanel();
    cache_options_box.setLayout(new BoxLayout(cache_options_box, BoxLayout.Y_AXIS));
    cache_options_box.setAlignmentX(0.0f);
    this.add(cache_options_box);
    
    cache_annotsCB.setAlignmentX(0.0f);
    cache_options_box.add(cache_annotsCB);
    cache_residuesCB.setAlignmentX(0.0f);
    cache_options_box.add(cache_residuesCB);
    JComponent usageP = Box.createHorizontalBox();
    usageP.add(Box.createRigidArea(new Dimension(5,0)));
    usageP.add(new JLabel("Cache Usage"));
    usageP.add(Box.createRigidArea(new Dimension(5,0)));
    usageP.add(cache_usage_selector);
    usageP.add(Box.createHorizontalGlue());
    usageP.setAlignmentX(0.0f);
    usageP.setBorder(BorderFactory.createEmptyBorder(3,0,3,0));
    cache_options_box.add(usageP);
    
    Box clear_cache_box = Box.createHorizontalBox();
    clear_cache_box.setBorder(BorderFactory.createEmptyBorder(3,0,3,0));
    clear_cache_box.add(Box.createHorizontalGlue());
    clear_cache_box.add(clear_cacheB);
    clear_cache_box.add(Box.createHorizontalGlue());
    clear_cache_box.setAlignmentX(0.0f);
    cache_options_box.add(clear_cache_box);
    
    cache_usage_selector.addItemListener(cache_usage_al);
    clear_cacheB.addActionListener(clear_cache_al);
    
    this.add(Box.createVerticalGlue());
  } 
 
  ActionListener clear_cache_al = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      LocalUrlCacher.clearCache();
    }
  };
  
  ActionListener reset_das_dna_server_al = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      UnibrowPrefsUtil.getLocationsNode().put(QuickLoadView2.PREF_DAS_DNA_SERVER_URL, 
          QuickLoadView2.DEFAULT_DAS_DNA_SERVER);
    }
  };
  
  // the cache_usage_selector will probably go away later
  ItemListener cache_usage_al = new ItemListener() {
    public void itemStateChanged(ItemEvent e) {
      String usage_str = (String) cache_usage_selector.getSelectedItem();
      int usage = ((Integer) DataLoadPrefsView.cache_usage_options.get(usage_str)).intValue();
      //QuickLoadServerModel.setCacheBehavior(usage, cache_annotsCB.isSelected(), cache_residuesCB.isSelected());
      UnibrowPrefsUtil.saveIntParam(LocalUrlCacher.PREF_CACHE_USAGE, usage);
    }
  };
  
  public String getName() {
    return "Data Sources";
  }
  
  public void refresh() {
    // the checkboxes and text fields refresh themselves automatically....
    cache_usage_selector.setSelectedItem(usage2str.get(
        new Integer(LocalUrlCacher.getPreferredCacheUsage())));
  }
  
  public Icon getIcon() {
    return null;
  }
  
  public String getToolTip() {
    return "Edit QuickLoad data sources and preferences";
  }
  
  public String getHelpTextHTML() {
    return null;
  }
  
  public String getInfoURL() {
    return null;
  }
  
  /** A main method for testing. */
  public static void main(String[] args) throws Exception {
    DataLoadPrefsView p = new DataLoadPrefsView();
    PreferencesPanel.testPanel(p);
  }
}