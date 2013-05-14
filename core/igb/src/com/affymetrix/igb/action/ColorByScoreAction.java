package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.color.ColorProvider;
import com.affymetrix.genometryImpl.color.Score;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;

public class ColorByScoreAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final ColorByScoreAction ACTION = new ColorByScoreAction("colorByScoreAction");
		
	static{
//		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ColorByScoreAction getAction() {
		return ACTION;
	}

	private ColorByScoreAction(String transKey) {
		super(BUNDLE.getString(transKey), "16x16/actions/blank_placeholder.png", null);
	}

	private void setColorByScore(TierLabelGlyph tlg) {
		ITrackStyleExtended style = tlg.getReferenceTier().getAnnotStyle();
		ColorProvider cp = style.getColorProvider();
		if(cp instanceof Score){
			cp = null;
		}else{
			cp = new Score();
			//((Score)cp).setTrackStyle(style);
		}
		style.setColorProvider(cp);
		this.putValue(SELECTED_KEY, cp != null);
		refreshMap(false, false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		setColorByScore(getTierManager().getSelectedTierLabels().get(0));
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}

}
