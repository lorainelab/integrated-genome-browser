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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.AbstractAction;
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
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.window.service.IWindowService;

public class WindowServiceDefaultImpl implements IWindowService {

	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("window_service_def");
	private static final String TABBED_PANES_TITLE = "Tabbed Panes";
	private static final Map<Component, Frame> comp2window = new HashMap<Component, Frame>();
	private Set<IGBTabPanel> addedPlugins = new HashSet<IGBTabPanel>();
	private JMenuItem move_tab_to_window_item;
	private JMenuItem move_tabbed_panel_to_window_item;
	private JMenu tabs_menu;
	private JFrame frm;
	private JTabbedPane tab_pane;
	private JSplitPane splitpane;
	private Container cpane;
	private boolean focusSet;

	public WindowServiceDefaultImpl() {
		super();
		tab_pane = new JTabbedPane();
		focusSet = false;
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
		splitpane.setTopComponent(map_view);
	}

	@Override
	public void setViewMenu(JMenu view_menu) {
		move_tab_to_window_item = new JMenuItem(BUNDLE.getString("openCurrentTabInNewWindow"), KeyEvent.VK_O);
		move_tabbed_panel_to_window_item = new JMenuItem(BUNDLE.getString("openTabbedPanesInNewWindow"), KeyEvent.VK_P);
		tabs_menu = new JMenu(BUNDLE.getString("showTabs"));
		move_tab_to_window_item.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					movePaneToWindow();
				}
			}
		);
		move_tabbed_panel_to_window_item.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					moveTabbedPanelToWindow();
				}
			}
		);
		view_menu.addSeparator();
		MenuUtil.addToMenu(view_menu, move_tab_to_window_item);
		MenuUtil.addToMenu(view_menu, move_tabbed_panel_to_window_item);
		view_menu.add(tabs_menu);
	}

	private void openTabInNewWindow(final JTabbedPane tab_pane) {
		Runnable r = new Runnable() {

			public void run() {
				int index = tab_pane.getSelectedIndex();
				if (index < 0) {
					ErrorHandler.errorPanel("No more panes!");
					return;
				}
				final IGBTabPanel comp = (IGBTabPanel) tab_pane.getComponentAt(index);
				openCompInWindow(comp, tab_pane);
			}
		};
		SwingUtilities.invokeLater(r);
	}

	private void openCompInWindow(final IGBTabPanel comp, final JTabbedPane tab_pane) {
		final String name = comp.getName();
		final String display_name = comp.getDisplayName();

		Image temp_icon = null;
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

			Rectangle pos = PreferenceUtils.retrieveWindowLocation(name, frame.getBounds());
			if (pos != null) {
				PreferenceUtils.setWindowSize(frame, pos);
			}
			frame.setVisible(true);
			frame.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosing(WindowEvent evt) {
					// save the current size into the preferences, so the window
					// will re-open with this size next time
					PreferenceUtils.saveWindowLocation(frame, name);
					comp2window.remove(comp);
					cont.remove(comp);
					cont.validate();
					frame.dispose();
					showTab(comp);
					PreferenceUtils.saveComponentState(name, PreferenceUtils.COMPONENT_STATE_TAB);
				}
			});
		} // extra window already exists, but may not be visible
		else {
			DisplayUtils.bringFrameToFront(comp2window.get(comp));
		}
		PreferenceUtils.saveComponentState(name, PreferenceUtils.COMPONENT_STATE_WINDOW);
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

	/**
	 * Saves information about which plugins are in separate windows and
	 * what their preferred sizes are.
	 */
	private void saveWindowLocations() {
		// Save the main window location
		PreferenceUtils.saveWindowLocation(frm, "main window");

		for (IGBTabPanel comp : addedPlugins) {
			Frame f = comp2window.get(comp);
			if (f != null) {
				PreferenceUtils.saveWindowLocation(f, comp.getName());
			}
		}
		Frame f = comp2window.get(tab_pane);
		if (f != null) {
			PreferenceUtils.saveWindowLocation(f, TABBED_PANES_TITLE);
		}
	}

	private void showTab(final IGBTabPanel plugin) { // always show a hidden plugin in tab panel (not separate window)
		int index = 0;
		while (index < tab_pane.getTabCount() && plugin.compareTo((IGBTabPanel)tab_pane.getComponentAt(index)) > 0) {
			index++;
		}
		tab_pane.insertTab(plugin.getTitle(), plugin.getIcon(), plugin, plugin.getToolTipText(), index);
		if (plugin.isFocus() && !focusSet) {
			tab_pane.setSelectedIndex(index);
			focusSet = true;
		}
		PreferenceUtils.saveComponentState(plugin.getName(), PreferenceUtils.COMPONENT_STATE_TAB);
	}

	private void hideTab(IGBTabPanel tabPanel) {
		if (tabPanel == null) {
			return;
		}
		Frame f = comp2window.get(tabPanel);
		if (f == null) {
			String name = tabPanel.getName();
			for (int i = 0; i < tab_pane.getTabCount(); i++) {
				if (name.equals(((IGBTabPanel)tab_pane.getComponentAt(i)).getName())) {
					tab_pane.remove(i);
				}
			}
		}
		else {
			f.dispose();
		}
		PreferenceUtils.saveComponentState(tabPanel.getName(), PreferenceUtils.COMPONENT_STATE_HIDDEN);
	}

	public void addTab(final IGBTabPanel plugin) {
		addedPlugins.add(plugin);

		String title = plugin.getTitle();
		String tool_tip = plugin.getToolTipText();
		if (tool_tip == null) {
			tool_tip = title;
		}
		boolean in_a_window = (PreferenceUtils.getComponentState(plugin.getName()).equals(PreferenceUtils.COMPONENT_STATE_WINDOW));
		boolean in_a_tab = (PreferenceUtils.getComponentState(plugin.getName()).equals(PreferenceUtils.COMPONENT_STATE_TAB));
		boolean hidden = (PreferenceUtils.getComponentState(plugin.getName()).equals(PreferenceUtils.COMPONENT_STATE_HIDDEN));
		if (!(in_a_window || in_a_tab || hidden)) {
			PreferenceUtils.saveComponentState(plugin.getName(), PreferenceUtils.COMPONENT_STATE_TAB);
			in_a_tab = true;
		}
		if (plugin.isMain() && hidden) { // this should never happen, but ...
			PreferenceUtils.saveComponentState(plugin.getName(), PreferenceUtils.COMPONENT_STATE_TAB);
			in_a_tab = true;
		}
		if (in_a_window) {
			openCompInWindow(plugin, tab_pane);
		}
		else if (in_a_tab) {
			showTab(plugin);
		}
		final JCheckBoxMenuItem jCheckBoxMenuItem = new JCheckBoxMenuItem(plugin.getDisplayName());
		jCheckBoxMenuItem.setSelected(in_a_window || in_a_tab);
		if (plugin.isMain()) {
			jCheckBoxMenuItem.setEnabled(false);
		}
		else {
			jCheckBoxMenuItem.addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (jCheckBoxMenuItem.isSelected()) {
							showTab(plugin);
						}
						else {
							hideTab(plugin);
						}
					}
				}
			);
		}
		tabs_menu.add(jCheckBoxMenuItem);
	}

	public void removeTab(final IGBTabPanel plugin) {
		addedPlugins.remove(plugin);
		hideTab(plugin);
		for (Component item : Arrays.asList(tabs_menu.getMenuComponents())) {
			if (((JMenuItem)item).getText().equals(plugin.getDisplayName())) {
				tabs_menu.remove(item);
			}
		}
		PreferenceUtils.saveComponentState(plugin.getName(), null);
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
	public void startup() {
	}

	@Override
	public void shutdown() {
		saveWindowLocations();
	}

	@Override
	public Set<IGBTabPanel> getPlugins() {
		return addedPlugins;
	}
}
