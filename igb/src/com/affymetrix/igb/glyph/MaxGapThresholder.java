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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.affymetrix.genoviz.widget.NeoWidgetI;


public final class MaxGapThresholder extends JPanel
  implements ChangeListener, ActionListener, FocusListener  {

  static int frm_width = 400;
  static int frm_height = 200;

  List graphs = new ArrayList();
  NeoWidgetI widg;
  JSlider tslider;
  JTextField maxgapTF;
  int default_thresh_max = 250;
  int default_thresh_min = 0;
  int thresh_max = default_thresh_max;
  int thresh_min = default_thresh_min;

  int maxgap_thresh = 0;

  int max_chars = 9;
  int max_pix_per_char = 6;
  int tf_min_xpix = max_chars * max_pix_per_char;
  int tf_max_xpix = tf_min_xpix + (2 * max_pix_per_char);
  int tf_min_ypix = 20;
  int tf_max_ypix = 25;

  static MaxGapThresholder showFramedThresholder(SmartGraphGlyph sgg, NeoWidgetI widg) {
    MaxGapThresholder dthresher = new MaxGapThresholder(sgg, widg);
    JFrame frm = new JFrame("Graph MaxGap Threshold Control");
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", dthresher);
    //    frm.setSize(frm_width, frm_height);
    frm.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
	Window w = evt.getWindow();
	w.setVisible(false);
	w.dispose();
      }
    } );
    frm.pack();
    frm.setVisible(true);
    return dthresher;
  }

  public MaxGapThresholder(SmartGraphGlyph gl, NeoWidgetI w) {
    this(w);
    setGraph(gl);
  }

  public MaxGapThresholder(NeoWidgetI w) {
    widg = w;

    tslider = new JSlider(JSlider.HORIZONTAL);
    tslider.setPreferredSize(new Dimension(400, 15));

    maxgapTF = new JTextField(max_chars);
    maxgapTF.setMinimumSize(new Dimension(tf_min_xpix, tf_min_ypix));
    maxgapTF.setMaximumSize(new Dimension(tf_max_xpix, tf_max_ypix));

    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    this.add(Box.createRigidArea(new Dimension(6,0)));
    this.add(new JLabel("Max Gap <= "));
    this.add(maxgapTF);
    this.add(tslider);

    tslider.addChangeListener(this);
    maxgapTF.addActionListener(this);
    
    maxgapTF.addFocusListener(this);
  }


  public void setGraphs(List newgraphs) {
    graphs.clear();
    tslider.removeChangeListener(this);
    maxgapTF.removeActionListener(this);

    int first_thresh = 0;
    boolean all_have_same_thresh = false;
    boolean all_have_thresh_on = false;
    
    int gcount = newgraphs.size();
    if (gcount > 0) {
      int newthresh = 0;
      for (int i=0; i<gcount; i++) {
	SmartGraphGlyph gl = (SmartGraphGlyph)newgraphs.get(i);
	graphs.add(gl);
        int this_thresh = (int) gl.getMaxGapThreshold();
	newthresh += this_thresh;
        if (i==0) {
          first_thresh = this_thresh;
          all_have_same_thresh = true;
          all_have_thresh_on = gl.getShowThreshold();
        } else {
          all_have_same_thresh = all_have_same_thresh && (this_thresh == first_thresh);
          all_have_thresh_on &= gl.getShowThreshold();
        }
      }
      maxgap_thresh = newthresh / gcount;
      tslider.setMinimum(thresh_min);
      tslider.setMaximum(thresh_max);
      tslider.setValue(maxgap_thresh);
      if (all_have_same_thresh) {
        maxgapTF.setText(Integer.toString(maxgap_thresh));
      } else {
        maxgapTF.setText("");
      }
    }

    tslider.addChangeListener(this);
    maxgapTF.addActionListener(this);
    setEnabled(all_have_thresh_on);
  }

  public void setGraph(SmartGraphGlyph gl) {
    List newgraphs = new ArrayList();
    newgraphs.add(gl);
    setGraphs(newgraphs);
  }

  public void setEnabled(boolean b) {
    super.setEnabled(b);
    tslider.setEnabled(b);
    maxgapTF.setEnabled(b);
  }
  
  public void stateChanged(ChangeEvent evt) {
    if (graphs.size() <= 0) { return; }
    Object src = evt.getSource();
    if (src == tslider) {
      int current_thresh = tslider.getValue();
      if (current_thresh != maxgap_thresh) {
	maxgap_thresh = current_thresh;
	for (int i=0; i<graphs.size(); i++) {
	  SmartGraphGlyph sgg = (SmartGraphGlyph)graphs.get(i);
	  sgg.setMaxGapThreshold(maxgap_thresh);
	}
	maxgapTF.removeActionListener(this);
	maxgapTF.setText(Integer.toString(maxgap_thresh));
	maxgapTF.addActionListener(this);
	widg.updateWidget();
      }
    }
  }

  public void actionPerformed(ActionEvent evt) {
    doAction(evt.getSource());
  }

  public void doAction(Object src) {
    if (graphs.size() <= 0) { return; }

    if (src == maxgapTF) try {
      int new_thresh = Integer.parseInt(maxgapTF.getText());
      if (new_thresh != maxgap_thresh) {
        boolean new_thresh_max = (new_thresh > thresh_max);
//	if ((new_thresh < thresh_min) || (new_thresh > thresh_max)) {
        if (new_thresh < thresh_min)  {
	  // new threshold outside of min/max possible, so keep current threshold instead
	  maxgapTF.setText(Integer.toString(maxgap_thresh));
	}
	else {
	  maxgap_thresh = new_thresh;
	  for (int i=0; i<graphs.size(); i++) {
	    SmartGraphGlyph sgg = (SmartGraphGlyph)graphs.get(i);
	    sgg.setMaxGapThreshold(maxgap_thresh);
	  }
	  tslider.removeChangeListener(this);
          if (new_thresh_max)  {
            thresh_max = maxgap_thresh;
            tslider.setMaximum(thresh_max);
          }
          else if (maxgap_thresh <= default_thresh_max)  {
            thresh_max = default_thresh_max;
            tslider.setMaximum(thresh_max);
          }
	  tslider.setValue(maxgap_thresh);
	  tslider.addChangeListener(this);
	  widg.updateWidget();
	}
      }
    } catch (NumberFormatException nfe) {
      setGraphs(new ArrayList(graphs));
    }
  }

  public void deleteGraph(GraphGlyph gl) {
    graphs.remove(gl);
    setGraphs(new ArrayList(graphs));
  }

  public void focusGained(FocusEvent e) {
  }

  public void focusLost(FocusEvent e) {
    Object src = e.getSource();
    if (src instanceof JTextField) {
      doAction(src);
    }
  }

}
