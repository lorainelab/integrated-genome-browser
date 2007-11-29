/**
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.  
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*   
*   @author nix
*   
*   Pulls a Dialog box to request user login info.
*/

package com.affymetrix.igb.util;
import java.awt.*;
import javax.swing.*;

/**Based on the UnibrowAuthenticator, a simple Dialog box to fetch userName and 
 * password info for authenticating with a DAS2 server.*/
public class SimpleAuthenticator {

  private JTextField userTF = new JTextField();
  private JPasswordField passwordTF = new JPasswordField();
  private JPanel messagePanel = new JPanel();


  public SimpleAuthenticator(String host) {
	  messagePanel.setLayout(new GridLayout(3, 2));
	  messagePanel.add(new JLabel("DAS2Server:"));
	  messagePanel.add(new JLabel(host));
	  messagePanel.add(new JLabel("User Name:" ));
	  messagePanel.add(userTF);
	  messagePanel.add(new JLabel("Password:"));
	  messagePanel.add(passwordTF);
  }

  /**@return - null if they hit cancel or a String[] of user, password.
   * @param message - to display in Dialog box title.*/
  public String[] requestAuthentication(String message)  {
    int result = JOptionPane.showOptionDialog(null, messagePanel, message, 
					      JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, 
					      null, null, null );
    // if clicked "cancel", then return a null, 
    if (result == JOptionPane.OK_OPTION) {
      String userName = userTF.getText().trim();
      String password = new String (passwordTF.getPassword()).trim();
      return new String[] {userName, password};
    }
    return null;
  }

  /*public static void main(String[] args) {
    SimpleAuthenticator sa = new SimpleAuthenticator("localhost/DAS2/");
    String[] x = sa.requestAuthentication("Authenticate or Hit Cancel");
    if (x != null) System.out.println(x[0]+" | "+x[1]);
    else System.out.println("Nullly");
  }*/

}
