package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.shared.RepackTiersAction;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.Action;

/**
 *
 * @author hiralv
 */
public class FilterAction extends RepackTiersAction {
	private static final long serialVersionUID = 1L;
	private static final FilterAction ACTION = new FilterAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static FilterAction getAction() {
		return ACTION;
	}

	private FilterAction() {
		super("Filter",
				"16x16/actions/hide.png",
				"22x22/actions/hide.png");
		this.ordinal = -6008400;
		this.putValue(Action.SELECTED_KEY, false);
	}

	/**
	 * Hides multiple tiers and then repacks.
	 *
	 * @param tiers a List of GlyphI objects for each of which getInfo() returns
	 * a TierGlyph.
	 */
	void filter(List<TierLabelGlyph> tiers) {
		for (TierLabelGlyph g : tiers) {
			if (g.getInfo() instanceof TierGlyph) {
				TierGlyph tier = (TierGlyph) g.getInfo();
				tier.filter((Boolean)this.getValue(Action.SELECTED_KEY));
			}
		}
		repack(true, false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		this.putValue(Action.SELECTED_KEY, !((Boolean)this.getValue(Action.SELECTED_KEY)));
		filter(getTierManager().getSelectedTierLabels());
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}

	@Override
	public boolean isEnabled(){
		return Selections.allGlyphs.size() > 0;
	}
}

