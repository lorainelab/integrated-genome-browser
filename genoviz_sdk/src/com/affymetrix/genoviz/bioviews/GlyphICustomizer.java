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

package com.affymetrix.genoviz.bioviews;

import com.affymetrix.genoviz.widget.NeoWidgetI;

// Here we import some classes to make this a bean.
import java.beans.Customizer;
import java.beans.PropertyChangeListener;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Generic customizer for a GlyphI -- still experimental.
 */
public class GlyphICustomizer extends Panel implements Customizer  {
  // a debugging field
  protected boolean even = false;

  protected GlyphI glyph;
  protected Vector glyph_vec;
  protected NeoWidgetI widget;
  protected GridBagLayout layout;
  protected GridBagConstraints labelConstraints = new GridBagConstraints();
  protected GridBagConstraints valueConstraints = new GridBagConstraints();
  protected FlowLayout valuePanelLayout = new FlowLayout(FlowLayout.LEFT);
  protected Panel widthPanel;
  protected Label widthLabel;

  Choice backgroundColorChoice;
  Choice foregroundColorChoice;
  Choice fuzzinessChoice;
  Choice widthChoice;
  Checkbox scrollingIncrBehavior = new Checkbox("auto-increment");
  Choice selectionChoice;
  Choice selectionColorChoice;
  Checkbox bandingBehavior = new Checkbox("active");
  Choice zoomingXChoice = new Choice();
  Choice zoomingYChoice = new Choice();
  Checkbox reshapingBehaviorX;
  Checkbox reshapingBehaviorY;

  TextField glyphBegT, glyphEndT;

  public GlyphICustomizer() {

    this.layout = new GridBagLayout();
    this.setLayout(this.layout);

    labelConstraints.fill = GridBagConstraints.HORIZONTAL;
    labelConstraints.anchor = GridBagConstraints.EAST;
    labelConstraints.gridx = 0;

    valueConstraints.gridy = 0;
    valueConstraints.anchor = GridBagConstraints.WEST;
    valueConstraints.gridwidth = GridBagConstraints.REMAINDER;

    includeForegroundColorEditor();
    includeBackgroundColorEditor();
    includeWidthEditor();
    includeSpanEditor();
  }


  public void includeForegroundColorEditor() {
    Panel foregroundColorPanel = new Panel();
    foregroundColorChoice = new Choice();
    foregroundColorPanel.setLayout(valuePanelLayout);
    Label foregroundColorLabel = new Label("Glyph Foreground Color:", Label.RIGHT);
    add(foregroundColorLabel);
    layout.setConstraints(foregroundColorLabel, labelConstraints);
    layout.setConstraints(foregroundColorPanel, valueConstraints);
    foregroundColorPanel.add(foregroundColorChoice);
    add(foregroundColorPanel);
    valueConstraints.gridy++;
  }

  public void includeBackgroundColorEditor() {
    Panel backgroundColorPanel = new Panel();
    backgroundColorChoice = new Choice();
    backgroundColorPanel.setLayout(valuePanelLayout);
    Label backgroundColorLabel = new Label("Glyph Background Color:", Label.RIGHT);
    add(backgroundColorLabel);
    layout.setConstraints(backgroundColorLabel, labelConstraints);
    layout.setConstraints(backgroundColorPanel, valueConstraints);
    backgroundColorPanel.add(backgroundColorChoice);
    add(backgroundColorPanel);
    valueConstraints.gridy++;
  }

  public void includeWidthEditor() {
    widthPanel = new Panel();
    widthChoice = new Choice();
    widthLabel = new Label("Glyph Width:", Label.RIGHT);
    add(widthLabel);
    layout.setConstraints(widthLabel, labelConstraints);
    layout.setConstraints(widthPanel, valueConstraints);
    widthPanel.add(widthChoice);
    add(widthPanel);
    valueConstraints.gridy++;
  }

  public void includeSpanEditor() {
    Panel spanPanel = new Panel();
    glyphBegT = new TextField();
    glyphEndT = new TextField();
    spanPanel.setLayout(valuePanelLayout);
    Label spanLabel = new Label("Glyph Span:", Label.RIGHT);
    add(spanLabel);
    layout.setConstraints(spanLabel, labelConstraints);
    layout.setConstraints(spanPanel, valueConstraints);
    spanPanel.setLayout(new GridLayout(0, 4));
    spanPanel.add(new Label("Beg: ", Label.RIGHT));
    spanPanel.add(glyphBegT);
    spanPanel.add(new Label("End: ", Label.RIGHT));
    spanPanel.add(glyphEndT);
    add(spanPanel);
    valueConstraints.gridy++;
  }

