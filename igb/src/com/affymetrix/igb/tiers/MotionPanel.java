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

import com.affymetrix.genoviz.widget.NeoWidgetI;
import com.affymetrix.genoviz.awt.AdjustableJSlider;
import com.affymetrix.genoviz.awt.NeoScrollbar;
import javax.swing.*;
import java.awt.Dimension;

/**
 *
 * @author  eric
 */
public class MotionPanel extends JPanel {
  
  /** Creates a new panel
   * with zooming and panning along the same axis.
   * @param theOrientation {@link SwingConstants#HORIZONTAL} or {@link SwingConstants#VERTICAL} 
   * @param thePanAxis {@link NeoWidgetI#PRIMARY_AXIS}, {@link NeoWidgetI#SECONDARY_AXIS}, or null.
   * If null then puts in two shims instead of scrollbar and slider.
   */
  public MotionPanel( int theOrientation, int thePanAxis ) {
    this( theOrientation, thePanAxis, thePanAxis );
  }
  
  /** Creates a new panel.
   * @param theOrientation {@link SwingConstants#HORIZONTAL} or {@link SwingConstants#VERTICAL} 
   * @param thePanAxis {@link NeoWidgetI#PRIMARY_AXIS}, {@link NeoWidgetI#SECONDARY_AXIS}, or -1.
   * If null then puts a shim in instead of a scrollbar.
   * @param theZoomAxis {@link NeoWidgetI#PRIMARY_AXIS}, {@link NeoWidgetI#SECONDARY_AXIS}, or -1.
   * If -1 then puts a shim in instead of a slider.
   */
  public MotionPanel( int theOrientation, int thePanAxis, int theZoomAxis ) {
    super();
    setOrientation( theOrientation );
    setPanAxis( thePanAxis );
    setZoomAxis( theZoomAxis );
  }

  private int orientation;
  private void setOrientation( int theOrientation ) {
    switch ( theOrientation ) {
      case SwingConstants.HORIZONTAL:
        setLayout( new javax.swing.BoxLayout( this, BoxLayout.Y_AXIS ) );
        break;
      case SwingConstants.VERTICAL:
        setLayout( new javax.swing.BoxLayout( this, BoxLayout.X_AXIS ) );
        break;
      default:
        throw new IllegalArgumentException( "Orientation must be HORIZONTAL or VERTICAL. Found " + theOrientation );
    }
    this.orientation = theOrientation;
  }
  
  class Shim extends JComponent implements Cloneable {
    private Dimension prefSize = new Dimension( 16, 16 ); // Same as NeoScrollbar.
    private Dimension minSize = new Dimension( 1, 1 );
    public Dimension getPreferredSize() {
      return this.prefSize;
    }
    public Dimension getMinimumSize() {
      return this.minSize;
    }
  }

  private NeoScrollbar pScroller;
  private NeoScrollbar sScroller;
  private void setPanAxis( int theAxis ) {
    switch ( theAxis ) {
      case NeoWidgetI.PRIMARY_AXIS:
        this.pScroller = new NeoScrollbar( this.orientation );
        add( this.pScroller );
        break;
      case NeoWidgetI.SECONDARY_AXIS:
        this.sScroller = new NeoScrollbar( this.orientation );
        add( this.sScroller );
        break;
      case -1:
        add( new Shim() );
        break;
      default:
        throw new IllegalArgumentException( "Axis must be NeoWidgetI.PRIMARY_AXIS or NeoWidgetI.SECONDARY_AXIS. Found " + theAxis );
    }
  }
  private AdjustableJSlider pZoomer;
  private AdjustableJSlider sZoomer;
  private void setZoomAxis( int theAxis ) {
    switch ( theAxis ) {
      case NeoWidgetI.PRIMARY_AXIS:
        this.pZoomer = new AdjustableJSlider( this.orientation );
        add( this.pZoomer );
        break;
      case NeoWidgetI.SECONDARY_AXIS:
        this.sZoomer = new AdjustableJSlider( this.orientation );
        add( this.sZoomer );
        break;
      case -1:
        add( new Shim() );
        break;
      default:
        throw new IllegalArgumentException( "Axis must be NeoWidgetI.PRIMARY_AXIS or NeoWidgetI.SECONDARY_AXIS. Found " + theAxis );
    }
  }

  public AdjustableJSlider getZoomer( int theAxis ) {
    switch ( theAxis ) {
      case NeoWidgetI.PRIMARY_AXIS:
        return this.pZoomer;
      case NeoWidgetI.SECONDARY_AXIS:
        return this.sZoomer;
    }
    return null;
  }

  public void setZoomer( AdjustableJSlider theZoomer ) {
    if ( theZoomer.getOrientation() != this.orientation ) {
      throw new IllegalArgumentException ( "Orientation does not match." );
    }
    this.remove( 1 );
    add( theZoomer );
  }

  public NeoScrollbar getPanner( int theAxis ) {
    switch ( theAxis ) {
      case NeoWidgetI.PRIMARY_AXIS:
        return this.pScroller;
      case NeoWidgetI.SECONDARY_AXIS:
        return this.sScroller;
    }
    return null;
  }

  /** for testing only */
  public static void main( String[] argv ) {
    JFrame f = new JFrame( "Motion Panel" );
    java.awt.Container c = f.getContentPane();
    c.add( new MotionPanel( SwingConstants.HORIZONTAL, NeoWidgetI.PRIMARY_AXIS ) );
    f.setDefaultCloseOperation( f.EXIT_ON_CLOSE );
    f.setSize( 300, 100 );
    f.show();
  }
}
