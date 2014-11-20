package com.lorainelab.igb.preprocessor;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.BAMSym;
import com.affymetrix.genometryImpl.symmetry.impl.PairedBamSymWrapper;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.igb.shared.SeqSymmetryPreprocessorI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dcnorris
 */
public class PairedReadPreprocessor implements SeqSymmetryPreprocessorI {

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
            bamSyms.clear();
        }
    }

    private List<SeqSymmetry> repackageSyms() {
        Table<String, Integer, List<BAMSym>> pairMap = HashBasedTable.create();
        List<SeqSymmetry> toReturn = new ArrayList<SeqSymmetry>();
        for (BAMSym bamSym : bamSyms) {
            String readName = bamSym.getName();
            int mateStart = bamSym.getMateStart();
            if (pairMap.contains(readName, mateStart)) {
                PairedBamSymWrapper bamSymWrapper;
                if (bamSym.isForward()) {
                    List<BAMSym> matchingBAMSyms = pairMap.get(readName, mateStart);
                    if (matchingBAMSyms.size() == 1) {
                        matchingBAMSyms = pairMap.remove(readName, mateStart);
                        bamSymWrapper = new PairedBamSymWrapper(bamSym, matchingBAMSyms.get(0));
                    } else {
                        //remove single matching sym
                        bamSymWrapper = new PairedBamSymWrapper(bamSym, matchingBAMSyms.remove(0));
                    }
                } else {
                    List<BAMSym> matchingBAMSyms = pairMap.get(readName, mateStart);
                    if (matchingBAMSyms.size() == 1) {
                        matchingBAMSyms = pairMap.remove(readName, mateStart);
                        bamSymWrapper = new PairedBamSymWrapper(matchingBAMSyms.get(0), bamSym);
                    } else {
                        //remove single matching sym
                        bamSymWrapper = new PairedBamSymWrapper(matchingBAMSyms.remove(0), bamSym);
                    }
                }
                toReturn.add(bamSymWrapper);
            } else {
                if (bamSym.isForward() && pairMap.contains(readName, bamSym.getStart())) {
                    List<BAMSym> matchingBAMSyms = pairMap.get(readName, bamSym.getStart());
                    matchingBAMSyms.add(bamSym);
                } else if (pairMap.contains(readName, bamSym.getEnd())) {
                    List<BAMSym> matchingBAMSyms = pairMap.get(readName, bamSym.getEnd());
                    matchingBAMSyms.add(bamSym);
                } else {
                    List<BAMSym> bamSymsList = new ArrayList<BAMSym>();
                    bamSymsList.add(bamSym);
                    if (bamSym.isForward()) {
                        pairMap.put(readName, bamSym.getStart(), bamSymsList);
                    } else {
                        pairMap.put(readName, bamSym.getEnd(), bamSymsList);
                    }
                }
            }
        }
        //add anything remaining
        for (List<BAMSym> bamSymList : pairMap.values()) {
            for (BAMSym bamSym : bamSymList) {
                toReturn.add(bamSym);
            }
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
