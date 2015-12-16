package com.affymetrix.igb.view;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.parsers.CytobandParser;
import com.affymetrix.genometry.style.DefaultStateProvider;
import com.affymetrix.genometry.style.GraphState;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometry.util.BioSeqUtils;
import com.affymetrix.genometry.util.GraphSymUtils;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.glyph.CytobandGlyph;
import com.affymetrix.igb.glyph.DefaultTierGlyph;
import com.affymetrix.igb.services.registry.MapTierTypeHolder;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.factories.AbstractTierGlyph;
import com.affymetrix.igb.view.factories.MapTierGlyphFactoryI;
import com.affymetrix.igb.view.factories.ProbeSetGlyphFactory;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.google.common.base.Strings;
import com.lorainelab.igb.genoviz.extensions.glyph.GraphGlyph;
import com.lorainelab.igb.genoviz.extensions.glyph.StyledGlyph;
import com.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jnicol
 */
public class TrackView {

    private static final TrackView instance = new TrackView();

    private TrackView() {
        super();
    }

    public static TrackView getInstance() {
        return instance;
    }

    /**
     * Hash of ITrackStyle to forward TierGlyph
     */
    private static final Map<ITrackStyleExtended, TierGlyph> style2forwardTierGlyph = new HashMap<>();
    /**
     * Hash of ITrackStyle to reverse TierGlyph
     */
    private static final Map<ITrackStyleExtended, TierGlyph> style2reverseTierGlyph = new HashMap<>();
    /**
     * Hash of ITrackStyle to TierGlyph.
     */
    private static final Map<ITrackStyleExtended, TierGlyph> gstyle2track = new HashMap<>();

    void clear() {
        style2forwardTierGlyph.clear();
        style2reverseTierGlyph.clear();
        gstyle2track.clear();
    }

    public TierGlyph getTier(ITrackStyleExtended style, StyledGlyph.Direction tier_direction) {
        if (style == null || tier_direction == null) {
            return null;
        }
        Map<ITrackStyleExtended, TierGlyph> style2track;
        if (style.isGraphTier()) {
            style2track = gstyle2track;
        } else if (tier_direction == StyledGlyph.Direction.REVERSE) {
            style2track = style2reverseTierGlyph;
        } else {
            style2track = style2forwardTierGlyph;
        }
        return style2track.get(style);
    }

    /**
     * get an new TierGlyphViewMode, unless there is already a TierGlyph for the
     * style/direction
     *
     * @param smv the SeqMapView
     * @param style the style
     * @param tier_direction the direction
     * @return the existing TierGlyph, or a new TierGlyphViewMode, for the
     * style/direction
     */
    synchronized TierGlyph getTrack(SeqMapView smv, ITrackStyleExtended style, StyledGlyph.Direction tier_direction) {
        AffyTieredMap seqmap = smv.getSeqMap();
        TierGlyph tierGlyph;
        tierGlyph = getTier(style, tier_direction);
        if (tierGlyph == null) {
            tierGlyph = new DefaultTierGlyph(style);
            tierGlyph.setDirection(tier_direction);

            // do not set packer here, will be set in ViewModeGlyph
            if (style.isGraphTier()) {
                gstyle2track.put(style, tierGlyph);
            } else if (tier_direction == StyledGlyph.Direction.REVERSE) {
                style2reverseTierGlyph.put(style, tierGlyph);
            } else {
                style2forwardTierGlyph.put(style, tierGlyph);
            }
            if (seqmap.getTierIndex(tierGlyph) == -1) {
                boolean above_axis = (tier_direction != StyledGlyph.Direction.REVERSE);
                seqmap.addTier(tierGlyph, above_axis);
            }
        } else if (seqmap.getTierIndex(tierGlyph) == -1) { //
            boolean above_axis = (tier_direction != StyledGlyph.Direction.REVERSE);
            seqmap.addTier(tierGlyph, above_axis);
        }

        if (!style.isGraphTier() && (tier_direction == StyledGlyph.Direction.FORWARD
                || tier_direction == StyledGlyph.Direction.BOTH || tier_direction == StyledGlyph.Direction.NONE)) {
            if (style.getSeparable()) {
                if (style.getSeparate()) {
                    tierGlyph.setDirection(StyledGlyph.Direction.FORWARD);
                } else {
                    tierGlyph.setDirection(StyledGlyph.Direction.BOTH);
                }
            } else {
                tierGlyph.setDirection(StyledGlyph.Direction.NONE);
            }
        }

//		smv.processTrack(tierGlyph);
        return tierGlyph;
    }

    void addTracks(SeqMapView smv, BioSeq seq) {
        // WARNING: use seq.getAnnotationCount() in loop, because some annotations end up lazily instantiating
        //   other annotations and adding them to the annotation list
        // For example, accessing methods for the first time on a LazyChpSym can cause it to dynamically add
        //      probeset annotation tracks
        for (int i = 0; i < seq.getAnnotationCount(); i++) {
            RootSeqSymmetry annotSym = seq.getAnnotation(i);
            // skip over any cytoband data.  It is shown in a different way
            if (annotSym instanceof TypeContainerAnnot) {
                TypeContainerAnnot tca = (TypeContainerAnnot) annotSym;
                if (CytobandGlyph.CYTOBAND_TIER_REGEX.matcher(tca.getType()).matches()) {
                    continue;
                }
            }
            addAnnotationGlyphs(smv, annotSym, seq);
        }
    }

    void addDependentAndEmptyTrack(SeqMapView smv, BioSeq seq) {
        for (DataSet dataSet : GeneralLoadUtils.getVisibleFeatures()) {
            addEmptyTierFor(dataSet, smv);
        }
    }

