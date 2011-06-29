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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.glyph.ResidueColorHelper;
import com.affymetrix.igb.tiers.CoordinateStyle;
import com.affymetrix.igb.view.OrfAnalyzer;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.UnibrowHairline;
import com.affymetrix.igb.util.ColorUtils;
import com.jidesoft.combobox.ColorComboBox;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 *  A panel that shows the preferences for particular special URLs and file locations.
 */
public final class OptionsView extends IPrefEditorComponent implements ActionListener, PreferenceChangeListener {

	private ColorComboBox aColorComboBox;
	private JLabel aLabel;
	private ColorComboBox cColorComboBox;
	private JLabel cLabel;
	private JPanel coordinatePanel;
	private ColorComboBox dynamicORFColorComboBox;
	private JLabel dynamicORFLabel;
	private ColorComboBox gColorComboBox;
	private JLabel gLabel;
	private ColorComboBox otherColorComboBox;
	private JLabel otherLabel;
	private ColorComboBox stopCodonColorComboBox;
	private JLabel stopCodonLabel;
	private ColorComboBox tColorComboBox;
	private JLabel tLabel;
	private JCheckBox askBeforeExitCheckBox;
	private JLabel backgroundLabel;
	private ColorComboBox bgColorComboBox;
	private JCheckBox confirmBeforeDeleteCheckBox;
	private JCheckBox confirmBeforeLoadingCheckBox;
	private ColorComboBox fgColorComboBox;
	private JLabel foregroundLabel;
	private JCheckBox keepZoomStripeCheckBox;
	private JLabel numFormatLabel;
	private JPanel orfAnalyzerPanel;
	private JPanel residueColorPanel;
	private static final long serialVersionUID = 1L;
	private final SeqMapView smv;
	JButton clear_prefsB = new JButton("Reset all preferences to defaults");
	String default_label_format = SeqMapView.VALUE_COORDINATE_LABEL_FORMAT_COMMA;
	String[] label_format_options = new String[]{SeqMapView.VALUE_COORDINATE_LABEL_FORMAT_FULL,
		SeqMapView.VALUE_COORDINATE_LABEL_FORMAT_COMMA,
		SeqMapView.VALUE_COORDINATE_LABEL_FORMAT_ABBREV};
	JComboBox coordinates_label_format_CB = PreferenceUtils.createComboBox(PreferenceUtils.getTopNode(),
			"Coordinates label format",
			label_format_options,
			default_label_format);

