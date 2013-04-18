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
	public Color getColor(Object obj){
		if(obj instanceof SymWithProps){
			return (Color) ((SymWithProps)obj).getProperty(TrackLineParser.ITEM_RGB);
		}
		return null;
	}
	
	@Override
	public void update(){
		//Do Nothing
	}
}
