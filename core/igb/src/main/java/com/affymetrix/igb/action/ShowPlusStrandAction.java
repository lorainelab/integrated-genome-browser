package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.AffyTieredMap;
import static com.affymetrix.igb.tiers.AffyTieredMap.SHOW_PLUS;
import java.awt.event.ActionEvent;

public class ShowPlusStrandAction extends GenericAction {

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
        this.putValue(SELECTED_KEY, SHOW_PLUS);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        SHOW_PLUS = !SHOW_PLUS;
        AffyTieredMap map = IGB.getInstance().getMapView().getSeqMap();
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
