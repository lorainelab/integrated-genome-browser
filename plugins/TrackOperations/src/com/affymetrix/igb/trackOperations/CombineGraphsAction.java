package com.affymetrix.igb.trackOperations;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.style.SimpleTrackStyle;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.osgi.service.IGBService;

/**
 *  Puts all selected graphs in the same tier.
 *  Current glyph factories do not support floating the combined graphs.
 */
public class CombineGraphsAction extends GenericAction {

	private static final long serialVersionUID = 1l;
	private static final CombineGraphsAction ACTION = new CombineGraphsAction();
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static CombineGraphsAction getAction() {
		return ACTION;
	}

	private CombineGraphsAction() {
		super(TrackOperationsTab.BUNDLE.getString("combineButton"), null, null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		List<RootSeqSymmetry> rootSyms = TrackOperationsTab.getSingleton().getRootSyms();
		super.actionPerformed(e);
		int gcount = rootSyms.size();
		float height = 0;

		// Note that the combo_style does not implement IFloatableTierStyle
		// because the glyph factory doesn't support floating combo graphs anyway.
		ITrackStyleExtended combo_style = null;
		String viewMode = "combo";

		Map<Color, Integer> colorMap = new HashMap<Color, Integer>();
		// If any of them already has a combo style, use that one
		for (int i = 0; i < gcount && combo_style == null; i++) {
			if (rootSyms.get(i) instanceof GraphSym) {
				GraphSym gsym = (GraphSym)rootSyms.get(i);
				combo_style = gsym.getGraphState().getComboStyle();
				Color col = gsym.getGraphState().getTierStyle().getBackground();
				int c = 0;
				if (colorMap.containsKey(col)) {
					c = colorMap.get(col) + 1;
				}
				colorMap.put(col, c);
			}
		}

		// otherwise, construct a new combo style
		if (combo_style == null) {
			combo_style = new SimpleTrackStyle("Joined Graphs", true);
			combo_style.setTrackName("Joined Graphs");
			combo_style.setExpandable(true);
			//	combo_style.setCollapsed(true);
			IGBService igbService = TrackOperationsTab.getSingleton().getIgbService();
			combo_style.setLabelForeground(igbService.getDefaultForegroundColor());
			combo_style.setForeground(igbService.getDefaultForegroundColor());
			Color background = igbService.getDefaultBackgroundColor();
			int c = -1;
			for (Entry<Color, Integer> color : colorMap.entrySet()) {
				if (color.getValue() > c) {
					background = color.getKey();
				}
			}
			combo_style.setLabelBackground(background);
			combo_style.setBackground(background);
			combo_style.setTrackNameSize(igbService.getDefaultTrackSize());
			combo_style.setViewMode(viewMode);
		}

		// Now apply that combo style to all the selected graphs
		int i = 0;
		for (SeqSymmetry sym : rootSyms) {
			if (sym instanceof GraphSym) {
				GraphSym gsym = (GraphSym)sym;
				GraphState gstate = gsym.getGraphState();
				gstate.setComboStyle(combo_style, i++);
				gstate.getTierStyle().setFloatTier(false); // ignored since combo_style is set
				height += gsym.getGraphState().getTierStyle().getHeight();
			}
		}
		combo_style.setHeight(height);

		TrackOperationsTab.getSingleton().updateViewer();
	}
}
