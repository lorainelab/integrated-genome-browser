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

package com.affymetrix.igb.prefs;

import com.affymetrix.igb.IGB;
import com.affymetrix.igb.menuitem.DasFeaturesAction2;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.prefs.*;
import javax.swing.*;

import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.util.WebBrowserControl;
import com.affymetrix.igb.view.OrfAnalyzer2;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.UnibrowHairline;

/**
 *  A panel that shows the preferences for particular special URLs and file locations. 
 */
public class OptionsView extends JPanel implements IPrefEditorComponent  {
    
  //final LocationEditPanel edit_panel1 = new LocationEditPanel();

  public OptionsView() {
    super();
    this.setName("Other Options");
    this.setLayout(new BorderLayout());

    JPanel main_box = new JPanel();
    main_box.setLayout(new BoxLayout(main_box,BoxLayout.Y_AXIS));
    main_box.setBorder(new javax.swing.border.EmptyBorder(5,5,5,5));
    
    //main_box.add(Box.createVerticalGlue());

    JScrollPane scroll_pane = new JScrollPane(main_box);
    this.add(scroll_pane, BorderLayout.CENTER);

    //edit_panel1.setBorder(new javax.swing.border.EtchedBorder());
    //String value = UnibrowPrefsUtil.getLocationsNode().get("QuickLoad URL", "");
    //edit_panel1.setLocation(UnibrowPrefsUtil.getLocationsNode(), "QuickLoad URL", value, true);

    //main_box.add(edit_panel1);
    //main_box.add(Box.createVerticalStrut(5));
    
    
    JPanel misc_box = new JPanel();
    boolean is_windows =  WebBrowserControl.isWindowsPlatform();
    if (is_windows) {
      misc_box.setLayout(new GridLayout(3,1));
    } else {
      misc_box.setLayout(new GridLayout(5,1));
    }
    //misc_box.setLayout(new BoxLayout(misc_box, BoxLayout.Y_AXIS));
    misc_box.setBorder(new javax.swing.border.EtchedBorder());
    misc_box.add(UnibrowPrefsUtil.createCheckBox("Ask before exiting", UnibrowPrefsUtil.getTopNode(), 
      UnibrowPrefsUtil.ASK_BEFORE_EXITING, true));

    misc_box.add(UnibrowPrefsUtil.createCheckBox("Keep hairline in view", UnibrowPrefsUtil.getTopNode(), 
      UnibrowHairline.PREF_KEEP_HAIRLINE_IN_VIEW, UnibrowHairline.default_keep_hairline_in_view));
    
    misc_box.add(UnibrowPrefsUtil.createCheckBox("Show DAS query genometry", UnibrowPrefsUtil.getTopNode(), 
      DasFeaturesAction2.PREF_SHOW_DAS_QUERY_GENOMETRY, DasFeaturesAction2.default_show_das_query_genometry));

    //misc_box.add(UnibrowPrefsUtil.createCheckBox("Sequence accessible", UnibrowPrefsUtil.getTopNode(), 
    //  IGB.PREF_SEQUENCE_ACCESSIBLE, IGB.default_sequence_accessible));

    if ( ! is_windows ) {
      misc_box.add(new JLabel("Browser command: "));
      // Default value is "", not WebBrowserControl.DEFAULT_BROWSER_CMD, to
      // force the WebBrowserControl to issue a warning.
      misc_box.add(UnibrowPrefsUtil.createTextField(
        UnibrowPrefsUtil.getTopNode(), WebBrowserControl.PREF_BROWSER_CMD, ""));
    }

    /*
    JPanel colors_box = new JPanel();
    colors_box.setLayout(new BoxLayout(colors_box, BoxLayout.Y_AXIS));
    colors_box.setBorder(new javax.swing.border.TitledBorder("Default colors"));
    main_box.add(colors_box);

    JButton fg_color = UnibrowPrefsUtil.createColorButton("Foreground", UnibrowPrefsUtil.getTopNode(), SeqMapView.PREF_DEFAULT_ANNOT_COLOR, SeqMapView.default_default_annot_color);
    JButton bg_color = UnibrowPrefsUtil.createColorButton("Background", UnibrowPrefsUtil.getTopNode(), SeqMapView.PREF_DEFAULT_BACKGROUND_COLOR, SeqMapView.default_default_background_color);
    colors_box.add(fg_color);
    colors_box.add(bg_color);
     */
    
    JPanel edge_match_box = new JPanel();
    edge_match_box.setLayout(new GridLayout(2,2));
    edge_match_box.setBorder(new javax.swing.border.TitledBorder("Edge matching"));

    JButton edge_match_color = UnibrowPrefsUtil.createColorButton(null, UnibrowPrefsUtil.getTopNode(), SeqMapView.PREF_EDGE_MATCH_COLOR, SeqMapView.default_edge_match_color);
    edge_match_box.add(new JLabel("Standard color: "));
    edge_match_box.add(edge_match_color);
    JButton fuzzy_edge_match_color = UnibrowPrefsUtil.createColorButton(null, UnibrowPrefsUtil.getTopNode(), SeqMapView.PREF_EDGE_MATCH_FUZZY_COLOR, SeqMapView.default_edge_match_fuzzy_color);
    edge_match_box.add(new JLabel("Fuzzy matching color: "));
    edge_match_box.add(fuzzy_edge_match_color);    
    
    JPanel orf_box = new JPanel();
    orf_box.setLayout(new GridLayout(2,2));
    orf_box.setBorder(new javax.swing.border.TitledBorder("ORF Analyzer"));

    JButton stop_codon_color = UnibrowPrefsUtil.createColorButton(null, UnibrowPrefsUtil.getTopNode(), OrfAnalyzer2.PREF_STOP_CODON_COLOR, OrfAnalyzer2.default_stop_codon_color);
    orf_box.add(new JLabel("Stop Codon: "));
    orf_box.add(stop_codon_color);
    JButton dynamic_orf_color = UnibrowPrefsUtil.createColorButton(null, UnibrowPrefsUtil.getTopNode(), OrfAnalyzer2.PREF_DYNAMIC_ORF_COLOR, OrfAnalyzer2.default_dynamic_orf_color);
    orf_box.add(new JLabel("Dynamic ORF: "));
    orf_box.add(dynamic_orf_color);

    JPanel axis_box = new JPanel();
    axis_box.setLayout(new GridLayout(3,2));
    axis_box.setBorder(new javax.swing.border.TitledBorder("Axis"));

    JButton axis_color_button2 = UnibrowPrefsUtil.createColorButton(null, UnibrowPrefsUtil.getTopNode(), SeqMapView.PREF_AXIS_COLOR, Color.BLACK);
    axis_box.add(new JLabel("Foreground: "));
    axis_box.add(axis_color_button2);

    JButton axis_back_color = UnibrowPrefsUtil.createColorButton(null, UnibrowPrefsUtil.getTopNode(), SeqMapView.PREF_AXIS_BACKGROUND, Color.WHITE);
    axis_box.add(new JLabel("Background: "));
    axis_box.add(axis_back_color);

    axis_box.add(new JLabel("Number format: "));
    String default_label_format = SeqMapView.VALUE_AXIS_LABEL_FORMAT_COMMA;
    String[] label_format_options = new String[] {SeqMapView.VALUE_AXIS_LABEL_FORMAT_FULL, SeqMapView.VALUE_AXIS_LABEL_FORMAT_COMMA};
    JComboBox axis_label_format_CB = UnibrowPrefsUtil.createComboBox(UnibrowPrefsUtil.getTopNode(), "Axis label format", label_format_options, default_label_format);
    axis_box.add(axis_label_format_CB);
    
    main_box.add(axis_box);
    main_box.add(edge_match_box);
    main_box.add(orf_box);
    main_box.add(misc_box);    
        
    validate();
  }

