/**
 * Copyright (c) 2006-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometry.symmetry.impl;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableSeqSpan;
import com.affymetrix.genometry.Scored;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SupportsCdsSpan;
import com.affymetrix.genometry.parsers.GFF3Parser;
import static com.affymetrix.genometry.parsers.GFF3Parser.GFF_PROPS_TO_FILTER;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.symmetry.SymSpanWithCds;
import com.affymetrix.genometry.tooltip.ToolTipConstants;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.FEATURE_TYPE;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.FRAME;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.ID;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.METHOD;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.SCORE;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.SOURCE;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.TYPE;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.SeqUtils;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A sym to efficiently store GFF version 3 annotations.
 *
 * See http://song.sourceforge.net/gff3.shtml
 *
 * @version $Id: GFF3Sym.java 7644 2011-03-03 18:36:35Z lfrohman $
 */
public final class GFF3Sym extends SimpleSymWithProps implements Scored, SupportsCdsSpan, SymSpanWithCds {

    private String id;
    private static boolean multipleCdsWarning = false;

    public static final char UNKNOWN_FRAME = UcscGffSym.UNKNOWN_FRAME;
    public static final String UNKNOWN_SOURCE = ".";

    // Assuming that these feature types are not case-sensitive
    public static final String FEATURE_TYPE_GENE = "gene";
    public static final String FEATURE_TYPE_MRNA = "mrna";
    public static final String FEATURE_TYPE_EXON = "exon";
    public static final String FEATURE_TYPE_CDS = "cds";
    public static final String FEATURE_TYPE_CHROMOSOME = "chromosome";

    // Assuming that these ontology types are not case-sensitive
    public static final String SOFA_GENE = "SO:0000704";
    public static final String SOFA_MRNA = "SO:0000234";
    public static final String SOFA_EXON = "SO:0000147";
    public static final String SOFA_CDS = "SO:0000316";

    private static final Pattern equalsP = Pattern.compile("=");
    private static final Pattern commaP = Pattern.compile(",");

    private static final List<String> bad_prop_names = Arrays.asList(FEATURE_TYPE, TYPE, SCORE, FRAME);

    private String source;
    private String method;
    private String feature_type;
    private MutableSeqSpan cdsSpan;
    private BioSeq seq; // "chrom"
    private int min; // "chromStart"
    private int max; // "chromEnd"
    private final float score;
    private final char frame;
    private final String attributes;

