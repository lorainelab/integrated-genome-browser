/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.widget;

import com.affymetrix.genoviz.bioviews.*;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Enumeration;

/**
 * Customizer for a NeoWidget for use with NeoWidget as a Java bean.
 */
public class NeoWidgetCustomizer
	extends NeoWidgetICustomizer
{

	protected NeoWidget widget;

	public NeoWidgetCustomizer() {

		super.includeFuzzinessEditor();

		// Scrolling
		Panel scrollingPanel = new Panel();
		scrollingPanel.setLayout(valuePanelLayout);
		Label scrollingLabel = new Label("Scrolling:", Label.RIGHT);
		add(scrollingLabel);
		layout.setConstraints(scrollingLabel, labelConstraints);
		layout.setConstraints(scrollingPanel, valueConstraints);
		scrollingPanel.add(scrollingIncrBehavior);
		add(scrollingPanel);
		valueConstraints.gridy++;

		// Rubber Band
		Panel bandingPanel = new Panel();
		bandingPanel.setLayout(valuePanelLayout);
		Label bandingLabel = new Label("Rubber Band:", Label.RIGHT);
		add(bandingLabel);
		layout.setConstraints(bandingLabel, labelConstraints);
		layout.setConstraints(bandingPanel, valueConstraints);
		bandingPanel.add(bandingBehavior);
		add(bandingPanel);
		valueConstraints.gridy++;

		// Zoom Behavior
		Panel zoomingPanel = new Panel();
		zoomingPanel.setLayout(valuePanelLayout);
		Label zoomingLabel = new Label("Zoom from:", Label.RIGHT);
		add(zoomingLabel);
		layout.setConstraints(zoomingLabel, labelConstraints);
		layout.setConstraints(zoomingPanel, valueConstraints);
		zoomingPanel.add(zoomingXChoice);
		zoomingPanel.add(zoomingYChoice);
		add(zoomingPanel);
		valueConstraints.gridy++;

		includeReshapeBehavior();

		// Pointer Precision (pixel fuzziness)

		// Selection
		includeSelection();

	}

	// Reshape Behavior
	public void includeReshapeBehavior() {
		Panel reshapingPanel = new Panel();
		reshapingBehaviorX = new Checkbox("Fit X");
		reshapingBehaviorX.addItemListener(this);
		reshapingBehaviorY = new Checkbox("Fit Y");
		reshapingBehaviorY.addItemListener(this);
		reshapingPanel.setLayout(valuePanelLayout);
		Label reshapingLabel = new Label("Reshape:", Label.RIGHT);
		add(reshapingLabel);
		layout.setConstraints(reshapingLabel, labelConstraints);
		layout.setConstraints(reshapingPanel, valueConstraints);
		reshapingPanel.add(reshapingBehaviorX);
		reshapingPanel.add(reshapingBehaviorY);
		add(reshapingPanel);
		valueConstraints.gridy++;
	}

	public void itemStateChanged(ItemEvent theEvent) {
		Object evtSource = theEvent.getSource();
		if (null == widget) {
		}
		else if (evtSource == this.scrollingIncrBehavior) {
			if(this.scrollingIncrBehavior.getState()) {
				widget.setScrollIncrementBehavior(NeoWidget.X,
						NeoWidget.AUTO_SCROLL_INCREMENT);
			}
			else {
				widget.setScrollIncrementBehavior(NeoWidget.X,
						NeoWidget.NO_AUTO_SCROLL_INCREMENT);
			}
			return;
		}
		else if (evtSource == this.selectionChoice
				||  evtSource == this.selectionColorChoice) {
			setSelectionAppearance();
			return;
				}
		else if (evtSource == this.bandingBehavior) {
			widget.setRubberBandBehavior(this.bandingBehavior.getState());
			widget.updateWidget();
			return;
		}
		else if (evtSource == this.zoomingXChoice) {
			setScaleConstraint( NeoWidgetI.X, this.zoomingXChoice.getSelectedItem());
			return;
		}
		else if (evtSource == this.zoomingYChoice) {
			setScaleConstraint( NeoWidgetI.Y, this.zoomingYChoice.getSelectedItem());
			return;
		}
		else if (evtSource == this.reshapingBehaviorX) {
			if (((Checkbox)evtSource).getState()) {
				widget.setReshapeBehavior(NeoWidgetI.X, NeoWidgetI.FITWIDGET);
				widget.updateWidget();
				widget.setSize(widget.getSize());
			}
			else {
				widget.setReshapeBehavior(NeoWidgetI.X, NeoWidgetI.NONE);
			}
			return;
		}
		else if (evtSource == this.reshapingBehaviorY) {
			if (((Checkbox)evtSource).getState()) {
				widget.setReshapeBehavior(NeoWidgetI.Y, NeoWidgetI.FITWIDGET);
				widget.updateWidget();
				widget.setSize(widget.getSize());
			}
			else {
				widget.setReshapeBehavior(NeoWidgetI.Y, NeoWidgetI.NONE);
			}
			return;
		}
		super.itemStateChanged(theEvent);
	}

	private void setSelectionAppearance() {
		int behavior = SceneI.SELECT_FILL;
		String s = this.selectionChoice.getSelectedItem();
		if (s.equals("Outlined")) {
			behavior = Scene.SELECT_OUTLINE;
		}
		else if (s.equals("Filled")) {
			behavior = Scene.SELECT_FILL;
		}
		else if (s.equals("None")) {
			behavior = Scene.SELECT_NONE;
		}
		else if( s.equals("Reversed") ) {
			behavior = Scene.SELECT_REVERSE;
		}

		widget.setSelectionAppearance(behavior);
		Color color = widget.getColor(this.selectionColorChoice.getSelectedItem());
		widget.setSelectionColor(color);
		widget.updateWidget();
	}

	private void setScaleConstraint(int theAxis, String theChoice) {
		int constraint = 0;
		if (theChoice.equalsIgnoreCase("Top")
				|| theChoice.equalsIgnoreCase("Left")) {
			widget.setZoomBehavior(theAxis, NeoWidgetI.CONSTRAIN_START);
				}
		else if (theChoice.equalsIgnoreCase("Center")
				|| theChoice.equalsIgnoreCase("Middle")) {
			widget.setZoomBehavior(theAxis, NeoWidgetI.CONSTRAIN_MIDDLE);
				}
		else if (theChoice.equalsIgnoreCase("Bottom")
				|| theChoice.equalsIgnoreCase("Right")) {
			widget.setZoomBehavior(theAxis, NeoWidgetI.CONSTRAIN_END);
				}
	}

	// PropertyChangeListener Methods

	public void setObject(Object bean) {
		NeoWidget widget;
		if (bean instanceof NeoWidget) {
			widget = (NeoWidget)bean;
			super.setObject(widget);
		}
		else {
			throw new IllegalArgumentException("need a NeoWidget");
		}

		// Scrolling
		int id = widget.getScrollIncrementBehavior(NeoWidgetI.X);
		scrollingIncrBehavior.setState(NeoWidget.AUTO_SCROLL_INCREMENT == id);

		// Selection is set in super.setObject().

		// Rubberband
		this.bandingBehavior.setState(widget.getRubberBandBehavior());

		// Zoom
		int zoomBehavior = widget.getZoomBehavior(NeoWidgetI.X);
		switch (zoomBehavior) {
			case NeoWidgetI.CONSTRAIN_START:
				zoomingXChoice.addItem("Left");
				zoomingXChoice.addItem("Center");
				zoomingXChoice.addItem("Right");
				break;
			case NeoWidgetI.CONSTRAIN_MIDDLE:
				zoomingXChoice.addItem("Center");
				zoomingXChoice.addItem("Left");
				zoomingXChoice.addItem("Right");
				break;
			case NeoWidgetI.CONSTRAIN_END:
				zoomingXChoice.addItem("Right");
				zoomingXChoice.addItem("Center");
				zoomingXChoice.addItem("Left");
				break;
			default:
				zoomingXChoice.addItem("Center");
				zoomingXChoice.addItem("Left");
				zoomingXChoice.addItem("Right");
				break;
		}
		zoomBehavior = widget.getZoomBehavior(NeoWidgetI.Y);
		switch (zoomBehavior) {
			case NeoWidgetI.CONSTRAIN_START:
				zoomingYChoice.addItem("Top");
				zoomingYChoice.addItem("Middle");
				zoomingYChoice.addItem("Bottom");
				break;
			case NeoWidgetI.CONSTRAIN_MIDDLE:
				zoomingYChoice.addItem("Middle");
				zoomingYChoice.addItem("Top");
				zoomingYChoice.addItem("Bottom");
				break;
			case NeoWidgetI.CONSTRAIN_END:
				zoomingYChoice.addItem("Bottom");
				zoomingYChoice.addItem("Middle");
				zoomingYChoice.addItem("Top");
				break;
			default:
				zoomingYChoice.addItem("Middle");
				zoomingYChoice.addItem("Top");
				zoomingYChoice.addItem("Bottom");
				break;
		}

		this.widget = widget;
	}


}