  // Bounds
  public void includeBounds() {
    Panel boundsPanel = new Panel();
    boundsPanel.setLayout(valuePanelLayout);
    Label boundsLabel = new Label("Bounds:", Label.RIGHT);
    add(boundsLabel);
    layout.setConstraints(boundsLabel, labelConstraints);
    layout.setConstraints(boundsPanel, valueConstraints);
    boundsPanel.setLayout(new GridLayout(0, 3));
    boundsPanel.add(new Label("X: ", Label.RIGHT));
    boundsPanel.add(new Scrollbar(Scrollbar.HORIZONTAL));
    boundsPanel.add(new Scrollbar(Scrollbar.HORIZONTAL));
    boundsPanel.add(new Label("Y: ", Label.RIGHT));
    boundsPanel.add(new Scrollbar(Scrollbar.HORIZONTAL));
    boundsPanel.add(new Scrollbar(Scrollbar.HORIZONTAL));
    add(boundsPanel);
    valueConstraints.gridy++;
  }

  public void includeFuzzinessEditor() {
    Panel fuzzinessPanel = new Panel();
    fuzzinessChoice = new Choice();
    fuzzinessPanel.setLayout(valuePanelLayout);
    Label fuzzinessLabel = new Label("pointer precision:", Label.RIGHT);
    add(fuzzinessLabel);
    layout.setConstraints(fuzzinessLabel, labelConstraints);
    layout.setConstraints(fuzzinessPanel, valueConstraints);
    fuzzinessPanel.add(fuzzinessChoice);
    add(fuzzinessPanel);
    valueConstraints.gridy++;
  }

  public boolean action(Event theEvent, Object theObject) {
    if (glyph == null && glyph_vec == null) {
      // if no glyph or glyph_vec, do nothing
    }
    else if (theEvent.target == this.backgroundColorChoice) {
      Color c = widget.getColor(this.backgroundColorChoice.getSelectedItem());
      if (glyph_vec != null) {
        GlyphI gl;
        for (int i=0; i<glyph_vec.size(); i++) {
          gl = (GlyphI)glyph_vec.elementAt(i);
          gl.setBackgroundColor(c);
        }
      }
      else if (glyph != null) {
        glyph.setBackgroundColor(c);
      }
      widget.updateWidget();
    }
    else if (theEvent.target == this.foregroundColorChoice) {
      Color c = widget.getColor(this.foregroundColorChoice.getSelectedItem());
      if (glyph_vec != null) {
        GlyphI gl;
        for (int i=0; i<glyph_vec.size(); i++) {
          gl = (GlyphI)glyph_vec.elementAt(i);
          gl.setForegroundColor(c);
        }
      }
      else if (glyph != null) {
        glyph.setForegroundColor(c);
      }
      widget.updateWidget();
    }

    else if (theEvent.target == this.widthChoice) {
      int width = Integer.parseInt(widthChoice.getSelectedItem());
      Rectangle2D cbox;
      if (glyph_vec != null) {
        GlyphI gl;
        for (int i=0; i<glyph_vec.size(); i++) {
          gl = (GlyphI)glyph_vec.elementAt(i);
          cbox = gl.getCoordBox();
          gl.setCoords(cbox.x, cbox.y, cbox.width, width);
        }
      }
      else if (glyph != null) {
        cbox = glyph.getCoordBox();
        glyph.setCoords(cbox.x, cbox.y, cbox.width, width);
      }
      widget.updateWidget();
    }

    else if (theEvent.target == glyphBegT) {
      if (glyph != null) {
        String str = glyphBegT.getText();
        Rectangle2D cbox = glyph.getCoordBox();
        double end = cbox.x + cbox.width;
        try {
          double beg = Float.valueOf(str).doubleValue();
          glyph.setCoords(beg, cbox.y, end-beg, cbox.height);
          widget.updateWidget();
        }
        catch (Exception ex) {
          System.out.println(ex.getMessage());
          ex.printStackTrace();
        }
      }
    }

    else if (theEvent.target == glyphEndT) {
      if (glyph != null) {
        String str = glyphEndT.getText();
        Rectangle2D cbox = glyph.getCoordBox();
        double beg = cbox.x;
        try {
          double end = Float.valueOf(str).doubleValue();
          // extra +1 for sequence-based maps, where
          // start=0, end=1 ==>  x=0, width=2
          glyph.setCoords(beg, cbox.y, end-beg+1, cbox.height);
          widget.updateWidget();
        }
        catch (Exception ex) {
          System.out.println(ex.getMessage());
          ex.printStackTrace();
        }
      }
    }
    return super.action(theEvent, theObject);
  }

