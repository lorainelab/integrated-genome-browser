package com.affymetrix.igb.colorproviders;

import java.awt.Color;

import com.affymetrix.genometryImpl.color.ColorProvider;
import com.affymetrix.genometryImpl.color.Parameter;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genoviz.color.ColorPalette;
import com.affymetrix.genoviz.color.ColorScheme;

/**
 *
 * @author hiralv
 */
public class Property extends ColorProvider {
	private final static String PROPERTY = "property";
	public final static String DEFAULT_PROPERTY = "id";
	
	protected Parameter<String> property = new Parameter<String>(DEFAULT_PROPERTY);
	private ColorPalette cp = new ColorPalette(ColorScheme.ACCENT8);
	
	public Property(){
		super();
		parameters.addParameter(PROPERTY, String.class, property);
	}
	
	@Override
	public Color getColor(SeqSymmetry sym){
		if(sym instanceof SymWithProps){
			Object value = ((SymWithProps)sym).getProperty(property.get());
			if(value != null){
				return cp.getColor(value.toString());
			}
		}
		return null;
	}
}
