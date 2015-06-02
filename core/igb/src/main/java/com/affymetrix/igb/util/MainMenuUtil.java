package com.affymetrix.igb.util;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.AMenuItem;
import com.affymetrix.igb.IGBConstants;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.action.AboutIGBAction;
import com.affymetrix.igb.action.AutoLoadThresholdAction;
import com.affymetrix.igb.action.CancelScriptAction;
import com.affymetrix.igb.action.ClampViewAction;
import com.affymetrix.igb.action.ClearVisualTools;
import com.affymetrix.igb.action.ConfigureScrollAction;
import com.affymetrix.igb.action.CopyResiduesAction;
import com.affymetrix.igb.action.DocumentationAction;
import com.affymetrix.igb.action.DrawCollapseControlAction;
import com.affymetrix.igb.action.ExitAction;
import com.affymetrix.igb.action.ExportFileAction;
import com.affymetrix.igb.action.IGBSupportAction;
import com.affymetrix.igb.action.LoadFileAction;
import com.affymetrix.igb.action.PreferencesAction;
import com.affymetrix.igb.action.RemoveFeatureAction;
import com.affymetrix.igb.action.RunScriptAction;
import com.affymetrix.igb.action.ShowAllVisualToolsAction;
import com.affymetrix.igb.action.ShowFilterMarkAction;
import com.affymetrix.igb.action.ShowFullFilePathInTrack;
import com.affymetrix.igb.action.ShowIGBTrackMarkAction;
import com.affymetrix.igb.action.ShowLockedTrackIconAction;
import com.affymetrix.igb.action.StartAutoScrollAction;
import com.affymetrix.igb.action.StopAutoScrollAction;
import com.affymetrix.igb.action.ToggleEdgeMatchingAction;
import com.affymetrix.igb.action.ToggleHairlineAction;
import com.affymetrix.igb.action.ToggleHairlineLabelAction;
import com.affymetrix.igb.action.ToggleToolTipAction;
import com.affymetrix.igb.shared.DeselectAllAction;
import com.affymetrix.igb.shared.LoadURLAction;
import com.affymetrix.igb.shared.SelectAllAction;
import com.affymetrix.igb.swing.JRPCheckBoxMenuItem;
import com.affymetrix.igb.swing.JRPMenu;
import com.affymetrix.igb.swing.JRPMenuBar;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.swing.MenuUtil;
import com.lorainelab.igb.services.IgbService;
import com.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hiralv
 */
