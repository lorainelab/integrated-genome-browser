package com.affymetrix.genometryImpl.parsers;

import java.io.InputStream;
import java.util.List;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 * This is an interface for all the functionality needed to process a
 * specific file type, .bam, .gff, .psl, ... each file type will have
 * one implementation.
 * used to parse data files and to create instances of SymLoaders.
 * This can be registered by bundles to allow new data file
 * formats to be added dynamically.
 * !!! all Parser implementations must have a zero argument constructor !!!
 */
public interface Parser {
	/**
	 * parse the input stream (local or remote) to return a list of SeqSymmetry
	 * @param is the input stream
	 * @param group the AnnotatedSeqGroup
	 * @param nameType either the feature name or annotation type
	 * @param uri the URI
	 * @param annotate_seq whether or not to use annotations
	 * @return the list of SeqSymmetry in the data stream
	 * @throws Exception
	 */
	public List<? extends SeqSymmetry> parse(InputStream is, AnnotatedSeqGroup group, String nameType, String uri, boolean annotate_seq) throws Exception;
}
