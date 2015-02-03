package com.affymetrix.genometry.operator;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryConstants;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.MisMatchGraphSym;
import com.affymetrix.genometry.symmetry.impl.MisMatchPileupGraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.List;

/**
 *
 * @author hiralv
 */
@Component(name = CopyMismatchOperator.COMPONENT_NAME, provide = Operator.class, immediate = true)
public class CopyMismatchOperator implements Operator, ICopy {

    public static final String COMPONENT_NAME = "CopyMismatchOperator";

    @Override
    public String getName() {
        return "copymismatch";
    }

    @Override
    public String getDisplay() {
        return GenometryConstants.BUNDLE.getString("operator_" + getName());
    }

    @Override
    public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
        if (symList.size() != 1 || !(symList.get(0) instanceof MisMatchGraphSym)) {
            return null;
        }
        MisMatchGraphSym sourceSym = (MisMatchGraphSym) symList.get(0);
        MisMatchGraphSym graphSym = null;
        int[] x = new int[sourceSym.getGraphXCoords().length];
        System.arraycopy(sourceSym.getGraphXCoords(), 0, x, 0, sourceSym.getGraphXCoords().length);
        int[] w = new int[sourceSym.getGraphWidthCoords().length];
        System.arraycopy(sourceSym.getGraphWidthCoords(), 0, w, 0, sourceSym.getGraphWidthCoords().length);
        float[] y = new float[sourceSym.getGraphYCoords().length];
        System.arraycopy(sourceSym.getGraphYCoords(), 0, y, 0, sourceSym.getGraphYCoords().length);

        int[][] residues = sourceSym.getAllResidues();
        int[] a = new int[residues[0].length];
        System.arraycopy(residues[0], 0, a, 0, residues[0].length);

        int[] t = new int[residues[1].length];
        System.arraycopy(residues[1], 0, t, 0, residues[0].length);

        int[] g = new int[residues[2].length];
        System.arraycopy(residues[2], 0, g, 0, residues[0].length);

        int[] c = new int[residues[3].length];
        System.arraycopy(residues[3], 0, c, 0, residues[0].length);

        int[] n = new int[residues[4].length];
        System.arraycopy(residues[4], 0, n, 0, residues[0].length);

        String id = sourceSym.getID();
        BioSeq seq = sourceSym.getGraphSeq();
        if (sourceSym instanceof MisMatchPileupGraphSym) {
            graphSym = new MisMatchPileupGraphSym(x, w, y, a, t, g, c, n, id, seq);
        } else {
            graphSym = new MisMatchGraphSym(x, w, y, a, t, g, c, n, id, seq);
        }

        return graphSym;
    }

    @Override
    public int getOperandCountMin(FileTypeCategory category) {
        return category == FileTypeCategory.Mismatch ? 1 : 0;
    }

    @Override
    public int getOperandCountMax(FileTypeCategory category) {
        return category == FileTypeCategory.Mismatch ? 1 : 0;
    }

    @Override
    public boolean supportsTwoTrack() {
        return false;
    }

    @Override
    public FileTypeCategory getOutputCategory() {
        return FileTypeCategory.Mismatch;
    }

    @Override
    public Operator newInstance() {
        try {
            return getClass().getConstructor().newInstance();
        } catch (Exception ex) {

        }
        return null;
    }
}
