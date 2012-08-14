package com.affymetrix.igb.glyph;

import java.util.List;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.GlyphProcessor;

public class GlyphProcessorUtil {
	private static GlyphProcessorUtil instance = new GlyphProcessorUtil();
	
	private GlyphProcessorUtil() {
		super();
	}
	
	public static GlyphProcessorUtil getInstance() {
		return instance;
	}

	public void fireProcessGlyph(GlyphI glyph) {
		List<GlyphProcessor> glyphProcessors = ExtensionPointHandler.getExtensionPoint(GlyphProcessor.class).getExtensionPointImpls();
		for (GlyphProcessor glyphProcessor : glyphProcessors) {
			glyphProcessor.processGlyph(glyph);
		}
	}

}
