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

package com.affymetrix.igb;

import java.awt.Color;
import java.awt.Graphics;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Checks to see if IGB is running.
 * Perhaps we should only allow localhost?
 */
public class IGBChecker extends java.applet.Applet {

  private static final int BAD_REQUEST = 400;
  private static final int BAD_GATEWAY = 502;
  private static final int SERVICE_UNAVAILABLE = 503;
  private static final String DEFAULT_LOCAL_IGB = "http://localhost:7085/Test.html";
  private static final String DEFAULT_IGB_JNLP = "igb.jnlp";
  private static int UNKNOWN = 0;
  private static int IGB_UP = 1;
  private int igbStatus = SERVICE_UNAVAILABLE;

  private String igbURL;

  public IGBChecker() {
    this.igbStatus = UNKNOWN;
  }

  public void init() {
    this.igbURL = getParameter( "igbURL" );
    if ( null == this.igbURL ) {
      this.igbURL = DEFAULT_LOCAL_IGB;
    }
    this.setBackground( Color.white );
  }

  /**
   * Check IGB every time the page is shown.
   * Perhaps this should be done on every paint?
   */
  public void start() {
    this.igbStatus = getStatusCode();
  }

  public void paint(Graphics g) {
    g.setColor( Color.black );
    //g.drawString( "IGB Status Check", 5, 15 );
    g.drawString( this.igbURL, 5, 15 );
    
    if ( this.igbStatus < 400 ) {
      g.setColor( Color.green );
      g.drawString( "IGB is running. " + this.igbStatus, 5, 30 );
    }
    else {
      g.setColor( Color.red );
      g.drawString( "IGB is NOT running! " + this.igbStatus, 5, 30 );
    }
  }

  /**
   * @return true iff IGB is up and accepting connections.
   */
  public boolean isAvailable() {
    return isAvailableAt( this.igbURL );
  }

  /**
   * @param theLocation where IGB is suspected to be running.
   * @return true iff IGB is up and accepting connections at the given location.
   */
  public boolean isAvailableAt( String theLocation ) {
    int status = getStatusCode( theLocation );
    return ( status < 400 );
  }

  /**
   * Try to get a response from IGB.
   * @return http response code from IGB if IGB is up and accepting connections.
   * An error code (&gt;= 400) otherwise.
   */
  public int getStatusCode() {
    return getStatusCode( this.igbURL );
  }

  /**
   * Try to get a response from IGB.
   * @param theURL locating an IGB resource.
   * @return http response code from IGB if IGB is up and accepting connections.
   * An error code (&gt;= 400) otherwise.
   */
  protected int getStatusCode( String theURL ) {

    System.out.println( "IGBChecker.getStatusCode:" );
    System.out.println( "Trying to contact IGB at " + theURL );
    try {
      URL igb_url = new URL( theURL );
      HttpURLConnection url_conn = ( HttpURLConnection ) igb_url.openConnection();
      this.igbStatus = url_conn.getResponseCode();
    }
    catch( java.net.MalformedURLException mue ) {
      this.igbStatus = BAD_REQUEST;
      System.err.println( mue.getMessage() );
    }
    catch( java.net.ConnectException ce ) {
      this.igbStatus = BAD_GATEWAY;
    }
    catch( java.io.IOException ioe ) {
      this.igbStatus = BAD_GATEWAY;
      ioe.printStackTrace();
    }
    catch( java.security.AccessControlException ace ) {
      this.igbStatus = BAD_GATEWAY;
      ace.printStackTrace();
    }
    System.out.println( "Response: " + this.igbStatus );
    return  this.igbStatus;
  }

  public String getAppletInfo() {
    return "IGBChecker 1.0\n" +
           "Affymetrix Inc.";
  }

  private static final String[][] paramInfo = {
    {"igbURL", "URL", "where IGB might be"},
  };
  public String[][] getParameterInfo() {
    return paramInfo;
  }

}
