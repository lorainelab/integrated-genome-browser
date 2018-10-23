package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.igb.IGBConstants;
import java.awt.Color;
import javax.swing.JColorChooser;

public class SoftClipColorAction extends ChangeColorActionA {

    private static final long serialVersionUID = 1L;
    private static final SoftClipColorAction ACTION = new SoftClipColorAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static SoftClipColorAction getAction() {
        return ACTION;
    }

    public SoftClipColorAction() {
        super(IGBConstants.BUNDLE.getString("softClipColor"), null, null); 
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
