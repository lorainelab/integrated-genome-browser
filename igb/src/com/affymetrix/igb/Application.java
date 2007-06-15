package com.affymetrix.igb;

import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.StatusBar;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public abstract class Application {
  
  final static boolean USE_STATUS_BAR = true;
  StatusBar status_bar;

  static Application singleton = null;
  
  public Application() {
    if (USE_STATUS_BAR) {
      status_bar = new StatusBar();
    }
  }

  public static Application getSingleton() {
    return singleton;
  }
  
  abstract public JFrame getFrame();
  abstract public SeqMapView getMapView();

  /** Sets the text in the status bar.
   *  Will also echo a copy of the string to System.out.
   *  It is safe to call this method even if the status bar is not being displayed.
   */
  public void setStatus(String s) {
    setStatus(s, true);
  }

  public void setStatusBarHairlinePosition(String s) {
    if (USE_STATUS_BAR && status_bar != null) {
      status_bar.setPosition(s);
    }
  }

  /** Sets the text in the status bar.
   *  Will optionally echo a copy of the string to System.out.
   *  It is safe to call this method even if the status bar is not being displayed.
   *  @param echo  Whether to echo a copy to System.out.
   */
  public void setStatus(String s, boolean echo) {
    if (USE_STATUS_BAR && status_bar != null) {
      status_bar.setStatus(s);
    }
    if (echo && s != null) {System.out.println(s);}
  }

  /** Opens a JOptionPane.ERROR_MESSAGE panel with the application frame
   *  as its parent.
   */
  public static void errorPanel(String title, String message) {
    ErrorHandler.errorPanel(Application.getSingleton().getFrame(), title, message, null);
  }

  /** Opens a JOptionPane.ERROR_MESSAGE panel with the given frame
   *  as its parent.
   *  This is designed to probably be safe from the EventDispatchThread or from
   *  any other thread.
   *  @param frame the parent frame, null is ok.
   *  @param e an exception (or error), if any.  null is ok. If not null,
   *  the exception text will be appended to the message and
   *  a stack trace might be printed on standard error.
   */
  public static void errorPanel(String message) {
    ErrorHandler.errorPanel(Application.getSingleton().getFrame(), "ERROR", message, null);
  }

  /** Opens a JOptionPane.ERROR_MESSAGE panel with the IGB
   *  panel as its parent, and the title "ERROR".
   */
  public static void errorPanel(String message, Throwable e) {
    ErrorHandler.errorPanel(Application.getSingleton().getFrame(), "ERROR", message, e);
  }

  /** Shows a panel asking for the user to confirm something.
   *  @return true if the user confirms, else false.
   */
  public static boolean confirmPanel(String message) {
    Application app = getSingleton();
    JFrame frame = (app==null) ? null : app.getFrame();
    return (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
      frame, message, "Confirm", JOptionPane.OK_CANCEL_OPTION));
  }

  public static void informPanel(String message) {
    Application app = getSingleton();
    JFrame frame = (app==null) ? null : app.getFrame();
    JOptionPane.showMessageDialog(frame, message, "Inform", JOptionPane.INFORMATION_MESSAGE);
  }

}