  public void destroy() {
    removeAll();
  }

  /** A main method for testing. */
  public static void main(String[] args) throws Exception {
    OptionsView p = new OptionsView();
   
    JDialog d = new JDialog();
    d.setTitle(p.getName());
    d.getContentPane().add(p);
    d.pack();
    
    d.setVisible(true);
    d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    d.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent e) {
        System.exit(0);
      }
    }
    );
  }
  
  public String getHelpTextHTML() {
    StringBuffer sb = new StringBuffer();

    sb.append("<h1>" + this.getName() + "</h1>\n");
    sb.append("<p>\n");
    sb.append("This panel allows you to change a variety of miscelaneous settings.  ");
    sb.append("In some cases, the changes will take effect immediately.  ");
    sb.append("In other cases, it will be necessary to shut-down and re-start the program before the changes take effect.  ");
    sb.append("</p>\n");
    
    sb.append("<p>\n");
    sb.append("<h2>Browser Command</h2>\n");
    sb.append("<b>Linux/Unix</b>: Set the command for opening a web address in your browser.  ");
    sb.append("Depending on your configuration, you may use something like ");
    sb.append("'firefox' or 'netscape', but you may need a full path like '/usr/bin/firefox'.  ");
    sb.append("<br><br>The command must accept the web address as a single argument on the command line.  ");
    sb.append("If you need to do something more sophisticated, you may define your own command script ");
    sb.append("for example '/home/user/openBrowser.sh'.  ");
    sb.append("<br><br><b>Macintosh X</b>: Set the command  to 'open' to use your default browser.  ");
    sb.append("<br><br><b>Windows</b>: This option will be hidden and your default browser will be used.  ");
    sb.append("<br><br>Changes do not require re-start.  ");
    sb.append("</p>\n");
    return sb.toString();
  }
  
  public Icon getIcon() {
    return null;
  }
  
  public String getToolTip() {
    return "Edit Miscelaneous Options";
  }
  
  public String getInfoURL() {
    return null;
  }   
  
  public void refresh() {
  }
  
}
