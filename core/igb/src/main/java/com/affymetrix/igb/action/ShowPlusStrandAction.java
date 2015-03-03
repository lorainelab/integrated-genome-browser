package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.event.ActionEvent;

public class ShowPlusStrandAction extends GenericAction {

    public static final String COMPONENT_NAME = "ShowPlusStrandAction";
    private static final long serialVersionUID = 1L;
    private static final ShowPlusStrandAction ACTION = new ShowPlusStrandAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ShowPlusStrandAction getAction() {
        return ACTION;
    }

    private ShowPlusStrandAction() {
        super("Show All (+) Tiers", "16x16/actions/blank_placeholder.png", null);
        this.putValue(SELECTED_KEY, AffyTieredMap.isShowPlus());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        AffyTieredMap.setShowPlus(!AffyTieredMap.isShowPlus());
        AffyTieredMap map = IGB.getSingleton().getMapView().getSeqMap();
        map.repackTheTiers(false, true);
    }

    @Override
    public boolean isToggle() {
        return true;
    }
    
    @Override
    public boolean isToolbarAction() {
        return false;
    }
}
