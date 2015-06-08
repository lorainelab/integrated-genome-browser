package com.affymetrix.igb.thresholding;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.lorainelab.igb.genoviz.extensions.glyph.GraphGlyph;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 *
 * @author hiralv
 */
public class ThresholdOperator implements Operator {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("thresholding");
    final public GraphGlyph sgg;
    final ViewI view;

    public ThresholdOperator(GraphGlyph sgg, ViewI view) {
        this.sgg = sgg;
        this.view = view;
    }

    public String getName() {
        return "threshold";
    }

    @Override
    public String getDisplay() {
        return BUNDLE.getString("thresholding");
    }

    public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
        SimpleSymWithProps psym = new SimpleSymWithProps();
        psym.addSpan(new SimpleMutableSeqSpan(0, aseq.getLength(), aseq));
        sgg.drawThresholdedRegions(view, psym, aseq);

        // If there were any overlapping child symmetries, collapse them
        // (by intersecting psym with itself)
        SimpleSymWithProps result_sym = new SimpleSymWithProps();
        SeqUtils.intersection(psym, psym, result_sym, aseq);
        psym = result_sym;

        return psym;
    }

    public int getOperandCountMin(FileTypeCategory category) {
        return category == FileTypeCategory.Graph ? 1 : 0;
    }

    public int getOperandCountMax(FileTypeCategory category) {
        return category == FileTypeCategory.Graph ? 1 : 0;
    }

    public Map<String, Class<?>> getParametersType() {
        return null;
    }

    public boolean setParametersValue(Map<String, Object> parms) {
        return true;
    }

    public boolean supportsTwoTrack() {
        return false;
    }

    public FileTypeCategory getOutputCategory() {
        return FileTypeCategory.Annotation;
    }

    @Override
    public Operator newInstance() {
        try {
            return getClass().getConstructor(GraphGlyph.class, ViewI.class).newInstance(sgg, view);
        } catch (Exception ex) {

        }
        return null;
    }

}
