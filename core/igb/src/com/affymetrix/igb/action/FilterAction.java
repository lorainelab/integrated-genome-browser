package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.filter.SymmetryFilterI;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.util.ConfigureOptionsDialog;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 */
public class FilterAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final FilterAction ACTION = new FilterAction();
		
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static FilterAction getAction() {
		return ACTION;
	}
	
	public FilterAction() {
		super("Filter...", "16x16/actions/hide.png", "22x22/actions/hide.png");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		ITrackStyleExtended style = getTierManager().getSelectedTiers().get(0).getAnnotStyle();
		SymmetryFilterI filter = style.getFilter();
		
		ConfigureOptionsDialog<SymmetryFilterI> filterDialog = new ConfigureOptionsDialog<SymmetryFilterI>(SymmetryFilterI.class, "Filter");
		filterDialog.setTitle("Filter");
		filterDialog.setLocationRelativeTo(getSeqMapView());
		filterDialog.setInitialValue(filter);
		filter = filterDialog.showDialog();
		for (TierGlyph tier : getTierManager().getSelectedTiers()) {
			applyFilter(filter, tier);
		}
		getSeqMapView().getSeqMap().repackTheTiers(true, true);
	}

	@Override
	public boolean isEnabled(){
		return Selections.allGlyphs.size() > 0;
	}
		
	private void applyFilter(SymmetryFilterI filter, TierGlyph tg) {
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
