package com.affymetrix.igb;

import com.affymetrix.igb.prefs.IPlugin;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.StatusBar;
import java.awt.Image;
import java.util.*;
import java.util.logging.Logger;
import javax.swing.*;

public abstract class Application {

	public static final boolean CACHE_GRAPHS = true;
	public final static boolean DEBUG_EVENTS = false;
	protected final StatusBar status_bar;
	private final List<String> progressStringList = new ArrayList<String>(); // list of progress bar messages.
	static Application singleton = null;
	private final Map<Class, IPlugin> plugin_hash = new HashMap<Class, IPlugin>();
	private static final String leftPaddingString = "  ";	//padding for status bar

	public Application() {
		singleton = this;
		status_bar = new StatusBar();
	}

	public void setPluginInstance(Class c, IPlugin plugin) {
		plugin_hash.put(c, plugin);
		plugin.putPluginProperty(IPlugin.TEXT_KEY_APP, this);
		plugin.putPluginProperty(IPlugin.TEXT_KEY_SEQ_MAP_VIEW, this.getMapView());
	}

	public IPlugin getPluginInstance(Class c) {
		return plugin_hash.get(c);
	}

	public static Application getSingleton() {
		return singleton;
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

	public final void addNotLockedUpMsg(String s) {
		progressStringList.add(s);
		if (status_bar.getStatus().trim().length() == 0) {
			setNotLockedUpStatus(s);
		}
	}

	public final void removeNotLockedUpMsg(String s) {
		if (!progressStringList.remove(s)) {
			Application.getApplicationLogger().fine("Didn't find progress message: " + s);
		}
		if (status_bar.getStatus().equals(leftPaddingString + s)) {
			// Time to change status message.
			if (progressStringList.isEmpty()) {
				setStatus("");
				status_bar.progressBar.setVisible(false);
			} else {
				setNotLockedUpStatus(progressStringList.get(0));
			}
		}
	}

	/**
	 * Set the status, and make a little progress bar so that the app doesn't look locked up.
	 * @param s
	 */
	private final synchronized void setNotLockedUpStatus(String s) {
		status_bar.setStatus(leftPaddingString + s);
		this.setNotLockedUpStatus();
	}

	/**
	 * Make a little progress bar so that the app doesn't look locked up.
	 */
	public final synchronized void setNotLockedUpStatus() {
		status_bar.progressBar.setVisible(true);
		status_bar.progressBar.setIndeterminate(true);
	}

	/** Sets the text in the status bar.
	 *  Will also echo a copy of the string to System.out.
	 *  It is safe to call this method even if the status bar is not being displayed.
	 */
	public final synchronized void setStatus(String s) {
		setStatus(s, true);
	}

	/** Sets the text in the status bar.
	 *  Will optionally echo a copy of the string to System.out.
	 *  It is safe to call this method even if the status bar is not being displayed.
	 *  @param echo  Whether to echo a copy to System.out.
	 */
	public final synchronized void setStatus(String s, boolean echo) {
		status_bar.setStatus(leftPaddingString + s);
		if (echo && s != null) {
			System.out.println(s);
		}
	}

	/** Opens a JOptionPane.ERROR_MESSAGE panel with the application frame
	 *  as its parent.
	 */
	public static void errorPanel(String title, String message) {
		Application app = getSingleton();
		JFrame frame = (app == null) ? null : app.getFrame();
		ErrorHandler.errorPanel(frame, title, message, null);
	}

	/** Opens a JOptionPane.ERROR_MESSAGE panel with the IGB
	 *  panel as its parent, and the title "ERROR".
	 */
	public static void errorPanel(String message) {
		Application app = getSingleton();
		JFrame frame = (app == null) ? null : app.getFrame();
		ErrorHandler.errorPanel(frame, "ERROR", message, null);
	}

	/** Opens a JOptionPane.ERROR_MESSAGE panel with the IGB
	 *  panel as its parent, and the title "ERROR".
	 *  @param e an exception (or error), if any.  null is ok. If not null,
	 *  the exception text will be appended to the message and
	 *  a stack trace might be printed on standard error.
	 */
	public static void errorPanel(String message, Throwable e) {
		Application app = getSingleton();
		JFrame frame = (app == null) ? null : app.getFrame();
		ErrorHandler.errorPanel(frame, "ERROR", message, e);
	}

	/** Shows a panel asking for the user to confirm something.
	 *  @return true if the user confirms, else false.
	 */
	public static boolean confirmPanel(String message) {
		Application app = getSingleton();
		JFrame frame = (app == null) ? null : app.getFrame();
		return (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
				frame, message, "Confirm", JOptionPane.YES_NO_OPTION));
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
}
