package com.affymetrix.igb.shared;

import com.lorainelab.igb.genoviz.extensions.api.StyledGlyph;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.action.TierHeightAction;
import static com.affymetrix.igb.shared.Selections.allGlyphs;
import static com.affymetrix.igb.shared.Selections.isAllButOneLocked;
import static com.affymetrix.igb.shared.Selections.isAnyLockable;
import com.affymetrix.igb.view.factories.DefaultTierGlyph;

/**
 *
 * @author hiralv
 */
public class LockTierHeightAction extends TierHeightAction {

    private static final long serialVersionUID = 1L;
    private final static LockTierHeightAction lockTierAction = new LockTierHeightAction();

    private Selections.RefreshSelectionListener enabler = new Selections.RefreshSelectionListener() {

        @Override
        public void selectionRefreshed() {
            if ((!isAllButOneLocked() && isAnyLockable())) {
                setEnabled(true);
            } else {
                setEnabled(false);
            }
        }

    };

    public static LockTierHeightAction getAction() {
        return lockTierAction;
    }

    static {
        GenericActionHolder.getInstance().addGenericAction(lockTierAction);
        Selections.addRefreshSelectionListener(lockTierAction.enabler);
    }

    private LockTierHeightAction() {
        super(BUNDLE.getString("lockTierHeightAction"), "16x16/actions/lock_track.png", "22x22/actions/lock_track.png");
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        super.actionPerformed(e);
        StyledGlyph[] glyphs = allGlyphs.toArray(new StyledGlyph[allGlyphs.size()]);
        int len = getTierManager().getVisibleTierGlyphs().size() - 1 == glyphs.length ? glyphs.length - 1 : glyphs.length;
        StyledGlyph glyph;
        for (int i = 0; i < len; i++) {
            glyph = glyphs[i];
            if (glyph instanceof DefaultTierGlyph && ((DefaultTierGlyph) glyph).getTierType() == TierGlyph.TierType.ANNOTATION) {
                setHeightFixed((DefaultTierGlyph) glyph);
            }
        }

        getTierMap().repackTheTiers(true, true);
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }

    @Override
    protected void setHeightFixed(DefaultTierGlyph dtg) {
        dtg.setHeightFixed(true);
    }
}
