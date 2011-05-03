package com.affymetrix.igb.window.service;

import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JPanel;

import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IGBTabPanel.TabState;

public interface IWindowService {
	/**
	 * run when the Window Service starts
	 */
	public void startup();
	/**
	 * run when the Window Service stops - when the program exits
	 */
	public void shutdown();
	/**
	 * save the state of the window
	 */
	public void saveState();
	/**
	 * restore the state of the window
	 */
	public void restoreState();
	/**
	 * pass in the main frame of the application (JFrame for Swing)
	 * @param jFrame the main frame of the application
	 */
	public void setMainFrame(JFrame jFrame);
	/**
	 * pass in the SeqMapView, this is the main IGB view
	 * @param jPanel the JPanel that contains the main IGB view
	 */
	public void setSeqMapView(JPanel jPanel);
	/**
	 * pass in the view menu
	 * @param view_menu the view menu
	 */
	public void setViewMenu(JMenu view_menu);
	/**
	 * pass in the status bar of the application, this is where
	 * message and some icons are displayed
	 * @param status_bar the status bar
	 */
	public void setStatusBar(JComponent status_bar);
	/**
	 * get all the tab panels that have been added
	 * @return the set of tab panels added
	 */
	public Set<IGBTabPanel> getPlugins();
	/**
	 * set the state of the given tab to the given state and update
	 * the view menu to the new value
	 * @param igbTabPanel the tab to change
	 * @param tabState the new state
	 */
	public void setTabStateAndMenu(IGBTabPanel panel, TabState tabState);
	/**
	 * select the given tab in the tab panel, bringing it to the front
	 * @param panel the IGBTabPanel
	 */
	public void selectTab(IGBTabPanel panel);
}
