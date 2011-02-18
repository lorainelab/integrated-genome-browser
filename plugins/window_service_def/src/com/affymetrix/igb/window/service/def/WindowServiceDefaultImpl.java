package com.affymetrix.igb.window.service.def;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.TabState;
import com.affymetrix.igb.window.service.IWindowService;

public class WindowServiceDefaultImpl implements IWindowService, TabStateHandler {

	private class TabStateMenuItem extends JRadioButtonMenuItem {
		private static final long serialVersionUID = 1L;
		private final TabState tabState;
		private TabStateMenuItem(final IGBTabPanel igbTabPanel, TabState _tabState) {
			super(BUNDLE.getString(_tabState.getName()));
			tabState = _tabState;
		    addActionListener(
				new ActionListener() {
					TabState state = tabState;
					@Override
					public void actionPerformed(ActionEvent e) {
						setTabState(igbTabPanel, state);
					}
				}
			);
		}
		public TabState getTabState() {
			return tabState;
		}
	}

	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("window_service_def");
	private JMenu tabs_menu;
	private JFrame frm;
	private Map<TabState, TabHolder> tabHolders;
	private Map<IGBTabPanel, JMenu> tabMenus;
	private Container cpane;
	private boolean initialized;
	private boolean focusSet;

	public WindowServiceDefaultImpl() {
		super();
		initialized = false;
		tabHolders = new HashMap<TabState, TabHolder>();
		tabHolders.put(TabState.COMPONENT_STATE_WINDOW, new WindowTabs(this));
		tabHolders.put(TabState.COMPONENT_STATE_HIDDEN, new HiddenTabs());
		tabMenus = new HashMap<IGBTabPanel, JMenu>();
		focusSet = false;
	}

	@Override
	public void setMainFrame(JFrame jFrame) {
		frm = jFrame;
		cpane = frm.getContentPane();
		cpane.setLayout(new BorderLayout());
		frm.addComponentListener(new ComponentListener() 
		{  
		        public void componentResized(ComponentEvent evt) {
		    		for (TabState tabState : tabHolders.keySet()) {
		    			tabHolders.get(tabState).resize();
		    		}
		        }

				@Override
				public void componentMoved(ComponentEvent e) {}

				@Override
				public void componentShown(ComponentEvent e) {}

				@Override
				public void componentHidden(ComponentEvent e) {}
		});
	}

	@Override
	public void setStatusBar(JComponent status_bar) {
		cpane.add(status_bar, BorderLayout.SOUTH);
	}

	@Override
	public void setSeqMapView(JPanel map_view) {
		JTabbedTrayPane left_pane = new JTabbedTrayLeftPane(map_view);
		tabHolders.put(TabState.COMPONENT_STATE_LEFT_TAB, left_pane);
		JTabbedTrayPane right_pane = new JTabbedTrayRightPane(left_pane);
		tabHolders.put(TabState.COMPONENT_STATE_RIGHT_TAB, right_pane);
		JTabbedTrayPane bottom_pane = new JTabbedTrayBottomPane(right_pane);
		tabHolders.put(TabState.COMPONENT_STATE_BOTTOM_TAB, bottom_pane);
		cpane.add("Center", bottom_pane);
	}

	@Override
	public void setViewMenu(JMenu view_menu) {
		tabs_menu = new JMenu(BUNDLE.getString("showTabs"));
		view_menu.addSeparator();
		view_menu.add(tabs_menu);
	}

	/**
	 * Saves information about which plugins are in separate windows and
	 * what their preferred sizes are.
	 */
	private void saveWindowLocations() {
		// Save the main window location
		PreferenceUtils.saveWindowLocation(frm, "main window");

		for (IGBTabPanel comp : tabHolders.get(TabState.COMPONENT_STATE_WINDOW).getPlugins()) {
			PreferenceUtils.saveWindowLocation(comp.getFrame(), comp.getName());
		}
	}

	public void addTab(final IGBTabPanel plugin) {
		if (!initialized) {
			for (TabHolder tabHolder : tabHolders.values()) {
				tabHolder.init();
			}
			initialized = true;
		}
		TabState tabState = TabState.getDefaultTabState();
		String tabStateString = PreferenceUtils.getComponentState(plugin.getName());
		if (tabStateString == null) {
			tabState = plugin.getDefaultState();
		}
		else {
			tabState = TabState.getTabStateByName(tabStateString);
		}
		setTabState(plugin, tabState);
		JMenu pluginMenu = new JMenu(plugin.getDisplayName());
		tabMenus.put(plugin, pluginMenu);
		ButtonGroup group = new ButtonGroup();

		for (TabState tabStateLoop : TabState.values()) {
		    JRadioButtonMenuItem menuItem = new TabStateMenuItem(plugin, tabStateLoop);
		    group.add(menuItem);
		    pluginMenu.add(menuItem);
		}
		setTabMenu(plugin);
		tabs_menu.add(pluginMenu);
	}

	private void setTabMenu(final IGBTabPanel plugin) {
		JMenu menu = tabMenus.get(plugin);
		TabState tabState = getTabState(plugin);
		for (int i = 0; i < menu.getItemCount(); i++) {
			TabStateMenuItem menuItem = (TabStateMenuItem)menu.getItem(i);
		    menuItem.setSelected(menuItem.getTabState() == tabState);
		}
	}

	public void removeTab(final IGBTabPanel plugin) {
		for (TabState tabState : tabHolders.keySet()) {
			tabHolders.get(tabState).removeTab(plugin);
		}
		for (Component item : Arrays.asList(tabs_menu.getMenuComponents())) {
			if (((JMenuItem)item).getText().equals(plugin.getDisplayName())) {
				tabs_menu.remove(item);
			}
		}
		PreferenceUtils.saveComponentState(plugin.getName(), null);
	}

	private void setTabState(IGBTabPanel panel, TabState tabState) {
		TabState oldTabState = getTabState(panel);
		if (oldTabState != null) {
			tabHolders.get(oldTabState).removeTab(panel);
		}
		if (tabState == null) {
			removeTab(panel);
		}
		else {
			boolean setFocus = false;
			if (panel.isFocus() && !focusSet) {
				setFocus = true;
				focusSet = true;
			}
			tabHolders.get(tabState).addTab(panel, setFocus);
		}
		PreferenceUtils.saveComponentState(panel.getName(), tabState.getName());
	}

	@Override
	public void startup() {
	}

	@Override
	public void shutdown() {
		saveWindowLocations();
	}

	@Override
	public void setDefaultState(IGBTabPanel panel) {
		setTabState(panel, TabState.getDefaultTabState());
		setTabMenu(panel);
	}

	private TabState getTabState(IGBTabPanel panel) {
		for (TabState tabState : tabHolders.keySet()) {
			if (tabHolders.get(tabState).getPlugins().contains(panel)) {
				return tabState;
			}
		}
		return null;
	}

	@Override
	public Set<IGBTabPanel> getPlugins() {
		HashSet<IGBTabPanel> plugins = new HashSet<IGBTabPanel>();
		for (TabState tabState : tabHolders.keySet()) {
			plugins.addAll(tabHolders.get(tabState).getPlugins());
		}
		return plugins;
	}
}
