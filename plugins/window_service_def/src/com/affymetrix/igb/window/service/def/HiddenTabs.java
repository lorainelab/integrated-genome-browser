package com.affymetrix.igb.window.service.def;

import java.util.HashSet;
import java.util.Set;

import com.affymetrix.igb.osgi.service.IGBTabPanel;

/**
 * TabHolder implementation for all tabs that are hidden
 *
 */
public class HiddenTabs implements TabHolder {
	private Set<IGBTabPanel> addedPlugins;

	public HiddenTabs() {
		super();
		addedPlugins = new HashSet<IGBTabPanel>();
	}

	@Override
	public Set<IGBTabPanel> getPlugins() {
		return addedPlugins;
	}

	@Override
	public void addTab(IGBTabPanel plugin) {
		addedPlugins.add(plugin);
	}

	@Override
	public void removeTab(IGBTabPanel plugin) {
		addedPlugins.remove(plugin);
	}

	@Override
	public void restoreState() {}

	@Override
	public void resize() {}

	@Override
	public void close() {}
}
