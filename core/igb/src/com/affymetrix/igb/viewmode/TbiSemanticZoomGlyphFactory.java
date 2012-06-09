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

	public TbiSemanticZoomGlyphFactory(MapViewGlyphFactoryI defaultGlyphFactory, MapViewGlyphFactoryI graphGlyphFactory) {
		super(defaultGlyphFactory, graphGlyphFactory);
	}

	@Override
	public String getExtension() {
		return TBI_ZOOM_DISPLAYER_EXTENSION;
	}

	@Override
	protected FileTypeCategory getFileTypeCategory() {
		return FileTypeCategory.Annotation;
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
