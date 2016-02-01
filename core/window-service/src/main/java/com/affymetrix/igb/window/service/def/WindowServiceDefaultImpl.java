package com.affymetrix.igb.window.service.def;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.igb.swing.JRPMenu;
import com.affymetrix.igb.swing.JRPMenuBar;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.swing.JRPRadioButtonMenuItem;
import com.affymetrix.igb.swing.MenuUtil;
import com.affymetrix.igb.window.service.IWindowService;
import com.affymetrix.igb.window.service.def.JTabbedTrayPane.TrayState;
import com.google.common.collect.Sets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;
import static org.lorainelab.igb.frame.FrameManager.MAIN_WINDOW_PREF_KEY;
import org.lorainelab.igb.frame.api.FrameManagerService;
import org.lorainelab.igb.services.IgbService;
import org.lorainelab.igb.services.window.WindowServiceLifecycleHook;
import org.lorainelab.igb.services.window.tabs.IgbTabPanel;
import org.lorainelab.igb.services.window.tabs.IgbTabPanel.TabState;
import org.lorainelab.igb.services.window.tabs.IgbTabPanelI;
import org.lorainelab.igb.services.window.tabs.TabHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(provide = {IWindowService.class})
public class WindowServiceDefaultImpl implements IWindowService, TabStateHandler, TrayStateChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(WindowServiceDefaultImpl.class);
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private final Map<TabState, JRPMenuItem> move_tab_to_window_items;
    private final Map<TabState, JRPMenuItem> move_tabbed_panel_to_window_items;
    private JMenu tabsMenu;
    private JFrame frame;
    private final Map<TabState, TabHolder> tabHolders;
    private final Map<IgbTabPanel, JMenu> tabMenus;
    private final Map<JMenu, Integer> tabMenuPositions;
    private volatile Set<WindowServiceLifecycleHook> stopRoutines;
    private JPanel innerPanel;
    private boolean tabSeparatorSet = false;
    private FrameManagerService frameManagerService;

    public WindowServiceDefaultImpl() {
        stopRoutines = Sets.newConcurrentHashSet();
        move_tab_to_window_items = new EnumMap<>(TabState.class);
        move_tabbed_panel_to_window_items = new EnumMap<>(TabState.class);
        tabHolders = new EnumMap<>(TabState.class);
        tabHolders.put(TabState.COMPONENT_STATE_WINDOW, new WindowTabs(this));
        tabHolders.put(TabState.COMPONENT_STATE_HIDDEN, new HiddenTabs());
        tabMenus = new ConcurrentHashMap<>();
        tabMenuPositions = new HashMap<>();
        tabsMenu = new JMenu(BUNDLE.getString("tabsMenu"));
    }

    @Activate
    public void activate() {
        frame = frameManagerService.getIgbMainFrame();
        innerPanel = new JPanel(new MigLayout("fill, hidemode 1"));
        frame.add(innerPanel, "grow");
    }

    @Reference
    public void setFrameManagerService(FrameManagerService frameManagerService) {
        this.frameManagerService = frameManagerService;
    }

    @Override
    public void setTopComponent1(JComponent topComponent1) {
        innerPanel.add(topComponent1, "north");
    }

    @Reference(optional = true, dynamic = true, multiple = false, unbind = "unsetIgbService")
    public void setIgbService(IgbService igbService) {
        igbService.addParentMenuBarEntry(tabsMenu, 12);
    }

    private void unsetIgbService(IgbService igbService) {
    }

    @Override
    public void setMainPanel(JPanel mainPanel) {
        JTabbedTrayPane bottomPane = new JTabbedTrayBottomPane(frameManagerService, mainPanel);
        JTabbedTrayPane leftPane = new JTabbedTrayLeftPane(frameManagerService, bottomPane);
        JTabbedTrayPane rightPane = new JTabbedTrayRightPane(frameManagerService, leftPane);
        initializePaneTrayState(bottomPane, TabState.COMPONENT_STATE_BOTTOM_TAB);
        bottomPane.setResizeWeight(1.0);
        initializePaneTrayState(leftPane, TabState.COMPONENT_STATE_LEFT_TAB);
        initializePaneTrayState(rightPane, TabState.COMPONENT_STATE_RIGHT_TAB);
        rightPane.setResizeWeight(1.0);
        innerPanel.add(rightPane, "grow");
        validateTabHolders();
    }

    private void initializePaneTrayState(JTabbedTrayPane pane, TabState tabState) {
        pane.addTrayStateChangeListener(this);
        tabHolders.put(tabState, pane);
        try {
            final Optional<String> preferredComponentState = PreferenceUtils.getComponentState(pane.getTitle());
            if (preferredComponentState.isPresent()) {
                final TrayState preferredTrayState = TrayState.valueOf(preferredComponentState.get());
                pane.setTrayState(preferredTrayState);
            } else {
                pane.setTrayState(TrayState.HIDDEN);
            }
        } catch (Exception ex) {
            logger.error("error setting pane tray state" + ex.getMessage(), ex);
            pane.setTrayState(TrayState.HIDDEN);
        }
    }

    @Override
    public void setTabsMenu(JRPMenuBar mbar) {
        int menuItemCounter = 0;
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
                },
                        menuItemCounter++
                );
                change_tab_state_item.setEnabled(false);
                MenuUtil.addToMenu(tabsMenu, change_tab_state_item);
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
                        ((JTabbedTrayPane) tabHolders.get(tabState)).setTrayState(TrayState.WINDOW);
                    }
                },
                        menuItemCounter++
                );
                move_tabbed_panel_to_window_item.setEnabled(false);
                MenuUtil.addToMenu(tabsMenu, move_tabbed_panel_to_window_item);
                move_tabbed_panel_to_window_items.put(tabState, move_tabbed_panel_to_window_item);
                trayStateChanged((JTabbedTrayPane) tabHolders.get(tabState), ((JTabbedTrayPane) tabHolders.get(tabState)).getTrayState());
            }
        }
        tabsMenu.addSeparator();
    }

    /**
     * Saves information about which plugins are in separate windows and what their preferred sizes are.
     */
    private void saveWindowLocations() {
        // Save the main window location
        PreferenceUtils.saveWindowLocation(frame, MAIN_WINDOW_PREF_KEY);

        tabHolders.values().forEach(org.lorainelab.igb.services.window.tabs.TabHolder::close);
        for (IgbTabPanel comp : tabHolders.get(TabState.COMPONENT_STATE_WINDOW).getIGBTabPanels()) {
            PreferenceUtils.saveWindowLocation(comp.getFrame(), comp.getName());
        }
    }

    /**
     * add a new tab pane to the window service
     *
     * @param tabPanel the tab pane
     */
    public void addTab(final IgbTabPanelI tabPanel) {
        SwingUtilities.invokeLater(() -> {
            IgbTabPanel igbTabPanel = tabPanel.getIgbTabPanel();
            TabState tabState = igbTabPanel.getDefaultTabState();
            final Optional<String> preferredTabState = PreferenceUtils.getComponentState(igbTabPanel.getName());
            if (preferredTabState.isPresent()) {
                tabState = TabState.valueOf(preferredTabState.get());
            }
            setTabState(igbTabPanel, tabState);
            TabHolder tabHolder = tabHolders.get(tabState);
            if (igbTabPanel.isFocus()) {
                tabHolder.selectTab(igbTabPanel);
            }
            JPopupMenu popup = new JPopupMenu();
            JRPMenu pluginMenu = new JRPMenu("WindowServiceDefaultImpl_tabPanel_" + igbTabPanel.getName().replaceAll(" ", "_"), igbTabPanel.getDisplayName());
            tabMenus.put(igbTabPanel, pluginMenu);
            tabMenuPositions.put(pluginMenu, igbTabPanel.getPosition());
            ButtonGroup group = new ButtonGroup();

            for (TabState tabStateLoop : igbTabPanel.getDefaultTabState().getCompatibleTabStates()) {
                TabStateMenuItem menuItem = new TabStateMenuItem("WindowServiceDefaultImpl_tabPanel_" + igbTabPanel.getName().replaceAll(" ", "_") + "_" + tabStateLoop.name().replaceAll(" ", "_"), igbTabPanel, tabStateLoop);
                group.add(menuItem);
                pluginMenu.add(menuItem);
                popup.add(new ActionWrapper(menuItem, group));
            }
            setTabMenu(igbTabPanel);
            if (igbTabPanel.getPosition() == IgbTabPanel.DEFAULT_TAB_POSITION) {
                if (!tabSeparatorSet) {
                    tabsMenu.addSeparator();
                    tabSeparatorSet = true;
                }
                tabsMenu.add(pluginMenu);
            } else {
                int menuPosition = findMenuItemPosition(igbTabPanel);
                tabsMenu.insert(pluginMenu, menuPosition);
            }
            igbTabPanel.setComponentPopupMenu(popup);
        });
        validateTabHolders();
    }

    private int findMenuItemPosition(final IgbTabPanel tabPanel) {
        int menuPosition = 0;
        for (int i = 0; i < tabsMenu.getItemCount(); i++) {
            boolean menuPositionAvailable = isMenuPositionAvailable(tabPanel, i);
            boolean withinValidRange = isWithinValidRange(tabPanel, i);
            if (menuPositionAvailable || withinValidRange) {
                if (tabMenuPositions.get(tabsMenu.getItem(i)) != null) {
                    menuPosition = i;
                    break;
                }
            }
        }
        return menuPosition;
    }

    private boolean isMenuPositionAvailable(final IgbTabPanel tabPanel, int i) {
        boolean menuPositionAvailable = tabMenuPositions.get(tabsMenu.getItem(i)) == null;
        return menuPositionAvailable;
    }

    private boolean isWithinValidRange(final IgbTabPanel tabPanel, int i) {
        boolean validIndex = false;
        if (tabMenuPositions.get(tabsMenu.getItem(i)) != null) {
            validIndex = tabPanel.getPosition() > tabMenuPositions.get(tabsMenu.getItem(i));
        }
        return validIndex;
    }

    public void validateTabHolders() {
        SwingUtilities.invokeLater(() -> {
            // Resize all tab holder after frame is set to visible.
            tabHolders.values().forEach(tabHolder -> {
                if (tabHolder instanceof JTabbedTrayPane) {
                    if (tabHolder.getIGBTabPanels().size() > 1) {
                        JTabbedTrayPane.class.cast(tabHolder).setTrayState(TrayState.EXTENDED);
                    }
                }
                tabHolder.resize();
            });

        });
    }

    /**
     * set the tab menu for a tab pane
     *
     * @param plugin the tab pane
     */
    private void setTabMenu(final IgbTabPanel plugin) {
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
    public void removeTab(IgbTabPanelI plugin) {
        IgbTabPanel igbTabPanel = plugin.getIgbTabPanel();
        for (TabState tabState : tabHolders.keySet()) {
            tabHolders.get(tabState).removeTab(igbTabPanel);
        }
        for (java.awt.Component item : Arrays.asList(tabsMenu.getMenuComponents())) {
            if (item instanceof JMenuItem && ((JMenuItem) item).getText().equals(igbTabPanel.getDisplayName())) {
                tabsMenu.remove(item);
            }
        }

        PreferenceUtils.saveComponentState(igbTabPanel.getName(), null);
    }

    /**
     * set a given tab pane to a given tab state
     *
     * @param panel the tab pane
     * @param tabState the new tab state
     */
    private void setTabState(IgbTabPanel panel, TabState tabState) {
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
    public void setTabStateAndMenu(IgbTabPanel igbTabPanel, TabState tabState) {
        setTabState(igbTabPanel, tabState);
        setTabMenu(igbTabPanel);
    }

    @Override
    public void startup() {
    }

    @Override
    public void shutdown() {
        saveWindowLocations();
        stopRoutines.forEach(WindowServiceLifecycleHook::stop);
    }

    @Override
    public void saveState() {
        saveWindowLocations();
    }

    @Override
    public void restoreState() {
        Rectangle pos = PreferenceUtils.retrieveWindowLocation(MAIN_WINDOW_PREF_KEY, frame.getBounds());
        if (pos != null) {
            PreferenceUtils.setWindowSize(frame, pos);
        }
        getPlugins().stream().forEach((tabPanel) -> {
            final Optional<String> preferredTabState = PreferenceUtils.getComponentState(tabPanel.getName());
            if (preferredTabState.isPresent()) {
                setTabState(tabPanel, TabState.valueOf(preferredTabState.get()));
            }
        });
        for (TabState tabState : tabHolders.keySet()) {
            TabHolder tabHolder = tabHolders.get(tabState);
            tabHolder.restoreState();
        }
    }

    @Override
    public void setDefaultState(IgbTabPanel panel) {
        setTabState(panel, panel.getDefaultTabState());
        setTabMenu(panel);
    }

    /**
     * get the tab state of a given tab pane
     *
     * @param panel the tab pane
     * @return the tab state of the give pane
     */
    private TabState getTabState(IgbTabPanel panel) {
        for (TabState tabState : tabHolders.keySet()) {
            if (tabHolders.get(tabState).getIGBTabPanels().contains(panel)) {
                return tabState;
            }
        }
        return null;
    }

    @Override
    public Set<IgbTabPanel> getPlugins() {
        HashSet<IgbTabPanel> plugins = new HashSet<>();
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
    public void selectTab(IgbTabPanel panel) {
        if (panel == null) {
            return;
        }
        tabHolders.values().stream().filter(tabHolder -> tabHolder.getIGBTabPanels().contains(panel)).forEach(tabHolder -> {
            tabHolder.selectTab(panel);
        });
    }

    @Reference(multiple = true, unbind = "removeStopRoutine", optional = true, dynamic = true)
    void addStopRoutine(WindowServiceLifecycleHook routine) {
        stopRoutines.add(routine);
    }

    void removeStopRoutine(WindowServiceLifecycleHook routine) {
        stopRoutines.remove(routine);
    }

    private class TabStateMenuItem extends JRPRadioButtonMenuItem {

        private static final long serialVersionUID = 1L;
        private final TabState tabState;

        private TabStateMenuItem(String id, final IgbTabPanel igbTabPanel, TabState _tabState) {
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
}
