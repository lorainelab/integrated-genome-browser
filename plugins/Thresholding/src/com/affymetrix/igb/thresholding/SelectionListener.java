package com.affymetrix.igb.thresholding;

import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.MultiGraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.thresholding.action.ThresholdingAction;

public class SelectionListener implements SeqSelectionListener, SymSelectionListener {
	private final IGBService igbService;
	private final GenometryModel gmodel;
	private BioSeq current_seq;
	private final ThresholdingAction thresholdingAction;
	private final JRPMenuItem thresholdingMenuItem;
	boolean is_listening = true; // used to turn on and off listening to GUI events
	private final List<GraphSym> grafs = new ArrayList<GraphSym>();
	private final List<GraphGlyph> glyphs = new ArrayList<GraphGlyph>();

	public SelectionListener(IGBService igbService, ThresholdingAction thresholdingAction, JRPMenuItem thresholdingMenuItem) {
		super();
		this.igbService = igbService;
		this.thresholdingAction = thresholdingAction;
		this.thresholdingMenuItem = thresholdingMenuItem;
		gmodel = GenometryModel.getGenometryModel();
	}

	@Override
	public void seqSelectionChanged(SeqSelectionEvent evt) {
		current_seq = evt.getSelectedSeq();
		resetSelectedGraphGlyphs(gmodel.getSelectedSymmetries(current_seq));
	}

	@Override
	public void symSelectionChanged(SymSelectionEvent evt) {
		List<SeqSymmetry> selected_syms = evt.getSelectedGraphSyms();
		// Only pay attention to selections from the main SeqMapView or its map.
		// Ignore the splice view as well as events coming from this class itself.

		Object src = evt.getSource();
		if (!(src == igbService.getSeqMapView() || src == igbService.getSeqMap())) {
			return;
		}

		resetSelectedGraphGlyphs(selected_syms);
	}

	private void resetSelectedGraphGlyphs(List<?> selected_syms) {
		int symcount = selected_syms.size();
		is_listening = false; // turn off propagation of events from the GUI while we modify the settings
		collectGraphsAndGlyphs(selected_syms, symcount);

		boolean all_are_floating = false;
		boolean all_show_axis = false;
		boolean all_show_label = false;
		boolean any_are_combined = false; // are any selections inside a combined tier
		boolean all_are_combined = false; // are all selections inside (a) combined tier(s)

		// Take the first glyph in the list as a prototype
		GraphGlyph first_glyph = null;
		GraphType graph_style = GraphType.LINE_GRAPH;
		if (!glyphs.isEmpty()) {
			first_glyph = glyphs.get(0);
			graph_style = first_glyph.getGraphStyle();
			all_are_floating = first_glyph.getGraphState().getTierStyle().getFloatTier();
			all_show_axis = first_glyph.getGraphState().getShowAxis();
			all_show_label = first_glyph.getGraphState().getShowLabel();
			boolean this_one_is_combined = (first_glyph.getGraphState().getComboStyle() != null);
			any_are_combined = this_one_is_combined;
			all_are_combined = this_one_is_combined;
		}

		// Now loop through other glyphs if there are more than one
		// and see if the graph_style and heatmap are the same in all selections
		for (GraphGlyph gl : glyphs) {
			all_are_floating = all_are_floating && gl.getGraphState().getTierStyle().getFloatTier();
			all_show_axis = all_show_axis && gl.getGraphState().getShowAxis();
			all_show_label = all_show_label && gl.getGraphState().getShowLabel();
			boolean this_one_is_combined = (gl.getGraphState().getComboStyle() != null);
			any_are_combined = any_are_combined || this_one_is_combined;
			all_are_combined = all_are_combined && this_one_is_combined;

			if (graph_style == null) {
				graph_style = GraphType.LINE_GRAPH;
			}
			else if (first_glyph.getGraphStyle() != gl.getGraphStyle()) {
				graph_style = GraphType.LINE_GRAPH;
			}
		}
		thresholdingAction.setGraphs(glyphs);
		thresholdingMenuItem.setEnabled(!glyphs.isEmpty());

		is_listening = true; // turn back on GUI events
	}

	private void collectGraphsAndGlyphs(List<?> selected_syms, int symcount) {
		if (grafs != selected_syms) {
			// in certain cases selected_syms arg and grafs list may be same, for example when method is being
			//     called to catch changes in glyphs representing selected sym, not the syms themselves)
			//     therefore don't want to change grafs list if same as selected_syms (especially don't want to clear it!)
			grafs.clear();
		}
		glyphs.clear();
		// First loop through and collect graphs and glyphs
		for (int i = 0; i < symcount; i++) {
			if (selected_syms.get(i) instanceof GraphSym) {
				GraphSym graf = (GraphSym) selected_syms.get(i);
				// only add to grafs if list is not identical to selected_syms arg
				if (grafs != selected_syms) {
					grafs.add(graf);
				}
				// add all graph glyphs representing graph sym
				//	  System.out.println("found multiple glyphs for graph sym: " + multigl.size());
				for (Glyph g : igbService.getVisibleTierGlyphs()) {
					ViewModeGlyph vg = ((TierGlyph)g).getViewModeGlyph();
					if (vg instanceof MultiGraphGlyph && vg.getChildren() != null) {
						for (GlyphI child : vg.getChildren()) {
							if (grafs.contains(child.getInfo())) {
								glyphs.add((GraphGlyph) child);
							}
						}
					}
					else if (grafs.contains(vg.getInfo())) {
						glyphs.add(((AbstractGraphGlyph) vg).getGraphGlyph());
					}
				}
			}
		}
	}
}
