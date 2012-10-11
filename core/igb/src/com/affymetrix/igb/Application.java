package com.affymetrix.igb;

import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.view.StatusBar;

import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.Timer;

public abstract class Application {

	public final static boolean DEBUG_EVENTS = false;
	private static final int delay = 2; //delay in seconds
	
	private final LinkedList<String> progressStringList = new LinkedList<String>(); // list of progress bar messages.
	ActionListener update_status_bar = new ActionListener() {

		public void actionPerformed(java.awt.event.ActionEvent ae) {
			synchronized (progressStringList) {
				String s = progressStringList.pop();
				progressStringList.addLast(s);
				setNotLockedUpStatus(s);
			}
		}
	};
	Timer timer = new Timer(delay*1000, update_status_bar);
	
	static Application singleton = null;
	public final static StatusBar status_bar = new StatusBar();
	
	public Application() {
		singleton = this;
	}

	public static Application getSingleton() {
		return singleton;
	}

	abstract public java.awt.Image getIcon();

	abstract public javax.swing.JFrame getFrame();

	abstract public com.affymetrix.igb.view.SeqMapView getMapView();
			
	public final void addNotLockedUpMsg(final String s) {
		synchronized (progressStringList) {
			progressStringList.addFirst(s);
		}
		update_status_bar.actionPerformed(null);
		
		if(!timer.isRunning()){
			timer.start();
		}
	}

	public final void removeNotLockedUpMsg(final String s) {
		synchronized (progressStringList) {
			progressStringList.remove(s);
			
			if (progressStringList.isEmpty()) {
				setNotLockedUpStatus(null);
				timer.stop();
			}
		}
	}

	/**
	 * Set the status text, and show a little progress bar
	 * so that the application doesn't look locked up.
	 * @param s text of the message
	 */
	private synchronized void setNotLockedUpStatus(String s) {
		status_bar.setStatus(s);
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
		status_bar.setStatus(s);
		if (echo && s != null && !s.isEmpty()) {
			System.out.println(s);
		}
	}

	public abstract void setSelField(Map<String, Object> properties, String s);
	
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
		JComponent comp = (app == null) ? null : app.getFrame().getRootPane();
		return confirmPanel(comp, message, node, check, def_val);
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
	
	public static void infoPanel(final String message, final Preferences node,
			final String check, final boolean def_val) {
	
		Application app = getSingleton();
		JComponent comp = (app == null) ? null : app.getFrame().getRootPane();
		
		if(node == null){
			JOptionPane.showMessageDialog(comp, message, "IGB", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		final JCheckBox checkbox = new JCheckBox("Do not show this message again.");
		Object[] params = new Object[]{message, checkbox};
		
		//If all parameters are provided then look up for boolean value from preference.
		final boolean b = node.getBoolean(check, def_val);

		//If user has already set preference then return true.
		if (b != def_val) {
			return;
		}

		JOptionPane.showMessageDialog(comp, params, "IGB", JOptionPane.INFORMATION_MESSAGE);
		
		if(checkbox.isSelected()){
			node.putBoolean(check, checkbox.isSelected() != b);
		}		
	}
}
