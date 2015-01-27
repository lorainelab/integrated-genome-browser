/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.service.api;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 *
 * @author dcnorris
 */
public interface IgbTabPanelI {

    int DEFAULT_TAB_POSITION = Integer.MAX_VALUE - 1;

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

    /**
     * get the default / initial state of this tab panel
     *
     * @return the default state of this tab
     */
    public TabState getDefaultTabState();

    public String getComponentName();
    
    public String getDisplayName();

    public JPanel getTabContent();

    public int getPosition();
}
