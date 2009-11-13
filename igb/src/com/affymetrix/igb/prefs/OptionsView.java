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

package com.affymetrix.igb.prefs;

import com.affymetrix.igb.glyph.CharSeqGlyph;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.view.OrfAnalyzer;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.UnibrowHairline;

/**
 *  A panel that shows the preferences for particular special URLs and file locations.
 */
public final class OptionsView extends JPanel implements IPrefEditorComponent, ActionListener  {

  //final LocationEditPanel edit_panel1 = new LocationEditPanel();
  JButton clear_prefsB = new JButton("Reset all preferences to defaults");

  public OptionsView() {
    super();
    this.setName("Other Options");
    this.setLayout(new BorderLayout());

    JPanel main_box = new JPanel();
    main_box.setLayout(new BoxLayout(main_box,BoxLayout.Y_AXIS));
    main_box.setBorder(new javax.swing.border.EmptyBorder(5,5,5,5));

  
    JScrollPane scroll_pane = new JScrollPane(main_box);
    this.add(scroll_pane, BorderLayout.CENTER);

    
    Box misc_box = Box.createVerticalBox();
    

   
    misc_box.add(UnibrowPrefsUtil.createCheckBox("Ask before exiting", UnibrowPrefsUtil.getTopNode(),
      UnibrowPrefsUtil.ASK_BEFORE_EXITING, UnibrowPrefsUtil.default_ask_before_exiting));
    misc_box.add(UnibrowPrefsUtil.createCheckBox("Keep hairline in view", UnibrowPrefsUtil.getTopNode(),
      UnibrowHairline.PREF_KEEP_HAIRLINE_IN_VIEW, UnibrowHairline.default_keep_hairline_in_view));

    

    misc_box.add(Box.createRigidArea(new Dimension(0,5)));

    misc_box.add(clear_prefsB);
    clear_prefsB.addActionListener(this);



    misc_box.add(Box.createRigidArea(new Dimension(0,5)));
	

    JPanel orf_box = new JPanel();
    orf_box.setLayout(new GridLayout(2,2));
    orf_box.setBorder(new javax.swing.border.TitledBorder("ORF Analyzer"));

    JButton stop_codon_color = UnibrowPrefsUtil.createColorButton(null, UnibrowPrefsUtil.getTopNode(), OrfAnalyzer.PREF_STOP_CODON_COLOR, OrfAnalyzer.default_stop_codon_color);
    orf_box.add(new JLabel("Stop Codon: "));
    orf_box.add(stop_codon_color);
    JButton dynamic_orf_color = UnibrowPrefsUtil.createColorButton(null, UnibrowPrefsUtil.getTopNode(), OrfAnalyzer.PREF_DYNAMIC_ORF_COLOR, OrfAnalyzer.default_dynamic_orf_color);
    orf_box.add(new JLabel("Dynamic ORF: "));
    orf_box.add(dynamic_orf_color);
	
	JPanel base_box = new JPanel();
    base_box.setLayout(new GridLayout(4,2));
    base_box.setBorder(new javax.swing.border.TitledBorder("Change Residue Colors"));

    JButton A_color = UnibrowPrefsUtil.createColorButton(null, UnibrowPrefsUtil.getTopNode(), CharSeqGlyph.PREF_A_COLOR, CharSeqGlyph.default_A_color);
	base_box.add(new JLabel("A: "));
	base_box.add(A_color);
	JButton T_color = UnibrowPrefsUtil.createColorButton(null, UnibrowPrefsUtil.getTopNode(), CharSeqGlyph.PREF_T_COLOR, CharSeqGlyph.default_T_color);
	base_box.add(new JLabel("T: "));
	base_box.add(T_color);
	JButton G_color = UnibrowPrefsUtil.createColorButton(null, UnibrowPrefsUtil.getTopNode(), CharSeqGlyph.PREF_G_COLOR, CharSeqGlyph.default_G_color);
	base_box.add(new JLabel("G: "));
	base_box.add(G_color);
	JButton C_color = UnibrowPrefsUtil.createColorButton(null, UnibrowPrefsUtil.getTopNode(), CharSeqGlyph.PREF_C_COLOR, CharSeqGlyph.default_C_color);
    base_box.add(new JLabel("C: "));
    base_box.add(C_color);
    


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
    String[] label_format_options = new String[] {SeqMapView.VALUE_AXIS_LABEL_FORMAT_FULL,
                                                  SeqMapView.VALUE_AXIS_LABEL_FORMAT_COMMA,
                                                  SeqMapView.VALUE_AXIS_LABEL_FORMAT_ABBREV};
    JComboBox axis_label_format_CB = UnibrowPrefsUtil.createComboBox(UnibrowPrefsUtil.getTopNode(), "Axis label format", label_format_options, default_label_format);
    axis_box.add(axis_label_format_CB);

    axis_box.setAlignmentX(0.0f);
   
    orf_box.setAlignmentX(0.0f);
    misc_box.setAlignmentX(0.0f);
	base_box.setAlignmentX(0.0f);

   
    main_box.add(axis_box);
   
    main_box.add(orf_box);
	main_box.add(base_box);
    main_box.add(Box.createRigidArea(new Dimension(0,5)));
    main_box.add(misc_box);

    validate();
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == clear_prefsB) {
      UnibrowPrefsUtil.clearPreferences(this);
    }
  }

 
  public String getHelpTextHTML() {
    StringBuffer sb = new StringBuffer();

    sb.append("<h1>" + this.getName() + "</h1>\n");
    sb.append("<p>\n");
    sb.append("This panel allows you to change a variety of miscellaneous settings.  ");
    sb.append("It is not necessary to re-start the program for these changes to take effect.  ");
    
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>Ask before exiting</h2>\n");
    sb.append("Whether to show a confirmation dialog before closing the program. ");
    sb.append("This can help you avoid accidentally losing your work.  ");
   
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>Keep hairline in view</h2>\n");
    sb.append("Whether to automatically prevent the hairline from moving ");
    sb.append("outside the view as you scroll.  ");
    sb.append("Recommend: true.");
   
    sb.append("</p>\n");

    return sb.toString();
  }

  public Icon getIcon() {
    return null;
  }

  public String getToolTip() {
    return "Edit Miscellaneous Options";
  }

  public String getInfoURL() {
    return null;
  }

  public void refresh() {
  }

}
