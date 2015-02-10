/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view;

import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SupportsCdsSpan;
import com.affymetrix.genometry.event.PropertyHolder;
import com.affymetrix.genometry.symmetry.impl.CdsSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.CDS_END;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.CDS_START;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.CHROMOSOME;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.END;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.LENGTH;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.METHOD;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.SEQ_ID;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.START;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.STRAND;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.TYPE;
import com.affymetrix.genoviz.bioviews.GlyphI;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tarun
 */
public class SeqMapViewPropertyHolderImpl implements PropertyHolder {
    
    SeqMapView seqMapView;

    public SeqMapViewPropertyHolderImpl(SeqMapView seqMapView) {
        this.seqMapView = seqMapView;
    }
    
    @Override
    public List<Map<String, Object>> getProperties() {
        List<Map<String, Object>> propList = new ArrayList<>();
        List<SeqSymmetry> selected_syms = seqMapView.getSelectedSyms();
        for (GlyphI glyph : seqMapView.getSeqMap().getSelected()) {

            if (glyph.getInfo() instanceof SeqSymmetry
                    && selected_syms.contains(glyph.getInfo())) {
                continue;
            }

            Map<String, Object> props = null;
            if (glyph.getInfo() instanceof Map) {
                props = (Map<String, Object>) glyph.getInfo();
            } else {
                props = new HashMap<>();
            }

            boolean direction = true;
            if (props.containsKey("direction")) {
                if (props.get("direction").equals("reverse")) {
                    direction = false;
                }
            }

            Rectangle2D.Double boundary = glyph.getSelectedRegion();
            int start = (int) boundary.getX();
            int length = (int) boundary.getWidth();
            int end = start + length;
            if (!direction) {
                int temp = start;
                start = end;
                end = temp;
            }
            props.put(START, start);
            props.put(END, end);
            props.put(LENGTH, length);

            propList.add(props);
        }
        propList.addAll(seqMapView.getTierManager().getProperties());
        return propList;
    }

    @Override
    public Map<String, Object> determineProps(SeqSymmetry sym) {
        
        Map<String, Object> props = new HashMap<>();
        if (sym == null) {
            return props;
        }
        Map<String, Object> tierprops = seqMapView.getTierManager().determineProps(sym);
        if (tierprops != null) {
            props.putAll(tierprops);
        }
        SeqSpan span = seqMapView.getViewSeqSpan(sym);
        if (span != null) {
            String chromID = span.getBioSeq().getID();
            props.put(CHROMOSOME, chromID);
            props.put(START,
                    NumberFormat.getIntegerInstance().format(span.getStart()));
            props.put(END,
                    NumberFormat.getIntegerInstance().format(span.getEnd()));
            props.put(LENGTH,
                    NumberFormat.getIntegerInstance().format(span.getLength()));
            props.put(STRAND,
                    span.isForward() ? "+" : "-");
            props.remove(SEQ_ID); // this is redundant if "chromosome" property is set
            if (props.containsKey(METHOD)) {
                props.remove(METHOD);
            }
            if (props.containsKey(TYPE)) {
                props.remove(TYPE);
            }
        }
        if (sym instanceof CdsSeqSymmetry) {
            sym = ((CdsSeqSymmetry) sym).getPropertySymmetry();
        }
        if (sym instanceof SupportsCdsSpan) {
            span = ((SupportsCdsSpan) sym).getCdsSpan();
            if (span != null) {
                props.put(CDS_START,
                        NumberFormat.getIntegerInstance().format(span.getStart()));
                props.put(CDS_END,
                        NumberFormat.getIntegerInstance().format(span.getEnd()));

            }
        }
        return props;
    
    }
    
}
