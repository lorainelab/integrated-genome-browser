package com.affymetrix.igb.action;

import com.affymetrix.genometry.style.ITrackStyleExtended;

public class SetDirectionStyleArrowAction extends SetDirectionStyleActionA {

    private static final long serialVersionUID = 1L;
    private static final SetDirectionStyleArrowAction ACTION
            = new SetDirectionStyleArrowAction();

//	static{
//		GenericActionHolder.getInstance().addGenericAction(ACTION);
//	}
    public static SetDirectionStyleArrowAction getAction() {
        return ACTION;
    }

    private SetDirectionStyleArrowAction() {
        super("Strand: Show Arrow",
                "16x16/actions/arrows.png",
                "22x22/actions/arrows.png");
        this.ordinal = -8006611;
    }

    @Override
    protected boolean isArrowStyle(ITrackStyleExtended style) {
        return true;
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        super.actionPerformed(e);
        UnsetDirectionStyleArrowAction.getAction().setEnabled(true);
        this.setEnabled(false);
    }

}
