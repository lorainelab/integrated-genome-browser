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
package com.affymetrix.igb.das2;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import java.net.*;

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;

public final class Das2Region {

    static public boolean USE_SEGMENT = true;  // segment param, or old version with seq included in other filters
    static public boolean USE_SEGMENT_URI = true;
    static public boolean USE_TYPE_URI = true;
    static public boolean URL_ENCODE_QUERY = true;
    private URI region_uri;
    private String name;
    private int length;
    private String info_url;  // doc_href
    //  List assembly;  // or should this be a SeqSymmetry??   // or composition of SmartAnnotBioSeq??
    private SeqSpan segment_span;
    private MutableAnnotatedBioSeq aseq;
    private Das2VersionedSource versioned_source;

    public Das2Region(Das2VersionedSource source, URI reg_uri, String nm, String info, int ln) {
        region_uri = reg_uri;
        name = nm;
        info_url = info;
        length = ln;

        versioned_source = source;
        AnnotatedSeqGroup genome = versioned_source.getGenome();
        // a)  see if id of Das2Region maps directly to an already seen annotated seq in genome
        //   check for prior existence of MutableAnnotatedBioSeq for Das2Region only if genome group is _not_ a Das2SeqGroup
        //      if group is a Das2SeqGroup, then calling getSeq() will trigger infinite loop as group attempts
        //      to initialize sequences via Das2VersionedSources.getSegments()
        //   But if genome is a Das2SeqGroup, then can assume that no seqs are in group that aren't
        //      being put there in this constructor, and these will be unique, so can skip check for prior existence
        if (!(genome instanceof Das2SeqGroup)) {
            aseq = genome.getSeq(name);
            if (aseq == null) {
                aseq = genome.getSeq(this.getID());
            }
        }
        // b) if can't find a previously seen genome for this DasSource, then
        //     create a new genome entry
        if (aseq == null) {
            // using name instead of id for now
            aseq = genome.addSeq(name, length);
        }
        segment_span = new SimpleSeqSpan(0, length, aseq);
    }

    public String getID() {
        return region_uri.toString();
    }

    public String getName() {
        return name;
    }

    public Das2VersionedSource getVersionedSource() {
        return versioned_source;
    }

    // or should this return a SmartAnnotbioSeq???
    public MutableAnnotatedBioSeq getAnnotatedSeq() {
        return aseq;
    }

    /**
     *   Converts a SeqSpan to a DAS2 region String.
     *   if include_strand, then appends strand info to end of String (":1") or (":-1").
     *
     *   Need to enhance this to deal with synonyms, so if seq id is different than
     *     corresponding region id, use region id instead.  To do this, probably
     *     need to add an Das2VersionedSource argument (Das2Region would work also,
     *     but probably better to have this method figure out region based on versioned source
     */
    // Note similarities to Das2FeatureSaxParser.getRangeString.
    public String getPositionString(SeqSpan span, boolean indicate_strand) {
        if (span == null) {
            return null;
        }
        String result = null;
        MutableAnnotatedBioSeq spanseq = span.getBioSeq();
        if (this.getAnnotatedSeq() == spanseq) {
            StringBuffer buf = new StringBuffer(100);
            buf.append(this.getName());
            buf.append("/");
            buf.append(Integer.toString(span.getMin()));
            buf.append(":");
            buf.append(Integer.toString(span.getMax()));
            if (indicate_strand) {
                if (span.isForward()) {
                    buf.append(":1");
                } else {
                    buf.append(":-1");
                }
            }
            result = buf.toString();
        } else {  // this region's annotated seq is _not_ the same seq as the span argument seq
            // throw an error?
            // return null?
            // try using Das2VersionedSource.getRegion()?
        }
        return result;
    }
}
