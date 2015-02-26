package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;

import static com.affymetrix.igb.shared.Selections.allStyles;
import static com.affymetrix.igb.shared.Selections.graphStates;
import com.affymetrix.igb.tiers.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TrackStyle;
import java.awt.event.ActionEvent;

public class RestoreToDefaultAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;

    private static final RestoreToDefaultAction ACTION = new RestoreToDefaultAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static RestoreToDefaultAction getAction() {
        return ACTION;
    }

    public RestoreToDefaultAction() {
        super("Restore track to Default settings", null, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (getTierManager().getSelectedTiers() == null) {
            return;
        }
        allStyles.stream().filter(style -> style instanceof TrackStyle).forEach(style -> {
            ((TrackStyle) style).restoreToDefault();
        });

        graphStates.forEach(com.affymetrix.genometry.style.GraphState::restoreToDefault);

        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
        getSeqMapView().updatePanel();
        getSeqMapView().getPopup().refreshMap(false, true);
    }
}
