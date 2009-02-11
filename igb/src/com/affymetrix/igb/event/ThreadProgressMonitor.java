/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.igb.event;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import java.util.Vector;

/**
 *  Provides a JDialog that can let the user know about an underlying process
 *  and provides a "Cancel" button to stop it.
 *  This is similar to the javax.swing.ProgressMonitor class, but does not
 *  show a JProgressBar, and has optional "OK" and "Cancel" buttons to dismiss the dialog.
 */
public class ThreadProgressMonitor {
  JOptionPane opt_pane;
  JDialog dialog;
  Thread thread;
  boolean is_cancelled = false;
  boolean is_closed = false;
  
  /** Creates a JDialog with parent component c.
   *  @param message  A message to display to the user.  Usually a String or String[].
   *  @param t  If non-null, this given Thread will be interrupted when
   *  the user presses the "Cancel" button (if there is a cancel button).
   *  @param can_cancel if true, there will be a "Cancel" button
   *  @param can_dismiss if true, there will be an "OK" button to close the dialog
   *  without interrupting the Thread.  (Can be dangerous)
   */
  public ThreadProgressMonitor(Component c, String title, Object message, 
  Thread t, boolean can_cancel, boolean can_dismiss) {
    String cancel_text = UIManager.getString("OptionPane.cancelButtonText");
    if (cancel_text == null) {cancel_text = "Cancel";}
    String ok_text = UIManager.getString("OptionPane.okButtonText");
    if (ok_text == null) {ok_text = "OK";}
    Vector<String> button_vector = new Vector<String>(2);
    if (can_dismiss) {button_vector.add(ok_text);}
    if (can_cancel) {button_vector.add(cancel_text);}
    final String cancel = cancel_text;
    final String ok = ok_text;
    String[] buttons = button_vector.toArray(new String[button_vector.size()]);
    this.thread = t;
    this.opt_pane = new JOptionPane(
      message,
      JOptionPane.INFORMATION_MESSAGE,
      JOptionPane.DEFAULT_OPTION, 
      (Icon) null, 
      buttons
    );
    this.dialog = opt_pane.createDialog(c, title);
    this.dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    opt_pane.addPropertyChangeListener(pcl);
  }

  PropertyChangeListener pcl = new java.beans.PropertyChangeListener() {
    public void propertyChange(java.beans.PropertyChangeEvent evt) {
      String prop = evt.getPropertyName();
      Object value = opt_pane.getValue();
      if ((evt.getSource() == opt_pane)
      && (prop.equals(JOptionPane.VALUE_PROPERTY)) && value != null) {
        if (value.equals(UIManager.getString("OptionPane.cancelButtonText"))) {
          cancelPressed();
        } else if (value.equals(UIManager.getString("OptionPane.okButtonText"))) {
          okPressed();
        }
      }
    }
  };
  
  public void setMessage(Object o) {
    if (is_closed) return;
    opt_pane.setMessage(o);
    dialog.pack();
  }

  public void setMessageEventually(final Object o) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        setMessage(o);
      }
    });
  }
  
  public void showDialog() {
    if (is_closed) return;
    dialog.pack();
    dialog.setVisible(true);
  }

  public void showDialogEventually() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        showDialog();
      }
    });
  }
  
  /** Permanently closes the dialog.  The dialogs in this method are not intended
   *  to be re-displayed after being closed.  This avoids some thread-related
   *  problems.  For example, if the closeDialog() method is activated before
   *  showDialog() or setMessage(), the dialog will remain closed, and may never
   *  actually have ever been shown.  Nonetheless, the proper thing to do usually
   *  is to call closeDialogEventually().
   */
  public void closeDialog() {
    is_closed = true;
    dialog.setVisible(false);
    opt_pane.removePropertyChangeListener(pcl);
    pcl = null;
    opt_pane = null;
    dialog = null;
  }

  public void closeDialogEventually() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        closeDialog();
      }
    });
  }
  
  /** This method is called when the user selects the "Cancel" button.
   *  It simply calls {@link #closeDialog()}, and if a thread was provided
   *  in the constructor it calls interrupt() on it.
   */
  public void cancelPressed() {
    dialog.setEnabled(false);
    is_cancelled = true;
    closeDialog();
    if (thread != null) {thread.interrupt();}
  }

  /** This method is called when the user selects the "OK" button.
   *  It simply calls {@link #closeDialog()}.
   */
  public void okPressed() {
    closeDialog();
  }
  
  /** Returns true if the cancel button has ever been pressed. */
  public boolean isCancelled() { return is_cancelled; }
}
