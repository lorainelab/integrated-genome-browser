/**
 *   Copyright (c) 2006-2007 Affymetrix, Inc.
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

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GFF3Sym;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 *  GFF version 3 parser.
 * <pre>
 *  GFF format is 9 tab-delimited fields:
 *   {@literal <seqname> <source> <feature> <start> <end> <score> <strand> <frame> <phase> <attributes>}
 *
 *  The attribute field contains URL-encoded tag-value pairs separated by semicolons:
 *  "tag1=val1;tag2=val2;tag3=this%20is%20a%20test".
 *
 *  See http://song.sourceforge.net/gff3.shtml.
 *</pre>
 *
 * @version $Id$
 */
public final class GFF3Parser {
	private static final boolean DEBUG = false;
	public static final int GFF3 = 3;

	// Any tag name beginning with a capital letter must be one of the reserved names.
	public static final String GFF3_ID = "ID";
	public static final String GFF3_NAME = "Name";
	public static final String GFF3_ALIAS = "Alias";
	public static final String GFF3_PARENT = "Parent";
	public static final String GFF3_TARGET = "Target";
	public static final String GFF3_GAP = "Gap";
	public static final String GFF3_DERIVES_FROM = "Derives_from";
	public static final String GFF3_NOTE = "Note";
	public static final String GFF3_DBXREF = "Dbxref";
	public static final String GFF3_ONTOLOGY_TERM = "Ontology_term";

	// Must be exactly one tab between each column; not spaces or multiple tabs.
	private static final Pattern line_regex = Pattern.compile("\\t");
	private static final Pattern directive_version = Pattern.compile("##gff-version\\s+(.*)");

	private static final boolean use_track_lines = true;
	private TrackLineParser track_line_parser = new TrackLineParser();

	private static final Set<String> IGNORABLE_TYPES;
	private static final Set<String> seenTypes = Collections.<String>synchronizedSet(new HashSet<String>());

	static {
		Set<String> types = new HashSet<String>();

		types.add("tf_binding_site");
		types.add("protein");

		IGNORABLE_TYPES = Collections.<String>unmodifiableSet(types);
	}

	/** Contains a list of parent ids which have been ignored */
	private Set<String> bad_parents = new HashSet<String>();

	public GFF3Parser() {
	}

	/**
	 *  Parses GFF3 format and adds annotations to the appropriate seqs on the
	 *  given seq group.
	 */
	public List<? extends SeqSymmetry> parse(InputStream istr, String default_source, AnnotatedSeqGroup seq_group)
		throws IOException {
			BufferedReader br = new BufferedReader(new InputStreamReader(istr));

			// Note that the parse(BufferedReader) method will call br.close(), so
			// don't worry about it.
			return parse(br, default_source, seq_group);
		}

