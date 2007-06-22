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

import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.igb.prefs.*;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.igb.util.UnibrowPrefsUtil;

import java.awt.Dimension;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

public class DataLoadPrefsView extends JPanel implements IPrefEditorComponent {
  
  static final String PREF_SYN_FILE_URL = "Synonyms File URL";
  
  static Map cache_usage_options;
  static Map usage2str;

  JButton clear_cacheB;
  JCheckBox cache_annotsCB;
  JCheckBox cache_residuesCB;
  JComboBox cache_usage_selector;
  JButton reset_das_dna_serverB = new JButton("Reset");
  JTextField syn_file_TF;

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
    server_box.add(Box.createRigidArea(new Dimension(6,0)));
    JButton reset_das_dna_serverB = new JButton("Reset");
    server_box.add(reset_das_dna_serverB);
    server_box.add(Box.createRigidArea(new Dimension(2,0)));
    reset_das_dna_serverB.addActionListener(reset_das_dna_server_al);
    server_box.setAlignmentX(0.0f);
    this.add(server_box);
    this.add(Box.createRigidArea(new Dimension(0, 5)));
    

    Box syn_box = Box.createVerticalBox();
    syn_box.setBorder(new javax.swing.border.TitledBorder("Personal Synonyms File"));
    syn_file_TF = 
        UnibrowPrefsUtil.createTextField(UnibrowPrefsUtil.getLocationsNode(), 
        PREF_SYN_FILE_URL, "");
    syn_file_TF.setMaximumSize(new Dimension(syn_file_TF.getMaximumSize().width,
        syn_file_TF.getPreferredSize().height));
    JButton browse_for_syn_fileB = new JButton("Browse");
    browse_for_syn_fileB.addActionListener(browse_for_syn_file_al);
    syn_file_TF.setAlignmentX(0.0f);
    syn_box.add(syn_file_TF);
    
    Box syn_box_line2 = new Box(BoxLayout.X_AXIS);
    syn_box_line2.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    syn_box_line2.add(browse_for_syn_fileB);
    syn_box_line2.add(Box.createRigidArea(new Dimension(10,0)));
    JButton load_synonymsB = new JButton("Load Synonyms");
    syn_box_line2.add(load_synonymsB);
    syn_box_line2.setAlignmentX(0.0f);
    syn_box.add(syn_box_line2);

    syn_box.setAlignmentX(0.0f);
    this.add(syn_box);
    this.add(Box.createRigidArea(new Dimension(0, 5)));
    
