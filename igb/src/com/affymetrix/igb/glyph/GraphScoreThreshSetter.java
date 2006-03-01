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

package com.affymetrix.igb.glyph;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.text.DecimalFormat;
import java.util.*;

import com.affymetrix.genoviz.widget.*;
import com.affymetrix.igb.view.GraphAdjusterView;

public class GraphScoreThreshSetter extends JPanel
  implements ChangeListener, ActionListener  {

  static Object placeholder_object = new Object();
  static DecimalFormat val_format;
  static DecimalFormat per_format;

  Dimension slider_sizepref = new Dimension(600, 15);
  Dimension textbox_sizepref = new Dimension(400, 15);
  boolean set_slider_sizepref = false;
  boolean set_textbox_sizepref = false;
  boolean thresh_is_min = true;

  java.util.List graphs = new ArrayList();
  Map flipped_hash = new HashMap();

  NeoWidgetI widg;
  GraphVisibleBoundsSetter per_info_provider;

  JSlider score_val_slider;
  JSlider score_percent_slider;
  JTextField score_valT;
  JTextField score_perT;
  JRadioButton thresh_aboveB;
  JRadioButton thresh_belowB;

  float sliders_per_percent = 10.0f;
  float percents_per_slider = 1.0f / sliders_per_percent;

  int total_val_sliders = 1000;
  float sliders_per_val;
  float vals_per_slider;


  float abs_min_val;
  float abs_max_val;
  float prev_score_val;

  float abs_min_per = 0;
  float abs_max_per = 100;
  float prev_score_per;


  int max_chars = 15;
  int max_pix_per_char = 6;
  int tf_min_xpix = max_chars * max_pix_per_char;
  int tf_max_xpix = tf_min_xpix + (2*max_pix_per_char);
  int tf_min_ypix = 10;
  int tf_max_ypix = 25;

  static {
    val_format = new DecimalFormat();
    val_format.setMinimumIntegerDigits(1);
    val_format.setMinimumFractionDigits(0);
    val_format.setMaximumFractionDigits(1);
    val_format.setGroupingUsed(false);
    per_format = new DecimalFormat();
    per_format.setMinimumIntegerDigits(1);
    per_format.setMinimumFractionDigits(0);
    per_format.setMaximumFractionDigits(1);
    per_format.setPositiveSuffix("%");
    per_format.setGroupingUsed(false);
  }

  public GraphScoreThreshSetter(NeoWidgetI w,
				GraphVisibleBoundsSetter bounds_setter) {
    widg = w;
    per_info_provider = bounds_setter;

    score_val_slider = new JSlider(JSlider.HORIZONTAL);
    score_percent_slider = new JSlider(JSlider.HORIZONTAL,
				     (int)(abs_min_per * sliders_per_percent),
				     (int)(abs_max_per * sliders_per_percent),
				     (int)(prev_score_per * sliders_per_percent));

    score_valT = new JTextField(20);
    score_perT = new JTextField(20);

    JPanel labP = new JPanel();
    JPanel textP = new JPanel();
    JPanel slideP = new JPanel();

    score_valT.setMinimumSize(new Dimension(tf_min_xpix, tf_min_ypix));
    score_perT.setMinimumSize(new Dimension(tf_min_xpix, tf_min_ypix));
    score_valT.setMaximumSize(new Dimension(tf_max_xpix, tf_max_ypix));
    score_perT.setMaximumSize(new Dimension(tf_max_xpix, tf_max_ypix));

    labP.setLayout(new BoxLayout(labP, BoxLayout.Y_AXIS));
    textP.setLayout(new BoxLayout(textP, BoxLayout.Y_AXIS));
    slideP.setLayout(new BoxLayout(slideP, BoxLayout.Y_AXIS));

    thresh_aboveB = new JRadioButton("> thresh");
    thresh_belowB = new JRadioButton("<= thresh");
    ButtonGroup pgroup = new ButtonGroup();
    pgroup.add(thresh_aboveB);
    pgroup.add(thresh_belowB);
    thresh_aboveB.setSelected(true);
    thresh_belowB.setSelected(false);
    JPanel directionP = new JPanel();
    directionP.setLayout(new GridLayout(1, 3));
    directionP.add(new JLabel("Direction: "));
    directionP.add(thresh_aboveB);
    directionP.add(thresh_belowB);

    labP.add(new JLabel("By Value:"));
    labP.add(new JLabel("By % Probes < Value:"));
    textP.add(score_valT);
    textP.add(score_perT);
    slideP.add(score_val_slider);
    slideP.add(score_percent_slider);

    //    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    this.setLayout(new BorderLayout());
    JPanel cbox = new JPanel();
    cbox.setLayout(new BoxLayout(cbox, BoxLayout.X_AXIS));
    this.add("South", directionP);
    this.add("Center", cbox);

    cbox.add(labP);
    cbox.add(textP);
    cbox.add(slideP);

  }

  /**
   *  Sets the list of graphs.
   *  Filters out any graphs that aren't SmartGraphGlyphs
   *  so other methods don't have to worry about doing type checking.
   */
  public void setGraphs(java.util.List newgraphs) {
    turnOffListening();
    graphs.clear();
    int gcount = newgraphs.size();
    for (int i=0; i<gcount; i++) {
      GraphGlyph gl = (GraphGlyph)newgraphs.get(i);
      if (gl instanceof SmartGraphGlyph) {
	graphs.add(gl);
      }
    }
    initPercents();
    initValues();
    setEnabled( ! graphs.isEmpty());
    turnOnListening();
  }

  public void setEnabled(boolean b) {
    super.setEnabled(b);
    score_val_slider.setEnabled(b);
    score_percent_slider.setEnabled(b);
    score_valT.setEnabled(b);
    score_perT.setEnabled(b);
    thresh_aboveB.setEnabled(b);
    thresh_belowB.setEnabled(b);
  }

  public void initValues() {
    if (graphs.size() > 0) {
    abs_min_val = Float.POSITIVE_INFINITY;
    abs_max_val = Float.NEGATIVE_INFINITY;
    float min_of_scoremins = Float.POSITIVE_INFINITY;
    float max_of_scoremins = Float.NEGATIVE_INFINITY;
    float avg_of_scoremins = 0;
    int gcount = graphs.size();

    for (int i=0; i<gcount; i++) {
      SmartGraphGlyph gl = (SmartGraphGlyph)graphs.get(i);
      float min = gl.getGraphMinY();
      float max = gl.getGraphMaxY();
      float scoremin = gl.getMinScoreThreshold();
      abs_min_val = Math.min(abs_min_val, min);
      abs_max_val = Math.max(abs_max_val, max);
      min_of_scoremins = Math.min(min_of_scoremins, scoremin);
      max_of_scoremins = Math.max(max_of_scoremins, scoremin);
      avg_of_scoremins += scoremin;
    }

    // set default thresh to average thresh of selected graphs
    avg_of_scoremins = avg_of_scoremins / gcount;

    float val_range = abs_max_val - abs_min_val;
    //    sliders_per_score = 1000.0f/val_range;

    sliders_per_val = (total_val_sliders) / val_range;
    vals_per_slider = 1.0f / sliders_per_val;

    score_val_slider.setMinimum(calcSliderForValue(abs_min_val));
    score_val_slider.setMaximum(calcSliderForValue(abs_max_val));

    score_val_slider.setValue(calcSliderForValue(avg_of_scoremins));
    if (min_of_scoremins == max_of_scoremins) {
      score_valT.setText(val_format.format(min_of_scoremins));
    }
    else {
      score_valT.setText(val_format.format(min_of_scoremins) +
			" : " + val_format.format(max_of_scoremins));
    }
    prev_score_val = avg_of_scoremins;
    }
  }

  public void initPercents() {
    if (graphs.size() > 0) {
    float min_of_scoremins = Float.POSITIVE_INFINITY;
    float max_of_scoremins = Float.NEGATIVE_INFINITY;
    float avg_of_scoremins = 0;
    int gcount = graphs.size();
    for (int i=0; i<gcount; i++) {
      SmartGraphGlyph gl = (SmartGraphGlyph)graphs.get(i);
      float val = gl.getMinScoreThreshold();
      float percent = per_info_provider.getPercentForValue(gl, val);
      min_of_scoremins = Math.min(min_of_scoremins, percent);
      max_of_scoremins = Math.max(max_of_scoremins, percent);
      avg_of_scoremins += percent;
    }
    avg_of_scoremins = avg_of_scoremins / gcount;
    if (min_of_scoremins == max_of_scoremins) {
      score_perT.setText(per_format.format(min_of_scoremins));
    }
    else {
      score_perT.setText(per_format.format(min_of_scoremins) + " : " + per_format.format(max_of_scoremins));
    }
    //    System.out.println("setting min percent slider: " + (int)(avg_of_scoremins * sliders_per_percent));
    score_percent_slider.setValue((int)(avg_of_scoremins * sliders_per_percent));
    prev_score_per = avg_of_scoremins;
    }
  }


  public float calcValueForSlider(int slider_val) {
    return slider_val/sliders_per_val;
  }

  public int calcSliderForValue(float thresh_score) {
    return (int)(thresh_score * sliders_per_val);
  }


  public void stateChanged(ChangeEvent evt) {
    if (graphs.size() <= 0) { return; }
    Object src = evt.getSource();
    if (src == score_val_slider) {
      float min_val = calcValueForSlider(score_val_slider.getValue());
      if (min_val != prev_score_val) {
	setScoreThreshold(min_val);
      }
    }
    else if (src == score_percent_slider) {
      float min_per = score_percent_slider.getValue() * percents_per_slider;
      if (min_per != prev_score_per) {
	setScoreMinPercent(min_per);
      }
    }
  }

  public void actionPerformed(ActionEvent evt) {
    if (graphs.size() <= 0) { return; }
    Object src = evt.getSource();
    if (src == score_valT) {
      try {
	float minval = Float.parseFloat(score_valT.getText());
	if (minval < abs_min_val) { minval = abs_min_val; }
	else if (minval > abs_max_val) { minval = abs_max_val; }
	setScoreThreshold(minval);  // also sets prev_min_val
      }
      catch (NumberFormatException ex) { // couldn't parse, keep same...
	score_valT.setText(val_format.format(prev_score_val));
      }
    }
    else if (src == score_perT) {
      try {
        float minper = GraphAdjusterView.parsePercent(score_perT.getText());
	if (minper < abs_min_per) { minper = abs_min_per; }
	else if (minper > abs_max_per) { minper = abs_max_per; }
	setScoreMinPercent(minper); // also sets prev_min_per
      }
      catch (NumberFormatException ex) { // couldn't parse, keep same...
	score_perT.setText(per_format.format(prev_score_per));
      }
    }
    else if (src == thresh_aboveB) {
      if (thresh_aboveB.isSelected()) {
	setThresholdDirection(true);
      }
    }
    else if (src == thresh_belowB) {
      if (thresh_belowB.isSelected()) {
	setThresholdDirection(false);
      }
    }
  }



  public void turnOffListening() {
    score_val_slider.removeChangeListener(this);
    score_valT.removeActionListener(this);
    score_percent_slider.removeChangeListener(this);
    score_perT.removeActionListener(this);
    thresh_aboveB.removeActionListener(this);
    thresh_belowB.removeActionListener(this);
  }

  public void turnOnListening() {
    score_val_slider.addChangeListener(this);
    score_valT.addActionListener(this);
    score_percent_slider.addChangeListener(this);
    score_perT.addActionListener(this);
    thresh_aboveB.addActionListener(this);
    thresh_belowB.addActionListener(this);
  }

  /**
   *  Sets the flag thresh_is_min.
   *  if (thresh_is_min), then values must >= threshold to pass.
   *  if (! thresh_is_min), then values must be <= threshold to pass.
   */
  public void setThresholdDirection(boolean b) {
    if (thresh_is_min != b) {
      turnOffListening();
      thresh_is_min = b;
      int gcount = graphs.size();
      for (int i=0; i<gcount; i++) {
	SmartGraphGlyph sgg = (SmartGraphGlyph)graphs.get(i);
	if (thresh_is_min) {
	  float prev_max_thresh = sgg.getMaxScoreThreshold();
	  sgg.setMinScoreThreshold(prev_max_thresh);
	  sgg.setMaxScoreThreshold(Float.POSITIVE_INFINITY);
	}
	else {
	  float prev_min_thresh = sgg.getMinScoreThreshold();
	  sgg.setMinScoreThreshold(Float.NEGATIVE_INFINITY);
	  sgg.setMaxScoreThreshold(prev_min_thresh);
	}
      }
      widg.updateWidget();
      turnOnListening();
    }
  }

  public void setScoreThreshold(float val) {
    setScoreThreshold(val, false);
  }

  public void setScoreThreshold(float val, boolean force_change) {
    int gcount = graphs.size();
    if (force_change  || (gcount > 0 && (val != prev_score_val))) {
      turnOffListening();
      float min_per = Float.POSITIVE_INFINITY;
      float max_per = Float.NEGATIVE_INFINITY;
      float avg_per = 0;
      for (int i=0; i<gcount; i++) {
	SmartGraphGlyph sgg = (SmartGraphGlyph)graphs.get(i);
	float percent = per_info_provider.getPercentForValue(sgg, val);
	min_per = Math.min(percent, min_per);
	max_per = Math.max(percent, max_per);
	avg_per += percent;

	if (thresh_is_min) {
	  sgg.setMinScoreThreshold(val);
	  sgg.setMaxScoreThreshold(Float.POSITIVE_INFINITY);
	}
	else {
	  sgg.setMinScoreThreshold(Float.NEGATIVE_INFINITY);
	  sgg.setMaxScoreThreshold(val);
	}
      }
      avg_per = avg_per / gcount;
      widg.updateWidget();

      // set values
      score_valT.setText(val_format.format(val));
      score_val_slider.setValue(calcSliderForValue(val));

      // set percentages
      if (min_per == max_per) {
	score_perT.setText(per_format.format(avg_per));
      }
      else {
	score_perT.setText(per_format.format(min_per) + " : " + per_format.format(max_per));
      }
      score_percent_slider.setValue((int)(min_per * sliders_per_percent));

      prev_score_val = val;
      turnOnListening();
    }
  }

  public void setScoreMinPercent(float percent) {
    int gcount = graphs.size();
    if (gcount > 0 && (percent != prev_score_per)) {
      turnOffListening();
      float min_val = Float.POSITIVE_INFINITY;
      float max_val = Float.NEGATIVE_INFINITY;
      float avg_val = 0;
      for (int i=0; i<gcount; i++) {
	SmartGraphGlyph sgg = (SmartGraphGlyph)graphs.get(i);
	float val = per_info_provider.getValueForPercent(sgg, percent);

	min_val = Math.min(val, min_val);
	max_val = Math.max(val, max_val);
	avg_val += val;

	if (thresh_is_min) {
	  sgg.setMinScoreThreshold(val);
	  sgg.setMaxScoreThreshold(Float.POSITIVE_INFINITY);
	}
	else {
	  sgg.setMinScoreThreshold(Float.NEGATIVE_INFINITY);
	  sgg.setMaxScoreThreshold(val);
	}
      }
      avg_val = avg_val / gcount;
      widg.updateWidget();

      // set percentages
      score_perT.setText(per_format.format(percent));
      score_percent_slider.setValue((int)(percent * sliders_per_percent));

      // set values
      if (min_val == max_val) {
	score_valT.setText(val_format.format(avg_val));
      }
      else {
	score_valT.setText(val_format.format(min_val) +
			 " : " + val_format.format(max_val));
      }
      score_val_slider.setValue(calcSliderForValue(avg_val));

      prev_score_per = percent;
      turnOnListening();
    }
  }


  /*
   static float[] flipArray(float[] forward) {
    int length = forward.length;
    float[] reverse = new float[length];
    for (int i=0; i<length; i++) {
      reverse[i] = forward[length-i-1];
    }
    return reverse;
  }
  */

  public void deleteGraph(GraphGlyph gl) {
    Object info = gl.getInfo();
    graphs.remove(gl);
    setGraphs(graphs);
  }


}