    /**
     * Constructor. The coordinates should be given exactly as they appear in a
     * GFF file. In principle, the first coordinate is supposed to be less than
     * the second one, but in practice this isn't always followed, so this
     * constructor will correct those errors and will also convert from base-1
     * to interbase-0 coordinates.
     *
     * @param source
     * @param feature_type
     * @param score
     * @param frame
     * @param attributes Attributes, formatted in GFF3 style.
     */
    public GFF3Sym(String source, String feature_type,
            float score, char frame, String attributes) {
        super();

        if (!UNKNOWN_SOURCE.equals(source)) {
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
        List<String> possible_names = getGFF3PropertyFromAttributes(GFF3Parser.GFF3_NAME, attributes);
        if (possible_names.size() > 0) {
            id = possible_names.get(0);
        } else {
            id = null;
        }
    }

    @Override
    public String getID() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getFeatureType() {
        return feature_type;
    }

    public float getScore() {
        return score;
    }

    public char getFrame() {
        return frame;
    }

    public String getAttributes() {
        return attributes;
    }

    @Override
    public Object getProperty(String name) {
        if (name.equals(ToolTipConstants.SOURCE) && source != null) {
            return source;
        } else if (name.equals(ToolTipConstants.METHOD)) {
            return method;
        } else if (name.equals(ToolTipConstants.FEATURE_TYPE) || name.equals(ToolTipConstants.TYPE)) {
            return feature_type;
        } else if (name.equals(ToolTipConstants.SCORE) && score != UNKNOWN_SCORE) {
            return score;
        } else if (name.equals(FRAME) && frame != UNKNOWN_FRAME) {
            return frame;
        } else if (name.equals(ToolTipConstants.ID)) {
            return getID();
        }
        List<String> temp = getGFF3PropertyFromAttributes(name, attributes);
        if (temp.isEmpty()) {
            return null;
        } else if (temp.size() == 1) {
            return temp.get(0);
        } else {
            return temp;
        }
    }

    /**
     * Overridden such that certain properties will be stored more efficiently.
     * Setting certain properties this way is not supported: these include
     * "attributes", SCORE and FRAME.
     */
    @Override
    public boolean setProperty(String name, Object val) {
        String lc_name = name.toLowerCase();
        if (name.equals(ID)) {
            if (val instanceof String) {
                id = (String) val;
                return true;
            } else {
                //id = null;
                return false;
            }
        }
        if (name.equals(SOURCE)) {
            if (val instanceof String) {
                source = (String) val;
                return true;
            } else {
                //source = null;
                return false;
            }
        }
        if (name.equals(METHOD)) {
            if (val instanceof String) {
                method = (String) val;
                return true;
            } else {
                //method = null;
                return false;
            }
        } else if (bad_prop_names.contains(lc_name)) {
            // May need to handle these later, but it is unlikely to be an issue
            throw new IllegalArgumentException("Currently can't modify property '" + name + "' via setProperty");
        }

        return super.setProperty(name, val);
    }

    @Override
    public Map<String, Object> getProperties() {
        return cloneProperties();
    }

    @Override
    public Map<String, Object> cloneProperties() {
        Map<String, Object> tprops = super.cloneProperties();
        if (tprops == null) {
            tprops = new HashMap<>();
        }
        if (getID() != null) {
            tprops.put(ID, getID());
        }
        if (source != null) {
            tprops.put(SOURCE, source);
        }
        if (method != null) {
            tprops.put(METHOD, method);
        }
        if (feature_type != null) {
            tprops.put(FEATURE_TYPE, feature_type);
            tprops.put(TYPE, feature_type);
        }
        if (score != UNKNOWN_SCORE) {
            tprops.put(SCORE, getScore());
        }
        if (frame != UNKNOWN_FRAME) {
            tprops.put(FRAME, frame);
        }
        addAllAttributesFromGFF3(tprops, attributes);

        return tprops.entrySet().stream().filter(entry -> !GFF_PROPS_TO_FILTER.contains(entry.getKey())).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
    }

    /**
     * Returns the property GFF3Parser.GFF3_ID from the attributes. This will be
     * a single String or null. This ID is intended to be used during processing
     * of the GFF3 file, and has no meaning outside the file.
     */
    public static String getIdFromGFF3Attributes(String attributes) {
        List<String> possibleIds = getGFF3PropertyFromAttributes(GFF3Parser.GFF3_ID, attributes);
        if (possibleIds.isEmpty()) {
            return null;
        } else {
            return possibleIds.get(0);
        }
    }

    private static void addAllAttributesFromGFF3(Map<String, Object> m, String attributes) {
        if (attributes == null) {
            return;
        }

        String[] tag_vals = attributes.split(";");

        for (String tag_val : tag_vals) {
            if ("".equals(tag_val)) {
                continue;
            }
            String[] tag_and_vals = equalsP.split(tag_val, 2);
            if (tag_and_vals.length == 2) {
                String[] vals = commaP.split(tag_and_vals[1]);
                for (int j = 0; j < vals.length; j++) {
                    vals[j] = GeneralUtils.URLDecode(vals[j]);
                }
                if (vals.length == 1) { // put a single String
                    m.put(tag_and_vals[0], vals[0]);
                } else { // put a String array
                    m.put(tag_and_vals[0], vals);
                }
            }
        }
    }

    /**
     * Returns a non-null String[].
     */
    public static List<String> getGFF3PropertyFromAttributes(String propName, String attributes) {
        List<String> results = Lists.newArrayList();
        if (attributes == null) {
            return results;
        }
        String[] tagVals = attributes.split(";");
        String propWithEquals = propName + "=";
        String val = null;

        for (String tag_val : tagVals) {
            if (tag_val.startsWith(propWithEquals)) {
                val = tag_val.substring(propWithEquals.length());
                break;
            }
        }
        if (val == null) {
            return results;
        }
        results.addAll(Lists.newArrayList(val.split(",")));
        results = results.stream().map(prop -> GeneralUtils.URLDecode(prop)).collect(Collectors.toList());
        return results;
    }

    /**
     * Converts feature types that IGB understands into one of the constant
     * strings: {@link #FEATURE_TYPE_GENE}, etc. Invalid ones, are simply
     * interned.
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
        if (FEATURE_TYPE_CHROMOSOME.equalsIgnoreCase(s)) {
            return FEATURE_TYPE_CHROMOSOME;
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

    @Override
    public String toString() {
        return "GFF3Sym: ID = '" + getProperty(GFF3Parser.GFF3_ID) + "'  type=" + feature_type
                + " children=" + getChildCount();
    }

    private static boolean isCdsSym(SeqSymmetry sym) {
        return sym instanceof GFF3Sym
                && ((GFF3Sym) sym).getFeatureType().equals(GFF3Sym.FEATURE_TYPE_CDS);
    }

    @Override
    public boolean hasCdsSpan() {
        return cdsSpan != null;
    }

    @Override
    public SeqSpan getCdsSpan() {
        return cdsSpan;
    }

    @Override
    public void addChild(SeqSymmetry sym) {
        if (isCdsSym(sym)) {
            for (int i = 0; i < sym.getSpanCount(); i++) {
                SeqSpan span = sym.getSpan(i);
                if (cdsSpan == null) {
                    cdsSpan = new SimpleMutableSeqSpan(span);
                } else {
                    SeqUtils.encompass(span, cdsSpan, cdsSpan);
                }
            }
        } else {
            super.addChild(sym);
        }
    }

    @Override
    public boolean isCdsStartStopSame() {
        return cdsSpan != null && cdsSpan.getStart() == cdsSpan.getEnd();
    }

    @Override
    public boolean isForward() {
        return this.getSpan(0) != null && this.getSpan(0).isForward();
    }

    @Override
    public BioSeq getBioSeq() {
        return this.getSpan(0) != null ? this.getSpan(0).getBioSeq() : null;
    }

//	public boolean hasCdsSpan() {
//		for(SeqSymmetry child : children) {
//			if (isCdsSym(child)) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	/**
//	 * TODO: this does not take into account multiple CDS for a single mRNA nor
//	 *       does it make use of the 5' and 3' UTR or multiple CDS regions on a
//	 *       single mRNA.
//	 *
//	 * TODO: Most of this should be precomputed in the addChild() or something
//	 *       so we do not need to compute it every time it is requested.
//	 *
//	 * @return A single SeqSpan covering the CDS region.
//	 */
//	public SeqSpan getCdsSpan() {
//		/* This can be null but Maps can store null keys */
//		String gff3ID;
//		Map<String,MutableSeqSpan> cdsSpans = new LinkedHashMap<String,MutableSeqSpan>();
//		MutableSeqSpan span = null;
//
//		for(SeqSymmetry child : children) {
//			if (isCdsSym(child)) {
//				gff3ID = getIdFromGFF3Attributes(((GFF3Sym)child).getAttributes());
//				for(int i = 0; i < child.getSpanCount(); i++) {
//					span = cdsSpans.get(gff3ID);
//					if (span == null) {
//						span = new SimpleMutableSeqSpan(child.getSpan(i));
//						cdsSpans.put(gff3ID, span);
//					} else {
//						SeqUtils.encompass(child.getSpan(i), span, span);
//					}
//				}
//			}
//		}
//
//		if (cdsSpans.isEmpty()) {
//			throw new IllegalArgumentException("This Symmetry does not have a CDS");
//		} else if (cdsSpans.size() > 1){
//			Logger.getLogger(
//					this.getClass().getName()).log(Level.WARNING,
//					"Multiple CDS spans detected.  Skipping remaining CDS spans.  (found {0} spans for {1})",
//					new Object[] {Integer.valueOf(cdsSpans.size()),
//					getIdFromGFF3Attributes(attributes)});
//
//			if (!multipleCdsWarning) {
//				multipleCdsWarning = !multipleCdsWarning;
//				SwingUtilities.invokeLater(new Runnable() {
//					public void run() {
//						/* TODO: This should use StringUtils.wrap() */
//						JOptionPane.showMessageDialog(null,
//								"Multiple CDS regions for a shared parent have been\ndetected in a GFF3 file.  Only the first CDS region\nencountered will be displayed.  This is a known\nlimitation of the GFF3 parser.",
//								"Multiple CDS Regions Detected",
//								JOptionPane.WARNING_MESSAGE);
//					}
//				});
//			}
//		}
//
//		return cdsSpans.entrySet().iterator().next().getValue();
//	}
//
//	/*
//	 *Returns Map of id to list of symmetries of cds spans.
//	 */
//	public Map<String, List<SeqSymmetry>> getCdsSpans() {
//		String gff3ID;
//		Map<String, List<SeqSymmetry>> cdsSpans = new LinkedHashMap<String, List<SeqSymmetry>>();
////		MutableSeqSpan span = null;
//
//		for (SeqSymmetry child : children) {
//			if (isCdsSym(child)) {
//				gff3ID = getIdFromGFF3Attributes(((GFF3Sym) child).getAttributes());
//				for (int i = 0; i < child.getSpanCount(); i++) {
//					List<SeqSymmetry> list = cdsSpans.get(gff3ID);
//					if (list == null) {
//						list = new ArrayList<SeqSymmetry>();
//						cdsSpans.put(gff3ID, list);
//						list.add(child);
//					}
//				}
//			}
//		}
//		return cdsSpans;
//	}
//
//	/**
//	 * Removes all cds symmetries.
//	 */
//	public void removeCdsSpans(){
//		List<SeqSymmetry> remove_list = new ArrayList<SeqSymmetry>();
//		for(SeqSymmetry child : children) {
//			if (isCdsSym(child)) {
//				remove_list.add(child);
//			}
//		}
//		children.removeAll(remove_list);
//	}
//
//	@Override
//	public Object clone() {
//		GFF3Sym dup = new GFF3Sym(this.source, this.feature_type, this.score, this.frame, this.attributes);
//		if (children != null) {
//			for (SeqSymmetry child : children) {
//				dup.addChild(child);
//			}
//		}
//		if (spans != null) {
//			for (SeqSpan span : spans) {
//				dup.addSpan(span);
//			}
//		}
//		dup.props = this.cloneProperties();
//		dup.method = this.method;
//
//		return dup;
//	}
}
