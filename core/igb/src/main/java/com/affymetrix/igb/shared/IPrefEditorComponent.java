/**
 * Copyright (c) 2001-2004 Affymetrix, Inc.
 * 
* Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 * 
* The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.shared;

import javax.swing.JPanel;

/**
 * An interface that should be implemented by any JComponent that can be used to
 * view and/or modify a sub-set of the preferences used by the program. There is
 * no requirement that the preferences be stored using the java.util.prefs
 * package, but that is what is generally expected.
 */
public abstract class IPrefEditorComponent extends JPanel {

    private static final long serialVersionUID = 1L;

    /**
     * Causes the JComponent to update its fields so that they match what is
     * stored in the java preferences. Some implementations may be listening to
     * preference change events and generally keep up-to-date without needing
     * this method. But this method may be called after large events, such as
     * importing an xml file containing preferences, or after deleting stored
     * preferences.
     */
    public abstract void refresh();
}
