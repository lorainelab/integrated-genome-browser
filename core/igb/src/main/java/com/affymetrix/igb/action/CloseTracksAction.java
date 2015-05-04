package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.SymSelectionEvent;
import com.affymetrix.genometry.event.SymSelectionListener;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.igb.IGBConstants;
import com.lorainelab.igb.genoviz.extensions.GraphGlyph;
import static com.affymetrix.igb.shared.Selections.allGlyphs;
import com.lorainelab.igb.genoviz.extensions.StyledGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.event.ActionEvent;
import java.util.List;

public class CloseTracksAction extends SeqMapViewActionA implements SymSelectionListener {

    private static final long serialVersionUID = 1L;
    private static final CloseTracksAction ACTION = new CloseTracksAction();

    //static{
    //	GenericActionHolder.getInstance().addGenericAction(ACTION);
    //	ACTION.setEnabled(false);
    //	GenometryModel.getInstance().addSymSelectionListener(ACTION);
    //}
    public static CloseTracksAction getAction() {
        return ACTION;
    }

    protected CloseTracksAction() {
        super(IGBConstants.BUNDLE.getString("closeTracksAction"),
                "16x16/status/image-missing.png",
                "22x22/status/image-missing.png");
        this.ordinal = -9007200;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String message = "Really remove entire data sets?";

        if (allGlyphs.size() == 1) {
            message = "Really remove entire " + allGlyphs.get(0).getAnnotStyle().getFeature().getDataSetName() + " data set?";
        }

        if (ModalUtils.confirmPanel(message, PreferenceUtils.CONFIRM_BEFORE_DELETE, PreferenceUtils.default_confirm_before_delete)) {

            super.actionPerformed(e);
            // First split the graph.
            //If graphs is joined then apply color to combo style too.
// TODO: Use code from split graph
            allGlyphs.stream().filter(vg -> vg instanceof GraphGlyph).forEach(vg -> {
                ITrackStyleExtended style = ((GraphGlyph) vg).getGraphState().getComboStyle();
                if (style != null) {
                    getSeqMapView().split(vg);
                }
            });

            for (StyledGlyph vg : allGlyphs) {
                DataSet gFeature = vg.getAnnotStyle().getFeature();
                if (gFeature != null) {
                    GeneralLoadView.getLoadView().removeDataSet(gFeature, true);
                }
            }
        }
    }

    /**
     * Override to enable only when there are tracks to close.
     */
    @Override
    public void symSelectionChanged(SymSelectionEvent evt) {
        List<TierLabelGlyph> tiers = getTierManager().getSelectedTierLabels();
        this.setEnabled(0 < tiers.size());
    }

}
