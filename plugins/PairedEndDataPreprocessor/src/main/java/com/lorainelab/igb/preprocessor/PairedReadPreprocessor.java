package com.lorainelab.igb.preprocessor;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.BAMSym;
import com.affymetrix.genometryImpl.symmetry.impl.PairedBamSymWrapper;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.igb.shared.GlyphPreprocessorI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import static com.google.common.base.Preconditions.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author dcnorris
 */
public class PairedReadPreprocessor implements GlyphPreprocessorI {

    List<BAMSym> bamSyms;
    SeqMapViewExtendedI gviewer;

    @Override
    public void process(RootSeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI gviewer, BioSeq seq) {
        checkNotNull(sym);
        checkNotNull(style);
        checkNotNull(seq);
        this.gviewer = checkNotNull(gviewer);
        if (style.isShowAsPaired()) {
            int glyphDepth = style.getGlyphDepth();
            bamSyms = new ArrayList<BAMSym>();
            collectBamSyms(sym, glyphDepth);
            List<SeqSymmetry> updatedList = repackageSyms();
            sym.removeChildren();
            for (SeqSymmetry child : updatedList) {
                sym.addChild(child);
            }
        }
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
        //add anything remaining
        for (Map.Entry<String, BAMSym> entry : pairMap.entrySet()) {
            toReturn.add(entry.getValue());
        }
        return toReturn;
    }

    private List<BAMSym> collectBamSyms(SeqSymmetry sym, int desired_leaf_depth) {
        sym = gviewer.transformForViewSeq(sym, gviewer.getViewSeq());
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
        return bamSyms;
    }

    @Override
    public String getName() {
        return "PairedReadPreprocessor";
    }
}
