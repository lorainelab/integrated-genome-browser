package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.AffyTieredMap;
import static com.affymetrix.igb.tiers.AffyTieredMap.SHOW_MIXED;
import java.awt.event.ActionEvent;

public class ShowMixedStrandAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final ShowMixedStrandAction ACTION = new ShowMixedStrandAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ShowMixedStrandAction getAction() {
        return ACTION;
    }

    private ShowMixedStrandAction() {
        super("Show (+/-) Tiers", null, null);
        this.putValue(SELECTED_KEY, SHOW_MIXED);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        SHOW_MIXED = !SHOW_MIXED;
        AffyTieredMap map = IGB.getInstance().getMapView().getSeqMap();
        map.repackTheTiers(false, true);
    }

    @Override
    public boolean isToggle() {
        return true;
    }
}
