
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TrackStyle;
import java.awt.event.ActionEvent;

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
		for(TierGlyph tg : getTierManager().getSelectedTiers()){
			final ITrackStyleExtended style = tg.getAnnotStyle();
			if (style instanceof TrackStyle) {
				style.setOperator(operator.getName());
				refreshMap(false, false);
			}
		}
	}
}
