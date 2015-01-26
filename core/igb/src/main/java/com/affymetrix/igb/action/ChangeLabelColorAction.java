package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.igb.IGBConstants;
import java.awt.Color;
import javax.swing.JColorChooser;

public class ChangeLabelColorAction extends ChangeColorActionA {

    private static final long serialVersionUID = 1L;
    private static final ChangeLabelColorAction ACTION = new ChangeLabelColorAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ChangeLabelColorAction getAction() {
        return ACTION;
    }

    public ChangeLabelColorAction() {
        super(IGBConstants.BUNDLE.getString("changeLabelColorAction"), "16x16/actions/label_color.png", "22x22/actions/label_color.png");
        iterateMultiGraph(false);
    }

    @Override
    protected void setChooserColor(JColorChooser chooser, ITrackStyleExtended style) {
        chooser.setColor(style.getLabelForeground());
    }

    @Override
    protected void setStyleColor(Color color, ITrackStyleExtended style) {
        style.setLabelForeground(color);
    }
}
