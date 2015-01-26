package com.affymetrix.igb.window.service.def;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.swing.MenuUtil;
import com.affymetrix.igb.swing.JRPMenu;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.swing.JRPRadioButtonMenuItem;
import com.affymetrix.igb.service.api.IGBTabPanel;
import com.affymetrix.igb.service.api.TabHolder;
import com.affymetrix.igb.service.api.IGBTabPanel.TabState;
import com.affymetrix.igb.service.api.IWindowRoutine;
import com.affymetrix.igb.window.service.IMenuCreator;
import com.affymetrix.igb.window.service.IWindowService;
import com.affymetrix.igb.window.service.def.JTabbedTrayPane.TrayState;
import java.util.EnumMap;

public class WindowServiceDefaultImpl implements IWindowService, TabStateHandler, TrayStateChangeListener {

    private class TabStateMenuItem extends JRPRadioButtonMenuItem {

        private static final long serialVersionUID = 1L;
        private final TabState tabState;

        private TabStateMenuItem(String id, final IGBTabPanel igbTabPanel, TabState _tabState) {
            super(id, BUNDLE.getString(_tabState.name()));
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

        public void actionPerformed(ActionEvent evt) {
            fireActionPerformed(evt);
        }
    }

    private class ActionWrapper extends AbstractAction {

        private final TabStateMenuItem menuItem;
        private final ButtonGroup group;

        ActionWrapper(TabStateMenuItem menuItem, ButtonGroup group) {
            super(menuItem.getText());
            this.menuItem = menuItem;
            this.group = group;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            group.setSelected(menuItem.getModel(), true);
            menuItem.actionPerformed(evt);
        }
    }

    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("window_service_def");
    private final Map<TabState, JRPMenuItem> move_tab_to_window_items;
    private final Map<TabState, JRPMenuItem> move_tabbed_panel_to_window_items;
    private JRPMenu tabs_menu;
    private JFrame frame;
    private Map<TabState, TabHolder> tabHolders;
    private Map<IGBTabPanel, JMenu> tabMenus;
    private Map<JMenu, Integer> tabMenuPositions;
    private HashSet<IWindowRoutine> stopRoutines;
    private Container cpane;
    private JPanel innerPanel;
    private boolean tabSeparatorSet = false;

    public WindowServiceDefaultImpl() {
        super();
        move_tab_to_window_items = new EnumMap<>(TabState.class);
        move_tabbed_panel_to_window_items = new EnumMap<>(TabState.class);
        tabHolders = new EnumMap<>(TabState.class);
        tabHolders.put(TabState.COMPONENT_STATE_WINDOW, new WindowTabs(this));
        tabHolders.put(TabState.COMPONENT_STATE_HIDDEN, new HiddenTabs());
        tabMenus = new HashMap<>();
        tabMenuPositions = new HashMap<>();
        stopRoutines = new HashSet<>();
    }

    @Override
    public void setMainFrame(JFrame jFrame) {
        frame = jFrame;
        cpane = frame.getContentPane();
        cpane.setLayout(new BorderLayout());
        innerPanel = new JPanel();
        innerPanel.setLayout(new BorderLayout());
        cpane.add(innerPanel, BorderLayout.CENTER);
    }

    @Override
    public void setStatusBar(JComponent status_bar) {
        innerPanel.add(status_bar, BorderLayout.SOUTH);
    }

    @Override
    public void setToolBar(JToolBar tool_bar) {
        cpane.add(tool_bar, BorderLayout.NORTH);
    }

    @Override
    public void setTopComponent1(JComponent topComponent1) {
        innerPanel.add(topComponent1, BorderLayout.NORTH);
    }

    @Override
    public void setTopComponent2(JComponent topComponent2) {
        // not implemented
    }