  private void setSelectionAppearance() {
    int behavior = SceneI.SELECT_FILL;
    String s = this.selectionChoice.getSelectedItem();
    if (s.equals("Outlined")) {
      behavior = SceneI.SELECT_OUTLINE;
    }
    else if (s.equals("Colored")) {
      behavior = SceneI.SELECT_FILL;
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

  public void addPropertyChangeListener(PropertyChangeListener listener){}
  public void removePropertyChangeListener(PropertyChangeListener listener){}

  public void setGlyph(GlyphI glyph) {
    this.glyph = glyph;
    if (glyph == null) {
      if (glyphBegT != null) { glyphBegT.setText(""); }
      if (glyphEndT != null) { glyphEndT.setText(""); }

      return;
    }

    Rectangle2D cbox = glyph.getCoordBox();

    if (glyphBegT != null) {
      glyphBegT.setText(Integer.toString((int)cbox.x));
    }
    if (glyphEndT != null) {
      // extra -1 to adjust for sequence-based maps, where
      // start=0, end=1 ==>  x=0, width=2
      glyphEndT.setText(Integer.toString((int)(cbox.x + cbox.width - 1)));
    }

    if (backgroundColorChoice != null && widget != null) {
      String color_name = widget.getColorName( glyph.getBackgroundColor() );
      backgroundColorChoice.select( color_name );
    }

    if( null != foregroundColorChoice && widget != null ) {
      String color_name = widget.getColorName( glyph.getForegroundColor() );
      foregroundColorChoice.select( color_name );
    }

  }

  public void setGlyphVector(Vector vec) {
    this.glyph_vec = vec;
    if (vec == null || vec.size() <= 0) {
      setGlyph(null);
    }
    else {
      setGlyph((GlyphI)vec.elementAt(0));
    }
  }

  public void setWidget(NeoWidgetI widget) {
    this.widget = widget;
    this.setObject(widget);
  }

  public void setObject(Object bean) {
    if (bean instanceof NeoWidgetI) {
      this.widget = (NeoWidgetI)bean;
    }
    else {
      throw new IllegalArgumentException("need a NeoWidgetI");
    }

    // Background
    if (null != backgroundColorChoice) {
      loadColorChoice( backgroundColorChoice, widget.getBackground() );
    }

    // Foreground
    if (null != foregroundColorChoice) {
       loadColorChoice(foregroundColorChoice, widget.getForeground());
    }

    if (widthChoice != null) {
      int choices[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20 };
      loadIntegerChoice(widthChoice, choices, 5);
    }


    // Selection
    if (null != selectionColorChoice) {
      int selectionBehavior = widget.getSelectionAppearance();
      switch (selectionBehavior) {
      case SceneI.SELECT_FILL:
        selectionChoice.addItem("Colored");
        selectionChoice.addItem("Outlined");
        break;
      case SceneI.SELECT_OUTLINE:
        selectionChoice.addItem("Outlined");
        selectionChoice.addItem("Colored");
        break;
      default:
        selectionChoice.addItem("Colored");
        selectionChoice.addItem("Outlined");
        break;
      }
      loadColorChoice(selectionColorChoice, widget.getSelectionColor());
    }
  }


  protected void loadColorChoice(Choice theChoice, Color theDefaultColor) {
    if (null == theDefaultColor) {
      loadColorChoice(theChoice, (String)null);
    }
    else {
      loadColorChoice(theChoice, widget.getColorName(theDefaultColor));
    }
  }

  protected void loadColorChoice(Choice theChoice, String theDefaultColor) {
    loadChoice(theChoice, widget.getColorNames());
    if (null != theDefaultColor) {
      theChoice.select(theDefaultColor);
    }
  }

  protected void loadChoice(Choice theChoice, Enumeration theChoices) {
    while (theChoices.hasMoreElements()) {
      theChoice.addItem(""+theChoices.nextElement());
    }
  }

  protected void loadChoice(Choice theChoice, Object[] theChoices, Object theDefault) {
    for (int i = 0; i < theChoices.length; i++) {
      theChoice.addItem(""+theChoices[i]);
    }
    if (null != theDefault) {
      theChoice.select(""+theDefault);
    }
  }

  protected void loadIntegerChoice(Choice theChoice, int[] theChoices) {
    for (int i = 0; i < theChoices.length; i++) {
      theChoice.addItem(""+theChoices[i]);
    }
  }

  protected void loadIntegerChoice(Choice theChoice, int[] theChoices, int theDefault) {
    int i;
    for (i = 0; i < theChoices.length && theChoices[i] <= theDefault; i++) {
      theChoice.addItem(""+theChoices[i]);
    }
    if (i < theChoices.length && theChoices[i] < theDefault) {
      theChoice.addItem(""+theDefault);
    }
    for ( ; i < theChoices.length; i++) {
      theChoice.addItem(""+theChoices[i]);
    }
    if (theChoices[--i] < theDefault) {
      theChoice.addItem(""+theDefault);
    }
    theChoice.select(""+theDefault);
  }

}
