package com.affymetrix.igb.colorproviders;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genoviz.color.ColorPalette;
import com.affymetrix.genoviz.color.ColorScheme;
import com.affymetrix.igb.IGBConstants;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
/*
    03/28/17
    Changes made to provide Heatmap editor for length
 */
public class Length extends Score {

    private static GenometryModel model = GenometryModel.getInstance();
    private ColorPalette cp = new ColorPalette(ColorScheme.ACCENT8);

    @Override
    public String getName() {
        return "length";
    }

    @Override
    public String getDisplay() {
        return IGBConstants.BUNDLE.getString("color_by_" + getName());
    }

    @Override
    public Color getColor(SeqSymmetry sym) {
//        return cp.getColor(String.valueOf(sym.getSpan(model.getSelectedSeq().orElse(null)).getLength()));

        float[] lData = new float[]{0f};
        model.getSelectedSeq().ifPresent(s -> {
            SeqSpan span = sym.getSpan(s);
            if (span != null) {
                lData[0] = span.getLength();
            } else {
                //This is to handle length when "genome" is selected in chromosomes
                lData[0] = sym.getSpan(0).getLength();
            }
        });
        return getScoreColor(lData[0]);
    }
}
