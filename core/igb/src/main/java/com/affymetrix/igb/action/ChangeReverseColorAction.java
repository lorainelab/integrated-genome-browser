package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.igb.IGBConstants;
import java.awt.Color;
import javax.swing.JColorChooser;

public class ChangeReverseColorAction extends ChangeColorActionA {

    private static final long serialVersionUID = 1L;
    private static final ChangeReverseColorAction ACTION = new ChangeReverseColorAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ChangeReverseColorAction getAction() {
        return ACTION;
    }

    public ChangeReverseColorAction() {
        super(IGBConstants.BUNDLE.getString("changeReverseColorAction"), null, null);
    }

    @Override
    protected void setChooserColor(JColorChooser chooser, ITrackStyleExtended style) {
        chooser.setColor(style.getReverseColor());
    }

    @Override
    protected void setStyleColor(Color color, ITrackStyleExtended style) {
        style.setReverseColor(color);
    }
}
