package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.event.ActionEvent;

public class ShowMinusStrandAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final ShowMinusStrandAction ACTION = new ShowMinusStrandAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ShowMinusStrandAction getAction() {
        return ACTION;
    }

    private ShowMinusStrandAction() {
        super("Show All (-) Tiers", "16x16/actions/blank_placeholder.png", null);
        this.putValue(SELECTED_KEY, AffyTieredMap.isShowMinus());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        AffyTieredMap.setShowMinus(!AffyTieredMap.isShowMinus());
        AffyTieredMap map = IGB.getSingleton().getMapView().getSeqMap();
        map.repackTheTiers(false, true);
    }

    @Override
    public boolean isToggle() {
        return true;
    }
}