	/**
	 *  Parses GFF3 format and adds annotations to the appropriate seqs on the
	 *  given seq group.
	 */
	public List<? extends SeqSymmetry> parse(BufferedReader br, String default_source, AnnotatedSeqGroup seq_group)
		throws IOException {
			if (DEBUG) { System.out.println("starting GFF3 parse."); }

			int line_count = 0;

			List<GFF3Sym> results = new ArrayList<GFF3Sym>();

			String line = null;

			Map<String,GFF3Sym> id2sym = new HashMap<String,GFF3Sym>();
			ArrayList<GFF3Sym> all_syms = new ArrayList<GFF3Sym>();
			String track_name = null;

			try {
				Thread thread = Thread.currentThread();
				while ((! thread.isInterrupted()) && ((line = br.readLine()) != null)) {
					if (line == null) { continue; }
					if ("###".equals(line)) {
						// This directive signals that we can process all parent-child relationships up to this point.
						// But there is not much benefit in doing so.
						continue;
					}
					if ("##FASTA".equals(line)) {
						break;
					}
					if (line.startsWith("##track")) {
						track_line_parser.parseTrackLine(line);
						TrackLineParser.createAnnotStyle(seq_group, track_line_parser.getCurrentTrackHash(), default_source);
						track_name = track_line_parser.getCurrentTrackHash().get(TrackLineParser.NAME);
						continue;
					}
					if (line.startsWith("##")) { processDirective(line); continue; }
					if (line.startsWith("#")) { continue; }
					String fields[] = line_regex.split(line);

					if (fields==null || fields.length < 8) { continue; }

					line_count++;
					if (DEBUG && (line_count % 10000) == 0) { System.out.println("" + line_count + " lines processed"); }

					String seq_name = fields[0].intern();
					String source;
					if (".".equals(fields[1])) {
						source = default_source;
					} else {
						source = fields[1].intern();
					}
					String feature_type = GFF3Sym.normalizeFeatureType(fields[2]);
					int coord_a = Integer.parseInt(fields[3]);
					int coord_b = Integer.parseInt(fields[4]);
					String score_str = fields[5];
					char strand_char = fields[6].charAt(0);
					char frame_char = fields[7].charAt(0);
					String attributes_field = null;
					// last_field is "attributes" in both GFF2 and GFF3, but uses different format.
					if (fields.length>=9) { attributes_field = new String(fields[8]); } // creating a new String saves memory

					float score = GFF3Sym.UNKNOWN_SCORE;
					if (! ".".equals(score_str)) { score = Float.parseFloat(score_str); }

					/* 
					 * Found a chromosome in the file.  Do an addSeq and set the
					 * length because we can.  Also, break out of this iteration
					 * of the loop: we do not want to create an annotation for
					 * the chromosome.
					 */
					if (GFF3Sym.FEATURE_TYPE_CHROMOSOME.equals(feature_type)) {
						seq_group.addSeq(seq_name, coord_b);
						continue;
					}

					if (IGNORABLE_TYPES.contains(feature_type.toLowerCase())) {
						String[] ids = GFF3Sym.getGFF3PropertyFromAttributes(GFF3_ID, attributes_field);
						if (ids.length > 0) {
							bad_parents.add(ids[0]);
						}
						synchronized (seenTypes) {
							if (seenTypes.add(feature_type.toLowerCase())) {
								System.out.println("Ignoring GFF3 type '" + feature_type + "'");
							}
						}
						continue;
					}

					MutableAnnotatedBioSeq seq = seq_group.getSeq(seq_name);
					if (seq == null) {
						seq = seq_group.addSeq(seq_name, 0);
					}

					/* Subtract 1 from min, translating 1-base to interbase */
					final int min = Math.min(coord_a, coord_b) - 1;
					final int max   = Math.max(coord_a, coord_b);

					if (max > seq.getLength()) { seq.setLength(max); }

					SimpleSeqSpan span = new SimpleSeqSpan(
							strand_char != '-' ? min : max,
							strand_char != '-' ? max : min,
							seq);

					/*
					   From GFF3 spec:
					   The ID attributes are only mandatory for those features that have children,
					   or for those that span multiple lines.  The IDs do not have meaning outside
					   the file in which they reside.
					 */
					String the_id = GFF3Sym.getIdFromGFF3Attributes(attributes_field);
					GFF3Sym old_sym = id2sym.get(the_id);
					if (the_id == null || the_id.equals("null") || "-".equals(the_id)) {
						GFF3Sym sym = createSym(source, feature_type, score, frame_char, attributes_field, span, track_name);
						all_syms.add(sym);
					} else if (old_sym == null) {
						GFF3Sym sym = createSym(source, feature_type, score, frame_char, attributes_field, span, track_name);
						all_syms.add(sym);
						id2sym.put(the_id, sym);
					} else {
						old_sym.addSpan(span);
					}
				}
			} finally {
				br.close();
			}

			for (GFF3Sym sym : all_syms) {
				String[] parent_ids = GFF3Sym.getGFF3PropertyFromAttributes(GFF3_PARENT, sym.getAttributes());

				String id = sym.getID();
				if (id != null && ! "-".equals(id)) {
					seq_group.addToIndex(id, sym);
				}

				if (parent_ids.length == 0) {
					// If no parents, then it is top-level
					results.add(sym);
					/* Do we really need to do for every span? */
					for(int i=0; i<sym.getSpanCount(); i++) {
						MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq) sym.getSpanSeq(i);
						seq.addAnnotation(sym);
					}
				} else {
					// Else, add this as a child to *each* parent in its parent list.
					// It is an error if the parent doesn't exist.
					for (int i=0; i<parent_ids.length; i++) {
						String parent_id = parent_ids[i];
						if ("-".equals(parent_id)) {
							throw new IOException("Parent ID cannot be '-'");
						}
						GFF3Sym parent_sym = id2sym.get(parent_id);
						if (bad_parents.contains(parent_id)) {
							/*
							 * bad_parents list contains ignored parents.  Child
							 * ids are added to the bad_parents list since we are
							 * ignoring them too.
							 */
							String[] ids = GFF3Sym.getGFF3PropertyFromAttributes(GFF3_ID, sym.getAttributes());
							if (ids.length > 0) {
								bad_parents.add(ids[0]);
							}
						} else if (parent_sym == null) {
							throw new IOException("No parent found with ID: " + parent_id);
						} else if (parent_sym == sym) {
							throw new IOException("Parent and child are the same for ID: " + parent_id);
						} else {
							parent_sym.addChild(sym);
						}
					}
				}
			}
			// hashtable no longer needed
			id2sym.clear();

			System.out.print("Finished parsing GFF3.");
			System.out.print("  line count: " + line_count);
			System.out.println("  result count: " + results.size());
			return results;
		}

	/**
	 *  Process directive lines in the input, which are lines beginning with "##".
	 *  Directives that are not understood are treated as comments.
	 *  Directives that are understood include "##gff-version", which must match "3".
	 */
	public void processDirective(String line) throws IOException {
		Matcher m = directive_version.matcher(line);
		if (m.matches()) {
			String vstr = m.group(1).trim();
			if (! "3".equals(vstr)) {
				throw new IOException("The specified GFF version can not be processed by this parser: version = '" + vstr + "'");
			}
			return;
		}
	}

	private static GFF3Sym createSym(String source, String feature_type, float score, char frame_char, String attributes_field, SimpleSeqSpan span, String track_name) {
		GFF3Sym sym = new GFF3Sym(source, feature_type, score, frame_char, attributes_field);
		sym.addSpan(span);
		if (use_track_lines && track_name != null) {
			sym.setProperty("method", track_name);
		} else {
			sym.setProperty("method", source);
		}
		return sym;
	}
}
