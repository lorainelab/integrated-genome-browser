package com.affymetrix.igb.osgi.service;

import java.util.Set;

/**
 * Abstract holder of tab panes.
 */
public interface TabHolder {
	/**
	 * Get the name of the tab holder.
	 * @return name of the TabHolder
	 */
	public String getName();
	/**
	 * Get all the tab panes that this holder contains.
	 * @return all the tab panes
	 */
	public Set<IGBTabPanel> getPlugins();
	/**
	 * Add a new tab pane to this holder.
	 * @param plugin the tab pane to add
	 */
	public void addTab(final IGBTabPanel plugin);
	/**
	 * Remove a tab pane from this holder.
	 * @param plugin the tab pane to remove
	 */
	public void removeTab(final IGBTabPanel plugin);
	/**
	 * Restore the state from the saved Preferences.
	 */
	public void restoreState();
	/**
	 * Resize the holder (only appropriate for trays).
	 */
	public void resize();
	/**
	 * Close the holder (only appropriate for trays).
	 */
	public void close();
	/**
	 * Select a tab pane in this holder (make it visible).
	 * @param panel the tab pane to remove
	 */
	public void selectTab(IGBTabPanel panel);
}
