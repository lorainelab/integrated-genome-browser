/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.genometryImpl.parsers;

import java.io.*;
import java.util.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.SeqUtils;

public final class SimpleBedParser implements AnnotationWriter {

	public String getMimeType() { return "text/plain"; }

	public boolean writeAnnotations(Collection<SeqSymmetry> syms, MutableAnnotatedBioSeq seq,
			String type, OutputStream outstream) throws IOException {
		boolean success;
		ArrayList<SeqSpan> spanlist = new ArrayList<SeqSpan>(syms.size());  // initialize to number of top-level syms, won't be lower...
		for (SeqSymmetry sym : syms) {
			SeqUtils.collectLeafSpans(sym, seq, spanlist);
		}

		try {
			Writer bw = new BufferedWriter(new OutputStreamWriter(outstream));
			for (SeqSpan span : spanlist) {
				bw.write(span.getBioSeq().getID());
				bw.write('\t');
				bw.write(Integer.toString(span.getMin()));
				bw.write('\t');
				bw.write(Integer.toString(span.getMax()));
				bw.write('\n');
			}
			bw.flush();
			success = true;
		}
		catch (Exception ex) {
			success = false;
			IOException ioe = new IOException(ex.getMessage());
			ioe.initCause(ex);
			throw ioe;
		}
		return success;
	}
}
