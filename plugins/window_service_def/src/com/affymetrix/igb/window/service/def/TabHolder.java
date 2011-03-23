package com.affymetrix.igb.window.service.def;

import java.util.Set;

import com.affymetrix.igb.osgi.service.IGBTabPanel;

/**
 * Abstract holder of tab panes
 */
public interface TabHolder {
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
	 * get the selected tab pane (only appropriate for trays)
	 * @return the tab pane that is currently selected
	 */
	public IGBTabPanel getSelectedIGBTabPanel();
	/**
	 * indicate that the tab with focus for the holder is found
	 * (the default state for the tab, not the current state)
	 */
	public void setFocusFound();
	/**
	 * resize the holder (only appropriate for trays)
	 */
	public void resize();
	/**
	 * close the holder (only appropriate for trays)
	 */
	public void close();
}
