package com.affymetrix.igb.window.service;

import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JPanel;

import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IGBTabPanel.TabState;

public interface IWindowService {
	public void startup();
	public void shutdown();
	public void setMainFrame(JFrame jFrame);
	public void setSeqMapView(JPanel jPanel);
	public void setViewMenu(JMenu view_menu);
	public void setStatusBar(JComponent status_bar);
	public Set<IGBTabPanel> getPlugins();
	public void setTabStateAndMenu(IGBTabPanel panel, TabState tabState);
}
