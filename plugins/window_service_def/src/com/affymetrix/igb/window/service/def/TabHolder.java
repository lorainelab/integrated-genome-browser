package com.affymetrix.igb.window.service.def;

import java.util.Set;

import com.affymetrix.igb.osgi.service.IGBTabPanel;

public interface TabHolder {
	public Set<IGBTabPanel> getPlugins();
	public void addTab(final IGBTabPanel plugin, boolean setFocus);
	public void removeTab(final IGBTabPanel plugin);
	public void resize();
	public IGBTabPanel getSelectedIGBTabPanel();
	public void close();
}
