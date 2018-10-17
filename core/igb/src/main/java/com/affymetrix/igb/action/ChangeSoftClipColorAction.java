package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.igb.IGBConstants;
import java.awt.Color;
import javax.swing.JColorChooser;

public class ChangeSoftClipColorAction extends ChangeColorActionA {

    private static final long serialVersionUID = 1L;
    private static final ChangeSoftClipColorAction ACTION = new ChangeSoftClipColorAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ChangeSoftClipColorAction getAction() {
        return ACTION;
    }

    public ChangeSoftClipColorAction() {
        super(IGBConstants.BUNDLE.getString("softClipColor"), "16x16/actions/blank_placeholder.png", "22x22/actions/blank_placeholder.png");
    }

    @Override
    protected void setChooserColor(JColorChooser chooser, ITrackStyleExtended style) {
        chooser.setColor(style.getsoftClipColor());
    }

    @Override
    protected void setStyleColor(Color color, ITrackStyleExtended style) {
        style.setsoftClipColor(color);
    }

}
