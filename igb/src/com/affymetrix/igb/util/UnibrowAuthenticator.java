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

package com.affymetrix.igb.util;

import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.igb.general.ServerList;

public final class UnibrowAuthenticator extends Authenticator  {
  JFrame frm;
  JTextField userTF = new JTextField();
  JPasswordField passwordTF = new JPasswordField();
  JPanel message_panel = new JPanel();
  JLabel promptL = new JLabel();
  JLabel urlL = new JLabel();
  String login = "";
  String password = "";
  String serverName = "";
  int attempts = 0;


  public UnibrowAuthenticator() {
    this(null);
  }

  public UnibrowAuthenticator(JFrame jf) {
    frm = jf;
    message_panel.setLayout(new GridLayout(4, 2));
    message_panel.add(new JLabel("Server"));
    message_panel.add(promptL);
    message_panel.add(new JLabel("URL"));
    message_panel.add(urlL);
    message_panel.add(new JLabel("User Name" ));
    message_panel.add(userTF);
    message_panel.add(new JLabel("Password"));
    message_panel.add(passwordTF);
  }

  protected PasswordAuthentication getPasswordAuthentication()  {

	// Don't try to authenticate when the url is for the
	// friendly icon
	if (this.getRequestingURL().toString().endsWith(".ico")) {
		return null;
	}
	
	promptL.setText(this.getRequestingPrompt());
	urlL.setText(this.getRequestingURL().toString());
	String url = this.getRequestingURL().toString();
	
	// Use the URL to find the server info, which will
	// keep track of login attempts, provide information
	// about the server name, and which may provide a 
	// default login and password.
	GenericServer server = getServer(url);
	if (server != null && server.serverObj != null) {
		login = server.login;
		password = server.password;
		serverName = server.serverName;
		if (serverName != null && !serverName.equals("")) {
			promptL.setText(serverName);
		}
	}
		
	// If user has tried to login more than 3 times, just
	// exit.  This server will be bypassed.
	if (server!= null) {
		server.loginAttempts++;
		if (server.loginAttempts > 3) {
			return null;
		}
	}
	
	// If the login and password were provided in preferences,
	// use that to login.  (Try once, then just show dialog
	// if login attempt was not successful.)
	PasswordAuthentication auth = null;
	if (server != null && server.loginAttempts == 1 && 
        login != null && !login.equals("") && password != null && !password.equals("")) {
    	auth = new PasswordAuthentication(this.login,  new String(this.password).toCharArray());    		
	} else if (server != null && server.loginAttempts == 1) {
	  // The login and password were not provided in preferences,
	  // so just login as guest
	  auth = new PasswordAuthentication("guest", new String("guest").toCharArray());
	}
	  else {
		// IGB couldn't login with the supplied login and password or the guest login
	  // and password, so present the login dialog for the user to
		// type in the login and password.
	    int result = JOptionPane.showOptionDialog(frm, message_panel, "Login", 
						      JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, 
						      null, null, null );

	    
	    // If user pressed OK, return a PasswordAuthentication based on the supplied 
	    // user name and password
	    if (result == JOptionPane.OK_OPTION) {
	      String user_name = userTF.getText();
	      char[] user_password = passwordTF.getPassword();
	      auth = new PasswordAuthentication(user_name, user_password);
	    } else if (result == JOptionPane.CANCEL_OPTION) {
	      // Bump up the login attempts so that user is
	      // no longer presented with login dialog after pressing
	      // cancel.
	      if (server != null) {
	        server.loginAttempts = 3;
	      }
	    }		
	}
    return auth;
  }
  
  /*
   * Try to look up the server name based on the requesting URL.
   * If the server isn't found, recurse, stripping off last part
   * of path up to "/".  Eventually, we should get the the base
   * URL under which the server info is hashed.
   */
  private GenericServer getServer(String url) {

		GenericServer server = ServerList.getServer(url);
		if (server == null && url.indexOf("/") > 0) {
			int lastSlashPos = url.lastIndexOf("/");
			if (lastSlashPos > 0) {
				url = url.substring(0, lastSlashPos);
			}
			return getServer(url);
		} else {
			urlL.setText(url);
			return server;
		}
	  
  }

  public static void main(String[] args) {
    String test_site = args[0];
    try {
      UnibrowAuthenticator test = new UnibrowAuthenticator();
      Authenticator.setDefault(test);
      URL test_url = new URL(test_site);
      InputStream istr = test_url.openStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(istr));
      String line;
      while ((line = br.readLine()) != null) {
	System.out.println(line);
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    System.exit(0);
  }

}
