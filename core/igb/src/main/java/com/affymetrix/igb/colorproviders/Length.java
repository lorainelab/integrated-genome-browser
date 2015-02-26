package com.affymetrix.igb.colorproviders;

import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.color.ColorProvider;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genoviz.color.ColorPalette;
import com.affymetrix.genoviz.color.ColorScheme;
import com.affymetrix.igb.IGBConstants;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class Length extends ColorProvider {

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
        return cp.getColor(String.valueOf(sym.getSpan(model.getSelectedSeq()).getLength()));
    }
}
