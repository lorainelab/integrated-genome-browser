package com.affymetrix.igb.plugins;

import javax.swing.JLabel;

import org.osgi.framework.Bundle;

public abstract class BundleColumn {
	public abstract String getTitle();
	public Class<?> getCellClass() { return JLabel.class; }
	public boolean isEditable() { return false; }
	public abstract Object getValue(Bundle bundle);
	public void setValue(Bundle bundle, Object aValue, IPluginsHandler pluginsHandler) {}
	public boolean tier2OK() { return true; }
}
