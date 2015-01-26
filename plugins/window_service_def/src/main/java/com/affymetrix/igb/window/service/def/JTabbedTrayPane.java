package com.affymetrix.igb.window.service.def;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.EventUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.swing.JRPTabbedPane;
import com.affymetrix.igb.service.api.IGBTabPanel;
import com.affymetrix.igb.service.api.TabHolder;
import com.affymetrix.igb.service.api.IGBTabPanel.TabState;

/**
 * TabHolder implementation for all tabs that are in a tab panel. This consists
 * of a split pane that has a JTabbedPane on one side and the actual view on the
 * other side. These may be stacked.
 */
public abstract class JTabbedTrayPane extends JSplitPane implements TabHolder {

    private static final long serialVersionUID = 1L;
    protected static final int MINIMUM_WIDTH = 40;

    protected enum TrayState {

        HIDDEN,
        RETRACTED,
        EXTENDED,
        WINDOW;

        /**
         * get the default state of all trays
         *
         * @return the default tray state
         */
        public static TrayState getDefaultTrayState() {
            return HIDDEN;
        }
    }
    protected final JComponent _baseComponent;
    private static final int DIVIDER_SIZE = 8;
    private double saveDividerProportionalLocation; // saved as percent, but implemented as pixels, due to problems with Swing
    protected final JRPTabbedPane tab_pane;
    private final TabState tabState;
    private final List<TrayStateChangeListener> trayStateChangeListeners;
    protected TrayState trayState;
    private final String title;
    private boolean retractDividerSet;
    private boolean minSizeSet;
    private JFrame frame;
    private boolean initialized = false;

    /**
     * set the JTabbedPane in the JSplitPane - different for different
     * orientations
     */
    protected abstract void setTabComponent();

    /**
     * get the full size (width or height) of the tray
     *
     * @return the full size of the tray
     */
    protected abstract int getFullSize();

    /**
     * set the minimum size of the two components
     */
    protected abstract void setMinSize();

    /**
     * return the width (or height) of the given tab panel
     *
     * @param tabComponent the tab panel
     * @return the width or height of the tab panel
     */
    protected abstract int getTabWidth(Component tabComponent);

    /**
     * get the int / pixel value of the divider location for the EXTEND tray
     * state (it is saved as a percentage)
     *
     * @return the extend state divider location
     */
    private int getExtendDividerLocation() {
        return (int) Math.round(getFullSize() * saveDividerProportionalLocation);
    }

    /**
     * get the int / pixel value of the divider location for the RETRACT tray
     * state
     *
     * @return the retract state divider location
     */
    private int getRetractDividerLocation() {
        if (tab_pane.getTabCount() < 2) {
            return -1;
        }
        int index = tab_pane.getSelectedIndex() < 1 ? 1 : tab_pane.getSelectedIndex();
        return getTabWidth(tab_pane.getComponentAt(index));
    }

    /**
     * get the int / pixel value of the divider location for the HIDDEN tray
     * state
     *
     * @return the hidden state divider location
     */
    protected abstract int getHideDividerLocation();

    /**
     * determines if the point is on the tab of a tabbed pane
     *
     * @param p the point to check
     * @return true if the point is on the tab, false otherwise
     */
    protected abstract boolean isOnTab(Point p);

    protected abstract Icon getRetractIcon();

    protected abstract Icon getExtendIcon();

    /**
     * save the divider location for the RETRACT tray state - as a percentage
     */
    private void saveDividerLocation() {
        if (getDividerLocation() == 0 || getFullSize() == 0) {
            return;
        }
        saveDividerProportionalLocation = (double) getDividerLocation() / (double) getFullSize();
    }

