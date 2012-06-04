
package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.action.SeqMapViewActionA;

import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 */
public class TransformAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private final Operator operator;
	
	public TransformAction(Operator operator) {
		super(operator.getDisplay(), null, null);
		this.operator = operator;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		List<TierGlyph> trackList;
		if (e != null && e.getSource() instanceof TrackListProvider) {
			trackList = ((TrackListProvider)e.getSource()).getTrackList();
		}
		else {
			trackList = getTierManager().getSelectedTiers();
		}
		for(TierGlyph tg : trackList){
			final ITrackStyleExtended style = tg.getAnnotStyle();
			Rectangle2D.Double savedCoordBox = tg.getCoordBox();
			try {
				
				style.setOperator(operator.getName());
				refreshMap(false, false);
				
				if(savedCoordBox != null){
					tg.setCoordBox(savedCoordBox);
				}
				
				getSeqMapView().getSeqMap().packTiers(false, true, false);
				getSeqMapView().getSeqMap().packTiers(false, true, false);
			}
			catch (Exception ex) {
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Transform error " + ex.getMessage());
			}
		}
	}
}
