/**
 *   Copyright (c) 2010 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.prefs;

import com.affymetrix.genometry.util.ServerTypeI;
import com.affymetrix.igb.general.ServerList;
import javax.swing.GroupLayout.Group;

public class BundleRepositoryPrefsView extends ServerPrefsView {

	private static final long serialVersionUID = 1L;
	private static BundleRepositoryPrefsView singleton;

	public static synchronized BundleRepositoryPrefsView getSingleton() {
		if (singleton == null) {
			singleton = new BundleRepositoryPrefsView();
		}
		return singleton;
	}

	private BundleRepositoryPrefsView() {
		super(ServerList.getRepositoryInstance());

		layout.setHorizontalGroup(layout.createParallelGroup().addComponent(sourcePanel));

		layout.setVerticalGroup(layout.createSequentialGroup().addComponent(sourcePanel));
	}

	@Override
	protected Group addServerComponents(Group group1, Group group2) {
		return group1.addComponent(sourcesScrollPane).addGroup(group2.addComponent(addServerButton).addComponent(removeServerButton));
	}

	@Override
	protected Group getServerButtons(Group group) {
		return group.addComponent(addServerButton).addComponent(removeServerButton);
	}

	@Override
	protected String getViewName() {
		return "Plugin Repositories";
	}

	@Override
	protected String getToolTip() {
		return "Edit plugin repositories and preferences";
	}

	@Override
	protected boolean isSortable() {
		return true;
	}

	@Override
	protected boolean enableCombo() {
		return false;
	}
	
	@Override
	protected void updateSource(String url, ServerTypeI type, String name, String newUrl, String mirrorURL){
		ServerList.getServerInstance().removeServer(url);
		ServerList.getServerInstance().removeServerFromPrefs(url);
		addDataSource(type, name, url, -1, false, mirrorURL);
	}
}
