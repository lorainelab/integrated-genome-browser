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
import java.io.*;
import javax.swing.*;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.menuitem.MenuUtil;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.swing.DisplayUtils;

public final class ConsoleView {
  
  static String TITLE;
  
  static JFrame frame;
    
  protected ConsoleView() {
    TITLE = Application.getSingleton().getApplicationName() + " Console";
  }
  
  /**
   *  Call this to create and initialize the singleton JFrame and
   *  to start the redirection of standard out and err streams into it.
   *  Call {@link #showConsole()} or {@link #getFrame()} when you are
   *  ready to display the frame.
   */
  public static void init() {
    getFrame();
  }
  
   /**
   *  Displays the console and brings it to the front.
   *  If necessary, it will be de-iconified.
   *  This will call {@link #init()} if necessary, but
   *  it is better for you to call init() at the time you want
   *  the console to begin working. 
   */
  public static void showConsole() {
    if (frame == null) {
      init();
    }
    frame.doLayout();
    frame.repaint();
    
    DisplayUtils.bringFrameToFront(frame);
  }
  
  /**
   *  Returns the JFrame that holds the console, creating it if necessary,
   *  but not displaying it if it isn't already displayed.
   *  If you want to display the frame, call {@link #showConsole()} instead.
   */
  public static JFrame getFrame() {
    if (frame == null) {
      frame = createFrame();
      Container cpane = frame.getContentPane();
      cpane.setLayout(new BorderLayout());

      JScrollPane outPane = createOutPane();

      cpane.add(outPane, BorderLayout.CENTER);
      frame.pack(); // set to default size based on contents

      // then try to get size from preferences
      Rectangle pos = UnibrowPrefsUtil.retrieveWindowLocation(TITLE, frame.getBounds());
      if (pos != null) {
        UnibrowPrefsUtil.setWindowSize(frame, pos);
      }
    }
    return frame;
  }
  
  static JScrollPane createOutPane() {
    // if it is ever necessary to make a public getOutPane() method,
    // we'll need to make sure that createOutPane() is only called once.

    JTextArea outArea = new JTextArea(20, 50);
    outArea.setEditable(false);
    
    JScrollPane outPane = new JScrollPane(outArea,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        
    try {
      // Send err to same text area as out
      // (But we could send err to a separate text area.)
      System.setOut(new PrintStream(new JTextAreaOutputStream(outArea, System.out)));
      System.setErr(new PrintStream(new JTextAreaOutputStream(outArea, System.err)));
      
    } catch (SecurityException se) {
      // This exception should not occur with WebStart, but I'm handling it anyway.
      
      String str = "The application may not have permission to re-direct output "
       + "to this view on your system.  "
       + "\n"
       + "You should be able to view output in the Java console, WebStart console, "
       + "or wherever you normally would view program output.  "
       + "\n\n";
      outArea.append(str);
    }

    return outPane;
  }
 
  /**
   *  Creates a JFrame to hold the console.
   */
  static JFrame createFrame() {
    final JFrame frame = new JFrame(TITLE);

    ImageIcon icon = MenuUtil.getIcon("toolbarButtonGraphics/development/Host16.gif");
    if (icon != null) { frame.setIconImage(icon.getImage()); }

    frame.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        UnibrowPrefsUtil.saveWindowLocation(frame, TITLE);
      }
    });
    
    return frame;
  }


  /**
   *  A class to help send a PrintStream, such as System.out,
   *  to a  JTextArea.  Adapted from code in the book "Swing Hacks"
   *  by Joshua Marinacci and Chris Adamson.  This is from hack #95.
   *  This sort of use of the code is allowed (even without attribution).
   *  See the preface of their book for details.
   */
  public static class JTextAreaOutputStream extends OutputStream {
    JTextArea ta;
    PrintStream original;
    char[] temp_char_array = new char[1];
    
    /**
     *  Creates an OutputStream that writes to the given JTextArea.
     *  @param echo  Can be null, or a PrintStream to which a copy of all output
     *    will also by written.  Thus you can send System.out to a text area
     *    and also still send an echo to the original System.out.
     */
    public JTextAreaOutputStream(JTextArea t, PrintStream echo) {
      this.ta = t;
      this.original = echo;
    }
    
    public void write(int i) {
      temp_char_array[0] = (char) i;
      String s = new String(temp_char_array);
      ta.append(s);
      if (original != null) {original.write(i);}
    }

    public void write(char[] buf, int off, int len) {
      String s = new String(buf, off, len);
      ta.append(s);
      if (original != null) {original.print(s);}
    }
  }
  
}
