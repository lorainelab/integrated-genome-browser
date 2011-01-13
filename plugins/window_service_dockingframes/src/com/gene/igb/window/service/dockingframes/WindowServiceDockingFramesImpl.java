package com.gene.igb.window.service.dockingframes;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JPanel;

import bibliothek.gui.dock.StackDockStation;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CLocation;
import bibliothek.gui.dock.common.CWorkingArea;
import bibliothek.gui.dock.common.DefaultSingleCDockable;

import com.affymetrix.igb.window.service.IPlugin;
import com.affymetrix.igb.window.service.PluginInfo;
import com.affymetrix.igb.window.service.IWindowService;

public class WindowServiceDockingFramesImpl implements IWindowService {

	private final Map<Class<?>, IPlugin> plugin_hash = new HashMap<Class<?>, IPlugin>();
	private final Map<Component, PluginInfo> comp2plugin = new HashMap<Component, PluginInfo>();
	private HashMap<String, JComponent> addedPlugins = new HashMap<String, JComponent>();
	private final List<JComponent> plugins = new ArrayList<JComponent>(16);
	private JPanel map_view;
	private CControl cControl;
    private CWorkingArea workingArea;
    private int focusIndex = -1;
    private DefaultSingleCDockable focusCDockable;
    private List<DefaultSingleCDockable> saveCDockables = new ArrayList<DefaultSingleCDockable>();

	public WindowServiceDockingFramesImpl() {
		super();
		workingArea = null;
	}

	@Override
	public void setMainFrame(JFrame frame) {
		cControl = new CControl(frame);
        frame.getContentPane().add( cControl.getContentArea() );
		frame.add(cControl.getContentArea()) ;
		frame.setVisible(true) ;
        workingArea = cControl.createWorkingArea( "plugins" );
        workingArea.setLocation( CLocation.base().normalRectangle( 0, 0, 1, 1 ) );
        workingArea.setVisible( true );
		for (DefaultSingleCDockable cDockable : saveCDockables) {
			addDockable(cDockable, cDockable == focusCDockable);
		}
		saveCDockables.clear();
	}

	@Override
	public void setStatusBar(JComponent status_bar) {
		DefaultSingleCDockable statusBarDockable = new DefaultSingleCDockable ("StatusBar", status_bar);
		statusBarDockable.setTitleShown(false);
		cControl.add(statusBarDockable);
		statusBarDockable.setLocation( CLocation.base().normalSouth( 0.01 ) );
		statusBarDockable.setVisible( true );
	}

	@Override
	public void setSeqMapView(JPanel map_view_) {
		map_view = map_view_;
		DefaultSingleCDockable seqMapViewDockable = new DefaultSingleCDockable ("SeqMapView", map_view);
		seqMapViewDockable.setTitleShown(false);
		cControl.add(seqMapViewDockable);
		seqMapViewDockable.setLocation( CLocation.base().normalNorth( 0.7 ) );
		seqMapViewDockable.setVisible( true );
	}

	private void setPluginInstance(Class<?> c, IPlugin plugin) {
		plugin_hash.put(c, plugin);
		plugin.putPluginProperty(IPlugin.TEXT_KEY_APP, this);
		plugin.putPluginProperty(IPlugin.TEXT_KEY_SEQ_MAP_VIEW, map_view);
	}

	private void addDockable(DefaultSingleCDockable page, boolean focus) {
		workingArea.add( page );
        page.setVisible( true );
        System.out.println("count = " + workingArea.getStation().getDockableCount());
        if (workingArea.getStation().getDockableCount() >= 1 && workingArea.getStation().getDockable(0) instanceof StackDockStation) {
    		StackDockStation stackDockStation = (StackDockStation)workingArea.getStation().getDockable(0);
        	if (focus) {
				focusIndex = stackDockStation.getDockableCount() - 1;
        	}
        	if (focusIndex != -1) {
				stackDockStation.getStackComponent().setSelectedIndex(focusIndex);
        	}
        }
	}

	private void loadPlugIn(PluginInfo pi, Object plugin) {
		if (plugin instanceof IPlugin) {
			IPlugin plugin_view = (IPlugin) plugin;
			setPluginInstance(plugin_view.getClass(), plugin_view);
		}

		String title = pi.getDisplayName();
		if (plugin instanceof JComponent) {
			comp2plugin.put((Component) plugin, pi);
			String tool_tip = ((JComponent) plugin).getToolTipText();
			if (tool_tip == null) {
				tool_tip = title;
			}
		}
		JComponent comp = (JComponent) plugin;
		DefaultSingleCDockable page = new DefaultSingleCDockable(title, title, comp);
		boolean focus = pi.getDefaultPosition() == 0;
//		page.setTitleShown(false);
		if (workingArea == null) {
			saveCDockables.add(page);
			if (focus) {
				focusCDockable = page;
			}
		}
		else {
			addDockable(page, focus);
		}
	}

	@Override
	public void addPlugIn(JComponent plugIn, String name, String title, int position) {
		loadPlugIn(new PluginInfo(plugIn.getClass().getName(), name, true, position), plugIn);
		addedPlugins.put(name, plugIn);
		plugins.add(plugIn);
	}

	@Override
	public boolean removePlugIn(String name) {
		return false;
	}

	@Override
	public void startup() {
	}

	@Override
	public void shutdown() {
	}

	@Override
	public List<JComponent> getPlugins() {
		return plugins;
	}

	@Override
	public void setViewMenu(JMenu view_menu) {}

}
