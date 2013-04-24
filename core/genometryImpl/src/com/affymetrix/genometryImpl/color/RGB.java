package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.parsers.TrackLineParser;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class RGB extends ColorProvider {
		
	@Override
	public Color getColor(Object obj){
		if(obj instanceof SymWithProps){
			return (Color) ((SymWithProps)obj).getProperty(TrackLineParser.ITEM_RGB);
		}
		return null;
	}
}
