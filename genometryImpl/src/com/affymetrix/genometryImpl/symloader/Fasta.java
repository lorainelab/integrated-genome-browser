package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jnicol
 */
public class Fasta extends SymLoader {
	private static final Pattern header_regex = Pattern.compile("^\\s*>(.+)");
	private final AnnotatedSeqGroup group;
	public Fasta(URI uri, AnnotatedSeqGroup group) {
		super(uri);
		this.group = group;
	}

	@Override
	public String getRegionResidues(SeqSpan span) {
		BufferedInputStream bis = null;
		BufferedReader br = null;
		int count = 0;
		String residues = "";
		Matcher matcher = header_regex.matcher("");
		try {
			bis = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
			br = new BufferedReader(new InputStreamReader(bis));
			String header = br.readLine();
			while (br.ready() && (!Thread.currentThread().isInterrupted())) {  // loop through lines till find a header line
				if (header == null) {
					continue;
				}  // skip null lines
				matcher.reset(header);
				boolean matched = matcher.matches();

				if (!matched) {
					continue;
				}
				String seqid = matcher.group(1);
				BioSeq seq = group.getSeq(seqid);
				boolean seqMatch = (seq != null && seq == span.getBioSeq());

				StringBuffer buf = new StringBuffer();
				while (br.ready() && (!Thread.currentThread().isInterrupted())) {
					String line = br.readLine();
					if (line == null || line.length() == 0) {
						continue;
					}  // skip null and empty lines

					if (line.charAt(0) == ';') {
						continue;
					} // skip comment lines

					// break if hit header for another sequence --
					if (line.startsWith(">")) {
						header = line;
						break;
					}
					if (seqMatch) {
						line = line.trim();
						if (count + line.length() <= span.getMin()) {
							// skip lines
							count += line.length();
							continue;
						}
						if (count > span.getMax()) {
							break; // should never happen
						}
						if (count < span.getMin()) {
							// skip beginning characters
							line = line.substring(span.getMin() - count);
							count = span.getMin();
						}
						if (count + line.length() >= span.getMax()) {
							// skip ending characters
							line = line.substring(0, count + line.length() - span.getMax());
						}
						buf.append(line);
					}
				}

				// Didn't use .toString() here because of a memory bug in Java
				// (See "stringbuffer memory java" for more details.)
				residues = new String(buf);
				buf.setLength(0);
				buf = null; // immediately allow the gc to use this memory
				residues = residues.trim();
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return residues;
	}
}
