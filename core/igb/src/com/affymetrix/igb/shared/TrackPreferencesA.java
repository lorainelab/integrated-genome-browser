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

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.symmetry.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.color.ColorSchemeComboBox;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.action.ChangeExpandMaxOptimizeAction;
import com.affymetrix.igb.graphTypes.BarGraphGlyph;
import com.affymetrix.igb.graphTypes.DotGraphGlyph;
import com.affymetrix.igb.graphTypes.HeatMapGraphGlyph;
import com.affymetrix.igb.graphTypes.LineGraphGlyph;
import com.affymetrix.igb.graphTypes.MinMaxAvgGraphGlyph;
import com.affymetrix.igb.graphTypes.StairStepGraphGlyph;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.GraphGlyph.GraphStyle;
import com.affymetrix.igb.tiers.TrackConstants;
import com.affymetrix.igb.viewmode.DynamicStyleHeatMap;
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
//igbService.addListSelectionListener(getColorSchemeComboBox());
	}

	private void updateDisplay() {
		updateDisplay(true, true);
	}

	private void updateDisplay(final boolean preserveX, final boolean preserveY){
		ThreadUtils.runOnEventQueue(new Runnable() {
	
			public void run() {
//				igbService.getSeqMap().updateWidget();
//				igbService.getSeqMapView().setTierStyles();
//				igbService.getSeqMapView().repackTheTiers(true, true);
				igbService.getSeqMapView().updatePanel(preserveX, preserveY);
			}
		});
	}
	
	private void refreshView() {
		ThreadUtils.runOnEventQueue(new Runnable() {	
			public void run() {
				igbService.getSeqMap().updateWidget();
			}
		});
	}
	
	@Override
	protected void viewModeComboBoxActionPerformedA(ActionEvent evt) {

	}

	@Override
	protected void floatCheckBoxActionPerformedA(ActionEvent evt) {
		final JCheckBox floatCheckBox = getFloatCheckBox();
	    String actionId = floatCheckBox.isSelected() ?
	    	"com.affymetrix.igb.action.FloatTiersAction" :
	    	"com.affymetrix.igb.action.UnFloatTiersAction";
		GenericAction action = GenericActionHolder.getInstance().getGenericAction(actionId);
		if (action != null) {
			action.actionPerformed(evt);
		}
//		updateDisplay();
	}

	@Override
	protected void labelCheckBoxActionPerformedA(ActionEvent evt) {
		final JCheckBox labelCheckBox = getLabelCheckBox();
		boolean b = labelCheckBox.isSelected();
		for (GraphGlyph gl : graphGlyphs) {
			gl.setShowLabel(b);
		}
		updateDisplay();
	}

	@Override
	protected void YAxisCheckBoxActionPerformedA(ActionEvent evt) {
		final JCheckBox YAxisCheckBox = getYAxisCheckBox();
		boolean b = YAxisCheckBox.isSelected();
		for (GraphGlyph gl : graphGlyphs) {
			gl.setShowAxis(b);
		}
		updateDisplay();
	}

	@Override
	protected void graphStyleHeatMapComboBoxActionPerformedA(ActionEvent evt) {
		if (graphGlyphs.isEmpty() || !is_listening) {
			return;
		}
		JComboBox heatMapComboBox = getGraphStyleHeatMapComboBox();
		String name = (String) heatMapComboBox.getSelectedItem();
		if (name == null) {
			return;
		}
		if (HeatMap.FOREGROUND_BACKGROUND.equals(name)) {
			for (GraphGlyph gl : graphGlyphs) {
				if ("heatmapgraph".equals(gl.getName())) {
					gl.setShowGraph(true);
					if (!(gl.getHeatMap() instanceof DynamicStyleHeatMap)) {
						gl.setHeatMap(new DynamicStyleHeatMap(HeatMap.FOREGROUND_BACKGROUND, gl.getAnnotStyle(), 0.0f, 0.5f));
					}
				}
			}
		}
		else {
			HeatMap hm = HeatMap.getStandardHeatMap(name);
			if (hm != null) {
				for (GraphGlyph gl : graphGlyphs) {
					if ("heatmapgraph".equals(gl.getName())) {
						gl.setShowGraph(true);
						gl.setHeatMap(hm);
					}
				}
			}
		}
		refreshView();
	}

	@Override
	protected void labelSizeComboBoxActionPerformedA(ActionEvent evt) {
		final JComboBox labelSizeComboBox = getLabelSizeComboBox();
		int fontsize = (Integer)labelSizeComboBox.getSelectedItem();
		if (fontsize <= 0) {
			return;
		}
		ParameteredAction action = (ParameteredAction) GenericActionHolder.getInstance()
				.getGenericAction("com.affymetrix.igb.action.TierFontSizeAction");
		action.performAction(fontsize);
		updateDisplay();
	}

	@Override
	protected void colorSchemeComboBoxActionPerformedA(ActionEvent evt) {
	}

	@Override
	protected void labelFieldComboBoxActionPerformedA(ActionEvent evt) {
		final JComboBox labelFieldComboBox = getLabelFieldComboBox();
		String labelField = (String)labelFieldComboBox.getSelectedItem();
		if (labelField == null) {
			return;
		}
		ParameteredAction action = (ParameteredAction) GenericActionHolder.getInstance()
				.getGenericAction("com.affymetrix.igb.action.LabelGlyphAction");
		action.performAction(labelField);
		updateDisplay();
	}

	@Override
	protected void strands2TracksCheckBoxActionPerformedA(ActionEvent evt) {
	    final JCheckBox strands2TracksCheckBox = getStrands2TracksCheckBox();
	    String actionId = strands2TracksCheckBox.isSelected() ?
			"com.affymetrix.igb.action.ShowTwoTiersAction" :
			"com.affymetrix.igb.action.ShowOneTierAction";
		GenericAction action = GenericActionHolder.getInstance().getGenericAction(actionId);
		if (action != null) {
			action.actionPerformed(evt);
		}
		updateDisplay();
	}

	@Override
	protected void strandsArrowCheckBoxActionPerformedA(ActionEvent evt) {
	    final JCheckBox strandsArrowCheckBox = getStrandsArrowCheckBox();
		String actionId = strandsArrowCheckBox.isSelected() ?
			"com.affymetrix.igb.action.SetDirectionStyleArrowAction" :
			"com.affymetrix.igb.action.UnsetDirectionStyleArrowAction";
		GenericAction action = GenericActionHolder.getInstance().getGenericAction(actionId);
		if (action != null) {
			action.actionPerformed(evt);
		}
		updateDisplay();
	}

	@Override
	protected void strandsColorCheckBoxActionPerformedA(ActionEvent evt) {
	    final JCheckBox strandsColorCheckBox = getStrandsColorCheckBox();
		String actionId = strandsColorCheckBox.isSelected() ?
			"com.affymetrix.igb.action.SetDirectionStyleColorAction" :
			"com.affymetrix.igb.action.UnsetDirectionStyleColorAction";
		GenericAction action = GenericActionHolder.getInstance().getGenericAction(actionId);
		if (action != null) {
			action.actionPerformed(evt);
		}
		is_listening = false;
		strandsForwardColorComboBoxReset();
		strandsReverseColorComboBoxReset();
		is_listening = true;
		updateDisplay();
	}

	
	@Override
	protected void buttonGroup1ActionPerformedA(ActionEvent evt) {
		for (GraphGlyph graphGlyph : graphGlyphs) {
			GraphStyle selectedMode = null;
			if (getGraphStyleLineRadioButton().isSelected()) {
				selectedMode = new LineGraphGlyph(graphGlyph);
			}
			if (getGraphStyleBarRadioButton().isSelected()) {
				selectedMode = new BarGraphGlyph(graphGlyph);
			}
			if (getGraphStyleStairStepRadioButton().isSelected()) {
				selectedMode = new StairStepGraphGlyph(graphGlyph);
			}
			if (getGraphStyleDotRadioButton().isSelected()) {
				selectedMode = new DotGraphGlyph(graphGlyph);
			}
			if (getGraphStyleMinMaxAvgRadioButton().isSelected()) {
				selectedMode = new MinMaxAvgGraphGlyph(graphGlyph);
			}
			if (getGraphStyleHeatMapRadioButton().isSelected()) {
				selectedMode = new HeatMapGraphGlyph(graphGlyph);
			}
			graphGlyph.setGraphStyle(selectedMode);
		}
		
		buttonGroup1Reset();
		graphStyleHeatMapComboBoxReset();
		refreshView();
	}
	
	@Override
	protected void labelColorComboBoxActionPerformedA(ActionEvent evt) {
		final ColorComboBox labelColorComboBox = getLabelColorComboBox();
		Color color = labelColorComboBox.getSelectedColor();
		ParameteredAction action = (ParameteredAction) GenericActionHolder.getInstance()
				.getGenericAction("com.affymetrix.igb.action.ChangeLabelColorAction");
		if (action != null && color != null) {
			action.performAction(color);
		}
		updateDisplay();
	}

	@Override
	protected void strandsForwardColorComboBoxActionPerformedA(ActionEvent evt) {
	    final ColorComboBox strandsForwardColorComboBox = getStrandsForwardColorComboBox();
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = strandsForwardColorComboBox.getSelectedColor();
		ParameteredAction action = (ParameteredAction) GenericActionHolder.getInstance()
				.getGenericAction("com.affymetrix.igb.action.ChangeForwardColorAction");
		if (action != null && color != null) {
			action.performAction(color);
		}
		updateDisplay();
	}

	@Override
	protected void strandsReverseColorComboBoxActionPerformedA(ActionEvent evt) {
	    final ColorComboBox strandsReverseColorComboBox = getStrandsReverseColorComboBox();
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = strandsReverseColorComboBox.getSelectedColor();
		ParameteredAction action = (ParameteredAction) GenericActionHolder.getInstance()
				.getGenericAction("com.affymetrix.igb.action.ChangeReverseColorAction");
		if (action != null && color != null) {
			action.performAction(color);
		}
		updateDisplay();
	}

	@Override
	protected void backgroundColorComboBoxActionPerformedA(ActionEvent evt) {
		final ColorComboBox backgroundColorComboBox = getBackgroundColorComboBox();
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = backgroundColorComboBox.getSelectedColor();
		ParameteredAction action = (ParameteredAction) GenericActionHolder.getInstance()
				.getGenericAction("com.affymetrix.igb.action.ChangeBackgroundColorAction");
		if (action != null && color != null) {
			action.performAction(color);
		}
		updateDisplay();
	}

	@Override
	protected void foregroundColorComboBoxActionPerformedA(ActionEvent evt) {
		final ColorComboBox foregroundColorComboBox = getForegroundColorComboBox();
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = foregroundColorComboBox.getSelectedColor();
		ParameteredAction action = (ParameteredAction) GenericActionHolder.getInstance()
				.getGenericAction("com.affymetrix.igb.action.ChangeForegroundColorAction");
		if (action != null && color != null) {
			action.performAction(color);
		}
		updateDisplay();
	}

	private void setStackDepth() {
		final JTextField stackDepthTextField = getStackDepthTextField();
		String mdepth_string = stackDepthTextField.getText();
		if (mdepth_string == null) {
			return;
		}
		
		ParameteredAction action = (ParameteredAction) GenericActionHolder.getInstance()
				.getGenericAction("com.affymetrix.igb.action.ChangeExpandMaxAction");
		try{
			action.performAction(Integer.parseInt(mdepth_string));
			updateDisplay(true, false);
		}catch(Exception ex){
			ErrorHandler.errorPanel("Invalid value "+mdepth_string);
		}
	}

	@Override
	protected void stackDepthTextFieldActionPerformedA(ActionEvent evt) {
		setStackDepth();
	}

	@Override
	protected void trackNameTextFieldActionPerformedA(ActionEvent evt) {
		final JTextField trackNameTextField = getTrackNameTextField();
		String name = trackNameTextField.getText();
		if (igbService.getSeqMapView() == null || allGlyphs == null || allGlyphs.isEmpty()) {
			return;
		}
		ParameteredAction action = (ParameteredAction) GenericActionHolder.getInstance()
				.getGenericAction("com.affymetrix.igb.action.RenameTierAction");
		if (allGlyphs.size() == 1) {
			action.performAction(allGlyphs.get(0).getAnnotStyle(), name);
		} else if (isAllGraph()){ // Special case for joined graph
			if(isOneJoined()){
				action.performAction((graphGlyphs.get(0)).getGraphState().getComboStyle(), name);
			}
		}
	}

	@Override
	protected void stackDepthGoButtonActionPerformedA(ActionEvent evt) {
		setStackDepth();
	}

	@Override
	protected void stackDepthAllButtonActionPerformedA(ActionEvent evt) {
		getStackDepthTextField().setText("" + ChangeExpandMaxOptimizeAction.getAction().getOptimum());
	}

	@Override
	protected void selectAllButtonActionPerformedA(ActionEvent evt) {
	}

	@Override
	protected void hideButtonActionPerformedA(ActionEvent evt) {
	}

	@Override
	protected void clearButtonActionPerformedA(ActionEvent evt) {
	}

	@Override
	protected void restoreToDefaultButtonActionPerformedA(ActionEvent evt) {
	}

	private boolean isAllGraph() {
		return allGlyphs.size() == graphGlyphs.size() && graphGlyphs.size() > 0;
	}

	private boolean isAllAnnot() {
		return allGlyphs.size() == annotGlyphs.size() && annotGlyphs.size() > 0;
	}

	private boolean isAnyJoined(){
		for (GraphGlyph gg : graphGlyphs) {
			if (gg.getGraphState().getComboStyle() != null) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isOneJoined(){
		if(graphGlyphs.size() < 2)
			return false;
		
		Object comboStyle = graphGlyphs.get(0).getGraphState().getComboStyle();
		if(comboStyle == null)
			return false;
		
		for(int i=1; i<graphGlyphs.size(); i++){
			if(graphGlyphs.get(i).getGraphState().getComboStyle() != comboStyle){
				return false;
			}
		}
		
		return true;
	}
	
	private boolean isAnyFloat() {
		for (ViewModeGlyph ag : allGlyphs) {
			if (ag.getAnnotStyle().getFloatTier()) {
				return true;
			}
		}
		return false;
	}

	private boolean isAllSupportTwoTrack() {
		for (ViewModeGlyph ag : annotGlyphs) {
			if (!MapViewModeHolder.getInstance().styleSupportsTwoTrack(ag.getAnnotStyle())) {
				return false;
			}
		}
		return true;
	}

	private boolean isAllGraphStyleLocked() {
		for(GraphGlyph graphGlyph : graphGlyphs){
			if(!graphGlyph.isGraphStyleLocked()){
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected void floatCheckBoxReset() {
		JCheckBox floatCheckBox = getFloatCheckBox();

		boolean allFloat = isAllGraph();
		for (GraphGlyph glyph : graphGlyphs) {
			if (!glyph.getAnnotStyle().getFloatTier()) {
				allFloat = false;
				break;
			}
		}

		floatCheckBox.setEnabled(isAllGraph() && !isAnyJoined() 
			// Floating is not allowed if only one track loaded
			&& ((igbService != null && igbService.getVisibleTierGlyphs() != null && igbService.getVisibleTierGlyphs().size() > 2) || allFloat)
		);
		floatCheckBox.setSelected(allFloat);
	}

	@Override
	protected void labelCheckBoxReset() {
		JCheckBox labelCheckBox = getLabelCheckBox();
		labelCheckBox.setEnabled(isAllGraph());
		boolean allLabel = isAllGraph();
		for (GraphGlyph glyph : graphGlyphs) {
			if (!glyph.getShowLabel()) {
				allLabel = false;
				break;
			}
		}
		labelCheckBox.setSelected(allLabel);
	}

	@Override
	protected void YAxisCheckBoxReset() {
		JCheckBox yAxisCheckBox = getYAxisCheckBox();
		yAxisCheckBox.setEnabled(isAllGraph());
		boolean allYAxis = isAllGraph();
		for (GraphGlyph gg : graphGlyphs) {
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
			for (GraphGlyph gg : graphGlyphs) {
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
				heatMapComboBox.setEnabled(true);
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
		Integer labelSize = -1;
		boolean labelSizeSet = false;
		for (ViewModeGlyph vg : allGlyphs) {
			if (labelSize == -1 && !labelSizeSet) {
				labelSize = (int)vg.getAnnotStyle().getTrackNameSize();
				labelSizeSet = true;
			}
			else if (labelSize != (int)vg.getAnnotStyle().getTrackNameSize()) {
				labelSize = -1;
			}
		}
		boolean enable = allGlyphs.size() > 0 && !isAnyFloat();
		labelSizeComboBox.setEnabled(enable);
		getLabelSizeLabel().setEnabled(enable);
		if (!enable || labelSize == -1) {
			labelSizeComboBox.setSelectedIndex(-1);
		}
		else {
			labelSizeComboBox.setSelectedItem(labelSize);
		}
	}

	@Override
	protected void colorSchemeComboBoxReset() {
		ColorSchemeComboBox colorSchemeComboBox = getColorSchemeComboBox();
		colorSchemeComboBox.setEnabled(allGlyphs.size() > 0);
		getColorSchemeLabel().setEnabled(allGlyphs.size() > 0);
	}

	private static SeqSymmetry getMostOriginalSymmetry(SeqSymmetry sym) {
		if (sym instanceof DerivedSeqSymmetry) {
			return getMostOriginalSymmetry(((DerivedSeqSymmetry) sym).getOriginalSymmetry());
		}
		return sym;
	}

	private Set<String> getFields(ViewModeGlyph glyph) {
		Set<String> fields = new TreeSet<String>();
		if (glyph.getInfo() != null && glyph.getInfo() instanceof SeqSymmetry) {
			SeqSymmetry sym = (SeqSymmetry)glyph.getInfo();
			if (sym.getChildCount() > 0) {
				SeqSymmetry child = sym.getChild(0);
				SeqSymmetry original = getMostOriginalSymmetry(child);
				if (original instanceof SymWithProps) {
					Map<String, Object> props = ((SymWithProps) original).getProperties();
					fields.add(TrackConstants.NO_LABEL);
					if(props != null){
						fields.addAll(props.keySet());
					}
				}
			}
		}
		return fields;
	}

	@Override
	protected void labelFieldComboBoxReset() {
		JComboBox labelFieldComboBox = getLabelFieldComboBox();
		labelFieldComboBox.setEnabled(isAllAnnot());
		getLabelFieldLabel().setEnabled(isAllAnnot());
		String labelField = null;
		boolean labelFieldSet = false;
		Set<String> allFields = null;
		for (ViewModeGlyph glyph : annotGlyphs) {
			if (glyph.getAnnotStyle() != null && glyph.getAnnotStyle().getLabelField() != null) {
				String field = glyph.getAnnotStyle().getLabelField();
				if (!labelFieldSet) {
					labelField = field;
					labelFieldSet = true;
				}
				else if (labelField != null && !field.equals(labelField)) {
					labelField = null;
				}
			}
			Set<String> fields = getFields(glyph);
			if (glyph.getInfo() instanceof SeqSymmetry) {
				if (allFields == null) {
					allFields = new TreeSet<String>(fields);
				}
				else {
					allFields.retainAll(fields);
				}
			}
		}
		if (allFields == null) {
			allFields = new TreeSet<String>();
		}
		labelFieldComboBox.setModel(new DefaultComboBoxModel(allFields.toArray()));
		if (labelField != null) {
			labelFieldComboBox.setSelectedItem(labelField);
		}
	}

	@Override
	protected void strands2TracksCheckBoxReset() {
		JCheckBox strands2TracksCheckBox = getStrands2TracksCheckBox();
		strands2TracksCheckBox.setEnabled(isAllAnnot() && isAllSupportTwoTrack());
		boolean all2Tracks = isAllAnnot();
		for (ViewModeGlyph glyph : annotGlyphs) {
			if (!glyph.getAnnotStyle().getSeparate()) {
				all2Tracks = false;
				break;
			}
		}
		strands2TracksCheckBox.setSelected(all2Tracks);
	}

	@Override
	protected void strandsArrowCheckBoxReset() {
		JCheckBox strandsArrowCheckBox = getStrandsArrowCheckBox();
		strandsArrowCheckBox.setEnabled(isAllAnnot() && isAllSupportTwoTrack());
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
			if (!(glyph.getAnnotStyle().getDirectionType() == TrackConstants.DIRECTION_TYPE.COLOR.ordinal() || glyph.getAnnotStyle().getDirectionType() == TrackConstants.DIRECTION_TYPE.BOTH.ordinal())) {
				allColor = false;
				break;
			}
		}
		return allColor;
	}

	@Override
	protected void strandsColorCheckBoxReset() {
		JCheckBox strandsColorCheckBox = getStrandsColorCheckBox();
		strandsColorCheckBox.setEnabled(isAllAnnot() && isAllSupportTwoTrack());
		strandsColorCheckBox.setSelected(isAllAnnot() && isAllStrandsColor());
	}

	@Override
	protected void strandsForwardColorComboBoxReset() {
		ColorComboBox strandsForwardColorComboBox = getStrandsForwardColorComboBox();
		strandsForwardColorComboBox.setEnabled(isAllAnnot() && isAllStrandsColor() && isAllSupportTwoTrack());
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
		strandsForwardColorComboBox.setSelectedColor(strandsForwardColor);
	}

	@Override
	protected void strandsReverseColorComboBoxReset() {
		ColorComboBox strandsReverseColorComboBox = getStrandsReverseColorComboBox();
		strandsReverseColorComboBox.setEnabled(isAllAnnot() && isAllStrandsColor() && isAllSupportTwoTrack());
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
		strandsReverseColorComboBox.setSelectedColor(strandsReverseColor);
	}

	protected void strandsLabelReset() {
		JLabel strandsLabel = getStrandsLabel();
		strandsLabel.setEnabled(isAllAnnot() && isAllSupportTwoTrack());
	}

	@Override
	protected void buttonGroup1Reset() {
		getGraphStyleLineRadioButton().setEnabled(isAllGraph());
		getGraphStyleBarRadioButton().setEnabled(isAllGraph());
		getGraphStyleStairStepRadioButton().setEnabled(isAllGraph());
		getGraphStyleDotRadioButton().setEnabled(isAllGraph());
		getGraphStyleMinMaxAvgRadioButton().setEnabled(isAllGraph());
		getGraphStyleHeatMapRadioButton().setEnabled(isAllGraph());
		if (isAllGraph()) {
			GraphType graphType = null;
			boolean graphTypeSet = false;
			for (GraphGlyph vg : graphGlyphs) {
				if (graphType == null && !graphTypeSet) {
					graphType = vg.getGraphStyle();
					graphTypeSet = true;
				}
				else if (graphType != vg.getGraphStyle()) {
					graphType = null;
					break;
				}
			}
			if (graphType == null) {
				unselectGraphStyle();
				if (isAllGraphStyleLocked()) {
					getGraphStyleLineRadioButton().setEnabled(false);
					getGraphStyleBarRadioButton().setEnabled(false);
					getGraphStyleStairStepRadioButton().setEnabled(false);
					getGraphStyleDotRadioButton().setEnabled(false);
					getGraphStyleMinMaxAvgRadioButton().setEnabled(false);
					getGraphStyleHeatMapRadioButton().setEnabled(false);
				}
			}
			else {
				if (graphType == GraphType.HEAT_MAP) {
					getGraphStyleHeatMapRadioButton().setText("");
					getGraphStyleHeatMapComboBox().setVisible(true);
				}
				else {
					getGraphStyleHeatMapRadioButton().setText("Heat Map");
					getGraphStyleHeatMapComboBox().setVisible(false);
				}
				switch (graphType) {
				case LINE_GRAPH:
					getGraphStyleLineRadioButton().setSelected(true);
					break;
				case BAR_GRAPH:
					getGraphStyleBarRadioButton().setSelected(true);
					break;
				case STAIRSTEP_GRAPH:
					getGraphStyleStairStepRadioButton().setSelected(true);
					break;
				case DOT_GRAPH:
					getGraphStyleDotRadioButton().setSelected(true);
					break;
				case MINMAXAVG:
					getGraphStyleMinMaxAvgRadioButton().setSelected(true);
					break;
				case HEAT_MAP:
					getGraphStyleHeatMapRadioButton().setSelected(true);
					break;
				}
			}
		}
		else {
			unselectGraphStyle();
			getGraphStyleHeatMapRadioButton().setText("Heat Map");
			getGraphStyleHeatMapComboBox().setVisible(false);
		}
	}

	@Override
	protected void labelColorComboBoxReset() {
		ColorComboBox labelColorComboBox = getLabelColorComboBox();
		Color labelColor = null;
		boolean labelColorSet = false;
		for (ViewModeGlyph ag : allGlyphs) {
			if (labelColor == null && !labelColorSet) {
				labelColor = ag.getAnnotStyle().getLabelForeground();
				if(ag instanceof AbstractGraphGlyph && ((AbstractGraphGlyph)ag).getGraphState().getComboStyle() != null){
					labelColor = ((AbstractGraphGlyph)ag).getGraphState().getComboStyle().getLabelForeground();
				}
				labelColorSet = true;
			}
			else if (labelColor != ag.getAnnotStyle().getLabelForeground()) {
				if(ag instanceof AbstractGraphGlyph && ((AbstractGraphGlyph)ag).getGraphState().getComboStyle() != null){
					if(labelColor != ((AbstractGraphGlyph)ag).getGraphState().getComboStyle().getLabelForeground()){
						labelColor = null;
					}
				}else{
					labelColor = null;
				}
				break;
			}
		}
		boolean enable = allGlyphs.size() > 0 && !isAnyFloat();
		labelColorComboBox.setEnabled(enable);
		getLabelColorLabel().setEnabled(enable);
		labelColorComboBox.setSelectedColor(enable ? labelColor : null);
	}

	@Override
	protected void backgroundColorComboBoxReset() {
		ColorComboBox backgroundColorComboBox = getBackgroundColorComboBox();
		boolean enable = allGlyphs.size() > 0 && !isAnyFloat();
		Color backgroundColor = null;
		if (enable) {
			backgroundColor = allGlyphs.get(0).getAnnotStyle().getBackground();
			for (ViewModeGlyph ag : allGlyphs) {
				if (backgroundColor != ag.getAnnotStyle().getBackground()) {
					backgroundColor = null;
					break;
				}
			}
		}
		backgroundColorComboBox.setEnabled(enable);
		getBackgroundColorLabel().setEnabled(enable);
		backgroundColorComboBox.setSelectedColor(enable ? backgroundColor : null);
	}

	@Override
	protected void foregroundColorComboBoxReset() {
		ColorComboBox foregroundColorComboBox = getForegroundColorComboBox();
		boolean enable = allGlyphs.size() > 0;
		foregroundColorComboBox.setEnabled(enable);
		getForegroundColorLabel().setEnabled(enable);
		Color foregroundColor = null;
		if (enable) {
			foregroundColor = allGlyphs.get(0).getAnnotStyle().getForeground();
			for (ViewModeGlyph ag : allGlyphs) {
				if (!(foregroundColor.equals(ag.getAnnotStyle().getForeground()))) {
					foregroundColor = null;
					break;
				}
			}
		}
		foregroundColorComboBox.setSelectedColor(foregroundColor);
	}

	@Override
	protected void stackDepthTextFieldReset() {
		JTextField stackDepthTextField = getStackDepthTextField();
		boolean enabled = annotGlyphs.size() > 0 && isAllAnnot();
		stackDepthTextField.setEnabled(enabled);
		getStackDepthLabel().setEnabled(enabled);
		stackDepthTextField.setText("");
		if (enabled) {
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
			if (stackDepth != -1) {
				stackDepthTextField.setText("" + stackDepth);
			}
		}
	}

	@Override
	protected void trackNameTextFieldReset() {
		JTextField trackNameTextField = getTrackNameTextField();
		if (allGlyphs.size() == 1) {
			trackNameTextField.setEnabled(true);
			trackNameTextField.setText(allGlyphs.get(0).getLabel());
		} else if (isAllGraph()){ // Special case for joined graph
			if(isOneJoined()){
				trackNameTextField.setEnabled(true);
				trackNameTextField.setText(graphGlyphs.get(0).getGraphState().getComboStyle().getTrackName());
			}else{
				trackNameTextField.setEnabled(false);
				trackNameTextField.setText("");
			}
		}else{
			trackNameTextField.setEnabled(false);
			trackNameTextField.setText("");
		}
	}

	@Override
	protected void stackDepthGoButtonReset() {
		JButton stackDepthGoButton = getStackDepthGoButton();
		stackDepthGoButton.setEnabled(annotGlyphs.size() > 0 && isAllAnnot());
	}

	@Override
	protected void stackDepthAllButtonReset() {
		JButton stackDepthAllButton = getStackDepthAllButton();
		stackDepthAllButton.setEnabled(annotGlyphs.size() > 0 && isAllAnnot());
	}

	@Override
	protected void selectAllButtonReset() {
		JButton selectAllButton = getSelectAllButton();
		selectAllButton.setEnabled(igbService != null && igbService.getVisibleTierGlyphs() != null && igbService.getVisibleTierGlyphs().size() > 1);
	}

	@Override
	protected void hideButtonReset() {
		JButton hideButton = getHideButton();
		boolean enable = allGlyphs.size() > 0 && !isAnyFloat();
		hideButton.setEnabled(enable);
	}

	@Override
	protected void clearButtonReset() {
		JButton clearButton = getClearButton();
		boolean enable = allGlyphs.size() > 0 && !isAnyFloat();
		clearButton.setEnabled(enable);
	}

	@Override
	protected void restoreToDefaultButtonReset() {
		JButton restoreToDefaultButton = getRestoreToDefaultButton();
		boolean enable = allGlyphs.size() > 0 && !isAnyFloat();
		restoreToDefaultButton.setEnabled(enable);
	}
}
