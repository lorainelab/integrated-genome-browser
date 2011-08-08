package com.affymetrix.igb.glyph;

import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.GlyphProcessor;
import com.affymetrix.igb.shared.GraphGlyph;

public class GlyphProcessorHolder {
	private static GlyphProcessorHolder instance = new GlyphProcessorHolder();
	
	private GlyphProcessorHolder() {
		super();
		addGlyphProcessor(new MismatchPileupGlyphProcessor());
	}
	
	public static GlyphProcessorHolder getInstance() {
		return instance;
	}

	private List<GlyphProcessor> glyphProcessors = new ArrayList<GlyphProcessor>();

	public void addGlyphProcessor(GlyphProcessor glyphProcessor) {
		glyphProcessors.add(glyphProcessor);
	}

	public void removeGlyphProcessor(GlyphProcessor glyphProcessor) {
		glyphProcessors.remove(glyphProcessor);
	}

	public List<GlyphProcessor> getGlyphProcessors() {
		return glyphProcessors;
	}

	public void fireProcessGlyph(GlyphI glyph) {
		for (GlyphProcessor glyphProcessor : glyphProcessors) {
			glyphProcessor.processGlyph(glyph);
		}
	}

	public GraphGlyph createGraphGlyph(GraphSym sym, GraphState gstate) {
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
