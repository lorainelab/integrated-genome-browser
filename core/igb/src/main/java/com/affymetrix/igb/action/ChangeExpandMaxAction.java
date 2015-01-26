package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.awt.event.ActionEvent;
import java.util.List;

public class ChangeExpandMaxAction extends ChangeExpandMaxActionA {

    private static final long serialVersionUID = 1L;
    private static final ChangeExpandMaxAction ACTION = new ChangeExpandMaxAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ChangeExpandMaxAction getAction() {
        return ACTION;
    }

    private ChangeExpandMaxAction() {
        super(BUNDLE.getString("changeExpandMaxAction"), "16x16/actions/max_stack_depth.png",
                "22x22/actions/max_stack_depth.png");
        putValue(SHORT_DESCRIPTION, BUNDLE.getString("changeExpandMaxActionToolTip"));
    }

    @Override
    protected List<TierLabelGlyph> getTiers() {
        return getTierManager().getSelectedTierLabels();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        changeExpandMax();
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }

    @Override
    public boolean isEnabled() {
        return (Selections.allGlyphs.size() > 0 && Selections.isAllAnnot());
    }

}
