/**
 * Copyright (c) 2006 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.event.*;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.tiers.TrackConstants;
import com.jidesoft.combobox.ColorComboBox;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.*;

import javax.swing.*;

/**
 * For Panels that update the Track styles (as opposed to the track style defaults)
 */
public abstract class TrackPreferencesA extends TrackPreferencesGUI {
	private static final long serialVersionUID = 1L;
	protected IGBService igbService;

	public TrackPreferencesA(IGBService _igbService) {
		super();
		igbService = _igbService;
//		TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(this);
	}


	@Override
	protected void viewModeComboBoxActionPerformedA(ActionEvent evt) {
	    final JComboBox viewModeComboBox = getViewModeComboBox();
		String viewmode = (String)viewModeComboBox.getSelectedItem();
		for (TierGlyph tier : selectedTiers) {
			tier.getAnnotStyle().setViewMode(viewmode);
		}
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				igbService.getSeqMap().updateWidget();
			}
		});
	}

	@Override
	protected void floatCheckBoxActionPerformedA(ActionEvent evt) {
		final JCheckBox floatCheckBox = getFloatCheckBox();
		if (floatCheckBox.isSelected()) {
			GenericAction floatAction = GenericActionHolder.getInstance().getGenericAction("com.affymetrix.igb.action.FloatTiersAction");
			if (floatAction != null) {
				floatAction.actionPerformed(null);
			}
		}
		else {
			GenericAction unFloatAction = GenericActionHolder.getInstance().getGenericAction("com.affymetrix.igb.action.UnFloatTiersAction");
			if (unFloatAction != null) {
				unFloatAction.actionPerformed(null);
			}
		}
	}

	@Override
	protected void YAxisCheckBoxActionPerformedA(ActionEvent evt) {
		final JCheckBox yAxisCheckBox = getyAxisCheckBox();
		boolean b = yAxisCheckBox.isSelected();
		for (AbstractGraphGlyph gl : graphGlyphs) {
			gl.setShowAxis(b);
		}
		igbService.getSeqMap().updateWidget();
	}

	@Override
	protected void graphStyleHeatMapComboBoxActionPerformedA(ActionEvent evt) {
		if (graphGlyphs.isEmpty() || !is_listening) {
			return;
		}
		JComboBox heatMapComboBox = getGraphStyleHeatMapComboBox();
		String name = (String) heatMapComboBox.getSelectedItem();
		HeatMap hm = HeatMap.getStandardHeatMap(name);

		if (hm != null) {
			for (AbstractGraphGlyph gl : graphGlyphs) {
				if ("heatmapgraph".equals(gl.getName())) {
					gl.setShowGraph(true);
					gl.setHeatMap(hm);
				}
			}
			igbService.getSeqMap().updateWidget();
		}
	}

	@Override
	protected void labelSizeComboBoxActionPerformedA(ActionEvent evt) {
		final JComboBox labelSizeComboBox = getLabelSizeComboBox();
		int fontsize = (Integer)labelSizeComboBox.getSelectedItem();
		if (selectedTiers == null || fontsize <= 0) {
			return;
		}
		for (TierGlyph tier : selectedTiers) {
			ITrackStyleExtended style = tier.getAnnotStyle();
			float prev_font_size = style.getTrackNameSize();
			try {
				style.setTrackNameSize(fontsize);
			} catch (Exception ex) {
				style.setTrackNameSize(prev_font_size);
			}
		}
		igbService.getSeqMapView().setTierStyles();
		igbService.getSeqMapView().repackTheTiers(true, true);
	}

	@Override
	protected void colorSchemeComboBoxActionPerformedA(ActionEvent evt) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void labelFieldComboBoxActionPerformedA(ActionEvent evt) {
		final JComboBox labelFieldComboBox = getLabelSizeComboBox();
		int fontsize = (Integer)labelFieldComboBox.getSelectedItem();
		if (selectedTiers == null || fontsize <= 0) {
			return;
		}
		String labelField = (String)labelFieldComboBox.getSelectedItem();
		for (TierGlyph tier : selectedTiers) {
			ITrackStyleExtended style = tier.getAnnotStyle();
			style.setLabelField(labelField);
		}
		igbService.getSeqMapView().setTierStyles();
		igbService.getSeqMapView().repackTheTiers(true, true);
	}

	@Override
	protected void collapsedCheckBoxActionPerformedA(ActionEvent evt) {
	    final JCheckBox collapsedCheckBox = getCollapsedCheckBox();
		if (collapsedCheckBox.isSelected()) {
			GenericAction collapseAction = GenericActionHolder.getInstance().getGenericAction("com.affymetrix.igb.action.CollapseAction");
			if (collapseAction != null) {
				collapseAction.actionPerformed(evt);
			}
		}
		else {
			GenericAction expandAction = GenericActionHolder.getInstance().getGenericAction("com.affymetrix.igb.action.ExpandAction");
			if (expandAction != null) {
				expandAction.actionPerformed(evt);
			}
		}
	}

	@Override
	protected void strands2TracksCheckBoxActionPerformedA(ActionEvent evt) {
	    final JCheckBox strands2TracksCheckBox = getStrands2TracksCheckBox();
		if (strands2TracksCheckBox.isSelected()) {
			GenericAction strands2TracksAction = GenericActionHolder.getInstance().getGenericAction("com.affymetrix.igb.action.ShowTwoTiersAction");
			if (strands2TracksAction != null) {
				strands2TracksAction.actionPerformed(evt);
			}
		}
		else {
			GenericAction strands1TrackAction = GenericActionHolder.getInstance().getGenericAction("com.affymetrix.igb.action.ShowOneTierAction");
			if (strands1TrackAction != null) {
				strands1TrackAction.actionPerformed(evt);
			}
		}
	}

	@Override
	protected void strandsArrowCheckBoxActionPerformedA(ActionEvent evt) {
	    final JCheckBox strandsArrowCheckBox = getStrandsArrowCheckBox();
		if (strandsArrowCheckBox.isSelected()) {
			GenericAction setArrowAction = GenericActionHolder.getInstance().getGenericAction("com.affymetrix.igb.action.SetDirectionStyleArrowAction");
			if (setArrowAction != null) {
				setArrowAction.actionPerformed(evt);
			}
		}
		else {
			GenericAction unsetArrowAction = GenericActionHolder.getInstance().getGenericAction("com.affymetrix.igb.action.UnsetDirectionStyleArrowAction");
			if (unsetArrowAction != null) {
				unsetArrowAction.actionPerformed(evt);
			}
		}
	}

	@Override
	protected void strandsColorCheckBoxActionPerformedA(ActionEvent evt) {
	    final JCheckBox strandsColorCheckBox = getStrandsColorCheckBox();
		if (strandsColorCheckBox.isSelected()) {
			GenericAction setColorAction = GenericActionHolder.getInstance().getGenericAction("com.affymetrix.igb.action.SetDirectionStyleColorAction");
			if (setColorAction != null) {
				setColorAction.actionPerformed(evt);
			}
		}
		else {
			GenericAction unsetColorAction = GenericActionHolder.getInstance().getGenericAction("com.affymetrix.igb.action.UnsetDirectionStyleColorAction");
			if (unsetColorAction != null) {
				unsetColorAction.actionPerformed(evt);
			}
		}
	}

	@Override
	protected void labelColorComboBoxActionPerformedA(ActionEvent evt) {
		final ColorComboBox labelColorComboBox = getLabelColorComboBox();
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = labelColorComboBox.getSelectedColor();
		if (color != null) {
			for (TierGlyph tier : selectedTiers) {
				tier.getAnnotStyle().setLabelForeground(color);
			}
		}
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				igbService.getSeqMap().updateWidget();
			}
		});
	}

	@Override
	protected void strandsForwardColorComboBoxActionPerformedA(ActionEvent evt) {
	    final ColorComboBox strandsForwardColorComboBox = getStrandsForwardColorComboBox();
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = strandsForwardColorComboBox.getSelectedColor();
		if (color != null) {
			for (TierGlyph tier : selectedTiers) {
				tier.getAnnotStyle().setForwardColor(color);
			}
		}
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				igbService.getSeqMap().updateWidget();
			}
		});
	}

	@Override
	protected void strandsReverseColorComboBoxActionPerformedA(ActionEvent evt) {
	    final ColorComboBox strandsReverseColorComboBox = getStrandsReverseColorComboBox();
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = strandsReverseColorComboBox.getSelectedColor();
		if (color != null) {
			for (TierGlyph tier : selectedTiers) {
				tier.getAnnotStyle().setReverseColor(color);
			}
		}
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				igbService.getSeqMap().updateWidget();
			}
		});
	}

	@Override
	protected void buttonGroup1ActionPerformedA(ActionEvent evt) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void backgroundColorComboBoxActionPerformedA(ActionEvent evt) {
		final ColorComboBox backgroundColorComboBox = getBackgroundColorComboBox();
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = backgroundColorComboBox.getSelectedColor();
		if (color != null) {
			for (TierGlyph tier : selectedTiers) {
				tier.getAnnotStyle().setBackground(color);
			}
		}
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				igbService.getSeqMap().updateWidget();
			}
		});
	}

	@Override
	protected void foregroundColorComboBoxActionPerformedA(ActionEvent evt) {
		final ColorComboBox foregroundColorComboBox = getForgroundColorComboBox();
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = foregroundColorComboBox.getSelectedColor();
		if (color != null) {
			for (TierGlyph tier : selectedTiers) {
				tier.getAnnotStyle().setForeground(color);
			}
		}
		igbService.getSeqMapView().updatePanel();
	}

	@Override
	protected void stackDepthTextFieldActionPerformedA(ActionEvent evt) {
		final JTextField stackDepthTextField = getStackDepthTextField();
		String mdepth_string = stackDepthTextField.getText();
		if (selectedTiers == null || mdepth_string == null) {
			return;
		}
		for (TierGlyph tier : selectedTiers) {
			int prev_max_depth = tier.getAnnotStyle().getMaxDepth();
			try {
				tier.getAnnotStyle().setMaxDepth(Integer.parseInt(mdepth_string));
			} catch (Exception ex) {
				tier.getAnnotStyle().setMaxDepth(prev_max_depth);
			}
		}
		igbService.getSeqMapView().setTierStyles();
		igbService.getSeqMapView().repackTheTiers(true, true);
	}

	@Override
	protected void trackNameTextFieldActionPerformedA(ActionEvent evt) {
		final JTextField trackNameTextField = getTrackNameTextField();
		String name = trackNameTextField.getText();
		if (igbService.getSeqMapView() == null) {
			return;
		}
		if (selectedTiers != null) {
			igbService.getSeqMapView().renameTier(selectedTiers.get(0), name);
		}
	}

	@Override
	protected void viewModeComboBoxReset() {
		JComboBox viewModeComboBox = getViewModeComboBox();
		viewModeComboBox.removeAll();
		viewModeComboBox.setEnabled(selectedTiers.size() > 0);
		if (selectedTiers.size() == 1) {
			ITrackStyleExtended style = selectedTiers.get(0).getAnnotStyle();
			if (style == null || selectedTiers.get(0).getInfo() == null) {
				return;
			}
			List<MapViewGlyphFactoryI> viewModes = MapViewModeHolder.getInstance().getAllViewModesFor(style.getFileTypeCategory(), style.getMethodName());
			for (MapViewGlyphFactoryI viewmode : viewModes) {
				viewModeComboBox.addItem(viewmode.getName());
			}
			String viewmode = style.getViewMode();
			viewModeComboBox.setSelectedItem(viewmode);
		}
	}

	@Override
	protected void floatCheckBoxReset() {
		JCheckBox floatCheckBox = getFloatCheckBox();
		floatCheckBox.setEnabled(isAllGraph());
		boolean allFloat = isAllGraph();
		for (AbstractGraphGlyph glyph : graphGlyphs) {
			if (!glyph.getAnnotStyle().getFloatTier()) {
				allFloat = false;
				break;
			}
		}
		floatCheckBox.setSelected(allFloat);
	}

	@Override
	protected void YAxisCheckBoxReset() {
		JCheckBox yAxisCheckBox = getyAxisCheckBox();
		yAxisCheckBox.setEnabled(isAllGraph());
		boolean allYAxis = isAllGraph();
		for (AbstractGraphGlyph gg : graphGlyphs) {
			if (!gg.getShowAxis()) {
				allYAxis = false;
				break;
			}
		}
		yAxisCheckBox.setSelected(isAllGraph() && allYAxis);
	}

	@Override
	protected void graphStyleHeatMapComboBoxReset() {
		JComboBox heatMapComboBox = getGraphStyleHeatMapComboBox();
		if (isAllGraph()) {
			boolean allHeatMap = true;
			HeatMap heatMap = null;
			boolean heatMapSet = false;
			for (AbstractGraphGlyph gg : graphGlyphs) {
				if (gg.getGraphStyle() != GraphType.HEAT_MAP) {
					allHeatMap = false;
					break;
				}
				if (heatMap == null && !heatMapSet) {
					heatMap = gg.getHeatMap();
					heatMapSet = true;
				}
				else if (heatMap != gg.getHeatMap()) {
					heatMap = null;
					break;
				}
			}
			if (allHeatMap) {
				if (heatMap == null) {
					heatMapComboBox.setSelectedIndex(-1);
				}
				else {
					heatMapComboBox.setSelectedItem(heatMap.getName());
				}
			}
			else {
				heatMapComboBox.setEnabled(false);
			}
		}
		else {
			heatMapComboBox.setEnabled(false);
		}
	}

	@Override
	protected void labelSizeComboBoxReset() {
		JComboBox labelSizeComboBox = getLabelSizeComboBox();
		labelSizeComboBox.setEnabled(allGlyphs.size() > 0);
		Integer labelSize = -1;
		boolean labelSizeSet = false;
		for (ViewModeGlyph vg : allGlyphs) {
			if (labelSize == -1 && !labelSizeSet) {
				labelSize = (int)vg.getAnnotStyle().getTrackNameSize();
				labelSizeSet = true;
			}
			else if (labelSize != (int)vg.getAnnotStyle().getTrackNameSize()) {
				labelSize = -1;
				break;
			}
		}
		if (labelSize == -1) {
			labelSizeComboBox.setSelectedIndex(-1);
		}
		labelSizeComboBox.setSelectedItem(labelSize);
	}

	@Override
	protected void colorSchemeComboBoxReset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void labelFieldComboBoxReset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void collapsedCheckBoxReset() {
		JCheckBox collapsedCheckBox = getCollapsedCheckBox();
		collapsedCheckBox.setEnabled(isAllAnnot());
		boolean allCollapsed = isAllAnnot();
		for (ViewModeGlyph glyph : annotGlyphs) {
			if (!(glyph.getAnnotStyle().getExpandable() && glyph.getAnnotStyle().getCollapsed())) {
				allCollapsed = false;
				break;
			}
		}
		collapsedCheckBox.setSelected(allCollapsed);
	}

	@Override
	protected void strands2TracksCheckBoxReset() {
		JCheckBox strands2TracksCheckBox = getStrands2TracksCheckBox();
		strands2TracksCheckBox.setEnabled(isAllAnnot());
		boolean all2Tracks = isAllAnnot();
		for (ViewModeGlyph glyph : annotGlyphs) {
			if (!glyph.getAnnotStyle().getSeparate()) {
				all2Tracks = false;
				break;
			}
		}
		strands2TracksCheckBox.setSelected(all2Tracks);
	}

	private boolean isAllGraph() {
		return allGlyphs.size() == graphGlyphs.size() && graphGlyphs.size() > 0;
	}

	private boolean isAllAnnot() {
		return allGlyphs.size() == annotGlyphs.size() && annotGlyphs.size() > 0;
	}

	@Override
	protected void strandsArrowCheckBoxReset() {
		JCheckBox strandsArrowCheckBox = getStrandsArrowCheckBox();
		strandsArrowCheckBox.setEnabled(isAllAnnot());
		boolean allArrow = isAllAnnot();
		for (ViewModeGlyph glyph : annotGlyphs) {
			if (!(glyph.getAnnotStyle().getDirectionType() == TrackConstants.DIRECTION_TYPE.ARROW.ordinal() || glyph.getAnnotStyle().getDirectionType() == TrackConstants.DIRECTION_TYPE.BOTH.ordinal())) {
				allArrow = false;
				break;
			}
		}
		strandsArrowCheckBox.setSelected(allArrow);
	}

	private boolean isAllStrandsColor() {
		boolean allColor = true;
		for (ViewModeGlyph glyph : annotGlyphs) {
			if (!(glyph.getAnnotStyle().getDirectionType() == TrackConstants.DIRECTION_TYPE.ARROW.ordinal() || glyph.getAnnotStyle().getDirectionType() == TrackConstants.DIRECTION_TYPE.BOTH.ordinal())) {
				allColor = false;
				break;
			}
		}
		return allColor;
	}

	@Override
	protected void strandsColorCheckBoxReset() {
		JCheckBox strandsColorCheckBox = getStrandsColorCheckBox();
		strandsColorCheckBox.setEnabled(isAllAnnot());
		strandsColorCheckBox.setSelected(isAllAnnot() && isAllStrandsColor());
		getStrandsLabel().setEnabled(isAllAnnot());
	}

	@Override
	protected void strandsForwardColorComboBoxReset() {
		ColorComboBox strandsForwardColorComboBox = getStrandsForwardColorComboBox();
		strandsForwardColorComboBox.setEnabled(isAllAnnot() && isAllStrandsColor());
		getStrandsForwardColorLabel().setEnabled(isAllAnnot() && isAllStrandsColor());
		Color strandsForwardColor = null;
		if (isAllAnnot() && isAllStrandsColor()) {
			boolean strandsForwardColorSet = false;
			for (ViewModeGlyph ag : annotGlyphs) {
				if (strandsForwardColor == null && !strandsForwardColorSet) {
					strandsForwardColor = ag.getAnnotStyle().getForwardColor();
					strandsForwardColorSet = true;
				}
				else if (strandsForwardColor != ag.getAnnotStyle().getForwardColor()) {
					strandsForwardColor = null;
					break;
				}
			}
		}
		strandsForwardColorComboBox.setBackground(strandsForwardColor);
	}

	@Override
	protected void strandsReverseColorComboBoxReset() {
		ColorComboBox strandsReverseColorComboBox = getStrandsReverseColorComboBox();
		strandsReverseColorComboBox.setEnabled(isAllAnnot() && isAllStrandsColor());
		getStrandsReverseColorLabel().setEnabled(isAllAnnot() && isAllStrandsColor());
		Color strandsReverseColor = null;
		if (isAllAnnot() && isAllStrandsColor()) {
			boolean strandsReverseColorSet = false;
			for (ViewModeGlyph ag : annotGlyphs) {
				if (strandsReverseColor == null && !strandsReverseColorSet) {
					strandsReverseColor = ag.getAnnotStyle().getReverseColor();
					strandsReverseColorSet = true;
				}
				else if (strandsReverseColor != ag.getAnnotStyle().getReverseColor()) {
					strandsReverseColor = null;
					break;
				}
			}
		}
		strandsReverseColorComboBox.setBackground(strandsReverseColor);
	}

	@Override
	protected void buttonGroup1Reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void labelColorComboBoxReset() {
		ColorComboBox labelColorComboBox = getLabelColorComboBox();
		labelColorComboBox.setEnabled(allGlyphs.size() > 0);
		getLabelColorLabel().setEnabled(allGlyphs.size() > 0);
		Color labelColor = null;
		boolean labelColorSet = false;
		for (ViewModeGlyph ag : annotGlyphs) {
			if (labelColor == null && !labelColorSet) {
				labelColor = ag.getAnnotStyle().getBackground();
				labelColorSet = true;
			}
			else if (labelColor != ag.getAnnotStyle().getReverseColor()) {
				labelColor = null;
				break;
			}
		}
		labelColorComboBox.setBackground(labelColor);
	}

	@Override
	protected void backgroundColorComboBoxReset() {
		ColorComboBox backgroundColorComboBox = getBackgroundColorComboBox();
		backgroundColorComboBox.setEnabled(allGlyphs.size() > 0);
		getBackgroundColorLabel().setEnabled(allGlyphs.size() > 0);
		Color backgroundColor = null;
		boolean backgroundColorSet = false;
		for (ViewModeGlyph ag : annotGlyphs) {
			if (backgroundColor == null && !backgroundColorSet) {
				backgroundColor = ag.getAnnotStyle().getBackground();
				backgroundColorSet = true;
			}
			else if (backgroundColor != ag.getAnnotStyle().getReverseColor()) {
				backgroundColor = null;
				break;
			}
		}
		backgroundColorComboBox.setBackground(backgroundColor);
	}

	@Override
	protected void foregroundColorComboBoxReset() {
		ColorComboBox forgroundColorComboBox = getBackgroundColorComboBox();
		forgroundColorComboBox.setEnabled(allGlyphs.size() > 0);
		getForegroundColorLabel().setEnabled(allGlyphs.size() > 0);
		Color forgroundColor = null;
		boolean forgroundColorSet = false;
		for (ViewModeGlyph ag : annotGlyphs) {
			if (forgroundColor == null && !forgroundColorSet) {
				forgroundColor = ag.getAnnotStyle().getBackground();
				forgroundColorSet = true;
			}
			else if (forgroundColor != ag.getAnnotStyle().getReverseColor()) {
				forgroundColor = null;
				break;
			}
		}
		forgroundColorComboBox.setBackground(forgroundColor);
	}

	@Override
	protected void stackDepthTextFieldReset() {
		JTextField stackDepthTextField = getStackDepthTextField();
		stackDepthTextField.setEnabled(annotGlyphs.size() > 0);
		getStackDepthLabel().setEnabled(annotGlyphs.size() > 0);
		Integer stackDepth = -1;
		boolean stackDepthSet = false;
		for (ViewModeGlyph ag : annotGlyphs) {
			if (stackDepth == -1 && !stackDepthSet) {
				stackDepth = ag.getAnnotStyle().getMaxDepth();
				stackDepthSet = true;
			}
			else if (stackDepth != ag.getAnnotStyle().getMaxDepth()) {
				stackDepth = -1;
				break;
			}
		}
		if (stackDepth == -1) {
			stackDepthTextField.setText("");
		}
		else {
			stackDepthTextField.setText("" + stackDepth);
		}
	}

	@Override
	protected void trackNameTextFieldReset() {
		JTextField trackNameTextField = getTrackNameTextField();
		trackNameTextField.setEnabled(allGlyphs.size() == 1);
		if (allGlyphs.size() == 1) {
			trackNameTextField.setText(allGlyphs.get(0).getLabel());
		}
		else {
			trackNameTextField.setText("");
		}
	}
}