    load_synonymsB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        processSynFile();
      }
    });
    
    
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
    //clear_cache_box.add(Box.createHorizontalGlue());
    clear_cache_box.add(Box.createRigidArea(new Dimension(5,0)));
    clear_cache_box.add(clear_cacheB);
    clear_cache_box.add(Box.createHorizontalGlue());
    clear_cache_box.setAlignmentX(0.0f);
    cache_options_box.add(clear_cache_box);
    
    cache_usage_selector.addItemListener(cache_usage_al);
    clear_cacheB.addActionListener(clear_cache_al);
    
    this.add(Box.createVerticalGlue());

    

    // Load the personal synonyms file, if one is specified.
    // It isn't crucial that this be run with SwingUtilities, but it doesn't hurt.
    Runnable r = new Runnable() {
      public void run() {
        String second_synonym_path = UnibrowPrefsUtil.getLocationsNode().get(PREF_SYN_FILE_URL, "");
        if (! second_synonym_path.trim().equals("")) {
          File f = new File(second_synonym_path);
          if (f.exists()) try {
            System.out.println("Loading personal synonyms from: " + second_synonym_path);
            FileInputStream fis = new FileInputStream(f);
            SynonymLookup.getDefaultLookup().loadSynonyms(fis);
          } catch (IOException ioe) {
            System.out.println("Error trying to read synonyms from: "+second_synonym_path);
            System.out.println("" + ioe.toString());
          }
        }
      }
    };
  
    SwingUtilities.invokeLater(r);
    
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

  JFileChooser chooser = null;
  
  ActionListener browse_for_syn_file_al = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      if (chooser == null) {
        chooser = new JFileChooser();
      }
      chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
      chooser.rescanCurrentDirectory();
      
      int option = chooser.showOpenDialog(DataLoadPrefsView.this);
      
      if (option == JFileChooser.APPROVE_OPTION) {
        File f = chooser.getSelectedFile();
        UnibrowPrefsUtil.getLocationsNode().put(PREF_SYN_FILE_URL, f.getPath());
      }
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
  
  void processSynFile() {
    String path = syn_file_TF.getText();
    try {
      UnibrowPrefsUtil.getLocationsNode().put(PREF_SYN_FILE_URL, path);
      File f = new File(path);
      if (! f.exists()) {
        ErrorHandler.errorPanel("File Not Found",
            "Synonyms file not found at the specified path\n" + path, this);
        return;
      } else {
        FileInputStream fis = new FileInputStream(f);
        SynonymLookup.getDefaultLookup().loadSynonyms(fis);
        
        JOptionPane.showMessageDialog(this, "Loaded synonyms from: " + f.getName(), 
            "Loaded Synonyms", JOptionPane.INFORMATION_MESSAGE);
      }
    } catch (IOException ioe) {
      ErrorHandler.errorPanel("ERROR",
          "Exception while reading from file\n" + path,
          this, ioe);
    }
  }
  
  
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
    StringBuffer sb = new StringBuffer();

    sb.append("<h1>" + this.getName() + "</h1>\n");
    sb.append("<p>\n");
    sb.append("This panel allows you to change settings for data sources.  ");
    sb.append("It is not necessary to re-start the program for these changes to take effect.  ");
    sb.append("</p>\n");
    
    sb.append("<p>\n");
    sb.append("<h2>Personal QuickLoad URL</h2>\n");
    sb.append("Optional, generally left blank.  You can put the URL of a local or remote directory ");
    sb.append("that contains QuickLoad data for species that are of interest to you. ");
    sb.append("Documentation for the QuickLoad directory structure can be found here: ");
    sb.append("<pre>\nhttp://sourceforge.net/docman/?group_id=129420\n</pre>");
    sb.append("It is possible to use a personal server and the NetAffx server in the same session. ");
    sb.append("Use the pull-down selector in the 'Data Access' 'QuickLoad' tab to switch between them. ");
    sb.append("</p>\n");
    
    sb.append("<p>\n");
    sb.append("<h2>Das DNA Server URL</h2>\n");
    sb.append("Optional. Specifies the DAS server where you wish to download DNA residues. ");
    sb.append("This server is used when you press 'Load Sequence in View'. ");
    sb.append("(The current QuickLoad server is used when you press 'Load All Sequence'). ");
    sb.append("The most common value here is <pre>http://genome.cse.ucsc.edu/cgi-bin/das</pre> ");
    sb.append("but that server doesn't include data for all species.");
    sb.append("</p>\n");
    
    sb.append("<p>\n");
    sb.append("<h2>Personal Synonyms File</h2>\n");
    sb.append("Optional.  The location of a synonyms file to use to help resolve cases where ");
    sb.append("different data files refer to the same genome or chromosome by different names. ");
    sb.append("For instance 'hg16' = 'ncbi.v34' and 'chrM' = 'chrMT' and 'chr1' = 'CHR1'. ");
    sb.append("This is simply a tab-delimited file where entries on the same row are all synonymous. ");
    sb.append("Synonyms will be <b>merged</b> from the NetAffx QuickLoad server, your personal QuickLoad server, and the file listed here. ");
    sb.append("</p>\n");
    
    sb.append("<p>\n");
    sb.append("<h2>Cache</h2>\n");
    sb.append("IGB stores files downloaded over the network in a local cache. ");
    sb.append("Files loaded from a local filesystem or network filesystem are not cached. ");
    sb.append("We recommend that you leave the 'Cache Usage' setting on 'Normal' and ");
    sb.append("that you choose true for both 'Cache Annotations' and 'Cache DNA Residues'.");
    sb.append("If disk storage space is a problem, you can press the 'Clear Cache' button. ");
    sb.append("You may also choose to turn the cache off, though performance will degrade. ");
    sb.append("Some, but not all, users find it necessary to turn off the cache when they ");
    sb.append("are not connected to the internet.  For most users, this is not necessary as ");
    sb.append("long as the cache already contains a few essential files. ");
    sb.append("</p>\n");
        
    return sb.toString();
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