    public JTabbedTrayPane(String id, TabState tabState, JComponent _baseComponent, int orientation, int splitOrientation, double _saveDividerProportionalLocation) {
        super(splitOrientation);
        this._baseComponent = _baseComponent;
        this.tabState = tabState;
        retractDividerSet = false;
        trayStateChangeListeners = new ArrayList<>();
        trayState = TrayState.HIDDEN;
        title = MessageFormat.format(WindowServiceDefaultImpl.BUNDLE.getString("tabbedPanesTitle"), WindowServiceDefaultImpl.BUNDLE.getString(tabState.name()));
        saveDividerProportionalLocation = PreferenceUtils.getDividerLocation(title);
        if (saveDividerProportionalLocation < 0) {
            saveDividerProportionalLocation = _saveDividerProportionalLocation;
        }
        tab_pane = createTabbedPane(id, orientation);
        tab_pane.addAncestorListener(
                new AncestorListener() {

                    @Override
                    public void ancestorAdded(AncestorEvent event) {
                    }

                    @Override
                    public void ancestorRemoved(AncestorEvent event) {
                    }

                    @Override
                    public void ancestorMoved(AncestorEvent event) {
                        if (trayState == TrayState.EXTENDED && initialized) {
                            saveDividerLocation();
                        }
                    }
                });
        tab_pane.addChangeListener(e -> {
            IGBTabPanel sel = (IGBTabPanel) tab_pane.getSelectedComponent();
            if (sel != null) {
                GenericActionHolder.getInstance().notifyActionPerformed(sel.getSelectAction());
            }
        });

        setOneTouchExpandable(false);
        setDividerSize(0);

        // Using JTabbedPane.SCROLL_TAB_LAYOUT makes it impossible to add a
        // pop-up menu (or any other mouse listener) on the tab handles.
        // (A pop-up with "Open tab in a new window" would be nice.)
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4465870
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4499556
        tab_pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tab_pane.setMinimumSize(new Dimension(0, 0));
        setTabComponent();
        tab_pane.addTab(null, null); // extend / retract

        MouseListener[] mouseListeners = tab_pane.getMouseListeners();
        if (mouseListeners == null) {
            System.out.println(MessageFormat.format(WindowServiceDefaultImpl.BUNDLE.getString("internalError"), this.getClass().getName()));
        } else {
            final MouseListener originalMouseListener = mouseListeners[0];
            tab_pane.removeMouseListener(originalMouseListener);
            tab_pane.addMouseListener(
                    new MouseListener() {

                        private int beforeIndex = -1;

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            originalMouseListener.mouseReleased(e);
                            int index = tab_pane.indexAtLocation(e.getX(), e.getY());
                            if (index > -1) {
                                if (trayState == TrayState.EXTENDED) {
                                    if (index == 0) {
                                        retractTray();
                                    }
                                } else if (trayState == TrayState.RETRACTED) {
                                    extendTray();
                                }
                            }
                            if (index == 0) {
                                tab_pane.setSelectedIndex(beforeIndex);
                            } else if (EventUtils.isOurPopupTrigger(e)) {
                                if (tab_pane.getSelectedComponent() instanceof JComponent) {
                                    JComponent jc = (JComponent) tab_pane.getSelectedComponent();
                                    JPopupMenu popup = jc.getComponentPopupMenu();
                                    if (popup != null && popup.getComponentCount() > 0) {
                                        popup.show(e.getComponent(), e.getX(), e.getY());
                                    }
                                }
                            }
                        }

                        @Override
                        public void mousePressed(MouseEvent e) {
                            beforeIndex = tab_pane.getSelectedIndex();
                            originalMouseListener.mousePressed(e);
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                            originalMouseListener.mouseExited(e);
                        }

                        @Override
                        public void mouseEntered(MouseEvent e) {
                            originalMouseListener.mouseEntered(e);
                        }

                        @Override
                        public void mouseClicked(MouseEvent e) {
                            originalMouseListener.mouseClicked(e);
                        }
                    }
            );
        }
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                resize();
            }
