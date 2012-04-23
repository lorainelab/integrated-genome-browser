package com.affymetrix.igb.action;

import java.util.List;

import javax.swing.JOptionPane;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.TierGlyph;

import com.affymetrix.igb.tiers.MaxSlotsChooser;
import com.affymetrix.igb.tiers.TierLabelGlyph;

public abstract class ChangeExpandMaxActionA extends RepackTiersAction {
	private static final long serialVersionUID = 1L;

	protected ChangeExpandMaxActionA(String text, String iconPath) {
		super(text, iconPath);
	}

	public void changeExpandMax(List<TierLabelGlyph> tier_label_glyphs, int max) {
		for (TierLabelGlyph tlg : tier_label_glyphs) {
			TierGlyph tier = tlg.getReferenceTier();
			ITrackStyleExtended style = tier.getAnnotStyle();
			switch (tier.getDirection()) {
				case FORWARD:
					style.setForwardMaxDepth(max);
					break;
				case REVERSE:
					style.setReverseMaxDepth(max);
					break;
				default:
				case BOTH:
				case NONE:
				case AXIS:
					style.setMaxDepth(max);
			}
		}
		repack(true);
	}

	protected void changeExpandMax(List<TierLabelGlyph> theTiers) {
		if (theTiers == null || theTiers.isEmpty()) {
			ErrorHandler.errorPanel("changeExpandMaxAll called with an empty list");
			return;
		}

		int ourLimit = 0;
		// Shouldn't we set the limit to the max of the limits in the tiers (remember n < 0 for all n).
		// Then we could combine this with the loop below.
		if (theTiers.size() == 1) {
			TierLabelGlyph tlg = theTiers.get(0);
			TierGlyph tg = (TierGlyph) tlg.getInfo();
			ITrackStyleExtended style = tg.getAnnotStyle();
			if (style != null) {
				switch (tg.getDirection()) {
					case FORWARD:
						ourLimit = style.getForwardMaxDepth();
						break;
					case REVERSE:
						ourLimit = style.getReverseMaxDepth();
						break;
					default:
						ourLimit = style.getMaxDepth();
				}
			}
		}

		int ourOptimum = 1;
		for (TierLabelGlyph tlg : theTiers) {
			TierGlyph tg = (TierGlyph) tlg.getInfo();
			ourOptimum = Math.max(ourOptimum, tg.getSlotsNeeded(getSeqMapView().getSeqMap().getView()));
		}

		MaxSlotsChooser chooser = new MaxSlotsChooser(IGBConstants.BUNDLE.getString("maxHeight"), ourLimit, ourOptimum, theTiers, this);
		
		/*int isOK = JOptionPane.showConfirmDialog(
				null,
				chooser,
				IGBConstants.BUNDLE.getString("changeMaxHeight"),
				JOptionPane.OK_CANCEL_OPTION);
		switch (isOK) {
			case JOptionPane.OK_OPTION:
				try {
					ourLimit = chooser.getValue();
				} catch (NumberFormatException nex) {
					ErrorHandler.errorPanel(nex.getLocalizedMessage()
							+ " Maximum must be an integer: "
							+ chooser.toString());
					return;
				}
				break;
			default:
				return;
		} */
		int newLimit = chooser.getValue();
		if(newLimit != ourLimit)
			changeExpandMax(theTiers, newLimit);
	}
}
