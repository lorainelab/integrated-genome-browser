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

package com.affymetrix.igb.prefs;

import javax.swing.Icon;


/**
 *  An interface that should be implemented by any JComponent that can be used 
 *  to view and/or modify a sub-set of the preferences used by the program.
 *  There is no requirement that the preferences be stored using the
 *  java.util.prefs package, but that is what is generally expected.
 */
public interface IPrefEditorComponent {
    
  /**
   *  Returns a String that can be used as the name of the component.
   *  Preferably a very short string, since it may be used as the name of
   *  a tab in a tab pane.
   *  @return a non-null, short String identifier.
   */
  public String getName();
  
  /**
   *  Will return an Icon for use in tab panes or frames containing the component.
   *  Null is ok.
   *  @return an Icon or null
   */
  public Icon getIcon();
  
  /**
   *  Returns a short String description, appropriate for use as a tool-tip.
   *  Can be equivalent to the name.
   *  @return A non-null String.
   */
  public String getToolTip();
  
  /**
   *  Gives help text explaining the function of this preferences editor component.
   *  If no help is available, should return null rather than an empty String.
   *  The help text should describe what effect changes in the preferences
   *  in the panel will have, how to make the changes (if it isn't obvious),
   *  and whether the changes are expected to take effect immediately or only
   *  after a re-start.
   *  @return Text in HTML format, or null
   */
  public String getHelpTextHTML();
  
  /**
   *  Returns a URL where the user can go for more information on this panel.
   *  @return a String or null
   */
  public String getInfoURL();
  
  /**
   *  Causes the JComponent to update its fields
   *  so that they match what is stored in the java preferences.
   *  Some implementations may be listening to preference change events and
   *  generally keep up-to-date without needing this method.  
   *  But this method may be called after large events, such as importing
   *  an xml file containing preferences, or after deleting stored preferences.
   */
  public void refresh();
}
