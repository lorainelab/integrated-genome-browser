package com.affymetrix.genometryImpl.parsers;

import java.net.URI;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.symloader.SymLoader;

public interface FileTypeHandler {
	/**
	 * get the name of the Parser, displayed in the FileChooser popup
	 * @return the name of the parser
	 */
	public String getName();
	/**
	 * get the file extensions (there may be more than one) for this
	 * file type
	 * @return the array of possible file extensions
	 */
	public String[] getExtensions();
	/**
	 * get an appropriate SymLoader for this file type
	 * @param uri the URI for the symloader
	 * @param featureName the feature name for the symloader
	 * @param group the AnnotatedSeqGroup for the symloader
	 * @return the SymLoader to use
	 */
	public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group);
	/**
	 * get a Parser for the file type
	 * @return the Parser
	 */
	public Parser getParser();
	/**
	 * get an IndexWriter for the file type
	 * @return the IndexWriter
	 */
	public IndexWriter getIndexWriter(String stream_name);

	/**
	 * @return the category of this file type
	 */
	public FileTypeCategory getFileTypeCategory();
}
