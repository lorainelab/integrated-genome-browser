/**
 *   Copyright (c) 2006 Affymetrix, Inc.
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

package com.affymetrix.igb.view;

import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.event.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.regex.*;
import javax.swing.*;

/** A Text Box for displaying and setting the range of a SeqMapView. */
class MapRangeBox extends JComponent implements NeoViewBoxListener, GroupSelectionListener {
  
  NeoMap map;
  SeqMapView gview;

  
  public JTextField range_box = new JTextField("") {
    public Dimension getPreferredSize() {
      Dimension d = super.getPreferredSize();
      return new Dimension(200, d.height);
    }
    public Dimension getMaximumSize() {
      Dimension d = super.getMaximumSize();
      return new Dimension(200, d.height);
    }
  };

  static final NumberFormat nformat = NumberFormat.getIntegerInstance();
  
  static final Pattern start_end_pattern = Pattern.compile("^\\s*([0-9,]+)\\s*[:]\\s*([0-9,]+)\\s*$");
  static final Pattern start_width_pattern = Pattern.compile("^\\s*([0-9,]+)\\s*[+]\\s*([0-9,]+)\\s*$");
  static final Pattern center_pattern = Pattern.compile("^\\s*([0-9,]+)\\s*\\s*$");
  
  public static final int FORMAT_START_END = 0;
  public static final int FORMAT_START_WIDTH = 1;
  public static final int FORMAT_CENTER = 2;
  
  public MapRangeBox(SeqMapView gview) {
    this.gview = gview;
    this.map = gview.getSeqMap();
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    
    this.add(range_box);

    range_box.setEditable(true);
    range_box.addActionListener(action_listener);
    map.addViewBoxListener(this);
  }

  public void destroy() {
    map.removeViewBoxListener(this);
    range_box.removeActionListener(action_listener);
    map = null;
    gview = null;
  }
  
  public void viewBoxChanged(NeoViewBoxChangeEvent e) {
    com.affymetrix.genoviz.bioviews.Rectangle2D vbox = e.getCoordBox();
    setRangeText(FORMAT_START_END, vbox.x, vbox.width + vbox.x);
  }
  
  public void groupSelectionChanged(GroupSelectionEvent evt) {
    range_box.setText("");
  }
    
  void setRangeText(int format, double start, double end) {
    if (format == FORMAT_START_END) {
      range_box.setText(nformat.format(start) + " : " + nformat.format(end));
    } else if (format == FORMAT_START_WIDTH) {
      range_box.setText(nformat.format(start) + " + " + nformat.format(end-start));      
    } else if (format == FORMAT_CENTER) {
      range_box.setText(nformat.format((start + end)/2));      
    }
  }
    
  ActionListener action_listener = new ActionListener() {
    public void actionPerformed(ActionEvent evt) {
      int[] current = map.getVisibleRange();
      double start = current[0];
      double end = current[1];
      gview.zoomTo(start, end);

      double width = end - start;
      int display_format = FORMAT_START_END;
      try {
        Matcher start_end_matcher = start_end_pattern.matcher(range_box.getText());
        Matcher start_width_matcher = start_width_pattern.matcher(range_box.getText());
        Matcher center_matcher = center_pattern.matcher(range_box.getText());
        
        if (start_end_matcher.matches()) {
          String start_text = start_end_matcher.group(1);
          String end_text = start_end_matcher.group(2);
          start = nformat.parse(start_text).doubleValue();
          end = nformat.parse(end_text).doubleValue();
          gview.zoomTo(start, end);
        }
        else if (start_width_matcher.matches()) {
          String start_text = start_width_matcher.group(1);
          String width_text = start_width_matcher.group(2);
          start = nformat.parse(start_text).doubleValue();
          end = start + nformat.parse(width_text).doubleValue();
          gview.zoomTo(start, end);
        }
        else if (center_matcher.matches()) {
          String center_text = center_matcher.group(1);
          double center = nformat.parse(center_text).doubleValue();
          start = center - width/2;
          end = center + width/2;
          gview.zoomTo(start, end);
          gview.setZoomSpotX(center);
        }
        else {
          setRangeText(display_format, start, end);
        }        
      } catch (Exception ex) {
        System.out.println("Exception: " + ex);
        setRangeText(display_format, start, end);
      }
    }
  };
}
