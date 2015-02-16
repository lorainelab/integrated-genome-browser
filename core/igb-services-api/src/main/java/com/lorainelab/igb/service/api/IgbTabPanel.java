package com.lorainelab.igb.service.api;

import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.igb.swing.JRPWidget;
import com.affymetrix.igb.swing.script.ScriptManager;
import com.affymetrix.igb.swing.util.Idable;

public abstract class IgbTabPanel extends JPanel implements Comparable<IgbTabPanel>, JRPWidget, Idable, IgbTabPanelI {

    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_TAB_POSITION = Integer.MAX_VALUE - 1;

    /**
     * the current state of the tab
     */
    public enum TabState {

        COMPONENT_STATE_LEFT_TAB(true, true),
        COMPONENT_STATE_RIGHT_TAB(true, true),
        COMPONENT_STATE_BOTTOM_TAB(true, false),
        COMPONENT_STATE_WINDOW(false, false),
        COMPONENT_STATE_HIDDEN(false, false);

        private final boolean tab;
        private final boolean portrait;

        TabState(boolean tab, boolean portrait) {
            this.tab = tab;
            this.portrait = portrait;
        }

        /**
         * this state is a tab (left, right or botton)
         *
         * @return true if this state is a tab, false for hidden or
         * windowed
         */
        public boolean isTab() {
            return tab;
        }

        /**
         * get the default tab state
         *
         * @return the default tab state
         */
        public static TabState getDefaultTabState() {
            return COMPONENT_STATE_BOTTOM_TAB;
        }

        /**
         * get the list of all tab states that the user can change
         * the tab - depends on the initial tab state of the tab.
         *
         * @return a list of all compatible tab states
         */
        public List<TabState> getCompatibleTabStates() {
            List<TabState> compatibleTabStates = new ArrayList<>();
            for (TabState tabState : TabState.values()) {
                if (portrait == tabState.portrait || !isTab() || !tabState.isTab()) {
                    compatibleTabStates.add(tabState);
                }
            }
            return compatibleTabStates;
        }
    }

    private class SelectAction extends GenericAction {

        private static final long serialVersionUID = 1L;

        private SelectAction() {
            super(null, null, null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            select();
        }
    }
    private final String displayName;
    private final String title;
    private final boolean focus;
    private final int position;
    private final String id;
    private JFrame frame;
    private Rectangle trayRectangle;
    private final SelectAction selectAction;

    public IgbTabPanel(String displayName, String title, String tooltip, boolean main) {
        this(displayName, title, tooltip, main, DEFAULT_TAB_POSITION);
    }

    protected IgbTabPanel(String displayName, String title, String tooltip, boolean focus, int position) {
        super();
        this.displayName = displayName;
        this.title = title;
        this.focus = focus;
        this.position = position;
        this.id = "IgbTabPanel_" + this.getClass().getSimpleName();
        this.setToolTipText(tooltip);
        ScriptManager.getInstance().addWidget(this);
        selectAction = new SelectAction();
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    /**
     * get the name to display to the user
     *
     * @return the name to display to the user (on the tab)
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * get the title of the tab panel
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    public SelectAction getSelectAction() {
        return selectAction;
    }

    /**
     * if this tab should be the tab selected when IGB starts
     *
     * @return true if this tab panel should get initial
     * selection/focus, false otherwise
     */
    public boolean isFocus() {
        return focus;
    }

    /**
     * get the position of the tab in the tray
     *
     * @return the tab position
     */
    public int getPosition() {
        return position;
    }

    /**
     * get the default / initial state of this tab panel
     *
     * @return the default state of this tab
     */
    public TabState getDefaultTabState() {
        return TabState.COMPONENT_STATE_BOTTOM_TAB;
    }

    /**
     * get the main Frame for this panel (only applies when
     * a separate window - tab state WINDOW)
     *
     * @return the JFrame of this tab panel
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Set the main Frame for this panel.
     * (only applies when a separate window - tab state WINDOW).
     *
     * @param frame the JFrame of this tab panel.
     */
    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    @Override
    public String toString() {
        return "IgbTabPanel: " + "displayName = " + displayName + ", class = " + this.getClass().getName();
    }

    /**
     * Returns the icon stored in the jar file.
     * It is expected to be at com.affymetrix.igb.igb.gif.
     *
     * @return null if the image file is not found or can't be opened.
     */
    public Icon getIcon() {
        return null;
    }

    /**
     * specify if this is an embedded tab (included in the IGB distribution).
     * DO NOT override this value, unless you are creating a tab that
     * will be included in the real IGB distribution. If you are not
     * sure, then leave as is (false).
     *
     * @return true if this is an embedded tab panel, false otherwise
     */
    public boolean isEmbedded() {
        return false;
    }

    /**
     * there are some tabs that, when they are moved to a separate popup
     * window, they are too small. For those tabs, we handle them specially,
     * so that the size is OK.
     *
     * @return if this tab needs to have the minimum size checked
     */
    public boolean isCheckMinimumWindowSize() {
        return false;
    }

    public final Rectangle getTrayRectangle() {
        return trayRectangle;
    }

    public final void setTrayRectangle(Rectangle tabRectangle) {
        this.trayRectangle = tabRectangle;
    }

    @Override
    public int compareTo(IgbTabPanel o) {
        if (o == null) {
            return 1;
        }
        int ret = Integer.valueOf(position).compareTo(o.position);
        if (ret != 0) {
            return ret;
        }
        return this.getDisplayName().compareTo(o.getDisplayName());
    }

    public void saveSession() {
    }

    public void loadSession() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean consecutiveOK() {
        return true;
    }

    public void select() {
        if (getParent().getParent() instanceof TabHolder) {
            ((TabHolder) getParent().getParent()).selectTab(this);
        }
    }

    // dummy operations for GUIBuilder
    public void setDefaultCloseOperation(int i) {
    } // ignore

    public Container getContentPane() {
        return this;
    }

    public void pack() {
    }

    @Override
    public IgbTabPanel getIgbTabPanel() {
        return this;
    }
}
