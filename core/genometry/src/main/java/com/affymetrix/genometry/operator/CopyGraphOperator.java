package com.affymetrix.genometry.operator;

import aQute.bnd.annotation.component.Component;
import java.util.List;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryConstants;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

@Component(name = CopyGraphOperator.COMPONENT_NAME, provide = Operator.class, immediate = true)
public final class CopyGraphOperator implements Operator, ICopy {

    public static final String COMPONENT_NAME = "CopyGraphOperator";

    public CopyGraphOperator() {
    }

    @Override
    public String getName() {
        return "copygraph";
    }

    @Override
    public String getDisplay() {
        return GenometryConstants.BUNDLE.getString("operator_" + getName());
    }

    @Override
    public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
        if (symList.size() != 1 || !(symList.get(0) instanceof GraphSym)) {
            return null;
        }
        GraphSym sourceSym = (GraphSym) symList.get(0);
        GraphSym graphSym;
        int[] x = new int[sourceSym.getGraphXCoords().length];
        System.arraycopy(sourceSym.getGraphXCoords(), 0, x, 0, sourceSym.getGraphXCoords().length);
        float[] y = new float[sourceSym.getGraphYCoords().length];
        System.arraycopy(sourceSym.getGraphYCoords(), 0, y, 0, sourceSym.getGraphYCoords().length);
        String id = sourceSym.getID();
        BioSeq seq = sourceSym.getGraphSeq();
        if (sourceSym.hasWidth()) {
            int[] w = new int[sourceSym.getGraphWidthCoords().length];
            System.arraycopy(sourceSym.getGraphWidthCoords(), 0, w, 0, sourceSym.getGraphWidthCoords().length);
            graphSym = new GraphSym(x, w, y, id, seq);
        } else {
            graphSym = new GraphSym(x, y, id, seq);
        }
        return graphSym;
    }

    @Override
    public int getOperandCountMin(FileTypeCategory category) {
        return category == FileTypeCategory.Graph ? 1 : 0;
    }

    @Override
    public int getOperandCountMax(FileTypeCategory category) {
        return category == FileTypeCategory.Graph ? 1 : 0;
    }

    @Override
    public boolean supportsTwoTrack() {
        return false;
    }

    @Override
    public FileTypeCategory getOutputCategory() {
        return FileTypeCategory.Graph;
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
