package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.util.List;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TrackConstants;

public abstract class SetDirectionStyleActionA extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;

	public SetDirectionStyleActionA(String text, String iconPath, String largeIconPath) {
		super(text, iconPath, largeIconPath);
	}

	protected boolean isArrowStyle(ITrackStyleExtended style) {
		return style.getDirectionType() == TrackConstants.DIRECTION_TYPE.ARROW.ordinal() ||
			style.getDirectionType() == TrackConstants.DIRECTION_TYPE.BOTH.ordinal();
	}

	protected boolean isColorStyle(ITrackStyleExtended style) {
		return style.getDirectionType() == TrackConstants.DIRECTION_TYPE.COLOR.ordinal() ||
				style.getDirectionType() == TrackConstants.DIRECTION_TYPE.BOTH.ordinal();
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
			TrackConstants.DIRECTION_TYPE directionType = null;
			if (!isArrowStyle(style) && !isColorStyle(style)) {
				directionType = TrackConstants.DIRECTION_TYPE.NONE;
			}
			if (isArrowStyle(style) && !isColorStyle(style)) {
				directionType = TrackConstants.DIRECTION_TYPE.ARROW;
			}
			if (!isArrowStyle(style) && isColorStyle(style)) {
				directionType = TrackConstants.DIRECTION_TYPE.COLOR;
			}
			if (isArrowStyle(style) && isColorStyle(style)) {
				directionType = TrackConstants.DIRECTION_TYPE.BOTH;
			}
			style.setDirectionType(directionType.ordinal());
			
			// Turn off color by RGB when direction type is either both or color
			if(directionType == TrackConstants.DIRECTION_TYPE.BOTH || directionType == TrackConstants.DIRECTION_TYPE.COLOR){
				style.setColorProvider(null);
			}
		}
	}
}
