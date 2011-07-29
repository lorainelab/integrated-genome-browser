package com.affymetrix.igb.general;

import java.util.prefs.Preferences;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.view.load.GeneralLoadUtils;

public class DataSourceList extends ServerList {
	public DataSourceList() {
		super("server");
	}
	@Override
	protected Preferences getPreferencesNode() {
		return PreferenceUtils.getServersNode();
	}
	@Override
	public boolean discoverServer(GenericServer gServer) {
		boolean result = GeneralLoadUtils.discoverServer(this, gServer);
		if (!result) {
			gServer.setEnabled(false);
		}
		return result;
	}
	@Override
	public void fireServerInitEvent(GenericServer server, ServerStatus status, boolean forceUpdate, boolean removedManually) {
		if (status == ServerStatus.NotResponding) {
			GeneralLoadUtils.removeServer(server);
		}
		super.fireServerInitEvent(server, status, forceUpdate, removedManually);
	}
	@Override
	public GenericVersion discoverVersion(String versionID, String versionName, GenericServer gServer, Object versionSourceObj, String speciesName) {
		return GeneralLoadUtils.discoverVersion(versionID, versionName, gServer, versionSourceObj, speciesName);
	}
}
