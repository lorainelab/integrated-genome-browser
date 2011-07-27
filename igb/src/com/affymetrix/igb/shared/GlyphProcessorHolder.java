package com.affymetrix.igb.shared;

import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genoviz.bioviews.GlyphI;

public class GlyphProcessorHolder {
	private static GlyphProcessorHolder instance = new GlyphProcessorHolder();
	private GlyphProcessorHolder() {
		super();
	}
	public static GlyphProcessorHolder getInstance() {
		return instance;
	}
	public interface GlyphProcessor {
		public void processGlyph(GlyphI glyph);
		public GraphGlyph createGraphGlyph(GraphSym sym, GraphState gstate);
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