    private void addAnnotationGlyphs(SeqMapView smv, RootSeqSymmetry annotSym, BioSeq seq) {
        // Map symmetry subclass or method type to a factory, and call factory to make glyphs
        String meth = BioSeqUtils.determineMethod(annotSym);
        if (meth != null) {
            MapTierGlyphFactoryI factory = MapTierTypeHolder.getDefaultFactoryFor(annotSym.getCategory());
            ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
            factory.createGlyphs(annotSym, style, smv, seq);
        }
    }

    public void deleteSymsOnSeq(SeqMapView smv, String method, BioSeq seq, DataSet dataSet) {

        if (seq != null) {
            SeqSymmetry sym = seq.getAnnotation(method);
            if (sym != null) {
                if (sym instanceof GraphSym) {
                    GlyphI glyph = smv.getSeqMap().getItemFromTier(sym);
                    if (glyph != null) {
                        if (glyph instanceof GraphGlyph) {
                            smv.split(glyph);
                        }
                        //map.removeItem(glyph);
                    }
                } else if (CytobandGlyph.CYTOBAND_TIER_REGEX.matcher("").reset(method).matches()) {
                    GlyphI glyph = smv.getAxisTier().getItem(sym);
                    if (glyph != null) {
                        smv.getSeqMap().removeItem(glyph);
                    }
                }
                seq.unloadAnnotation(sym);

                if (dataSet != null) {
                    dataSet.clear(seq);
                    if (dataSet.getLoadStrategy() == LoadStrategy.GENOME || dataSet.getLoadStrategy() == LoadStrategy.AUTOLOAD) {
                        dataSet.setLoadStrategy(LoadStrategy.NO_LOAD);
                    }
                }
            }
        }
    }

    public void addEmptyTierFor(DataSet dataSet, SeqMapView gviewer) {

        // No sequence selected or if it is cytoband or it is residue file. Then return
        if (gviewer.getAnnotatedSeq() == null || dataSet.getDataSetName().equals(CytobandParser.CYTOBAND)
                || dataSet.getDataSetName().toLowerCase().contains(CytobandParser.CYTOBAND)
                || (dataSet.getSymL() != null && (dataSet.getSymL().isResidueLoader() || dataSet.getSymL().getExtension().equalsIgnoreCase("cyt")))) {
            return;
        }

        ITrackStyleExtended style;

        // If feature has at least one track then don't add default.
        // Also if track has been loaded on one sequence then load it
        // for other sequence.
        if (!Strings.isNullOrEmpty(dataSet.getMethod())) {
            String method = dataSet.getMethod();
            if (method.endsWith(ProbeSetGlyphFactory.NETAFFX_PROBESETS)
                    || method.equals(CytobandParser.CYTOBAND_TIER_NAME)) {
                return;
            }
            style = getStyle(method, dataSet);

            if (style == null) {
                return;
            }

            addTierFor(style, gviewer);

        } else {
            style = getStyle(dataSet.getURI().toString(), dataSet);
            style.setFeature(dataSet);
            addTierFor(style, gviewer);
        }

    }

    public ITrackStyleExtended getStyle(String method, DataSet dataSet) {
        if (GraphSymUtils.isAGraphExtension(dataSet.getExtension())) {
            GraphState state = DefaultStateProvider.getGlobalStateProvider().getGraphState(method, dataSet.getDataSetName(), dataSet.getExtension(), dataSet.getProperties());

            if (state.getTierStyle().isFloatTier()) {
                return null;
            }

            return state.getTierStyle();
        } else {
            return DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(method, dataSet.getDataSetName(), dataSet.getExtension(), dataSet.getProperties());
        }
    }

    public void addTierFor(ITrackStyleExtended style, SeqMapView gviewer) {
        if (!style.isGraphTier()) {
            StyledGlyph.Direction direction = style.getSeparate() ? StyledGlyph.Direction.FORWARD : StyledGlyph.Direction.BOTH;
            //rootSym = (category == FileTypeCategory.ScoredContainer) ? new ScoredContainerSym() : new TypeContainerAnnot(style.getMethodName());
            TierGlyph tgfor = gviewer.getTrack(style, direction);
            if (tgfor.getChildCount() == 0) {
                ((AbstractTierGlyph) tgfor).initUnloaded();
            }
            tgfor.pack(gviewer.getSeqMap().getView());
            if (style.getSeparate()) {
                TierGlyph tgrev = gviewer.getTrack(style, StyledGlyph.Direction.REVERSE);
                if (tgrev.getChildCount() == 0) {
                    ((AbstractTierGlyph) tgrev).initUnloaded();
                }
                tgrev.pack(gviewer.getSeqMap().getView());
            }
        } else {
            //rootSym = new GraphSym(new int[]{}, new float[]{}, style.getMethodName(), seq);
            TierGlyph tg = gviewer.getTrack(style, StyledGlyph.Direction.NONE);
            if (tg.getChildCount() == 0 && !style.isFloatTier() && !style.getJoin()) {
                ((AbstractTierGlyph) tg).initUnloaded();
            }
            tg.pack(gviewer.getSeqMap().getView());
        }
    }

    private void addDummyChild(TierGlyph tierGlyph) {
        if (tierGlyph.getChildCount() == 0
                && !tierGlyph.getAnnotStyle().isFloatTier()
                && !tierGlyph.getAnnotStyle().getJoin()) {
            GlyphI glyph = new FillRectGlyph();
            glyph.setCoords(0, 0, 0, tierGlyph.getChildHeight());
            tierGlyph.addChild(glyph);
        }
    }
}
