/**
*   Copyright (c) 2005-2006 Affymetrix, Inc.
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.text.DecimalFormat;

public class StatusBar extends JPanel {
  
  JLabel position_ta;
  JLabel status_ta;
  JLabel memory_ta;

  JPopupMenu popup_menu = new JPopupMenu();
  
  DecimalFormat num_format;
  
  /** Delay in milliseconds between updates of the status (such as memory usage).  */
  static int timer_delay_ms = 5000;
  
  public StatusBar() {
    position_ta = new JLabel("");
    status_ta = new JLabel("");
    memory_ta = new JLabel("");
    position_ta.setBorder(new BevelBorder(BevelBorder.LOWERED));
    status_ta.setBorder(new BevelBorder(BevelBorder.LOWERED));
    memory_ta.setBorder(new BevelBorder(BevelBorder.LOWERED));
    
    position_ta.setToolTipText("Hairline Position");
    status_ta.setToolTipText("Shows Selected Item, or other Message");
    memory_ta.setToolTipText("Memory Used / Available");
    
    //num_format = NumberFormat.getIntegerInstance();
    num_format = new DecimalFormat();
    num_format.setMaximumFractionDigits(1);
    num_format.setMinimumFractionDigits(1);
    
    BorderLayout bl = new BorderLayout();
    setLayout(bl);
    
    this.add(position_ta, BorderLayout.WEST);
    this.add(status_ta, BorderLayout.CENTER);
    this.add(memory_ta, BorderLayout.EAST);

    JMenuItem gc_MI = new JMenuItem("Release Unused Memory");
    popup_menu.add(gc_MI);
    
    gc_MI.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        System.gc();
      }
    });
    
    memory_ta.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent evt) {
        if (evt.isPopupTrigger()) { popup_menu.show(memory_ta, evt.getX(), evt.getY()); }
      }
      public void mouseReleased(MouseEvent evt) {
        if (evt.isPopupTrigger()) { popup_menu.show(memory_ta, evt.getX(), evt.getY()); }
      }
    });
        
    ActionListener al = new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        updateMemory();
      }
    };
    javax.swing.Timer timer = new javax.swing.Timer(timer_delay_ms, al);
    timer.setInitialDelay(0);
    timer.start();
  }
    
  /** Sets the String in the status bar.
   *  HTML can be used if prefixed with "<html>".
   *  Can be safely called from any thread.
   *  @param s  a String, null is ok; null will erase the status String.
   */
  public void setStatus(String s) {
    if (s == null) { s = ""; }
    
    updateSafely(status_ta, s);
    updateMemory();
  }
  
  /** Sets the String in the position status bar.
   *  HTML can be used if prefixed with "<html>".
   *  Can be safely called from any thread.
   *  @param s  a String, null is ok; null will erase the String.
   */
  public void setPosition(String s) {
    if (s == null) { s = ""; }
    
    updateSafely(position_ta, s);
  }
  
  /**
   *  Causes the memory indicator to update its value.  Normally you do not
   *  need to call this method as the memory value will be updated from
   *  time to time automatically. 
   */
  public void updateMemory() {
    Runtime rt = Runtime.getRuntime();
    long memory  = rt.totalMemory() - rt.freeMemory();
    
    double mb = 1.0 * memory / (1024 * 1024);
    String text = num_format.format(mb) + " MB";

    long max_memory = rt.maxMemory();
    if (max_memory != Long.MAX_VALUE) {
      double max = 1.0 * rt.maxMemory() / (1024 * 1024);
      text += " / " + num_format.format(max) + " MB";
    
      //double percentage = 100.0d * memory / rt.maxMemory();    
      //text += "; "+num_format.format(percentage) + "%";
    
      /*
      if (percentage > 90.0d) {
         text = "<html><font color=\"red\">" + text + "</font></html>";
      }
      */
    }
    updateSafely(memory_ta, text);    
  }
  
  /**
   *  Update a JLabel in a way that is safe from either the GUI thread or
   *  any other thread.
   */
  void updateSafely(final JLabel label, final String text) {
    if (SwingUtilities.isEventDispatchThread()) {
      label.setText(text);
    } else {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            label.setText(text);
          }
        }
      );
    }
  }
}
