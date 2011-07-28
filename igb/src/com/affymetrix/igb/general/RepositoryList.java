package com.affymetrix.igb.general;

import java.util.prefs.Preferences;

import com.affymetrix.genometryImpl.util.PreferenceUtils;

public class RepositoryList extends ServerList {
	public RepositoryList() {
		super("repository");
	}
	protected Preferences getPreferencesNode() {
		return PreferenceUtils.getRepositoriesNode();
	}

	public boolean hasTypes() {
		return false;
	}
}
