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

package com.affymetrix.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Six Way Split Pane for IGB.
 * @author  Eric Blossom
 */
public class SixWaySplitPane extends JComponent {
  
  private Component[] interiorPanel = new Component[6];
  
  private final JSplitPane westernRegion = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
  private final JSplitPane centralRegion = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
  private final JSplitPane easternRegion = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
  private final JSplitPane vSplitOne = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, westernRegion, centralRegion );
  private final JSplitPane vSplitTwo = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, vSplitOne, easternRegion );
  
  /**
   * Sycronized multiple split panes.
   */
  private class SyncroSizer extends ComponentAdapter {
    private Set peers = new HashSet();
    public void add( JSplitPane thePane ) {
      this.peers.add( thePane );
      thePane.setContinuousLayout( true );
    }
    public void componentResized( ComponentEvent ce ) {
      Component src = ( Component ) ce.getSource();
      JSplitPane srcPane = ( JSplitPane ) src.getParent();
      Iterator it = this.peers.iterator();
      while ( it.hasNext() ) {
        Object o = it.next();
        JSplitPane jsp = ( JSplitPane ) o;
        if ( jsp != src ) {
          src.removeComponentListener( this );
          jsp.setDividerLocation( srcPane.getDividerLocation() );
          src.addComponentListener( this );
        }
      }
    }
  }
  private SyncroSizer syncroSizer = new SyncroSizer();
  
  /** Creates a new instance of SixWaySplitPane */
  public SixWaySplitPane() {
    setLayout( new BorderLayout() );
    this.add( vSplitTwo, BorderLayout.CENTER );
    this.syncroSizer.add( this.westernRegion );
    this.syncroSizer.add( this.centralRegion );
    this.syncroSizer.add( this.easternRegion );
    this.westernRegion.setBorder( null );
    int w = this.westernRegion.getDividerSize() - 2;
    this.westernRegion.setDividerSize( w );
    this.centralRegion.setBorder( null );
    this.centralRegion.setDividerSize( w );
    this.easternRegion.setBorder( null );
    this.easternRegion.setDividerSize( w );
    this.vSplitOne.setBorder( null );
    this.vSplitOne.setDividerSize( w );
    //this.vSplitTwo.setBorder( null ); // Still want border around the whole panel.
    this.vSplitTwo.setDividerSize( w );
  }
  
public void addNorthWest( Component theCandidate ) {
    assert null == this.interiorPanel[0];
    this.interiorPanel[0] = theCandidate;
    this.westernRegion.setTopComponent( this.interiorPanel[0] );
    theCandidate.addComponentListener( this.syncroSizer );
    assert null != this.interiorPanel[0];
  }
  public void addNorth( Component theCandidate ) {
    assert null == this.interiorPanel[1];
    this.interiorPanel[1] = theCandidate;
    this.centralRegion.setTopComponent( this.interiorPanel[1] );
    theCandidate.addComponentListener( this.syncroSizer );
    assert null != this.interiorPanel[1];
  }
public void addNorthEast( Component theCandidate ) {
    assert null == this.interiorPanel[2];
    this.interiorPanel[2] = theCandidate;
    this.easternRegion.setTopComponent( this.interiorPanel[2] );
    theCandidate.addComponentListener( this.syncroSizer );
    assert null != this.interiorPanel[2];
  }
public void addWest( Component theCandidate ) {
    assert null == this.interiorPanel[3];
    this.interiorPanel[3] = theCandidate;
    this.westernRegion.setBottomComponent( this.interiorPanel[3] );
    assert null != this.interiorPanel[3];
  }
public void addCenter( Component theCandidate ) {
    assert null == this.interiorPanel[4];
    this.interiorPanel[4] = theCandidate;
    this.centralRegion.setBottomComponent( this.interiorPanel[4] );
    assert null != this.interiorPanel[4];
  }
public void addEast( Component theCandidate ) {
    assert null == this.interiorPanel[5];
    this.interiorPanel[5] = theCandidate;
    this.easternRegion.setBottomComponent( this.interiorPanel[5] );
    assert null != this.interiorPanel[5];
}
  
  /**
   * Test the new split pane.
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    JFrame f = new JFrame( "Test of SixWaySplitPane" );
    SixWaySplitPane c = new SixWaySplitPane();
    JComponent b = new JButton( "Center" );
    b.setMinimumSize( new Dimension( 20, 20 ) );
    c.addCenter( b );
    b = new JButton( "North" );
    b.setMinimumSize( new Dimension( 10, 20 ) );
    c.addNorth( b );
    b = new JButton( "West" );
    b.setMinimumSize( new Dimension( 20, 10 ) );
    c.addWest( b );
    c.addNorthWest( new JButton( "NW" ) );
    c.addEast( new JButton( "East" ) );
    c.addNorthEast( new JButton( "NE" ) );
    Container content = f.getContentPane();
    content.setLayout( new BorderLayout() );
    content.add( c, BorderLayout.CENTER );
    f.setBounds( 20, 40, 400, 300 );
    f.setVisible( true );
    f.setDefaultCloseOperation( f.DISPOSE_ON_CLOSE );
    f.addWindowListener( new WindowAdapter() {
      public void windowClosed( WindowEvent e ) {
        System.exit( 0 );
      }
    } );
  }

}
