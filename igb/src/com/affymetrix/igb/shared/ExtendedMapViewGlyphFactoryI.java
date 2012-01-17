package com.affymetrix.igb.shared;


/**
 *
 * @author hiralv
 */
public interface ExtendedMapViewGlyphFactoryI extends MapViewGlyphFactoryI {
	
	public String getName();
	
	public boolean isFileSupported(String format);
}
