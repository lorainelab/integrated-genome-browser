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

import java.applet.*;
import java.awt.*;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.awt.NeoScrollbar;
import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.bioviews.GlyphI;

public class TutorialGlyphDemo extends Applet {

  public void init() {
    NeoMap map = new NeoMap(true, false);
    map.setSelectionEvent(map.ON_MOUSE_DOWN);
    map.setMapRange(0, 10000);
    map.addAxis(30);
    GlyphI tglyph;

    tglyph = new RoundedRect();
    tglyph.setCoords(5000, 50, 1000, 30);
    tglyph.setColor(Color.green);
    map.addItem(tglyph);

    tglyph = new RoundedRect();
    tglyph.setCoords(3000, 80, 1000, 15);
    tglyph.setColor(Color.blue);
    map.addItem(tglyph);

    NeoScrollbar xzoomer = new NeoScrollbar(NeoScrollbar.VERTICAL);
    map.setZoomer(map.X, xzoomer);

    NeoPanel pan = new NeoPanel();
    pan.setLayout(new BorderLayout());
    pan.add("Center", map);
    pan.add("West", xzoomer);
    this.setLayout(new BorderLayout());
    this.add("Center", pan);

  }

}