    @Override
    public void setSeqMapView(JPanel map_view) {
        JTabbedTrayPane bottom_pane = new JTabbedTrayBottomPane(map_view);
        bottom_pane.setResizeWeight(1.0);
        bottom_pane.addTrayStateChangeListener(this);
        tabHolders.put(TabState.COMPONENT_STATE_BOTTOM_TAB, bottom_pane);
        try {
            bottom_pane.invokeTrayState(TrayState.valueOf(PreferenceUtils.getComponentState(bottom_pane.getTitle())));
        } catch (Exception x) {
            bottom_pane.invokeTrayState(TrayState.getDefaultTrayState());
        }
        JTabbedTrayPane left_pane = new JTabbedTrayLeftPane(bottom_pane);
        left_pane.addTrayStateChangeListener(this);
        tabHolders.put(TabState.COMPONENT_STATE_LEFT_TAB, left_pane);
        try {
            left_pane.invokeTrayState(TrayState.valueOf(PreferenceUtils.getComponentState(left_pane.getTitle())));
        } catch (Exception x) {
            left_pane.invokeTrayState(TrayState.getDefaultTrayState());
        }
        JTabbedTrayPane right_pane = new JTabbedTrayRightPane(left_pane);
        right_pane.setResizeWeight(1.0);
        right_pane.addTrayStateChangeListener(this);
        tabHolders.put(TabState.COMPONENT_STATE_RIGHT_TAB, right_pane);
        try {
            right_pane.invokeTrayState(TrayState.valueOf(PreferenceUtils.getComponentState(right_pane.getTitle())));
        } catch (Exception x) {
            right_pane.invokeTrayState(TrayState.getDefaultTrayState());
        }
        innerPanel.add(right_pane, BorderLayout.CENTER);
    }

    @Override
    public void setMenuCreator(IMenuCreator menuCreator) {
        ((WindowTabs) tabHolders.get(TabState.COMPONENT_STATE_WINDOW)).setMenuCreator(menuCreator);
    }

