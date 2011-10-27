package com.affymetrix.igb.glyph;

import java.util.List;

import com.affymetrix.common.ExtensionPointImplHolder;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.GlyphProcessor;
import com.affymetrix.igb.shared.GraphGlyph;

public class GlyphProcessorUtil {
	private static GlyphProcessorUtil instance = new GlyphProcessorUtil();
	
	private GlyphProcessorUtil() {
		super();
		ExtensionPointImplHolder.getInstance(GlyphProcessor.class).addExtensionPointImpl(new MismatchPileupGlyphProcessor());
	}
	
	public static GlyphProcessorUtil getInstance() {
		return instance;
	}

	public void fireProcessGlyph(GlyphI glyph) {
		List<GlyphProcessor> glyphProcessors = ExtensionPointImplHolder.getInstance(GlyphProcessor.class).getExtensionPointImpls();
		for (GlyphProcessor glyphProcessor : glyphProcessors) {
			glyphProcessor.processGlyph(glyph);
		}
	}

	public GraphGlyph createGraphGlyph(GraphSym sym, GraphState gstate) {
		List<GlyphProcessor> glyphProcessors = ExtensionPointImplHolder.getInstance(GlyphProcessor.class).getExtensionPointImpls();
		GraphGlyph graphGlyph = null;
		for (GlyphProcessor glyphProcessor : glyphProcessors) {
			graphGlyph = glyphProcessor.createGraphGlyph(sym, gstate);
			if (graphGlyph != null) {
				break;
			}
		}
		return graphGlyph;
	}
}
