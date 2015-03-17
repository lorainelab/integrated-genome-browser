/**
 * Copyright (c) 2001-2006 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.prefs;

import static com.affymetrix.common.CommonUtils.IS_LINUX;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.action.ClearPreferencesAction;
import com.affymetrix.igb.action.ExportPreferencesAction;
import com.affymetrix.igb.action.ImportPreferencesAction;
import com.affymetrix.igb.action.PreferencesHelpAction;
import com.affymetrix.igb.action.PreferencesHelpTabAction;
import com.affymetrix.igb.swing.MenuUtil;
import com.lorainelab.igb.services.window.preferences.IPrefEditorComponent;
import com.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public final class PreferencesPanel extends JPanel {

    public static int TAB_TIER_PREFS_VIEW = -1;
    public static int TAB_OTHER_OPTIONS_VIEW = -1;
    public static int TAB_DATALOAD_PREFS = -1;
    private static final long serialVersionUID = 1L;
    public static final String WINDOW_NAME = "Preferences Window";
    private JFrame frame = null;
    public static PreferencesPanel singleton = null;
    private final JTabbedPane tab_pane;
    private final static String PREFERENCES = BUNDLE.getString("Preferences");
    private final static String HELP = BUNDLE.getString("helpMenu");
    public TrackPreferencesPanel tpvGUI = null;

    private PreferencesPanel() {
        this.setLayout(new BorderLayout());

        tab_pane = new JTabbedPane();

        this.add(tab_pane, BorderLayout.CENTER);

        // using SCROLL_TAB_LAYOUT would disable the tool-tips, due to a Swing bug.
        //tab_pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    /**
     * Creates an instance of PreferencesView. It will contain tabs for setting
     * various types of preferences. You can put this view in any JComponent you
     * wish, but probably the best idea is to use {@link #getFrame()}.
     */
    public static PreferencesPanel getSingleton() {
        if (singleton != null) {
            return singleton;
        }
        singleton = new PreferencesPanel();
        singleton.tpvGUI = new TierPreferencesPanel();
        singleton.tpvGUI.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentHidden(ComponentEvent e) {
                ((TierPrefsView) singleton.tpvGUI.tdv).removedFromView();
            }
        });

        TAB_TIER_PREFS_VIEW = singleton.addPrefEditorComponent(singleton.tpvGUI);
        TAB_OTHER_OPTIONS_VIEW = singleton.addPrefEditorComponent(new OtherOptionsView());
        TAB_DATALOAD_PREFS = singleton.addPrefEditorComponent(DataLoadPrefsView.getSingleton());
        return singleton;
    }

    /**
     * Set the tab pane to the given index.
     */
    public void setTab(int i) {
        if (i < 0 || i >= tab_pane.getComponentCount()) {
            return;
        }
        tab_pane.setSelectedIndex(i);
        Component c = tab_pane.getComponentAt(i);
        if (c instanceof IPrefEditorComponent) {
            IPrefEditorComponent p = (IPrefEditorComponent) c;
            p.refresh();
        }
    }

    public int getTabIndex(Component component) {
        for (int i = 0; i < tab_pane.getComponentCount(); i++) {
            Component c = tab_pane.getComponentAt(i);
            if (component == c) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Adds the given component as a panel to the tab pane of preference
     * editors.
     *
     * @param pec An implementation of PrefEditorComponent that must also be an
     * instance of java.awt.Component.
     * @return the index of the added tab in the tab pane.
     */
    public int addPrefEditorComponent(final IPrefEditorComponent pec) {
        tab_pane.add(pec);
        pec.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent e) {
                pec.refresh();
            }
        });
        return tab_pane.indexOfComponent(pec);
    }

    public void addPrefEditorComponent(PreferencesPanelProvider panelProvider) {
        tab_pane.add(panelProvider.getPanel());
        panelProvider.getPanel().addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent e) {
                panelProvider.refresh();
            }
        });
    }

    public void removePrefEditorComponent(PreferencesPanelProvider panelProvider) {
        tab_pane.remove(panelProvider.getPanel());
    }

    public IPrefEditorComponent[] getPrefEditorComponents() {
        int count = tab_pane.getTabCount();
        IPrefEditorComponent[] comps = new IPrefEditorComponent[count];
        for (int i = 0; i < count; i++) {
            comps[i] = (IPrefEditorComponent) tab_pane.getComponentAt(i);
        }
        return comps;
    }

    /**
     * Gets a JFrame containing the PreferencesView
     */
    public JFrame getFrame() {
        int width = 558;
        int height = 582;
        if (IS_LINUX) {
            width = 574;
            height = 610;
        }
        if (frame == null) {
            frame = new JFrame(PREFERENCES);
            final Container cont = frame.getContentPane();
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent evt) {
                    // save the current size into the preferences, so the window
                    // will re-open with this size next time
                    PreferenceUtils.saveWindowLocation(frame, WINDOW_NAME);
                    // if the TierPrefsView is being displayed, the apply any changes from it.
                    // if it is not being displayed, then its changes have already been applied in componentHidden()
                    if (singleton.tpvGUI != null) {
                        if (singleton.tab_pane.getSelectedComponent() == singleton.tpvGUI) {
                            ((TierPrefsView) (singleton.tpvGUI.tdv)).removedFromView();
                        }
                    }
                    frame.dispose();
                }
            });
            JMenuBar menubar = this.getMenuBar();
            frame.setJMenuBar(menubar);
            cont.add(this);
            frame.pack(); // pack() to set frame to its preferred size
            Rectangle pos = PreferenceUtils.retrieveWindowLocation(WINDOW_NAME, new Rectangle(width, height));
            if (pos != null) {
                PreferenceUtils.setWindowSize(frame, pos);
            }
            /*
             * sets the Preferences window at the centre of the IGB window
             */
            frame.addWindowListener(singleton.tpvGUI);
            frame.setLocationRelativeTo(IGB.getSingleton().getFrame());
        }

        singleton.tpvGUI.refresh();	// update component list

        return frame;
    }

    private JMenuBar getMenuBar() {
        JMenuBar menu_bar = new JMenuBar();
        JMenu prefs_menu = new JMenu(PREFERENCES);
        prefs_menu.setMnemonic('P');

        JMenuItem exp = new JMenuItem(ExportPreferencesAction.getAction());
        JMenuItem imp = new JMenuItem(ImportPreferencesAction.getAction());
        JMenuItem clr = new JMenuItem(ClearPreferencesAction.getAction());
        MenuUtil.addToMenu(prefs_menu, exp, PREFERENCES);
        MenuUtil.addToMenu(prefs_menu, imp, PREFERENCES);
        MenuUtil.addToMenu(prefs_menu, clr, PREFERENCES);

        menu_bar.add(prefs_menu);

        JMenu help_menu = new JMenu(HELP);
        help_menu.setMnemonic('H');

        JMenuItem help = new JMenuItem(PreferencesHelpAction.getAction());
        JMenuItem helpTab = new JMenuItem(PreferencesHelpTabAction.getAction());
        MenuUtil.addToMenu(help_menu, help, PREFERENCES);
        MenuUtil.addToMenu(help_menu, helpTab, PREFERENCES);

        menu_bar.add(help_menu);
        return menu_bar;
    }

    public Component getSelectedTabComponent() {
        return tab_pane.getSelectedComponent();
    }
}
