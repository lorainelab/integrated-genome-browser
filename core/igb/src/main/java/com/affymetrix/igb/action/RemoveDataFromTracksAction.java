package com.affymetrix.igb.action;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.GraphGlyph;
import static com.affymetrix.igb.shared.Selections.allGlyphs;
import static com.affymetrix.igb.shared.Selections.allStyles;

import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;

public class RemoveDataFromTracksAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static final RemoveDataFromTracksAction ACTION = new RemoveDataFromTracksAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static RemoveDataFromTracksAction getAction() {
        return ACTION;
    }

    protected RemoveDataFromTracksAction() {
        super(IGBConstants.BUNDLE.getString("deleteAction"), null,
                "16x16/actions/remove_data.png",
                "22x22/actions/remove_data.png", KeyEvent.VK_UNDEFINED);
        this.ordinal = -9007300;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        BioSeq seq = GenometryModel.getInstance().getSelectedSeq();

        if (IGB.confirmPanel(MessageFormat.format(IGBConstants.BUNDLE.getString("confirmDelete"), seq.getID()),
                PreferenceUtils.CONFIRM_BEFORE_CLEAR, PreferenceUtils.default_confirm_before_clear)) {

            // First split the graph.
            //If graphs is joined then apply color to combo style too.
// TODO: Use code from split graph
            allGlyphs.stream().filter(vg -> vg instanceof GraphGlyph).forEach(vg -> {
                ITrackStyleExtended style = ((GraphGlyph) vg).getGraphState().getComboStyle();
                if (style != null) {
                    getSeqMapView().split(vg);
                }
            });

            for (ITrackStyleExtended style : allStyles) {
                GeneralLoadView.getLoadView().clearTrack(style);
            }
        }
        //getSeqMapView().dataRemoved();	// refresh
    }
}
