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

import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.widget.NeoMap;
import java.awt.*;

public class NeoMapPanel extends NeoPanel {

  Adjustable zoomer, scroller;
  NeoMap map = null;
  GridBagLayout gbl = new GridBagLayout();

  GridBagConstraints mapConstraints = new GridBagConstraints(); {
    this.mapConstraints.fill = GridBagConstraints.BOTH;
    this.mapConstraints.gridx = 1; this.mapConstraints.gridy = 0;
    this.mapConstraints.gridwidth = 1; this.mapConstraints.gridheight = 1;
    this.mapConstraints.weightx = 1.0; this.mapConstraints.weighty = 1.0;
  }

  GridBagConstraints zoomerConstraints = new GridBagConstraints(); {
    this.zoomerConstraints.fill = GridBagConstraints.BOTH;
    this.zoomerConstraints.gridx = 0; this.zoomerConstraints.gridy = 0;
    this.zoomerConstraints.gridwidth = 1; this.zoomerConstraints.gridheight = 1;
    this.zoomerConstraints.weightx = 0.0; this.zoomerConstraints.weighty = 1.0;
  }

  GridBagConstraints scrollerConstraints = new GridBagConstraints(); {
    this.scrollerConstraints.fill = GridBagConstraints.BOTH;
    this.scrollerConstraints.gridx = 1; this.scrollerConstraints.gridy = 1;
    this.scrollerConstraints.gridwidth = 1; this.scrollerConstraints.gridheight = 1;
    this.scrollerConstraints.weightx = 1.0; this.scrollerConstraints.weighty = 0.0;
  }

  public NeoMapPanel() {
    super();
    setLayout( gbl );
  }

  public Component add( Component c ) {
    if ( c instanceof Adjustable ) {
      Adjustable a = ( Adjustable ) c;
      switch ( a.getOrientation() ) {
      case Adjustable.HORIZONTAL:
        this.scroller = a;
        this.gbl.setConstraints( (Component) this.scroller, this.scrollerConstraints );
        if ( null != this.map ) this.map.setRangeScroller( this.scroller );
        break;
      case Adjustable.VERTICAL:
        this.zoomer = a;
        this.gbl.setConstraints( (Component) this.zoomer, this.zoomerConstraints );
        if ( null != this.map ) this.map.setRangeZoomer( this.zoomer );
        break;
      }
    }
    else if ( c instanceof NeoMap ) {
      this.map = ( NeoMap ) c;
      this.gbl.setConstraints( (Component) this.map, this.mapConstraints );
    }
    return super.add( c );
  }

}
