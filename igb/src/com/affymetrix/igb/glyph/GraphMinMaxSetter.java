/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.affymetrix.genoviz.widget.*;

public class GraphMinMaxSetter extends JPanel 
  implements ChangeListener, ActionListener  {
  java.util.List graphs;
  NeoWidgetI widg;
  JSlider min_slider;
  JSlider max_slider;
  JTextField min_valT;
  JTextField max_valT;

  int total_slider_units = 1000;
  int minor_tick_spacing = total_slider_units / 100;
  int major_tick_spacing = total_slider_units / 20;
  
  float min;  // min ycoord for graph
  float max;  // max ycoord for graph
  float prevhi;  // last setting for graph visible ycoord ceiling
  float prevlo;  // last setting for graph visible ycoord floor
  float sliders_per_coord; // slider units per y coord
  float coords_per_slider; // y coords per slider unit

  static GraphMinMaxSetter showFramedAdjuster(java.util.List graphlist, NeoWidgetI widg) {
    GraphMinMaxSetter test_setter = new GraphMinMaxSetter(graphlist, widg);
    JFrame frm = new JFrame("Graph Min/Max Adjuster");
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", test_setter);
    frm.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
	Window w = evt.getWindow();
	w.setVisible(false);
	w.dispose();
      }
    } );
    frm.pack();
    frm.show();
    return test_setter;
  }
  
  public GraphMinMaxSetter(NeoWidgetI w) {    
    this.widg = w;
    min_valT = new JTextField(8);
    max_valT = new JTextField(8);
    min_slider = new JSlider(JSlider.HORIZONTAL);
    max_slider = new JSlider(JSlider.HORIZONTAL);

    min_slider.setPreferredSize(new Dimension(400, 15));
    max_slider.setPreferredSize(new Dimension(400, 15));

    JPanel sliderpan = new JPanel();
    sliderpan.setLayout(new GridLayout(2,1));
    sliderpan.add(min_slider);
    sliderpan.add(max_slider);

    JPanel valpan = new JPanel();
    valpan.setLayout(new GridLayout(2,2));
    valpan.add(new JLabel("MIN:"));
    valpan.add(min_valT);
    valpan.add(new JLabel("MAX:"));
    valpan.add(max_valT);

    this.setLayout(new BorderLayout());
    this.add("Center", sliderpan);
    this.add("West", valpan);

    min_slider.addChangeListener(this);
    max_slider.addChangeListener(this);
    min_valT.addActionListener(this);
    max_valT.addActionListener(this);
  }


  public GraphMinMaxSetter(java.util.List graphlist, NeoWidgetI w) {
    this(w);
    setGraphs(graphlist);
  }


  public void setGraphs(java.util.List graphlist) {
    // removing listeners so manipulations in this method won't trigger events, 
    //   then adding them back in at end of method
    min_slider.removeChangeListener(this);
    max_slider.removeChangeListener(this);
    min_valT.removeActionListener(this);
    max_valT.removeActionListener(this);

    //    this.graphs = graphlist;
    this.graphs = new ArrayList(graphlist);
    int gcount = graphs.size();
    min = Float.POSITIVE_INFINITY;
    max = Float.NEGATIVE_INFINITY;
    prevlo = 0;
    prevhi = 0;
    for (int i=0; i<gcount; i++) {
      SmartGraphGlyph gl = (SmartGraphGlyph)graphs.get(i);
      min = (float)Math.min(min, (float)gl.getGraphMinY());
      max = (float)Math.max(max, (float)gl.getGraphMaxY());
      prevlo += (float)gl.getVisibleMinY();
      prevhi += (float)gl.getVisibleMaxY(); 
    }
    prevlo = prevlo / (float)gcount;
    prevhi = prevhi / (float)gcount;
    sliders_per_coord = ((float)total_slider_units) / (max - min);
    coords_per_slider = 1.0f / sliders_per_coord;

    min_valT.setText(Float.toString(prevlo));
    max_valT.setText(Float.toString(prevhi));

    min_slider.setMinimum((int)(min * sliders_per_coord));
    min_slider.setMaximum((int)(max * sliders_per_coord));
    min_slider.setValue((int)(prevlo * sliders_per_coord));

    max_slider.setMinimum((int)(min * sliders_per_coord));
    max_slider.setMaximum((int)(max * sliders_per_coord));
    max_slider.setValue((int)(prevhi * sliders_per_coord));

    // reinstating listeners that were removed at beginning of method
    min_slider.addChangeListener(this);
    max_slider.addChangeListener(this);
    min_valT.addActionListener(this);
    max_valT.addActionListener(this);
  }


  public void stateChanged(ChangeEvent evt) {
    Object src = evt.getSource();
    float newhi = (max_slider.getValue()/sliders_per_coord);
    float newlo = (min_slider.getValue()/sliders_per_coord);
    //    System.out.println("newmin = " + newlo + ", newmax = " + newhi + ", prevhi = " + prevhi);
      
    if (src == max_slider) {
      if (newhi <= prevlo) {
	prevhi = prevlo+1;
	max_slider.setValue((int)(prevhi * sliders_per_coord));
	max_slider.updateUI();
      }
      else if (newhi != prevhi) {
	//	sgg.setVisibleMaxY(newhi);
	setVisibleMaxY(newhi);
	prevhi = newhi;
	widg.updateWidget();
      }
      max_valT.setText(Float.toString((float)prevhi));
    }
    else if (src == min_slider) {
      if (newlo >= prevhi) {
	prevlo = prevhi-1;
	min_slider.setValue((int)(prevlo * sliders_per_coord));
	min_slider.updateUI();
      }
      else if (newlo != prevlo) {
	//	sgg.setVisibleMinY(newlo);
	setVisibleMinY(newlo);
	prevlo = newlo;
	widg.updateWidget();
      }
      min_valT.setText(Float.toString((float)prevlo));
    }
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();

    if (src == min_valT || src == max_valT)  {
      try {
	float minval = Float.parseFloat(min_valT.getText());
	float maxval = Float.parseFloat(max_valT.getText());
	if (minval != prevlo && src == min_valT) {
	  if (minval > prevhi) {
	    minval = prevhi-1;
	    min_valT.setText(Float.toString(minval));
	  }
	  prevlo = minval;
	  min_slider.setValue((int)(minval * sliders_per_coord));
	}
	else if (maxval != prevhi && src == max_valT) {
	  if (maxval < prevlo) {
	    maxval = prevlo+1;
	    max_valT.setText(Float.toString(maxval));
	  }
	  prevhi = maxval;
	  max_slider.setValue((int)(maxval * sliders_per_coord));
	}
	setVisibleMaxY(maxval);
	setVisibleMinY(minval);
      }
      catch (Exception ex) {
	ex.printStackTrace();
      }
      widg.updateWidget();
    }
  }

  /**
   *   Set visible max Y for all graphs under control of GraphMinMaxSetter.
   *   Doesn't force an updateWidget().
   */  
  public void setVisibleMaxY(float val) {
    int gcount = graphs.size();
    for (int i=0; i<gcount; i++) {
      SmartGraphGlyph gl = (SmartGraphGlyph)graphs.get(i);
      gl.setVisibleMaxY(val);
    }
  }

  /**
   *   Set visible min Y for all graphs under control of GraphMinMaxSetter.
   *   Doesn't force an updateWidget().
   */
  public void setVisibleMinY(float val) {
    int gcount = graphs.size();
    for (int i=0; i<gcount; i++) {
      SmartGraphGlyph gl = (SmartGraphGlyph)graphs.get(i);
      gl.setVisibleMinY(val);
    }
  }

}