	public OptionsView() {
		super();
		this.setName("Other Options");
		this.setToolTipText("Edit Miscellaneous Options");
		this.setLayout(new BorderLayout());

		Application igb = Application.getSingleton();
		if (igb != null) {
			smv = igb.getMapView();
		} else {
			smv = null;
		}

		JPanel main_box = new JPanel();

		JScrollPane scroll_pane = new JScrollPane(main_box);
		this.add(scroll_pane, BorderLayout.CENTER); //This line adds the components to the class panel
		clear_prefsB.addActionListener(this);

		coordinatePanel = new javax.swing.JPanel();
		backgroundLabel = new javax.swing.JLabel();
		foregroundLabel = new javax.swing.JLabel();
		numFormatLabel = new javax.swing.JLabel();
		coordinates_label_format_CB = PreferenceUtils.createComboBox(PreferenceUtils.getTopNode(),
				"Coordinates label format",
				label_format_options,
				default_label_format);
		;
		bgColorComboBox = ColorUtils.createColorComboBox(PreferenceUtils.getTopNode(), CoordinateStyle.PREF_COORDINATE_BACKGROUND, Color.WHITE, this);
		fgColorComboBox = ColorUtils.createColorComboBox(PreferenceUtils.getTopNode(), CoordinateStyle.PREF_COORDINATE_COLOR, Color.BLACK, this);
		orfAnalyzerPanel = new javax.swing.JPanel();
		stopCodonLabel = new javax.swing.JLabel();
		dynamicORFLabel = new javax.swing.JLabel();
		stopCodonColorComboBox = ColorUtils.createColorComboBox(PreferenceUtils.getTopNode(), OrfAnalyzer.PREF_STOP_CODON_COLOR, OrfAnalyzer.default_stop_codon_color, this);
		dynamicORFColorComboBox = ColorUtils.createColorComboBox(PreferenceUtils.getTopNode(), OrfAnalyzer.PREF_DYNAMIC_ORF_COLOR, OrfAnalyzer.default_dynamic_orf_color, this);
		residueColorPanel = new javax.swing.JPanel();
		aLabel = new javax.swing.JLabel();
		tLabel = new javax.swing.JLabel();
		gLabel = new javax.swing.JLabel();
		cLabel = new javax.swing.JLabel();
		otherLabel = new javax.swing.JLabel();
		aColorComboBox = ColorUtils.createColorComboBox(PreferenceUtils.getTopNode(), ResidueColorHelper.PREF_A_COLOR, ResidueColorHelper.default_A_color, this);
		tColorComboBox = ColorUtils.createColorComboBox(PreferenceUtils.getTopNode(), ResidueColorHelper.PREF_T_COLOR, ResidueColorHelper.default_T_color, this);
		gColorComboBox = ColorUtils.createColorComboBox(PreferenceUtils.getTopNode(), ResidueColorHelper.PREF_G_COLOR, ResidueColorHelper.default_G_color, this);
		cColorComboBox = ColorUtils.createColorComboBox(PreferenceUtils.getTopNode(), ResidueColorHelper.PREF_C_COLOR, ResidueColorHelper.default_C_color, this);
		otherColorComboBox = ColorUtils.createColorComboBox(PreferenceUtils.getTopNode(), ResidueColorHelper.PREF_OTHER_COLOR, ResidueColorHelper.default_other_color, this);
		askBeforeExitCheckBox = PreferenceUtils.createCheckBox("Ask before exit", PreferenceUtils.getTopNode(),
				PreferenceUtils.ASK_BEFORE_EXITING, PreferenceUtils.default_ask_before_exiting);
		confirmBeforeDeleteCheckBox = PreferenceUtils.createCheckBox("Confirm before delete", PreferenceUtils.getTopNode(),
				PreferenceUtils.CONFIRM_BEFORE_DELETE, PreferenceUtils.default_confirm_before_delete);
		keepZoomStripeCheckBox = PreferenceUtils.createCheckBox("Keep zoom stripe in view", PreferenceUtils.getTopNode(),
				UnibrowHairline.PREF_KEEP_HAIRLINE_IN_VIEW, UnibrowHairline.default_keep_hairline_in_view);
		confirmBeforeLoadingCheckBox = PreferenceUtils.createCheckBox("Confirm before loading large data set", PreferenceUtils.getTopNode(),
				PreferenceUtils.CONFIRM_BEFORE_LOAD, PreferenceUtils.default_confirm_before_load);
		

		coordinatePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Coordinates"));

		backgroundLabel.setText("Background:");

		foregroundLabel.setText("Foreground:");

		numFormatLabel.setText("Number format:");

		org.jdesktop.layout.GroupLayout CoordinatePanelLayout = new org.jdesktop.layout.GroupLayout(coordinatePanel);
		coordinatePanel.setLayout(CoordinatePanelLayout);
		CoordinatePanelLayout.setHorizontalGroup(
				CoordinatePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(CoordinatePanelLayout.createSequentialGroup().addContainerGap().add(CoordinatePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(CoordinatePanelLayout.createSequentialGroup().add(backgroundLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(bgColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(17, 17, 17).add(foregroundLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(fgColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(CoordinatePanelLayout.createSequentialGroup().add(numFormatLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(coordinates_label_format_CB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))).addContainerGap(63, Short.MAX_VALUE)));
		CoordinatePanelLayout.setVerticalGroup(
				CoordinatePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(CoordinatePanelLayout.createSequentialGroup().add(CoordinatePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(backgroundLabel).add(foregroundLabel).add(bgColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(fgColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(CoordinatePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(numFormatLabel).add(coordinates_label_format_CB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		orfAnalyzerPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("ORF Analyzer (Sliced View tab)"));

		stopCodonLabel.setText("Stop Codon:");

		dynamicORFLabel.setText("Dynamic ORF:");

		org.jdesktop.layout.GroupLayout orfAnalyzerPanelLayout = new org.jdesktop.layout.GroupLayout(orfAnalyzerPanel);
		orfAnalyzerPanel.setLayout(orfAnalyzerPanelLayout);
		orfAnalyzerPanelLayout.setHorizontalGroup(
				orfAnalyzerPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(orfAnalyzerPanelLayout.createSequentialGroup().addContainerGap().add(stopCodonLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(stopCodonColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(17, 17, 17).add(dynamicORFLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(dynamicORFColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addContainerGap(52, Short.MAX_VALUE)));
		orfAnalyzerPanelLayout.setVerticalGroup(
				orfAnalyzerPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(orfAnalyzerPanelLayout.createSequentialGroup().add(orfAnalyzerPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER).add(stopCodonLabel).add(dynamicORFLabel).add(stopCodonColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(dynamicORFColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		residueColorPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Residue Colors"));

		aLabel.setText("A:");

		tLabel.setText("T:");

		gLabel.setText("G:");

		cLabel.setText("C:");

		otherLabel.setText("Other:");

		org.jdesktop.layout.GroupLayout residueColorPanelLayout = new org.jdesktop.layout.GroupLayout(residueColorPanel);
		residueColorPanel.setLayout(residueColorPanelLayout);
		residueColorPanelLayout.setHorizontalGroup(
				residueColorPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(residueColorPanelLayout.createSequentialGroup().addContainerGap().add(aLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(aColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(tLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(tColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(13, 13, 13).add(gLabel).add(13, 13, 13).add(gColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(cLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(cColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(13, 13, 13).add(otherLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(otherColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addContainerGap(7, Short.MAX_VALUE)));
		residueColorPanelLayout.setVerticalGroup(
				residueColorPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(residueColorPanelLayout.createSequentialGroup().add(residueColorPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER).add(aLabel).add(aColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(gLabel).add(cLabel).add(otherLabel).add(tLabel).add(tColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(gColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(cColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(otherColorComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		//clear_prefsB.setText("Reset preference to defaults");

		org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(main_box);
		main_box.setLayout(layout);
		
		//GuiBuilder Layout Code
		layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(confirmBeforeLoadingCheckBox)
                            .add(layout.createSequentialGroup()
                                .add(askBeforeExitCheckBox)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(confirmBeforeDeleteCheckBox))
                            .add(keepZoomStripeCheckBox)
                            .add(coordinatePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(residueColorPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(orfAnalyzerPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .add(layout.createSequentialGroup()
                        .add(82, 82, 82)
                        .add(clear_prefsB)))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(coordinatePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(10, 10, 10)
                .add(residueColorPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 56, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(13, 13, 13)
                .add(orfAnalyzerPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 59, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(askBeforeExitCheckBox)
                    .add(confirmBeforeDeleteCheckBox))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(keepZoomStripeCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(2, 2, 2)
                .add(confirmBeforeLoadingCheckBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(clear_prefsB)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
		
		
		
		
		main_box.add(coordinatePanel);
		main_box.add(residueColorPanel);
		main_box.add(orfAnalyzerPanel);
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

	public void refresh() {
	}

	public void preferenceChange(PreferenceChangeEvent pce) {
		if (smv != null) {
			smv.getSeqMap().updateWidget();
		}
	}
}
