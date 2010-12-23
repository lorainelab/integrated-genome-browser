package com.affymetrix.igb.window.service.def;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import com.affymetrix.genometryImpl.util.DisplayUtils;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.window.service.IPlugin;
import com.affymetrix.igb.window.service.PluginInfo;
import com.affymetrix.igb.window.service.IWindowService;

public class WindowServiceDefaultImpl implements IWindowService, ActionListener {

	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("window_service_def");
	private static final String TABBED_PANES_TITLE = "Tabbed Panes";
	private static final Map<Component, Frame> comp2window = new HashMap<Component, Frame>();
	private final Map<Class<?>, IPlugin> plugin_hash = new HashMap<Class<?>, IPlugin>();
	private final Map<Component, PluginInfo> comp2plugin = new HashMap<Component, PluginInfo>();
	private final Map<Component, JCheckBoxMenuItem> comp2menu_item = new HashMap<Component, JCheckBoxMenuItem>();
	private HashMap<String, JComponent> addedPlugins = new HashMap<String, JComponent>();
	private final List<Object> plugins = new ArrayList<Object>(16);
	private JMenuItem move_tab_to_window_item;
	private JMenuItem move_tabbed_panel_to_window_item;
	private JFrame frm;
	private JTabbedPane tab_pane;
	private JSplitPane splitpane;
	private JPanel map_view;
	private Container cpane;

	public WindowServiceDefaultImpl() {
		super();
		tab_pane = new JTabbedPane();
	}

	@Override
	public void setMainFrame(JFrame jFrame) {
		frm = jFrame;
		cpane = frm.getContentPane();
		int table_height = 250;
		int fudge = 55;

		cpane.setLayout(new BorderLayout());
		splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitpane.setOneTouchExpandable(true);
		splitpane.setDividerSize(8);
		splitpane.setDividerLocation(frm.getHeight() - (table_height + fudge));

		boolean tab_panel_in_a_window = (PreferenceUtils.getComponentState(TABBED_PANES_TITLE).equals(PreferenceUtils.COMPONENT_STATE_WINDOW));
		if (tab_panel_in_a_window) {
			openTabbedPanelInNewWindow(tab_pane);
		} else {
			splitpane.setBottomComponent(tab_pane);
		}

		cpane.add("Center", splitpane);

		// Using JTabbedPane.SCROLL_TAB_LAYOUT makes it impossible to add a
		// pop-up menu (or any other mouse listener) on the tab handles.
		// (A pop-up with "Open tab in a new window" would be nice.)
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4465870
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4499556
		tab_pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tab_pane.setMinimumSize(new Dimension(0, 0));
	
	}

	@Override
	public void setStatusBar(JComponent status_bar) {
		cpane.add(status_bar, BorderLayout.SOUTH);
	}

	@Override
	public void setSeqMapView(JPanel map_view) {
		this.map_view = map_view;
		splitpane.setTopComponent(map_view);
	}

	@Override
	public void setViewMenu(JMenu view_menu) {
		move_tab_to_window_item = new JMenuItem(BUNDLE.getString("openCurrentTabInNewWindow"), KeyEvent.VK_O);
		move_tabbed_panel_to_window_item = new JMenuItem(BUNDLE.getString("openTabbedPanesInNewWindow"), KeyEvent.VK_P);
		move_tab_to_window_item.addActionListener(this);
		move_tabbed_panel_to_window_item.addActionListener(this);
		view_menu.addSeparator();
		MenuUtil.addToMenu(view_menu, move_tab_to_window_item);
		MenuUtil.addToMenu(view_menu, move_tabbed_panel_to_window_item);
	}

	private void openTabInNewWindow(final JTabbedPane tab_pane) {
		Runnable r = new Runnable() {

			public void run() {
				int index = tab_pane.getSelectedIndex();
				if (index < 0) {
					ErrorHandler.errorPanel("No more panes!");
					return;
				}
				final JComponent comp = (JComponent) tab_pane.getComponentAt(index);
				openCompInWindow(comp, tab_pane);
			}
		};
		SwingUtilities.invokeLater(r);
	}

