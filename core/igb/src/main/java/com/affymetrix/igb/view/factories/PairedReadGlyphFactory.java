package com.affymetrix.igb.view.factories;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.BAMSym;
import com.affymetrix.genometryImpl.symmetry.impl.PairedBamSymWrapper;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph;
import static com.google.common.base.Preconditions.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author dcnorris
 */
//will not extend annotationGlyphFactory in the future is this implementation approach makes sense
public class PairedReadGlyphFactory extends AnnotationGlyphFactory {

    private TierGlyph pairedTier;
    private static final String READ_NAME_SYM_PROPERTY = "Read name";
    private List<BAMSym> bamSyms;

    @Override
    public void createGlyphs(RootSeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI gviewer, BioSeq seq) {
        checkNotNull(sym);
        checkNotNull(style);
        checkNotNull(seq);
        seqMap = checkNotNull(gviewer);

        int glyphDepth = style.getGlyphDepth();
        TierGlyph.Direction tierDirection = !style.getSeparable() ? TierGlyph.Direction.BOTH : TierGlyph.Direction.FORWARD;
        pairedTier = seqMap.getTrack(style, tierDirection);
        pairedTier.setTierType(TierGlyph.TierType.ANNOTATION);
        pairedTier.setInfo(sym);
        setTrack(new Track(pairedTier));
        bamSyms = new ArrayList<BAMSym>();
        collectBamSyms(sym, glyphDepth);
        List<SeqSymmetry> updatedList = repackageSyms();
        sym.removeChildren();
        for (SeqSymmetry child : updatedList) {
            sym.addChild(child);
        }
        addLeafsToTier(sym, glyphDepth);
        doMiddlegroundShading(pairedTier, gviewer, seq);
    }

    private List<SeqSymmetry> repackageSyms() {
        Map<String, BAMSym> pairMap = new HashMap<String, BAMSym>();
        List<SeqSymmetry> toReturn = new ArrayList<SeqSymmetry>();
        for (BAMSym bamSym : bamSyms) {
            String readName = bamSym.getName();
            if (pairMap.containsKey(readName)) {
                PairedBamSymWrapper bamSymWrapper;
                if (bamSym.isForward()) {
                    bamSymWrapper = new PairedBamSymWrapper(bamSym, pairMap.remove(readName));
                } else {
                    bamSymWrapper = new PairedBamSymWrapper(pairMap.remove(readName), bamSym);
                }
                toReturn.add(bamSymWrapper);
            } else {
                pairMap.put(readName, bamSym);
            }
        }
        //add anything remaining to list
        for (Map.Entry<String, BAMSym> entry : pairMap.entrySet()) {
            toReturn.add(entry.getValue());
        }

        return toReturn;
    }

    protected void collectBamSyms(SeqSymmetry sym, int desired_leaf_depth) {
        sym = seqMap.transformForViewSeq(sym, seqMap.getViewSeq());
        int depth = SeqUtils.getDepthFor(sym);
        if (depth > desired_leaf_depth || sym instanceof TypeContainerAnnot) {
            int childCount = sym.getChildCount();
            for (int i = 0; i < childCount; i++) {
                collectBamSyms(sym.getChild(i), desired_leaf_depth);
            }
        } else {  // depth == desired_leaf_depth
            if (sym instanceof BAMSym) {
                bamSyms.add((BAMSym) sym);
            }
        }
    }

    @Override
    public String getName() {
        return "PairedRead";
    }
}
