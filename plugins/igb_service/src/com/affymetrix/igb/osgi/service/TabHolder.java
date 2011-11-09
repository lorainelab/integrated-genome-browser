package com.affymetrix.igb.osgi.service;

import java.util.Set;


/**
 * Abstract holder of tab panes
 */
public interface TabHolder {
	/**
	 * get the name of the tab holder
	 * @return name of the TabHolder
	 */
	public String getName();
	/**
	 * get all the tab panes that this holder contains
	 * @return all the tab panes
	 */
	public Set<IGBTabPanel> getPlugins();
	/**
	 * add a new tab pane to this holder
	 * @param plugin the tab pane to add
	 */
	public void addTab(final IGBTabPanel plugin);
	/**
	 * remove a tab pane from this holder
	 * @param plugin the tab pane to remove
	 */
	public void removeTab(final IGBTabPanel plugin);
	/**
	 * restore the state from the saved Preferences
	 */
	public void restoreState();
	/**
	 * resize the holder (only appropriate for trays)
	 */
	public void resize();
	/**
	 * close the holder (only appropriate for trays)
	 */
	public void close();
	/**
	 * select a tab pane in this holder (make it visible)
	 * @param plugin the tab pane to remove
	 */
	public void selectTab(IGBTabPanel panel);
}
