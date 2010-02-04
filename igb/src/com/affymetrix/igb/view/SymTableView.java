package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import java.util.*;
import java.text.NumberFormat;

public final class SymTableView extends PropertySheet implements SymSelectionListener {
    public SymTableView() {
        super(true);
        setPreferredSize(new java.awt.Dimension(100, 250));
        setMinimumSize(new java.awt.Dimension(100, 250));
        GenometryModel.getGenometryModel().addSymSelectionListener(this);
    }

    public void symSelectionChanged(SymSelectionEvent evt) {
        Object src = evt.getSource();
        // if selection event originally came from here, then ignore it...
        if (src == this) {
            return;
        }
        SeqMapView mapView = null;
        if (src instanceof SeqMapView) {
            mapView = (SeqMapView) src;
        }
        showSyms(evt.getSelectedSyms(), mapView);
    }

    private void showSyms(List<SeqSymmetry> selected_syms, SeqMapView seqMap) {
        List<Map<String, Object>> propList = new ArrayList<Map<String, Object>>();
		for (SeqSymmetry sym : selected_syms) {
            Map<String, Object> props = determineProps(sym, seqMap);
            propList.add(props);
        }
        Map[] prop_array = propList.toArray(new Map[propList.size()]);

        List<String> prop_order = determineOrder();
        this.showProperties(prop_array, prop_order, "");
    }

    private static Map<String, Object> determineProps(SeqSymmetry sym, SeqMapView seqMap) {
        Map<String, Object> props = null;
        if (sym instanceof SymWithProps) {
            // using Propertied.cloneProperties() here instead of Propertied.getProperties()
            //   because adding start, end, id, and length as additional key-val pairs to props Map
            //   and don't want these to bloat up sym's properties
            props = ((SymWithProps) sym).cloneProperties();
        }
        if (props == null && sym instanceof DerivedSeqSymmetry) {
            SeqSymmetry original_sym = ((DerivedSeqSymmetry) sym).getOriginalSymmetry();
            if (original_sym instanceof SymWithProps) {
                props = ((SymWithProps) original_sym).cloneProperties();
            }
        }
        if (props == null) {
            // make an empty hashtable if sym has no properties...
            props = new Hashtable<String, Object>();
        }
        String symid = sym.getID();
        if (symid != null) {
            props.put("id", symid);
        }
        if (seqMap != null) {
            SeqSpan span = seqMap.getViewSeqSpan(sym);
            if (span != null) {
                String chromID = span.getBioSeq().getID();
                props.put("chromosome", chromID);
                props.put("start",
                        NumberFormat.getIntegerInstance().format(span.getStart()));
                props.put("end",
                        NumberFormat.getIntegerInstance().format(span.getEnd()));
                props.put("length",
                        NumberFormat.getIntegerInstance().format(span.getLength()));
                props.remove("seq id"); // this is redundant if "chromosome" property is set
                if (props.containsKey("method") && !props.containsKey("type")) {
                    props.put("type", props.get("method"));
                    props.remove("method");
                }
            }
        }
		if (sym instanceof GraphSym) {
			float[] range = ((GraphSym)sym).getVisibleYRange();
			props.put("min score", range[0]);
			props.put("max score", range[1]);
		}
        return props;
    }

    // The general order these fields should show up in.
    private static List<String> determineOrder() {
        List<String> prop_order;

        prop_order = new ArrayList<String>(20);
        prop_order.add("gene name");
        prop_order.add("name");
        prop_order.add("id");
        prop_order.add("chromosome");
        prop_order.add("start");
        prop_order.add("end");
        prop_order.add("length");
		prop_order.add("min score");
		prop_order.add("max score");
        prop_order.add("type");
        prop_order.add("same orientation");
        prop_order.add("query length");
        prop_order.add("# matches");
        prop_order.add("# target inserts");
        prop_order.add("# target bases inserted");
        prop_order.add("# query bases inserted");
        prop_order.add("# query inserts");
        prop_order.add("seq id");
        prop_order.add("cds min");
        prop_order.add("cds max");

        return prop_order;
    }
}


