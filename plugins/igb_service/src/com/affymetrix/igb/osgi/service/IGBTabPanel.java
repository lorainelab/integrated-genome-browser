package com.affymetrix.igb.osgi.service;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public abstract class IGBTabPanel extends JPanel implements Comparable<IGBTabPanel> {
	private static final long serialVersionUID = 1L;

	protected final IGBService igbService;
	private final String displayName;
	private final String title;
	private final boolean main;
	private String state;
	private JTabbedPane tab_pane;
	private JFrame frame;

	public IGBTabPanel(IGBService igbService, String displayName, String title, boolean main) {
		super();
		this.igbService = igbService;
		this.displayName = displayName;
		this.title = title;
		this.main = main;
	}

	public String getName() {
		return getClass().getName();
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getTitle() {
		return title;
	}

	public boolean isMain() {
		return main;
	}

	public boolean isFocus() {
		return false;
	}

	public String getState() {
		return state;
	}

	public Icon getIcon() {
		return null;
	}

	public void setState(String state) {
		this.state = state;
	}

	public void setTabPane(JTabbedPane tab_pane) {
		this.tab_pane = tab_pane;
	}

	@Override
	public String toString() {
		return "IGBTabPanel: " + "displayName = " + displayName + ", class = " + this.getClass().getName();
	}

	@Override
	public int compareTo(IGBTabPanel o) {
		return this.getDisplayName().compareTo(o.getDisplayName());
	}
}
