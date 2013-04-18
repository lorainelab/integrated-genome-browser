package com.affymetrix.genometryImpl.color;

import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class Strand implements ColorProvider {
	
	private Color forwardColor = new Color(204, 255, 255), reverseColor = new Color(51, 255, 255);
	
	@Override
	public Color getColor(Object obj) {
		if(obj == Boolean.TRUE){
			return forwardColor;
		}
		return reverseColor;
	}

	public void setForwardColor(Color color){
		forwardColor = color;
	}
	
	public Color getForwardColor(){
		return forwardColor;
	}
	
	public void setReverseColor(Color color){
		reverseColor = color;
	}
	
	public Color getReverseColor(){
		return reverseColor;
	}
	
	@Override
	public void update() {
		//Do Nothing
	}
}
