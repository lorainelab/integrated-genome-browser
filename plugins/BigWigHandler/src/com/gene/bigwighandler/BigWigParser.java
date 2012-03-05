package com.gene.bigwighandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.graph.GraphParser;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class BigWigParser implements GraphParser {

	@Override
	public List<? extends SeqSymmetry> parse(InputStream is,
			AnnotatedSeqGroup group, String nameType, String uri,
			boolean annotate_seq) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<GraphSym> readGraphs(InputStream istr, String stream_name,
			AnnotatedSeqGroup seq_group, BioSeq seq) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeGraphFile(GraphSym gsym, AnnotatedSeqGroup seq_group,
			String file_name) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
