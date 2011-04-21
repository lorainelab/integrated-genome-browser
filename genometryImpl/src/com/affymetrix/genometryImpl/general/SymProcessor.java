package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.parsers.BAMParser;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.parsers.Parser;
import com.affymetrix.genometryImpl.symloader.SymLoader;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Singleton to perform all SymLoader static methods
 */
public class SymProcessor {
	private static final SymProcessor instance = new SymProcessor();

	public static SymProcessor getInstance() {
		return instance;
	}

	private SymProcessor() {
		super();
    }
	/**
	 * parse the input stream, with parser determined by extension.
	 * @param extension
	 * @param uri - the URI corresponding to the file/URL
	 * @param istr
	 * @param group
	 * @param featureName
	 * @return list of symmetries
	 * @throws Exception
	 */
	public List<? extends SeqSymmetry> parse(
			String extension, URI uri, InputStream istr, AnnotatedSeqGroup group, String featureName, SeqSpan overlap_span)
			throws Exception {
		BufferedInputStream bis = new BufferedInputStream(istr);
		extension = extension.substring(extension.indexOf('.') + 1);	// strip off first .

		FileTypeHandler fileTypeHandler = FileTypeHolder.getInstance().getFileTypeHandler(extension);
		if (fileTypeHandler == null) {
			Logger.getLogger(SymLoader.class.getName()).log(
				Level.WARNING, "ABORTING FEATURE LOADING, FORMAT NOT RECOGNIZED: {0}", extension);
			return null;
		}
		else {
			Parser parser = fileTypeHandler.getParser();
			if (parser instanceof BAMParser) {
				return ((BAMParser)parser).parse(uri, istr, group, featureName, overlap_span);
			}
			else {
				return parser.parse(bis, group, featureName, uri.toString(), false);
			}
		}
	}
}
