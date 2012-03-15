package com.affymetrix.igb.glyph;

import java.util.List;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
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

	public AbstractGraphGlyph createGraphGlyph(GraphSym sym, GraphState gstate) {
		List<GlyphProcessor> glyphProcessors = ExtensionPointHandler.getExtensionPoint(GlyphProcessor.class).getExtensionPointImpls();
		AbstractGraphGlyph graphGlyph = null;
		for (GlyphProcessor glyphProcessor : glyphProcessors) {
			graphGlyph = glyphProcessor.createGraphGlyph(sym, gstate);
			if (graphGlyph != null) {
				break;
			}
		}
		return graphGlyph;
	}
}
