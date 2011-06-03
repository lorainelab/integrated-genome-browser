package com.affymetrix.igb.glyph;

import java.util.ArrayList;
import java.util.List;

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
}
