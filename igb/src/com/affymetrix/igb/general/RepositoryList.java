package com.affymetrix.igb.general;

import java.util.prefs.Preferences;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBServiceImpl;

public class RepositoryList extends ServerList {
	public RepositoryList() {
		super("repository");
	}
	@Override
	protected Preferences getPreferencesNode() {
		return PreferenceUtils.getRepositoriesNode();
	}
	@Override
	public boolean hasTypes() {
		return false;
	}
	@Override
	public boolean discoverServer(GenericServer gServer) {
		Application.getSingleton().addNotLockedUpMsg("Loading repository " + gServer);
		boolean result = IGBServiceImpl.getInstance().repositoryAdded(gServer.URL);
		if (!result) {
			gServer.setEnabled(false);
			IGBServiceImpl.getInstance().repositoryRemoved(gServer.URL);
		}
		Application.getSingleton().removeNotLockedUpMsg("Loading repository " + gServer);
		return result;
	}
	@Override
	public void fireServerInitEvent(GenericServer server, ServerStatus status, boolean forceUpdate, boolean removedManually) {
		if (status == ServerStatus.NotResponding) {
			IGBServiceImpl.getInstance().repositoryRemoved(server.URL);
		}
		super.fireServerInitEvent(server, status, forceUpdate, removedManually);
	}
	@Override
	public GenericVersion discoverVersion(String versionID, String versionName, GenericServer gServer, Object versionSourceObj, String speciesName) {
		return null;
	}
}
