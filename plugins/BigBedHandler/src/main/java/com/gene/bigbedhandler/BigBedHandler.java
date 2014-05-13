package com.gene.bigbedhandler;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import com.affymetrix.genometryImpl.parsers.Parser;
import com.affymetrix.genometryImpl.symloader.SymLoader;

public class BigBedHandler implements FileTypeHandler {
	private static final String[] EXTENSIONS = new String[]{"bb", "bigBed", "bigbed"};

	public BigBedHandler() {
		super();
	}

	@Override
	public String getName() {
		return "BED";
	}

	@Override
	public String[] getExtensions() {
		return EXTENSIONS;
	}

	public static List<String> getFormatPrefList() {
		return Arrays.asList(EXTENSIONS);
	}

	@Override
	public SymLoader createSymLoader(URI uri, String featureName,
			AnnotatedSeqGroup group) {
		return new BigBedSymLoader(uri, featureName, group);
	}

	@Override
	public Parser getParser() {
		return null;
	}

	@Override
	public IndexWriter getIndexWriter(String stream_name) {
		return null;
	}

	@Override
	public FileTypeCategory getFileTypeCategory() {
		return FileTypeCategory.Annotation;
	}
}
