package com.affymetrix.igb.window.service.def;

import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.event.EventUtils;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.swing.JRPTabbedPane;
import com.lorainelab.igb.services.window.tabs.IgbTabPanel;
import com.lorainelab.igb.services.window.tabs.IgbTabPanel.TabState;
import com.lorainelab.igb.services.window.tabs.TabHolder;
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
import java.util.Optional;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TabHolder implementation for all tabs that are in a tab panel. This consists of a split pane that has a JTabbedPane
 * on one side and the actual view on the other side. These may be stacked.
 */
public abstract class JTabbedTrayPane extends JSplitPane implements TabHolder {

    private static final long serialVersionUID = 1L;
    protected static final int MINIMUM_WIDTH = 40;
    private static final Logger logger = LoggerFactory.getLogger(JTabbedTrayPane.class);

    private static final int DIVIDER_SIZE = 8;
    protected final JComponent baseComponent;
    private double saveDividerProportionalLocation; // saved as percent, but implemented as pixels, due to problems with Swing
    protected final JRPTabbedPane tabPane;
    private final TabState tabState;
    private final List<TrayStateChangeListener> trayStateChangeListeners;
    protected TrayState trayState;
    private final String title;
    private boolean retractDividerSet;
    private boolean minSizeSet;
    private JFrame frame;
    private boolean initialized = false;

