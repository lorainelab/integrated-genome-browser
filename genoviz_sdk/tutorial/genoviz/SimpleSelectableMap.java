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

package tutorial.genoviz;

import com.affymetrix.genoviz.bioviews.SceneI;
import com.affymetrix.genoviz.event.NeoRubberBandListener;
import com.affymetrix.genoviz.event.NeoRubberBandEvent;

import java.awt.Event;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.Vector;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class SimpleSelectableMap extends SimpleMap0
  implements NeoRubberBandListener {

  protected boolean isApplet = true;

  public SimpleSelectableMap() {
    super();
    this.map.setSelectionEvent(this.map.ON_MOUSE_DOWN);
    this.map.setSelectionAppearance(SceneI.SELECT_OUTLINE);
    this.map.addRubberBandListener(this);
    this.map.addMouseListener(ml);
  }

  protected void report(String s) {
    if (isApplet) {
      showStatus(s);
    }
    else {
      System.out.println(s);
    }
  }

  protected MouseListener ml = new MouseAdapter() {
    public void mouseClicked(MouseEvent e) {
      report(e.toString());
    }
    public void mouseEntered(MouseEvent e) {
      report(e.toString());
    }
    public void mouseExited(MouseEvent e) {
      report(e.toString());
    }
    public void mousePressed(MouseEvent e) {
      report(e.toString());
    }
    public void mouseReleased(MouseEvent e) {
      report(e.toString());
    }
  };

  public String getAppletInfo() {
    return "Simple Selectable Map - genoviz Software, Inc.";
  }



  public void rubberBandChanged(NeoRubberBandEvent theEvent) {
    // Here we add some selection by rubberband.
    if (theEvent.getID() == NeoRubberBandEvent.BAND_END
      && theEvent.getSource() == map
      && map.NO_SELECTION != map.getSelectionEvent())
    {
      Rectangle pixelBox = theEvent.getPixelBox();
      pixelBox.setSize(pixelBox.width+1, pixelBox.height+1);
      int fuzziness = map.getPixelFuzziness();
      if (fuzziness <= pixelBox.height || fuzziness <= pixelBox.width) {
        // Rubberband is non-trivial.
        // Select items within it.
        Vector items = map.getItems(pixelBox);
        map.select(items);
        map.updateWidget();
      }
    }
  }

  public static void main (String argv[]) {
    SimpleSelectableMap me = new SimpleSelectableMap();
    me.isApplet = false;
    Frame f = new Frame("GenoViz");
    f.add("Center", me);

    f.addWindowListener( new WindowAdapter() {
      public void windowClosing( WindowEvent e ) {
        Window w = (Window) e.getSource();
        w.dispose();
      }
      public void windowClosed( WindowEvent e ) {
        System.exit( 0 );
      }
    } );
    me.addFileMenuItems(f);
    f.pack();
    f.setBounds(20, 40, 400, 500);
    f.show();
  }

}
