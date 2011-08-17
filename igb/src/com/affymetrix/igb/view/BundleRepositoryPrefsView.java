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
package com.affymetrix.igb.view;

import java.awt.Dimension;

import javax.swing.GroupLayout;
import javax.swing.SwingConstants;
import javax.swing.GroupLayout.Group;

import com.affymetrix.genoviz.swing.recordplayback.JRPTextField;
import com.affymetrix.igb.general.ServerList;

public class BundleRepositoryPrefsView extends ServerPrefsView {
	private static final long serialVersionUID = 1L;

	public BundleRepositoryPrefsView() {
		super(ServerList.getRepositoryInstance());
	
		layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(sourcePanel));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(sourcePanel));
	}

	@Override
	protected Group addServerComponents(Group group1, Group group2) {
		return group1
		.addComponent(sourcesScrollPane)
		.addGroup(group2
			.addComponent(addServerButton)
			.addComponent(removeServerButton));
	}

	@Override
	protected Group getServerButtons(Group group) {
		return group.addComponent(addServerButton)
		.addComponent(removeServerButton);
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
	protected Group addAddSourceComponents(AddSourceGroupCreator addSourceGroupCreator, GroupLayout layout) {
		return addSourceGroupCreator.createGroup1(layout)
		.addComponent(messageContainer)
		.addGroup(addSourceGroupCreator.createGroup2(layout)
			.addComponent(nameLabel)
			.addComponent(name))
		.addGroup(addSourceGroupCreator.createGroup2(layout)
			.addComponent(urlLabel)
			.addComponent(url));
	}

	@Override
	protected void addDataSource() {
		addDataSource(null, name.getText(), url.getText());
	}

	@Override
	protected void setSize(GroupLayout layout, JRPTextField name) {
		layout.linkSize(nameLabel, urlLabel);
		name.setPreferredSize(new Dimension(300, name.getPreferredSize().height));
		layout.linkSize(name);
		layout.linkSize(SwingConstants.VERTICAL, name, url);
	}
}
