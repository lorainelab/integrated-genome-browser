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

import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.action.ClearPreferencesAction;
import com.affymetrix.igb.action.ExportPreferencesAction;
import com.affymetrix.igb.action.ImportPreferencesAction;
import com.affymetrix.igb.action.PreferencesHelpAction;
import com.affymetrix.igb.action.PreferencesHelpTabAction;
import com.affymetrix.igb.swing.JRPJPanel;
import com.affymetrix.igb.swing.JRPTabbedPane;
import com.affymetrix.igb.swing.MenuUtil;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.lorainelab.igb.services.window.HtmlHelpProvider;
import com.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PreferencesPanel extends JRPJPanel implements HtmlHelpProvider {

    //TODO Delete these constants.
//    public static int TAB_TIER_PREFS_VIEW = -1;
//    public static int TAB_OTHER_OPTIONS_VIEW = -1;
//    public static int TAB_DATALOAD_PREFS = -1;
    private static final long serialVersionUID = 1L;
    public static final String WINDOW_NAME = "Preferences Window";
    private JFrame frame = null;
    public static PreferencesPanel singleton = null;
    private final JRPTabbedPane tabbedPane;
    private final static String PREFERENCES = BUNDLE.getString("Preferences");
    private final static String HELP = BUNDLE.getString("helpMenu");
    public TrackPreferencesPanel tpvGUI = null;
    private static final Logger logger = LoggerFactory.getLogger(PreferencesPanel.class);
    Map<String, PreferencesPanelProvider> prefPanels;

    private PreferencesPanel() {
        super(PreferencesPanel.class.getName());
        this.setLayout(new BorderLayout());

        tabbedPane = new JRPTabbedPane(PreferencesPanel.class.getName());
        prefPanels = new ConcurrentHashMap<>();
        this.add(tabbedPane, BorderLayout.CENTER);
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

        singleton.addPreferencePanel(singleton.tpvGUI);
        singleton.addPreferencePanel(new OtherOptionsView());
        singleton.addPreferencePanel(DataLoadPrefsView.getSingleton());
        return singleton;
    }

    /**
     * Set the tab pane to the given index.
     */
    public void setTab(int i) {
        if (i < 0 || i >= tabbedPane.getComponentCount()) {
            return;
        }
        tabbedPane.setSelectedIndex(i);
        Component c = tabbedPane.getComponentAt(i);
        if (c instanceof PreferencesPanelProvider) {
            PreferencesPanelProvider p = (PreferencesPanelProvider) c;
            p.refresh();
        }
    }

    public int getTabIndex(String tabName) {
        for (int i = 0; i < tab_pane.getComponentCount(); i++) {
            Component c = tab_pane.getComponentAt(i);
            if (c instanceof PreferencesPanelProvider) {
                if (((PreferencesPanelProvider) c).getName().equalsIgnoreCase(tabName)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int getTabIndex(Component component) {
        for (int i = 0; i < tabbedPane.getComponentCount(); i++) {
            Component c = tabbedPane.getComponentAt(i);
            if (component == c) {
                return i;
            }
        }
        return -1;
    }

    public void addPreferencePanel(PreferencesPanelProvider panelProvider) {
        prefPanels.put(panelProvider.getPanel().getName(), panelProvider);
        addPanelToTab(panelProvider);
        panelProvider.getPanel().addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent e) {
                panelProvider.refresh();
            }
        });
    }

    private void addPanelToTab(PreferencesPanelProvider panelProvider) {
        boolean panelAdded = false;
        for (int i = tabbedPane.getTabCount(); i > 0; i--) {
            JRPJPanel panel = (JRPJPanel) tabbedPane.getComponentAt(i - 1);
            if (panelProvider.getWeight() > panel.getWeight()) {
                tabbedPane.add(panelProvider.getPanel(), i);
                panelAdded = true;
                break;
            }
        }
        if (!panelAdded) {
            tabbedPane.add(panelProvider.getPanel());
        }
    }

    public void removePrefEditorComponent(PreferencesPanelProvider panelProvider) {
        tabbedPane.remove(panelProvider.getPanel());
    }

    public PreferencesPanelProvider[] getPrefEditorComponents() {
        int count = tabbedPane.getTabCount();
        PreferencesPanelProvider[] comps = new PreferencesPanelProvider[count];
        for (int i = 0; i < count; i++) {
            comps[i] = (PreferencesPanelProvider) tabbedPane.getComponentAt(i);
        }
        return comps;
    }

    /**
     * Gets a JFrame containing the PreferencesView
     */
    public JFrame getFrame() {
        int width = 750;
        int height = 650;
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
                        if (singleton.tabbedPane.getSelectedComponent() == singleton.tpvGUI) {
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
            frame.setLocationRelativeTo(IGB.getInstance().getFrame());
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
        return tabbedPane.getSelectedComponent();
    }

    @Override
    public String getHelpHtml() {
        String htmlText = null;
        try {
            htmlText = Resources.toString(PreferencesPanel.class.getResource("/help/com.affymetrix.igb.prefs.PreferencesPanel.html"), Charsets.UTF_8);
        } catch (IOException ex) {
            logger.error("Help file not found " , ex);
        }
        return htmlText;
    }
}