@Component(name = MainMenuUtil.COMPONENT_NAME, immediate = true)
public class MainMenuUtil implements MainMenuManager {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MainMenuUtil.class);
    public static final String COMPONENT_NAME = "MainMenuUtil";
    private static final String ID_PREFIX = "IGB_main_";
    private JRPMenuBar menuBar;
    private IgbService igbService;
    private final List<IgbMenuItemProvider> igbMenuItemProviderQueue;
    private final List<AMenuItem> aMenuItemQueue;
    private boolean componentActivated;
    public static final int TOOLS_MENU_POSITION = 3;

    public MainMenuUtil() {
        componentActivated = false;
        igbMenuItemProviderQueue = new ArrayList<>();
        aMenuItemQueue = new ArrayList<>();
    }

    @Activate
    public void activate() {
        componentActivated = true;
        menuBar = (JRPMenuBar) igbService.getApplicationFrame().getJMenuBar();
        loadMenu();
        loadQueuedMenuItems();
    }

    @Reference(optional = false)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    private void loadQueuedMenuItems() {
        igbMenuItemProviderQueue.stream().forEach(provider -> addMenuItem(provider));
        aMenuItemQueue.stream().forEach(aMenuItem -> addAMenuItem(aMenuItem));
    }

    private void loadMenu() {
        // load the menu from the Preferences
        Preferences mainMenuPrefs = PreferenceUtils.getAltNode(PreferenceUtils.MENU_NODE_NAME);
        try {
            if (mainMenuPrefs.childrenNames().length == 0) {
                loadDefaultMenu();
            } else {
                for (String childMenu : mainMenuPrefs.childrenNames()) {
                    loadTopMenu(mainMenuPrefs.node(childMenu));
                }
            }
        } catch (BackingStoreException x) {
            Logger.getLogger(MainMenuUtil.class.getName()).log(Level.SEVERE, "error loading menu preferences", x);
        }
    }

    private void fileMenu() {
        int menuItemCounter = 0;
        JRPMenu fileMenu = MenuUtil.getRPMenu(menuBar, ID_PREFIX + "fileMenu", BUNDLE.getString("fileMenu"), 0);
        fileMenu.setMnemonic(BUNDLE.getString("fileMenuMnemonic").charAt(0));
        MenuUtil.addToMenu(fileMenu, new JRPMenuItem(ID_PREFIX + "fileMenu_loadFile", LoadFileAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(fileMenu, new JRPMenuItem(ID_PREFIX + "fileMenu_loadURL", LoadURLAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(fileMenu, new JRPMenuItem(ID_PREFIX + "fileMenu_exportFile", ExportFileAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(fileMenu, new JRPMenuItem(ID_PREFIX + "fileMenu_closeTracks", RemoveFeatureAction.getAction(), menuItemCounter++));
        fileMenu.addSeparator(menuItemCounter++);
        MenuUtil.addToMenu(fileMenu, new JRPMenuItem(ID_PREFIX + "fileMenu_preferences", PreferencesAction.getAction(), menuItemCounter++));
        fileMenu.addSeparator(menuItemCounter++);
        MenuUtil.addToMenu(fileMenu, new JRPMenuItem(ID_PREFIX + "fileMenu_exit", ExitAction.getAction(), menuItemCounter++));
    }

    private void editMenu() {
        int menuItemCounter = 0;
        JRPMenu editMenu = MenuUtil.getRPMenu(menuBar, ID_PREFIX + "editMenu", BUNDLE.getString("editMenu"), 1);
        editMenu.setMnemonic(BUNDLE.getString("editMenuMnemonic").charAt(0));
        MenuUtil.addToMenu(editMenu, new JRPMenuItem(ID_PREFIX + "editMenu_copyResidues", CopyResiduesAction.getAction(), menuItemCounter++));
        JMenu select_menu = new JRPMenu(ID_PREFIX + "editMenu_select", IGBConstants.BUNDLE.getString("selectTracks"));
        select_menu.setIcon(MenuUtil.getIcon("16x16/actions/blank_placeholder.png"));
        select_menu.add(new JRPMenuItem(ID_PREFIX + "editMenu_select_all", SelectAllAction.getAction(), menuItemCounter++));
        select_menu.add(new JRPMenuItem(ID_PREFIX + "editMenu_deselect_all", DeselectAllAction.getAction(), menuItemCounter++));
        editMenu.add(select_menu);
    }

    private void viewMenu() {
        int menuItemCounter = 0;
        JRPMenu viewMenu = MenuUtil.getRPMenu(menuBar, ID_PREFIX + "viewMenu", BUNDLE.getString("viewMenu"), 2);
        viewMenu.setMnemonic(BUNDLE.getString("viewMenuMnemonic").charAt(0));
        MenuUtil.addToMenu(viewMenu, new JRPMenuItem(ID_PREFIX + "viewMenu_setThreshold", AutoLoadThresholdAction.getAction(), menuItemCounter++));
        viewMenu.addSeparator(menuItemCounter++);
        MenuUtil.addToMenu(viewMenu, new JRPCheckBoxMenuItem(ID_PREFIX + "viewMenu_clampView", ClampViewAction.getAction(), menuItemCounter++));
        viewMenu.addSeparator(menuItemCounter++);
        MenuUtil.addToMenu(viewMenu, new JRPMenuItem(ID_PREFIX + "viewMenu_clearVisualTools", ClearVisualTools.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(viewMenu, new JRPMenuItem(ID_PREFIX + "viewMenu_showVisualTools", ShowAllVisualToolsAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(viewMenu, new JRPCheckBoxMenuItem(ID_PREFIX + "viewMenu_showHairline", ToggleHairlineAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(viewMenu, new JRPCheckBoxMenuItem(ID_PREFIX + "viewMenu_toggleHairlineLabel", ToggleHairlineLabelAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(viewMenu, new JRPCheckBoxMenuItem(ID_PREFIX + "viewMenu_toggleToolTip", ToggleToolTipAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(viewMenu, new JRPCheckBoxMenuItem(ID_PREFIX + "viewMenu_drawCollapseControl", DrawCollapseControlAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(viewMenu, new JRPCheckBoxMenuItem(ID_PREFIX + "viewMenu_showIGBTrackMark", ShowIGBTrackMarkAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(viewMenu, new JRPCheckBoxMenuItem(ID_PREFIX + "viewMenu_showFilterMark", ShowFilterMarkAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(viewMenu, new JRPCheckBoxMenuItem(ID_PREFIX + "viewMenu_toggleHairlineLabel", ToggleEdgeMatchingAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(viewMenu, new JRPCheckBoxMenuItem(ID_PREFIX + "viewMenu_showLockTrackIcon", ShowLockedTrackIconAction.getAction(), menuItemCounter++));

        MenuUtil.addToMenu(viewMenu, new JRPCheckBoxMenuItem(ID_PREFIX + "viewMenu_showFullFilePathInTrack", ShowFullFilePathInTrack.getAction(), menuItemCounter++));//TK
    }

    private void toolMenu() {
        int menuItemCounter = 0;
        JRPMenu toolsMenu;
        toolsMenu = MenuUtil.getRPMenu(menuBar, ID_PREFIX + "toolsMenu", BUNDLE.getString("toolsMenu"), 3);
        toolsMenu.setMnemonic(BUNDLE.getString("toolsMenuMnemonic").charAt(0));
        MenuUtil.addToMenu(toolsMenu, new JRPMenuItem(ID_PREFIX + "toolsMenu_start_autoscroll", StartAutoScrollAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(toolsMenu, new JRPMenuItem(ID_PREFIX + "toolsMenu_stop_autoscroll", StopAutoScrollAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(toolsMenu, new JRPMenuItem(ID_PREFIX + "toolsMenu_configure_autoscroll", ConfigureScrollAction.getAction(), menuItemCounter++));
        toolsMenu.addSeparator(menuItemCounter++);
        JMenu scripts_menu = new JRPMenu(ID_PREFIX + "toolsMenu_scripts", BUNDLE.getString("scripts"), menuItemCounter++);
        scripts_menu.setIcon(MenuUtil.getIcon("16x16/actions/blank_placeholder.png"));
        MenuUtil.addToMenu(scripts_menu, new JRPMenuItem(ID_PREFIX + "toolsMenu_scripts_runScript", RunScriptAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(scripts_menu, new JRPMenuItem(ID_PREFIX + "toolsMenu_scripts_cancelScript", CancelScriptAction.getAction(), menuItemCounter++));
        toolsMenu.add(scripts_menu);
        toolsMenu.addSeparator(menuItemCounter++);
    }

    private void helpMenu() {
        int menuItemCounter = 0;
        JRPMenu helpMenu = MenuUtil.getRPMenu(menuBar, ID_PREFIX + "helpMenu", BUNDLE.getString("helpMenu"), 7);
        helpMenu.setMnemonic(BUNDLE.getString("helpMenuMnemonic").charAt(0));
        MenuUtil.addToMenu(helpMenu, new JRPMenuItem(ID_PREFIX + "helpMenu_aboutIGB", AboutIGBAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(helpMenu, new JRPMenuItem(ID_PREFIX + "helpMenu_IGBSupport", IGBSupportAction.getAction(), menuItemCounter++));
        MenuUtil.addToMenu(helpMenu, new JRPMenuItem(ID_PREFIX + "helpMenu_documentation", DocumentationAction.getAction(), menuItemCounter++));
    }

    private void loadDefaultMenu() {
        fileMenu();
        editMenu();
        viewMenu();
        toolMenu();
        helpMenu();
    }

    @Override
    public JRPMenu getMenu(String menuId) {
        String fullId = ID_PREFIX + menuId + "Menu";
        int num_menus = menuBar.getMenuCount();
        for (int i = 0; i < num_menus; i++) {
            JRPMenu menu_i = (JRPMenu) menuBar.getMenu(i);
            if (fullId.equals(menu_i.getId())) {
                return menu_i;
            }
        }
        return null;
    }

    //TODO replace all instances of AMenuItem with IgbMenuItemProvider
    @Reference(optional = true, multiple = true, unbind = "removeAMenuItem", dynamic = true)
    public void addAMenuItem(AMenuItem aMenuItem) {
        if (componentActivated) {
            JMenu parent = getMenu(aMenuItem.getParentMenu());
            if (parent == null) {
                logger.warn("No menu found with name {}. {} is not added.", new Object[]{aMenuItem.getParentMenu(), aMenuItem.getMenuItem()});
                return;
            }
            if (aMenuItem.getLocation() == -1) {
                MenuUtil.addToMenu(parent, aMenuItem.getMenuItem());
            } else {
                MenuUtil.insertIntoMenu(parent, aMenuItem.getMenuItem(), aMenuItem.getLocation());
            }
        } else {

        }
    }

    public void removeAMenuItem(AMenuItem aMenuItem) {
        JMenu parent = getMenu(aMenuItem.getParentMenu());
        if (parent == null) {
            logger.warn("No menu found with name {}. {} is cannot be removed.", new Object[]{aMenuItem.getMenuItem(), aMenuItem.getMenuItem()});
            return;
        }
        MenuUtil.removeFromMenu(parent, aMenuItem.getMenuItem());
    }

    @Reference(optional = true, multiple = true, unbind = "removeMenuItem", dynamic = true)
    public void addMenuItem(IgbMenuItemProvider igbMenuItemProvider) {
        if (componentActivated) {
            JRPMenu parent = getMenu(igbMenuItemProvider.getParentMenuName());
            if (parent == null) {
                logger.warn("No menu found with name {}. {} is not added.", new Object[]{igbMenuItemProvider.getParentMenuName(), igbMenuItemProvider.getMenuItem()});
                return;
            }
            if (igbMenuItemProvider.getMenuItemWeight() == -1) {
                MenuUtil.addToMenu(parent, igbMenuItemProvider.getMenuItem());
            } else {
                MenuUtil.insertIntoMenu(parent, igbMenuItemProvider.getMenuItem(), igbMenuItemProvider.getMenuItemWeight());
            }
        } else {
            igbMenuItemProviderQueue.add(igbMenuItemProvider);
        }
    }

    public void removeMenuItem(IgbMenuItemProvider igbMenuItemProvider) {
        JMenu parent = getMenu(igbMenuItemProvider.getParentMenuName());
        if (parent == null) {
            logger.warn("No menu found with name {}. {} is cannot be removed.", new Object[]{igbMenuItemProvider.getParentMenuName(), igbMenuItemProvider.getMenuItem()});
            return;
        }
        MenuUtil.removeFromMenu(parent, igbMenuItemProvider.getMenuItem());
        parent.revalidate();
    }

    private void loadTopMenu(Preferences menuPrefs) {
        String key = menuPrefs.get("menu", "???");
        JRPMenu menu = MenuUtil.getRPMenu(menuBar, ID_PREFIX + "" + key, BUNDLE.getString(key));
        menu.setMnemonic(BUNDLE.getString(key + "Mnemonic").charAt(0));
        try {
            for (String childMenu : menuPrefs.childrenNames()) {
                loadMenuItem(menu, ID_PREFIX, menuPrefs.node(childMenu));
            }
        } catch (BackingStoreException x) {
            Logger.getLogger(MainMenuUtil.class.getName()).log(Level.SEVERE, "error loading menu preferences", x);
        }
    }

    private void loadMenuItem(JRPMenu menu, String id, Preferences menuItemPrefs) {
        if (menuItemPrefs.get("separator", null) != null) {
            menu.addSeparator();
        } else if (menuItemPrefs.get("menu", null) != null) {
            loadSubMenu(menu, id, menuItemPrefs);
        } else if (menuItemPrefs.get("item", null) != null) {
            loadLeafItem(menu, menuItemPrefs);
        } else {
            Logger.getLogger(MainMenuUtil.class.getName()).log(Level.SEVERE, "error in menu preferences definition");
        }
    }

    private void loadSubMenu(JRPMenu menu, String id, Preferences menuPrefs) {
        String key = menuPrefs.get("menu", "???");
        JRPMenu submenu = new JRPMenu(id + "" + key, BUNDLE.getString(key));
        menu.add(submenu);
        try {
            for (String childMenu : menuPrefs.childrenNames()) {
                loadMenuItem(submenu, id, menuPrefs.node(childMenu));
            }
        } catch (BackingStoreException x) {
            Logger.getLogger(MainMenuUtil.class.getName()).log(Level.SEVERE, "error loading menu preferences", x);
        }
    }

    private void loadLeafItem(JRPMenu menu, Preferences menuItemPrefs) {
        String className = menuItemPrefs.get("item", null);
        if (className.indexOf('.') == -1) {
            className = "com.affymetrix.igb.action." + className; // default
        }
        try {
            Class<?> clazz = Class.forName(className);
            Method m = clazz.getDeclaredMethod("getAction");
            GenericAction action = (GenericAction) m.invoke(null);
            String id = menu.getId() + "_" + menuItemPrefs.get("item", "???");
            JMenuItem item = action.isToggle() ? new JRPCheckBoxMenuItem(id, action, -1) : new JRPMenuItem(id, action, -1);
            if (action.usePrefixInMenu()) {
                MenuUtil.addToMenu(menu, item, menu.getText());
            } else {
                MenuUtil.addToMenu(menu, item);
            }
        } catch (Exception ex) {
            logger.error("error loading menu preferences", ex);
        }
    }
}