	private void openCompInWindow(final JComponent comp, final JTabbedPane tab_pane) {
		final String title;
		final String display_name;
		final String tool_tip = comp.getToolTipText();

		if (comp2plugin.get(comp) instanceof PluginInfo) {
			PluginInfo pi = comp2plugin.get(comp);
			title = pi.getPluginName();
			display_name = pi.getDisplayName();
		} else {
			title = comp.getName();
			display_name = comp.getName();
		}

		Image temp_icon = null;
		if (comp instanceof IPlugin) {
			IPlugin pv = (IPlugin) comp;
			ImageIcon image_icon = (ImageIcon) pv.getPluginProperty(IPlugin.TEXT_KEY_ICON);
			if (image_icon != null) {
				temp_icon = image_icon.getImage();
			}
		}
		if (temp_icon == null) {
			temp_icon = getIcon();
		}

		// If not already open in a new window, make a new window
		if (comp2window.get(comp) == null) {
			tab_pane.remove(comp);
			tab_pane.validate();

			final JFrame frame = new JFrame(display_name);
			final Image icon = temp_icon;
			if (icon != null) {
				frame.setIconImage(icon);
			}
			final Container cont = frame.getContentPane();
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

			cont.add(comp);
			comp.setVisible(true);
			comp2window.put(comp, frame);
			frame.pack(); // pack() to set frame to its preferred size

			Rectangle pos = PreferenceUtils.retrieveWindowLocation(title, frame.getBounds());
			if (pos != null) {
				PreferenceUtils.setWindowSize(frame, pos);
			}
			frame.setVisible(true);
			frame.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosing(WindowEvent evt) {
					// save the current size into the preferences, so the window
					// will re-open with this size next time
					PreferenceUtils.saveWindowLocation(frame, title);
					comp2window.remove(comp);
					cont.remove(comp);
					cont.validate();
					frame.dispose();
					tab_pane.addTab(display_name, null, comp, (tool_tip == null ? display_name : tool_tip));
					PreferenceUtils.saveComponentState(title, PreferenceUtils.COMPONENT_STATE_TAB);
					JCheckBoxMenuItem menu_item = comp2menu_item.get(comp);
					if (menu_item != null) {
						menu_item.setSelected(false);
					}
				}
			});
		} // extra window already exists, but may not be visible
		else {
			DisplayUtils.bringFrameToFront(comp2window.get(comp));
		}
		PreferenceUtils.saveComponentState(title, PreferenceUtils.COMPONENT_STATE_WINDOW);
	}

	private void openTabbedPanelInNewWindow(final JComponent comp) {

		final String title = TABBED_PANES_TITLE;
		final String display_name = title;

		// If not already open in a new window, make a new window
		if (comp2window.get(comp) == null) {
			splitpane.remove(comp);
			splitpane.validate();

			final JFrame frame = new JFrame(display_name);
			final Image icon = getIcon();
			if (icon != null) {
				frame.setIconImage(icon);
			}
			final Container cont = frame.getContentPane();
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

			cont.add(comp);
			comp.setVisible(true);
			comp2window.put(comp, frame);
			frame.pack(); // pack() to set frame to its preferred size

			Rectangle pos = PreferenceUtils.retrieveWindowLocation(title, frame.getBounds());
			if (pos != null) {
				//check that it's not too small, problems with using two screens
				int posW = (int) pos.getWidth();
				if (posW < 650) {
					posW = 650;
				}
				int posH = (int) pos.getHeight();
				if (posH < 300) {
					posH = 300;
				}
				pos.setSize(posW, posH);
				PreferenceUtils.setWindowSize(frame, pos);
			}
			frame.setVisible(true);

			final Runnable return_panes_to_main_window = new Runnable() {

				public void run() {
					// save the current size into the preferences, so the window
					// will re-open with this size next time
					PreferenceUtils.saveWindowLocation(frame, title);
					comp2window.remove(comp);
					cont.remove(comp);
					cont.validate();
					frame.dispose();
					splitpane.setBottomComponent(comp);
					splitpane.setDividerLocation(0.70);
					PreferenceUtils.saveComponentState(title, PreferenceUtils.COMPONENT_STATE_TAB);
					JCheckBoxMenuItem menu_item = comp2menu_item.get(comp);
					if (menu_item != null) {
						menu_item.setSelected(false);
					}
				}
			};

			frame.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosing(WindowEvent evt) {
					SwingUtilities.invokeLater(return_panes_to_main_window);
				}
			});

			JMenuBar mBar = new JMenuBar();
			frame.setJMenuBar(mBar);
			JMenu menu1 = new JMenu("Windows");
			menu1.setMnemonic('W');
			mBar.add(menu1);

			menu1.add(new AbstractAction("Return Tabbed Panes to Main Window") {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent evt) {
					SwingUtilities.invokeLater(return_panes_to_main_window);
				}
			});
			menu1.add(new AbstractAction("Open Current Tab in New Window") {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent evt) {
					openTabInNewWindow(tab_pane);
				}
			});
		} // extra window already exists, but may not be visible
		else {
			DisplayUtils.bringFrameToFront(comp2window.get(comp));
		}
		PreferenceUtils.saveComponentState(title, PreferenceUtils.COMPONENT_STATE_WINDOW);
	}

	private void addToPopupWindows(final JComponent comp, final String title) {
		JCheckBoxMenuItem popupMI = new JCheckBoxMenuItem(title);
		popupMI.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				JCheckBoxMenuItem src = (JCheckBoxMenuItem) evt.getSource();
				Frame frame = comp2window.get(comp);
				if (frame == null) {
					openCompInWindow(comp, tab_pane);
					src.setSelected(true);
				}
			}
		});
		comp2menu_item.put(comp, popupMI);
	}

	/**
	 * Saves information about which plugins are in separate windows and
	 * what their preferred sizes are.
	 */
	private void saveWindowLocations() {
		// Save the main window location
		PreferenceUtils.saveWindowLocation(frm, "main window");

		for (Component comp : comp2plugin.keySet()) {
			Frame f = comp2window.get(comp);
			if (f != null) {
				PluginInfo pi = comp2plugin.get(comp);
				PreferenceUtils.saveWindowLocation(f, pi.getPluginName());
			}
		}
		Frame f = comp2window.get(tab_pane);
		if (f != null) {
			PreferenceUtils.saveWindowLocation(f, TABBED_PANES_TITLE);
		}
	}

	private void setPluginInstance(Class<?> c, IPlugin plugin) {
		plugin_hash.put(c, plugin);
		plugin.putPluginProperty(IPlugin.TEXT_KEY_APP, this);
		plugin.putPluginProperty(IPlugin.TEXT_KEY_SEQ_MAP_VIEW, map_view);
	}

	private void loadPlugIn(PluginInfo pi, Object plugin, int position) {
		ImageIcon icon = null;

		if (plugin instanceof IPlugin) {
			IPlugin plugin_view = (IPlugin) plugin;
			this.setPluginInstance(plugin_view.getClass(), plugin_view);
			icon = (ImageIcon) plugin_view.getPluginProperty(IPlugin.TEXT_KEY_ICON);
		}

		if (plugin instanceof JComponent) {

			comp2plugin.put((Component) plugin, pi);
			String title = pi.getDisplayName();
			String tool_tip = ((JComponent) plugin).getToolTipText();
			if (tool_tip == null) {
				tool_tip = title;
			}
			JComponent comp = (JComponent) plugin;
			boolean in_a_window = (PreferenceUtils.getComponentState(title).equals(PreferenceUtils.COMPONENT_STATE_WINDOW));
			addToPopupWindows(comp, title);
			JCheckBoxMenuItem menu_item = comp2menu_item.get(comp);
			menu_item.setSelected(in_a_window);
			if (in_a_window) {
				openCompInWindow(comp, tab_pane);
			} else if (position == -1) {
				tab_pane.addTab(title, icon, comp, tool_tip);
			} else {
				tab_pane.insertTab(title, icon, comp, tool_tip, position);
			}
			if (position == 0) {
				tab_pane.setSelectedIndex(0);
			}
		}
	}

	/** Returns the icon stored in the jar file.
	 *  It is expected to be at com.affymetrix.igb.igb.gif.
	 *  @return null if the image file is not found or can't be opened.
	 */
	private Image getIcon() {
		Image icon = null;
		try {
			URL url = WindowServiceDefaultImpl.class.getResource("igb.gif");
			if (url != null) {
				icon = Toolkit.getDefaultToolkit().getImage(url);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// It isn't a big deal if we can't find the icon, just return null
		}
		return icon;
	}

	private void movePaneToWindow() {
		openTabInNewWindow(tab_pane);
	}

	private void moveTabbedPanelToWindow() {
		openTabbedPanelInNewWindow(tab_pane);
	}

	@Override
	public void addPlugIn(JComponent plugIn, String name, String title, int position) {
		loadPlugIn(new PluginInfo(plugIn.getClass().getName(), name, true, position), plugIn, position);
		addedPlugins.put(name, plugIn);
		plugins.add(plugIn);
	}

	@Override
	public boolean removePlugIn(String name) {
		if (name == null) {
			return false;
		}
		JComponent plugIn = addedPlugins.get(name);
		Frame frame = comp2window.get(plugIn);
		if (frame == null) {
			for (int i = 0; i < tab_pane.getTabCount(); i++) {
				if (name.equals(tab_pane.getTitleAt(i))) {
					tab_pane.remove(i);
					return true;
				}
			}
		}
		else {
			frame.dispose();
			comp2window.remove(plugIn);
		}
		PreferenceUtils.saveComponentState(name, PreferenceUtils.COMPONENT_STATE_TAB); // default - can't delete state
		return false;
	}

	@Override
	public void startup() {
	}

	@Override
	public void shutdown() {
		saveWindowLocations();
	}

	@Override
	public JComponent getView(String viewName) {
		Class<?> viewClass;
		try {
			viewClass = Class.forName(viewName);
		}
		catch (ClassNotFoundException x) {
			System.out.println("IGBServiceImpl.getView() failed for " + viewName);
			return null;
		}
		for (Object plugin : plugins) {
			if (viewClass.isAssignableFrom(plugin.getClass())) {
				return (JComponent)plugin;
			}
		}
		return null;
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		if (src == move_tab_to_window_item) {
			movePaneToWindow();
		} else if (src == move_tabbed_panel_to_window_item) {
			moveTabbedPanelToWindow();
		}
	}
}
