package com.affymetrix.igb.osgi.service;

import javax.swing.JComponent;
import javax.swing.JMenu;

public interface IGBService {
	public String[] getRequiredBundles();
	public boolean addMenu(JMenu menu);
	public boolean removeMenu(String menuName);
	public void addPlugIn(JComponent plugin, String name);
	public boolean removePlugIn(String name);
	public void displayError(String title, String errorText);
	public void displayError(String errorText);
}
