package com.lorainelab.igb.preprocessor;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.BAMSym;
import com.affymetrix.genometry.symmetry.impl.MultiTierSymWrapper;
import com.affymetrix.genometry.symmetry.impl.PairedBamSymWrapper;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometry.util.SeqUtils;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.List;

import com.lorainelab.igb.genoviz.extensions.api.SeqMapViewExtendedI;
import com.affymetrix.igb.service.api.SeqSymmetryPreprocessorI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = PairedReadPreprocessor.COMPONENT_NAME, provide = {SeqSymmetryPreprocessorI.class})
public class PairedReadPreprocessor implements SeqSymmetryPreprocessorI {

    public static final String COMPONENT_NAME = "PairedReadPreprocessor";
    private static final Logger logger = LoggerFactory.getLogger(PairedReadPreprocessor.class);
    private List<BAMSym> bamSyms;
    private SeqMapViewExtendedI gviewer;

    @Override
    public void process(RootSeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI gviewer, BioSeq seq) {
        checkNotNull(sym);
        checkNotNull(style);
        checkNotNull(seq);
        this.gviewer = checkNotNull(gviewer);
        if (style.isShowAsPaired()) {
            int glyphDepth = style.getGlyphDepth();
            bamSyms = new ArrayList<>();
            collectBamSyms(sym, glyphDepth);
            List<SeqSymmetry> updatedList = repackageSyms();
            sym.removeChildren();
            updatedList.forEach(sym::addChild);
            bamSyms.clear();
        }
    }

    private List<SeqSymmetry> repackageSyms() {
        Table<String, Integer, List<BAMSym>> pairMap = HashBasedTable.create();
        List<SeqSymmetry> toReturn = new ArrayList<>();
        for (BAMSym bamSym : bamSyms) {
            String readName = bamSym.getName();
            int mateStart = bamSym.getMateStart();
            if (pairMap.contains(readName, mateStart)) {
                MultiTierSymWrapper bamSymWrapper;
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
                    List<BAMSym> bamSymsList = new ArrayList<>();
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
    public FileTypeCategory getCategory() {
        return FileTypeCategory.Annotation; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getName() {
        return COMPONENT_NAME;
    }
}
