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

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.widget.NeoWidgetI;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.genometry.SimpleSymWithProps;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.tiers.AnnotStyle;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.view.GraphAdjusterView;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.swing.DisplayUtils;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.text.DecimalFormat;
import java.util.*;


public class GraphScoreThreshSetter extends JPanel
  implements ChangeListener, ActionListener  {

  SeqMapView gviewer = null;
  
  static Object placeholder_object = new Object();
  static DecimalFormat val_format;
  static DecimalFormat per_format;
  static DecimalFormat shift_format;
  static String BLANK = "";
  static String ON = "On";
  static String OFF = "Off";

  Dimension slider_sizepref = new Dimension(600, 15);
  Dimension textbox_sizepref = new Dimension(400, 15);
  boolean set_slider_sizepref = false;
  boolean set_textbox_sizepref = false;
//  boolean thresh_is_min = true;

  java.util.List graphs = new ArrayList();
  Map flipped_hash = new HashMap();

  NeoWidgetI widg;
  GraphVisibleBoundsSetter per_info_provider;
  MaxGapThresholder max_gap_thresher;
  MinRunThresholder min_run_thresher;

  JSlider score_val_slider;
  JSlider score_percent_slider;
  JTextField score_valT;
  JTextField score_perT;
  JRadioButton thresh_aboveB;
  JRadioButton thresh_belowB;
  JRadioButton thresh_unknownB; // invisible radio button
  JTextField shift_startTF = new JTextField("0", 5);
  JTextField shift_endTF = new JTextField("0", 5);
  JComboBox threshCB = new JComboBox();
  JButton tier_threshB = new JButton("Make Track");

  float sliders_per_percent = 10.0f;
  float percents_per_slider = 1.0f / sliders_per_percent;

  int total_val_sliders = 1000;
  float sliders_per_val;
  float vals_per_slider;


  float abs_min_val;
  float abs_max_val;
  float prev_thresh_val;

  float abs_min_per = 0;
  float abs_max_per = 100;
  float prev_thresh_per;

  boolean show_min_and_max = false;

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
    shift_format = new DecimalFormat();
  }

  public GraphScoreThreshSetter(SeqMapView gviewer,
				GraphVisibleBoundsSetter bounds_setter) {
    this.gviewer = gviewer;
    widg = gviewer.getSeqMap();
    per_info_provider = bounds_setter;

    score_val_slider = new JSlider(JSlider.HORIZONTAL);
    score_percent_slider = new JSlider(JSlider.HORIZONTAL,
				     (int)(abs_min_per * sliders_per_percent),
				     (int)(abs_max_per * sliders_per_percent),
				     (int)(prev_thresh_per * sliders_per_percent));

    score_valT = new JTextField(10);
    score_perT = new JTextField(10);

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
    thresh_unknownB = new JRadioButton(">?< thresh"); // invisible button
    ButtonGroup pgroup = new ButtonGroup();
    pgroup.add(thresh_aboveB);
    pgroup.add(thresh_belowB);
    pgroup.add(thresh_unknownB);
    thresh_aboveB.setSelected(false);
    thresh_belowB.setSelected(false);
    thresh_unknownB.setSelected(true);
    JPanel directionP = new JPanel();
    directionP.setLayout(new GridLayout(1, 3));
    directionP.add(new JLabel("Direction: "));
    directionP.add(thresh_aboveB);
    directionP.add(thresh_belowB);

    labP.add(new JLabel("By Value:"));
    labP.add(new JLabel("By Percentile:"));
    textP.add(score_valT);
    textP.add(score_perT);
    slideP.add(score_val_slider);
    slideP.add(score_percent_slider);
    
    JPanel cbox = new JPanel();
    cbox.setLayout(new BoxLayout(cbox, BoxLayout.X_AXIS));
    cbox.add(labP);
    cbox.add(textP);
    cbox.add(slideP);

    JPanel thresh_toggle_pan = new JPanel();
    thresh_toggle_pan.setLayout(new GridLayout(1, 2));
    threshCB.addItem(BLANK);
    threshCB.addItem(ON);
    threshCB.addItem(OFF);
    threshCB.setPreferredSize(new Dimension(30, 10));
    threshCB.setMaximumSize(new Dimension(60, 30));
    
    JPanel thresh_butP = new JPanel();
    thresh_butP.setLayout(new BoxLayout(thresh_butP, BoxLayout.X_AXIS));
    thresh_butP.add(new JLabel("Visibility  "));
    thresh_butP.add(threshCB);
    thresh_butP.add(tier_threshB);

    JPanel thresh_shiftP = new JPanel();
    thresh_shiftP.setBorder(new TitledBorder("Offsets for Thresholded Regions"));
    thresh_shiftP.setLayout(new GridLayout(1, 4));
    thresh_shiftP.add(new JLabel("Start  ", JLabel.RIGHT));
    thresh_shiftP.add(shift_startTF);
    thresh_shiftP.add(new JLabel("End  ", JLabel.RIGHT));
    thresh_shiftP.add(shift_endTF);
    thresh_shiftP.setMaximumSize(new Dimension(300, tf_max_ypix + 30));
    
    
    min_run_thresher = new MinRunThresholder(gviewer.getSeqMap());
    max_gap_thresher = new MaxGapThresholder(gviewer.getSeqMap());
        
    JPanel center_panel = new JPanel();    
    center_panel.setLayout(new BorderLayout());
    center_panel.add("South", directionP);
    center_panel.add("Center", cbox);

    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.add(thresh_butP);
    this.add(center_panel);
    this.add(thresh_shiftP);
    this.add(max_gap_thresher);
    this.add(min_run_thresher);    
  }
  
  JFrame thresh_setter_frame = null;
  
  public JFrame showFrame() {

    if (thresh_setter_frame == null) {
      JPanel thresh_setter_panel =  (JPanel) this;
      thresh_setter_frame = new JFrame("Graph Thresholds");
      thresh_setter_frame.getContentPane().add(thresh_setter_panel);
      thresh_setter_frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
      thresh_setter_frame.pack();

      Image icon = IGB.getIcon();
      if (icon != null) { thresh_setter_frame.setIconImage(icon); }

      Rectangle pos = UnibrowPrefsUtil.retrieveWindowLocation(thresh_setter_frame.getTitle(), thresh_setter_frame.getBounds());
      if (pos != null) {
        UnibrowPrefsUtil.setWindowSize(thresh_setter_frame, pos);
      }
      thresh_setter_frame.show();
      thresh_setter_frame.addWindowListener( new WindowAdapter() {
	  public void windowClosing(WindowEvent evt) {
            // save the current size into the preferences, so the window
            // will re-open with this size next time
            UnibrowPrefsUtil.saveWindowLocation(thresh_setter_frame, thresh_setter_frame.getTitle());
	  }
	});
    } else {
      DisplayUtils.bringFrameToFront(thresh_setter_frame);
    }
    return thresh_setter_frame;
  }

  /**
   *  Sets the list of GraphGlyphs.
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

    boolean thresh_is_on = false;
    if (graphs.isEmpty()) {
      threshCB.setSelectedIndex(-1);
      threshCB.setEnabled(false);
      tier_threshB.setEnabled(false);
    } else {
      SmartGraphGlyph first_glyph = (SmartGraphGlyph) graphs.get(0);
      boolean show_thresholds_match = true;
      boolean thresh_directions_match = true;
      boolean thresh_starts_match = true;
      boolean thresh_ends_match = true;

      gcount = graphs.size();
      for (int i=1; i<gcount; i++) {
        SmartGraphGlyph sggl = (SmartGraphGlyph) graphs.get(i);
        show_thresholds_match &= (first_glyph.getShowThreshold() == sggl.getShowThreshold());
        thresh_directions_match &= (first_glyph.getThresholdDirection() == sggl.getThresholdDirection());
        thresh_starts_match &= (first_glyph.getThreshStartShift() == sggl.getThreshStartShift());
        thresh_ends_match &= (first_glyph.getThreshEndShift() == sggl.getThreshEndShift());
      }
          
      if (show_thresholds_match) {
        if (first_glyph.getShowThreshold()) {
          threshCB.setSelectedItem(ON);
          thresh_is_on = true;
        } else {
          threshCB.setSelectedItem(OFF);
        }
      } else {
        threshCB.setSelectedIndex(-1);
      }
      
      if (thresh_directions_match) {
        if (first_glyph.getThresholdDirection() == GraphState.THRESHOLD_DIRECTION_GREATER) {
          thresh_aboveB.setSelected(true);
        } else {
          thresh_belowB.setSelected(true);
        }
      } else {
        thresh_unknownB.setSelected(true);
      }
      
      if (thresh_starts_match) {
        shift_startTF.setText(shift_format.format(first_glyph.getThreshStartShift()));
      } else {
        shift_startTF.setText("");
      }
      
      if (thresh_ends_match) {
        shift_endTF.setText(shift_format.format(first_glyph.getThreshEndShift()));
      } else {
        shift_endTF.setText("");
      }
      
      threshCB.setEnabled(true);
      tier_threshB.setEnabled(true);
    }
    
    max_gap_thresher.setGraphs(graphs);
    min_run_thresher.setGraphs(graphs);
    
    score_val_slider.setEnabled(thresh_is_on);
    score_percent_slider.setEnabled(thresh_is_on);
    score_valT.setEnabled(thresh_is_on);
    score_perT.setEnabled(thresh_is_on);
    thresh_aboveB.setEnabled(thresh_is_on);
    thresh_belowB.setEnabled(thresh_is_on);
    shift_startTF.setEnabled(thresh_is_on);
    shift_endTF.setEnabled(thresh_is_on);
    
    turnOnListening();
  }

  public void initValues() {
    if (graphs.size() > 0) {
    abs_min_val = Float.POSITIVE_INFINITY;
    abs_max_val = Float.NEGATIVE_INFINITY;
    float min_of_score_vals = Float.POSITIVE_INFINITY;
    float max_of_score_vals = Float.NEGATIVE_INFINITY;
    float avg_of_score_vals = 0;
    int gcount = graphs.size();

    for (int i=0; i<gcount; i++) {
      SmartGraphGlyph gl = (SmartGraphGlyph)graphs.get(i);
      float min = gl.getGraphMinY();
      float max = gl.getGraphMaxY();
      float score_val;
      if (gl.getThresholdDirection() == GraphState.THRESHOLD_DIRECTION_GREATER) {
        score_val = gl.getMinScoreThreshold();
      } else { // assume direction is GraphState.THRESHOLD_DIRECTION_LESS
        score_val = gl.getMaxScoreThreshold();        
      }
      abs_min_val = Math.min(abs_min_val, min);
      abs_max_val = Math.max(abs_max_val, max);
      min_of_score_vals = Math.min(min_of_score_vals, score_val);
      max_of_score_vals = Math.max(max_of_score_vals, score_val);
      avg_of_score_vals += score_val;
    }

    // set default thresh to average thresh of selected graphs
    avg_of_score_vals = avg_of_score_vals / gcount;

    float val_range = abs_max_val - abs_min_val;
    //    sliders_per_score = 1000.0f/val_range;

    sliders_per_val = (total_val_sliders) / val_range;
    vals_per_slider = 1.0f / sliders_per_val;

    score_val_slider.setMinimum(calcSliderForValue(abs_min_val));
    score_val_slider.setMaximum(calcSliderForValue(abs_max_val));

    score_val_slider.setValue(calcSliderForValue(avg_of_score_vals));
    if (min_of_score_vals == max_of_score_vals) {
      score_valT.setText(val_format.format(min_of_score_vals));
    } else {
      if (show_min_and_max) {
        score_valT.setText(val_format.format(min_of_score_vals) +
          " : " + val_format.format(max_of_score_vals));
      } else {
        score_valT.setText("");
      }
    }
    prev_thresh_val = avg_of_score_vals;
    } else {
      score_valT.setText("");
    }
  }

  public void initPercents() {
    if (graphs.size() > 0) {
    float min_of_score_vals = Float.POSITIVE_INFINITY;
    float max_of_score_vals = Float.NEGATIVE_INFINITY;
    float avg_of_score_vals = 0;
    int gcount = graphs.size();
    for (int i=0; i<gcount; i++) {
      SmartGraphGlyph gl = (SmartGraphGlyph)graphs.get(i);
      float val;
      if (gl.getThresholdDirection() == GraphState.THRESHOLD_DIRECTION_GREATER) {
        val = gl.getMinScoreThreshold();
      } else { // assume direction is GraphState.THRESHOLD_DIRECTION_LESS
        val = gl.getMaxScoreThreshold();        
      }
      float percent = per_info_provider.getPercentForValue(gl, val);
      min_of_score_vals = Math.min(min_of_score_vals, percent);
      max_of_score_vals = Math.max(max_of_score_vals, percent);
      avg_of_score_vals += percent;
    }
    avg_of_score_vals = avg_of_score_vals / gcount;
    if (min_of_score_vals == max_of_score_vals) {
      score_perT.setText(per_format.format(min_of_score_vals));
    } else {
      if (show_min_and_max) {
        score_perT.setText(per_format.format(min_of_score_vals) + " : " + per_format.format(max_of_score_vals));        
      } else {
        score_perT.setText("");
      }
    }
    //    System.out.println("setting min percent slider: " + (int)(avg_of_scoremins * sliders_per_percent));
    score_percent_slider.setValue((int)(avg_of_score_vals * sliders_per_percent));
    prev_thresh_per = avg_of_score_vals;
    } else {
      score_perT.setText("");
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
      float thresh = calcValueForSlider(score_val_slider.getValue());
      //if (thresh != prev_thresh_val) {
        setScoreThreshold(thresh);
      //}
    } else if (src == score_percent_slider) {
      float thresh_per = score_percent_slider.getValue() * percents_per_slider;
      //if (thresh_per != prev_thresh_per) {
        setScoreThresholdByPercent(thresh_per);
      //}
    }
  }

  public void actionPerformed(ActionEvent evt) {
    if (graphs.size() <= 0) { return; }
    Object src = evt.getSource();
    if (src == score_valT) {
      try {
	float thresh = Float.parseFloat(score_valT.getText());
	if (thresh < abs_min_val) { thresh = abs_min_val; }
	else if (thresh > abs_max_val) { thresh = abs_max_val; }
        setScoreThreshold(thresh);  // also sets prev_thresh_val          
      }
      catch (NumberFormatException ex) { // couldn't parse, keep same...
	score_valT.setText(val_format.format(prev_thresh_val));
      }
    }
    else if (src == score_perT) {
      try {
        float thresh_per = GraphAdjusterView.parsePercent(score_perT.getText());
	if (thresh_per < abs_min_per) { thresh_per = abs_min_per; }
	else if (thresh_per > abs_max_per) { thresh_per = abs_max_per; }
	setScoreThresholdByPercent(thresh_per); // also sets prev_thresh_per
      }
      catch (NumberFormatException ex) { // couldn't parse, keep same...
	score_perT.setText(per_format.format(prev_thresh_per));
      }
    }
    else if (src == thresh_aboveB) {
      setScoreThreshold(prev_thresh_val, GraphState.THRESHOLD_DIRECTION_GREATER, true);
    }
    else if (src == thresh_belowB) {
      setScoreThreshold(prev_thresh_val, GraphState.THRESHOLD_DIRECTION_LESS, true);
    }
    else if (src == shift_startTF) {
      try {
        int start_shift = Integer.parseInt(shift_startTF.getText());
        adjustThreshStartShift(graphs, start_shift);
      }
      catch (Exception ex) { ex.printStackTrace(); }
    }
    else if (src == shift_endTF) {
      try {
        int end_shift = Integer.parseInt(shift_endTF.getText());
        adjustThreshEndShift(graphs, end_shift);
      }
      catch (Exception ex) { ex.printStackTrace(); }
    }
    else if (src == tier_threshB) {
      int gcount = graphs.size();
      for (int i=0; i<gcount; i++) {
        SmartGraphGlyph sggl = (SmartGraphGlyph) graphs.get(i);
        System.out.println("pickling graph: " + sggl.getLabel());
        pickleThreshold(sggl);
      }
      widg.updateWidget();
    }
    else if (src == threshCB) {
      String selection = (String)((JComboBox)threshCB).getSelectedItem();
      boolean thresh_on = (selection == ON);
      boolean thresh_off = (selection == OFF);
      int gcount = graphs.size();
      for (int i=0; i<gcount; i++) {
        SmartGraphGlyph sggl = (SmartGraphGlyph) graphs.get(i);
        sggl.setShowThreshold(thresh_on);
      }
      widg.updateWidget();
      this.setGraphs(new java.util.ArrayList(graphs));
    }
  }



  public void turnOffListening() {
    score_val_slider.removeChangeListener(this);
    score_valT.removeActionListener(this);
    score_percent_slider.removeChangeListener(this);
    score_perT.removeActionListener(this);
    thresh_aboveB.removeActionListener(this);
    thresh_belowB.removeActionListener(this);
    threshCB.removeActionListener(this);
    tier_threshB.removeActionListener(this);
    shift_startTF.removeActionListener(this);
    shift_endTF.removeActionListener(this);
  }

  public void turnOnListening() {
    score_val_slider.addChangeListener(this);
    score_valT.addActionListener(this);
    score_percent_slider.addChangeListener(this);
    score_perT.addActionListener(this);
    thresh_aboveB.addActionListener(this);
    thresh_belowB.addActionListener(this);
    threshCB.addActionListener(this);
    tier_threshB.addActionListener(this);
    shift_startTF.addActionListener(this);
    shift_endTF.addActionListener(this);
  }
  
  void setScoreThreshold(float thresh) {
    if (thresh_aboveB.isSelected()) {
      setScoreThreshold(thresh, GraphState.THRESHOLD_DIRECTION_GREATER);  // also sets prev_thresh_val
    } else {
      setScoreThreshold(thresh, GraphState.THRESHOLD_DIRECTION_LESS);  // also sets prev_thresh_val          
    }    
  }
  
  public void setScoreThreshold(float val, int direction) {
    setScoreThreshold(val, direction, true);
  }

  public void setScoreThreshold(float val, int direction, boolean force_change) {
    int gcount = graphs.size();
    force_change = true;  // (maybe temporarily) force change to always take place
    if (force_change  || (gcount > 0 && (val != prev_thresh_val))) {
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

        sgg.setThresholdDirection(direction);
	if (direction == GraphState.THRESHOLD_DIRECTION_GREATER) {
	  sgg.setMinScoreThreshold(val);
	}
	else if (direction == GraphState.THRESHOLD_DIRECTION_LESS) {
	  sgg.setMaxScoreThreshold(val);
	}
        else {
          System.out.println("Threshold 'between' not yet implemented");
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
      } else {
        if (show_min_and_max) {
          score_perT.setText(per_format.format(min_per) + " : " + per_format.format(max_per));
        } else {
          score_perT.setText("");
        }
      }
      score_percent_slider.setValue((int)(min_per * sliders_per_percent));

      turnOnListening();
    }
    prev_thresh_val = val;
  }

  void setScoreThresholdByPercent(float thresh) {
    if (thresh_aboveB.isSelected()) {
      setScoreThresholdByPercent(thresh, GraphState.THRESHOLD_DIRECTION_GREATER);  // also sets prev_thresh_per
    } else {
      setScoreThresholdByPercent(thresh, GraphState.THRESHOLD_DIRECTION_LESS);  // also sets prev_thresh_per
    }    
  }

  void setScoreThresholdByPercent(float percent, int direction) {
    int gcount = graphs.size();
    boolean force_change = true;  // (maybe temporarily) force change to always take place
    if (force_change  || (gcount > 0 && (percent != prev_thresh_per))) {
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

        sgg.setThresholdDirection(direction);
	if (direction == GraphState.THRESHOLD_DIRECTION_GREATER) {
	  sgg.setMinScoreThreshold(val);
	}
	else if (direction == GraphState.THRESHOLD_DIRECTION_LESS) {
	  sgg.setMaxScoreThreshold(val);
	}
        else {
          System.out.println("Threshold 'between' not yet implemented");
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
      } else {
        if (show_min_and_max) {
          score_valT.setText(val_format.format(min_val) +
            " : " + val_format.format(max_val));
        } else {
          score_valT.setText("");
        }
      }
      score_val_slider.setValue(calcSliderForValue(avg_val));

      turnOnListening();
    }
    prev_thresh_per = percent;
  }


  /**
   *  Sets the ThreshStartShift on a collection of SmartGraphGlyphs.
   */
  public void adjustThreshStartShift(Collection glyphs, int shift) {
    Iterator iter = glyphs.iterator();
    while (iter.hasNext()) {
      SmartGraphGlyph sggl = (SmartGraphGlyph) iter.next();
      sggl.setThreshStartShift(shift);
    }
  }

  /**
   *  Sets the ThreshEndShift on a collection of SmartGraphGlyphs.
   */
  public void adjustThreshEndShift(Collection glyphs, int shift) {
    Iterator iter = glyphs.iterator();
    while (iter.hasNext()) {
      SmartGraphGlyph sggl = (SmartGraphGlyph) iter.next();
      sggl.setThreshEndShift(shift);
    }
  }

  static DecimalFormat nformat = new DecimalFormat();
  int pickle_count = 0;
  public void pickleThreshold(SmartGraphGlyph sgg) {
    SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq) gmodel.getSelectedSeq();

    SimpleSymWithProps psym = new SimpleSymWithProps();
    psym.addSpan(new SimpleMutableSeqSpan(0, aseq.getLength(), aseq));
    //    String meth = "graph pickle " + pickle_count;
    String meth =
      "thresh, min_score=" + nformat.format(sgg.getMinScoreThreshold()) +
      ", max_score=" + nformat.format(sgg.getMaxScoreThreshold()) +
      ", thresh_direction=" + sgg.getThresholdDirection() +
      ", max_gap=" + (int)sgg.getMaxGapThreshold() +
      ", min_run=" + (int)sgg.getMinRunThreshold() +
      ", graph: " + sgg.getLabel();
    pickle_count++;
    psym.setProperty("method", meth);
    ViewI view = gviewer.getSeqMap().getView();
    sgg.drawThresholdedRegions(view, psym, aseq);
    aseq.addAnnotation(psym);
    Color col = sgg.getColor();
    //    Color col = Color.red;
    AnnotStyle annot_style = AnnotStyle.getInstance(meth, false);
    annot_style.setColor(col);
    annot_style.setGlyphDepth(1);
    
    gviewer.setAnnotatedSeq(gmodel.getSelectedSeq(), true, true);
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
    // done in setGraphs? max_gap_thresher.deleteGraph(gl);
    //  min_run_thresher.deleteGraph(gl);
    setGraphs(graphs);
  }


}
