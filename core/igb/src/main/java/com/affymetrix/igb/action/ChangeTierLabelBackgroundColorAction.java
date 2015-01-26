/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import java.awt.Color;
import java.awt.event.ActionEvent;

/**
 *
 * @author dcnorris
 */
public class ChangeTierLabelBackgroundColorAction extends ChangeTierLabelColorActionA {

    protected static final java.awt.Color DEFAULT_COLOR = javax.swing.UIManager.getColor("Button.background");
    private static final long serialVersionUID = 1L;
    private static final ChangeTierLabelBackgroundColorAction ACTION = new ChangeTierLabelBackgroundColorAction();

    private ChangeTierLabelBackgroundColorAction() {
        super(BUNDLE.getString("changeTierLabelBackgroundColorAction"), null, null);
    }

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ChangeTierLabelBackgroundColorAction getAction() {
        return ACTION;
    }

    public Color getBackgroundColor() {
        return DEFAULT_COLOR;
    }

    public Color getForegroundColor() {
        return DEFAULT_COLOR.brighter();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        changeColor(false);
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }
}
