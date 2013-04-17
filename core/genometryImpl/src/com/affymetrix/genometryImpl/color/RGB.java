package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.parsers.TrackLineParser;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class RGB implements ColorProvider {
	private static RGB INSTANCE = new RGB();
	
	private RGB(){}
	
	public static RGB getInstance(){
		return INSTANCE;
	}
	
	@Override
	public Color getColor(SymWithProps sym){
		return (Color) sym.getProperty(TrackLineParser.ITEM_RGB);
	}
}
