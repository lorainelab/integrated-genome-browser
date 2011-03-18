package com.affymetrix.genometryImpl.symloader;

import java.net.URI;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;

/**
 * interface to create instances of SymLoaders. This can be registered
 * by bundles to allow new data file formats to be added dynamically.
 */
public interface SymLoaderFactory {
	public String[] getExtensions();
	public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group);
}
