package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.igb.IGBConstants;
import java.awt.Color;
import javax.swing.JColorChooser;

public class ChangeBackgroundColorAction extends ChangeColorActionA {

    private static final long serialVersionUID = 1L;
    private static final ChangeBackgroundColorAction ACTION = new ChangeBackgroundColorAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ChangeBackgroundColorAction getAction() {
        return ACTION;
    }

    public ChangeBackgroundColorAction() {
        super(IGBConstants.BUNDLE.getString("changeBGColorAction"),
                "16x16/actions/BG_color.png",
                "22x22/actions/BG_color.png");
        this.ordinal = -6008100;
        iterateMultiGraph(false);
    }

    @Override
    protected void setChooserColor(JColorChooser chooser, ITrackStyleExtended style) {
        chooser.setColor(style.getBackground());
    }

    @Override
    protected void setStyleColor(Color color, ITrackStyleExtended style) {
        style.setBackground(color);
    }
}
