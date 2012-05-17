package com.affymetrix.igb.viewmode;

import java.net.URI;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symloader.BaiZoomSymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;

public class BaiSemanticZoomGlyphFactory extends GzIndexedSemanticZoomGlyphFactory {
	public static final String BAI_ZOOM_DISPLAYER_EXTENSION = "bai";

	public BaiSemanticZoomGlyphFactory(MapViewGlyphFactoryI defaultGlyphFactory, MapViewGlyphFactoryI graphGlyphFactory) {
		super(defaultGlyphFactory, graphGlyphFactory);
	}

	@Override
	public String getExtension() {
		return BAI_ZOOM_DISPLAYER_EXTENSION;
	}

	@Override
	protected FileTypeCategory getFileTypeCategory() {
		return FileTypeCategory.Alignment;
	}

	@Override
	protected SymLoader createSummarySymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
		return new BaiZoomSymLoader(uri, featureName, group);
	}

}
