package com.affymetrix.igb.action;

import com.affymetrix.igb.shared.RepackTiersAction;
import java.util.List;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.ParameteredAction;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.MaxSlotsChooser;
import com.affymetrix.igb.tiers.TierLabelGlyph;

public abstract class ChangeExpandMaxActionA extends RepackTiersAction implements ParameteredAction{
	private static final long serialVersionUID = 1L;

	protected ChangeExpandMaxActionA(String text, String iconPath, String largeIconPath) {
		super(text, iconPath, largeIconPath);
	}

	protected abstract List<TierLabelGlyph> getTiers();
	
	public void changeExpandMax(int max) {
		List<TierLabelGlyph> tier_label_glyphs = getTiers();
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
		this.getSeqMapView().seqMapRefresh();
		this.getSeqMapView().getSeqMap().updateWidget();
	}

	@Override
	public void performAction(Object parameter){
		if(parameter.getClass() != Integer.class)
			return; 
		
		changeExpandMax((Integer)parameter);
	}
	
	
	public int getOptimum() {
		List<TierLabelGlyph> theTiers = getTierManager().getAllTierLabels();
		int ourOptimum = 1;
		for (TierLabelGlyph tlg : theTiers) {
			TierGlyph tg = (TierGlyph) tlg.getInfo();
			if(tg.getAnnotStyle().isGraphTier())
				continue;
			ourOptimum = Math.max(ourOptimum, tg.getSlotsNeeded(getSeqMapView().getSeqMap().getView()));
		}
		return ourOptimum;
	}

	protected void changeExpandMax() {
		List<TierLabelGlyph> theTiers = getTiers();
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

		int ourOptimum = getOptimum();
		for (TierLabelGlyph tlg : theTiers) {
			TierGlyph tg = (TierGlyph) tlg.getInfo();
			ourOptimum = Math.max(ourOptimum, tg.getSlotsNeeded(getSeqMapView().getSeqMap().getView()));
		}

		MaxSlotsChooser chooser = new MaxSlotsChooser(IGBConstants.BUNDLE.getString("maxHeight"), ourLimit, ourOptimum, this);
		chooser.setVisible(true);
		
	}
}
