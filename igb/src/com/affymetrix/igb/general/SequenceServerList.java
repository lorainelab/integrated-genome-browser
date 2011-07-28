package com.affymetrix.igb.general;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.prefs.Preferences;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.ServerUtils;

public class SequenceServerList extends ServerList {
	public SequenceServerList() {
		super("sequenceServer");
	}
	protected Preferences getPreferencesNode() {
		return PreferenceUtils.getSequenceServersNode();
	}

	private int getServerOrder(GenericServer server) {
		String url = GeneralUtils.URLEncode(ServerUtils.formatURL(server.URL, server.serverType));
		return Integer.parseInt(PreferenceUtils.getSequenceServersNode().node(url).get("order", "0"));
	}

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
}
