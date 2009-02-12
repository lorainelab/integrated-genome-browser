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

package genoviz.tutorial;

import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.widget.*;
import java.awt.*;
import java.awt.event.*;

public class HairMap extends SimpleMap3 {
	private VisibleRange zoomPoint = new VisibleRange();

	public HairMap() {
		NeoRangeListener zoomAdjuster = new NeoRangeListener() {
			public void rangeChanged( NeoRangeEvent e ) {
				double midPoint = ( e.getVisibleEnd() + e.getVisibleStart() ) / 2.0f;

				map.setZoomBehavior( map.X, map.CONSTRAIN_COORD, midPoint );
				map.updateWidget();
			}

		};

		this.zoomPoint.addListener( zoomAdjuster );
	}

	public void init() {
		super.init();

		Shadow hairline = new Shadow( this.map );

		hairline.setSelectable( false );

		MouseListener zoomPointAdjuster = new MouseAdapter() {
			public void mouseReleased( MouseEvent e ) {
				double focus = ( ( NeoMouseEvent ) e ).getCoordX();

				zoomPoint.setSpot( focus );
			}

		};

		this.map.addMouseListener( zoomPointAdjuster );
		this.zoomPoint.addListener( hairline );
	}

	public static void main( String argv[] ) {
		SimpleMap0 me = new HairMap();
		Frame f = new Frame( "GenoViz" );

		f.add( me, BorderLayout.CENTER );
		me.addFileMenuItems( f );
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
		f.setBounds( 20, 40, 400, 500 );
		f.show();
	}

}
