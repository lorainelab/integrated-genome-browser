package com.affymetrix.igb;

import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.StatusBar;

import java.awt.Image;
import java.util.*;
import java.util.prefs.Preferences;
import javax.swing.*;

public abstract class Application {

	public final static boolean DEBUG_EVENTS = false;
	protected final StatusBar status_bar;
	private final Set<String> progressStringList = Collections.synchronizedSet(new LinkedHashSet<String>()); // list of progress bar messages.
	static Application singleton = null;

	public Application() {
		singleton = this;
		status_bar = new StatusBar();
	}

	public static Application getSingleton() {
		return singleton;
	}

	abstract public Image getIcon();

	abstract public JFrame getFrame();

	abstract public SeqMapView getMapView();

	public final void addNotLockedUpMsg(final String s) {
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				progressStringList.add(s);
				if (status_bar.getStatus().trim().length() == 0) {
					setNotLockedUpStatus(s, true);
				}
			}
		});
	}

	public final void removeNotLockedUpMsg(final String s) {

		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				progressStringList.remove(s);
				if (status_bar.getStatus().equals(s) || status_bar.getStatus().trim().length() == 0) {
					// Time to change status message.
					if (progressStringList.isEmpty()) {
						setNotLockedUpStatus(null, false);
					} else {
						setNotLockedUpStatus(progressStringList.iterator().next(), true);
					}
				}
			}
		});
	}

	/**
	 * Set the status text, and show a little progress bar so that the app doesn't look locked up.
	 * @param s text of the message
	 * @param visible if the progress bar is to be displayed
	 */
	private synchronized void setNotLockedUpStatus(String s, boolean visible) {
		status_bar.setStatus(s);
		status_bar.progressBar.setVisible(visible);
	}
	
	/** Sets the text in the status bar.
	 *  Will also echo a copy of the string to System.out.
	 *  It is safe to call this method even if the status bar is not being displayed.
	 */
	public final void setStatus(String s) {
		setStatus(s, true);
	}

	/** Sets the text in the status bar.
	 *  Will optionally echo a copy of the string to System.out.
	 *  It is safe to call this method even if the status bar is not being displayed.
	 *  @param echo  Whether to echo a copy to System.out.
	 */
	public final void setStatus(final String s, final boolean echo) {
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				status_bar.setStatus(s);
				if (echo && s != null && !s.isEmpty()) {
					System.out.println(s);
				}
			}
		});
	}

	/**
	 * Shows a panel asking for the user to confirm something.
	 *
	 * @param message the message String to display to the user
	 * @return true if the user confirms, else false.
	 */
	public static boolean confirmPanel(String message) {
		return confirmPanel(message, null, null, false);
	}
	
	public static boolean confirmPanel(final JComponent comp, String message) {
		return confirmPanel(comp, message, null, null, false);
	}
	
	public static boolean confirmPanel(final String message, final Preferences node,
			final String check, final boolean def_val) {
		Application app = getSingleton();
		JFrame frame = (app == null) ? null : app.getFrame();
		return confirmPanel(frame.getRootPane(), message, node, check, def_val);
	}
	
	public static boolean confirmPanel(final JComponent comp, final String message, final Preferences node,
			final String check, final boolean def_val) {
		Object[] params = null;

		//If no node is provided then show default message
		if (node == null) {
			return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
					comp, message, "Confirm", JOptionPane.YES_NO_OPTION);
		}

		//If all parameters are provided then look up for boolean value from preference.
		final boolean b = node.getBoolean(check, def_val);

		//If user has already set preference then return true.
		if (b != def_val) {
			return true;
		}

		//If preference is not set then show message with option to disable it.
		final JCheckBox checkbox = new JCheckBox("Do not show this message again.");
		params = new Object[]{message, checkbox};

		int ret = JOptionPane.showConfirmDialog(
				comp, params, "Confirm", JOptionPane.YES_NO_OPTION);

		if (JOptionPane.YES_OPTION == ret) {
			if(checkbox.isSelected()){
				node.putBoolean(check, checkbox.isSelected() != b);
			}
			return true;
		}

		return false;
	}
	
}
