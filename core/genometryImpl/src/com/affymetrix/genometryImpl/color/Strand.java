package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class Strand extends ColorProvider {
	private final static String FORWARD_COLOR = "+";
	private final static String REVERSE_COLOR = "-";
	private final static Color DEFAULT_FORWARD_COLOR = new Color(204, 255, 255);
	private final static Color DEFAULT_REVERSE_COLOR = new Color(51, 255, 255);
	private static GenometryModel model = GenometryModel.getGenometryModel();
	
	private Parameter<Color> forwardColor = new Parameter<Color>(DEFAULT_FORWARD_COLOR);
	private Parameter<Color> reverseColor = new Parameter<Color>(DEFAULT_REVERSE_COLOR);
	public Strand(){
		super();
		parameters.addParameter(FORWARD_COLOR, Color.class, forwardColor);
		parameters.addParameter(REVERSE_COLOR, Color.class, reverseColor);
	}
		
	@Override
	public Color getColor(SeqSymmetry sym) {
		if(sym.getSpan(model.getSelectedSeq()).isForward()){
			return forwardColor.get();
		}
		return reverseColor.get();
	}
}
