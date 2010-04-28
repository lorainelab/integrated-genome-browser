package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.SymLoader;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jnicol
 */
public class BNIB extends SymLoader {
	public BNIB(URI uri) {
		super(uri);
	}

	@Override
	public String getRegionResidues(SeqSpan span) {
		Logger.getLogger(this.getClass().getName()).log(
				Level.WARNING, "Retrieving region is not supported.  Returning entire chromosome.");
		return "";
	}
}
