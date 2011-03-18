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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.glyph.ResidueColorHelper;
import com.affymetrix.igb.tiers.AxisStyle;
import com.affymetrix.igb.view.OrfAnalyzer;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.UnibrowHairline;
import com.affymetrix.igb.util.ColorUtils;
import com.affymetrix.igb.view.load.AutoLoad;

/**
 *  A panel that shows the preferences for particular special URLs and file locations.
 */
public final class OptionsView extends IPrefEditorComponent implements ActionListener  {
  private static final long serialVersionUID = 1L;

  //final LocationEditPanel edit_panel1 = new LocationEditPanel();
  JButton clear_prefsB = new JButton("Reset all preferences to defaults");

  public OptionsView() {
    super();
    this.setName("Other Options");
	this.setToolTipText("Edit Miscellaneous Options");
    this.setLayout(new BorderLayout());

    JPanel main_box = new JPanel();
    main_box.setLayout(new BoxLayout(main_box,BoxLayout.Y_AXIS));
    main_box.setBorder(new javax.swing.border.EmptyBorder(5,5,5,5));

  
    JScrollPane scroll_pane = new JScrollPane(main_box);
    this.add(scroll_pane, BorderLayout.CENTER);
	clear_prefsB.addActionListener(this);
    
    Box misc_box = Box.createVerticalBox();
	
	Box misc_box_cols = Box.createHorizontalBox();
	Box misc_box_col1 = Box.createVerticalBox();
	Box misc_box_col2 = Box.createVerticalBox();

	misc_box_col1.setAlignmentX(Component.TOP_ALIGNMENT);
    misc_box_col1.add(PreferenceUtils.createCheckBox("Ask before exiting", PreferenceUtils.getTopNode(),
      PreferenceUtils.ASK_BEFORE_EXITING, PreferenceUtils.default_ask_before_exiting));
    misc_box_col1.add(PreferenceUtils.createCheckBox("Keep zoom stripe in view", PreferenceUtils.getTopNode(),
      UnibrowHairline.PREF_KEEP_HAIRLINE_IN_VIEW, UnibrowHairline.default_keep_hairline_in_view));

	misc_box_col2.setAlignmentX(Component.TOP_ALIGNMENT);
    misc_box_col2.add(PreferenceUtils.createCheckBox("Confirm before delete", PreferenceUtils.getTopNode(),
      PreferenceUtils.CONFIRM_BEFORE_DELETE, PreferenceUtils.default_confirm_before_delete));
    misc_box_col2.add(PreferenceUtils.createCheckBox("Confirm before load", PreferenceUtils.getTopNode(),
      PreferenceUtils.RESET_LOAD_CONFIRM_BOX_OPTION, PreferenceUtils.default_confirm_before_delete));

    misc_box_cols.add(misc_box_col1);
	misc_box_cols.add(Box.createHorizontalGlue());
	misc_box_cols.add(misc_box_col2);
	misc_box_cols.add(Box.createRigidArea(new Dimension(100,0)));

	misc_box.add(misc_box_cols);
    misc_box.add(Box.createRigidArea(new Dimension(0,5))); 
    misc_box.add(Box.createRigidArea(new Dimension(0,5)));
	

    Box orf_box = Box.createVerticalBox();
    orf_box.setBorder(new javax.swing.border.TitledBorder("ORF Analyzer"));

	orf_box.add(addColorChooser("Stop Codon",OrfAnalyzer.PREF_STOP_CODON_COLOR, OrfAnalyzer.default_stop_codon_color));
	orf_box.add(addColorChooser("Dynamic ORF",OrfAnalyzer.PREF_DYNAMIC_ORF_COLOR, OrfAnalyzer.default_dynamic_orf_color));
	
	Box base_box = Box.createVerticalBox();
    base_box.setBorder(new javax.swing.border.TitledBorder("Change Residue Colors"));

	base_box.add(addColorChooser("A", ResidueColorHelper.PREF_A_COLOR, ResidueColorHelper.default_A_color));
	base_box.add(addColorChooser("T", ResidueColorHelper.PREF_T_COLOR, ResidueColorHelper.default_T_color));
	base_box.add(addColorChooser("G", ResidueColorHelper.PREF_G_COLOR, ResidueColorHelper.default_G_color));
	base_box.add(addColorChooser("C", ResidueColorHelper.PREF_C_COLOR, ResidueColorHelper.default_C_color));
	base_box.add(addColorChooser("Other", ResidueColorHelper.PREF_OTHER_COLOR, ResidueColorHelper.default_other_color));


	String default_label_format = SeqMapView.VALUE_AXIS_LABEL_FORMAT_COMMA;
    String[] label_format_options = new String[] {SeqMapView.VALUE_AXIS_LABEL_FORMAT_FULL,
                                                  SeqMapView.VALUE_AXIS_LABEL_FORMAT_COMMA,
                                                  SeqMapView.VALUE_AXIS_LABEL_FORMAT_ABBREV};
    JComboBox axis_label_format_CB = PreferenceUtils.createComboBox(PreferenceUtils.getTopNode(), "Axis label format", label_format_options, default_label_format);

    Box axis_box = Box.createVerticalBox();
    axis_box.setBorder(new javax.swing.border.TitledBorder("Axis"));

	axis_box.add(addColorChooser("Foreground", AxisStyle.PREF_AXIS_COLOR, Color.BLACK));
	axis_box.add(addColorChooser("Background", AxisStyle.PREF_AXIS_BACKGROUND, Color.WHITE));
	axis_box.add(addToPanel("Number format", axis_label_format_CB));
    

    axis_box.setAlignmentX(0.0f);
    orf_box.setAlignmentX(0.0f);
    misc_box.setAlignmentX(0.0f);
	base_box.setAlignmentX(0.0f);

   
    main_box.add(axis_box);
    main_box.add(orf_box);
	main_box.add(base_box);
    main_box.add(Box.createRigidArea(new Dimension(0,5)));
    main_box.add(misc_box);
	main_box.add(clear_prefsB);

    validate();
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == clear_prefsB) {
      // The option pane used differs from the confirmDialog only in
		 // that "No" is the default choice.
		 String[] options = {"Yes", "No"};
		 if (JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(
						 this, "Really reset all preferences to defaults?\n(this will also exit the application)", "Clear preferences?",
						 JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
						 options, options[1])) {

			 try {
				 PreferenceUtils.clearPreferences();
				 System.exit(0);
			 } catch (Exception e) {
				 ErrorHandler.errorPanel("ERROR", "Error clearing preferences", e);
			 }
		 }
    }
  }

  private static JPanel addColorChooser(String label_str, String pref_name, Color default_color) {
		JComponent component = ColorUtils.createColorComboBox(PreferenceUtils.getTopNode(), pref_name, default_color);
		return addToPanel(label_str, component);
	}

  private static JPanel addToPanel(String label_str, JComponent component) {
		Dimension size = new Dimension(100, 22);
        component.setMaximumSize(size);
        component.setPreferredSize(size);
        component.setMinimumSize(size);
		component.setAlignmentX(Component.RIGHT_ALIGNMENT);

		JPanel inner_panel = new JPanel();
		inner_panel.setLayout(new BoxLayout(inner_panel,BoxLayout.PAGE_AXIS));
		inner_panel.add(Box.createRigidArea(new Dimension(20,0)));
		inner_panel.add(component);

		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 2));
		panel.add(new JLabel(label_str + ": "));
		panel.add(inner_panel);
		
		return panel;
	}

  public void refresh() {
  }
}
