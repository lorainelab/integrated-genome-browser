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

package com.affymetrix.igb.bookmarks;

import com.affymetrix.igb.IGB;
import java.awt.*;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;

/**
 * A GUI editor for a Unibrow Control Bookmark.
 * This just has some fields for seeing and setting various parameters,
 * but does not have a "submit" button.  You need to supply the
 * "submit" button, etc.
 * Call {@link #setGUIFromBookmark(Bookmark)} to update the display.
 * Call {@link #setBookmarkFromGUI(Bookmark)} when a "submit" button is pressed.
 * @author  ed
 */
public class UCBEditor {
  Box main_box = new Box(BoxLayout.Y_AXIS);
  
  JLabel version_label = new JLabel("Version:", JLabel.TRAILING);
  JTextField version = new JTextField(20);
  JLabel seq_label = new JLabel("Seq ID:", JLabel.TRAILING);
  JTextField seq = new JTextField(20);

  JLabel start_label = new JLabel("Start:", JLabel.TRAILING);
  JTextField start = new JTextField(20);
  JLabel end_label = new JLabel("End:", JLabel.TRAILING);
  JTextField end = new JTextField(20);

  JLabel select_start_label = new JLabel("Select Start:", JLabel.TRAILING);
  JTextField select_start = new JTextField(20);
  JLabel select_end_label = new JLabel("Select End:", JLabel.TRAILING);
  JTextField select_end = new JTextField(20);

  Bookmark the_bm = null;
  
  public UCBEditor() {
    version_label.setLabelFor(version);
    seq_label.setLabelFor(seq);
    start_label.setLabelFor(start);
    end_label.setLabelFor(end);
    
    Box line1 = new Box(BoxLayout.X_AXIS);
    line1.add(version_label);
    line1.add(Box.createHorizontalStrut(5));
    line1.add(version);
    line1.add(Box.createHorizontalStrut(5));
    line1.add(seq_label);
    line1.add(Box.createHorizontalStrut(5));
    line1.add(seq);

    Box line2 = new Box(BoxLayout.X_AXIS);
    line2.add(start_label);
    line2.add(Box.createHorizontalStrut(5));
    line2.add(start);
    line2.add(Box.createHorizontalStrut(5));
    line2.add(end_label);
    line2.add(Box.createHorizontalStrut(5));
    line2.add(end);

    Box line3 = new Box(BoxLayout.X_AXIS);
    line3.add(select_start_label);
    line3.add(Box.createHorizontalStrut(5));
    line3.add(select_start);
    line3.add(Box.createHorizontalStrut(5));
    line3.add(select_end_label);
    line3.add(Box.createHorizontalStrut(5));
    line3.add(select_end);

    main_box.add(line1);
    main_box.add(line2);
    main_box.add(line3);
    main_box.add(Box.createVerticalGlue());

    setEnabled(false);
  }
  
  public Component getComponent() {
    return main_box;
  }
    
  public void setBookmarkFromGUI(Bookmark the_bm) {
    System.out.println("Applying changes to my bookmark ");
    System.out.println("Before: "+the_bm.getURL().toExternalForm());
    Map map = the_bm.getParameters();
    setStringParameter(map, Bookmark.VERSION, version.getText());
    setStringParameter(map, Bookmark.SEQID, seq.getText());
    setStringParameter(map, Bookmark.START, start.getText());
    setStringParameter(map, Bookmark.END, end.getText());
    setStringParameter(map, Bookmark.SELECTSTART, select_start.getText());
    setStringParameter(map, Bookmark.SELECTEND, select_end.getText());
    String str = Bookmark.constructURL(map);
    try {
      URL url = new URL(str);
      the_bm.setURL(url);
    } catch (MalformedURLException e) {
      JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this.main_box);
      IGB.errorPanel(frame, "Error", "Cannot construct bookmark", e);
    }
    System.out.println("");
    System.out.println("After: "+the_bm.getURL().toExternalForm());
    System.out.println("");
  }

  public void setGUIFromBookmark(Bookmark bm) {
    Map map;
    if (bm == null || ! bm.isUnibrowControl()) {
      this.setEnabled(false);
      map = Collections.EMPTY_MAP;
    } else {
      this.setEnabled(true);
      map = bm.getParameters();
    }
    seq.setText(getStringParameter(map, Bookmark.SEQID));
    version.setText(getStringParameter(map, Bookmark.VERSION));
    start.setText(getStringParameter(map, Bookmark.START));
    end.setText(getStringParameter(map, Bookmark.END));
    select_start.setText(getStringParameter(map, Bookmark.SELECTSTART));
    select_end.setText(getStringParameter(map, Bookmark.SELECTEND));
    return;
  }

  void setEnabled(boolean b) {
    version.setEnabled(b);
    seq.setEnabled(b);
    start.setEnabled(b);
    end.setEnabled(b);
    select_start.setEnabled(b);
    select_end.setEnabled(b);
  }
  
  String getStringParameter(Map map, String key) {
    String[] strings = (String []) map.get(key);
    if (strings == null) return "";
    else return (strings[0] == null ? "" : strings[0]);
  }

  void setStringParameter(Map map, String key, String value) {
    if (value == null) {
      value="";
    }
    String[] strings = new String[] {value};
    map.put(key, value);
  }
}
 