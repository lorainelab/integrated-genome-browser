package com.affymetrix.igb.shared;

import com.lorainelab.igb.genoviz.extensions.api.StyledGlyph;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.general.IParameters;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symloader.Delegate;
import com.affymetrix.genometryImpl.symmetry.impl.GraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TrackStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TrackUtils {

    private static final TrackUtils instance = new TrackUtils();

    public static TrackUtils getInstance() {
        return instance;
    }

    private TrackUtils() {
        super();
    }

    public void addTrack(SeqSymmetry sym, String method, Operator operator, ITrackStyleExtended preferredStyle) {
        makeNonPersistentStyle((SymWithProps) sym, method, operator, preferredStyle);
        BioSeq aseq = GenometryModel.getInstance().getSelectedSeq();
        aseq.addAnnotation(sym);
        Application.getSingleton().getMapView().setAnnotatedSeq(aseq, true, true);
    }

    private ITrackStyleExtended makeNonPersistentStyle(SymWithProps sym, String human_name, Operator operator, ITrackStyleExtended preferredStyle) {
		// Needs a unique name so that if any later tier is produced with the same
        // human name, it will not automatically get the same color, etc.
        String unique_name = IGBStateProvider.getUniqueName(human_name);
        sym.setProperty("method", unique_name);
        if (sym.getProperty("id") == null || sym instanceof GraphSym) {
            sym.setProperty("id", unique_name);
        }
        ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(unique_name, human_name, Delegate.EXT, null);
        if (preferredStyle == null) {
            style.setGlyphDepth(1);
            style.setSeparate(false); // there are not separate (+) and (-) strands

			// This might have become obsolete
            // style.setCustomizable(false); // the user can change the color, but not much else is meaningful
        } else {
            style.copyPropertiesFrom(preferredStyle);
            style.setGlyphDepth(Math.max(1, SeqUtils.getDepth(sym) - 1));
        }
        style.setTrackName(human_name);
        style.setGraphTier(sym instanceof GraphSym);
        style.setExpandable(sym instanceof GraphSym);
        style.setLabelField(null);
        if (operator instanceof Operator.Style && style instanceof TrackStyle) {
            ((TrackStyle) style).setProperties(((Operator.Style) operator).getStyleProperties());
        }

        return style;
    }

    public List<SeqSymmetry> getSymsFromLabelGlyphs(List<TierLabelGlyph> labels) {
        List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
        for (TierLabelGlyph label : labels) {
            TierGlyph glyph = label.getReferenceTier();
            RootSeqSymmetry rootSym = (RootSeqSymmetry) glyph.getInfo();
            if (rootSym == null && glyph.getChildCount() > 0 && glyph.getChild(0) instanceof RootSeqSymmetry) {
                rootSym = (RootSeqSymmetry) glyph.getChild(0).getInfo();
            }
            syms.add(rootSym);
        }
        return syms;
    }

    public List<RootSeqSymmetry> getSymsTierGlyphs(List<StyledGlyph> tierGlyphs) {
        List<RootSeqSymmetry> syms = new ArrayList<RootSeqSymmetry>();
        for (StyledGlyph glyph : tierGlyphs) {
            if (glyph instanceof TierGlyph && ((TierGlyph) glyph).getTierType() == TierGlyph.TierType.GRAPH) {
                for (GlyphI g : glyph.getChildren()) {
                    if (g instanceof GraphGlyph) {
                        collectRootSym(g, syms);
                    }
                }
            } else {
                collectRootSym(glyph, syms);
            }
        }
        return syms;
    }

    private void collectRootSym(GlyphI glyph, List<RootSeqSymmetry> syms) {
        RootSeqSymmetry rootSym = (RootSeqSymmetry) glyph.getInfo();
        if (rootSym == null && glyph.getChildCount() > 0 && glyph.getChild(0) instanceof RootSeqSymmetry) {
            rootSym = (RootSeqSymmetry) glyph.getChild(0).getInfo();
        }
        if (rootSym != null) {
            syms.add(rootSym);
        }
    }

    private Map<FileTypeCategory, Integer> getTrackCounts(List<? extends SeqSymmetry> syms) {
        Map<FileTypeCategory, Integer> trackCounts = new EnumMap<FileTypeCategory, Integer>(FileTypeCategory.class);
        for (SeqSymmetry sym : syms) {
            if (sym != null) {
                FileTypeCategory category = ((RootSeqSymmetry) sym).getCategory();
                if (trackCounts.get(category) == null) {
                    trackCounts.put(category, 0);
                }
                trackCounts.put(category, trackCounts.get(category) + 1);
            }
        }
        return trackCounts;
    }

    public boolean checkCompatible(List<? extends SeqSymmetry> syms, Operator operator, boolean paramsOK) {

        if (!paramsOK && operator instanceof IParameters) {
            Map<String, Class<?>> params = ((IParameters) operator).getParametersType();
            if (null != params) {
                if (0 < params.size()) {
                    return false;
                }
            }
        }

        Map<FileTypeCategory, Integer> trackCounts = getTrackCounts(syms);
        for (FileTypeCategory category : FileTypeCategory.values()) {
            int count = (trackCounts.get(category) == null) ? 0 : trackCounts.get(category);
            if (count < operator.getOperandCountMin(category)
                    || count > operator.getOperandCountMax(category)) {
                return false;
            }
        }
        return true;
    }

}
