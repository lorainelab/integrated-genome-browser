/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.swing;

import com.affymetrix.common.CommonUtils;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

public abstract class MenuUtil {

//  private static JMenuBar main_menu_bar = new JMenuBar();
    private static Map<String, KeyStroke> accelerators = new HashMap<>();

    public static void setAccelerators(Map<String, KeyStroke> _accelerators) {
        MenuUtil.accelerators = _accelerators;
    }

    /**
     * Sets the accelerator for the given JMenuItem based on the preference
     * associated with the action command. The action command Strings should be
     * unique across the whole application.
     */
    private static void addAccelerator(JMenuItem item, String command) {
        item.setAccelerator(accelerators.get(command));
    }

    /**
     * Sets up an association such that the accelerator given in the user prefs
     * for the action_command will cause the given action_command to be sent to
     * the given ActionListener when that accelerator key is pressed and the
     * given component is in the window that has keyboard focus. If there was no
     * user preference given for the action command, this routine does nothing.
     */
    public static KeyStroke addAccelerator(JComponent comp, ActionListener al,
            String action_command) {
        KeyStroke ks = accelerators.get(action_command);
        if (ks != null) {
            comp.registerKeyboardAction(al, action_command, ks,
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        return ks;
    }

//  public static final JMenuBar getMainMenuBar() {
//    return main_menu_bar;
//  }
    private static JMenu findMenu(JMenuBar main_menu_bar, String name) {
        int num_menus = main_menu_bar.getMenuCount();
        for (int i = 0; i < num_menus; i++) {
            JMenu menu_i = main_menu_bar.getMenu(i);
            if (name.equals(menu_i.getText())) {
                menu_i.getName();
                return menu_i;
            }
        }
        return null;
    }

    private static void addMenu(JRPMenuBar main_menu_bar, JMenu new_menu, int index) {
        try {
            main_menu_bar.add(new_menu, index);
        } catch (IllegalArgumentException ex) {
            addMenu(main_menu_bar, new_menu);
        }
    }

    private static void addMenu(JMenuBar main_menu_bar, JMenu new_menu) {
        main_menu_bar.add(new_menu);

    }

    public static JMenu getMenu(JMenuBar main_menu_bar, String name) {
        JMenu new_menu = findMenu(main_menu_bar, name);
        if (new_menu != null) {
            return new_menu;
        }
        new_menu = new JMenu(name);
        new_menu.setName(name); // JMenu.getName() and JMenu.getText() aren't automatically equal
        addMenu(main_menu_bar, new_menu);
        return new_menu;
    }

    public static JRPMenu getRPMenu(JRPMenuBar menuBar, String id, String name, int index) {
        JRPMenu new_menu = (JRPMenu) findMenu(menuBar, name);
        if (new_menu != null) {
            return new_menu;
        }
        new_menu = new JRPMenu(id, name, index);
        new_menu.setName(name); // JMenu.getName() and JMenu.getText() aren't automatically equal
        addMenu(menuBar, new_menu, index);
        return new_menu;
    }

    public static JRPMenu getRPMenu(JMenuBar menuBar, String id, String name) {
        JRPMenu new_menu = (JRPMenu) findMenu(menuBar, name);
        if (new_menu != null) {
            return new_menu;
        }
        new_menu = new JRPMenu(id, name);
        new_menu.setName(name); // JMenu.getName() and JMenu.getText() aren't automatically equal
        addMenu(menuBar, new_menu);
        return new_menu;
    }

    /**
     * Calls {@link #addToMenu(JMenu, JMenuItem)} with command set to null.
     */
    public static JMenuItem addToMenu(JMenu menu, JMenuItem item) {
        return addToMenu(menu, item, "");
    }

    public static JMenuItem addToMenu(JMenu menu, JMenuItem item, String prefix) {
        Action action = item.getAction();
        if (action != null) {
            String command = action.getClass().getName(); // duplicates GenericAction.getId()
            if (prefix != null && prefix.length() > 0) {
                command = prefix + "/" + command;
            }
            addAccelerator(item, command);
        }
        return menu.add(item);
    }

    public static JMenuItem insertIntoMenu(JMenu menu, JMenuItem item, int position) {
        Action action = item.getAction();
        if (action != null) {
            addAccelerator(item, action.getClass().getName());
        } // duplicates GenericAction.getId()
        return menu.insert(item, position);
    }

    public static void removeFromMenu(JMenu menu, JMenuItem item) {
        menu.remove(item);
    }

    public static ImageIcon getIcon(String resource_name) {
        return CommonUtils.getInstance().getIcon(resource_name);
    }
}
