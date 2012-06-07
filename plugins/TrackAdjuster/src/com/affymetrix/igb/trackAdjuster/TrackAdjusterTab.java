/**
 * Copyright (c) 2006 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.trackAdjuster;

import com.affymetrix.genoviz.color.ColorSchemeComboBox;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;

import java.awt.FlowLayout;
import java.util.*;
import javax.swing.*;

/**
 * TrackAdjusterTab consists of two GUIBuilder designed panels, the
 * TrackPreferencesGUI, which is a common Panel, and the YScaleAxisGUI
 * used only byt TrackAdjuster. Since TrackPreferencesGUI is common, it
 * does not handle user actions, the TrackAdjusterTab handles them. The
 * YScaleAxisGUI handles it's own actions.
 */
public final class TrackAdjusterTab extends IGBTabPanel {
	private static final long serialVersionUID = 1L;
	//System.out.println() statements do not show on the screen, they are not translated.
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("trackAdjuster");
	private static final int TAB_POSITION = 3;
    public ButtonGroup stylegroup = new ButtonGroup();
    public JPanel rangePanel = new JPanel();

	public boolean is_listening = true; // used to turn on and off listening to GUI events
	public GraphVisibleBoundsSetter vis_bounds_setter;

	private JRadioButton graphP_hidden_styleB;
    private ColorSchemeComboBox colorSchemeComboBox;

	private IGBService igbService;
	private TrackPreferencesSeqMapViewPanel trackPreferencesSeqMapViewPanel;
	private YScaleAxisGUI yScaleAxisGUI;

	public TrackAdjusterTab(IGBService _igbService) {
		super(_igbService, BUNDLE.getString("trackAdjusterTab"), BUNDLE.getString("trackAdjusterTab"), false, TAB_POSITION);
		igbService = _igbService;
		vis_bounds_setter = new GraphVisibleBoundsSetter(igbService.getSeqMap());
		setLayout(new FlowLayout());
		trackPreferencesSeqMapViewPanel = new TrackPreferencesSeqMapViewPanel(igbService);
		yScaleAxisGUI = new YScaleAxisGUI(this);
	    add(trackPreferencesSeqMapViewPanel);
	    add(yScaleAxisGUI);
		igbService.addListSelectionListener(colorSchemeComboBox);
		graphP_hidden_styleB = new JRadioButton();
		trackPreferencesSeqMapViewPanel.getButtonGroup1().add(graphP_hidden_styleB);
		graphP_hidden_styleB.setSelected(true); // deselect all visible radio buttons
	}
}
