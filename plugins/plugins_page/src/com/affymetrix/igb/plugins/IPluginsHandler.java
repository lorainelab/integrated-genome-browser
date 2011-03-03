package com.affymetrix.igb.plugins;

import javax.swing.ImageIcon;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public interface IPluginsHandler {
	public void displayError(String errorText);
	public void clearError();
	public void installBundle(Bundle bundle);
	public Bundle getFilteredBundle(int index);
	public int getFilteredBundleCount();
	public ImageIcon getIcon(String name);
	public Bundle getBundleAtRow(int row);
	public boolean isUpdatable(Bundle bundle);
	public Version getLatestVersion(Bundle bundle);
}
