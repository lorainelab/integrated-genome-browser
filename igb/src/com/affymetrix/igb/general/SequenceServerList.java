package com.affymetrix.igb.general;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.ServerUtils;
import com.affymetrix.igb.view.load.GeneralLoadUtils;

public class SequenceServerList extends ServerList {
	private final Map<String, List<GenericVersion>> species2genericVersionList =
		new LinkedHashMap<String, List<GenericVersion>>();	// the list of versions associated with the species
	public SequenceServerList() {
		super("sequenceServer");
	}
	@Override
	protected Preferences getPreferencesNode() {
		return PreferenceUtils.getSequenceServersNode();
	}
	private int getServerOrder(GenericServer server) {
		String url = GeneralUtils.URLEncode(ServerUtils.formatURL(server.URL, server.serverType));
		return Integer.parseInt(PreferenceUtils.getSequenceServersNode().node(url).get("order", "0"));
	}
	@Override
	public synchronized Collection<GenericServer> getAllServers() {
		ArrayList<GenericServer> allServers = new ArrayList<GenericServer>(url2server.values());
		Collections.sort(allServers,
			new Comparator<GenericServer>() {
				@Override
				public int compare(GenericServer o1, GenericServer o2) {
					return getServerOrder(o1) - getServerOrder(o2);
				}
			}
		);
		return allServers;
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
	public synchronized GenericVersion discoverVersion(String versionID, String versionName, GenericServer gServer, Object versionSourceObj, String speciesName) {
		String preferredVersionName = GeneralLoadUtils.LOOKUP.getPreferredName(versionName);
		GenericVersion gVersion = new GenericVersion(null, versionID, preferredVersionName, gServer, versionSourceObj);
		List<GenericVersion> gVersionList = getSpeciesVersionList(speciesName);
		if (!gVersionList.contains(gVersion)) {
			gVersionList.add(gVersion);
		}
		return gVersion;
	}
	/**
	 * Get list of versions for given species.  Create it if it doesn't exist.
	 * @param speciesName
	 * @return list of versions for the given species.
	 */
	private List<GenericVersion> getSpeciesVersionList(String speciesName) {
		List<GenericVersion> gVersionList;
		if (!species2genericVersionList.containsKey(speciesName)) {
			gVersionList = new ArrayList<GenericVersion>();
			species2genericVersionList.put(speciesName, gVersionList);
		} else {
			gVersionList = species2genericVersionList.get(speciesName);
		}
		return gVersionList;
	}

	public List<GenericVersion> getGenericVersions(final String speciesName){
		return species2genericVersionList.get(speciesName);
	}
}
