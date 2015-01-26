package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.igb.IGBConstants;
import java.awt.Color;
import javax.swing.JColorChooser;

public class ChangeForegroundColorAction extends ChangeColorActionA {

    private static final long serialVersionUID = 1L;
    private static final ChangeForegroundColorAction ACTION = new ChangeForegroundColorAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ChangeForegroundColorAction getAction() {
        return ACTION;
    }

    public ChangeForegroundColorAction() {
        super(IGBConstants.BUNDLE.getString("changeFGColorAction"), "16x16/actions/FG_color.png", "22x22/actions/FG_color.png");
    }

    @Override
    protected void setChooserColor(JColorChooser chooser, ITrackStyleExtended style) {
        chooser.setColor(style.getForeground());
    }

    @Override
    protected void setStyleColor(Color color, ITrackStyleExtended style) {
        style.setForeground(color);
    }

}
