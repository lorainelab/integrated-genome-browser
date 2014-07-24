package com.affymetrix.genometryImpl.parsers.graph;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.Parser;
import com.affymetrix.genometryImpl.symmetry.impl.GraphSym;

/**
 * interface for all graph file type funtionality. Only applicable to
 * graph file types
 */
public interface GraphParser extends Parser {
	public List<GraphSym> readGraphs(InputStream istr, String stream_name, AnnotatedSeqGroup seq_group, BioSeq seq)
		throws IOException;
	public void writeGraphFile(GraphSym gsym, AnnotatedSeqGroup seq_group, String file_name) throws IOException;
}
