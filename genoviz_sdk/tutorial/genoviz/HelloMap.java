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
import com.affymetrix.genoviz.glyph.StringGlyph;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class HelloMap extends Applet {
  NeoMap map;

  public HelloMap() {
    map = new NeoMap();

    map.setMapRange( 1, 100 );
    map.setMapOffset( 0, 50 );
    map.addAxis( 10 );
    map.configure( "-offset 25 -color black " 
                   + "-glyphtype com.affymetrix.genoviz.glyph.StringGlyph -width 9" );

    StringGlyph sg = ( StringGlyph ) map.addItem( 45, 54 );

    sg.setString( "Hello Map" );
    setLayout( new BorderLayout() );

    NeoPanel pan = new NeoPanel();

    pan.setLayout( new BorderLayout() );
    pan.add( "Center", map );
    add( "Center", pan );
  }

  public String getAppletInfo() {
    return ( "Hello Map Demo - genoviz Software, Inc." );
  }

  public static void main( String argv[] ) {
    Frame f = new Frame( "GenoViz" );

    f.add( "Center", new HelloMap() );
    f.addWindowListener( new WindowAdapter() {
      public void windowClosing( WindowEvent e ) {
        Window w = ( Window ) e.getSource();

        w.dispose();
      }

      public void windowClosed( WindowEvent e ) {
        System.exit( 0 );
      }

    } );
    f.pack();
    f.setBounds( 20, 40, 300, 250 );
    f.show();
  }

}
