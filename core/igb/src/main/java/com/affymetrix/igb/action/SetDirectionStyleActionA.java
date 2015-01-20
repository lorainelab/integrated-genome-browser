package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TrackConstants;
import java.awt.event.ActionEvent;
import java.util.List;

public abstract class SetDirectionStyleActionA extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;

    public SetDirectionStyleActionA(String text, String iconPath, String largeIconPath) {
        super(text, iconPath, largeIconPath);
    }

    protected boolean isArrowStyle(ITrackStyleExtended style) {
        return style.getDirectionType() == TrackConstants.DirectionType.ARROW.ordinal()
                || style.getDirectionType() == TrackConstants.DirectionType.BOTH.ordinal();
    }

    protected boolean isColorStyle(ITrackStyleExtended style) {
        return style.getDirectionType() == TrackConstants.DirectionType.COLOR.ordinal()
                || style.getDirectionType() == TrackConstants.DirectionType.BOTH.ordinal();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        changeDirectionStyle(getTierManager().getSelectedTiers());
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }

    private void changeDirectionStyle(final List<TierGlyph> tier_glyphs) {
        for (TierGlyph tier : tier_glyphs) {
            ITrackStyleExtended style = tier.getAnnotStyle();

            if (style == null) {
                continue;
            }
            TrackConstants.DirectionType directionType = null;
            if (!isArrowStyle(style) && !isColorStyle(style)) {
                directionType = TrackConstants.DirectionType.NONE;
            }
            if (isArrowStyle(style) && !isColorStyle(style)) {
                directionType = TrackConstants.DirectionType.ARROW;
            }
            if (!isArrowStyle(style) && isColorStyle(style)) {
                directionType = TrackConstants.DirectionType.COLOR;
            }
            if (isArrowStyle(style) && isColorStyle(style)) {
                directionType = TrackConstants.DirectionType.BOTH;
            }
            style.setDirectionType(directionType.ordinal());

            // Turn off color by RGB when direction type is either both or color
            if (directionType == TrackConstants.DirectionType.BOTH || directionType == TrackConstants.DirectionType.COLOR) {
                style.setColorProvider(null);
            }
        }
    }
}
