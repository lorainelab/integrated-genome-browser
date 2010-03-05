package com.affymetrix.genometryImpl.symmetry;

import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.parsers.BAMParser;


/**
 *
 * @author jnicol
 * This is to be used for all random-access symmetries.
 * Currently used to represent BAM files.
 */
public final class RandomAccessSym extends SimpleSymWithProps {

	public final BAMParser parser;

	public RandomAccessSym(BAMParser parser) {
		this.parser = parser;
	}
	
}
