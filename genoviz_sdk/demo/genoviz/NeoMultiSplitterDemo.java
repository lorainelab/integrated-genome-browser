/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package demo.genoviz;

import java.awt.*;
import java.applet.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.affymetrix.genoviz.awt.NeoMultiSplitter;
import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.glyph.StringGlyph;
import com.affymetrix.genoviz.widget.NeoMap;

public class NeoMultiSplitterDemo extends Applet {

  private NeoMap map1, map2, map3, map4;
  Frame mainframe = new Frame();
  Image backgroundImage = null;

  public NeoMultiSplitterDemo() {
    mainframe.setTitle("NeoMultiSplitterDemo");
    mainframe.addWindowListener(new WindowAdapter(){
      public void windowClosing(WindowEvent e) {
        mainframe.setVisible( false );
      }
    } );

    map1 = setUpMap();
    StringGlyph sg1 = new StringGlyph();
    sg1.setString("NeoMap 1");
    sg1.setCoords(20,20,40,10);
    map1.addItem(sg1);

    map2 = setUpMap();
    StringGlyph sg2 = new StringGlyph();
    sg2.setString("NeoMap 2");
    sg2.setCoords(20,20,40,10);
    map2.addItem(sg2);

    map3 = setUpMap();
    StringGlyph sg3 = new StringGlyph();
    sg3.setString("NeoMap 3");
    sg3.setCoords(20,20,40,10);
    map3.addItem(sg3);

    map4 = setUpMap();
    StringGlyph sg4 = new StringGlyph();
    sg4.setString("NeoMap 4");
    sg4.setCoords(20,20,40,10);
    map4.addItem(sg4);

    NeoMultiSplitter wholePanel = new NeoMultiSplitter(NeoMultiSplitter.VERTICAL);

    wholePanel.add("1",map1);

    // One difference between AWT's SplitPane and a NeoMultiSplitter
    // is that the NeoMultiSplitter can contain (split) more than two panels.
    Panel rightSide;
    rightSide = new NeoPanel();                             // To compare with an ordinary GridLayout,
    rightSide.setLayout( new java.awt.GridLayout( 2, 0 ) ); // Comment out the next line.
    rightSide.add( "2a", map2 );
    rightSide.add( "2b", map3 );

    wholePanel.add( "2", rightSide );

    wholePanel.setDividerSize(10); // Divider size could be changed

    mainframe.add(wholePanel);
    mainframe.setBounds(200,150,600,500);
    mainframe.show();
  }

  public void init() {
    String param;

    param = getParameter("background");
    if (null != param) {
      backgroundImage = this.getImage(this.getDocumentBase(), param);
    }

    if (null == backgroundImage) {
      Label placeholder =
        new Label("Running genoviz NeoMap Demo", Label.CENTER);
      this.setLayout(new BorderLayout());
      this.add("Center", placeholder);
      placeholder.setBackground(Color.black);
    }
  }

  public void start() {
    mainframe.setVisible( true );
  }

  public void stop() {
    mainframe.setVisible( false );
  }

  public NeoMap setUpMap() {
    NeoMap map = new NeoMap();
    map.setMapOffset(-100,100);
    map.setMapRange(0,100);
    map.addAxis(0);
    //The background color influences the color of the transient splitter bar.
    //Uncomment the following line to try changing the map's background color
    //map.setBackground(Color.cyan);
    return map;
  }

  public void paint(Graphics g) {
    if (null == this.backgroundImage) {
      super.paint(g);
    }
    else {
      g.drawImage(this.backgroundImage, 0, 0, this.getSize().width, this.getSize().height, this);
    }
  }

}
