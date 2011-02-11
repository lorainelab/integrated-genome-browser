package com.affymetrix.igb.window.service;

import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JPanel;

import com.affymetrix.igb.osgi.service.ExtensionPoint;
import com.affymetrix.igb.osgi.service.ExtensionPointRegistry;
import com.affymetrix.igb.osgi.service.IGBTabPanel;

public interface IWindowService {
	public void startup();
	public void shutdown();
	public void setMainFrame(JFrame jFrame);
	public void setSeqMapView(JPanel jPanel);
	public void setViewMenu(JMenu view_menu);
	public void setStatusBar(JComponent status_bar);
	public Set<IGBTabPanel> getPlugins();
	public ExtensionPoint<IGBTabPanel> setExtensionPointRegistry(ExtensionPointRegistry registry);
}
