
package com.affymetrix.igb.viewmode;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.graph.ScoredIntervalParser;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.*;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.SeqUtils;

import com.affymetrix.genoviz.widget.NeoMap;

import com.affymetrix.igb.shared.*;



/**
 *
 * @author hiralv
 */
public abstract class AbstractScoredContainerGlyphFactory extends MapViewGlyphFactoryA {
	private static final String[] supportedFormat = {"sin", "egr", "egr.txt", "map", "chp"};

//	private static final boolean DEBUG = false;
	private static final boolean separate_by_strand = true;
	
	private static final MapViewGlyphFactoryI annotFactory = new AnnotationGlyphFactory(FileTypeCategory.ScoredContainer);
	
	/** Does nothing. */
	public void init(Map<String, Object> options) {
	}

	private List<ViewModeGlyph> displayGraphs(ScoredContainerSym original_container, SeqMapViewExtendedI smv) {
		BioSeq aseq = smv.getAnnotatedSeq();


		if (original_container.getSpan(aseq) == null) {
			return null;
		}
		GraphIntervalSym[] the_graph_syms = determineGraphSyms(smv, aseq, original_container);
		List<ViewModeGlyph> vmgs = new ArrayList<ViewModeGlyph>();

		for (GraphIntervalSym gis : the_graph_syms) {
			vmgs.add(displayGraphSym(gis, smv));
		}

		return vmgs;
	}

	private static GraphIntervalSym[] determineGraphSyms(SeqMapViewExtendedI smv, BioSeq aseq, ScoredContainerSym original_container) {
		BioSeq vseq = smv.getViewSeq();
		AnnotatedSeqGroup seq_group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
		if (aseq != vseq) {
			DerivedSeqSymmetry derived_sym = SeqUtils.copyToDerived(original_container);
			SeqUtils.transformSymmetry(derived_sym, smv.getTransformPath());
			return makeGraphsFromDerived(derived_sym, seq_group, vseq);
		}
		// aseq == vseq, so no transformation needed
		return makeGraphs(original_container, seq_group);
	}

	private static GraphIntervalSym[] makeGraphs(ScoredContainerSym container, AnnotatedSeqGroup seq_group) {
		int score_count = container.getScoreCount();
		List<GraphIntervalSym> results = null;
		if (separate_by_strand) {
			results = new ArrayList<GraphIntervalSym>(score_count * 2);
		} else {
			results = new ArrayList<GraphIntervalSym>(score_count);
		}

		for (int i = 0; i < score_count; i++) {
			String score_name = container.getScoreName(i);
			if (separate_by_strand) {
				GraphIntervalSym forward_gsym = container.makeGraphSym(score_name, true, seq_group);
				if (forward_gsym != null) {
					results.add(forward_gsym);
				}
				GraphIntervalSym reverse_gsym = container.makeGraphSym(score_name, false, seq_group);
				if (reverse_gsym != null) {
					results.add(reverse_gsym);
				}
			} else {
				GraphIntervalSym gsym = container.makeGraphSym(score_name, seq_group);
				if (gsym != null) {
					results.add(gsym);
				}
			}
		}
		return results.toArray(new GraphIntervalSym[results.size()]);
	}

	private static GraphIntervalSym[] makeGraphsFromDerived(DerivedSeqSymmetry derived_parent_sym,
			AnnotatedSeqGroup seq_group, BioSeq seq) {
		ScoredContainerSym original_container = (ScoredContainerSym) derived_parent_sym.getOriginalSymmetry();

		int score_count = original_container.getScoreCount();
		List<GraphIntervalSym> results = null;
		if (separate_by_strand) {
			results = new ArrayList<GraphIntervalSym>(score_count * 2);
		} else {
			results = new ArrayList<GraphIntervalSym>(score_count);
		}

		for (int i = 0; i < score_count; i++) {
			String score_name = original_container.getScoreName(i);
			if (separate_by_strand) {
				GraphIntervalSym forward_gsym = makeGraphSymFromDerived(derived_parent_sym, score_name, seq_group, seq, '+');
				if (forward_gsym != null) {
					results.add(forward_gsym);
				}
				GraphIntervalSym reverse_gsym = makeGraphSymFromDerived(derived_parent_sym, score_name, seq_group, seq, '-');
				if (reverse_gsym != null) {
					results.add(reverse_gsym);
				}
			} else {
				GraphIntervalSym gsym = makeGraphSymFromDerived(derived_parent_sym, score_name, seq_group, seq, '.');
				if (gsym != null) {
					results.add(gsym);
				}
			}
		}

		return results.toArray(new GraphIntervalSym[results.size()]);
	}

