package com.affymetrix.genometryImpl.parsers;

import java.io.InputStream;
import java.util.List;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;

public final class LinkPSLParser extends AbstractPSLParser {
	@Override
	public List<? extends SeqSymmetry> parse(InputStream is,
			AnnotatedSeqGroup group, String nameType, String uri, boolean annotate_seq)
			throws Exception {
		setIsLinkPsl(true);
		enableSharedQueryTarget(true);
		// annotate _target_ (which is chromosome for consensus annots, and consensus seq for probeset annots
		// why is annotate_target parameter below set to false?
		// If the name ends with ".link.psl" then assume it is a mapping
		// of probe sets to consensus seqs to genome.
		// Why is it using uri and nameType based on if it was annotate_seq? HV 12/12/11
		return parse(is, annotate_seq ? uri : nameType, null, group, null, false, annotate_seq, false);
	}
}
