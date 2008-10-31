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

package com.affymetrix.igb.tiers;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import com.affymetrix.genoviz.awt.*;
import com.affymetrix.genoviz.widget.*;


public class TestSplitter {

  public static void main(String[] args) {
    TestSplitter test = new TestSplitter();
    test.doTest();
  }

  public void doTest() {
    JFrame frm = new JFrame("SplitterTest");
    Container cpane = frm.getContentPane();
    Component testcomp = null;
    testcomp = new NeoCanvas();
    System.out.println(testcomp);

    testcomp.setBackground(Color.pink);

    JSplitPane splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitpane.setLeftComponent(new JButton("test button"));
    splitpane.setRightComponent(testcomp);
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", splitpane);
    frm.setSize(600, 400);
    frm.show();
    frm.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
	System.exit(0);
      } 
    } );

    //    testcomp.addMouseListener(this);
  

    testcomp.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent evt) {
	System.out.println(evt.getSource());
      }
    } );

  }


}
