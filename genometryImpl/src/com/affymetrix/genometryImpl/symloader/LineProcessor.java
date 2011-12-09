package com.affymetrix.genometryImpl.symloader;

import java.net.URI;
import java.util.List;

import org.broad.tribble.readers.LineReader;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 * file types that use tabix/bin search must implement this interface
 * there should be one LineProcessor for each file type
 * that performs the parsing of data lines
 */
public interface LineProcessor {
	/**
	 * this is the main method. The LineReader will return
	 * the lines that are in the span (Seq, start, end) requested
	 * and those lines will be passed in here to be parsed
	 * into SeqSymmetry
	 * @param seq the sequence
	 * @param lineReader the LineReader from QueryReader
	 * @return the SeqSymmetry list from the parsing
	 */
	public List<? extends SeqSymmetry> processLines(BioSeq seq, LineReader lineReader) throws Exception;
	/**
	 * perform any initialization here
	 * @param uri the uri of the data source
	 */
	public void init(URI uri) throws Exception;
	/**
	 * @return the pref list (file extensions) 
	 */
	public List<String> getFormatPrefList();

	public SeqSpan getSpan(String line);

	public boolean processInfoLine(String line, List<String> infoLines);
}
