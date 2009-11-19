/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

/** A parser for the Affymetrix Generic CHP files containing copy number data. */
package com.affymetrix.genometryImpl.parsers.gchp;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import java.io.*;
import java.util.*;

public final class AffyCnChpParser {
	public void parse(File file, ChromLoadPolicy loadPolicy, InputStream istr, String stream_name, AnnotatedSeqGroup seq_group) throws IOException {
		SingletonGenometryModel.logInfo("Parsing with " + this.getClass().getName() + ": " + stream_name);
		try {

			AffyGenericChpFile chpFile = AffyGenericChpFile.parse(file, loadPolicy, istr, false);

			AffyDataGroup group = chpFile.groups.get(0);
			AffyDataSet dataSet = group.getDataSets().get(0);


			for (String seq_name : dataSet.getChromosomeNames()) {
				// Make sure that all the seq's mentioned in the header are
				// present in the SeqGroup.  Adds them if necessary.
				getSeq(seq_group, seq_name);
			}            

			for (AffySingleChromData data : dataSet.getSingleChromData()) {
				BioSeq seq = getSeq(seq_group, data.displayName);
				List<SeqSymmetry> syms = data.makeGraphs(seq);
				for (SeqSymmetry sym : syms) {
					seq.addAnnotation(sym); 
				}
			}

		} catch (Exception e) {
			if (! (e instanceof IOException)) {
				IOException ioe = new IOException("IOException for file: " + stream_name);
				e.printStackTrace();
				ioe.initCause(e);
				throw ioe;
			}
		}
	}

	private BioSeq getSeq(AnnotatedSeqGroup seq_group, String seqid) {
		BioSeq aseq = seq_group.getSeq(seqid);
		if (aseq == null) {
			aseq = seq_group.addSeq(seqid, 1);
		}
		return aseq;
	} 
}
