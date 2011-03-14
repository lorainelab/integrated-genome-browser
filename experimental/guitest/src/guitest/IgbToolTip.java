/*
 * A custom JToolTip that stays put when the user mouses over it, and also
 * let's users click and follow hyperlinks. This may not work!
 *
 * See 
 * http://www.codeguru.com/java/articles/122.shtml
 * http://www.exampledepot.com/egs/javax.swing/tooltip_StayTt.html
 */

package guitest;

import java.awt.event.MouseEvent;
import javax.swing.JToolTip;
import java.awt.event.MouseMotionListener;

/**
 * @author aloraine
 */
public class IgbToolTip extends JToolTip implements MouseMotionListener {

    public void mouseDragged(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseMoved(MouseEvent me) {
        //If the mouse moves into me, stay visible
        System.err.println("I got a MouseEvent. Yay for me!");
    }



}

