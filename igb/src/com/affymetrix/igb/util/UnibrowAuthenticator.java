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
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.*;
import javax.swing.*;

public final class UnibrowAuthenticator extends Authenticator  {
  JFrame frm;
  JTextField userTF = new JTextField();
  JPasswordField passwordTF = new JPasswordField();
  JPanel message_panel = new JPanel();
  JLabel hostL = new JLabel();
  JLabel promptL = new JLabel();

  public UnibrowAuthenticator() {
    this(null);
  }

  public UnibrowAuthenticator(JFrame jf) {
    frm = jf;
    message_panel.setLayout(new GridLayout(4, 2));
    message_panel.add(new JLabel("Site"));
    message_panel.add(hostL);
    message_panel.add(new JLabel("Realm"));
    message_panel.add(promptL);
    message_panel.add(new JLabel("User Name" ));
    message_panel.add(userTF);
    message_panel.add(new JLabel("Password"));
    message_panel.add(passwordTF);
  }

  protected PasswordAuthentication getPasswordAuthentication()  {
    int result = JOptionPane.showOptionDialog(frm, message_panel, "Enter Network Password", 
					      JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, 
					      null, null, null );
    // if clicked "cancel", then return a null, 
    // otherwise return a PasswordAuthentication based on the supplied user name and password
    PasswordAuthentication auth = null;
    if (result == JOptionPane.OK_OPTION) {
      String user_name = userTF.getText();
      char[] user_password = passwordTF.getPassword();
      auth = new PasswordAuthentication(user_name, user_password);
    }
    return auth;
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
