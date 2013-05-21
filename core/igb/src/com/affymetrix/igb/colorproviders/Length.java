package com.affymetrix.igb.colorproviders;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.color.ColorProvider;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.color.ColorPalette;
import com.affymetrix.genoviz.color.ColorScheme;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class Length extends ColorProvider {
	private static GenometryModel model = GenometryModel.getGenometryModel();
	private ColorPalette cp = new ColorPalette(ColorScheme.ACCENT8);
	
	@Override
	public Color getColor(SeqSymmetry sym){
		return cp.getColor(String.valueOf(sym.getSpan(model.getSelectedSeq()).getLength()));
	}
}
