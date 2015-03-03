package com.affymetrix.igb.shared;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.igb.tiers.TrackstylePropertyMonitor;
import com.affymetrix.genometry.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.action.TierHeightAction;
import static com.affymetrix.igb.shared.Selections.allGlyphs;
import static com.affymetrix.igb.shared.Selections.isAnyLocked;
import com.affymetrix.igb.view.factories.DefaultTierGlyph;
import com.lorainelab.igb.genoviz.extensions.TierGlyph.TierType;

/**
 *
 * @author hiralv
 */
public class UnlockTierHeightAction extends TierHeightAction {

    private static final long serialVersionUID = 1L;
    private final static UnlockTierHeightAction unlockTierAction = new UnlockTierHeightAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(unlockTierAction);
        Selections.addRefreshSelectionListener(unlockTierAction.enabler);
    }

    public static UnlockTierHeightAction getAction() {
        return unlockTierAction;
    }

    protected Selections.RefreshSelectionListener enabler = () -> {
        if (isAnyLocked()) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    };

    private UnlockTierHeightAction() {
        super(BUNDLE.getString("unlockTierHeightAction"), "16x16/actions/unlock_track.png", "22x22/actions/unlock_track.png");
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        super.actionPerformed(e);
        allGlyphs.stream().filter(glyph -> glyph instanceof DefaultTierGlyph && ((DefaultTierGlyph) glyph).getTierType() == TierType.ANNOTATION).forEach(glyph -> {
            setHeightFixed((DefaultTierGlyph) glyph);
        });

        getTierMap().repackTheTiers(true, true);
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }

    @Override
    protected void setHeightFixed(DefaultTierGlyph dtg) {
        dtg.setHeightFixed(false);
    }
    
    @Override
    public boolean isToolbarAction() {
        return false;
    }
}
