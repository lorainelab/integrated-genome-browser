/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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

package com.affymetrix.igb.menuitem;

import javax.swing.*;
import java.awt.event.ActionListener;

import com.affymetrix.igb.util.UnibrowPrefsUtil;

public abstract class MenuUtil {

  
  /** Sets the accelerator for the given JMenuItem based on
   *  the preference associated with the action command.
   *  The action command Strings should be unique across the whole application.
   */
  public static final void addAccelerator(JMenuItem item, String command) {
    item.setAccelerator(UnibrowPrefsUtil.getAccelerator(command));
  }
  
  /** Sets up an association such that the accelerator given in
   *  the user prefs for the action_command will cause the given
   *  action_command to be sent to the given ActionListener
   *  when that accelerator key is pressed and the given component
   *  is in the window that has keyboard focus.
   *  If there was no user preference given for the action command,
   *  this routine does nothing.
   *  @return a KeyStroke, from {@link UnibrowPrefsUtil#getAccelerator(String)}
   *  is returned as a convenience
   *  @see UnibrowPrefsUtil#getAccelerator(String)
   */
  public static final KeyStroke addAccelerator(JComponent comp, ActionListener al,
    String action_command) {
    KeyStroke ks = UnibrowPrefsUtil.getAccelerator(action_command);
    if (ks != null) {
      comp.registerKeyboardAction(al, action_command, ks,
       JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
    return ks;
  }

  /**
   *  Calls {@link #addToMenu(JMenu, JMenuItem, String)}
   *  with command set to null.
   */
  public static final JMenuItem addToMenu(JMenu menu, JMenuItem item) {
    return addToMenu(menu, item, null);
  }

  /** Adds a JMenuItem to a JMenu after first setting the accelerator
   *  based on the user preference associated with the given command String.
   *  @param command  A String that should uniquely identify the given
   *    menu item, unique across the entire application.  If null,
   *    the value of JMenuItem.getText() will be used, but there is
   *    a risk that that will not be unique.
   *  @see UnibrowPrefsUtil#getAccelerator(String)
   */
  public static final JMenuItem addToMenu(JMenu menu, JMenuItem item, String command) {
    if (command == null) {command = item.getText();}
    if (command != null) { addAccelerator(item, command); }
    return menu.add(item);
  }

  /**
   *  Loads an ImageIcon from the specified system resource.
   *  The system resource should be in the classpath, for example,
   *  it could be in the jlfgr-1_0.jar file.  If the resource is
   *  absent or can't be found, this routine will not throw an exception,
   *  but will return null.
   *  For example: "toolbarButtonGraphics/general/About16.gif".
   *  @return An ImageIcon or null if the one specified could not be found.
   */
  public static ImageIcon getIcon(String resource_name) {
    ImageIcon icon = null;
    try {
      // Note: MenuUtil.class.getResource(resource_name) does not work;
      // ClassLoader.getSystemResource(resource_name) works locally, but not with WebStart;
      //
      // Both of these work locally and with WebStart:
      //  MenuUtil.class.getClassLoader().getResource(resource_name)
      //  Thread.currentThread().getContextClassLoader().getResource(resource_name)
      java.net.URL url = Thread.currentThread().getContextClassLoader().getResource(resource_name);      
      if (url != null) {
        icon = new ImageIcon(url);
      }
    } catch (Exception e) {
      // It isn't a big deal if we can't find the icon, just return null
    }
    return icon;    
  }
}
