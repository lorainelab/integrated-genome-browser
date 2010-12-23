package com.affymetrix.igb.window.service;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JPanel;

public interface IWindowService {
	public void startup();
	public void shutdown();
	public void setMainFrame(JFrame jFrame);
	public void setSeqMapView(JPanel jPanel);
	public void setViewMenu(JMenu view_menu);
	public void addPlugIn(JComponent plugin, String name, String title, int position);
	public boolean removePlugIn(String name);
	public void setStatusBar(JComponent status_bar);
	public JComponent getView(String viewName);
}
