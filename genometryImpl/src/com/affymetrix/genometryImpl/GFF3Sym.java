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
package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.UcscGffSym;
import com.affymetrix.genometryImpl.SingletonSymWithProps;
import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.parsers.GFF3Parser;
import com.affymetrix.genometry.*;

import java.net.URLDecoder;
import java.util.*;
import java.util.regex.*;

/**
 *  A sym to efficiently store GFF version 3 annotations.
 *
 *  See http://song.sourceforge.net/gff3.shtml
 */
public class GFF3Sym extends SingletonSymWithProps implements Scored {

	public static final char UNKNOWN_FRAME = UcscGffSym.UNKNOWN_FRAME;
	public static final String UNKNOWN_SOURCE = ".";

	// Assuming that these feature types are not case-sensitive
	public static final String FEATURE_TYPE_GENE = "gene";
	public static final String FEATURE_TYPE_MRNA = "mrna";
	public static final String FEATURE_TYPE_EXON = "exon";
	public static final String FEATURE_TYPE_CDS = "cds";

	// Assuming that these ontology types are not case-sensitive
	public static final String SOFA_GENE = "SO:0000704";
	public static final String SOFA_MRNA = "SO:0000234";
	public static final String SOFA_EXON = "SO:0000147";
	public static final String SOFA_CDS = "SO:0000316";

	String source;
	String method;
	public String feature_type;
	float score;
	char frame;
	String attributes;

	/**
	 * Constructor.
	 * The coordinates should be given exactly as they appear in a GFF file.
	 * In principle, the first coordinate is supposed to be less than the second one,
	 * but in practice this isn't always followed, so this constructor will correct
	 * those errors and will also convert from base-1 to interbase-0 coordinates.
	 * @param a  The coordinate in column 4 of the GFF file.
	 * @param b  The coordinate in column 5 of the GFF file.
	 * @param attributes   Attributes, formatted in GFF3 style.
	 */
	public GFF3Sym(BioSeq seq, String source, String feature_type, int a, int b,
			float score, char strand, char frame, String attributes) {
		super(0, 0, seq);

		// GFF spec says coord_A <= coord_B, but this is not always obeyed
		int max = Math.max(a, b);
		int min = Math.min(a, b);
		// convert from base-1 numbering to interbase-0 numbering
		min--;

		if (strand == '-') {
			setCoords(max, min);
		} else {
			setCoords(min, max);
		}

		if (! UNKNOWN_SOURCE.equals(source)) {
			this.source = source;
		} else {
			this.source = UNKNOWN_SOURCE;  // Basically equivalent to this.source = source.intern()
		}
		this.method = null;
		this.feature_type = feature_type;
		this.score = score;
		this.frame = frame;
		this.attributes = attributes;

		// in GFF3, the property "ID" is intended to have meaning only inside the file itself.
		// the property "Name" is more like what we think of as an ID in Genometry
		String[] possible_names = getGFF3PropertyFromAttributes(GFF3Parser.GFF3_NAME, attributes);
		if (possible_names.length > 0) {
			this.id = possible_names[0];
		} else {
			this.id = null;
		}
	}

	/**
	 * Return the ID.  Unknown if the original contract allows null to be
	 * returned, but this class and users of this class assume it can
	 * return null.
	 *
	 * @return ID or null
	 */
	@Override
		public String getID() {
			// This is overridden because we only want to check the value of this.id,
			// we do NOT want to check for a property named "id".  This is because GFF3
			// has a very different notion of what an ID is.  In GFF3 and "ID", in upper case,
			// only has meaning while processing the file and should be ignored later.
			if (this.id == null) {
				return null;
			} else {
				return this.id.toString();
			}
		}
	public String getSource()  { return source; }
	public String getFeatureType()  { return feature_type; }
	public float getScore()  { return score; }
	public char getFrame()  { return frame; }
	public String getAttributes() { return attributes; }

	public Object getProperty(String name) {
		if (name.equals("source") && source != null) { return source; }
		else if (name.equals("method")) { return method; }
		else if (name.equals("feature_type") || name.equals("type")) { return feature_type; }
		else if (name.equals("score") && score != UNKNOWN_SCORE) { return new Float(score); }
		else if (name.equals("frame") && frame != UNKNOWN_FRAME) { return new Character(frame); }
		else if (name.equals("id")) {
			return getID();
		}
		String[] temp = getGFF3PropertyFromAttributes(name, attributes);
		if (temp.length == 0) {
			return null;
		} else if (temp.length == 1) {
			return temp[0];
		} else {
			return temp;
		}
	}

	static final List bad_prop_names = Arrays.asList(new String[] {
		"feature_type", "type", "score", "frame"
	});

