package com.lorainelab.das.parser;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableSeqSpan;
import com.affymetrix.genometry.Scored;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SupportsCdsSpan;
import com.lorainelab.das.parser.DASFeatureParser.Orientation;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometry.symmetry.TypedSym;
import com.affymetrix.genometry.util.SeqUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author sgblanch
 * @version $Id: DASSymmetry.java 9348 2011-11-02 21:42:40Z lfrohman $
 */
public class DASSymmetry extends SimpleSymWithProps implements Scored, SupportsCdsSpan, TypedSym {

    private final float score;
    private final String type;

    DASSymmetry(GroupBean group, FeatureBean feature, BioSeq sequence) {
        score = Scored.UNKNOWN_SCORE;
        if (!group.getType().isEmpty()) {
            type = group.getType();
        } else if (!feature.getTypeLabel().isEmpty()) {
            type = feature.getTypeLabel();
        } else {
            type = feature.getTypeID();
        }

        this.addSpan(new SimpleMutableSeqSpan(new SimpleMutableSeqSpan(
                feature.getOrientation() == Orientation.REVERSE ? feature.getEnd() : feature.getStart(),
                feature.getOrientation() == Orientation.REVERSE ? feature.getStart() : feature.getEnd(),
                sequence)));
        this.setID(group.getID());
        this.addLinks(group.getLinks());
        this.setProperty("label", group.getLabel().isEmpty() ? group.getID() : group.getLabel());
    }

    DASSymmetry(FeatureBean feature, BioSeq sequence) {
        score = feature.getScore();
        type = feature.getTypeLabel().isEmpty() ? feature.getTypeID() : feature.getTypeLabel();
        this.addSpan(new SimpleMutableSeqSpan(
                feature.getOrientation() == Orientation.REVERSE ? feature.getEnd() : feature.getStart(),
                feature.getOrientation() == Orientation.REVERSE ? feature.getStart() : feature.getEnd(),
                sequence));
        this.setID(feature.getID());
        this.addLinks(feature.getLinks());
        this.setProperty("label", feature.getLabel().isEmpty() ? feature.getID() : feature.getLabel());
    }

    /**
     * Add a child SeqSymmetry to this SeqSymmetry. This method will force the
     * first SeqSpan on this SeqSymmetry to encompass the first SeqSpan of the
     * child SeqSymmetry. This is necessary for the graphics code to render
     * this SeqSymmetry correctly.
     *
     * @param child
     */
    @Override
    public void addChild(SeqSymmetry child) {
        super.addChild(child);

        if (child.getSpanCount() > 0 && this.getSpanCount() > 0 && this.getSpan(0) instanceof MutableSeqSpan) {
            SeqUtils.encompass(child.getSpan(0), this.getSpan(0), (MutableSeqSpan) this.getSpan(0));
        }
    }

    public float getScore() {
        return score;
    }

    public boolean hasCdsSpan() {
        for (SeqSymmetry child : children) {
            if (isCdsSym(child)) {
                return true;
            }
        }
        return false;
    }

    public SeqSpan getCdsSpan() {
        MutableSeqSpan span = null;

        for (SeqSymmetry child : children) {
            if (isCdsSym(child)) {
                for (int i = 0; i < child.getSpanCount(); i++) {
                    if (span == null) {
                        span = new SimpleMutableSeqSpan(child.getSpan(i));
                    } else {
                        SeqUtils.encompass(child.getSpan(i), span, span);
                    }
                }
            }
        }

//		if (span == null) {
//			throw new IllegalArgumentException("This Symmetry does not have a CDS");
//		}
        return span;
    }

    private boolean isCdsSym(SeqSymmetry sym) {
        return sym instanceof TypedSym && ((TypedSym) sym).getType().startsWith("exon:coding");
    }

    public String getType() {
        return type;
    }

    private void addLinks(List<LinkBean> links) {
        String url, title;

        if (links.size() == 1) {
            url = links.get(0).getURL();
            title = links.get(0).getTitle();
            title = !title.isEmpty() ? title : url;
            this.setProperty("link", url);
            this.setProperty("link_name", title == null ? url : title);
        } else if (links.size() > 1) {
            Map<String, String> linkMap = new HashMap<>();
            for (LinkBean linkBean : links) {
                url = linkBean.getURL();
                title = linkBean.getTitle();
                title = !title.isEmpty() ? title : url;
                if (url != null && !url.isEmpty()) {
                    linkMap.put(title, url);
                }
            }
            this.setProperty("link", linkMap);
        }
    }
}
