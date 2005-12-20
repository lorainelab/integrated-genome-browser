/**
*   Copyright (c) 2005 Affymetrix, Inc.
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

import java.awt.Frame;

/**
 *  General Swing display utilities.
 */
public class DisplayUtils {
  
  /** De-iconify a Frame and bring it to the front of the display, 
   *  without changing the minimized/maximized state.
   */
  public static void bringFrameToFront(Frame frame) {
    boolean isShowing = frame.isShowing();
    if ((frame.getExtendedState() & Frame.ICONIFIED) == Frame.ICONIFIED) {
      // de-iconify it while leaving the maximized/minimized state flags alone
      frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
    }
    if (! frame.isShowing()) { frame.show(); }
    frame.toFront();
  }
  
}
