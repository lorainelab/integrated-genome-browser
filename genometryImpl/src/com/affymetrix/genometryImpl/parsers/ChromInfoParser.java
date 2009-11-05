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
import java.util.regex.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;

public final class ChromInfoParser {
	private static final Pattern tab_regex = Pattern.compile("\t");
	//static BioSeq default_seq_template = new BioSeq();

	//BioSeq template_seq = default_seq_template;

	/** Constructs a ChromInfoParser with a default template sequence. */
	//public ChromInfoParser() { }

	//public ChromInfoParser(BioSeq template) {
	//template_seq = template;
	//}

	/**
	 *  Parses a chrom_info.txt file, creates a new AnnotatedSeqGroup and
	 *  adds it to the GenometryModel.
	 */
	public static final AnnotatedSeqGroup parse(InputStream istr, GenometryModel gmodel, String genome_version)
		throws IOException {

		AnnotatedSeqGroup seq_group = gmodel.addSeqGroup(genome_version);
		BufferedReader dis = new BufferedReader(new InputStreamReader(istr));
		String line;
		while ((line = dis.readLine()) != null) {
			if ( (line.length() == 0) || line.equals("") || line.startsWith("#"))  { continue; }
			String[] fields = tab_regex.split(line);
			if (fields.length <= 0) { continue; }
			String chrom_name = fields[0];

			int chrLength = Integer.parseInt(fields[1]);
			BioSeq chrom = seq_group.getSeq(chrom_name);
			if (chrom == null) {  // if chrom already in seq group, then don't add to list
				chrom = seq_group.addSeq(chrom_name, chrLength);
				chrom.setVersion(genome_version);
			}
		}
		return seq_group;
	}

}
