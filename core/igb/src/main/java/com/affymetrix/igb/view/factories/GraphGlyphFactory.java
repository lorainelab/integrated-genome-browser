package com.affymetrix.igb.view.factories;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.impl.GraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.igb.graphTypes.DotGraphType;
import com.affymetrix.igb.graphTypes.EmptyBarGraphType;
import com.affymetrix.igb.graphTypes.FillBarGraphType;
import com.affymetrix.igb.graphTypes.HeatMapGraphType;
import com.affymetrix.igb.graphTypes.LineGraphType;
import com.affymetrix.igb.graphTypes.MinMaxAvgGraphType;
import com.affymetrix.igb.graphTypes.StairStepGraphType;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.MapTierGlyphFactoryA;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraphGlyphFactory extends MapTierGlyphFactoryA {

    private static final Logger ourLogger
            = Logger.getLogger(GraphGlyphFactory.class.getPackage().getName());

    public final static Map<GraphType, Class<? extends GraphGlyph.GraphStyle>> type2Style;

    static {
        type2Style = new EnumMap<GraphType, Class<? extends GraphGlyph.GraphStyle>>(GraphType.class);
        type2Style.put(GraphType.EMPTY_BAR_GRAPH, EmptyBarGraphType.class);
        type2Style.put(GraphType.DOT_GRAPH, DotGraphType.class);
        type2Style.put(GraphType.FILL_BAR_GRAPH, FillBarGraphType.class);
        type2Style.put(GraphType.HEAT_MAP, HeatMapGraphType.class);
        type2Style.put(GraphType.LINE_GRAPH, LineGraphType.class);
        type2Style.put(GraphType.MINMAXAVG, MinMaxAvgGraphType.class);
        type2Style.put(GraphType.STAIRSTEP_GRAPH, StairStepGraphType.class);
    }
    private boolean check_same_seq = true;
    /**
     * Name of a parameter for the init() method. Set to Boolean.TRUE or
     * Boolean.FALSE. Determines whether the glyph factory will try to determine
     * whether the GraphSym that it is drawing is defined on the
     * currently-displayed bioseq. In some cases, you may want to intentionally
     * display a graph on a seq that has a different ID without checking to see
     * if the IDs match.
     */
    private static final String CHECK_SAME_SEQ_OPTION = "Check Same Seq";

    /**
     * Name of a parameter for the init() method. Set to an instance of Double.
     * Controls a parameter of the GraphGlyph.
     *
     * @see GraphGlyph#setTransitionScale(double)
     */
    /**
     * Allows you to set the parameter CHECK_SAME_SEQ_OPTION.
     */
    @Override
    public void init(Map<String, Object> options) {
        Boolean ccs = (Boolean) options.get(CHECK_SAME_SEQ_OPTION);
        if (ccs != null) {
            check_same_seq = ccs.booleanValue();
        }
    }

    /**
     * Makes a GraphGlyph to represent the input GraphSym, and either adds it as
     * a floating graph to the SeqMapView or adds it in a tier, depending on
     * getGraphState().getGraphStyle().getFloatGraph() and
     * getGraphState().getComboStyle(). All graphs that share the same tier
     * style or the same combo tier style, will go in the same tier. Graphs with
     * a non-null combo tier style will go into an attached tier, never a
     * floating glyph. Also adds to the SeqMapView's GraphState-to-TierGlyph
     * hash if needed.
     */
    private GraphGlyph displayGraph(GraphSym graf, SeqMapViewExtendedI smv, boolean check_same_seq) {
        BioSeq aseq = smv.getAnnotatedSeq();
        BioSeq vseq = smv.getViewSeq();
        BioSeq graph_seq = graf.getGraphSeq();

        if (check_same_seq && graph_seq != aseq) {
            // may need to modify to handle case where GraphGlyph's seq is one of seqs in aseq's composition...
            return null;
        }

		// GAH 2006-03-26
        //    want to add code here to handle situation where a "virtual" seq is being display on SeqMapView,
        //       and it is composed of GraphSym's from multiple annotated seqs, but they're really from the
        //       same data source (or they're the "same" data on different chromosomes for example)
        //       In this case want these displayed as a single graph
		//   match these up based on identical graph names / ids, then:
        //    Approach 1)
        //       build a CompositeGraphSym on the virtual seq
        //       make a single GraphGlyph
        //    Approach 2)
        //       create a new CompositeGraphGlyph subclass (or do I already have this?)
        //       make multiple GraphGlyphs
        //    Approach 3)
        //       ???
        GraphSym newgraf = graf;
        if (check_same_seq && graph_seq != vseq) {
			// The new graph doesn't need a new GraphState or a new ID.
            // Changing any graph properties will thus apply to the original graph.
            SeqSymmetry mapping_sym = smv.transformForViewSeq(graf, graph_seq);
            newgraf = GraphSymUtils.transformGraphSym(graf, mapping_sym);

            // Just making sure that it won't result in npe
            if (smv.getTransformPath().length > 0 && smv.getTransformPath()[0].getSpan(vseq) != null) {
                SeqSpan span = newgraf.getSpan(vseq);
                newgraf.removeSpan(span);
                span = smv.getTransformPath()[0].getSpan(vseq);
                newgraf.addSpan(span);
            }
        }
        if (newgraf == null || newgraf.getPointCount() == 0) {
            return null;
        }

        String graph_name = newgraf.getGraphName();
        if (graph_name == null) {
            // this probably never actually happens
            graph_name = "Graph #" + System.currentTimeMillis();
            newgraf.setGraphName(graph_name);
        }

        return displayGraphSym(newgraf, graf, smv);
    }

    protected void setGraphType(GraphSym newgraf, GraphState gstate, GraphGlyph graphGlyph) {
        try {
            GraphGlyph.GraphStyle style = type2Style.get(gstate.getGraphStyle()).getConstructor(new Class[]{GraphGlyph.class}).newInstance(graphGlyph);
            graphGlyph.setGraphStyle(style);
        } catch (Exception ex) {
            Logger.getLogger(GraphGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
            graphGlyph.setGraphStyle(new MinMaxAvgGraphType(graphGlyph));
        }
    }

    /**
     * Almost exactly the same as ScoredContainerGlyphFactory.displayGraphSym.
     */
    private GraphGlyph displayGraphSym(GraphSym newgraf, GraphSym graf, SeqMapViewExtendedI smv) {
        GraphState gstate = graf.getGraphState();
        GraphGlyph graphGlyph = new GraphGlyph(newgraf, gstate);
        setGraphType(newgraf, gstate, graphGlyph);
        ITrackStyleExtended tier_style = gstate.getTierStyle();
        tier_style.setTrackName(newgraf.getGraphName());
//		tier_style.setCollapsed(isGenome);
        if (gstate.getComboStyle() != null) {
            tier_style = gstate.getComboStyle();
        }

//		graphGlyph.setCoords(0, tier_style.getY(), newgraf.getGraphSeq().getLength(), gstate.getTierStyle().getHeight());
        SeqSpan pspan = smv.getViewSeqSpan(newgraf);
        if (pspan == null || pspan.getLength() == 0) {
            return null;
        }
        graphGlyph.setCoords(pspan.getMin(), tier_style.getY(), pspan.getLength(), gstate.getTierStyle().getHeight());
//		smv.setDataModelFromOriginalSym(graphGlyph, graf); // has side-effect of graph_glyph.setInfo(graf)
        // Allow floating glyphs ONLY when combo style is null.
        // (Combo graphs cannot yet float.)
        //if (/*gstate.getComboStyle() == null && */ gstate.getTierStyle().getFloatGraph()) {
        //	smv.getFloaterGlyph().checkBounds(graphGlyph, smv.getSeqMap().getView());
        //	smv.addToPixelFloaterGlyph(graph_glyph);
        //} else {
			/*
         TierGlyph.Direction direction = TierGlyph.Direction.NONE;
         if (GraphSym.GRAPH_STRAND_MINUS.equals(graf.getProperty(GraphSym.PROP_GRAPH_STRAND))) {
         direction = TierGlyph.Direction.REVERSE;
         }else if(GraphSym.GRAPH_STRAND_PLUS.equals(graf.getProperty(GraphSym.PROP_GRAPH_STRAND))) {
         direction = TierGlyph.Direction.FORWARD;
         }
         TierGlyph tglyph = smv.getGraphTrack(tier_style, direction);
         if(gstate.getComboStyle() != null && !(tglyph.getPacker() instanceof GraphFasterExpandPacker)){
         tglyph.setExpandedPacker(new GraphFasterExpandPacker());
         }
         if (isGenome && !(tglyph.getPacker() instanceof CollapsePacker)) {
         CollapsePacker cp = new CollapsePacker();
         cp.setParentSpacer(0); // fill tier to the top and bottom edges
         cp.setAlignment(CollapsePacker.ALIGN_CENTER);
         tglyph.setPacker(cp);
         }
         tglyph.addChild(graph_glyph);
         tglyph.pack(map.getView(), false);
         */
        if (graphGlyph.getScene() != null) {
            graphGlyph.pack(smv.getSeqMap().getView());
        }
        //}
        graphGlyph.setInfo(newgraf);
        TierGlyph.Direction direction = TierGlyph.Direction.NONE;
        if (GraphSym.GRAPH_STRAND_MINUS.equals(graf.getProperty(GraphSym.PROP_GRAPH_STRAND))) {
            direction = TierGlyph.Direction.REVERSE;
        } else if (GraphSym.GRAPH_STRAND_PLUS.equals(graf.getProperty(GraphSym.PROP_GRAPH_STRAND))) {
            direction = TierGlyph.Direction.FORWARD;
        }
        graphGlyph.setDirection(direction);

        return graphGlyph;
    }

    @Override
    public void createGlyphs(RootSeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI smv, BioSeq seq) {
        if (sym instanceof GraphSym) {
            GraphGlyph graphGlyph = displayGraph((GraphSym) sym, smv, check_same_seq);
            if (graphGlyph != null) {
                if (style.getFloatTier()) {
                    graphGlyph.setCoords(0, style.getY(), smv.getViewSeq().getLength(), graphGlyph.getCoordBox().getHeight());
                    smv.getFloaterGlyph().addChild(graphGlyph);
//					smv.getFloaterGlyph().checkBounds(graphGlyph, smv.getSeqMap().getView());
//					smv.addToPixelFloaterGlyph(graphGlyph);
                } else {
                    GraphSym graf = (GraphSym) sym;
                    if (graf.getGraphState().getComboStyle() != null/* && !(result.getPacker() instanceof GraphFasterExpandPacker)*/) {
                        //result.setExpandedPacker(new GraphFasterExpandPacker());
                        style = graf.getGraphState().getComboStyle();
                    }
                    addGraphGlyphToTier(graphGlyph, style, smv, seq);
                }
            }
        } else {
            ourLogger.log(Level.SEVERE,
                    "GenericGraphGlyphFactory.getViewModeGlyph() called, but symmetry passed in is NOT a GraphSym: {0}", sym);
        }
    }

    public static TierGlyph addGraphGlyphToTier(GraphGlyph graphGlyph, ITrackStyleExtended style, SeqMapViewExtendedI smv, BioSeq seq) {
        TierGlyph result = smv.getTrack(style, graphGlyph.getDirection());
        result.setDataModelFromOriginalSym(graphGlyph, graphGlyph.getInfo());
        result.setCoords(0, style.getY(), smv.getViewSeq().getLength(), graphGlyph.getCoordBox().getHeight());
        result.addChild(graphGlyph);
        result.setTierType(TierGlyph.TierType.GRAPH);
        result.setInfo(graphGlyph.getInfo());
        doMiddlegroundShading(result, smv, seq);
        return result;
    }

    private static GraphState getGraphState(ITrackStyleExtended style) {
        String featureName = null, extension = null;
        Map<String, String> featureProps = null;
        if (style.getFeature() != null) {
            featureName = style.getFeature().featureName;
            extension = style.getFeature().getExtension();
            featureProps = style.getFeature().featureProps;
        }
        return DefaultStateProvider.getGlobalStateProvider().getGraphState(style.getMethodName(), featureName, extension, featureProps);
    }

    @Override
    public String getName() {
        return "Graph";
    }
}
