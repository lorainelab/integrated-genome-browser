package com.affymetrix.igb;

import com.affymetrix.igb.prefs.IPlugin;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.StatusBar;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

public abstract class Application {
  
  final static boolean USE_STATUS_BAR = true;
  public static boolean ALLOW_DELETING_DATA = false;
  public static boolean CACHE_GRAPHS = true;

  public final static boolean DEBUG_EVENTS = false;
  StatusBar status_bar;

  static Application singleton = null;
  
  public Application() {
    if (USE_STATUS_BAR) {
      status_bar = new StatusBar();
    }
    singleton = this;
  }
  
  Map plugin_hash = new HashMap();
  
  public void setPluginInstance(Class c, IPlugin plugin) {
    plugin_hash.put(c, plugin);
    //icon = (ImageIcon) plugin.getPluginProperty(IPlugin.TEXT_KEY_ICON);

    plugin.putPluginProperty(IPlugin.TEXT_KEY_APP, this);
    plugin.putPluginProperty(IPlugin.TEXT_KEY_SEQ_MAP_VIEW, this.getMapView());
  }

  public IPlugin getPluginInstance(Class c) {
    return (IPlugin) plugin_hash.get(c);
  }
  
  public static Application getSingleton() {
    if (singleton == null) {
      return new NullApplication();
    } else {
      return singleton;
    }
  }
  
  abstract public Image getIcon();
  abstract public JFrame getFrame();
  abstract public SeqMapView getMapView();
  abstract public String getApplicationName();
  abstract public String getVersion();
  
  /** Allows you to get arbitrary parameter strings, possibly from a ResourceBundle. */
  abstract public String getResourceString(String key);

  public static boolean isSequenceAccessible() {
    return true;
  }

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
    Application app = getSingleton();
    JFrame frame = (app==null) ? null : app.getFrame();
    ErrorHandler.errorPanel(frame, title, message, null);
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
    Application app = getSingleton();
    JFrame frame = (app==null) ? null : app.getFrame();
    ErrorHandler.errorPanel(frame, "ERROR", message, null);
  }

  /** Opens a JOptionPane.ERROR_MESSAGE panel with the IGB
   *  panel as its parent, and the title "ERROR".
   */
  public static void errorPanel(String message, Throwable e) {
    Application app = getSingleton();
    JFrame frame = (app==null) ? null : app.getFrame();
    ErrorHandler.errorPanel(frame, "ERROR", message, e);
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

  public abstract Logger getLogger();

  public static Logger getApplicationLogger() {
    return getSingleton().getLogger();
  }
  
  public static void logError(String msg) {
    getApplicationLogger().severe(msg);
  }
  
  public static void logWarning(String msg) {
    getApplicationLogger().warning(msg);
  }
  
  public static void logInfo(String msg) {
    getApplicationLogger().info(msg);
  }
  
  public static void logDebug(String msg) {
    getApplicationLogger().fine(msg);
  }
  
  /** A very basic implementation of Application.  It returns null from most methods. */
  static class NullApplication extends Application {
    public Image getIcon() { return null;}
    public JFrame getFrame() { return null;}
    public SeqMapView getMapView() {return null;}
    public String getApplicationName() {return "Null Application";}
    public String getVersion() {return "0.0";}
    public String getResourceString(String key) { return null; }

    /** Returns a non-null Logger. */
    public Logger getLogger() {
      return Logger.getLogger("Null Logger");
    }
  };
}
