package com.affymetrix.igb.view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.text.NumberFormat;

public class StatusBar extends JPanel {
  
  JLabel status_ta;
  JLabel memory_ta;
  
  NumberFormat num_format;
  
  /** Delay in milliseconds between updates of the status (such as memory usage).  */
  static int timer_delay_ms = 5000;
  
  public StatusBar() { 
    status_ta = new JLabel("");
    memory_ta = new JLabel("");
    status_ta.setBorder(new BevelBorder(BevelBorder.LOWERED));
    memory_ta.setBorder(new BevelBorder(BevelBorder.LOWERED));
    num_format = NumberFormat.getIntegerInstance();
    
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
    status_ta.setText(s);
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
    memory /= (1024 * 1024); // to get in MB
    memory_ta.setText(num_format.format(memory) + " MB");
  }
}