//		    public void componentResized(ComponentEvent e) {
//		    	resize();
//		    }

            @Override
            public void componentMoved(ComponentEvent e) {
                resize();
            }
        });
    }

    private void unWindow() {
        Container cont = frame.getContentPane();
        cont.remove(tab_pane);
        cont.validate();
        frame.dispose();
        frame = null;
        setTabComponent();
    }

    /**
     * put the tray in the HIDDEN tray state this happens when there are not
     * tabs in the tray
     */
    private void hideTray() {
        if (trayState == TrayState.HIDDEN) {
            return;
        }
        if (trayState == TrayState.WINDOW) {
            unWindow();
        }
        if (trayState == TrayState.EXTENDED) {
            saveDividerLocation();
        }
        setDividerLocation(getHideDividerLocation());
        setDividerSize(0);
        trayState = TrayState.HIDDEN;
        PreferenceUtils.saveComponentState(title, TrayState.HIDDEN.toString());
        notifyTrayStateChangeListeners();
    }

    /**
     * put the tray in the EXTEND tray state this happens when the user clicks
     * on a tab from the RETRACTED tray state, or clicks on a different tab in
     * the EXTENDED tray state
     */
    private void extendTray() {
        if (trayState == TrayState.WINDOW) {
            unWindow();
        }
        setDividerLocation(getExtendDividerLocation());
        setDividerSize(DIVIDER_SIZE);
        trayState = TrayState.EXTENDED;
        PreferenceUtils.saveComponentState(title, TrayState.EXTENDED.toString());
        notifyTrayStateChangeListeners();
        tab_pane.setIconAt(0, getRetractIcon());
        tab_pane.setToolTipTextAt(0, "retract tray.");
        if (tab_pane.getSelectedIndex() < 1) {
            tab_pane.setSelectedIndex(1);
        }
    }

    /**
     * put the tray in the RETRACT tray state this happens when the user clicks
     * on the already selected tab from the EXTENDED tray state
     */
    private void retractTray() {
        if (trayState == TrayState.WINDOW) {
            unWindow();
        }
        if (trayState == TrayState.EXTENDED) {
            saveDividerLocation();
        }
        int retractDividerLocation = getRetractDividerLocation();
        if (retractDividerLocation != -1) {
            setDividerLocation(retractDividerLocation);
            retractDividerSet = true;
        }
        setDividerSize(0);
        trayState = TrayState.RETRACTED;
        PreferenceUtils.saveComponentState(title, TrayState.RETRACTED.toString());
        notifyTrayStateChangeListeners();
        tab_pane.setIconAt(0, getExtendIcon());
        tab_pane.setToolTipTextAt(0, "extend tray");
    }

    /**
     * put the tray in the WINDOW tray state (a separate popup window) this
     * happens when the user selects the appropriate menu item
     */
    public void windowTray() {
        if (trayState == TrayState.EXTENDED) {
            saveDividerLocation();
        }
        setDividerLocation(getHideDividerLocation());
        setDividerSize(0);

        remove(tab_pane);
        validate();

        frame = new JFrame(title);
        final Container cont = frame.getContentPane();
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        cont.add(tab_pane);
        tab_pane.setVisible(true);
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

        final Runnable return_panes_to_main_window = () -> {
            // save the current size into the preferences, so the window
            // will re-open with this size next time
            PreferenceUtils.saveWindowLocation(frame, title);
            setDividerLocation(saveDividerProportionalLocation);
            extendTray();
        };

        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent evt) {
                SwingUtilities.invokeLater(return_panes_to_main_window);
            }
        });

        trayState = TrayState.WINDOW;
        PreferenceUtils.saveComponentState(title, TrayState.WINDOW.toString());
        notifyTrayStateChangeListeners();
    }

    /**
     * puts the tray into the given tray state
     *
     * @param newState the new state for the tray
     */
    public void invokeTrayState(TrayState newState) {
        if (trayState == newState) {
            return;
        }
        switch (newState) {
            case HIDDEN:
                hideTray();
                break;
            case RETRACTED:
                retractTray();
                break;
            case EXTENDED:
                extendTray();
                break;
            case WINDOW:
                windowTray();
                break;
        }
    }

    @Override
    public void addTab(final IGBTabPanel plugin) {
        int index = 0;
        while (index < tab_pane.getTabCount() && plugin.compareTo((IGBTabPanel) tab_pane.getComponentAt(index)) > 0) {
            index++;
        }
        tab_pane.insertTab(plugin.getTitle(), plugin.getIcon(), plugin, plugin.getToolTipText(), index);
        if (tab_pane.getTabCount() == 2) { //  first is extend / retract button
            initTray();
        }
        tab_pane.validate();
    }

    @Override
    public void removeTab(final IGBTabPanel plugin) {
        plugin.setTrayRectangle(plugin.getBounds());
        String name = plugin.getName();
        boolean selectedTabRemoved = false;
        for (int i = 1; i < tab_pane.getTabCount(); i++) {
            if (name.equals(tab_pane.getComponentAt(i).getName())) {
                if (tab_pane.getSelectedIndex() == i) {
                    selectedTabRemoved = true;
                }
                tab_pane.remove(i);
                tab_pane.validate();
                break;
            }
        }
        if (tab_pane.getTabCount() == 1) {
            hideTray();
        } else if (selectedTabRemoved) {
            for (int i = 1; i < tab_pane.getTabCount(); i++) {
                if (((IGBTabPanel) tab_pane.getComponentAt(i)).isFocus()) {
                    tab_pane.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    @Override
    public Set<IGBTabPanel> getIGBTabPanels() {
        Set<IGBTabPanel> plugins = new HashSet<>();
        for (int i = 0; i < tab_pane.getTabCount(); i++) {
            IGBTabPanel panel = (IGBTabPanel) tab_pane.getComponentAt(i);
            if (panel != null) {
                plugins.add(panel);
            }
        }
        return plugins;
    }

    @Override
    public void selectTab(IGBTabPanel panel) {
        tab_pane.setSelectedComponent(panel);
    }

    public IGBTabPanel getSelectedIGBTabPanel() {
        return (IGBTabPanel) tab_pane.getSelectedComponent();
    }

    /**
     * create the JTabbedPane for the Tray
     *
     * @param tabPlacement the tabPlacement (orientation) for the JTabbedPane
     * @return the JTabbedPane
     */
    protected JRPTabbedPane createTabbedPane(String id, int tabPlacement) {
        return new JRPTabbedPane(id, tabPlacement);
    }

    /**
     * standard getter
     *
     * @return the tabState
     */
    public TabState getTabState() {
        return tabState;
    }

    /**
     * standard getter
     *
     * @return the trayState
     */
    public TrayState getTrayState() {
        return trayState;
    }

    /**
     * call all the TrayStateChangeListeners, notifying them of a change in the
     * tray state of this tray
     */
    private void notifyTrayStateChangeListeners() {
        for (TrayStateChangeListener trayStateChangeListener : trayStateChangeListeners) {
            trayStateChangeListener.trayStateChanged(this, trayState);
        }
    }

    /**
     * add a new listener for tray state changes
     *
     * @param trayStateChangeListener the new listener
     */
    public void addTrayStateChangeListener(TrayStateChangeListener trayStateChangeListener) {
        trayStateChangeListeners.add(trayStateChangeListener);
    }

    /**
     * remove an existing listener for tray state changes it will no longer be
     * notified of changes
     *
     * @param trayStateChangeListener the listener to remove
     */
    public void removeTrayStateChangeListener(TrayStateChangeListener trayStateChangeListener) {
        trayStateChangeListeners.remove(trayStateChangeListener);
    }

    /**
     * get the text title of the tray - displayed in the caption bar if the tray
     * is put into a separate popup
     *
     * @return the title of the tray
     */
    public String getTitle() {
        return title;
    }

    private void initTray() {
        if (trayState == TrayState.HIDDEN) {
            invokeTrayState(TrayState.EXTENDED);
        }
        if (trayState == TrayState.EXTENDED && !minSizeSet) {
            setMinSize();
            minSizeSet = true;
        }
        if (trayState == TrayState.RETRACTED && !retractDividerSet) {
            setDividerLocation(getRetractDividerLocation());
            retractDividerSet = true;
        }
    }

    private IGBTabPanel getTabPanel(String tabName) {
        IGBTabPanel tabPanel = null;
        for (IGBTabPanel loopPanel : getIGBTabPanels()) {
            if (loopPanel.getName().equals(tabName)) {
                tabPanel = loopPanel;
                break;
            }
        }
        return tabPanel;
    }

    @Override
    public void restoreState() {
        double dividerProportionalLocation = PreferenceUtils.getDividerLocation(title);
        if (dividerProportionalLocation >= 0) {
            saveDividerProportionalLocation = dividerProportionalLocation;
        }
        String trayState = PreferenceUtils.getComponentState(title);
        if (trayState != null) {
            invokeTrayState(TrayState.valueOf(trayState));
        }
        String selectedTabPanelName = PreferenceUtils.getSelectedTab(title);
        if (selectedTabPanelName != null) {
            selectTab(getTabPanel(selectedTabPanelName));
        }
    }

    @Override
    public void resize() {
        switch (trayState) {
            case HIDDEN:
                setDividerLocation(getHideDividerLocation());
                break;
            case RETRACTED:
                int retractDividerLocation = getRetractDividerLocation();
                if (retractDividerLocation != -1) {
                    setDividerLocation(retractDividerLocation);
                    retractDividerSet = true;
                }
                break;
            case EXTENDED:
                setDividerLocation(getExtendDividerLocation());
                break;
            default:
                break;
        }
    }

    @Override
    public void close() {
        if (trayState == TrayState.WINDOW) {
            PreferenceUtils.saveWindowLocation(frame, title);
        } else if (trayState == TrayState.EXTENDED) {
            saveDividerLocation();
        }
        if (getSelectedIGBTabPanel() == null) {
            PreferenceUtils.saveSelectedTab(title, null);
        } else {
            PreferenceUtils.saveSelectedTab(title, getSelectedIGBTabPanel().getName());
        }
        PreferenceUtils.saveDividerLocation(title, saveDividerProportionalLocation);
        PreferenceUtils.saveComponentState(title, trayState.toString());
    }

    @Override
    public String getName() {
        return title;
    }
}
