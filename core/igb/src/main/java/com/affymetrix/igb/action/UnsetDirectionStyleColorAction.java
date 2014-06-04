package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

public class UnsetDirectionStyleColorAction extends SetDirectionStyleActionA {

    private static final long serialVersionUID = 1L;
    private static final UnsetDirectionStyleColorAction ACTION
            = new UnsetDirectionStyleColorAction();

//	static{
//		GenericActionHolder.getInstance().addGenericAction(ACTION);
//	}
    public static UnsetDirectionStyleColorAction getAction() {
        return ACTION;
    }

    private UnsetDirectionStyleColorAction() {
        super("Strand: Hide Color",
                "16x16/actions/strandscoloredsame.png",
                "22x22/actions/strandscoloredsame.png");
        this.ordinal = -6006412;
    }

    @Override
    protected boolean isColorStyle(ITrackStyleExtended style) {
        return false;
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        super.actionPerformed(e);
        SetDirectionStyleColorAction.getAction().setEnabled(true);
        this.setEnabled(false);
    }

}
