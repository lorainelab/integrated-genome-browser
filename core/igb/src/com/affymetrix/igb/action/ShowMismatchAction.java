package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;

import com.affymetrix.igb.shared.StyledGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import static com.affymetrix.igb.shared.Selections.*;

/**
 *
 * @author hiralv
 */
public class ShowMismatchAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private final static ShowMismatchAction showMismatchAction = new ShowMismatchAction();
	
	public static ShowMismatchAction getAction(){
		return showMismatchAction;
	}
	
	private ShowMismatchAction() {
		super("Show Mismatch Only", null, null);
	}
	
	@Override
	public void actionPerformed(java.awt.event.ActionEvent e) {
		super.actionPerformed(e);
		for(StyledGlyph glyph : allGlyphs){
			if(((RootSeqSymmetry)glyph.getInfo()).getCategory() == FileTypeCategory.Alignment){
				glyph.getAnnotStyle().setShowResidueMask(isSelected());
			}
		}
		refreshMap(false, false);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
}
