package com.affymetrix.igb.viewmode;

import java.net.URI;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symloader.TbiZoomSymLoader;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;

public class TbiSemanticZoomGlyphFactory extends GzIndexedSemanticZoomGlyphFactory {
	public static final String TBI_ZOOM_DISPLAYER_EXTENSION = "tbi";
	private final FileTypeCategory category;

	public TbiSemanticZoomGlyphFactory(FileTypeCategory category, MapViewGlyphFactoryI defaultDetailGlyphFactory, MapViewGlyphFactoryI heatMapGraphGlyphFactory, MapViewGlyphFactoryI graphGlyphFactory) {
		super(defaultDetailGlyphFactory, heatMapGraphGlyphFactory, graphGlyphFactory);
		this.category = category;
	}

	@Override
	public String getName() {
		return TBI_ZOOM_DISPLAYER_EXTENSION + "_semantic_zoom_" + category.toString().toLowerCase();
	}

	@Override
	public String getExtension() {
		return TBI_ZOOM_DISPLAYER_EXTENSION;
	}

	@Override
	protected FileTypeCategory getFileTypeCategory() {
		return category;
	}

	@Override
	protected SymLoader createSummarySymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
		return new TbiZoomSymLoader(uri, featureName, group);
	}

	@Override
	public boolean isURISupported(String uri) {
		String extension = FileTypeHolder.getInstance().getExtensionForURI(uri);
		return FileTypeHolder.getInstance().getTabixFileTypes().contains(extension) && super.isURISupported(uri);
	}

	@Override
	public boolean canAutoLoad(String uri) {
		return isURISupported(uri);
	}
}
