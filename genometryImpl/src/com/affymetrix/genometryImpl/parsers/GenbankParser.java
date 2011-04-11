package com.affymetrix.genometryImpl.parsers;

import java.io.InputStream;
import java.util.List;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SeqSymmetry;

public class GenbankParser implements Parser {
	@Override
	public List<? extends SeqSymmetry> parse(InputStream is,
			AnnotatedSeqGroup group, String nameType, String uri,
			boolean annotate_seq) throws Exception {
		throw new IllegalStateException("not processed here");
	}
}
