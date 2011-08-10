package com.affymetrix.igb.shared;

import com.affymetrix.igb.glyph.MapViewGlyphFactoryI;

/**
 *
 * @author hiralv
 */
public interface ExtendedMapViewGlyphFactoryI extends MapViewGlyphFactoryI {
	
	public String getName();
	
	public boolean isFileSupported(String format);
}