    public JTabbedTrayPane(String id, TabState tabState, JComponent _baseComponent, int orientation, int splitOrientation, double _saveDividerProportionalLocation) {
        super(splitOrientation);
        this.baseComponent = _baseComponent;
        this.tabState = tabState;
        retractDividerSet = false;
        trayStateChangeListeners = new ArrayList<>();
        trayState = TrayState.HIDDEN;
        title = MessageFormat.format(WindowServiceDefaultImpl.BUNDLE.getString("tabbedPanesTitle"), WindowServiceDefaultImpl.BUNDLE.getString(tabState.name()));
        saveDividerProportionalLocation = PreferenceUtils.getDividerLocation(title);
        if (saveDividerProportionalLocation < 0) {
            saveDividerProportionalLocation = _saveDividerProportionalLocation;
        }
        tabPane = createTabbedPane(id, orientation);
        tabPane.addAncestorListener(
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
        tabPane.addChangeListener(e -> {
            IgbTabPanel sel = (IgbTabPanel) tabPane.getSelectedComponent();
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
        tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabPane.setMinimumSize(new Dimension(0, 0));
        setTabComponent();
        tabPane.addTab(null, null); // extend / retract
        MouseListener[] mouseListeners = tabPane.getMouseListeners();
        if (mouseListeners == null) {
        } else {
            final MouseListener originalMouseListener = mouseListeners[0];
            tabPane.removeMouseListener(originalMouseListener);
            tabPane.addMouseListener(new MouseListener() {

                private int beforeIndex = -1;

                @Override
                public void mouseReleased(MouseEvent e) {
                    originalMouseListener.mouseReleased(e);
                    int index = tabPane.indexAtLocation(e.getX(), e.getY());
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
                        tabPane.setSelectedIndex(beforeIndex);
                    } else if (EventUtils.isOurPopupTrigger(e)) {
                        if (tabPane.getSelectedComponent() instanceof JComponent) {
                            JComponent jc = (JComponent) tabPane.getSelectedComponent();
                            JPopupMenu popup = jc.getComponentPopupMenu();
                            if (popup != null && popup.getComponentCount() > 0) {
                                popup.show(e.getComponent(), e.getX(), e.getY());
                            }
                        }
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    beforeIndex = tabPane.getSelectedIndex();
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

    /**
     * set the JTabbedPane in the JSplitPane - different for different orientations
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
     * get the int / pixel value of the divider location for the EXTEND tray state (it is saved as a percentage)
     *
     * @return the extend state divider location
     */
    private int getExtendDividerLocation() {
        return (int) Math.round(getFullSize() * saveDividerProportionalLocation);
    }

    /**
     * get the int / pixel value of the divider location for the RETRACT tray state
     *
     * @return the retract state divider location
     */
    private int getRetractDividerLocation() {
        if (tabPane.getTabCount() < 2) {
            return -1;
        }
        int index = tabPane.getSelectedIndex() < 1 ? 1 : tabPane.getSelectedIndex();
        return getTabWidth(tabPane.getComponentAt(index));
    }

    /**
     * get the int / pixel value of the divider location for the HIDDEN tray state
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

    private void unWindow() {
        Container cont = frame.getContentPane();
        cont.remove(tabPane);
        cont.validate();
        frame.dispose();
        frame = null;
        setTabComponent();
    }

    /**
     * put the tray in the HIDDEN tray state this happens when there are not tabs in the tray
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
     * put the tray in the EXTEND tray state this happens when the user clicks on a tab from the RETRACTED tray state,
     * or clicks on a different tab in the EXTENDED tray state
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
        tabPane.setIconAt(0, getRetractIcon());
        tabPane.setToolTipTextAt(0, "retract tray.");
        if (tabPane.getSelectedIndex() < 1) {
            if (tabPane.getTabCount() > 1) {
                tabPane.setSelectedIndex(1);
            }
        }
    }

    /**
     * put the tray in the RETRACT tray state this happens when the user clicks on the already selected tab from the
     * EXTENDED tray state
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
        tabPane.setIconAt(0, getExtendIcon());
        tabPane.setToolTipTextAt(0, "extend tray");
    }

    /**
     * put the tray in the WINDOW tray state (a separate popup window) this happens when the user selects the
     * appropriate menu item
     */
    public void windowTray() {
        if (trayState == TrayState.EXTENDED) {
            saveDividerLocation();
        }
        setDividerLocation(getHideDividerLocation());
        setDividerSize(0);

        remove(tabPane);
        validate();

        frame = new JFrame(title);
        final Container cont = frame.getContentPane();
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        cont.add(tabPane);
        tabPane.setVisible(true);
        frame.pack();
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
    public void setTrayState(TrayState newState) {
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
    public void addTab(final IgbTabPanel plugin) {
        int index = 0;
        while (index < tabPane.getTabCount() && plugin.compareTo((IgbTabPanel) tabPane.getComponentAt(index)) > 0) {
            index++;
        }
        tabPane.insertTab(plugin.getTitle(), plugin.getIcon(), plugin, plugin.getToolTipText(), index);
        if (tabPane.getTabCount() == 2) { //  first is extend / retract button
            initTray();
        }
        tabPane.validate();
    }

    @Override
    public void removeTab(final IgbTabPanel plugin) {
        plugin.setTrayRectangle(plugin.getBounds());
        String name = plugin.getName();
        boolean selectedTabRemoved = false;
        for (int i = 1; i < tabPane.getTabCount(); i++) {
            if (name.equals(tabPane.getComponentAt(i).getName())) {
                if (tabPane.getSelectedIndex() == i) {
                    selectedTabRemoved = true;
                }
                tabPane.remove(i);
                tabPane.validate();
                break;
            }
        }
        if (tabPane.getTabCount() == 1) {
            hideTray();
        } else if (selectedTabRemoved) {
            for (int i = 1; i < tabPane.getTabCount(); i++) {
                if (((IgbTabPanel) tabPane.getComponentAt(i)).isFocus()) {
                    tabPane.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    @Override
    public Set<IgbTabPanel> getIGBTabPanels() {
        Set<IgbTabPanel> plugins = new HashSet<>();
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            IgbTabPanel panel = (IgbTabPanel) tabPane.getComponentAt(i);
            if (panel != null) {
                plugins.add(panel);
            }
        }
        return plugins;
    }

    @Override
    public void selectTab(IgbTabPanel panel) {
        tabPane.setSelectedComponent(panel);
    }

    public IgbTabPanel getSelectedIGBTabPanel() {
        return (IgbTabPanel) tabPane.getSelectedComponent();
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
     * call all the TrayStateChangeListeners, notifying them of a change in the tray state of this tray
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
     * remove an existing listener for tray state changes it will no longer be notified of changes
     *
     * @param trayStateChangeListener the listener to remove
     */
    public void removeTrayStateChangeListener(TrayStateChangeListener trayStateChangeListener) {
        trayStateChangeListeners.remove(trayStateChangeListener);
    }

    /**
     * get the text title of the tray - displayed in the caption bar if the tray is put into a separate popup
     *
     * @return the title of the tray
     */
    public String getTitle() {
        return title;
    }

    private void initTray() {
        if (trayState == TrayState.HIDDEN) {
            setTrayState(TrayState.EXTENDED);
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

    private IgbTabPanel getTabPanel(String tabName) {
        IgbTabPanel tabPanel = null;
        for (IgbTabPanel loopPanel : getIGBTabPanels()) {
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
        final Optional<String> preferreTrayState = PreferenceUtils.getComponentState(title);
        if (preferreTrayState.isPresent()) {
            setTrayState(TrayState.valueOf(preferreTrayState.get()));
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

    protected enum TrayState {

        HIDDEN, RETRACTED, EXTENDED, WINDOW;

        /**
         * get the default state of all trays
         *
         * @return the default tray state
         */
        public static TrayState getDefaultTrayState() {
            return HIDDEN;
        }
    }
}
