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

package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.widget.NeoWidgetI;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.bioviews.GlyphICustomizer;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.Toolkit;
import java.util.Enumeration;

/**
 * Customizer for a NeoWidget for use with NeoWidget as a Java bean.
 */
public class LabelGlyphCustomizer extends GlyphICustomizer  {

  protected LabelGlyph label;

  TextField labelText = new TextField(30);
  Checkbox showBackgroundButton = new Checkbox();
  Choice fontNameChoice = new Choice();
  Choice fontSizeChoice = new Choice();
  Choice fontStyleChoice = new Choice();
  Choice fontColorChoice = new Choice();

  Choice spacingChoice = new Choice();

  public LabelGlyphCustomizer() {
    super();

    Panel showbgPanel = new Panel();
    showbgPanel.setLayout(valuePanelLayout);
    Label showbgLabel = new Label("Show Background:", Label.RIGHT);
    this.add(showbgLabel);
    layout.setConstraints(showbgLabel, labelConstraints);
    layout.setConstraints(showbgPanel, valueConstraints);
    showbgPanel.add(showBackgroundButton);
    this.add(showbgPanel);
    valueConstraints.gridy++;

    // Font
    Panel fontPanel = new Panel();
    fontPanel.setLayout(valuePanelLayout);
    Label fontLabel = new Label("Font:", Label.RIGHT);
    add(fontLabel);
    layout.setConstraints(fontLabel, labelConstraints);
    layout.setConstraints(fontPanel, valueConstraints);
    fontPanel.add(fontNameChoice);
    fontPanel.add(fontStyleChoice);
    fontPanel.add(fontSizeChoice);
    add(fontPanel);
    valueConstraints.gridy++;

    Label infoLabel = new Label("Text:", Label.RIGHT);
    add(infoLabel);
    Panel labelTextPanel = new Panel();
    labelTextPanel.setLayout(valuePanelLayout);
    layout.setConstraints(infoLabel, labelConstraints);
    layout.setConstraints(labelTextPanel, valueConstraints);
    labelTextPanel.add(labelText);
    add(labelTextPanel);
    valueConstraints.gridy++;

  }

  public void setWidget(NeoWidgetI widget) {
    super.setWidget(widget);
    loadFonts();
  }

  public void setLabel(LabelGlyph label) {
    this.label = label;
    super.setGlyph(label);
  }

  public void loadFonts() {
    String[] fl = Toolkit.getDefaultToolkit().getFontList();
    String fontName = "Courier";
    loadChoice(fontNameChoice, fl, fontName);
    int fontSize[] = { 8, 10, 12, 14, 18, 24 };
    int defltFontSize = 12;
    loadIntegerChoice(fontSizeChoice, fontSize, defltFontSize);
    Color fontColor = Color.blue;
    loadColorChoice(fontColorChoice, fontColor);
    String[] styles = { "PLAIN", "BOLD", "ITALIC" };
    String fontStyle = "PLAIN";
    loadChoice(fontStyleChoice, styles, fontStyle);
  }

  public boolean action(Event theEvent, Object theObject) {
    if (null == widget) {
    }
    if (theEvent.target == this.fontNameChoice) {
      label.setFontName(((Choice)theEvent.target).getSelectedItem());
      widget.updateWidget();
      return true;
    }
    else if (theEvent.target == this.fontStyleChoice) {
      String style_string = (((Choice)theEvent.target).getSelectedItem());
      int style_int = Font.PLAIN;
      if (style_string.equals("PLAIN")) { style_int = Font.PLAIN; }
      if (style_string.equals("BOLD")) { style_int = Font.BOLD; }
      if (style_string.equals("ITALIC")) { style_int = Font.ITALIC; }
      label.setFontStyle(style_int);
      widget.updateWidget();
      return true;
    }

    else if (theEvent.target == this.fontSizeChoice) {
      String s = ((Choice)theEvent.target).getSelectedItem();
      int size = Integer.parseInt(s);
      label.setFontSize(size);
      widget.updateWidget();
      return true;
    }
    else if (theEvent.target == this.fontColorChoice) {
      String cs = ((Choice)theEvent.target).getSelectedItem();
      Color c = widget.getColor(cs);
      label.setTextColor(c);
      widget.updateWidget();
      return true;
    }
    else if (theEvent.target == labelText) {
      label.setText(labelText.getText());
      widget.updateWidget();
      return true;
    }
    else if ( theEvent.target == showBackgroundButton ) {
      label.setShowBackground( showBackgroundButton.getState() );
      widget.updateWidget();
      return true;
    }

    return super.action(theEvent, theObject);
  }

}