    @Override
    public void setTabsMenu(JMenuBar mbar) {
        this.tabs_menu = MenuUtil.getRPMenu(mbar, "IGB_main_tabsMenu", BUNDLE.getString("tabsMenu"));
        for (final TabState tabState : TabState.values()) {
            if (tabState.isTab()) {
                JRPMenuItem change_tab_state_item = new JRPMenuItem(
                        "WindowServiceDefaultImpl_change_tab_state_item_" + tabState.name().replaceAll(" ", "_"),
                        new GenericAction(MessageFormat.format(BUNDLE.getString("openCurrentTabInNewWindow"), BUNDLE.getString(tabState.name())), null, null) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                setTabState(((JTabbedTrayPane) tabHolders.get(tabState)).getSelectedIGBTabPanel(), TabState.COMPONENT_STATE_WINDOW);
                            }
                        }
                );
                change_tab_state_item.setEnabled(false);
                MenuUtil.addToMenu(tabs_menu, change_tab_state_item);
                move_tab_to_window_items.put(tabState, change_tab_state_item);
            }
        }
        for (final TabState tabState : TabState.values()) {
            if (tabState.isTab()) {
                JRPMenuItem move_tabbed_panel_to_window_item = new JRPMenuItem(
                        "WindowServiceDefaultImpl_move_tabbed_panel_to_window_item_" + tabState.name().replaceAll(" ", "_"),
                        new GenericAction(MessageFormat.format(BUNDLE.getString("openTabbedPanesInNewWindow"), BUNDLE.getString(tabState.name())), null, null) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                ((JTabbedTrayPane) tabHolders.get(tabState)).invokeTrayState(TrayState.WINDOW);
                            }
                        }
                );
                move_tabbed_panel_to_window_item.setEnabled(false);
                MenuUtil.addToMenu(tabs_menu, move_tabbed_panel_to_window_item);
                move_tabbed_panel_to_window_items.put(tabState, move_tabbed_panel_to_window_item);
                trayStateChanged((JTabbedTrayPane) tabHolders.get(tabState), ((JTabbedTrayPane) tabHolders.get(tabState)).getTrayState());
            }
        }
        tabs_menu.addSeparator();
    }

    /**
     * Saves information about which plugins are in separate windows and what
     * their preferred sizes are.
     */
    private void saveWindowLocations() {
        // Save the main window location
        PreferenceUtils.saveWindowLocation(frame, "main window");

        tabHolders.values().forEach(com.affymetrix.igb.service.api.TabHolder::close);
        for (IGBTabPanel comp : tabHolders.get(TabState.COMPONENT_STATE_WINDOW).getIGBTabPanels()) {
            PreferenceUtils.saveWindowLocation(comp.getFrame(), comp.getName());
        }
    }

    /**
     * add a new tab pane to the window service
     *
     * @param tabPanel the tab pane
     */
    public void addTab(final IGBTabPanel tabPanel) {
        TabState tabState = tabPanel.getDefaultState();
        try {
            tabState = TabState.valueOf(PreferenceUtils.getComponentState(tabPanel.getName()));
        } catch (Exception x) {
        }
        setTabState(tabPanel, tabState);
        TabHolder tabHolder = tabHolders.get(tabState);
        if (tabPanel.isFocus()) {
            tabHolder.selectTab(tabPanel);
        }
//		if (PreferenceUtils.getSelectedTab(tabHolder.getName()) == null && tabPanel.isFocus()) {
//			tabHolder.selectTab(tabPanel);
//		}
//		else if (tabPanel.getName().equals(PreferenceUtils.getSelectedTab(tabHolder.getName()))) {
//			tabHolder.selectTab(tabPanel);
//		}
        JPopupMenu popup = new JPopupMenu();
        JRPMenu pluginMenu = new JRPMenu("WindowServiceDefaultImpl_tabPanel_" + tabPanel.getName().replaceAll(" ", "_"), tabPanel.getDisplayName());
        tabMenus.put(tabPanel, pluginMenu);
        tabMenuPositions.put(pluginMenu, tabPanel.getPosition());
        ButtonGroup group = new ButtonGroup();

        for (TabState tabStateLoop : tabPanel.getDefaultState().getCompatibleTabStates()) {
            TabStateMenuItem menuItem = new TabStateMenuItem("WindowServiceDefaultImpl_tabPanel_" + tabPanel.getName().replaceAll(" ", "_") + "_" + tabStateLoop.name().replaceAll(" ", "_"), tabPanel, tabStateLoop);
            group.add(menuItem);
            pluginMenu.add(menuItem);
            popup.add(new ActionWrapper(menuItem, group));
        }
        setTabMenu(tabPanel);
        if (tabPanel.getPosition() == IGBTabPanel.DEFAULT_TAB_POSITION) {
            if (!tabSeparatorSet) {
                tabs_menu.addSeparator();
                tabSeparatorSet = true;
            }
            tabs_menu.add(pluginMenu);
        } else {
            int menuPosition = findMenuItemPosition(tabPanel);
            tabs_menu.insert(pluginMenu, menuPosition);
        }
        tabPanel.setComponentPopupMenu(popup);
    }

    private int findMenuItemPosition(final IGBTabPanel tabPanel) {
        int menuPosition = 0;
        for (int i = 0; i < tabs_menu.getItemCount(); i++) {
            boolean menuPositionAvailable = isMenuPositionAvailable(tabPanel, i);
            boolean withinValidRange = isWithinValidRange(tabPanel, i);
            if (menuPositionAvailable || withinValidRange) {
                if (tabMenuPositions.get(tabs_menu.getItem(i)) != null) {
                    menuPosition = i;
                    break;
                }
            }
        }
        return menuPosition;
    }

    private boolean isMenuPositionAvailable(final IGBTabPanel tabPanel, int i) {
        boolean menuPositionAvailable = tabMenuPositions.get(tabs_menu.getItem(i)) == null;
        return menuPositionAvailable;
    }

    private boolean isWithinValidRange(final IGBTabPanel tabPanel, int i) {
        boolean validIndex = false;
        if (tabMenuPositions.get(tabs_menu.getItem(i)) != null) {
            validIndex = tabPanel.getPosition() > tabMenuPositions.get(tabs_menu.getItem(i));
        }
        return validIndex;
    }

    public void showTabs() {
        // here we keep count of the embedded tabs that are added. When
        // all the embedded tabs have been added, we initialize the tab panes
        // this is to prevent the flashing of new tabs added
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);

            // Resize all tab holder after frame is set to visible.
            tabHolders.values().forEach(com.affymetrix.igb.service.api.TabHolder::resize);
        });
    }

    /**
     * set the tab menu for a tab pane
     *
     * @param plugin the tab pane
     */
    private void setTabMenu(final IGBTabPanel plugin) {
        JMenu menu = tabMenus.get(plugin);
        TabState tabState = getTabState(plugin);
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem menuItem = menu.getItem(i);
            if (menuItem != null && menuItem instanceof TabStateMenuItem) {
                menuItem.setSelected(((TabStateMenuItem) menuItem).getTabState() == tabState);
            }
        }
    }

    /**
     * remove a tab pane from the window service
     *
     * @param plugin the tab pane
     */
    public void removeTab(final IGBTabPanel plugin) {
        for (TabState tabState : tabHolders.keySet()) {
            tabHolders.get(tabState).removeTab(plugin);
        }
        Arrays.asList(tabs_menu.getMenuComponents()).stream().filter(item -> item instanceof JMenuItem && ((JMenuItem) item).getText().equals(plugin.getDisplayName())).forEach(tabs_menu::remove);
        PreferenceUtils.saveComponentState(plugin.getName(), null);
    }

    /**
     * set a given tab pane to a given tab state
     *
     * @param panel the tab pane
     * @param tabState the new tab state
     */
    private void setTabState(IGBTabPanel panel, TabState tabState) {
        if (panel == null) {// || tabState == getTabState(panel)) {
            return;
        }
        TabState oldTabState = getTabState(panel);
        if (oldTabState != null) {
            tabHolders.get(oldTabState).removeTab(panel);
        }
        if (tabState == null) {
            removeTab(panel);
        } else {
            tabHolders.get(tabState).addTab(panel);
        }
        PreferenceUtils.saveComponentState(panel.getName(), tabState.name());
    }

    @Override
    public void setTabStateAndMenu(IGBTabPanel panel, TabState tabState) {
        setTabState(panel, tabState);
        setTabMenu(panel);
    }

    @Override
    public void startup() {
    }

    @Override
    public void shutdown() {
        saveWindowLocations();
        stopRoutines.forEach(com.affymetrix.igb.service.api.IWindowRoutine::stop);
    }

    @Override
    public void saveState() {
        saveWindowLocations();
    }

    @Override
    public void restoreState() {
        Rectangle pos = PreferenceUtils.retrieveWindowLocation("main window", frame.getBounds());
        if (pos != null) {
            PreferenceUtils.setWindowSize(frame, pos);
        }
        for (IGBTabPanel tabPanel : new HashSet<>(getPlugins())) {
            setTabState(tabPanel, TabState.valueOf(PreferenceUtils.getComponentState(tabPanel.getName())));
        }
        for (TabState tabState : tabHolders.keySet()) {
            TabHolder tabHolder = tabHolders.get(tabState);
            tabHolder.restoreState();
        }
    }

    @Override
    public void setDefaultState(IGBTabPanel panel) {
        setTabState(panel, panel.getDefaultState());
        setTabMenu(panel);
    }

    /**
     * get the tab state of a given tab pane
     *
     * @param panel the tab pane
     * @return the tab state of the give pane
     */
    private TabState getTabState(IGBTabPanel panel) {
        for (TabState tabState : tabHolders.keySet()) {
            if (tabHolders.get(tabState).getIGBTabPanels().contains(panel)) {
                return tabState;
            }
        }
        return null;
    }

    @Override
    public Set<IGBTabPanel> getPlugins() {
        HashSet<IGBTabPanel> plugins = new HashSet<>();
        for (TabState tabState : tabHolders.keySet()) {
            plugins.addAll(tabHolders.get(tabState).getIGBTabPanels());
        }
        return plugins;
    }

    @Override
    public void trayStateChanged(JTabbedTrayPane trayPane, TrayState trayState) {
        for (JRPMenuItem menuItem : new JRPMenuItem[]{move_tab_to_window_items.get(trayPane.getTabState()), move_tabbed_panel_to_window_items.get(trayPane.getTabState())}) {
            if (menuItem != null) {
                menuItem.setEnabled(trayState != TrayState.HIDDEN && trayState != TrayState.WINDOW);
            }
        }
    }

    @Override
    public void selectTab(IGBTabPanel panel) {
        if (panel == null) {
            return;
        }
        tabHolders.values().stream().filter(tabHolder -> tabHolder.getIGBTabPanels().contains(panel)).forEach(tabHolder -> {
            tabHolder.selectTab(panel);
        });
    }

    void addStopRoutine(IWindowRoutine routine) {
        stopRoutines.add(routine);
    }

    void removeStopRoutine(IWindowRoutine routine) {
        stopRoutines.remove(routine);
    }
}
