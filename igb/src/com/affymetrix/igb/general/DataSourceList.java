package com.affymetrix.igb.general;

import java.util.prefs.Preferences;

import com.affymetrix.genometryImpl.util.PreferenceUtils;

public class DataSourceList extends ServerList {
	public DataSourceList() {
		super("server");
	}
	protected Preferences getPreferencesNode() {
		return PreferenceUtils.getServersNode();
	}
}
