package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public class Strand extends ColorProvider {
	private final static String FORWARD_COLOR = "+";
	private final static String REVERSE_COLOR = "-";
	private static GenometryModel model = GenometryModel.getGenometryModel();
	
	private Parameters parameters;
	public Strand(){
		parameters = new Parameters();
		parameters.addParameter(FORWARD_COLOR, Color.class, new Color(204, 255, 255));
		parameters.addParameter(REVERSE_COLOR, Color.class, new Color(51, 255, 255));
	}
		
	@Override
	public Color getColor(SeqSymmetry sym) {
		if(sym.getSpan(model.getSelectedSeq()).isForward()){
			return (Color)parameters.getParameterValue(FORWARD_COLOR);
		}
		return (Color)parameters.getParameterValue(REVERSE_COLOR);
	}

	@Override
	public Map<String, Class<?>> getParameters(){
		return parameters.getParametersType();
	}

	@Override
	public boolean setParameter(String key, Object value) {
		return parameters.setParameterValue(key, value);
	}
	
	@Override
	public Object getParameterValue(String key) {
		return parameters.getParameterValue(key);
	}
}
