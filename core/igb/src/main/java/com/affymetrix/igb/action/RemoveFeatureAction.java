package com.affymetrix.igb.action;

import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.event.SymSelectionEvent;
import com.affymetrix.genometry.event.SymSelectionListener;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import static com.affymetrix.igb.shared.Selections.allStyles;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class RemoveFeatureAction extends SeqMapViewActionA implements SymSelectionListener {

    private static final long serialVersionUID = 1L;
    private static final RemoveFeatureAction ACTION = new RemoveFeatureAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
        ACTION.setEnabled(false);
        GenometryModel.getInstance().addSymSelectionListener(ACTION);
    }

    public static RemoveFeatureAction getAction() {
        return ACTION;
    }

    protected RemoveFeatureAction() {
        super(IGBConstants.BUNDLE.getString("deleteFeatureAction"), null,
                "16x16/actions/delete_track.png",
                "22x22/actions/delete_track.png", KeyEvent.VK_UNDEFINED);
        this.ordinal = -9007300;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (allStyles.isEmpty()) {
            return;
        }

        super.actionPerformed(e);
        String message = "Really remove all selected data set ?";
        if (ModalUtils.confirmPanel(message, PreferenceUtils.CONFIRM_BEFORE_DELETE, PreferenceUtils.default_confirm_before_delete)) {
            allStyles.stream().filter(style -> style.getFeature() != null).forEach(style -> {
                GeneralLoadView.getLoadView().removeFeature(style.getFeature(), true);
            });
        }
        getSeqMapView().dataRemoved();	// refresh
    }

    public void symSelectionChanged(SymSelectionEvent evt) {
        List<TierLabelGlyph> tiers = getTierManager().getSelectedTierLabels();
        this.setEnabled(0 < tiers.size());
    }
}
