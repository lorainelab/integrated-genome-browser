package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.parsers.TrackLineParser;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import java.awt.Color;
import java.util.Map;

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
	
	@Override
	public Map<String, Class<?>> getParameters(){
		return null;
	}

	@Override
	public void setParameters( Map<String, Object> params){
	}

	@Override
	public boolean setParameter(String key, Object value){
		return false;
	}
}
