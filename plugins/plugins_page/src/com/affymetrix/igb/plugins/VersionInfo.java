package com.affymetrix.igb.plugins;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class VersionInfo {
	private final Bundle bundle;
	private final IPluginsHandler pluginsHandler;
	public VersionInfo(Bundle bundle, IPluginsHandler pluginsHandler) {
		super();
		this.bundle = bundle;
		this.pluginsHandler = pluginsHandler;
	}
	public Version getVersion() {
		return bundle.getVersion();
	}
	public Version getLatestVersion() {
		return pluginsHandler.getLatestVersion(bundle);
	}
	public String toString() {
		return pluginsHandler.isUpdatable(bundle) ?
				"<html>" + getVersion() + " (<b>" + getLatestVersion() + "</b>)</html>)" :
				"" + getVersion();
	}
}
