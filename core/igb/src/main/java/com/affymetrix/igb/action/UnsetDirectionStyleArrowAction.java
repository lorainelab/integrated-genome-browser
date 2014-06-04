package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

public class UnsetDirectionStyleArrowAction extends SetDirectionStyleActionA {

    private static final long serialVersionUID = 1L;
    private static final UnsetDirectionStyleArrowAction ACTION
            = new UnsetDirectionStyleArrowAction();

//	static{
//		GenericActionHolder.getInstance().addGenericAction(ACTION);
//	}
    public static UnsetDirectionStyleArrowAction getAction() {
        return ACTION;
    }

    private UnsetDirectionStyleArrowAction() {
        super("Strand: Hide Arrow",
                "16x16/actions/no_arrows.png",
                "22x22/actions/no_arrows.png");
        this.ordinal = -8006512;
    }

    @Override
    protected boolean isArrowStyle(ITrackStyleExtended style) {
        return false;
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        super.actionPerformed(e);
        SetDirectionStyleArrowAction.getAction().setEnabled(true);
        this.setEnabled(false);
    }

}
