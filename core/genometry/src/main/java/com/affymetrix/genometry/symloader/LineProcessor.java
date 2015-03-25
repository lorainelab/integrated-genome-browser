package com.affymetrix.genometry.symloader;

import java.net.URI;
import java.util.List;

import org.broad.tribble.readers.LineReader;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

/**
 * file types that use tabix/bin search must implement this interface there
 * should be one LineProcessor for each file type that performs the parsing of
 * data lines
 */
public interface LineProcessor {

    /**
     * This is the main method. The LineReader will return the lines that are in
     * the span (Seq, start, end) requested and those lines will be passed in
     * here to be parsed into SeqSymmetry
     *
     * @param seq the sequence
     * @param lineReader the LineReader from QueryReader
     * @return the SeqSymmetry list from the parsing
     */
    public List<? extends SeqSymmetry> processLines(BioSeq seq, LineReader lineReader) throws Exception;

    /**
     * perform any initialization here
     *
     * @param uri the uri of the data source
     */
    public void init(URI uri) throws Exception;

    /**
     * get the file extensions
     *
     * @return the pref list (file extensions)
     */
    public List<String> getFormatPrefList();

    /**
     * interpret the line as a SeqSymmetry and return the span for the
     * SeqSymmetry
     *
     * @param line - the imput line
     * @return - the SeqSpan represented by the line
     */
    public SeqSpan getSpan(String line);

    /**
     * Check to see if this line is an "info" line - a special line giving
     * information about the data, not specific SeqSymmetries, and possibly
     * adding the line to the passed infoLines parameter. These lines vary by
     * file type and may start with clone(), for example.
     *
     * @param line - the line to check
     * @param infoLines - accumulation of info lines
     * @return - true if this line was an info line, false otherwise
     */
    public boolean processInfoLine(String line, List<String> infoLines);

    /**
     * Tells if it is okay to load data on multiple threads.
     *
     * @return
     */
    public boolean isMultiThreadOK();
}
