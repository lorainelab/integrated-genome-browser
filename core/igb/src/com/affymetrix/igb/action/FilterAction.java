package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.filter.SymmetryFilterI;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.RepackTiersAction;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 */
public class FilterAction extends RepackTiersAction {
	private static final long serialVersionUID = 1L;
	private final SymmetryFilterI filter;

	public FilterAction(SymmetryFilterI filter, String name) {
		super(name,
				"16x16/actions/hide.png",
				"22x22/actions/hide.png");
		this.filter = filter;
		this.ordinal = -6008400;
	}

	private void filter(List<TierLabelGlyph> tiers) {
		for (TierLabelGlyph g : tiers) {
			if (g.getInfo() instanceof TierGlyph) {
				TierGlyph tier = (TierGlyph) g.getInfo();
				applyFilter(tier);
			}
		}
		repack(true, false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		filter(getTierManager().getSelectedTierLabels());
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}

	@Override
	public boolean isEnabled(){
		return Selections.allGlyphs.size() > 0;
	}
	
	private void applyFilter(TierGlyph tg) {
		tg.getAnnotStyle().setFilter(filter);
		if(filter != null){
			BioSeq annotseq = getSeqMapView().getAnnotatedSeq();
			for(GlyphI glyph : tg.getChildren()){
				if(glyph.getInfo() != null){
					glyph.setVisibility(filter.filterSymmetry(annotseq, (SeqSymmetry)glyph.getInfo()));
				} else {
					// Should not ever happen
					Logger.getLogger(FilterAction.class.getName()).log(Level.WARNING, "Found a glyph with null info at location {0}", glyph.getCoordBox());
				}
			}
		} else {
			for(GlyphI glyph : tg.getChildren()){
				glyph.setVisibility(true);
			}
		}
	}
}
