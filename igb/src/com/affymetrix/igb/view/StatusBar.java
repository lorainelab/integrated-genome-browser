package com.affymetrix.igb.view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.text.DecimalFormat;

public class StatusBar extends JPanel {
  
  JLabel status_ta;
  JLabel memory_ta;
  
  DecimalFormat num_format;
  
  /** Delay in milliseconds between updates of the status (such as memory usage).  */
  static int timer_delay_ms = 5000;
  
  public StatusBar() { 
    status_ta = new JLabel("");
    memory_ta = new JLabel("");
    status_ta.setBorder(new BevelBorder(BevelBorder.LOWERED));
    memory_ta.setBorder(new BevelBorder(BevelBorder.LOWERED));
    //num_format = NumberFormat.getIntegerInstance();
    num_format = new DecimalFormat();
    num_format.setMaximumFractionDigits(1);
    num_format.setMinimumFractionDigits(1);
    
    BorderLayout bl = new BorderLayout();
    setLayout(bl);
    
    this.add(status_ta, BorderLayout.CENTER);
    this.add(memory_ta, BorderLayout.EAST);
        
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
   *  @param s  a String, null is ok; null will erase the status String.
   */
  public void setStatus(String s) {
    if (s == null) { s = ""; }
    
    updateSafely(status_ta, s);
    updateMemory();
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
