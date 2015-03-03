package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.lorainelab.igb.genoviz.extensions.TierGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.event.ActionEvent;
import java.util.List;

public class ShowAllAction extends RepackTiersAction {

    private static final long serialVersionUID = 1L;
    private static final ShowAllAction ACTION = new ShowAllAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ShowAllAction getAction() {
        return ACTION;
    }

    private ShowAllAction() {
        super(BUNDLE.getString("showAllAction"),
                "16x16/actions/show_all.png",
                "22x22/actions/show_all.png");
        this.ordinal = -6008700;
    }

    public void showAllTiers() {
        List<TierLabelGlyph> tiervec = getTierManager().getAllTierLabels();

        for (TierLabelGlyph label : tiervec) {
            TierGlyph tier = (TierGlyph) label.getInfo();
            ITrackStyleExtended style = tier.getAnnotStyle();
            if (style != null) {
                style.setShow(true);
                tier.setVisibility(true);
            }
        }
        getTierManager().sortTiers();
        GeneralLoadView.getLoadView().refreshDataManagementView();
        repack(false, true);
        //refreshMap(false, true); // when re-showing all tier, do strech_to_fit in the y-direction
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        showAllTiers();
    }
}
