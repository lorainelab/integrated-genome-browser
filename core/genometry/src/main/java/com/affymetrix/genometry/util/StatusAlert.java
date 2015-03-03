package com.affymetrix.genometry.util;

import javax.swing.Icon;

/**
 *
 * @author hiralv
 */
public interface StatusAlert {

    public final int HIDE_ALERT = 0;
    public final int KEEP_ALERT = 1;

    /**
     * Icon to be displayed
     *
     * @return
     */
    public Icon getIcon();

    /**
     * Message to be displayed next to icon
     *
     * @return
     */
    public String getDisplayMessage();

    /**
     * Tooltip when mouse hovered over
     *
     * @return
     */
    public String getToolTip();

    /**
     * Action to be performed when clicked
     *
     * @return integer 1 to keep and 0 to remove.
     */
    public int actionPerformed();
}
