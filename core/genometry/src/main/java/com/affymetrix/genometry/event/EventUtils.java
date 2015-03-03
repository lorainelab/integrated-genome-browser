package com.affymetrix.genometry.event;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

/**
 *
 * @author hiralv
 */
public abstract class EventUtils {

    /**
     * Should trigger a pop up menu on whatever platform is being used. Notice
     * that this differs from
     * {@link java.awt.event.MouseEvent#isPopupTrigger() AWT's Version}.
     */
    public static boolean isOurPopupTrigger(MouseEvent evt) {
        int mods = evt.getModifiers();
        return (evt.isMetaDown()
                || evt.isControlDown() // (for Apple Macintosh Computers)
                || ((mods & InputEvent.BUTTON3_MASK) != 0));
    }
}
