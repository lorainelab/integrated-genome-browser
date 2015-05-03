package com.affymetrix.igb.action;

import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.igb.IGBConstants;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.lorainelab.igb.genoviz.extensions.TierGlyph;
import com.affymetrix.igb.tiers.SeqMapViewPopup;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UseAsReferenceSeqAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static final UseAsReferenceSeqAction ACTION = new UseAsReferenceSeqAction();

    public static UseAsReferenceSeqAction getAction() {
        return ACTION;
    }

    private UseAsReferenceSeqAction() {
        super(BUNDLE.getString("useAsReferenceSeqAction"), null, null);
    }

    private void useTrackAsReferenceSequence(TierGlyph tier) throws Exception {
        ITrackStyleExtended style = tier.getAnnotStyle();
        DataSet feature = style.getFeature();
        GeneralLoadView.getLoadView().useAsRefSequence(feature);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            super.actionPerformed(e);
            List<TierGlyph> current_tiers = getTierManager().getSelectedTiers();
            if (current_tiers.size() > 1) {
                ErrorHandler.errorPanel(IGBConstants.BUNDLE.getString("multTrackError"));
            }
            useTrackAsReferenceSequence(current_tiers.get(0));
        } catch (Exception ex) {
            Logger.getLogger(SeqMapViewPopup.class.getPackage().getName()).log(
                    Level.SEVERE, null, ex);
        }
    }

}