	/**
	 *  Overriden such that certain properties will be stored more efficiently.
	 *  Setting certain properties this way is not supported:
	 *  these include "attributes", "score" and "frame".
	 */
	public boolean setProperty(String name, Object val) {
		String lc_name = name.toLowerCase();
		if (name.equals("id")) {
			if (val instanceof String) {
				id = (String) val;
				return true;
			}
			else {
				//id = null;
				return false;
			}
		}
		if (name.equals("source")) {
			if (val instanceof String) {
				source = (String) val;
				return true;
			}
			else {
				//source = null;
				return false;
			}
		}
		if (name.equals("method")) {
			if (val instanceof String) {
				method = (String) val;
				return true;
			}
			else {
				//method = null;
				return false;
			}
		}
		else if (bad_prop_names.contains(lc_name)) {
			// May need to handle these later, but it is unlikely to be an issue
			throw new IllegalArgumentException("Currently can't modify property '" + name +"' via setProperty");
		}

		return super.setProperty(name, val);
	}

	public Map<String,Object> getProperties() {
		return cloneProperties();
	}

	public Map<String,Object> cloneProperties() {
		Map<String,Object> tprops = super.cloneProperties();
		if (tprops == null) {
			tprops = new HashMap<String,Object>();
		}
		if (getID() != null) {
			tprops.put("id", getID());
		}
		if (source != null) {
			tprops.put("source", source);
		}
		if (method != null) {
			tprops.put("method", method);
		}
		if (feature_type != null) {
			tprops.put("feature_type", feature_type);
			tprops.put("type", feature_type);
		}
		if (score != UNKNOWN_SCORE) {
			tprops.put("score", new Float(getScore()));
		}
		if (frame != UNKNOWN_FRAME) {
			tprops.put("frame", new Character(frame));
		}
		addAllAttributesFromGFF3(tprops, attributes);

		return tprops;
	}

	/** Returns the property GFF3Parser.GFF3_ID from the attributes.
	 *  This will be a single String or null.  This ID is intended to be used
	 *  during processing of the GFF3 file, and has no meaning outside the file.
	 */
	public static String getIdFromGFF3Attributes(String attributes) {
		String[] possible_ids = getGFF3PropertyFromAttributes(GFF3Parser.GFF3_ID, attributes);
		if (possible_ids.length == 0) {
			return null;
		} else {
			return possible_ids[0];
		}
	}

	static final Pattern equalsP = Pattern.compile("=");
	static final Pattern commaP = Pattern.compile(",");

	public static void addAllAttributesFromGFF3(Map<String,Object> m, String attributes) {
		if (attributes == null) {
			return;
		}

		String[] tag_vals = attributes.split(";");

		for (int i=0; i<tag_vals.length; i++) {
			if ("".equals(tag_vals[i])) {
				continue;
			}
			String[] tag_and_vals = equalsP.split(tag_vals[i], 2);
			if (tag_and_vals.length == 2) {
				String[] vals = commaP.split(tag_and_vals[1]);
				for (int j=0; j<vals.length; j++) {
					vals[j] = URLDecoder.decode(vals[j]);
				}
				if (vals.length == 1) { // put a single String
					m.put(tag_and_vals[0], vals[0]);
				} else { // put a String array
					m.put(tag_and_vals[0], vals);
				}
			}
		}
	}

	static final String[] EMPTY_RESULT = new String[0];

	/** Returns a non-null String[]. */
	public static String[] getGFF3PropertyFromAttributes(String prop_name, String attributes) {
		if (attributes == null) {
			return EMPTY_RESULT;
		}
		String[] tag_vals = attributes.split(";");
		String prop_with_equals = prop_name + "=";
		String val = null;

		for (int i=0; i<tag_vals.length; i++) {
			if (tag_vals[i].startsWith(prop_with_equals)) {
				val = tag_vals[i].substring(prop_with_equals.length());
				break;
			}
		}
		if (val == null) {
			return EMPTY_RESULT;
		}
		String[] results = val.split(",");
		for (int i=0; i<results.length; i++) {
			results[i] = URLDecoder.decode(results[i]);
		}
		return results;
	}

	/**
	 *  Converts feature types that IGB understands into one of the constant strings:
	 *  {@link #FEATURE_TYPE_GENE}, etc.  Invalid ones, are simply interned.
	 */
	public static String normalizeFeatureType(String s) {

		if (FEATURE_TYPE_GENE.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_GENE;
		}
		if (FEATURE_TYPE_EXON.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_EXON;
		}
		if (FEATURE_TYPE_MRNA.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_MRNA;
		}
		if (FEATURE_TYPE_CDS.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_CDS;
		}

		if (SOFA_GENE.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_GENE;
		}
		if (SOFA_EXON.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_EXON;
		}
		if (SOFA_MRNA.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_MRNA;
		}
		if (SOFA_CDS.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_CDS;
		}

		return s.intern();
	}

	public String toString() {
		return "GFF3Sym: ID = '" + getProperty(GFF3Parser.GFF3_ID) + "'  type=" + feature_type
			+ " children=" + getChildCount();
	}

	public boolean isMultiLine() {
		return false;
	}

	public static final class MultiLineGFF3Sym extends GFF3Sym {
		public MultiLineGFF3Sym(BioSeq seq, String source, String feature_type, int a, int b,
				float score, char strand, char frame, String attributes) {
			super(seq, source, feature_type, a, b, score, strand, frame, attributes);
		}

		public boolean isMultiLine() {
			return true;
		}
	}
}