	// strands should be one of '+', '-' or '.'
	// name -- should be a score name in the original ScoredContainerSym
	private static GraphIntervalSym makeGraphSymFromDerived(DerivedSeqSymmetry derived_parent, String name,
			AnnotatedSeqGroup seq_group, BioSeq seq, final char strands) {
		ScoredContainerSym original_container = (ScoredContainerSym) derived_parent.getOriginalSymmetry();

		float[] original_scores = original_container.getScores(name);

		// Simply knowing the correct graph ID is the key to getting the correct
		// graph state, with the accompanying tier style and tier combo style.
		String id = original_container.getGraphID(seq_group, name, strands);

		if (original_scores == null) {
			System.err.println("ScoreContainerSym.makeGraphSym() called, but no scores found for: " + name);
			return null;
		}

		int derived_child_count = derived_parent.getChildCount();
		IntArrayList xcoords = new IntArrayList(derived_child_count);
		IntArrayList wcoords = new IntArrayList(derived_child_count);
		FloatArrayList ycoords = new FloatArrayList(derived_child_count);

		for (int i = 0; i < derived_child_count; i++) {
			Object child = derived_parent.getChild(i);
			if (child instanceof DerivedSeqSymmetry) {
				DerivedSeqSymmetry derived_child = (DerivedSeqSymmetry) derived_parent.getChild(i);
				SeqSpan cspan = derived_child.getSpan(seq);
				if (cspan != null) {
					if (strands == '.' || (strands == '+' && cspan.isForward())
							|| (strands == '-' && !cspan.isForward())) {
						xcoords.add(cspan.getMin());
						wcoords.add(cspan.getLength());
						IndexedSym original_child = (IndexedSym) derived_child.getOriginalSymmetry();
						// the index of this child in the original parent symmetry.
						// it is very possible that original_index==i in all cases,
						// but I'm not sure of that yet
						int original_index = original_child.getIndex();
						ycoords.add(original_scores[original_index]);
					}
				}
			}
		}
		xcoords.trimToSize();
		wcoords.trimToSize();
		ycoords.trimToSize();
		GraphIntervalSym gsym = null;
		if (!xcoords.isEmpty()) {
			gsym = new GraphIntervalSym(xcoords.elements(),
					wcoords.elements(), ycoords.elements(), id, seq);
			if (strands == '-') {
				gsym.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_MINUS);
			} else if (strands == '+') {
				gsym.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_PLUS);
			} else {
				gsym.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_BOTH);
			}
		}
		return gsym;
	}

	private AbstractGraphGlyph displayGraphSym(GraphIntervalSym graf, SeqMapViewExtendedI smv) {
		AbstractGraphGlyph graph_glyph = createViewModeGlyph(graf, graf.getGraphState(), smv);
		GraphState gstate = graph_glyph.getGraphState();
		ITrackStyleExtended tier_style = gstate.getTierStyle();
		tier_style.setTrackName(graf.getGraphName());

		NeoMap map = smv.getSeqMap();
		Rectangle2D.Double cbox = map.getCoordBounds();
		graph_glyph.setCoords(cbox.x, tier_style.getY(), cbox.width, tier_style.getHeight());
		smv.setDataModelFromOriginalSym(graph_glyph, graf); // has side-effect of graph_glyph.setInfo(graf)
		// Allow floating glyphs ONLY when combo style is null.
		// (Combo graphs cannot yet float.)
//		if (gstate.getComboStyle() == null && gstate.getTierStyle().getFloatGraph()) {
//			graph_glyph.setCoords(cbox.x, tier_style.getY(), cbox.width, tier_style.getHeight());
//			GraphGlyphUtils.checkPixelBounds(graph_glyph, map);
//			smv.addToPixelFloaterGlyph(graph_glyph);
//		} else {
//			if (gstate.getComboStyle() != null) {
//				tier_style = gstate.getComboStyle();
//			}
//			TierGlyph.Direction tier_direction = TierGlyph.Direction.FORWARD;
//			if (GraphSym.GRAPH_STRAND_MINUS.equals(graf.getProperty(GraphSym.PROP_GRAPH_STRAND))) {
//				tier_direction = TierGlyph.Direction.REVERSE;
//			}
//			TierGlyph tglyph = smv.getGraphTrack(tier_style, tier_direction);
//			tglyph.addChild(graph_glyph);
			if(graph_glyph.getScene() != null){
				graph_glyph.pack(map.getView());
			}
//		}
		return graph_glyph;
	}

	@Override
	public String getName() {
		return "scored";
	}

	@Override
	public boolean isCategorySupported(FileTypeCategory category) {
		return category == FileTypeCategory.ScoredContainer;
	}

	public boolean isFileSupported(String fileFormat) {
		if(fileFormat == null)
			return false;

		for(String format : supportedFormat){
			if(format.equals(fileFormat)){
				return true;
			}
		}
		return false;
	}

	protected abstract AbstractGraphGlyph createViewModeGlyph(GraphIntervalSym graf, GraphState graphState, SeqMapViewExtendedI smv);

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style, TierGlyph.Direction tier_direction, SeqMapViewExtendedI smv) {
		if (sym == null) {
			return new ScoredContainerViewModeGlyph(style);
		}
		else if (sym instanceof ScoredContainerSym) {
			ViewModeGlyph annot = annotFactory.getViewModeGlyph(sym, style, tier_direction, smv);
			ScoredContainerViewModeGlyph scored = new ScoredContainerViewModeGlyph(style);
			scored.setInfo(sym);
			
			if(annot == null){
				return scored;
			}

			scored.addChild(annot);
			
			boolean attach_graphs = PreferenceUtils.getBooleanParam(ScoredIntervalParser.PREF_ATTACH_GRAPHS,
				ScoredIntervalParser.default_attach_graphs);
			
			if(!attach_graphs){
				return scored;
			}
			
			List<ViewModeGlyph> vmgs = displayGraphs((ScoredContainerSym) sym, smv);
			if(vmgs == null){
				return scored;
			}
			
			for(ViewModeGlyph vmg : vmgs){
				scored.addChild(vmg);
			}

			return scored;
		} else {
			System.err.println("GenericGraphGlyphFactory.createGlyph() called, but symmetry "
					+ "passed in is NOT a GraphSym: " + sym);
		}
		return null;
	}
}
