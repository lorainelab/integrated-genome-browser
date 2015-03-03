package com.affymetrix.igb.action;

import com.affymetrix.igb.IGBConstants;
import java.awt.event.ActionEvent;

public class RepackAllTiersAction extends RepackTiersAction {

    private static final long serialVersionUID = 1L;
    private static final RepackAllTiersAction ACTION = new RepackAllTiersAction();

    public RepackAllTiersAction() {
        super(IGBConstants.BUNDLE.getString("repackAllTracksAction"),
                "16x16/actions/Repack_all.png",
                "22x22/actions/Repack_all.png");
        this.ordinal = -6008510;
    }

    public static RepackAllTiersAction getAction() {
        return ACTION;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        repackTiers(getTierManager().getAllTierLabels());
    }
}
