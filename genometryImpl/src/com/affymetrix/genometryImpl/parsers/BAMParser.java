package com.affymetrix.genometryImpl.parsers;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.util.GeneralUtils;

public class BAMParser implements Parser {

	@Override
	public List<? extends SeqSymmetry> parse(InputStream is,
			AnnotatedSeqGroup group, String nameType, String uri,
			boolean annotate_seq) throws Exception {
		throw new IllegalStateException("BAMParser error, wrong method called");
	}

	public List<? extends SeqSymmetry> parse(URI uri, InputStream istr, AnnotatedSeqGroup group, String featureName, SeqSpan overlap_span) {
		File bamfile = GeneralUtils.convertStreamToFile(istr, featureName);
		bamfile.deleteOnExit();
		BAM bam = new BAM(bamfile.toURI(),featureName, group);
		//for DAS/2 responses, the bam data is already trimmed so should just load it and not build an index, note bam files loaded from a url are not parsed here but elsewhere so the only http inputs are from DAS
		if (uri.getScheme().equals("http")) return bam.parseAll(overlap_span.getBioSeq());
		return bam.getRegion(overlap_span);
	}
}
