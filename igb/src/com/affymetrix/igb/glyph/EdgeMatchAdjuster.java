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
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.affymetrix.genoviz.widget.*;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.IGB;

public class EdgeMatchAdjuster extends JPanel implements ChangeListener  {
  static int frm_width = 400;
  static int frm_height = 200;
  GlyphEdgeMatcher edge_matcher;
  SeqMapView gviewer;
  JSlider tslider;
  int thresh_min = 0;
  int thresh_max = 100;
  int prev_thresh;

  private static EdgeMatchAdjuster singleton_adjuster = null;
  private static JFrame singleton_frame = null;

  public static EdgeMatchAdjuster showFramedThresholder(
    GlyphEdgeMatcher matcher, SeqMapView view) {
    if (singleton_adjuster == null) {
      singleton_adjuster = new EdgeMatchAdjuster(matcher, view);

      singleton_frame = new JFrame("Edge Sensitivity Adjuster");
      Image icon = IGB.getIcon();
      if (icon != null) { singleton_frame.setIconImage(icon); }
      Container cpane = singleton_frame.getContentPane();
      cpane.setLayout(new BorderLayout());
      cpane.add("Center", singleton_adjuster);

      singleton_frame.addWindowListener( new WindowAdapter() {
        public void windowClosing(WindowEvent evt) { 
          Window w = evt.getWindow();
          w.setVisible(false);
          w.dispose();
        }
      } );
    }

    singleton_adjuster.gviewer = view; // in case the SeqMapView isn't the same
    singleton_frame.pack();
    singleton_frame.setState(Frame.NORMAL);
    singleton_frame.toFront();
    singleton_frame.show();
    return singleton_adjuster;
  }

  private EdgeMatchAdjuster(GlyphEdgeMatcher matcher, SeqMapView view) {
    edge_matcher = matcher;
    gviewer = view;
    prev_thresh = (int)matcher.getFuzziness();
    tslider = new JSlider(JSlider.HORIZONTAL, 
                         thresh_min, thresh_max, prev_thresh);
    tslider.setMinorTickSpacing(2);
    tslider.setMajorTickSpacing(10);
    tslider.setPaintTicks(true);
    tslider.setPaintLabels(true);
    tslider.addChangeListener(this);
    tslider.setPreferredSize(new Dimension(400, 70));
    this.setLayout(new BorderLayout());
    this.add("Center", tslider);
  }

  public void stateChanged(ChangeEvent evt) {
    Object src = evt.getSource();
    if (src == tslider) {
      int current_thresh = tslider.getValue();
      if (current_thresh != prev_thresh) { 
        gviewer.adjustEdgeMatching(current_thresh);
        prev_thresh = current_thresh;
      }
    }
  }

}
