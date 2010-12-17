package com.affymetrix.igb.plugins;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public interface IPluginsHandler {
	public void displayError(String errorText);
	public void clearError();
	public void installBundle(Bundle bundle);
	public List<Bundle> getFilteredBundles();
	public boolean isUpdatable(Bundle bundle);
	public Version getLatestVersion(Bundle bundle);
	public boolean isTier2Bundle(Bundle bundle);
}
