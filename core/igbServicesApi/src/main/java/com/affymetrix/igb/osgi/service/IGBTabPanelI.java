/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.osgi.service;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.osgi.service.IGBTabPanel.TabState;
import com.affymetrix.igb.swing.JRPWidget;
import com.affymetrix.igb.swing.util.Idable;
import java.awt.Container;
import java.awt.Rectangle;
import javax.swing.Icon;
import javax.swing.JFrame;

/**
 *
 * @author dcnorris
 */
public interface IGBTabPanelI extends Comparable<IGBTabPanel>, Idable, JRPWidget {
    
    int DEFAULT_TAB_POSITION = Integer.MAX_VALUE - 1;

    @Override
    int compareTo(IGBTabPanel o);

    @Override
    boolean consecutiveOK();

    Container getContentPane();

    /**
     * get the default / initial state of this tab panel
     * @return the default state of this tab
     */
    TabState getDefaultState();

    /**
     * get the name to display to the user
     * @return the name to display to the user (on the tab)
     */
    String getDisplayName();

    /**
     * get the main Frame for this panel (only applies when
     * a separate window - tab state WINDOW)
     * @return the JFrame of this tab panel
     */
    JFrame getFrame();

    /** Returns the icon stored in the jar file.
     *  It is expected to be at com.affymetrix.igb.igb.gif.
     *  @return null if the image file is not found or can't be opened.
     */
    Icon getIcon();

    @Override
    String getId();

    String getName();

    /**
     * get the position of the tab in the tray
     * @return the tab position
     */
    int getPosition();

    GenericAction getSelectAction();

    /**
     * get the title of the tab panel
     * @return the title
     */
    String getTitle();

    Rectangle getTrayRectangle();

    /**
     * there are some tabs that, when they are moved to a separate popup
     * window, they are too small. For those tabs, we handle them specially,
     * so that the size is OK.
     * @return if this tab needs to have the minimum size checked
     */
    boolean isCheckMinimumWindowSize();

    /**
     * specify if this is an embedded tab (included in the IGB distribution).
     * DO NOT override this value, unless you are creating a tab that
     * will be included in the real IGB distribution. If you are not
     * sure, then leave as is (false).
     * @return true if this is an embedded tab panel, false otherwise
     */
    boolean isEmbedded();

    /**
     * if this tab should be the tab selected when IGB starts
     * @return true if this tab panel should get initial
     * selection/focus, false otherwise
     */
    boolean isFocus();

    void loadSession();

    void pack();

    void saveSession();

    void select();

    // dummy operations for GUIBuilder
    void setDefaultCloseOperation(int i);

    /**
     * Set the main Frame for this panel.
     * (only applies when a separate window - tab state WINDOW).
     * @param frame the JFrame of this tab panel.
     */
    void setFrame(JFrame frame);

    void setTrayRectangle(Rectangle tabRectangle);

    @Override
    String toString();
    
}
