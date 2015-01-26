package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.igb.IGBConstants;
import java.awt.Color;
import javax.swing.JColorChooser;

public class ChangeForwardColorAction extends ChangeColorActionA {

    private static final long serialVersionUID = 1L;
    private static final ChangeForwardColorAction ACTION = new ChangeForwardColorAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ChangeForwardColorAction getAction() {
        return ACTION;
    }

    public ChangeForwardColorAction() {
        super(IGBConstants.BUNDLE.getString("changeForwardColorAction"), null, null);
    }

    @Override
    protected void setChooserColor(JColorChooser chooser, ITrackStyleExtended style) {
        chooser.setColor(style.getForwardColor());
    }

    @Override
    protected void setStyleColor(Color color, ITrackStyleExtended style) {
        style.setForwardColor(color);
    }
}
